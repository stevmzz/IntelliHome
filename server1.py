import socket
import threading
import tkinter as tk
from tkinter import scrolledtext, ttk
from datetime import datetime
import sqlite3
from tkinter import messagebox

class ChatServer:
    def __init__(self, host='192.168.43.149', port=1717):
        # Inicializar la base de datos
        self.init_database()
        
        # Configuración del servidor
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((host, port))
        self.server_socket.listen(5)
        self.clients = []
        self.users = self.load_users_from_db()

        # Configuración de la interfaz gráfica
        self.root = tk.Tk()
        self.root.title("IntelliHome Server")
        self.root.configure(bg='#1e1e1e')
        
        # Crear notebook para pestañas
        self.notebook = ttk.Notebook(self.root)
        self.notebook.pack(expand=True, fill='both', padx=20, pady=20)
        
        # Pestañas
        self.logs_frame = tk.Frame(self.notebook, bg='#1e1e1e')
        self.users_frame = tk.Frame(self.notebook, bg='#1e1e1e')
        self.properties_frame = tk.Frame(self.notebook, bg='#1e1e1e')
        
        self.notebook.add(self.logs_frame, text='Terminal')
        self.notebook.add(self.users_frame, text='Users')
        self.notebook.add(self.properties_frame, text='Properties')
        
        # Configurar pestañas
        self.setup_logs_tab()
        self.setup_users_tab()
        self.setup_properties_tab()

        self.refresh_users_table()  # Añadir esta línea
        self.refresh_properties_table()  # Añadir esta línea

        # Iniciar el hilo de conexiones
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.daemon = True
        self.thread.start()

        self.center_window()
        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

    def init_database(self):
        try:
            self.conn = sqlite3.connect('intellihome.db', check_same_thread=False)
            self.cursor = self.conn.cursor()
            
            # Tabla de usuarios
            self.cursor.execute('''
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    full_name TEXT,
                    email TEXT,
                    description TEXT,
                    hobbies TEXT,
                    phone TEXT,
                    verification TEXT,
                    iban TEXT,
                    birth_date TEXT,
                    user_type TEXT,
                    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')

            # Tabla de propiedades
            self.cursor.execute('''
                CREATE TABLE IF NOT EXISTS properties (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_id INTEGER,
                    title TEXT NOT NULL,
                    description TEXT,
                    price_per_night REAL NOT NULL,
                    location TEXT,
                    capacity INTEGER,
                    property_type TEXT,
                    photos TEXT,
                    amenities TEXT,
                    rules TEXT,
                    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                )
            ''')
            
            self.conn.commit()
        except Exception as e:
            print(f"Error inicializando la base de datos: {e}")
            messagebox.showerror("Error", f"Error en la base de datos: {e}")

    def load_users_from_db(self):
        users = {}
        try:
            self.cursor.execute("SELECT * FROM users")
            rows = self.cursor.fetchall()
            for row in rows:
                users[row[1]] = {
                    'password': row[2],
                    'data': [row[3], row[1], row[4], row[2], row[5], row[6], 
                            row[7], row[8], row[9], row[10], row[11]]
                }
        except Exception as e:
            print(f"Error cargando usuarios: {e}")
        return users

    def center_window(self):
        self.root.update_idletasks()
        width = self.root.winfo_width()
        height = self.root.winfo_height()
        x = (self.root.winfo_screenwidth() // 2) - (width // 2)
        y = (self.root.winfo_screenheight() // 2) - (height // 2)
        self.root.geometry(f'{width}x{height}+{x}+{y}')

    def setup_logs_tab(self):
        title_label = tk.Label(self.logs_frame,
                             text="IntelliHome Server Console",
                             font=('Consolas', 16, 'bold'),
                             bg='#1e1e1e',
                             fg='#96C0FA')
        title_label.pack(pady=(0, 10))

        self.chat_display = scrolledtext.ScrolledText(
            self.logs_frame,
            width=70,
            height=25,
            bg='#2d2d2d',
            fg='#ffffff',
            insertbackground='#00ff00',
            font=('Consolas', 10),
            state='disabled'
        )
        self.chat_display.pack(pady=(0, 10))
        
        input_frame = tk.Frame(self.logs_frame, bg='#1e1e1e')
        input_frame.pack(fill='x', pady=(0, 10))

        self.message_entry = tk.Entry(
            input_frame,
            width=50,
            bg='#2d2d2d',
            fg='#ffffff',
            insertbackground='#00ff00',
            font=('Consolas', 10),
            relief='flat',
            bd=5
        )
        self.message_entry.pack(side='left', padx=(0, 10))

        self.send_button = tk.Button(
            input_frame,
            text="ENVIAR",
            command=self.send_message_thread,
            bg='#2d2d2d',
            fg='#96C0FA',
            activebackground='#3d3d3d',
            activeforeground='#00ff00',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        self.send_button.pack(side='left', padx=5)

        self.quit_button = tk.Button(
            self.logs_frame,
            text="CERRAR SERVIDOR",
            command=self.close_server,
            bg='#2d2d2d',
            fg='#ff4444',
            activebackground='#3d3d3d',
            activeforeground='#ff4444',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        self.quit_button.pack(pady=5)

        self.chat_display.tag_config('success', foreground='#00ff00')
        self.chat_display.tag_config('error', foreground='#ff4444')
        self.chat_display.tag_config('info', foreground='#96C0FA')

    def setup_users_tab(self):
        table_frame = tk.Frame(self.users_frame, bg='#1e1e1e')
        table_frame.pack(expand=True, fill='both', padx=10, pady=10)

        columns = ('ID', 'Usuario', 'Nombre', 'Email', 'Teléfono', 'Tipo', 'Fecha Registro')
        self.users_tree = ttk.Treeview(table_frame, columns=columns, show='headings')

        for col in columns:
            self.users_tree.heading(col, text=col)
            self.users_tree.column(col, width=100)

        y_scrollbar = ttk.Scrollbar(table_frame, orient='vertical', command=self.users_tree.yview)
        x_scrollbar = ttk.Scrollbar(table_frame, orient='horizontal', command=self.users_tree.xview)
        self.users_tree.configure(yscrollcommand=y_scrollbar.set, xscrollcommand=x_scrollbar.set)

        self.users_tree.grid(row=0, column=0, sticky='nsew')
        y_scrollbar.grid(row=0, column=1, sticky='ns')
        x_scrollbar.grid(row=1, column=0, sticky='ew')

        table_frame.grid_columnconfigure(0, weight=1)
        table_frame.grid_rowconfigure(0, weight=1)

        refresh_button = tk.Button(
            self.users_frame,
            text="Actualizar Tabla",
            command=self.refresh_users_table,
            bg='#2d2d2d',
            fg='#96C0FA',
            activebackground='#3d3d3d',
            activeforeground='#00ff00',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        refresh_button.pack(pady=10)

    def setup_properties_tab(self):
        table_frame = tk.Frame(self.properties_frame, bg='#1e1e1e')
        table_frame.pack(expand=True, fill='both', padx=10, pady=10)

        columns = ('ID', 'Propietario', 'Título', 'Ubicación', 'Precio/Noche', 'Tipo', 'Fecha Creación')
        self.properties_tree = ttk.Treeview(table_frame, columns=columns, show='headings')

        for col in columns:
            self.properties_tree.heading(col, text=col)
            self.properties_tree.column(col, width=100)

        y_scrollbar = ttk.Scrollbar(table_frame, orient='vertical', command=self.properties_tree.yview)
        x_scrollbar = ttk.Scrollbar(table_frame, orient='horizontal', command=self.properties_tree.xview)
        self.properties_tree.configure(yscrollcommand=y_scrollbar.set, xscrollcommand=x_scrollbar.set)

        self.properties_tree.grid(row=0, column=0, sticky='nsew')
        y_scrollbar.grid(row=0, column=1, sticky='ns')
        x_scrollbar.grid(row=1, column=0, sticky='ew')

        table_frame.grid_columnconfigure(0, weight=1)
        table_frame.grid_rowconfigure(0, weight=1)

        refresh_button = tk.Button(
            self.properties_frame,
            text="Actualizar Propiedades",
            command=self.refresh_properties_table,
            bg='#2d2d2d',
            fg='#96C0FA',
            activebackground='#3d3d3d',
            activeforeground='#00ff00',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        refresh_button.pack(pady=10)

    def refresh_users_table(self):
        for item in self.users_tree.get_children():
            self.users_tree.delete(item)
            
        try:
            self.cursor.execute("""
                SELECT id, username, full_name, email, phone, user_type, registration_date 
                FROM users
                ORDER BY registration_date DESC
            """)
            
            for row in self.cursor.fetchall():
                self.users_tree.insert('', 'end', values=row)
                
        except Exception as e:
            print(f"Error actualizando tabla de usuarios: {e}")
            messagebox.showerror("Error", f"Error actualizando tabla: {e}")

    def refresh_properties_table(self):
        for item in self.properties_tree.get_children():
            self.properties_tree.delete(item)
            
        try:
            self.cursor.execute("""
                SELECT p.id, u.username, p.title, p.location, p.price_per_night, 
                       p.property_type, p.creation_date 
                FROM properties p
                JOIN users u ON p.owner_id = u.id
                ORDER BY p.creation_date DESC
            """)
            
            for row in self.cursor.fetchall():
                self.properties_tree.insert('', 'end', values=row)
                
        except Exception as e:
            print(f"Error actualizando tabla de propiedades: {e}")
            messagebox.showerror("Error", f"Error actualizando tabla: {e}")

    def update_chat_display(self, message):
        self.root.after(0, self._update_chat_display, message)

    def _update_chat_display(self, message):
        self.chat_display.config(state='normal')
        
        tag = 'info'
        if "exitoso" in message.lower() or "success" in message.lower():
            tag = 'success'
        elif "error" in message.lower() or "fallido" in message.lower():
            tag = 'error'
            
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.chat_display.insert(tk.END, f"[{timestamp}] ", 'info')
        self.chat_display.insert(tk.END, f"{message}\n", tag)
        self.chat_display.see(tk.END)
        self.chat_display.config(state='disabled')

    def accept_connections(self):
        print("Servidor iniciado. Esperando conexiones...")
        self.update_chat_display("Servidor iniciado y esperando conexiones...")
        while True:
            try:
                client_socket, addr = self.server_socket.accept()
                self.clients.append(client_socket)
                self.update_chat_display(f"Nueva conexión de {addr}")
                print(f"Nueva conexión aceptada de {addr}")
                
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
                client_thread.daemon = True
                client_thread.start()
            except Exception as e:
                print(f"Error aceptando conexión: {e}")
                break

    def handle_client(self, client_socket):
        client_socket.settimeout(10.0)
        while True:
            try:
                message = client_socket.recv(1024).decode('utf-8')
                print(f"Mensaje recibido: {message}")
                
                if not message:
                    break
                    
                if message.startswith("LOGIN:"):
                    self.handle_login(client_socket, message)
                elif message.startswith("REGISTER:"):
                    self.handle_register(client_socket, message)
                elif message.startswith("ADD_PROPERTY:"):
                    self.handle_add_property(client_socket, message)
                elif message.startswith("GET_PROPERTIES:"):
                    self.handle_get_properties(client_socket, message)
                elif message.startswith("UPDATE_PROPERTY:"):
                    self.handle_update_property(client_socket, message)
                elif message.startswith("DELETE_PROPERTY:"):
                    self.handle_delete_property(client_socket, message)
                elif message.startswith("GET_ALL_PROPERTIES"):
                    self.handle_get_all_properties(client_socket, message)
                
            except socket.timeout:
                print("Timeout en la conexión con el cliente")
                break
            except Exception as e:
                print(f"Error manejando cliente: {e}")
                break
        
        self.close_client_connection(client_socket)

    def handle_login(self, client_socket, message):
        try:
            username, password = [x.strip() for x in message[6:].split(',')]
            print(f"Intento de login - Usuario: {username}")
            
            self.cursor.execute("SELECT id, password, user_type FROM users WHERE username = ?", (username,))
            result = self.cursor.fetchone()
            
            if result and result[1] == password:
                user_type = result[2]
                print(f"Login exitoso para usuario: {username}, tipo: {user_type}")
                response = f"SUCCESS:{user_type}"
                client_socket.sendall(f"{response}\n".encode('utf-8'))
                self.update_chat_display(f"Usuario {username} ({user_type}) ha iniciado sesión")
            else:
                print(f"Login fallido para usuario: {username}")
                client_socket.sendall("ERROR:INVALID_CREDENTIALS\n".encode('utf-8'))
                
        except Exception as e:
            print(f"Error en login: {e}")
            client_socket.sendall(f"ERROR:{str(e)}\n".encode('utf-8'))
    
    def handle_register(self, client_socket, message):
        try:
            user_data = message[9:].strip().split(',')
            username = user_data[1].strip()
            user_type = user_data[10].strip()
            print(f"Intento de registro - Usuario: {username}, Tipo: {user_type}")

            # Verificar si el usuario ya existe
            self.cursor.execute("SELECT username FROM users WHERE username = ?", (username,))
            if self.cursor.fetchone():
                print(f"Intento de registro fallido: el usuario {username} ya existe")
                client_socket.sendall("ERROR:USERNAME_EXISTS\n".encode('utf-8'))
                return

            # Verificar si el email ya está registrado
            email = user_data[2].strip()
            self.cursor.execute("SELECT email FROM users WHERE email = ?", (email,))
            if self.cursor.fetchone():
                print(f"Intento de registro fallido: el email {email} ya está registrado")
                client_socket.sendall("ERROR:EMAIL_EXISTS\n".encode('utf-8'))
                return

            # Proceder con el registro
            self.cursor.execute("""
                INSERT INTO users (
                    full_name, username, email, password, description,
                    hobbies, phone, verification, iban, birth_date, user_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, user_data)
            
            self.conn.commit()
            
            # Actualizar la lista de usuarios en memoria
            self.users = self.load_users_from_db()  # Añadir esta línea
            self.refresh_users_table()
            
            response = f"SUCCESS:{user_type}"
            client_socket.sendall(f"{response}\n".encode('utf-8'))
            print(f"Usuario registrado exitosamente: {username} como {user_type}")
            self.update_chat_display(f"Nuevo usuario registrado: {username} ({user_type})")
            
        except Exception as e:
            print(f"Error en registro: {e}")
            client_socket.sendall(f"ERROR:{str(e)}\n".encode('utf-8'))

    def print_users_debug(self):
        print("\n=== Debug de Usuarios ===")
        print("Usuarios en memoria:", list(self.users.keys()))
        self.cursor.execute("SELECT id, username FROM users")
        db_users = self.cursor.fetchall()
        print("Usuarios en base de datos:", [(uid, uname) for uid, uname in db_users])
        print("=======================\n")

    def handle_get_properties(self, client_socket, message):
        try:
            username = message.split(':')[1].strip()
            print(f"Solicitando propiedades para usuario: '{username}'")
                    
            if not username:
                print("Error: username vacío")
                client_socket.sendall("ERROR:EMPTY_USERNAME\n".encode('utf-8'))
                return
                    
            # Obtener todas las propiedades
            query = """
                SELECT 
                    p.id, 
                    u.username,  -- nombre del propietario
                    p.title, 
                    COALESCE(p.description, ''), 
                    COALESCE(p.price_per_night, 0),
                    COALESCE(p.location, ''), 
                    COALESCE(p.capacity, 0), 
                    COALESCE(p.property_type, ''),
                    COALESCE(p.amenities, ''), 
                    COALESCE(p.photos, ''), 
                    COALESCE(p.rules, ''),
                    COALESCE(p.creation_date, CURRENT_TIMESTAMP)
                FROM properties p 
                JOIN users u ON p.owner_id = u.id
                ORDER BY p.creation_date DESC
            """
            
            self.cursor.execute(query)
            properties = self.cursor.fetchall()
            print(f"Propiedades encontradas: {len(properties)}")
            
            if not properties:
                client_socket.sendall("SUCCESS:\n".encode('utf-8'))
                return
            
            properties_data = []
            for prop in properties:
                # Asegurarse de que todos los campos estén presentes
                prop_data = [
                    str(prop[0]),      # id
                    str(prop[1]),      # username (propietario)
                    str(prop[2]),      # title
                    str(prop[3]),      # description
                    str(prop[4]),      # price_per_night
                    str(prop[5]),      # location
                    str(prop[6]),      # capacity
                    str(prop[7]),      # property_type
                    str(prop[8]),      # amenities
                    str(prop[9]),      # photos
                    str(prop[10]),     # rules
                    str(prop[11])      # creation_date
                ]
                prop_str = "|".join(prop_data)
                properties_data.append(prop_str)
                
            response = "SUCCESS:" + ";".join(properties_data)
            print(f"Enviando respuesta con {len(properties_data)} propiedades")
            client_socket.sendall(f"{response}\n".encode('utf-8'))
            
        except Exception as e:
            error_msg = f"Error al obtener propiedades: {str(e)}"
            print(error_msg)
            self.update_chat_display(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_add_property(self, client_socket, message):
        try:
            data = message[12:].split(',', maxsplit=9)
            username = data[0].strip()
            
            self.cursor.execute("SELECT id FROM users WHERE username = ?", (username,))
            user_result = self.cursor.fetchone()
            
            if not user_result:
                error_msg = f"Usuario no encontrado: '{username}'"
                print(error_msg)
                client_socket.sendall("ERROR:USER_NOT_FOUND\n".encode('utf-8'))
                return
                        
            owner_id = user_result[0]
            
            try:
                # Parsear los datos
                title = data[1].strip()
                description = data[2].strip()
                price = float(data[3]) if data[3].strip() else 0
                location = data[4].strip()
                capacity = int(data[5]) if data[5].strip() else 0
                property_type = data[6].strip()
                amenities = data[7].strip()  # Ya viene como string con formato |
                photos = data[8].strip()     # Ya viene como string con formato |
                rules = data[9].strip()      # Ya viene como string con formato |
                
                # Insertar la propiedad
                self.cursor.execute("""
                    INSERT INTO properties (
                        owner_id, title, description, price_per_night,
                        location, capacity, property_type, amenities, photos, rules
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    owner_id, title, description, price,
                    location, capacity, property_type, amenities, photos, rules
                ))
                
                self.conn.commit()
                self.refresh_properties_table()
                
                success_msg = f"Propiedad añadida exitosamente para usuario: {username}"
                print(success_msg)
                self.update_chat_display(success_msg)
                
                client_socket.sendall("SUCCESS:PROPERTY_ADDED\n".encode('utf-8'))
                
            except Exception as e:
                error_msg = f"Error al procesar datos: {str(e)}"
                print(error_msg)
                client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))
                    
        except Exception as e:
            error_msg = f"Error al añadir propiedad: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_delete_property(self, client_socket, message):
        try:
            # Format: DELETE_PROPERTY:property_id,username
            property_id, username = message[15:].split(',')
            property_id = property_id.strip()
            username = username.strip()

            # Verificar que el usuario sea el propietario
            self.cursor.execute("""
                SELECT p.id 
                FROM properties p 
                JOIN users u ON p.owner_id = u.id 
                WHERE p.id = ? AND u.username = ?
            """, (property_id, username))

            if not self.cursor.fetchone():
                client_socket.sendall("ERROR:UNAUTHORIZED\n".encode('utf-8'))
                return

            # Eliminar la propiedad
            self.cursor.execute("DELETE FROM properties WHERE id = ?", (property_id,))
            self.conn.commit()
            self.refresh_properties_table()
            client_socket.sendall("SUCCESS:PROPERTY_DELETED\n".encode('utf-8'))
            self.update_chat_display(f"Propiedad {property_id} eliminada por {username}")

        except Exception as e:
            error_msg = f"Error al eliminar propiedad: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def close_client_connection(self, client_socket):
        try:
            if client_socket in self.clients:
                self.clients.remove(client_socket)
            client_socket.close()
            print("Conexión con cliente cerrada")
        except Exception as e:
            print(f"Error cerrando conexión con cliente: {e}")

    def send_message_thread(self):
        message = self.message_entry.get().strip()
        if message:
            self.broadcast(message + "\n")
            self.message_entry.delete(0, tk.END)

    def broadcast(self, message, sender_socket=None):
        self.update_chat_display(f"Broadcast: {message}")
        for client in self.clients:
            if client != sender_socket:
                try:
                    client.send(message.encode('utf-8'))
                except:
                    client.close()
                    self.clients.remove(client)

    def close_server(self):
        try:
            # Cerrar conexiones de clientes
            for client in self.clients:
                try:
                    client.close()
                except:
                    pass
            
            # Cerrar socket del servidor
            self.server_socket.close()
            
            # Cerrar conexión de base de datos
            if self.conn:
                self.conn.close()
                
            print("Servidor cerrado correctamente")
            self.update_chat_display("Servidor cerrado correctamente")
            
        except Exception as e:
            print(f"Error al cerrar el servidor: {e}")
            self.update_chat_display(f"Error al cerrar el servidor: {e}")
        finally:
            self.root.quit()
            self.root.destroy()
    
    def handle_get_all_properties(self, client_socket, message):
        try:
            # Obtener todas las propiedades ordenadas por fecha de creación
            query = """
                SELECT 
                    p.id, u.username, p.title, COALESCE(p.description, ''), COALESCE(p.price_per_night, 0),
                    COALESCE(p.location, ''), COALESCE(p.capacity, 0), COALESCE(p.property_type, ''),
                    COALESCE(p.amenities, ''), COALESCE(p.photos, ''), COALESCE(p.rules, ''),
                    COALESCE(p.creation_date, CURRENT_TIMESTAMP)
                FROM properties p 
                JOIN users u ON p.owner_id = u.id
                ORDER BY p.creation_date DESC
            """
            
            self.cursor.execute(query)
            properties = self.cursor.fetchall()
            print(f"Total de propiedades encontradas: {len(properties)}")
            
            if not properties:
                client_socket.sendall("SUCCESS:\n".encode('utf-8'))
                return
            
            properties_data = []
            for prop in properties:
                prop_str = "|".join(str(item) if item is not None else "" for item in prop)
                properties_data.append(prop_str)
                
            response = "SUCCESS:" + ";".join(properties_data)
            client_socket.sendall(f"{response}\n".encode('utf-8'))
            
        except Exception as e:
            error_msg = f"Error al obtener propiedades: {str(e)}"
            print(error_msg)
            self.update_chat_display(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

if __name__ == "__main__":
    try:
        servidor = ChatServer()
    except Exception as e:
        print(f"Error al iniciar el servidor: {e}")