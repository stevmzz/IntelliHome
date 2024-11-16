package com.example.miprimeraplicacion;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class MonitoreoActivity extends AppCompatActivity implements ServerCommunication.WebSocketListener {
    private static final String TAG = "MonitoreoActivity";

    // Constants
    private static final String LED_ON_COMMAND = "LED_ON:";
    private static final String LED_OFF_COMMAND = "LED_OFF:";
    private static final String DOOR_OPEN_COMMAND = "DOOR_OPEN:";
    private static final String DOOR_CLOSE_COMMAND = "DOOR_CLOSE:";
    private static final long[] FIRE_VIBRATION_PATTERN = {0, 500, 200, 500};

    // UI Components
    private ImageView imageLogo;
    private Button humedadButton;
    private Button exitButton;
    private CardView logoCard;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    // State variables
    private boolean[] buttonStates = new boolean[8];
    private boolean[] doorStates = new boolean[6];
    private boolean isFireDetected = false;
    private String currentUsername;
    private Executor executor;

    // System services
    private Vibrator vibrator;

    // Handler for UI updates
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private MaterialAlertDialogBuilder currentDialogBuilder;
    private androidx.appcompat.app.AlertDialog currentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoreo);

        initializeViews();
        setupVibrator();
        setupBiometricAuth();
        setupWebSocket();
        checkInitialFireStatus();
    }

    private void initializeViews() {
        // Get stored preferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "");
        if (currentUsername.isEmpty()) {
            showError("Error: No se encontró usuario");
            finish();
            return;
        }

        // Initialize UI components
        imageLogo = findViewById(R.id.imageLogo);
        logoCard = findViewById(R.id.logoCard);
        humedadButton = findViewById(R.id.humedad);
        exitButton = findViewById(R.id.exitButton);

        // Initialize executor for biometric operations
        executor = ContextCompat.getMainExecutor(this);

        // Initialize states
        Arrays.fill(buttonStates, false);
        Arrays.fill(doorStates, false);

        // Setup controls
        setupButtonControls();
        setupDoorControls();
        setupListeners();
    }

    private void setupVibrator() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager vibratorManager =
                    (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void setupListeners() {
        humedadButton.setOnClickListener(v -> requestDHTData());
        exitButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupButtonControls() {
        // Configurar los botones de luces
        setupButtonToggle(R.id.button1, 0);
        setupButtonToggle(R.id.button2, 1);
        setupButtonToggle(R.id.button3, 2);
        setupButtonToggle(R.id.button4, 3);
        setupButtonToggle(R.id.button5, 4);
        setupButtonToggle(R.id.button6, 5);
        setupButtonToggle(R.id.button7, 6);
        setupButtonToggle(R.id.button8, 7);
    }

    private void setupDoorControls() {
        // Configurar las puertas
        setupDoorToggle(R.id.doorGarage, 0, "GARAGE");
        setupDoorToggle(R.id.doorMain, 1, "MAIN");
        setupDoorToggle(R.id.doorKitchen, 2, "KITCHEN");
        setupDoorToggle(R.id.doorLiving, 3, "LIVING");
        setupDoorToggle(R.id.doorBedroom1, 4, "BEDROOM1");
        setupDoorToggle(R.id.doorBedroom2, 5, "BEDROOM2");
    }

    private void setupButtonToggle(int buttonId, final int index) {
        final ImageButton button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            buttonStates[index] = !buttonStates[index];
            updateButtonUI(button, buttonStates[index]);
            sendLedCommand(index, buttonStates[index]);
        });
    }

    private void updateButtonUI(ImageButton button, boolean isOn) {
        button.setImageResource(isOn ? R.drawable.bombo_on : R.drawable.bombo_off);
    }

    private void sendLedCommand(int index, boolean isOn) {
        final String command = isOn ? LED_ON_COMMAND + index : LED_OFF_COMMAND + index;

        ServerCommunication.sendToServer(command, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                if (!response.startsWith("SUCCESS")) {
                    uiHandler.post(() -> {
                        buttonStates[index] = !buttonStates[index];
                        ImageButton button = findViewById(getButtonId(index));
                        updateButtonUI(button, buttonStates[index]);
                        showError("Error al controlar el LED");
                    });
                }
            }

            @Override
            public void onError(String error) {
                uiHandler.post(() -> {
                    buttonStates[index] = !buttonStates[index];
                    ImageButton button = findViewById(getButtonId(index));
                    updateButtonUI(button, buttonStates[index]);
                    showError("Error: " + error);
                });
            }
        });
    }

    private void setupDoorToggle(int doorId, final int index, final String doorName) {
        final ImageButton doorButton = findViewById(doorId);
        doorButton.setOnClickListener(v ->
                showBiometricPrompt(doorName, new BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        handleDoorOperation(doorButton, index, doorName);
                    }

                    @Override
                    public void onError(String error) {
                        showError("Autenticación requerida para operar las puertas");
                    }

                    @Override
                    public void onFailed() {
                        showError("Fallo en la autenticación biométrica");
                    }
                })
        );
    }

    private void setupBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS) {

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verificación biométrica")
                    .setSubtitle("Use su huella digital para verificar su identidad")
                    .setNegativeButtonText("Cancelar")
                    .build();

            biometricPrompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            showError(errString.toString());
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            showError("Autenticación fallida");
                        }
                    });
        }
    }

    private void setupWebSocket() {
        ServerCommunication.initializeWebSocket(this);
    }

    private void checkInitialFireStatus() {
        Log.d(TAG, "Checking initial fire status");
        ServerCommunication.sendToServer("CHECK_FIRE", new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Initial fire status response: " + response);
                if (response.startsWith("SUCCESS:")) {
                    boolean fireDetected = response.contains("FIRE_DETECTED");
                    Log.d(TAG, "Initial fire detection status: " + (fireDetected ? "DETECTED" : "NOT DETECTED"));
                    uiHandler.post(() -> handleFireStatus(fireDetected));
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking fire status: " + error);
            }
        });
    }

    private void handleFireStatus(boolean fireDetected) {
        Log.d(TAG, "Handling fire status update: " + (fireDetected ? "DETECTED" : "NOT DETECTED"));

        // Ejecutar en el hilo principal
        uiHandler.post(() -> {
            try {
                // Actualizar el estado interno primero
                boolean stateChanged = fireDetected != isFireDetected;
                isFireDetected = fireDetected;

                // Actualizar UI solo si hubo cambio
                if (stateChanged) {
                    Log.d(TAG, "Fire status changed - updating UI");

                    // Actualizar imagen
                    if (imageLogo != null) {
                        imageLogo.setImageResource(fireDetected ? R.drawable.fuego : R.drawable.logo);
                    }

                    // Actualizar fondo
                    if (logoCard != null) {
                        logoCard.setCardBackgroundColor(ContextCompat.getColor(this,
                                fireDetected ? R.color.error_light : R.color.white));
                    }

                    // Manejar alertas
                    if (fireDetected) {
                        startFireAlert();
                    } else {
                        stopFireAlert();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI for fire status: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void startFireAlert() {
        Log.d(TAG, "Starting fire alert");

        // Detener alerta anterior si existe
        stopFireAlert();

        // Iniciar vibración
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(FIRE_VIBRATION_PATTERN, 0));
            } else {
                vibrator.vibrate(FIRE_VIBRATION_PATTERN, 0);
            }
        }

        // Crear y mostrar diálogo de alerta
        currentDialogBuilder = new MaterialAlertDialogBuilder(this, R.style.AlertDialog_Fire)
                .setTitle("¡ALERTA DE FUEGO!")
                .setMessage("Se ha detectado fuego en la propiedad")
                .setIcon(R.drawable.ic_warning)
                .setCancelable(false)
                .setPositiveButton("Entendido", (dialog, which) -> {
                    // No hacer nada especial al presionar el botón
                });

        // Crear y mostrar el diálogo
        try {
            currentDialog = currentDialogBuilder.create();
            currentDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing fire alert dialog: " + e.getMessage());
        }
    }

    private void stopFireAlert() {
        Log.d(TAG, "Stopping fire alert");

        // Detener vibración
        if (vibrator != null) {
            vibrator.cancel();
        }

        // Cerrar diálogo si existe
        try {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing fire alert dialog: " + e.getMessage());
        } finally {
            currentDialog = null;
            currentDialogBuilder = null;
        }
    }

    private void handleDoorOperation(ImageButton doorButton, int index, String doorName) {
        doorStates[index] = !doorStates[index];
        String command = doorStates[index] ?
                DOOR_OPEN_COMMAND + doorName :
                DOOR_CLOSE_COMMAND + doorName;

        doorButton.setImageResource(doorStates[index] ?
                R.drawable.door_open : R.drawable.door_closed);

        ServerCommunication.sendToServer(command, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                if (!response.startsWith("SUCCESS")) {
                    uiHandler.post(() -> {
                        doorStates[index] = !doorStates[index];
                        doorButton.setImageResource(doorStates[index] ?
                                R.drawable.door_open : R.drawable.door_closed);
                        showError("Error al controlar la puerta");
                    });
                }
            }

            @Override
            public void onError(String error) {
                uiHandler.post(() -> {
                    doorStates[index] = !doorStates[index];
                    doorButton.setImageResource(doorStates[index] ?
                            R.drawable.door_open : R.drawable.door_closed);
                    showError("Error: " + error);
                });
            }
        });
    }

    private void requestDHTData() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Obteniendo datos del sensor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ServerCommunication.sendToServer("READ_DHT", new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                uiHandler.post(() -> {
                    progressDialog.dismiss();
                    if (response.startsWith("SUCCESS:")) {
                        String[] data = response.substring(8).split(",");
                        if (data.length == 2) {
                            try {
                                float temperatura = Float.parseFloat(data[0]);
                                float humedad = Float.parseFloat(data[1]);
                                showDHTDataDialog(temperatura, humedad);
                            } catch (NumberFormatException e) {
                                showError("Error en formato de datos");
                            }
                        } else {
                            showError("Datos incompletos del sensor");
                        }
                    } else {
                        showError("Error al leer sensor");
                    }
                });
            }

            @Override
            public void onError(String error) {
                uiHandler.post(() -> {
                    progressDialog.dismiss();
                    showError("Error: " + error);
                });
            }
        });
    }

    private void showDHTDataDialog(float temperatura, float humedad) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_dht_data, null);
        TextView tempTextView = dialogView.findViewById(R.id.temperatureValue);
        TextView humTextView = dialogView.findViewById(R.id.humidityValue);

        tempTextView.setText(String.format("%.1f°C", temperatura));
        humTextView.setText(String.format("%.1f%%", humedad));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Datos Ambientales")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void showBiometricPrompt(String doorName, BiometricCallback callback) {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verificación requerida")
                    .setSubtitle("Use su huella digital para operar la puerta " + doorName)
                    .setNegativeButtonText("Cancelar")
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            callback.onSuccess();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError(errString.toString());
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            callback.onFailed();
                        }
                    });

            biometricPrompt.authenticate(promptInfo);
        } else {
            handleBiometricError(canAuthenticate, callback);
        }
    }

    private void handleBiometricError(int errorCode, BiometricCallback callback) {
        String message;
        switch (errorCode) {
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                message = "Este dispositivo no soporta autenticación biométrica";
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                message = "La autenticación biométrica no está disponible en este momento";
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                message = "No hay huellas digitales registradas en el dispositivo";
                break;
            default:
                message = "Error al iniciar la autenticación biométrica";
                break;
        }
        callback.onError(message);
    }

    private int getButtonId(int index) {
        int[] ids = {
                R.id.button1, R.id.button2, R.id.button3, R.id.button4,
                R.id.button5, R.id.button6, R.id.button7, R.id.button8
        };
        return ids[index];
    }

    // WebSocket callbacks
    @Override
    public void onMessage(String message) {
        Log.d(TAG, "WebSocket message received: " + message);
        try {
            JSONObject json = new JSONObject(message);
            String messageType = json.getString("type");
            String status = json.getString("status");

            Log.d(TAG, "Message type: " + messageType + ", status: " + status);

            if ("fire_status".equals(messageType)) {
                boolean fireDetected = "FIRE_DETECTED".equals(status);
                Log.d(TAG, "Fire status updated: " + (fireDetected ? "DETECTED" : "NOT DETECTED"));

                uiHandler.post(() -> {
                    try {
                        handleFireStatus(fireDetected);
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling fire status: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onFailure(Throwable t) {
        Log.e(TAG, "WebSocket error: " + t.getMessage());
        uiHandler.postDelayed(() -> {
            if (!isFinishing()) {
                ServerCommunication.reconnectWebSocket(this);
            }
        }, 5000);
    }

    @Override
    public void onClosed() {
        Log.d(TAG, "WebSocket connection closed");
    }

    private void showError(String message) {
        if (!isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    // Lifecycle methods
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ServerCommunication.closeWebSocket();
        stopFireAlert();
        uiHandler.removeCallbacksAndMessages(null);

        if (biometricPrompt != null) {
            biometricPrompt.cancelAuthentication();
            biometricPrompt = null;
        }

        executor = null;
        vibrator = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopFireAlert();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Verify and reconnect WebSocket if needed
        if (!ServerCommunication.isWebSocketConnected()) {
            Log.d(TAG, "WebSocket not connected, reconnecting...");
            ServerCommunication.reconnectWebSocket(this);
        } else {
            Log.d(TAG, "WebSocket already connected");
        }

        // Check initial fire status
        checkInitialFireStatus();
    }

    @Override
    public void onBackPressed() {
        if (isFireDetected) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("¡Alerta Activa!")
                    .setMessage("Hay una alerta de fuego activa. ¿Seguro que desea salir?")
                    .setPositiveButton("Sí", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // Interface for biometric authentication callbacks
    private interface BiometricCallback {
        void onSuccess();
        void onError(String error);
        void onFailed();
    }
}