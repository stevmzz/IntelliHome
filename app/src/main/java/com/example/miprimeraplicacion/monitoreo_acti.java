package com.example.miprimeraplicacion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class monitoreo_acti extends AppCompatActivity {

    // Array para almacenar el estado actual de cada botón (true: bombo_on, false: bombo_off)
    private boolean[] buttonStates = new boolean[8];
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitoreo);

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

        // Inicializar y configurar el botón "Salir"
        exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Terminar la actividad actual y volver a la pantalla anterior
                finish();
            }
        });
    }

    // Método para configurar el comportamiento de cada botón
    private void setupButtonToggle(int buttonId, final int index) {
        final ImageButton button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Alternar el estado del botón
                buttonStates[index] = !buttonStates[index];
                // Cambiar la imagen según el estado
                if (buttonStates[index]) {
                    button.setImageResource(R.drawable.bombo_on);
                } else {
                    button.setImageResource(R.drawable.bombo_off);
                }
            }
        });
    }
}
