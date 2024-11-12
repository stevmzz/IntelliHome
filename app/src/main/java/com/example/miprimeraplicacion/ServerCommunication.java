package com.example.miprimeraplicacion;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ServerCommunication {
    private static final String TAG = "ServerCommunication";
    private static final String SERVER_IP = "192.168.100.45";
    private static final int SERVER_PORT = 1717;
    private static final int SOCKET_TIMEOUT = 5000;
    private static final int RECONNECT_DELAY = 5000;

    private static WebSocket webSocket;
    private static com.example.miprimeraplicacion.ServerCommunication.WebSocketListener currentListener;
    private static boolean isConnecting = false;
    private static final Object lock = new Object();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public interface ServerResponseListener {
        void onResponse(String response);
        void onError(String error);
    }

    public interface WebSocketListener {
        void onMessage(String message);
        void onFailure(Throwable t);
        void onClosed();
    }

    public static synchronized void initializeWebSocket(WebSocketListener listener) {
        synchronized (lock) {
            if (isConnecting) {
                Log.d(TAG, "Connection already in progress");
                return;
            }
            isConnecting = true;
        }

        Log.d(TAG, "Initializing WebSocket connection");
        currentListener = listener;

        if (webSocket != null) {
            Log.d(TAG, "Closing existing WebSocket connection");
            webSocket.close(1000, "New connection requested");
            webSocket = null;
        }

        String wsUrl = "ws://" + SERVER_IP + ":8765";
        Log.d(TAG, "Connecting to WebSocket URL: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = client.newWebSocket(request, new okhttp3.WebSocketListener() {
            @Override
            public void onOpen(WebSocket socket, okhttp3.Response response) {
                Log.d(TAG, "WebSocket connection opened successfully");
                synchronized (lock) {
                    isConnecting = false;
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "WebSocket message received: " + text);
                if (currentListener != null) {
                    currentListener.onMessage(text);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                t.printStackTrace();

                synchronized (lock) {
                    isConnecting = false;
                }

                if (currentListener != null) {
                    currentListener.onFailure(t);
                }

                // Programar reconexión automática
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (currentListener != null && !isConnecting) {
                        Log.d(TAG, "Attempting automatic reconnection...");
                        initializeWebSocket(currentListener);
                    }
                }, RECONNECT_DELAY);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                synchronized (lock) {
                    isConnecting = false;
                }

                if (currentListener != null) {
                    currentListener.onClosed();
                }
            }
        });
    }

    public static void sendToServer(final String message, final ServerResponseListener listener) {
        new AsyncTask<Void, Void, CommunicationResult>() {
            @Override
            protected CommunicationResult doInBackground(Void... voids) {
                Socket socket = null;
                PrintWriter out = null;
                BufferedReader in = null;

                try {
                    Log.d(TAG, "Iniciando conexión con servidor: " + SERVER_IP + ":" + SERVER_PORT);

                    socket = new Socket();
                    socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), SOCKET_TIMEOUT);
                    socket.setSoTimeout(SOCKET_TIMEOUT);

                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    Log.d(TAG, "Enviando mensaje: " + message);
                    out.println(message);
                    out.flush();

                    Log.d(TAG, "Esperando respuesta...");
                    String response = in.readLine();

                    if (response == null) {
                        Log.e(TAG, "No se recibió respuesta del servidor");
                        return new CommunicationResult(false, "No se recibió respuesta del servidor");
                    }

                    Log.d(TAG, "Respuesta recibida: " + response);
                    return new CommunicationResult(true, response);

                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Timeout de conexión: " + e.getMessage());
                    return new CommunicationResult(false, "Tiempo de espera agotado - Verifica la conexión");

                } catch (Exception e) {
                    Log.e(TAG, "Error de comunicación: " + e.getMessage());
                    e.printStackTrace();
                    return new CommunicationResult(false, e.getMessage());

                } finally {
                    closeQuietly(in, out, socket);
                }
            }

            @Override
            protected void onPostExecute(CommunicationResult result) {
                if (listener != null) {
                    if (result.isSuccess) {
                        listener.onResponse(result.message);
                    } else {
                        listener.onError(result.message);
                    }
                }
            }
        }.execute();
    }

    public static boolean isWebSocketConnected() {
        synchronized (lock) {
            return webSocket != null && !isConnecting;
        }
    }

    public static void reconnectWebSocket(WebSocketListener listener) {
        Log.d(TAG, "Manual WebSocket reconnection requested");
        closeWebSocket();
        initializeWebSocket(listener);
    }

    public static void closeWebSocket() {
        synchronized (lock) {
            if (webSocket != null) {
                Log.d(TAG, "Closing WebSocket connection");
                try {
                    webSocket.close(1000, "Closing connection");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing WebSocket: " + e.getMessage());
                }
                webSocket = null;
            }
            isConnecting = false;
            currentListener = null;
        }
    }

    private static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error cerrando recurso: " + e.getMessage());
                }
            }
        }
    }

    private static class CommunicationResult {
        final boolean isSuccess;
        final String message;

        CommunicationResult(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }
    }

    // Método auxiliar para verificar la conexión
    public static void checkConnection(final ServerResponseListener listener) {
        sendToServer("PING", new ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                if (response.equals("PONG")) {
                    listener.onResponse("Conexión establecida");
                } else {
                    listener.onError("Respuesta inválida del servidor");
                }
            }

            @Override
            public void onError(String error) {
                listener.onError("Error de conexión: " + error);
            }
        });
    }
}