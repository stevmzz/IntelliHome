package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    private TextInputEditText usernameEditText;
    private TextInputEditText securityAnswerEditText;
    private TextInputEditText newPasswordEditText;
    private TextInputEditText confirmNewPasswordEditText;
    private TextInputLayout newPasswordLayout;
    private TextInputLayout confirmNewPasswordLayout;
    private MaterialButton resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password_activity);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.username);
        securityAnswerEditText = findViewById(R.id.securityAnswer);
        newPasswordEditText = findViewById(R.id.newPassword);
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPassword);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmNewPasswordLayout = findViewById(R.id.confirmNewPasswordLayout);
        resetButton = findViewById(R.id.buttonReset);
    }

    private void setupListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        usernameEditText.addTextChangedListener(textWatcher);
        securityAnswerEditText.addTextChangedListener(textWatcher);
        newPasswordEditText.addTextChangedListener(textWatcher);
        confirmNewPasswordEditText.addTextChangedListener(textWatcher);

        resetButton.setOnClickListener(v -> handlePasswordReset());
    }

    private void validateFields() {
        boolean isValid = !usernameEditText.getText().toString().trim().isEmpty() &&
                !securityAnswerEditText.getText().toString().trim().isEmpty() &&
                validatePassword() &&
                validateConfirmPassword();

        resetButton.setEnabled(isValid);
    }

    private boolean validatePassword() {
        String password = newPasswordEditText.getText().toString().trim();
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            newPasswordLayout.setError("Password must contain at least 8 characters, including uppercase, lowercase, number and special character");
            return false;
        }
        newPasswordLayout.setError(null);
        return true;
    }

    private boolean validateConfirmPassword() {
        String password = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmNewPasswordEditText.getText().toString().trim();
        if (!password.equals(confirmPassword)) {
            confirmNewPasswordLayout.setError("Passwords do not match");
            return false;
        }
        confirmNewPasswordLayout.setError(null);
        return true;
    }

    private void handlePasswordReset() {
        String username = usernameEditText.getText().toString().trim().toLowerCase();
        String securityAnswer = securityAnswerEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();

        resetButton.setEnabled(false);
        resetButton.setText("Resetting password...");

        // Format: RESET_PASSWORD:username,securityAnswer,newPassword
        String resetData = String.format("RESET_PASSWORD:%s,%s,%s",
                username, securityAnswer, newPassword);

        ServerCommunication.sendToServer(resetData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    resetButton.setEnabled(true);
                    resetButton.setText("Reset Password");

                    if (response.startsWith("SUCCESS")) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password successfully reset", Toast.LENGTH_SHORT).show();

                        // Redirigir al login
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = response.contains(":") ?
                                response.split(":")[1] : "Unknown error";

                        if ("INVALID_USER".equals(errorMessage)) {
                            usernameEditText.setError("User not found");
                        } else if ("INVALID_ANSWER".equals(errorMessage)) {
                            securityAnswerEditText.setError("Incorrect security answer");
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error resetting password: " + error);
                runOnUiThread(() -> {
                    resetButton.setEnabled(true);
                    resetButton.setText("Reset Password");
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Connection error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}