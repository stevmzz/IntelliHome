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
import android.widget.RadioGroup;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

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

        fullNameEditText.addTextChangedListener(textWatcher);
        usernameEditText.addTextChangedListener(textWatcher);
        emailEditText.addTextChangedListener(textWatcher);
        confirmPasswordEditText.addTextChangedListener(textWatcher);
        descriptionEditText.addTextChangedListener(textWatcher);
        hobbiesEditText.addTextChangedListener(textWatcher);
        phoneEditText.addTextChangedListener(textWatcher);
        verificationEditText.addTextChangedListener(textWatcher);
        ibanEditText.addTextChangedListener(textWatcher);
        birthDateEditText.addTextChangedListener(textWatcher);

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

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateAllFields()) {
                    Intent intent = new Intent(PruebaActivity.this, ExitActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(PruebaActivity.this, "Por favor, corrige los errores.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        showHidePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(passwordEditText, showHidePasswordButton);
            }
        });

        showHideConfirmPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(confirmPasswordEditText, showHideConfirmPasswordButton);
            }
        });

        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PruebaActivity.this, "Términos y condiciones aceptados", Toast.LENGTH_SHORT).show();
            }
        });

        buttonReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PruebaActivity.this, "Debe aceptar los términos y condiciones para continuar", Toast.LENGTH_SHORT).show();
            }
        });
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

        isValid &= validatePassword();
        isValid &= validateConfirmPassword();
        isValid &= validateEmail();
        isValid &= validateBirthDate();

        // Aquí puedes añadir más validaciones para otros campos si es necesario

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

            if (age < 18) {
                birthDateEditText.setError("Debes ser mayor de 18 años para registrarte.");
                return false;
            } else {
                birthDateEditText.setError(null);
                return true;
            }
        } catch (ParseException e) {
            birthDateEditText.setError("Por favor, ingrese una fecha válida en formato DD/MM/AAAA.");
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
}