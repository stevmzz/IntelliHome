package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;
import android.util.Log;
import android.content.SharedPreferences;  // Esta es la importación necesaria
import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class PruebaActivity extends AppCompatActivity {

    private EditText fullNameEditText, usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private EditText descriptionEditText, hobbiesEditText, phoneEditText, verificationEditText, ibanEditText, birthDateEditText;
    private ImageButton showHidePasswordButton, showHideConfirmPasswordButton;
    private Button buttonContinue, buttonSelectPhoto, buttonAccept, buttonReject;
    private ImageView imageViewPhoto;
    private RadioGroup userTypeGroup;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prueba);

        initializeViews();
        setupListeners();
        buttonContinue.setEnabled(false);
    }

    private void initializeViews() {
        fullNameEditText = findViewById(R.id.fullName);
        usernameEditText = findViewById(R.id.username);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        descriptionEditText = findViewById(R.id.description);
        hobbiesEditText = findViewById(R.id.descripcion);
        phoneEditText = findViewById(R.id.telefono);
        verificationEditText = findViewById(R.id.Verificacion);
        ibanEditText = findViewById(R.id.Iban);
        birthDateEditText = findViewById(R.id.fechaNacimiento);
        showHidePasswordButton = findViewById(R.id.showHidePasswordButton);
        showHideConfirmPasswordButton = findViewById(R.id.showHideConfirmPasswordButton);
        buttonContinue = findViewById(R.id.buttonContinue);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        userTypeGroup = findViewById(R.id.userTypeGroup);
        buttonAccept = findViewById(R.id.buttonAccept);
        buttonReject = findViewById(R.id.buttonReject);
    }

    private void setupListeners() {
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

        // Agregar TextWatcher a todos los campos
        fullNameEditText.addTextChangedListener(textWatcher);
        emailEditText.addTextChangedListener(textWatcher);
        confirmPasswordEditText.addTextChangedListener(textWatcher);
        descriptionEditText.addTextChangedListener(textWatcher);
        hobbiesEditText.addTextChangedListener(textWatcher);
        phoneEditText.addTextChangedListener(textWatcher);
        verificationEditText.addTextChangedListener(textWatcher);
        ibanEditText.addTextChangedListener(textWatcher);
        birthDateEditText.addTextChangedListener(textWatcher);

        // TextWatcher especial para username (conversión a minúsculas)
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
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // TextWatcher para validación de contraseña
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Click listeners
        buttonContinue.setOnClickListener(v -> {
            if (validateAllFields()) {
                registerUser();
            } else {
                Toast.makeText(PruebaActivity.this, "Por favor, corrige los errores.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSelectPhoto.setOnClickListener(v -> openGallery());

        showHidePasswordButton.setOnClickListener(v ->
                togglePasswordVisibility(passwordEditText, showHidePasswordButton));

        showHideConfirmPasswordButton.setOnClickListener(v ->
                togglePasswordVisibility(confirmPasswordEditText, showHideConfirmPasswordButton));

        buttonAccept.setOnClickListener(v ->
                Toast.makeText(PruebaActivity.this, "Términos y condiciones aceptados", Toast.LENGTH_SHORT).show());

        buttonReject.setOnClickListener(v ->
                Toast.makeText(PruebaActivity.this, "Debe aceptar los términos y condiciones para continuar", Toast.LENGTH_SHORT).show());
    }

    private void checkFieldsForEmptyValues() {
        boolean allFieldsFilled = !fullNameEditText.getText().toString().trim().isEmpty() &&
                !usernameEditText.getText().toString().trim().isEmpty() &&
                !emailEditText.getText().toString().trim().isEmpty() &&
                !passwordEditText.getText().toString().trim().isEmpty() &&
                !confirmPasswordEditText.getText().toString().trim().isEmpty() &&
                !descriptionEditText.getText().toString().trim().isEmpty() &&
                !hobbiesEditText.getText().toString().trim().isEmpty() &&
                !phoneEditText.getText().toString().trim().isEmpty() &&
                !verificationEditText.getText().toString().trim().isEmpty() &&
                !ibanEditText.getText().toString().trim().isEmpty() &&
                !birthDateEditText.getText().toString().trim().isEmpty() &&
                userTypeGroup.getCheckedRadioButtonId() != -1;

        buttonContinue.setEnabled(allFieldsFilled);
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validar nombre completo
        String fullName = fullNameEditText.getText().toString().trim();
        if (fullName.length() < 3) {
            fullNameEditText.setError("El nombre debe tener al menos 3 caracteres");
            isValid = false;
        }

        // Validar usuario
        String username = usernameEditText.getText().toString().trim();
        if (username.length() < 4) {
            usernameEditText.setError("El usuario debe tener al menos 4 caracteres");
            isValid = false;
        }

        // Validar email
        if (!validateEmail()) {
            isValid = false;
        }

        // Validar contraseñas
        if (!validatePassword()) {
            isValid = false;
        }
        if (!validateConfirmPassword()) {
            isValid = false;
        }

        // Validar descripción
        String description = descriptionEditText.getText().toString().trim();
        if (description.length() < 10) {
            descriptionEditText.setError("La descripción debe tener al menos 10 caracteres");
            isValid = false;
        }

        // Validar hobbies
        String hobbies = hobbiesEditText.getText().toString().trim();
        if (hobbies.length() < 5) {
            hobbiesEditText.setError("Los hobbies deben tener al menos 5 caracteres");
            isValid = false;
        }

        // Validar teléfono (8 dígitos)
        String phone = phoneEditText.getText().toString().trim();
        if (!phone.matches("\\d{8}")) {
            phoneEditText.setError("El teléfono debe tener 8 dígitos");
            isValid = false;
        }

        // Validar IBAN (CR + 20-25 números)
        String iban = ibanEditText.getText().toString().trim().toUpperCase();
        if (!iban.startsWith("CR") || !iban.substring(2).matches("[0-9]{20,25}")) {
            ibanEditText.setError("IBAN inválido (formato: CR + 20-25 números)");
            isValid = false;
        }

        // Validar fecha de nacimiento
        if (!validateBirthDate()) {
            isValid = false;
        }

        // Validar tipo de usuario
        if (userTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Seleccione un tipo de usuario", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private boolean validatePassword() {
        String passwordInput = passwordEditText.getText().toString().trim();

        if (!PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            passwordEditText.setError("La contraseña debe tener al menos 8 caracteres, incluir una mayúscula, una minúscula, un número y un carácter especial.");
            return false;
        } else {
            passwordEditText.setError(null);
            return true;
        }
    }

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

    private boolean validateEmail() {
        String emailInput = emailEditText.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            emailEditText.setError("Por favor, ingrese una dirección de correo electrónico válida.");
            return false;
        } else {
            emailEditText.setError(null);
            return true;
        }
    }

    private boolean validateBirthDate() {
        String dateInput = birthDateEditText.getText().toString().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);

        try {
            Date birthDate = sdf.parse(dateInput);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(birthDate);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - year;
            if (today.get(Calendar.MONTH) < month ||
                    (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)) {
                age--;
            }

            if (age < 0) {
                birthDateEditText.setError("Debes ser mayor de 18 años para registrarte.");
                return false;
            } else {
                birthDateEditText.setError(null);
                return true;
            }
        } catch (ParseException e) {
            birthDateEditText.setError("Por favor, ingrese una fecha válida en formato DD/MM/YYYY.");
            return false;
        }
    }

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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imageViewPhoto.setImageURI(imageUri);
        }
    }

    private void registerUser() {
        // Ocultar teclado
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Obtener el ID del RadioButton seleccionado
        int selectedId = userTypeGroup.getCheckedRadioButtonId();

        Log.d("RegisterUser", "ID del RadioButton seleccionado: " + selectedId);

        // Asegurarnos de que uno está seleccionado
        if (selectedId == -1) {
            Toast.makeText(this, "Por favor seleccione un tipo de usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        String userType = selectedId == R.id.radioAlquilador ? "alquilador" : "arrendador";
        Log.d("RegisterUser", "Tipo de usuario seleccionado: " + userType);

        String userData = String.format("REGISTER:%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                fullNameEditText.getText().toString().trim(),
                usernameEditText.getText().toString().trim().toLowerCase(),
                emailEditText.getText().toString().trim(),
                passwordEditText.getText().toString().trim(),
                descriptionEditText.getText().toString().trim(),
                hobbiesEditText.getText().toString().trim(),
                phoneEditText.getText().toString().trim(),
                verificationEditText.getText().toString().trim().toUpperCase(),
                ibanEditText.getText().toString().trim().toUpperCase(),
                birthDateEditText.getText().toString().trim(),
                userType);

        Log.d("RegisterUser", "Datos de registro: " + userData);

        ServerCommunication.sendToServer(userData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d("RegisterUser", "Respuesta del servidor: " + response);

                runOnUiThread(() -> {
                    String[] parts = response.split(":");
                    if (parts[0].equals("SUCCESS")) {
                        // Guardar el tipo de usuario
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_type", userType);
                        editor.apply();

                        Log.d("RegisterUser", "Tipo de usuario guardado: " + userType);

                        Toast.makeText(PruebaActivity.this,
                                "Registro exitoso como " + userType,
                                Toast.LENGTH_SHORT).show();

                        // Redirigir según el tipo de usuario
                        Intent intent;
                        if ("alquilador".equals(userType)) {
                            intent = new Intent(PruebaActivity.this, AlquiladorActivity.class);
                        } else {
                            intent = new Intent(PruebaActivity.this, ArrendadorActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage;
                        if (response.contains("USERNAME_EXISTS")) {
                            errorMessage = "El nombre de usuario ya está en uso";
                            usernameEditText.setError(errorMessage);
                        } else if (response.contains("EMAIL_EXISTS")) {
                            errorMessage = "El email ya está registrado";
                            emailEditText.setError(errorMessage);
                        } else {
                            errorMessage = "Error en el registro: " + response;
                        }
                        Log.e("RegisterUser", "Error en registro: " + errorMessage);
                        Toast.makeText(PruebaActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("RegisterUser", "Error de conexión: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PruebaActivity.this,
                            "Error de conexión: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

}