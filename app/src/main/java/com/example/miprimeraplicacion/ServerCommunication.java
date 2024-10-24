package com.example.miprimeraplicacion;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerCommunication {
    private static final String TAG = "ServerCommunication";
    private static final String SERVER_IP = "192.168.100.6";
    private static final int SERVER_PORT = 1717;
    private static final int SOCKET_TIMEOUT = 10000; // 10 segundos de timeout

    public interface ServerResponseListener {
        void onResponse(String response);
        void onError(String error);
    }

    public static void sendToServer(final String message, final ServerResponseListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Socket socket = null;
                PrintWriter out = null;
                BufferedReader in = null;

                try {
                    // Log de inicio de conexión
                    Log.d(TAG, "Iniciando conexión con servidor: " + SERVER_IP + ":" + SERVER_PORT);

                    // Crear y configurar el socket con timeout
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), SOCKET_TIMEOUT);
                    socket.setSoTimeout(SOCKET_TIMEOUT); // Timeout para lectura

                    // Configurar streams de entrada/salida
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Enviar mensaje
                    Log.d(TAG, "Enviando mensaje: " + message);
                    out.println(message);
                    out.flush(); // Asegurar envío inmediato

                    // Esperar y leer respuesta
                    Log.d(TAG, "Esperando respuesta del servidor...");
                    String response = in.readLine();

                    if (response == null) {
                        Log.e(TAG, "Respuesta nula recibida del servidor");
                        return "Error: No se recibió respuesta del servidor";
                    }

                    Log.d(TAG, "Respuesta recibida: " + response);
                    return response;

                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Timeout de conexión: " + e.getMessage());
                    return "Error: Tiempo de espera agotado - Verifica la conexión";

                } catch (Exception e) {
                    Log.e(TAG, "Error de comunicación: " + e.getMessage());
                    return "Error: " + e.getMessage();

                } finally {
                    // Cerrar todos los recursos
                    try {
                        Log.d(TAG, "Cerrando conexiones...");
                        if (out != null) {
                            out.close();
                        }
                        if (in != null) {
                            in.close();
                        }
                        if (socket != null && !socket.isClosed()) {
                            socket.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al cerrar conexiones: " + e.getMessage());
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    if (result.startsWith("Error:")) {
                        Log.e(TAG, "Error en comunicación: " + result);
                        listener.onError(result.substring(7)); // Remover "Error: " del mensaje
                    } else {
                        Log.d(TAG, "Comunicación exitosa: " + result);
                        listener.onResponse(result);
                    }
                } else {
                    Log.e(TAG, "Resultado nulo de la comunicación");
                    listener.onError("Error desconocido en la comunicación");
                }
            }
        }.execute();
    }
}