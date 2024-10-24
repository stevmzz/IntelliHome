package com.example.miprimeraplicacion;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddPropertyActivity extends AppCompatActivity {
    private static final String TAG = "AddPropertyActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private String currentUsername;

    // UI Components
    private EditText titleEditText;
    private AutoCompleteTextView propertyTypeAutoComplete;
    private EditText descriptionEditText;
    private EditText priceEditText;
    private EditText locationEditText;
    private EditText capacityEditText;
    private ChipGroup amenitiesChipGroup;
    private EditText rulesEditText;
    private Button addPhotoButton;
    private RecyclerView photosRecyclerView;
    private Button saveButton;
    private Button cancelButton;

    // Photo handling
    private ArrayList<String> selectedPhotosBase64;
    private PhotoAdapter photoAdapter;

    // Datos estáticos
    private final String[] PROPERTY_TYPES = {"Moderna", "Mansión", "Tecnológica", "Rústica"};
    private final String[] AMENITIES = {
            "Cocina equipada", "Aire acondicionado", "Calefacción", "Wi-Fi gratuito",
            "Televisión por cable", "Lavadora y secadora", "Piscina", "Jardín",
            "Barbacoa", "Terraza", "Gimnasio", "Garaje", "Sistema de seguridad",
            "Baño en suite", "Muebles de exterior", "Microondas", "Lavavajillas",
            "Cafetera", "Ropa de cama incluida", "Áreas comunes", "Camas adicionales",
            "Servicio de limpieza", "Transporte público cercano", "Mascotas permitidas",
            "Cerca de comercios", "Suelo radiante", "Área de trabajo", "Sistemas de entretenimiento",
            "Chimenea", "Internet alta velocidad"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_property);

        // Obtener y verificar el username al inicio
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "").trim();

        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Usuario actual: " + currentUsername);
        selectedPhotosBase64 = new ArrayList<>();
        initializeViews();
        setupPropertyTypeDropdown();
        setupAmenitiesChips();
        setupPhotoSelection();
        setupListeners();
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.titleEditText);
        propertyTypeAutoComplete = findViewById(R.id.propertyTypeAutoComplete);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);
        locationEditText = findViewById(R.id.locationEditText);
        capacityEditText = findViewById(R.id.capacityEditText);
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        rulesEditText = findViewById(R.id.rulesEditText);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupPropertyTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                PROPERTY_TYPES
        );
        propertyTypeAutoComplete.setAdapter(adapter);
    }

    private void setupAmenitiesChips() {
        for (String amenity : AMENITIES) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCheckable(true);
            amenitiesChipGroup.addView(chip);
        }
    }

    private void setupPhotoSelection() {
        photoAdapter = new PhotoAdapter(this, selectedPhotosBase64, position -> {
            selectedPhotosBase64.remove(position);
            photoAdapter.notifyItemRemoved(position);
        });

        photosRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photosRecyclerView.setAdapter(photoAdapter);

        addPhotoButton.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProperty());
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permiso denegado para acceder a las fotos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Redimensionar la imagen
                Bitmap resizedBitmap = resizeBitmap(bitmap, 800);

                // Convertir a Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Agregar a la lista y actualizar RecyclerView
                selectedPhotosBase64.add(base64Image);
                photoAdapter.notifyItemInserted(selectedPhotosBase64.size() - 1);

            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void saveProperty() {
        try {
            // Re-verificar el username
            if (currentUsername.isEmpty()) {
                Log.e(TAG, "Error: Username vacío al intentar guardar");
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Validaciones básicas
            String title = titleEditText.getText().toString().trim();
            if (title.isEmpty()) {
                titleEditText.setError("El título es requerido");
                return;
            }

            // Recopilar datos
            String propertyType = propertyTypeAutoComplete.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String location = locationEditText.getText().toString().trim();
            String rules = rulesEditText.getText().toString().trim();

            // Log de datos básicos
            Log.d(TAG, "Preparando datos para enviar:");
            Log.d(TAG, "Usuario: " + currentUsername);
            Log.d(TAG, "Título: " + title);
            Log.d(TAG, "Tipo: " + propertyType);

            // Manejar campos numéricos
            String priceStr = "0";
            try {
                if (!priceEditText.getText().toString().trim().isEmpty()) {
                    double price = Double.parseDouble(priceEditText.getText().toString().trim());
                    priceStr = String.valueOf((int)price);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error en formato de precio: " + e.getMessage());
            }

            String capacityStr = "0";
            try {
                if (!capacityEditText.getText().toString().trim().isEmpty()) {
                    int capacity = Integer.parseInt(capacityEditText.getText().toString().trim());
                    capacityStr = String.valueOf(capacity);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error en formato de capacidad: " + e.getMessage());
            }

            // Obtener amenidades seleccionadas
            List<String> selectedAmenities = getSelectedAmenities();
            String amenitiesStr = String.join("|", selectedAmenities);
            Log.d(TAG, "Amenidades seleccionadas: " + amenitiesStr);

            // Preparar string de fotos
            StringBuilder photosStr = new StringBuilder();
            if (!selectedPhotosBase64.isEmpty()) {
                photosStr.append(selectedPhotosBase64.get(0));
                for (int i = 1; i < selectedPhotosBase64.size(); i++) {
                    photosStr.append("|").append(selectedPhotosBase64.get(i));
                }
            }
            Log.d(TAG, "Número de fotos a enviar: " + selectedPhotosBase64.size());

            // Validar datos antes de enviar
            Log.d(TAG, "Validando datos antes de enviar:");
            Log.d(TAG, "Precio: " + priceStr);
            Log.d(TAG, "Capacidad: " + capacityStr);
            Log.d(TAG, "Ubicación: " + location);
            Log.d(TAG, "Reglas: " + rules);

            // Construir string de datos para enviar al servidor
            String propertyData = String.format("ADD_PROPERTY:%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    currentUsername,
                    title.replace(",", " "),
                    description.replace(",", " "),
                    priceStr,
                    location.replace(",", " "),
                    capacityStr,
                    propertyType.isEmpty() ? "No especificado" : propertyType.replace(",", " "),
                    amenitiesStr.replace(",", " "),
                    photosStr.toString(),
                    rules.replace(",", " ")
            );

            // Log del mensaje final
            Log.d(TAG, "Enviando datos al servidor:");
            Log.d(TAG, "Longitud total de datos: " + propertyData.length());
            Log.d(TAG, "Datos: " + propertyData.substring(0, Math.min(500, propertyData.length())) + "...");

            // Enviar al servidor
            ServerCommunication.sendToServer(propertyData, new ServerCommunication.ServerResponseListener() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Respuesta del servidor recibida: " + response);
                    runOnUiThread(() -> {
                        if (response.startsWith("SUCCESS")) {
                            Toast.makeText(AddPropertyActivity.this,
                                    "Propiedad guardada exitosamente", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String errorMsg = response.startsWith("ERROR:") ?
                                    response.substring(6) : response;
                            Log.e(TAG, "Error del servidor: " + errorMsg);
                            Toast.makeText(AddPropertyActivity.this,
                                    "Error al guardar la propiedad: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error de comunicación: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(AddPropertyActivity.this,
                                "Error de conexión: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error guardando propiedad: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la propiedad", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getSelectedAmenities() {
        List<String> selectedAmenities = new ArrayList<>();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedAmenities.add(chip.getText().toString());
            }
        }
        return selectedAmenities;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}