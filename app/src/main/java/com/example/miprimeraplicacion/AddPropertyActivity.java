package com.example.miprimeraplicacion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddPropertyActivity extends AppCompatActivity {
    private static final int PICK_IMAGES_REQUEST = 1;
    private EditText titleEditText, descriptionEditText, priceEditText, locationEditText, capacityEditText;
    private Spinner propertyTypeSpinner;
    private ChipGroup amenitiesChipGroup;
    private RecyclerView photosRecyclerView;
    private Button addPhotosButton, saveButton;
    private List<Uri> selectedPhotos = new ArrayList<>();
    private List<String> amenitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_property);

        initializeViews();
        setupSpinners();
        setupAmenities();
        setupListeners();
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);
        locationEditText = findViewById(R.id.locationEditText);
        capacityEditText = findViewById(R.id.capacityEditText);
        propertyTypeSpinner = findViewById(R.id.propertyTypeSpinner);
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        addPhotosButton = findViewById(R.id.addPhotosButton);
        saveButton = findViewById(R.id.saveButton);

        photosRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupSpinners() {
        String[] propertyTypes = {"Rústica", "Tecnológica", "Moderna", "Mansión"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, propertyTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        propertyTypeSpinner.setAdapter(adapter);
    }

    private void setupAmenities() {
        amenitiesList = Arrays.asList(
                "Cocina equipada", "Aire acondicionado", "Calefacción", "Wi-Fi gratuito",
                "TV por cable", "Lavadora/Secadora", "Piscina", "Jardín", "Barbacoa",
                "Terraza", "Gimnasio", "Garaje", "Sistema de seguridad", "Baño en suite",
                "Muebles exterior", "Microondas", "Lavavajillas", "Cafetera", "Ropa de cama",
                "Áreas comunes", "Camas adicionales", "Servicio limpieza", "Transporte público",
                "Mascotas permitidas", "Cerca de comercios", "Suelo radiante", "Área trabajo",
                "Sistema entretenimiento", "Chimenea", "Internet alta velocidad"
        );

        for (String amenity : amenitiesList) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCheckable(true);
            amenitiesChipGroup.addView(chip);
        }
    }

    private void setupListeners() {
        addPhotosButton.setOnClickListener(v -> {
            if (selectedPhotos.size() >= 10) {
                Toast.makeText(this, "Máximo 10 fotos permitidas", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Seleccionar fotos"), PICK_IMAGES_REQUEST);
        });

        saveButton.setOnClickListener(v -> saveProperty());
    }

    private void saveProperty() {
        if (!validateInputs()) {
            return;
        }

        Property property = new Property();
        property.setTitle(titleEditText.getText().toString());
        property.setDescription(descriptionEditText.getText().toString());
        property.setPricePerNight(Double.parseDouble(priceEditText.getText().toString()));
        property.setLocation(locationEditText.getText().toString());
        property.setCapacity(Integer.parseInt(capacityEditText.getText().toString()));
        property.setPropertyType(propertyTypeSpinner.getSelectedItem().toString());

        // Recoger amenidades seleccionadas
        List<String> selectedAmenities = new ArrayList<>();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedAmenities.add(chip.getText().toString());
            }
        }
        property.setAmenities(selectedAmenities);

        // TODO: Implementar lógica para subir fotos al servidor
        // Por ahora, solo guardamos las URIs localmente
        List<String> photoUrls = new ArrayList<>();
        for (Uri uri : selectedPhotos) {
            photoUrls.add(uri.toString());
        }
        property.setPhotoUrls(photoUrls);

        // Enviar al servidor
        String propertyData = formatPropertyData(property);
        ServerCommunication.sendToServer(propertyData, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    if (response.startsWith("SUCCESS")) {
                        Toast.makeText(AddPropertyActivity.this, "Propiedad guardada exitosamente", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddPropertyActivity.this, "Error al guardar la propiedad: " + response, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddPropertyActivity.this, "Error de conexión: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean validateInputs() {
        if (titleEditText.getText().toString().isEmpty()) {
            titleEditText.setError("Campo requerido");
            return false;
        }
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "Debe agregar al menos una foto", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Agregar más validaciones según sea necesario
        return true;
    }

    private String formatPropertyData(Property property) {
        // Formato: ADD_PROPERTY:título,descripción,precio,ubicación,capacidad,tipo,amenidades,fotos
        StringBuilder data = new StringBuilder("ADD_PROPERTY:");
        data.append(property.getTitle()).append(",")
                .append(property.getDescription()).append(",")
                .append(property.getPricePerNight()).append(",")
                .append(property.getLocation()).append(",")
                .append(property.getCapacity()).append(",")
                .append(property.getPropertyType()).append(",")
                .append(String.join("|", property.getAmenities())).append(",")
                .append(String.join("|", property.getPhotoUrls()));

        return data.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 10 - selectedPhotos.size());
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedPhotos.add(imageUri);
                }
            } else if (data.getData() != null) {
                selectedPhotos.add(data.getData());
            }
            updatePhotosRecyclerView();
        }
    }

    private void updatePhotosRecyclerView() {
        // TODO: Implementar un adaptador para mostrar las fotos seleccionadas
    }
}