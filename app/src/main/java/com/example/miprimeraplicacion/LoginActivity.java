package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private void saveUserType(String userType) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_type", userType);
        editor.apply();
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
                    String[] parts = response.split(":");
                    if (parts[0].equals("SUCCESS")) {
                        String userType = parts.length > 1 ? parts[1].trim() : "";
                        Log.d(TAG, "Tipo de usuario recibido: " + userType);

                        if (userType.isEmpty()) {
                            Log.e(TAG, "Error: Tipo de usuario vacío en la respuesta");
                            Toast.makeText(LoginActivity.this, "Error: No se pudo determinar el tipo de usuario", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Guardar el tipo de usuario
                        saveUserType(userType);
                        Log.d(TAG, "Tipo de usuario guardado en SharedPreferences: " + userType);

                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso como " + userType, Toast.LENGTH_SHORT).show();

                        // Redirigir según el tipo de usuario
                        Intent intent;
                        if ("alquilador".equals(userType)) {
                            intent = new Intent(LoginActivity.this, AlquiladorActivity.class);
                        } else if ("arrendador".equals(userType)) {
                            intent = new Intent(LoginActivity.this, ArrendadorActivity.class);
                        } else {
                            Log.e(TAG, "Tipo de usuario no reconocido: " + userType);
                            Toast.makeText(LoginActivity.this, "Error: Tipo de usuario no válido", Toast.LENGTH_LONG).show();
                            return;
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = parts.length > 1 ? parts[1] : "Error desconocido";
                        Log.d(TAG, "Login fallido: " + errorMessage);
                        if ("INVALID_CREDENTIALS".equals(errorMessage)) {
                            Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error en el inicio de sesión: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
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