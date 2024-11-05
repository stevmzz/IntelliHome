package com.example.miprimeraplicacion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MonitoreoActivity extends AppCompatActivity {

    private boolean[] buttonStates = new boolean[8];
    private Button exitButton;

    // Constantes para los comandos de LED
    private static final String LED_ON_COMMAND = "LED_ON:";
    private static final String LED_OFF_COMMAND = "LED_OFF:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoreo);

        // Inicializa los estados de los botones a false (bombo_off)
        for (int i = 0; i < buttonStates.length; i++) {
            buttonStates[i] = false;
        }

        // Configurar los botones
        setupButtonToggle(R.id.button1, 0);
        setupButtonToggle(R.id.button2, 1);
        setupButtonToggle(R.id.button3, 2);
        setupButtonToggle(R.id.button4, 3);
        setupButtonToggle(R.id.button5, 4);
        setupButtonToggle(R.id.button6, 5);
        setupButtonToggle(R.id.button7, 6);
        setupButtonToggle(R.id.button8, 7);

        exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupButtonToggle(int buttonId, final int index) {
        final ImageButton button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Alternar el estado del botón
                buttonStates[index] = !buttonStates[index];

                // Preparar el comando para enviar al servidor
                final String command = buttonStates[index] ?
                        LED_ON_COMMAND + index :
                        LED_OFF_COMMAND + index;

                // Enviar comando al servidor
                ServerCommunication.sendToServer(command, new ServerCommunication.ServerResponseListener() {
                    @Override
                    public void onResponse(String response) {
                        if (response != null && response.startsWith("SUCCESS")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Cambiar la imagen según el estado actual del botón
                                    button.setImageResource(buttonStates[index] ?
                                            R.drawable.bombo_on :
                                            R.drawable.bombo_off);

                                    // Mostrar mensaje de éxito
                                    String status = buttonStates[index] ? "encendido" : "apagado";
                                    Toast.makeText(MonitoreoActivity.this,
                                            "LED " + (index + 1) + " " + status,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Revertir estado si hay error
                            buttonStates[index] = !buttonStates[index];
                            showError("Error al controlar el LED");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Revertir estado en caso de error
                        buttonStates[index] = !buttonStates[index];
                        showError("Error de conexión: " + error);
                    }
                });
            }
        });
    }

    private void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MonitoreoActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}