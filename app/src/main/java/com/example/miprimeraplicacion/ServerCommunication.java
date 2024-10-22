package com.example.miprimeraplicacion;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerCommunication {
    private static final String TAG = "ServerCommunication";
    private static final String SERVER_IP = "192.168.100.6"; // Asegúrate de que esta IP sea correcta
    private static final int SERVER_PORT = 1717;
    private static final int SOCKET_TIMEOUT = 5000; // 5 segundos de timeout

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
                    Log.d(TAG, "Intentando conectar al servidor: " + SERVER_IP + ":" + SERVER_PORT);
                    socket = new Socket(SERVER_IP, SERVER_PORT);
                    socket.setSoTimeout(SOCKET_TIMEOUT);

                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    Log.d(TAG, "Enviando mensaje al servidor: " + message);
                    out.println(message);

                    String response = in.readLine();
                    Log.d(TAG, "Respuesta recibida del servidor: " + response);

                    return response;
                } catch (Exception e) {
                    Log.e(TAG, "Error de comunicación: " + e.getMessage(), e);
                    return "Error: " + e.getMessage();
                } finally {
                    try {
                        if (out != null) out.close();
                        if (in != null) in.close();
                        if (socket != null) socket.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al cerrar la conexión: " + e.getMessage(), e);
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error:")) {
                    Log.e(TAG, "Error en la comunicación: " + result);
                    listener.onError(result);
                } else {
                    Log.d(TAG, "Comunicación exitosa, respuesta: " + result);
                    listener.onResponse(result);
                }
            }
        }.execute();
    }
}