package com.example.miprimeraplicacion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.util.Log;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.radiobutton.MaterialRadioButton;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import android.widget.ImageView;  // Para ImageView
import android.content.Context;   // Para Context
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import android.widget.TextView;  // Faltaba este import

public class PruebaActivity extends AppCompatActivity {

    private static final String TAG = "PruebaActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    // UI Components
    private TextInputEditText fullNameEditText, usernameEditText, emailEditText;
    private TextInputEditText passwordEditText, confirmPasswordEditText;
    private TextInputEditText descriptionEditText, hobbiesEditText, phoneEditText;
    private TextInputEditText verificationEditText, ibanEditText, birthDateEditText;
    private TextInputLayout passwordLayout, confirmPasswordLayout;
    private MaterialButton buttonContinue, buttonSelectPhoto, buttonTakePhoto;
    private MaterialButton buttonAccept, buttonReject;
    private MaterialCheckBox termsCheckbox;
    private MaterialRadioButton radioArrendador, radioAlquilador;
    private ImageView imageViewPhoto;

    private static final int FINGERPRINT_PERMISSION_CODE = 3;
    private boolean isFingerPrintRegistered = false;
    private ImageView fingerprintIcon;
    private TextView fingerprintStatus;
    private MaterialButton buttonRegisterFingerprint;

    // Estados
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private Uri photoURI;

    // Validación de contraseña
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
        // TextInputEditText
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

        // TextInputLayout
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        // Buttons
        buttonContinue = findViewById(R.id.buttonContinue);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto);
        buttonAccept = findViewById(R.id.buttonAccept);
        buttonReject = findViewById(R.id.buttonReject);

        // Others
        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        radioArrendador = findViewById(R.id.radioArrendador);
        radioAlquilador = findViewById(R.id.radioAlquilador);

        fingerprintIcon = findViewById(R.id.fingerprintIcon);
        fingerprintStatus = findViewById(R.id.fingerprintStatus);
        buttonRegisterFingerprint = findViewById(R.id.buttonRegisterFingerprint);
    }

    private void setupListeners() {
        // TextWatcher general para validación de campos
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

        buttonRegisterFingerprint.setOnClickListener(v -> {
            checkBiometricSupport();
        });

        // Aplicar TextWatcher a todos los campos
        fullNameEditText.addTextChangedListener(textWatcher);
        emailEditText.addTextChangedListener(textWatcher);
        descriptionEditText.addTextChangedListener(textWatcher);
        hobbiesEditText.addTextChangedListener(textWatcher);
        phoneEditText.addTextChangedListener(textWatcher);
        verificationEditText.addTextChangedListener(textWatcher);
        ibanEditText.addTextChangedListener(textWatcher);
        birthDateEditText.addTextChangedListener(textWatcher);

        // Username TextWatcher especial (conversión a minúsculas)
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

        // Password TextWatcher para validación en tiempo real
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

        // Confirm Password TextWatcher
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword();
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar los endIcon para mostrar/ocultar contraseña
        passwordLayout.setEndIconOnClickListener(v -> togglePasswordVisibility(passwordEditText, passwordLayout));
        confirmPasswordLayout.setEndIconOnClickListener(v -> togglePasswordVisibility(confirmPasswordEditText, confirmPasswordLayout));

        // Terms Checkbox
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonContinue.setEnabled(isChecked && areAllFieldsFilled());
            if (!isChecked) {
                Toast.makeText(this, "You must accept the terms and conditions to continue",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Photo buttons
        buttonSelectPhoto.setOnClickListener(v -> openGallery());
        buttonTakePhoto.setOnClickListener(v -> takePicture());

        // Terms buttons
        buttonAccept.setOnClickListener(v -> {
            termsCheckbox.setChecked(true);
            Toast.makeText(this, "Terms and conditions accepted", Toast.LENGTH_SHORT).show();
        });

        buttonReject.setOnClickListener(v -> {
            termsCheckbox.setChecked(false);
            Toast.makeText(this, "You must accept the terms and conditions to continue",
                    Toast.LENGTH_SHORT).show();
        });

        // Continue button
        buttonContinue.setOnClickListener(v -> {
            if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, "Please accept the terms and conditions",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (validateAllFields()) {
                registerUser();
            } else {
                Toast.makeText(this, "Please correct the errors before continuing",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBiometricSupport() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Device doesn't support fingerprint", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No fingerprint enrolled", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Register Fingerprint")
                .setSubtitle("Place your finger on the sensor")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        runOnUiThread(() -> {
                            isFingerPrintRegistered = true;
                            fingerprintStatus.setText("Fingerprint registered");
                            fingerprintStatus.setTextColor(getResources().getColor(R.color.success));
                            buttonRegisterFingerprint.setEnabled(false);
                            buttonRegisterFingerprint.setText("Fingerprint Registered");
                            checkFieldsForEmptyValues();  // Asegurarse de que esto se llama
                            Toast.makeText(PruebaActivity.this,
                                    "Fingerprint registered successfully",
                                    Toast.LENGTH_SHORT).show();  // Agregar feedback
                        });
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(PruebaActivity.this, errString, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(PruebaActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private void checkFieldsForEmptyValues() {
        boolean shouldEnableContinue = areAllFieldsFilled() && termsCheckbox.isChecked();
        buttonContinue.setEnabled(shouldEnableContinue);
    }

    private boolean areAllFieldsFilled() {
        return !fullNameEditText.getText().toString().trim().isEmpty() &&
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
                (radioArrendador.isChecked() || radioAlquilador.isChecked()) &&  // Agregar && aquí
                isFingerPrintRegistered;
    }

    private void togglePasswordVisibility(TextInputEditText editText, TextInputLayout layout) {
        boolean isVisible = editText.getInputType() !=
                (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        if (isVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.setEndIconDrawable(R.drawable.ojo_contra);
        } else {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            layout.setEndIconDrawable(R.drawable.ojo_contra2);
        }
        editText.setSelection(editText.length());
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validar nombre completo
        String fullName = fullNameEditText.getText().toString().trim();
        if (fullName.length() < 3) {
            fullNameEditText.setError("Name must be at least 3 characters long");
            isValid = false;
        }

        if (!isFingerPrintRegistered) {
            Toast.makeText(this, "You must register your fingerprint", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validar usuario
        String username = usernameEditText.getText().toString().trim();
        if (username.length() < 4) {
            usernameEditText.setError("Username must be at least 4 characters long");
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
            descriptionEditText.setError("Description must be at least 10 characters long");
            isValid = false;
        }

        // Validar hobbies
        String hobbies = hobbiesEditText.getText().toString().trim();
        if (hobbies.length() < 5) {
            hobbiesEditText.setError("Hobbies must be at least 5 characters long");
            isValid = false;
        }

        // Validar teléfono (8 dígitos)
        String phone = phoneEditText.getText().toString().trim();
        if (!phone.matches("\\d{8}")) {
            phoneEditText.setError("Phone must be exactly 8 digits");
            isValid = false;
        }

        // Validar IBAN (CR + 20-25 números)
        String iban = ibanEditText.getText().toString().trim().toUpperCase();
        if (!iban.startsWith("CR") || !iban.substring(2).matches("[0-9]{20,25}")) {
            ibanEditText.setError("Invalid IBAN format (CR + 20-25 numbers)");
            isValid = false;
        }

        // Validar fecha de nacimiento y edad
        if (!validateBirthDate()) {
            isValid = false;
        }

        // Validar tipo de usuario
        if (!radioArrendador.isChecked() && !radioAlquilador.isChecked()) {
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validar términos y condiciones
        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "You must accept the terms and conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private boolean validateEmail() {
        String emailInput = emailEditText.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return false;
        }
        return true;
    }

    private boolean validatePassword() {
        String passwordInput = passwordEditText.getText().toString().trim();
        if (!PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            passwordLayout.setError("Password must contain at least 8 characters, including uppercase, lowercase, number and special character");
            return false;
        } else {
            passwordLayout.setError(null);
            return true;
        }
    }

    private boolean validateConfirmPassword() {
        String passwordInput = passwordEditText.getText().toString().trim();
        String confirmPasswordInput = confirmPasswordEditText.getText().toString().trim();
        if (!passwordInput.equals(confirmPasswordInput)) {
            confirmPasswordLayout.setError("Passwords do not match");
            return false;
        } else {
            confirmPasswordLayout.setError(null);
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

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);

            // Ajustar edad si aún no ha cumplido años este año
            if (today.get(Calendar.MONTH) < calendar.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) < calendar.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            if (age < 18) {
                birthDateEditText.setError("You must be at least 18 years old");
                return false;
            }
            return true;
        } catch (ParseException e) {
            birthDateEditText.setError("Please enter a valid date (DD/MM/YYYY)");
            return false;
        }
    }

    // Manejo de imágenes
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    CAMERA_REQUEST);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Error creating image file", ex);
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (photoFile != null) {
                    try {
                        photoURI = FileProvider.getUriForFile(this,
                                getApplicationContext().getPackageName() + ".provider",
                                photoFile);

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                        // Otorgar permisos temporales
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching camera", e);
                        Toast.makeText(this, "Error launching camera", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in dispatchTakePictureIntent", e);
            Toast.makeText(this, "Error accessing camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        return image;
    }

    // Manejo de resultados de la cámara y galería
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                    Uri imageUri = data.getData();
                    try {
                        imageViewPhoto.setImageURI(imageUri);
                        imageViewPhoto.setBackground(null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading gallery image", e);
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == CAMERA_REQUEST && photoURI != null) {
                    try {
                        imageViewPhoto.setImageURI(photoURI);
                        imageViewPhoto.setBackground(null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading camera image", e);
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onActivityResult", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    // Manejo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Registro de usuario
    private void registerUser() {
        // Ocultar teclado
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Mostrar progreso
        buttonContinue.setEnabled(false);
        buttonContinue.setText("Registering...");

        String userType = radioAlquilador.isChecked() ? "alquilador" : "arrendador";
        Log.d(TAG, "Selected user type: " + userType);

        String userData = String.format("REGISTER:%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%b",  // Agregar %b para el booleano
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
                userType,
                isFingerPrintRegistered);

        Log.d(TAG, "Sending registration data to server");

        ServerCommunication.sendToServer(userData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Server response: " + response);

                runOnUiThread(() -> {
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Continue");

                    String[] parts = response.split(":");
                    if (parts[0].equals("SUCCESS")) {
                        handleSuccessfulRegistration(userType);
                    } else {
                        handleRegistrationError(response);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Server communication error: " + error);
                runOnUiThread(() -> {
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Continue");
                    Toast.makeText(PruebaActivity.this,
                            "Connection error: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleSuccessfulRegistration(String userType) {
        // Guardar preferencias del usuario
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_type", userType);
        editor.putString("username", usernameEditText.getText().toString().trim().toLowerCase());
        editor.putBoolean("fingerprint_registered", isFingerPrintRegistered);
        editor.apply();

        Log.d(TAG, "User preferences saved");

        Toast.makeText(PruebaActivity.this,
                "Registration successful as " + userType,
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
    }

    private void handleRegistrationError(String response) {
        if (response.contains("USERNAME_EXISTS")) {
            usernameEditText.setError("Username already exists");
            usernameEditText.requestFocus();
        } else if (response.contains("EMAIL_EXISTS")) {
            emailEditText.setError("Email already registered");
            emailEditText.requestFocus();
        } else {
            Toast.makeText(PruebaActivity.this,
                    "Registration error: " + response,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoURI != null) {
            outState.putString("photoURI", photoURI.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String savedPhotoURI = savedInstanceState.getString("photoURI");
        if (savedPhotoURI != null) {
            photoURI = Uri.parse(savedPhotoURI);
            imageViewPhoto.setImageURI(photoURI);
            imageViewPhoto.setBackgroundResource(0);
        }
    }

    // Método para limpiar recursos cuando se destruye la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar imagen temporal si existe
        if (photoURI != null) {
            File photoFile = new File(photoURI.getPath());
            if (photoFile.exists()) {
                photoFile.delete();
            }
        }
    }
}