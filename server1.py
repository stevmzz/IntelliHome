import socket
import threading
import tkinter as tk
from tkinter import scrolledtext
from datetime import datetime

class ChatServer:
    def __init__(self, host='192.168.100.6', port=1717):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((host, port))
        self.server_socket.listen(5)
        self.clients = []
        self.users = {}

        # Configuración de la interfaz gráfica
        self.root = tk.Tk()
        self.root.title("IntelliHome Server")
        self.root.configure(bg='#1e1e1e')
        
        # Crear un frame principal
        main_frame = tk.Frame(self.root, bg='#1e1e1e', padx=20, pady=20)
        main_frame.pack(expand=True, fill='both')

        # Título
        title_label = tk.Label(main_frame,
                             text="IntelliHome Server Console",
                             font=('Consolas', 16, 'bold'),
                             bg='#1e1e1e',
                             fg='#96C0FA')
        title_label.pack(pady=(0, 10))

        # Display de chat con nuevo estilo
        self.chat_display = scrolledtext.ScrolledText(
            main_frame,
            width=70,
            height=25,
            bg='#2d2d2d',
            fg='#ffffff',
            insertbackground='#00ff00',
            font=('Consolas', 10),
            state='disabled'
        )
        self.chat_display.pack(pady=(0, 10))
        
        # Frame para la entrada y botón
        input_frame = tk.Frame(main_frame, bg='#1e1e1e')
        input_frame.pack(fill='x', pady=(0, 10))

        # Entry con nuevo estilo
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

        # Botones con nuevo estilo
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
            main_frame,
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

        # Añadir información de estado
        status_frame = tk.Frame(main_frame, bg='#1e1e1e')
        status_frame.pack(fill='x', pady=(10, 0))
        
        status_label = tk.Label(
            status_frame,
            text=f"Servidor activo en {host}:{port}",
            font=('Consolas', 8),
            bg='#1e1e1e',
            fg='#888888'
        )
        status_label.pack(side='left')

        # Configurar tags para colores en el chat
        self.chat_display.tag_config('success', foreground='#00ff00')
        self.chat_display.tag_config('error', foreground='#ff4444')
        self.chat_display.tag_config('info', foreground='#00aaff')

        # Iniciar el hilo de conexiones
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.daemon = True
        self.thread.start()

        # Centrar la ventana
        self.center_window()
        
        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

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
                        print(f"Intento de login - Usuario: {username}, Contraseña: {password}")
                        print(f"Usuarios registrados: {self.users}")
                        
                        if username in self.users and self.users[username]['password'] == password:
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
                        password = user_data[3].strip()

                        if username in self.users:
                            print(f"Intento de registro fallido: el usuario {username} ya existe")
                            client_socket.sendall("ERROR: Usuario ya existe\n".encode('utf-8'))
                        else:
                            clean_data = [field.strip() for field in user_data]
                            self.users[username] = {
                                'password': password,
                                'data': clean_data
                            }
                            print(f"Nuevo usuario registrado: {username} con contraseña {password}")
                            print(f"Usuarios actuales: {self.users}")
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
        
        # Determinar el tag basado en el contenido del mensaje
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