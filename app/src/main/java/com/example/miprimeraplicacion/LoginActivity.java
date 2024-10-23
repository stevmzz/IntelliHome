package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText usernameEditText;
    private EditText passwordEditText;
    private ImageButton showHidePasswordButton;
    private Button loginButton;
    private Button forgotPasswordButton;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password2);
        showHidePasswordButton = findViewById(R.id.showHidePasswordButton);
        loginButton = findViewById(R.id.buttonContinue2);
        forgotPasswordButton = findViewById(R.id.buttonForgotPassword2);
    }

    private void setupListeners() {
        // Añadir TextWatcher para convertir usuario a minúsculas mientras escribe
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (!text.equals(text.toLowerCase())) {
                    usernameEditText.setText(text.toLowerCase());
                    usernameEditText.setSelection(text.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        showHidePasswordButton.setOnClickListener(v -> togglePasswordVisibility());
        loginButton.setOnClickListener(v -> handleLogin());
        forgotPasswordButton.setOnClickListener(v -> handleForgotPassword());
    }

    private void togglePasswordVisibility() {
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

    private void handleLogin() {
        Log.d(TAG, "Iniciando proceso de login");

        // Ocultar teclado
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Asegurar que el usuario esté en minúsculas
        String username = usernameEditText.getText().toString().trim().toLowerCase();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Campos de usuario o contraseña vacíos");
            return;
        }

        String loginData = String.format("LOGIN:%s,%s", username, password);
        Log.d(TAG, "Enviando datos de login al servidor: " + loginData);

        ServerCommunication.sendToServer(loginData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Respuesta del servidor recibida: " + response);
                runOnUiThread(() -> {
                    if ("SUCCESS".equals(response)) {
                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Login exitoso, iniciando ExitActivity");

                        Intent intent = new Intent(LoginActivity.this, AlquiladorActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Login fallido: " + response);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error en la comunicación con el servidor: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error de conexión: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleForgotPassword() {
        Toast.makeText(this, "Funcionalidad de recuperación de contraseña no implementada", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Intento de recuperación de contraseña (no implementado)");
    }
}