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
        self.users = {}  # Diccionario para almacenar los usuarios registrados

        # Configuración de la interfaz gráfica 
        self.root = tk.Tk()
        self.root.title("Servidor de Chat")

        self.chat_display = scrolledtext.ScrolledText(self.root, state='disabled', width=50, height=20)
        self.chat_display.pack(pady=10)

        self.message_entry = tk.Entry(self.root, width=40)
        self.message_entry.pack(pady=5)

        self.send_button = tk.Button(self.root, text="Enviar", command=self.send_message_thread)
        self.send_button.pack(pady=5)

        self.quit_button = tk.Button(self.root, text="Salir", command=self.close_server)
        self.quit_button.pack(pady=5)

        # Hilo para manejar el servidor con el fin de que sea en hilos separados
        self.thread = threading.Thread(target=self.accept_connections)
        self.thread.start()

        self.root.protocol("WM_DELETE_WINDOW", self.close_server)
        self.root.mainloop()

    def accept_connections(self):
        while True: # Este while es para siempre escuchar nuevos clientes
            client_socket, addr = self.server_socket.accept()
            self.clients.append(client_socket)
            self.chat_display.config(state='normal')
            self.chat_display.insert(tk.END, f"Conexión de {addr}\n")
            self.chat_display.config(state='disabled')
            threading.Thread(target=self.handle_client, args=(client_socket,)).start() # Para que sea en hilo separado

    def handle_client(self, client_socket):
        while True:
            try:
                message = client_socket.recv(1024).decode('utf-8')
                print(f"Mensaje recibido: {message}")  # Línea de depuración
                if message:
                    if message.startswith("REGISTER:"):
                        self.handle_registration(message[9:], client_socket)
                    elif message.startswith("LOGIN:"):
                        self.handle_login(message[6:], client_socket)
                    else:
                        self.broadcast(message, client_socket)
                else:
                    break
            except Exception as e:
                print(f"Error manejando cliente: {e}")  # Línea de depuración
                break
        client_socket.close()
        self.clients.remove(client_socket)


    def handle_registration(self, data, client_socket):
        user_data = data.split(',')
        username = user_data[1]
        password = user_data[3]

        if username in self.users:
            client_socket.send("ERROR: Usuario ya existe".encode('utf-8'))
        else:
            self.users[username] = {'password': password, 'data': user_data}
            client_socket.send("SUCCESS".encode('utf-8'))
            print(f"Nuevo usuario registrado: {username} con contraseña {password}")
            print(f"Usuarios actuales: {self.users}")  # Línea de depuración
            self.chat_display.config(state='normal')
            self.chat_display.insert(tk.END, f"Nuevo usuario registrado: {username}\n")
            self.chat_display.config(state='disabled')


    def handle_login(self, data, client_socket):
        try:
            username, password = data.split(',')
            print(f"Usuarios registrados: {self.users}")  # Línea de depuración
            print(f"Intentando iniciar sesión con usuario: {username} y contraseña: {password}")
            if username in self.users:
                print(f"Usuario encontrado: {username}")
                if self.users[username]['password'] == password:
                    client_socket.send("SUCCESS".encode('utf-8'))
                    self.chat_display.config(state='normal')
                    self.chat_display.insert(tk.END, f"Usuario conectado: {username}\n")
                    self.chat_display.config(state='disabled')
                else:
                    print(f"Contraseña incorrecta para usuario: {username}")
                    client_socket.send("ERROR: Usuario o contraseña incorrectos".encode('utf-8'))
            else:
                print(f"Usuario no encontrado: {username}")
                client_socket.send("ERROR: Usuario o contraseña incorrectos".encode('utf-8'))
        except Exception as e:
            print(f"Error en handle_login: {e}")
            client_socket.send("ERROR: Excepción al manejar el login".encode('utf-8'))




    def broadcast(self, message, sender_socket):
        self.chat_display.config(state='normal')
        self.chat_display.insert(tk.END, f"Cliente: {message}\n")
        self.chat_display.config(state='disabled')

        for client in self.clients: # para cada cliente que haya
            if client != sender_socket:  # No enviar al remitente
                try:
                    client.send(message.encode('utf-8')) # envía el mensaje
                except:
                    client.close()
                    self.clients.remove(client)

    def broadcast1(self, message, sender_socket): # Esto es para que sirva el boton
        self.chat_display.config(state='normal')
        self.chat_display.insert(tk.END, f"Servidor: {message}\n")
        self.chat_display.config(state='disabled')
        
        for client in self.clients:
            try:
                client.send(message.encode('utf-8'))
            except:
                client.close()
                self.clients.remove(client)

    def send_message_thread(self):
        #Se debe agregar \n para que termine la cadena que se requiere enviar
        threading.Thread(target=self.broadcast1(self.message_entry.get()+"\n",None)).start()
        self.message_entry.delete(0, tk.END)  # Limpiar la entrada

    def close_server(self):
        for client in self.clients:
            client.close()
        self.server_socket.close()
        self.root.destroy()

if __name__ == "__main__":
    ChatServer()