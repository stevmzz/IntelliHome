import socket
import threading
import tkinter as tk
from tkinter import scrolledtext
import json

class ChatServer:
    def __init__(self, host='192.168.100.6', port=1717):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((host, port))
        self.server_socket.listen(5)
        self.clients = []
        self.users = {}  # Diccionario para almacenar usuarios

        # Configuración de la interfaz gráfica
        self.root = tk.Tk()
        self.root.title("IntelliHomeServer")

        self.chat_display = scrolledtext.ScrolledText(self.root, state='disabled', width=50, height=20)
        self.chat_display.pack(pady=10)

        self.message_entry = tk.Entry(self.root, width=40)
        self.message_entry.pack(pady=5)

        self.send_button = tk.Button(self.root, text="Enviar", command=self.send_message_thread)
        self.send_button.pack(pady=5)

        self.quit_button = tk.Button(self.root, text="Salir", command=self.close_server)
        self.quit_button.pack(pady=5)

        # Iniciar hilo para aceptar conexiones
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.daemon = True  # El hilo se cerrará cuando el programa principal termine
        self.thread.start()

        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

    def accept_connections(self):
        print("Servidor iniciado. Esperando conexiones...")
        while True:
            try:
                client_socket, addr = self.server_socket.accept()
                self.clients.append(client_socket)
                self.chat_display.config(state='normal')
                self.chat_display.insert(tk.END, f"Nueva conexión de {addr}\n")
                self.chat_display.config(state='disabled')
                print(f"Nueva conexión aceptada de {addr}")
                
                # Iniciar un nuevo hilo para manejar al cliente
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
                client_thread.daemon = True
                client_thread.start()
            except Exception as e:
                print(f"Error aceptando conexión: {e}")
                break

    def handle_client(self, client_socket):
        client_socket.settimeout(10.0)  # Timeout de 10 segundos
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
        """Actualiza el display de chat de manera segura desde cualquier hilo"""
        self.root.after(0, self._update_chat_display, message)

    def _update_chat_display(self, message):
        """Implementación real de la actualización del display"""
        self.chat_display.config(state='normal')
        self.chat_display.insert(tk.END, f"{message}\n")
        self.chat_display.see(tk.END)
        self.chat_display.config(state='disabled')

    def broadcast(self, message, sender_socket=None):
        """Envía un mensaje a todos los clientes excepto al remitente"""
        self.update_chat_display(f"Broadcast: {message}")
        
        for client in self.clients:
            if client != sender_socket:
                try:
                    client.send(message.encode('utf-8'))
                except:
                    client.close()
                    self.clients.remove(client)

    def send_message_thread(self):
        """Envía un mensaje desde la interfaz del servidor"""
        message = self.message_entry.get().strip()
        if message:
            self.broadcast(message + "\n")
            self.message_entry.delete(0, tk.END)

    def close_server(self):
        """Cierra el servidor y todas las conexiones"""
        try:
            # Cerrar todas las conexiones de clientes
            for client in self.clients:
                try:
                    client.close()
                except:
                    pass
            # Cerrar el socket del servidor
            self.server_socket.close()
            print("Servidor cerrado correctamente")
        except Exception as e:
            print(f"Error al cerrar el servidor: {e}")
        finally:
            # Cerrar la ventana
            self.root.quit()
            self.root.destroy()

if __name__ == "__main__":
    try:
        servidor = ChatServer()
    except Exception as e:
        print(f"Error al iniciar el servidor: {e}")