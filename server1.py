import socket
import threading
import tkinter as tk
from tkinter import scrolledtext, ttk
from datetime import datetime
import sqlite3
from tkinter import messagebox
from twilio.rest import Client
import time
import serial
import websockets
import json
import asyncio

sidTwilio = 'ACd64bd0758f4a7eb9b8ea2e97daa1a032'
tokenTwilio = '183d889b352f38548d1339d5e9335e38'

class ChatServer:
    def __init__(self, host='192.168.100.45', port=1717):
    
        # Inicializar la base de datos
        self.init_database()
        
        # Configuración del servidor
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((host, port))
        self.server_socket.listen(5)
        self.clients = []
        self.users = self.load_users_from_db()

        # Añadir soporte WebSocket - MOVER AQUÍ
        self.websocket_clients = set()
        self.websocket_server = None
        
        # Iniciar servidor WebSocket
        self.start_websocket_server()

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

        try:
            self.arduino = serial.Serial('COM3', 9600, timeout=1)
            time.sleep(2)  # Esperar inicialización de Arduino
            self.arduino_lock = threading.Lock()
            
            # Primera lectura con múltiples intentos
            self._update_chat_display("Inicializando conexión con Arduino...")
            max_attempts = 3
            dht_success = False
            
            for attempt in range(max_attempts):
                try:
                    # Limpiar buffers antes de cada intento
                    self.arduino.reset_input_buffer()
                    self.arduino.reset_output_buffer()
                    
                    # Intentar leer el sensor
                    response = self.handle_arduino_command("READ_DHT")
                    if "DHT_DATA" in response:
                        data = eval(response.split("DHT_DATA:")[1])
                        self._update_chat_display(
                            f"Sensor DHT11 conectado - Temperatura: {data['temperatura']}°C, Humedad: {data['humedad']}%"
                        )
                        dht_success = True
                        break
                    else:
                        if attempt < max_attempts - 1:
                            self._update_chat_display(f"Reintentando lectura del sensor ({attempt + 1}/{max_attempts})...")
                            time.sleep(2)  # Esperar entre intentos
                except Exception as e:
                    if attempt < max_attempts - 1:
                        self._update_chat_display(f"Error en intento {attempt + 1}: {str(e)}")
                        time.sleep(2)
                    else:
                        raise e

            if not dht_success:
                self._update_chat_display("Advertencia: No se pudo inicializar el sensor DHT11")
            
            # Confirmar que la conexión básica con Arduino está funcionando
            self._update_chat_display("Arduino conectado y funcionando")

            self.fire_thread = threading.Thread(target=self.monitor_fire)
            self.fire_thread.daemon = True
            self.fire_thread.start()
            
        except Exception as e:
            self._update_chat_display(f"Error conectando con Arduino: {e}")
            self.arduino = None
            self.arduino_lock = None

        self.refresh_users_table()  # Añadir esta línea
        self.refresh_properties_table()  # Añadir esta línea

        # Iniciar el hilo de conexiones
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.daemon = True
        self.thread.start()

        self.center_window()
        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

        if self.arduino:
            self.update_chat_display("Conexión con Arduino establecida")
        else:
            self.update_chat_display("No se pudo conectar con Arduino")

    def init_database(self):
        try:
            self.conn = sqlite3.connect('intellihome.db', check_same_thread=False)
            self.cursor = self.conn.cursor()
            
            # Verificar las tablas existentes
            self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
            existing_tables = {table[0] for table in self.cursor.fetchall()}
            print("Tablas existentes:", existing_tables)
            
            # Verificar si la columna fingerprint_registered existe
            if 'users' in existing_tables:
                self.cursor.execute("PRAGMA table_info(users)")
                columns = [col[1] for col in self.cursor.fetchall()]
                
                if 'fingerprint_registered' not in columns:
                    print("Agregando columna fingerprint_registered a users")
                    self.cursor.execute('''
                        ALTER TABLE users
                        ADD COLUMN fingerprint_registered BOOLEAN DEFAULT 0
                    ''')
                    self.conn.commit()
                    print("Columna fingerprint_registered agregada exitosamente")
            else:
                print("Creando tabla users")
                self.cursor.execute('''
                    CREATE TABLE users (
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
                        fingerprint_registered BOOLEAN DEFAULT 0,
                        registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                ''')

            # Crear tabla properties si no existe
            if 'properties' not in existing_tables:
                print("Creando tabla properties")
                self.cursor.execute('''
                    CREATE TABLE properties (
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

            # Crear tabla rented_properties si no existe
            if 'rented_properties' not in existing_tables:
                print("Creando tabla rented_properties")
                self.cursor.execute("""
                    CREATE TABLE rented_properties (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        property_id INTEGER,
                        rented_by TEXT,
                        check_in_date DATE,
                        check_out_date DATE,
                        total_nights INTEGER,
                        total_price REAL,
                        rent_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        status TEXT DEFAULT 'active',
                        FOREIGN KEY (property_id) REFERENCES properties(id),
                        FOREIGN KEY (rented_by) REFERENCES users(username)
                    )
                """)
            
            # Activar soporte para claves foráneas
            self.cursor.execute("PRAGMA foreign_keys = ON")
            
            self.conn.commit()
            print("Base de datos inicializada correctamente")
            
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
        self.message_entry.pack(side='left', padx=(185, 10))

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

        # Frame para botones
        button_frame = tk.Frame(self.users_frame, bg='#1e1e1e')
        button_frame.pack(pady=10)

        refresh_button = tk.Button(
            button_frame,
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
        refresh_button.pack(side='left', padx=5)

        clear_button = tk.Button(
            button_frame,
            text="Limpiar Usuarios",
            command=self.clear_users_table,
            bg='#2d2d2d',
            fg='#ff4444',
            activebackground='#3d3d3d',
            activeforeground='#ff4444',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        clear_button.pack(side='left', padx=5)

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

        # Frame para botones
        button_frame = tk.Frame(self.properties_frame, bg='#1e1e1e')
        button_frame.pack(pady=10)

        refresh_button = tk.Button(
            button_frame,
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
        refresh_button.pack(side='left', padx=5)

        clear_button = tk.Button(
            button_frame,
            text="Limpiar Propiedades",
            command=self.clear_properties_table,
            bg='#2d2d2d',
            fg='#ff4444',
            activebackground='#3d3d3d',
            activeforeground='#ff4444',
            font=('Consolas', 10, 'bold'),
            relief='flat',
            bd=0,
            padx=20
        )
        clear_button.pack(side='left', padx=5)

    def clear_users_table(self):
        if messagebox.askyesno("Confirmar", "¿Estás seguro de que quieres eliminar todos los usuarios? Esta acción no se puede deshacer."):
            try:
                self.cursor.execute("DELETE FROM users")
                self.conn.commit()
                self.users = {}  # Limpiar el diccionario de usuarios en memoria
                self.refresh_users_table()
                self.update_chat_display("Tabla de usuarios limpiada exitosamente")
                messagebox.showinfo("Éxito", "Todos los usuarios han sido eliminados")
            except Exception as e:
                error_msg = f"Error al limpiar la tabla de usuarios: {str(e)}"
                self.update_chat_display(error_msg)
                messagebox.showerror("Error", error_msg)

    def clear_properties_table(self):
        if messagebox.askyesno("Confirmar", "¿Estás seguro de que quieres eliminar todas las propiedades? Esta acción no se puede deshacer."):
            try:
                self.cursor.execute("DELETE FROM properties")
                self.conn.commit()
                self.refresh_properties_table()
                self.update_chat_display("Tabla de propiedades limpiada exitosamente")
                messagebox.showinfo("Éxito", "Todas las propiedades han sido eliminadas")
            except Exception as e:
                error_msg = f"Error al limpiar la tabla de propiedades: {str(e)}"
                self.update_chat_display(error_msg)
                messagebox.showerror("Error", error_msg)

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
            # Modificar las columnas del Treeview - removemos la columna de estado
            self.properties_tree['columns'] = ('ID', 'Propietario', 'Título', 'Ubicación', 'Precio/Noche', 'Tipo', 'Fecha Creación')
            
            for col in self.properties_tree['columns']:
                self.properties_tree.heading(col, text=col)
                self.properties_tree.column(col, width=100)

            # Consulta simplificada - mostramos todas las propiedades
            self.cursor.execute("""
                SELECT 
                    p.id, 
                    u.username, 
                    p.title, 
                    p.location, 
                    p.price_per_night, 
                    p.property_type,
                    p.creation_date
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

    def _update_chat_display(self, message, tag='info'):
        self.chat_display.config(state='normal')
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

    def handle_check_property_status(self, client_socket, message):
        try:
            property_id, username = message[21:].split(',')
            property_id = property_id.strip()
            username = username.strip()

            # Verificar si la propiedad está alquilada
            self.cursor.execute("""
                SELECT rented_by FROM rented_properties 
                WHERE property_id = ? AND status = 'active'
            """, (property_id,))
            
            result = self.cursor.fetchone()
            
            if result:
                if result[0] == username:
                    client_socket.sendall("RENTED_BY_YOU\n".encode('utf-8'))
                else:
                    client_socket.sendall("RENTED_BY_OTHER\n".encode('utf-8'))
            else:
                client_socket.sendall("AVAILABLE\n".encode('utf-8'))
                
        except Exception as e:
            error_msg = f"Error verificando estado de propiedad: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

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
                elif message.startswith("LED_ON:") or message.startswith("LED_OFF:"):
                    self.handle_led_command(client_socket, message)
                elif message.startswith("CHECK_PROPERTY_AVAILABILITY:"):
                    self.handle_check_property_availability(client_socket, message)
                elif message.startswith("DOOR_OPEN:") or message.startswith("DOOR_CLOSE:"):
                    self.handle_door_command(client_socket, message)
                elif message.startswith("READ_DHT"):
                    self.handle_dht_reading(client_socket)
                elif message.startswith("CHECK_FIRE"):
                    self.handle_fire_reading(client_socket)
                elif message.startswith("REGISTER:"):
                    self.handle_register(client_socket, message)
                elif message.startswith("ADD_PROPERTY:"):
                    self.handle_add_property(client_socket, message) 
                elif message.startswith("GET_PROPERTIES:"):
                    self.handle_get_properties(client_socket, message)
                elif message.startswith("GET_ALL_PROPERTIES"):
                    self.handle_get_all_properties(client_socket, message)
                elif message.startswith("CHECK_PROPERTY_STATUS:"):
                    self.handle_check_property_status(client_socket, message)
                elif message.startswith("RENT_PROPERTY:"):
                    self.handle_rent_property(client_socket, message)
                elif message.startswith("GET_RENT_HISTORY:"):    # Agregar esta línea
                    self.handle_get_rent_history(client_socket, message)
                
            except socket.timeout:
                print("Timeout en la conexión con el cliente")
                break
            except Exception as e:
                print(f"Error manejando cliente: {e}")
                break
        
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
            fingerprint_registered = user_data[11].strip() if len(user_data) > 11 else "0"  # Convertir a booleano
            
            print(f"Intento de registro - Usuario: {username}, Tipo: {user_type}, Huella: {fingerprint_registered}")

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
                    hobbies, phone, verification, iban, birth_date, user_type,
                    fingerprint_registered
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                user_data[0].strip(),     # full_name
                username,                  # username
                email,                    # email
                user_data[3].strip(),     # password
                user_data[4].strip(),     # description
                user_data[5].strip(),     # hobbies
                user_data[6].strip(),     # phone
                user_data[7].strip(),     # verification
                user_data[8].strip(),     # iban
                user_data[9].strip(),     # birth_date
                user_type,                # user_type
                fingerprint_registered    # fingerprint_registered
            ))
            
            self.conn.commit()
            
            # Actualizar la lista de usuarios en memoria
            self.users = self.load_users_from_db()
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
                    
            # Query modificada para excluir propiedades alquiladas
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
                LEFT JOIN rented_properties r ON p.id = r.property_id AND r.status = 'active'
                WHERE u.username = ? AND r.id IS NULL
                ORDER BY p.creation_date DESC
            """
            
            self.cursor.execute(query, (username,))
            properties = self.cursor.fetchall()
            print(f"Propiedades encontradas para {username}: {len(properties)}")
            
            if not properties:
                client_socket.sendall("SUCCESS:\n".encode('utf-8'))
                return
            
            properties_data = []
            for prop in properties:
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
            print(f"Enviando respuesta con {len(properties_data)} propiedades para {username}")
            client_socket.sendall(f"{response}\n".encode('utf-8'))
            
        except Exception as e:
            error_msg = f"Error al obtener propiedades: {str(e)}"
            print(error_msg)
            self.update_chat_display(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_add_property(self, client_socket, message):
        try:
            print(f"Mensaje completo recibido: {message}")
        
            message_content = message.replace("ADD_PROPERTY:", "").strip()
            data = message_content.split(',', maxsplit=9) # Dividir en exactamente 10 partes
            
            if len(data) != 10:
                raise ValueError(f"Datos insuficientes. Esperados: 10, Recibidos: {len(data)}")
            
            # Obtener datos
            username = data[0].strip()
            title = data[1].strip()
            description = data[2].strip()
            price = float(data[3]) if data[3].strip() else 0
            location = data[4].strip()
            capacity = int(data[5]) if data[5].strip() else 0
            property_type = data[6].strip()
            amenities = data[7].strip()
            photos = data[8].strip()
            rules = data[9].strip()

            # Verificar usuario
            self.cursor.execute("SELECT id FROM users WHERE username = ?", (username,))
            user_result = self.cursor.fetchone()
            if not user_result:
                raise ValueError(f"Usuario no encontrado: {username}")
            
            owner_id = user_result[0]

            # Procesar amenidades - mantener el formato original con |
            clean_amenities = amenities.strip()
            print(f"Amenidades a guardar: {clean_amenities}")

            # Procesar reglas - guardar como texto plano
            clean_rules = rules.strip()
            print(f"Reglas a guardar: {clean_rules}")

            # Insertar en la base de datos
            self.cursor.execute("""
                INSERT INTO properties (
                    owner_id, title, description, price_per_night,
                    location, capacity, property_type, amenities, photos, rules
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                owner_id, title, description, price,
                location, capacity, property_type, 
                clean_amenities,  # Amenidades separadas por |
                photos, 
                clean_rules      # Reglas como texto plano
            ))
            
            self.conn.commit()
            self.refresh_properties_table()
            
            print(f"Datos insertados en la base de datos:")
            print(f"Amenidades: {clean_amenities}")
            print(f"Reglas: {clean_rules}")
            
            client_socket.sendall("SUCCESS:PROPERTY_ADDED\n".encode('utf-8'))
            
        except Exception as e:
            error_msg = f"Error al agregar propiedad: {str(e)}"
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
            # Cerrar conexiones websocket
            if hasattr(self, 'websocket_clients'):
                for client in list(self.websocket_clients):
                    try:
                        asyncio.run(client.close())
                    except:
                        pass
                self.websocket_clients.clear()
            
            if self.websocket_server:
                self.websocket_server.close()
            
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
            query = """
                SELECT 
                    p.id, u.username, p.title, COALESCE(p.description, ''), 
                    COALESCE(p.price_per_night, 0), COALESCE(p.location, ''), 
                    COALESCE(p.capacity, 0), COALESCE(p.property_type, ''),
                    COALESCE(p.amenities, ''), COALESCE(p.photos, ''), 
                    COALESCE(p.rules, ''), COALESCE(p.creation_date, CURRENT_TIMESTAMP)
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

    def handle_reset_password(self, client_socket, message):
        try:
            # Formato esperado: RESET_PASSWORD:username,security_answer,new_password
            # Primero separamos el comando del resto del mensaje
            command_parts = message.split(':', 1)  # Dividir solo en el primer :
            if len(command_parts) != 2:
                raise ValueError("Formato inválido del mensaje")
                
            # Ahora separamos los datos
            data = command_parts[1].split(',')
            if len(data) != 3:
                raise ValueError("Datos incompletos")
                
            username, security_answer, new_password = [x.strip() for x in data]
            print(f"Intento de reset de contraseña para usuario: '{username}'")
            
            # Verificar que el usuario existe y la respuesta de seguridad
            self.cursor.execute("""
                SELECT id, verification
                FROM users 
                WHERE username = ?
            """, (username,))
            
            result = self.cursor.fetchone()
            
            if not result:
                print(f"Usuario no encontrado: '{username}'")
                client_socket.sendall("ERROR:INVALID_USER\n".encode('utf-8'))
                return
                
            user_id, stored_answer = result
            
            print(f"Comparando respuestas - Almacenada: '{stored_answer}', Recibida: '{security_answer}'")
            
            # Comparar respuestas ignorando mayúsculas/minúsculas y espacios
            if stored_answer.lower().strip() != security_answer.lower().strip():
                print(f"Respuesta de seguridad incorrecta para usuario: {username}")
                print(f"Esperada: '{stored_answer.lower().strip()}', Recibida: '{security_answer.lower().strip()}'")
                client_socket.sendall("ERROR:INVALID_ANSWER\n".encode('utf-8'))
                return
                
            # Actualizar la contraseña
            self.cursor.execute("""
                UPDATE users 
                SET password = ? 
                WHERE id = ?
            """, (new_password, user_id))
            
            self.conn.commit()
            print(f"Contraseña actualizada exitosamente para usuario: {username}")
            
            # Actualizar usuarios en memoria
            self.users = self.load_users_from_db()
            
            client_socket.sendall("SUCCESS:Password updated successfully\n".encode('utf-8'))
            self.update_chat_display(f"Contraseña actualizada para usuario: {username}")
            
        except Exception as e:
            error_msg = f"Error en reset de contraseña: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))
            
        finally:
            # Agregar logs adicionales para depuración
            print("Estado final del proceso de reset de contraseña:")
            self.cursor.execute("SELECT username, verification FROM users WHERE username = ?", (username,))
            debug_result = self.cursor.fetchone()
            if debug_result:
                print(f"Usuario en BD - Nombre: '{debug_result[0]}', Verificación: '{debug_result[1]}'")
            else:
                print("Usuario no encontrado en la verificación final")

    def handle_check_property_availability(self, client_socket, message):
        try:
            # Log inicial para debug
            print(f"Recibida solicitud de disponibilidad: {message}")
            
            # Formato esperado: CHECK_PROPERTY_AVAILABILITY:property_id,check_in,check_out
            parts = message[28:].split(',')
            if len(parts) != 3:
                error_msg = f"Formato inválido: se requieren ID y fechas. Recibido: {parts}"
                print(error_msg)
                client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))
                return
                
            property_id, check_in, check_out = [p.strip() for p in parts]
            
            print(f"Verificando disponibilidad - ID: {property_id}, "
                f"Check-in: {check_in}, Check-out: {check_out}")
            
            # Verificar si hay reservas que se solapan
            query = """
                SELECT COUNT(*) FROM rented_properties 
                WHERE property_id = ? 
                AND status = 'active'
                AND (
                    (check_in_date <= ? AND check_out_date >= ?) OR
                    (check_in_date <= ? AND check_out_date >= ?) OR
                    (check_in_date >= ? AND check_out_date <= ?)
                )
            """
            
            try:
                self.cursor.execute(query, (
                    property_id, 
                    check_in, check_in, 
                    check_out, check_out, 
                    check_in, check_out
                ))
                
                count = self.cursor.fetchone()[0]
                
                # Log del resultado
                print(f"Resultado de la consulta - Conflictos encontrados: {count}")
                
                if count > 0:
                    print(f"Propiedad {property_id} no disponible para las fechas solicitadas")
                    client_socket.sendall("UNAVAILABLE\n".encode('utf-8'))
                else:
                    print(f"Propiedad {property_id} disponible para las fechas solicitadas")
                    client_socket.sendall("AVAILABLE\n".encode('utf-8'))
                    
            except sqlite3.Error as sql_e:
                error_msg = f"Error en la base de datos: {str(sql_e)}"
                print(error_msg)
                client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))
                
        except Exception as e:
            error_msg = f"Error verificando disponibilidad: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))
            
        finally:
            # Asegurarnos de que siempre haya una respuesta
            try:
                client_socket.sendall("\n".encode('utf-8'))
            except:
                pass

    def handle_rent_property(self, client_socket, message):
        try:
            # Validar formato del mensaje
            if ":" not in message:
                raise ValueError("Formato de mensaje inválido")
                
            command, data = message.split(":", 1)
            if command != "RENT_PROPERTY":
                raise ValueError("Comando incorrecto")
                
            # Procesar los datos (ahora incluyendo fechas)
            try:
                parts = data.strip().split(",")
                if len(parts) != 4:
                    raise ValueError("Se requieren: ID de propiedad, usuario, fecha entrada y fecha salida")
                    
                property_id, username, check_in_date, check_out_date = [p.strip() for p in parts]
                
                print(f"Intento de alquiler - ID: '{property_id}', Usuario: '{username}', "
                    f"Entrada: {check_in_date}, Salida: {check_out_date}")
                
                # Validar que el ID sea numérico
                try:
                    property_id = int(property_id)
                except ValueError:
                    raise ValueError(f"ID de propiedad no válido: {property_id}")
                
                # Verificar que la propiedad exista y obtener sus datos
                self.cursor.execute("""
                    SELECT p.id, p.title, p.location, p.price_per_night, u2.phone
                    FROM properties p
                    JOIN users u ON p.owner_id = u.id
                    JOIN users u2 ON u2.username = ?
                    WHERE p.id = ?
                """, (username, property_id))
                
                property_data = self.cursor.fetchone()
                if not property_data:
                    raise ValueError(f"Propiedad {property_id} no encontrada")
                        
                # Verificar disponibilidad para las fechas solicitadas
                self.cursor.execute("""
                    SELECT COUNT(*) FROM rented_properties 
                    WHERE property_id = ? 
                    AND status = 'active'
                    AND (
                        (check_in_date <= ? AND check_out_date >= ?) OR
                        (check_in_date <= ? AND check_out_date >= ?) OR
                        (check_in_date >= ? AND check_out_date <= ?)
                    )
                """, (property_id, check_in_date, check_in_date, check_out_date, 
                    check_out_date, check_in_date, check_out_date))
                
                if self.cursor.fetchone()[0] > 0:
                    raise ValueError("Propiedad no disponible para las fechas seleccionadas")
                
                # Calcular noches y precio total
                check_in = datetime.strptime(check_in_date, '%Y-%m-%d')
                check_out = datetime.strptime(check_out_date, '%Y-%m-%d')
                total_nights = (check_out - check_in).days
                total_price = total_nights * property_data[3]  # price_per_night está en índice 3
                
                # Registrar el alquiler con las fechas y totales
                try:
                    self.cursor.execute("""
                        INSERT INTO rented_properties (
                            property_id, rented_by, check_in_date, check_out_date,
                            total_nights, total_price, rent_date, status
                        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'active')
                    """, (property_id, username, check_in_date, check_out_date, 
                        total_nights, total_price))
                    
                    self.conn.commit()
                    print(f"Propiedad {property_id} alquilada exitosamente por {username} "
                        f"del {check_in_date} al {check_out_date}")

                    # Preparar datos para la notificación
                    property_info = {
                        'title': property_data[1],
                        'location': property_data[2],
                        'price': property_data[3],
                        'check_in': check_in_date,
                        'check_out': check_out_date,
                        'total_nights': total_nights,
                        'total_price': total_price
                    }
                    renter_phone = property_data[4]

                    # Enviar notificación
                    if renter_phone:
                        notification_sent = self.send_rental_notification(property_info, renter_phone)
                        if notification_sent:
                            print(f"Notificación enviada al inquilino {username}")
                        else:
                            print(f"Error enviando notificación al inquilino {username}")
                    else:
                        print(f"No se encontró número de teléfono para el usuario {username}")
                        
                    client_socket.sendall("SUCCESS:Property rented successfully\n".encode('utf-8'))
                    
                except Exception as e:
                    self.conn.rollback()
                    raise Exception(f"Error al registrar el alquiler: {str(e)}")
                    
            except ValueError as ve:
                raise ValueError(f"Error en los datos: {str(ve)}")
                    
        except Exception as e:
            error_msg = f"Error al alquilar propiedad: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_get_rent_history(self, client_socket, message):
        try:
            username = message.split(':')[1].strip()
            print(f"Obteniendo historial para usuario: {username}")
            
            # Llamar al debug primero
            self.debug_rent_history(username)
            
            # Obtener fecha actual para comparación
            current_date = datetime.now().strftime('%Y-%m-%d')
            print(f"Fecha actual: {current_date}")
            
            query = """
                SELECT 
                    p.title,
                    p.description,
                    r.check_in_date,
                    r.check_out_date,
                    r.total_nights,
                    r.total_price,
                    CASE 
                        WHEN DATE(?) BETWEEN DATE(r.check_in_date) AND DATE(r.check_out_date) THEN 'ACTIVE'
                        WHEN DATE(?) > DATE(r.check_out_date) THEN 'COMPLETED'
                        WHEN DATE(?) < DATE(r.check_in_date) THEN 'PENDING'
                        ELSE 'INACTIVE'
                    END as status
                FROM rented_properties r
                JOIN properties p ON r.property_id = p.id
                WHERE r.rented_by = ?
                ORDER BY r.rent_date DESC
            """
            
            self.cursor.execute(query, (current_date, current_date, current_date, username))
            rentals = self.cursor.fetchall()
            print(f"Consulta ejecutada. Encontradas {len(rentals)} propiedades alquiladas")
            
            if not rentals:
                print("No se encontraron propiedades alquiladas")
                client_socket.sendall("SUCCESS:\n".encode('utf-8'))
                return
            
            rental_data = []
            for rental in rentals:
                # Log detallado de cada alquiler
                print("\nProcesando alquiler:")
                print(f"  Título: {rental[0]}")
                print(f"  Descripción: {rental[1]}")
                print(f"  Check-in: {rental[2]}")
                print(f"  Check-out: {rental[3]}")
                print(f"  Noches: {rental[4]}")
                print(f"  Precio: {rental[5]}")
                print(f"  Estado: {rental[6]}")
                
                rental_str = (f"{rental[0]}|{rental[1]}|{rental[2]}|{rental[3]}|"
                        f"{rental[4]}|{rental[5]}|{rental[6]}")
                rental_data.append(rental_str)
            
            response = "SUCCESS:" + ";".join(rental_data)
            print(f"\nEnviando respuesta con {len(rental_data)} propiedades")
            print(f"Longitud de la respuesta: {len(response)} bytes")
            client_socket.sendall(f"{response}\n".encode('utf-8'))
            
        except Exception as e:
            error_msg = f"Error obteniendo historial: {str(e)}"
            print(error_msg)
            import traceback
            traceback.print_exc()
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def debug_rent_history(self, username):
        print("\n=== Debug de Historial de Alquileres ===")
        
        try:
            # Verificar total de alquileres
            self.cursor.execute("SELECT COUNT(*) FROM rented_properties")
            total = self.cursor.fetchone()[0]
            print(f"Total de alquileres en la base de datos: {total}")
            
            # Verificar alquileres del usuario
            self.cursor.execute("""
                SELECT r.*, p.title 
                FROM rented_properties r
                JOIN properties p ON r.property_id = p.id
                WHERE r.rented_by = ?
            """, (username,))
            
            rentals = self.cursor.fetchall()
            print(f"Alquileres encontrados para {username}: {len(rentals)}")
            
            for rental in rentals:
                print(f"ID: {rental[0]}")
                print(f"  Propiedad: {rental[-1]} (ID: {rental[1]})")
                print(f"  Fechas: {rental[3]} - {rental[4]}")
                print(f"  Estado: {rental[8]}")
                print()
                
        except Exception as e:
            print(f"Error en debug: {e}")
        print("================================\n")

    def send_rental_notification(self, property_info, renter_phone):
        try:
            account_sid = sidTwilio
            auth_token = tokenTwilio
            client = Client(account_sid, auth_token)

            # Formatear el número de teléfono
            formatted_phone = f"whatsapp:+506{renter_phone.strip('+')}"
            
            # Obtener fecha actual formateada
            current_date = datetime.now().strftime("%d/%m/%Y")
            
            # Formatear el precio
            formatted_price = "₡{:,.2f}".format(float(property_info['price']))

            # Mensaje profesional actualizado con fechas
            message_body = (
                "🏠 *Confirmación de Reserva - IntelliHome*\n\n"
                f"Estimado/a cliente,\n\n"
                f"Le confirmamos que su reserva ha sido procesada exitosamente:\n\n"
                f"📍 *Propiedad:* {property_info['title']}\n"
                f"📌 *Ubicación:* {property_info['location']}\n"
                f"📅 *Fecha de entrada:* {property_info['check_in']}\n"
                f"📅 *Fecha de salida:* {property_info['check_out']}\n"
                f"💰 *Precio por noche:* {formatted_price}\n\n"
                "Si necesita asistencia o tiene alguna pregunta, no dude en contactarnos.\n\n"
                "Gracias por confiar en IntelliHome. ¡Le deseamos una excelente estadía!"
            )

            message = client.messages.create(
                body=message_body,
                from_='whatsapp:+14155238886',
                to=formatted_phone
            )

            print(f"Notificación de alquiler enviada exitosamente: {message.sid}")
            print(f"Enviada a: {formatted_phone}")
            return True

        except Exception as e:
            print(f"Error enviando notificación: {str(e)}")
            return False

    def handle_led_command(self, client_socket, message):
        try:
            response = self.handle_arduino_command(message)
            if response.startswith("SUCCESS"):
                client_socket.sendall(f"{response}\n".encode('utf-8'))
                # Actualizar UI
                led_index = message.split(":")[1]
                status = "encendido" if "LED_ON" in message else "apagado"
                self._update_chat_display(f"LED {led_index} {status}")
            else:
                raise Exception(response)
                
        except Exception as e:
            error_msg = f"Error en comando LED: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_door_command(self, client_socket, message):
        try:
            response = self.handle_arduino_command(message)
            if response.startswith("SUCCESS"):
                client_socket.sendall(f"{response}\n".encode('utf-8'))
                # Actualizar UI
                command_parts = message.split(":")
                action = "abierta" if "DOOR_OPEN" in message else "cerrada"
                door_name = command_parts[1] if len(command_parts) > 1 else "desconocida"
                self._update_chat_display(f"Puerta {door_name} {action}")
            else:
                raise Exception(response)
                
        except Exception as e:
            error_msg = f"Error en comando de puerta: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_arduino_command(self, command):
        """Maneja la comunicación con Arduino con timeouts específicos para cada comando"""
        if not self.arduino:
            return "ERROR:Arduino no conectado"
            
        try:
            with self.arduino_lock:
                # Limpiar buffers antes de enviar
                self.arduino.reset_input_buffer()
                self.arduino.reset_output_buffer()
                
                # Enviar comando silenciosamente
                self.arduino.write(f"{command}\n".encode())
                self.arduino.flush()
                
                # Ajustar timeout según el comando
                if command.startswith("READ_DHT"):
                    timeout = 2.0
                    sleep_interval = 0.2
                else:
                    timeout = 1.0
                    sleep_interval = 0.1
                
                # Esperar respuesta
                start_time = time.time()
                response = ""
                
                while time.time() - start_time < timeout:
                    if self.arduino.in_waiting:
                        try:
                            line = self.arduino.readline().decode().strip()
                            if line:
                                # Si el mensaje corresponde al comando que enviamos, guardarlo
                                if (command.startswith("READ_DHT") and "DHT_DATA" in line) or \
                                (command.startswith("LED") and "SUCCESS" in line) or \
                                (command.startswith("DOOR") and "SUCCESS" in line) or \
                                (command.startswith("CHECK_FIRE") and ("ALERTA" in line or "INFO" in line)):
                                    response = line
                                    break
                        except UnicodeDecodeError:
                            continue
                    time.sleep(sleep_interval)
                
                if not response:
                    if command.startswith("READ_DHT"):
                        return "ERROR:DHT_TIMEOUT"
                    return "ERROR:No response from Arduino"
                    
                return response
                    
        except Exception as e:
            return f"ERROR:{str(e)}"

    def handle_dht_reading(self, client_socket):
        """Manejo mejorado de lectura del sensor DHT11"""
        try:
            for attempt in range(3):
                response = self.handle_arduino_command("READ_DHT")
                
                if "DHT_DATA" in response:
                    try:
                        data_str = response.split("DHT_DATA:")[1]
                        data = eval(data_str)
                        temp = float(data['temperatura'])
                        hum = float(data['humedad'])
                        
                        if 0 <= temp <= 50 and 0 <= hum <= 100:
                            client_response = f"SUCCESS:{temp},{hum}"
                            client_socket.sendall(f"{client_response}\n".encode('utf-8'))
                            self._update_chat_display(
                                f"Lectura DHT11 - Temperatura: {temp}°C, Humedad: {hum}%"
                            )
                            return
                    except Exception as e:
                        if attempt == 2:
                            raise Exception(f"Error procesando datos: {str(e)}")
                elif "DHT_TIMEOUT" in response:
                    if attempt < 2:
                        self._update_chat_display(f"Timeout en intento {attempt + 1}, reintentando...")
                        time.sleep(1)
                        continue
                    else:
                        raise Exception("Sensor no responde después de varios intentos")
                else:
                    if attempt < 2:
                        self._update_chat_display(f"Error en intento {attempt + 1}, reintentando...")
                        time.sleep(1)
                        continue
                    else:
                        raise Exception(f"Error en lectura: {response}")
                        
            raise Exception("No se pudieron obtener lecturas válidas después de 3 intentos")
                
        except Exception as e:
            error_msg = f"Error leyendo sensor DHT11: {str(e)}"
            print(error_msg)
            self._update_chat_display(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def handle_fire_reading(self, client_socket):
        """Manejo de lectura del sensor de fuego"""
        try:
            response = self.handle_arduino_command("CHECK_FIRE")
            
            if "ALERTA: Llama detectada!" in response:
                client_socket.sendall("SUCCESS:FIRE_DETECTED\n".encode('utf-8'))
            elif "INFO: Llama apagada" in response:
                client_socket.sendall("SUCCESS:NO_FIRE\n".encode('utf-8'))
            else:
                client_socket.sendall("ERROR:Invalid response\n".encode('utf-8'))

        except Exception as e:
            error_msg = f"Error leyendo sensor de fuego: {str(e)}"
            print(error_msg)
            client_socket.sendall(f"ERROR:{error_msg}\n".encode('utf-8'))

    def send_fire_notification(self, phone_number):
        """Envía notificación de WhatsApp cuando se detecta fuego"""
        try:
            account_sid = sidTwilio
            auth_token = tokenTwilio
            client = Client(account_sid, auth_token)

            # Formatear el número de teléfono
            formatted_phone = f"whatsapp:+506{phone_number.strip('+')}"
            
            # Obtener fecha y hora actual formateada
            current_time = datetime.now().strftime("%d/%m/%Y %H:%M:%S")

            # Mensaje de alerta
            message_body = (
                "🚨 *¡ALERTA DE FUEGO! - IntelliHome*\n\n"
                "Se ha detectado fuego en su propiedad\n\n"
                f"⏰ *Fecha y hora:* {current_time}\n\n"
                "🔴 *ACCIONES REQUERIDAS:*\n"
                "1. Evacuar el área inmediatamente\n"
                "2. Llamar a los bomberos (9-1-1)\n"
                "3. No intente combatir el fuego usted mismo\n\n"
                "Este es un mensaje automático del sistema de seguridad."
            )

            message = client.messages.create(
                body=message_body,
                from_='whatsapp:+14155238886',
                to=formatted_phone
            )

            print(f"Notificación de fuego enviada exitosamente: {message.sid}")
            print(f"Enviado a: {formatted_phone}")
            return True

        except Exception as e:
            print(f"Error enviando notificación de fuego: {str(e)}")
            return False

    def monitor_fire(self):
        """Monitoreo optimizado del sensor de fuego usando WebSocket"""
        last_state = None
        buffer = ""
        last_notification_time = 0
        NOTIFICATION_INTERVAL = 10  # Intervalo de 10 segundos entre notificaciones
        
        async def send_fire_status(status):
            if not self.websocket_clients:
                return
                
            message = json.dumps({
                'type': 'fire_status',
                'status': status,
                'timestamp': int(time.time() * 1000)
            })
            
            disconnected = set()
            for client in self.websocket_clients:
                try:
                    await client.send(message)
                except websockets.exceptions.ConnectionClosed:
                    disconnected.add(client)
                except Exception as e:
                    print(f"Error enviando mensaje a cliente WebSocket: {e}")
                    disconnected.add(client)
            
            self.websocket_clients.difference_update(disconnected)

        while True:
            try:
                if self.arduino and self.arduino.in_waiting > 0:
                    with self.arduino_lock:
                        try:
                            while self.arduino.in_waiting:
                                char = self.arduino.read().decode()
                                if char == '\n':
                                    line = buffer.strip()
                                    buffer = ""
                                    
                                    current_state = None
                                    if "ALERTA: Llama detectada!" in line:
                                        current_state = "FIRE_DETECTED"
                                        self._update_chat_display(
                                            "⚠️ ALERTA: ¡Se ha detectado fuego en la propiedad!",
                                            'error'
                                        )
                                        
                                        # Verificar si es tiempo de enviar otra notificación
                                        current_time = time.time()
                                        if current_time - last_notification_time >= NOTIFICATION_INTERVAL:
                                            # Obtener números de teléfono de todos los usuarios
                                            self.cursor.execute("SELECT phone FROM users WHERE phone IS NOT NULL AND phone != ''")
                                            phones = self.cursor.fetchall()
                                            
                                            for phone in phones:
                                                self.send_fire_notification(phone[0])
                                            
                                            last_notification_time = current_time
                                            
                                    elif "INFO: Llama apagada" in line:
                                        current_state = "NO_FIRE"
                                        self._update_chat_display(
                                            "✅ INFO: No se detecta fuego en la propiedad",
                                            'success'
                                        )
                                        # Resetear el tiempo de la última notificación cuando el fuego se apaga
                                        last_notification_time = 0
                                    
                                    # Solo transmitir si hay cambio de estado
                                    if current_state and current_state != last_state:
                                        last_state = current_state
                                        loop = asyncio.new_event_loop()
                                        asyncio.set_event_loop(loop)
                                        try:
                                            loop.run_until_complete(send_fire_status(current_state))
                                        finally:
                                            loop.close()
                                else:
                                    buffer += char
                                    
                        except UnicodeDecodeError:
                            buffer = ""
                            pass
                
                time.sleep(0.01)
                    
            except Exception as e:
                print(f"Error en monitoreo de fuego: {e}")
                time.sleep(0.1)

    def start_websocket_server(self):
        try:
            async def init_websocket():
                async def websocket_handler(websocket):
                    try:
                        print(f"Nueva conexión WebSocket establecida")
                        self.websocket_clients.add(websocket)
                        try:
                            await websocket.wait_closed()
                        finally:
                            self.websocket_clients.remove(websocket)
                            print(f"Cliente WebSocket desconectado")
                    except Exception as e:
                        print(f"Error en websocket_handler: {e}")

                async with websockets.serve(
                    websocket_handler, 
                    self.server_socket.getsockname()[0], 
                    8765
                ) as server:
                    self.websocket_server = server
                    await asyncio.Future()  # run forever

            # Crear un nuevo event loop en un hilo separado
            def run_websocket_server():
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                loop.run_until_complete(init_websocket())

            websocket_thread = threading.Thread(target=run_websocket_server, daemon=True)
            websocket_thread.start()
            print("Servidor WebSocket iniciado en puerto 8765")
                
        except Exception as e:
            print(f"Error iniciando servidor WebSocket: {e}")

    async def broadcast_fire_status(self, status):
        if not self.websocket_clients:
            return
            
        message = json.dumps({
            'type': 'fire_status',
            'status': status
        })
        
        disconnected = set()
        for client in self.websocket_clients:
            try:
                await client.send(message)
            except websockets.exceptions.ConnectionClosed:
                disconnected.add(client)
            except Exception as e:
                print(f"Error enviando mensaje a cliente WebSocket: {e}")
                disconnected.add(client)
        
        # Eliminar clientes desconectados
        self.websocket_clients.difference_update(disconnected)

if __name__ == "__main__":
    try:
        servidor = ChatServer()
    except Exception as e:
        print(f"Error al iniciar el servidor: {e}")