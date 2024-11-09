package com.example.miprimeraplicacion;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MonitoreoActivity extends AppCompatActivity {

    private boolean[] buttonStates = new boolean[8];
    private boolean[] doorStates = new boolean[6]; // Para las 6 puertas
    private Button exitButton;
    private String currentUsername;

    // Constantes para los comandos
    private static final String LED_ON_COMMAND = "LED_ON:";
    private static final String LED_OFF_COMMAND = "LED_OFF:";
    private static final String DOOR_OPEN_COMMAND = "DOOR_OPEN:";
    private static final String DOOR_CLOSE_COMMAND = "DOOR_CLOSE:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoreo);

        // Obtener el nombre de usuario actual
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "");

        // Inicializar estados
        for (int i = 0; i < buttonStates.length; i++) {
            buttonStates[i] = false;
        }
        for (int i = 0; i < doorStates.length; i++) {
            doorStates[i] = false;
        }

        Button humedadButton = findViewById(R.id.humedad);
        humedadButton.setOnClickListener(v -> {
            requestDHTData();
        });

        // Configurar los botones de luces
        setupButtonToggle(R.id.button1, 0);
        setupButtonToggle(R.id.button2, 1);
        setupButtonToggle(R.id.button3, 2);
        setupButtonToggle(R.id.button4, 3);
        setupButtonToggle(R.id.button5, 4);
        setupButtonToggle(R.id.button6, 5);
        setupButtonToggle(R.id.button7, 6);
        setupButtonToggle(R.id.button8, 7);

        // Configurar las puertas con funcionalidad local y servidor
        setupDoorToggle(R.id.doorGarage, 0, "GARAGE");
        setupDoorToggle(R.id.doorMain, 1, "MAIN");
        setupDoorToggle(R.id.doorKitchen, 2, "KITCHEN");
        setupDoorToggle(R.id.doorLiving, 3, "LIVING");
        setupDoorToggle(R.id.doorBedroom1, 4, "BEDROOM1");
        setupDoorToggle(R.id.doorBedroom2, 5, "BEDROOM2");

        exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());
    }

    private void requestDHTData() {
        // Mostrar diálogo de carga
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Obteniendo datos del sensor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Enviar comando al servidor
        ServerCommunication.sendToServer("READ_DHT", new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.startsWith("SUCCESS:")) {
                        String[] data = response.substring(8).split(",");
                        if (data.length == 2) {
                            float temperatura = Float.parseFloat(data[0]);
                            float humedad = Float.parseFloat(data[1]);
                            showDHTDataDialog(temperatura, humedad);
                        } else {
                            showError("Formato de datos inválido");
                        }
                    } else {
                        showError("Error al obtener datos del sensor");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Error: " + error);
                });
            }
        });
    }

    private void showDHTDataDialog(float temperatura, float humedad) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded);

        // Inflar el layout personalizado
        View view = getLayoutInflater().inflate(R.layout.dialog_dht_data, null);

        // Configurar los TextView del layout
        TextView tempTextView = view.findViewById(R.id.temperatureValue);
        TextView humTextView = view.findViewById(R.id.humidityValue);

        // Formatear los valores con un decimal
        tempTextView.setText(String.format("%.1f°C", temperatura));
        humTextView.setText(String.format("%.1f%%", humedad));

        builder.setView(view)
                .setTitle("Datos Ambientales")
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void setupButtonToggle(int buttonId, final int index) {
        final ImageButton button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            // Alternar el estado del botón y actualizar UI inmediatamente
            buttonStates[index] = !buttonStates[index];
            button.setImageResource(buttonStates[index] ?
                    R.drawable.bombo_on :
                    R.drawable.bombo_off);

            // Preparar el comando para enviar al servidor
            final String command = buttonStates[index] ?
                    LED_ON_COMMAND + index :
                    LED_OFF_COMMAND + index;

            // Enviar comando al servidor
            ServerCommunication.sendToServer(command, new ServerCommunication.ServerResponseListener() {
                @Override
                public void onResponse(String response) {
                    if (response != null && response.startsWith("SUCCESS")) {
                        runOnUiThread(() -> {
                            String status = buttonStates[index] ? "encendido" : "apagado";
                            Toast.makeText(MonitoreoActivity.this,
                                    "LED " + (index + 1) + " " + status,
                                    Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        buttonStates[index] = !buttonStates[index];
                        runOnUiThread(() -> {
                            button.setImageResource(buttonStates[index] ?
                                    R.drawable.bombo_on :
                                    R.drawable.bombo_off);
                            showError("Error al controlar el LED");
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    buttonStates[index] = !buttonStates[index];
                    runOnUiThread(() -> {
                        button.setImageResource(buttonStates[index] ?
                                R.drawable.bombo_on :
                                R.drawable.bombo_off);
                        showError("Error de conexión: " + error);
                    });
                }
            });
        });
    }

    private void setupDoorToggle(int doorId, final int index, final String doorName) {
        final ImageButton doorButton = findViewById(doorId);
        doorButton.setOnClickListener(v -> {
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
            });
        });
    }

    private void handleDoorOperation(ImageButton doorButton, int index, String doorName) {
        // Cambiar el estado de la puerta localmente
        doorStates[index] = !doorStates[index];

        // Actualizar la imagen
        doorButton.setImageResource(doorStates[index] ?
                R.drawable.door_open :
                R.drawable.door_closed);

        // Preparar comando
        final String command = doorStates[index] ?
                DOOR_OPEN_COMMAND + doorName :
                DOOR_CLOSE_COMMAND + doorName;

        // Enviar al servidor
        ServerCommunication.sendToServer(command, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                if (response != null && response.startsWith("SUCCESS")) {
                    runOnUiThread(() -> {
                        String status = doorStates[index] ? "abierta" : "cerrada";
                        Toast.makeText(MonitoreoActivity.this,
                                "Puerta " + doorName + " " + status,
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    doorStates[index] = !doorStates[index];
                    runOnUiThread(() -> {
                        doorButton.setImageResource(doorStates[index] ?
                                R.drawable.door_open :
                                R.drawable.door_closed);
                        showError("Error al controlar la puerta");
                    });
                }
            }

            @Override
            public void onError(String error) {
                doorStates[index] = !doorStates[index];
                runOnUiThread(() -> {
                    doorButton.setImageResource(doorStates[index] ?
                            R.drawable.door_open :
                            R.drawable.door_closed);
                    showError("Error de conexión: " + error);
                });
            }
        });
    }

    private void showBiometricPrompt(String doorName, BiometricCallback callback) {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Verificación requerida")
                        .setSubtitle("Use su huella digital para operar la puerta " + doorName)
                        .setNegativeButtonText("Cancelar")
                        .build();

                BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                        ContextCompat.getMainExecutor(this),
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                callback.onSuccess();
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, CharSequence errString) {
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
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                showError("Este dispositivo no soporta autenticación biométrica");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                showError("La autenticación biométrica no está disponible en este momento");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                showError("No hay huellas digitales registradas en el dispositivo");
                break;
            default:
                showError("Error al iniciar la autenticación biométrica");
                break;
        }
    }

    private void showError(final String message) {
        runOnUiThread(() ->
                Toast.makeText(MonitoreoActivity.this, message, Toast.LENGTH_SHORT).show()
        );
    }

    // Interfaz para manejar la respuesta de la autenticación biométrica
    private interface BiometricCallback {
        void onSuccess();
        void onError(String error);
        void onFailed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos si es necesario
    }
}