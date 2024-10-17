package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.text.InputType;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private ImageButton showHidePasswordButton;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity); // Asegúrate de usar el layout correcto

        // Referencia a los elementos de la interfaz
        passwordEditText = findViewById(R.id.password2);
        showHidePasswordButton = findViewById(R.id.showHidePasswordButton);

        // Listener para mostrar/ocultar la contraseña
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

        // Listener para el botón "Forgot Password?"
        findViewById(R.id.buttonForgotPassword2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
                // Puedes implementar la funcionalidad que desees aquí
            }
        });
    }
}
