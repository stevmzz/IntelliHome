package com.example.miprimeraplicacion;

import com.example.miprimeraplicacion.CostaRicaLocations;
import com.example.miprimeraplicacion.LocationAutocompleteHelper;
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
import androidx.core.content.FileProvider;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AlertDialog;





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
    private AutoCompleteTextView locationEditText;
    private EditText capacityEditText;
    private ChipGroup amenitiesChipGroup;
    private EditText rulesEditText;
    private Button addPhotoButton;
    private RecyclerView photosRecyclerView;
    private Button saveButton;
    private Button cancelButton;
    private Button takePhotoButton;
    private static final int CAMERA_REQUEST = 2;  // Para los permisos de cámara
    private Uri photoURI;  // Para mantener la referencia de la foto tomada


    // Photo handling
    private ArrayList<String> selectedPhotosBase64;
    private PhotoAdapter photoAdapter;

    private LocationAutocompleteHelper locationHelper;

    String decodedString = "some value";


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

        // Primero inicializar las vistas
        initializeViews();

        // Configurar el autocompletado de ubicación
        locationHelper = new LocationAutocompleteHelper(
                this,
                locationEditText,
                location -> {
                    // Opcional: mostrar un Toast con la ubicación seleccionada
                    Toast.makeText(AddPropertyActivity.this,
                            "Ubicación seleccionada: " + location,
                            Toast.LENGTH_SHORT).show();
                }
        );

        // Vincular el botón y configurar el listener
        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        addPhotoButton.setOnClickListener(v -> openGallery());

        setupPropertyTypeDropdown();
        setupAmenitiesChips();
        setupPhotoSelection();
        setupListeners();
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
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
        takePhotoButton = findViewById(R.id.takePhotoButton);
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
            updatePhotoButtonsState();
        });

        photosRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photosRecyclerView.setAdapter(photoAdapter);

        addPhotoButton.setOnClickListener(v -> {
            if (selectedPhotosBase64.size() >= 10) {
                Toast.makeText(this, "Máximo 10 fotos permitidas", Toast.LENGTH_SHORT).show();
                return;
            }
            checkPermissionAndPickImage();
        });

        takePhotoButton.setOnClickListener(v -> {
            if (selectedPhotosBase64.size() >= 10) {
                Toast.makeText(this, "Máximo 10 fotos permitidas", Toast.LENGTH_SHORT).show();
                return;
            }
            checkCameraPermissionAndTakePhoto();
        });

        updatePhotoButtonsState();
    }

    private void updatePhotoButtonsState() {
        boolean maxPhotosReached = selectedPhotosBase64.size() >= 10;
        addPhotoButton.setEnabled(!maxPhotosReached);
        takePhotoButton.setEnabled(!maxPhotosReached);

        if (maxPhotosReached) {
            addPhotoButton.setAlpha(0.5f);
            takePhotoButton.setAlpha(0.5f);
        } else {
            addPhotoButton.setAlpha(1.0f);
            takePhotoButton.setAlpha(1.0f);
        }
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

    private void checkCameraPermissionAndTakePhoto() {
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

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Mapear los permisos solicitados a sus resultados
            Map<String, Integer> perms = new HashMap<>();
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
            }

            // Verificar si se concedieron los permisos esenciales
            boolean allGranted = true;
            for (String permission : permissions) {
                if (perms.get(permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Todos los permisos fueron concedidos
                Toast.makeText(this, "Permisos concedidos. ¡Ahora puedes agregar imágenes!", Toast.LENGTH_SHORT).show();
            } else {
                // Algunos permisos fueron denegados
                Toast.makeText(this, "Permisos denegados. No se puede completar la acción.", Toast.LENGTH_SHORT).show();

                // Opcional: Mostrar un diálogo para explicar por qué los permisos son importantes
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }
                if (shouldShowRationale) {
                    showPermissionExplanationDialog();
                } else {
                    // El usuario marcó "No volver a preguntar"
                    Toast.makeText(this, "Debes habilitar los permisos desde la configuración.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso requerido")
                .setMessage("Se necesita este permiso para continuar.")
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss())
                .show();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                if (selectedPhotosBase64.size() >= 10) {
                    Toast.makeText(this, "Máximo 10 fotos permitidas", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap bitmap = null;

                if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                    // Procesar imagen seleccionada de la galería
                    Uri imageUri = data.getData();
                    bitmap = decodeSampledBitmapFromUri(imageUri);
                } else if (requestCode == CAMERA_REQUEST && photoURI != null) {
                    // Procesar imagen capturada con la cámara
                    bitmap = decodeSampledBitmapFromUri(photoURI);
                }

                if (bitmap != null) {
                    String base64Image = processImage(bitmap);
                    if (base64Image != null && !base64Image.isEmpty()) {
                        selectedPhotosBase64.add(base64Image);
                        photoAdapter.notifyItemInserted(selectedPhotosBase64.size() - 1);
                        updatePhotoButtonsState();
                    } else {
                        Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error procesando la imagen: " + e.getMessage(), e);
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri) throws IOException {
        // Obtén las dimensiones de la imagen
        InputStream input = getContentResolver().openInputStream(uri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        // Calcula el factor de escala
        int reqWidth = 800; // Ajusta según sea necesario
        int reqHeight = 800;
        int scaleFactor = Math.min(options.outWidth / reqWidth, options.outHeight / reqHeight);

        // Decodifica la imagen con el factor de escala
        options.inJustDecodeBounds = false;

        input = getContentResolver().openInputStream(uri);

        options.inSampleSize = 2; // Reducir tamaño
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        input.close();

        return bitmap;
    }



    private String processImage(Bitmap bitmap) {
        try {
            // Redimensionar la imagen
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800);

            // Comprimir la imagen
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            // Convertir a Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // Limpiar el Base64 de caracteres problemáticos
            base64Image = base64Image.replace("\n", "")
                    .replace("\r", "")
                    .replace(" ", "")
                    .replace("\t", "");

            // Log del tamaño
            Log.d(TAG, "Tamaño de la imagen en Base64: " + base64Image.length());

            return base64Image;
        } catch (Exception e) {
            Log.e(TAG, "Error procesando imagen: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoURI != null) {
            outState.putString("photoURI", photoURI.toString());
        }
    }

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
            // Validaciones básicas
            if (currentUsername.isEmpty()) {
                Log.e(TAG, "Error: Username vacío al intentar guardar");
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_LONG).show();
                return;
            }

            // Validar título
            String title = titleEditText.getText().toString().trim();
            if (title.isEmpty()) {
                titleEditText.setError("El título es requerido");
                return;
            }

            // Recopilar datos básicos
            String description = descriptionEditText.getText().toString().trim();
            String propertyType = propertyTypeAutoComplete.getText().toString().trim();
            String rulesText = rulesEditText.getText().toString().trim();

            // Validar ubicación
            String location = locationHelper.getCurrentLocation();
            if (location.isEmpty()) {
                Toast.makeText(this, "Por favor seleccione una ubicación válida",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Procesar precio
            String priceStr = "0";
            try {
                String priceText = priceEditText.getText().toString().trim();
                if (!priceText.isEmpty()) {
                    priceStr = priceText;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error en formato de precio: " + e.getMessage());
            }

            // Procesar capacidad
            String capacityStr = "0";
            try {
                String capacityText = capacityEditText.getText().toString().trim();
                if (!capacityText.isEmpty()) {
                    capacityStr = capacityText;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error en formato de capacidad: " + e.getMessage());
            }

            // Procesar amenidades
            List<String> selectedAmenities = getSelectedAmenities();
            String amenitiesStr = selectedAmenities.isEmpty() ? " " : String.join("|", selectedAmenities);

            // Procesar fotos
            String photoStr = selectedPhotosBase64.isEmpty() ? " " : selectedPhotosBase64.get(0);

            // Asegurar valores por defecto para campos vacíos
            description = description.isEmpty() ? " " : description;
            propertyType = propertyType.isEmpty() ? "No especificado" : propertyType;
            rulesText = rulesText.isEmpty() ? " " : rulesText;

            // Reemplazar cualquier coma en los textos por espacios
            title = title.replace(",", " ");
            description = description.replace(",", " ");
            location = location.replace(",", " ");
            propertyType = propertyType.replace(",", " ");
            rulesText = rulesText.replace(",", " ");

            // Construir el mensaje final
            String propertyData = String.format("ADD_PROPERTY:%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    currentUsername.trim(),
                    title,
                    description,
                    priceStr,
                    location,
                    capacityStr,
                    propertyType,
                    amenitiesStr,
                    photoStr,
                    rulesText);

            // Enviar al servidor
            Log.d(TAG, "Enviando datos al servidor...");
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
                                    "Error al guardar la propiedad: " + errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error de comunicación: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(AddPropertyActivity.this,
                                "Error de conexión: " + error,
                                Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error guardando propiedad: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la propiedad: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
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