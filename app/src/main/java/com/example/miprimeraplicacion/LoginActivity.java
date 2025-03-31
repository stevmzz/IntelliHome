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
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout passwordLayout;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
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
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.buttonContinue2);
        forgotPasswordButton = findViewById(R.id.buttonForgotPassword2);

        // Configurar el icono de mostrar/ocultar contraseña
        passwordLayout.setEndIconOnClickListener(v -> togglePasswordVisibility());
    }

    private void setupListeners() {
        // Convertir usuario a minúsculas mientras escribe
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

        loginButton.setOnClickListener(v -> handleLogin());
        forgotPasswordButton.setOnClickListener(v -> handleForgotPassword());

        // Validación en tiempo real
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordLayout.setError(null); // Limpiar error al escribir
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordLayout.setEndIconDrawable(R.drawable.ojo_contra);
        } else {
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordLayout.setEndIconDrawable(R.drawable.ojo_contra2);
        }
        isPasswordVisible = !isPasswordVisible;
        passwordEditText.setSelection(passwordEditText.length());
    }

    private void saveUserType(String userType) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_type", userType);
        editor.putString("username", usernameEditText.getText().toString().trim().toLowerCase());
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

        // Validación de campos
        String username = usernameEditText.getText().toString().trim().toLowerCase();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            ((TextInputLayout) usernameEditText.getParent().getParent()).setError("Ingrese un usuario");
            return;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Ingrese una contraseña");
            return;
        }

        // Mostrar progreso
        loginButton.setEnabled(false);
        loginButton.setText("Iniciando sesión...");

        String loginData = String.format("LOGIN:%s,%s", username, password);
        Log.d(TAG, "Enviando datos de login al servidor: " + loginData);

        ServerCommunication.sendToServer(loginData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Respuesta del servidor recibida: " + response);
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Continue");

                    String[] parts = response.split(":");
                    if (parts[0].equals("SUCCESS")) {
                        String userType = parts.length > 1 ? parts[1].trim() : "";
                        Log.d(TAG, "Tipo de usuario recibido: " + userType);

                        if (userType.isEmpty()) {
                            Log.e(TAG, "Error: Tipo de usuario vacío en la respuesta");
                            passwordLayout.setError("Error: No se pudo determinar el tipo de usuario");
                            return;
                        }

                        saveUserType(userType);
                        Log.d(TAG, "Tipo de usuario guardado en SharedPreferences: " + userType);

                        // Redirigir según el tipo de usuario
                        Intent intent;
                        if ("alquilador".equals(userType)) {
                            intent = new Intent(LoginActivity.this, AlquiladorActivity.class);
                        } else if ("arrendador".equals(userType)) {
                            intent = new Intent(LoginActivity.this, ArrendadorActivity.class);
                        } else {
                            Log.e(TAG, "Tipo de usuario no reconocido: " + userType);
                            passwordLayout.setError("Error: Tipo de usuario no válido");
                            return;
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = parts.length > 1 ? parts[1] : "Error desconocido";
                        Log.d(TAG, "Login fallido: " + errorMessage);
                        if ("INVALID_CREDENTIALS".equals(errorMessage)) {
                            passwordLayout.setError("Usuario o contraseña incorrectos");
                        } else {
                            passwordLayout.setError("Error en el inicio de sesión: " + errorMessage);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error en la comunicación con el servidor: " + error);
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Continue");
                    passwordLayout.setError("Error de conexión: " + error);
                });
            }
        });
    }

    private void handleForgotPassword() {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
}