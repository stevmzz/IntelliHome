import socket
import threading
import tkinter as tk
from tkinter import scrolledtext, ttk
from datetime import datetime
import sqlite3
from tkinter import messagebox

class ChatServer:
    def __init__(self, host='192.168.100.6', port=1717):
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
        
        # Pestaña de logs
        self.logs_frame = tk.Frame(self.notebook, bg='#1e1e1e')
        self.users_frame = tk.Frame(self.notebook, bg='#1e1e1e')
        
        self.notebook.add(self.logs_frame, text='Terminal')
        self.notebook.add(self.users_frame, text='Users')
        
        # Configurar pestaña de logs
        self.setup_logs_tab()
        
        # Configurar pestaña de usuarios
        self.setup_users_tab()

        # Iniciar el hilo de conexiones
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.daemon = True
        self.thread.start()

        # Centrar la ventana
        self.center_window()
        
        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

    def init_database(self):
        try:
            self.conn = sqlite3.connect('intellihome.db', check_same_thread=False)
            self.cursor = self.conn.cursor()
            
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

        self.refresh_users_table()

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

    def save_user_to_db(self, user_data):
        try:
            self.cursor.execute("""
                INSERT INTO users (
                    username, password, full_name, email, description, 
                    hobbies, phone, verification, iban, birth_date, user_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                user_data[1],                # username
                user_data[3],                # password
                user_data[0],                # full_name
                user_data[2],                # email
                user_data[4],                # description
                user_data[5],                # hobbies
                user_data[6],                # phone
                user_data[7],                # verification
                user_data[8],                # iban
                user_data[9],                # birth_date
                user_data[10].strip()        # user_type
            ))
            self.conn.commit()
            self.refresh_users_table()
        except Exception as e:
            print(f"Error guardando usuario en DB: {e}")
            raise e

    def center_window(self):
        self.root.update_idletasks()
        width = self.root.winfo_width()
        height = self.root.winfo_height()
        x = (self.root.winfo_screenwidth() // 2) - (width // 2)
        y = (self.root.winfo_screenheight() // 2) - (height // 2)
        self.root.geometry(f'{width}x{height}+{x}+{y}')

    def accept_connections(self):
        print("Servidor iniciado. Esperando conexiones...")
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
                    try:
                        username, password = [x.strip() for x in message[6:].split(',')]
                        print(f"Intento de login - Usuario: {username}")
                        
                        self.cursor.execute("SELECT password FROM users WHERE username = ?", (username,))
                        result = self.cursor.fetchone()
                        
                        if result and result[0] == password:
                            print(f"Login exitoso para usuario: {username}")
                            client_socket.sendall("SUCCESS\n".encode('utf-8'))
                            self.update_chat_display(f"Usuario {username} ha iniciado sesión")
                        else:
                            print(f"Login fallido para usuario: {username}")
                            client_socket.sendall("ERROR: Usuario o contraseña incorrectos\n".encode('utf-8'))
                            
                    except Exception as e:
                        print(f"Error en login: {e}")
                        client_socket.sendall(f"ERROR: {str(e)}\n".encode('utf-8'))
                
                elif message.startswith("REGISTER:"):
                    try:
                        user_data = message[9:].strip().split(',')
                        username = user_data[1].strip()

                        self.cursor.execute("SELECT username FROM users WHERE username = ?", (username,))
                        if self.cursor.fetchone():
                            print(f"Intento de registro fallido: el usuario {username} ya existe")
                            client_socket.sendall("ERROR: Usuario ya existe\n".encode('utf-8'))
                        else:
                            self.save_user_to_db(user_data)
                            
                            self.users[username] = {
                                'password': user_data[3],
                                'data': user_data
                            }
                            
                            print(f"Nuevo usuario registrado: {username}")
                            client_socket.sendall("SUCCESS\n".encode('utf-8'))
                            self.update_chat_display(f"Nuevo usuario registrado: {username}")
                            
                    except Exception as e:
                        print(f"Error en registro: {e}")
                        client_socket.sendall(f"ERROR: {str(e)}\n".encode('utf-8'))
            
            except socket.timeout:
                print("Timeout en la conexión con el cliente")
                break
            except Exception as e:
                print(f"Error manejando cliente: {e}")
                break
        
        print("Cerrando conexión con el cliente")
        try:
            client_socket.close()
            if client_socket in self.clients:
                self.clients.remove(client_socket)
        except:
            pass

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

    def broadcast(self, message, sender_socket=None):
        self.update_chat_display(f"Broadcast: {message}")
        
        for client in self.clients:
            if client != sender_socket:
                try:
                    client.send(message.encode('utf-8'))
                except:
                    client.close()
                    self.clients.remove(client)

    def send_message_thread(self):
        message = self.message_entry.get().strip()
        if message:
            self.broadcast(message + "\n")
            self.message_entry.delete(0, tk.END)

    def close_server(self):
        try:
            for client in self.clients:
                try:
                    client.close()
                except:
                    pass
            self.server_socket.close()
            self.conn.close()
            print("Servidor cerrado correctamente")
        except Exception as e:
            print(f"Error al cerrar el servidor: {e}")
        finally:
            self.root.quit()
            self.root.destroy()

if __name__ == "__main__":
    try:
        servidor = ChatServer()
    except Exception as e:
        print(f"Error al iniciar el servidor: {e}")