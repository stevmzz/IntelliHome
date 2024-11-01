package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private TextView textViewChat;
    private Socket socket;
    private PrintWriter out;
    private Scanner in;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prueba);

        // Referencia a los EditText y ImageButton
        EditText passwordEditText = findViewById(R.id.password);
        EditText confirmPasswordEditText = findViewById(R.id.confirmPassword);
        ImageButton showHidePasswordButton = findViewById(R.id.password);
        ImageButton showHideConfirmPasswordButton = findViewById(R.id.password2);
        Button buttonContinue = findViewById(R.id.buttonContinue);

        // Configuración del botón continuar para abrir otra actividad
        buttonContinue.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, PrincipalActivity.class); // Cambia a PrincipalActivity
            startActivity(intent);
        });

        // Listener para el botón de visibilidad de contraseña
        showHidePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    showHidePasswordButton.setImageResource(R.drawable.ojo_contra);
                } else {
                    passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    showHidePasswordButton.setImageResource(R.drawable.ojo_contra2);
                }
                isPasswordVisible = !isPasswordVisible;
                passwordEditText.setSelection(passwordEditText.length());
            }
        });

        // Listener para el botón de visibilidad de confirmación de contraseña
        showHideConfirmPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfirmPasswordVisible) {
                    confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    showHideConfirmPasswordButton.setImageResource(R.drawable.ojo_contra);
                } else {
                    confirmPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    showHideConfirmPasswordButton.setImageResource(R.drawable.ojo_contra2);
                }
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                confirmPasswordEditText.setSelection(confirmPasswordEditText.length());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
