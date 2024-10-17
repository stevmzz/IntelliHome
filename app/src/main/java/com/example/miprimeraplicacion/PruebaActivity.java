package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import java.util.regex.Pattern;

public class PruebaActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText fullNameEditText;
    private EditText usernameEditText;
    private EditText emailEditText;
    private ImageButton showHidePasswordButton;
    private ImageButton showHideConfirmPasswordButton;
    private Button buttonContinue;
    private Button buttonSelectPhoto;
    private ImageView imageViewPhoto;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prueba); // Asegúrate de que este es el layout correcto

        // Referencias a los elementos de la interfaz
        fullNameEditText = findViewById(R.id.fullName);
        usernameEditText = findViewById(R.id.username);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        showHidePasswordButton = findViewById(R.id.showHidePasswordButton);
        showHideConfirmPasswordButton = findViewById(R.id.showHideConfirmPasswordButton);
        buttonContinue = findViewById(R.id.buttonContinue);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);

        // Desactivar el botón continuar al inicio
        buttonContinue.setEnabled(false);

        // Listeners para campos de texto
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        fullNameEditText.addTextChangedListener(textWatcher);
        usernameEditText.addTextChangedListener(textWatcher);
        emailEditText.addTextChangedListener(textWatcher);
        confirmPasswordEditText.addTextChangedListener(textWatcher);

        // Validación de contraseña en tiempo real
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(); // Validar la contraseña mientras se escribe
                checkFieldsForEmptyValues(); // Verificar si se deben habilitar los botones
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Listener para el botón de continuar
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validatePassword() && validateConfirmPassword()) {
                    Intent intent = new Intent(PruebaActivity.this, ExitActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(PruebaActivity.this, "Por favor, corrige los errores.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Listener para el botón de seleccionar foto
        buttonSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Listener para el botón de visibilidad de contraseña
        showHidePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(passwordEditText, showHidePasswordButton);
            }
        });

        // Listener para el botón de visibilidad de confirmación de contraseña
        showHideConfirmPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(confirmPasswordEditText, showHideConfirmPasswordButton);
            }
        });
    }

    // Método para verificar que los campos no estén vacíos
    private void checkFieldsForEmptyValues() {
        String fullName = fullNameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        buttonContinue.setEnabled(!fullName.isEmpty() && !username.isEmpty() && !email.isEmpty() &&
                !password.isEmpty() && !confirmPassword.isEmpty());
    }

    // Método para validar la contraseña
    private boolean validatePassword() {
        String passwordInput = passwordEditText.getText().toString().trim();

        if (!PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            passwordEditText.setError("Debe tener 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
            return false;
        } else {
            passwordEditText.setError(null);
            return true;
        }
    }

    // Método para validar que la confirmación de la contraseña coincida
    private boolean validateConfirmPassword() {
        String passwordInput = passwordEditText.getText().toString().trim();
        String confirmPasswordInput = confirmPasswordEditText.getText().toString().trim();

        if (!passwordInput.equals(confirmPasswordInput)) {
            confirmPasswordEditText.setError("Las contraseñas no coinciden.");
            return false;
        } else {
            confirmPasswordEditText.setError(null);
            return true;
        }
    }

    // Método para alternar la visibilidad de las contraseñas
    private void togglePasswordVisibility(EditText editText, ImageButton button) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            button.setImageResource(R.drawable.ojo_contra2);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button.setImageResource(R.drawable.ojo_contra);
        }
        editText.setSelection(editText.length());
    }

    // Método para abrir la galería
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Método para manejar el resultado de la selección de la imagen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imageViewPhoto.setImageURI(imageUri);
        }
    }
}
