package com.example.miprimeraplicacion;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PropertyDetailActivity extends AppCompatActivity {
    private ImageView propertyImageView;
    private TextView photoCountText;
    private TextView titleTextView;
    private TextView priceTextView;
    private TextView ownerNameTextView;
    private TextView locationTextView;
    private TextView capacityTextView;
    private TextView typeTextView;
    private TextView descriptionTextView;
    private TextView rulesTextView;
    private MaterialButton rentButton;
    private ImageButton backButton;
    private TextView amenitiesTextView;


    private static final String TAG = "PropertyDetailActivity";
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
    private Property property;
    private int currentPhotoIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        initializeViews();
        setupListeners();

        property = (Property) getIntent().getSerializableExtra("property");
        if (property != null) {
            displayPropertyDetails(property);
            checkIfPropertyIsRented();
        } else {
            Toast.makeText(this, "Error al cargar los detalles", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        propertyImageView = findViewById(R.id.propertyImageView);

        photoCountText = findViewById(R.id.photoCountText);
        titleTextView = findViewById(R.id.titleTextView);
        priceTextView = findViewById(R.id.priceTextView);
        ownerNameTextView = findViewById(R.id.ownerNameTextView);
        locationTextView = findViewById(R.id.locationTextView);
        capacityTextView = findViewById(R.id.capacityTextView);
        typeTextView = findViewById(R.id.typeTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        amenitiesTextView = findViewById(R.id.amenitiesTextView);
        rulesTextView = findViewById(R.id.rulesTextView);
        rentButton = findViewById(R.id.rentButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        propertyImageView.setOnClickListener(v -> showNextPhoto());
        rentButton.setOnClickListener(v -> showRentConfirmationDialog());
    }

    private void showNextPhoto() {
        if (property != null && property.hasPhotos()) {
            List<String> photos = property.getPhotoUrls();
            currentPhotoIndex = (currentPhotoIndex + 1) % photos.size();
            displayPhoto(photos.get(currentPhotoIndex));
            updatePhotoCounter(photos.size());
        }
    }

    private void displayPhoto(String base64Photo) {
        try {
            if (base64Photo != null && !base64Photo.isEmpty()) {
                byte[] decodedString = Base64.decode(base64Photo, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (bitmap != null) {
                    runOnUiThread(() -> propertyImageView.setImageBitmap(bitmap));
                } else {
                    runOnUiThread(() -> propertyImageView.setImageResource(R.drawable.placeholder_home));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying photo: " + e.getMessage());
            runOnUiThread(() -> propertyImageView.setImageResource(R.drawable.placeholder_home));
        }
    }




    private void updatePhotoCounter(int totalPhotos) {
        if (totalPhotos > 1) {
            photoCountText.setVisibility(View.VISIBLE);
            photoCountText.setText((currentPhotoIndex + 1) + "/" + totalPhotos);
        } else {
            photoCountText.setVisibility(View.GONE);
        }
    }

    private void checkIfPropertyIsRented() {
        String username = getUsernameFromPrefs();
        String checkRequest = String.format("CHECK_PROPERTY_STATUS:%s,%s", property.getId(), username);

        ServerCommunication.sendToServer(checkRequest, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    if (response.startsWith("RENTED")) {
                        rentButton.setEnabled(false);
                        rentButton.setText("Propiedad no disponible");
                        rentButton.setBackgroundTintList(ColorStateList.valueOf(
                                getResources().getColor(R.color.gray)));
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking property status: " + error);
            }
        });
    }

    private void showRentConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rent_confirmation, null);
        builder.setView(dialogView)
                .setPositiveButton("Confirmar", (dialog, which) -> rentProperty())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void rentProperty() {
        String username = getUsernameFromPrefs();
        if (username.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que tengamos un ID válido
        if (property == null || property.getId() == null || property.getId().trim().isEmpty()) {
            Toast.makeText(this, "Error: Propiedad no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiar el ID de cualquier carácter no deseado
        String propertyId = property.getId().trim().replace(":", "");
        Log.d(TAG, "Intentando alquilar propiedad ID: " + propertyId + " para usuario: " + username);

        // Asegurarnos de que el formato del mensaje sea correcto
        String rentRequest = "RENT_PROPERTY:" + propertyId + "," + username;

        ServerCommunication.sendToServer(rentRequest, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Respuesta del servidor para alquiler: " + response);
                runOnUiThread(() -> {
                    if (response.startsWith("SUCCESS")) {
                        showSuccessAnimation();
                        rentButton.setEnabled(false);
                        rentButton.setText("Propiedad alquilada");
                        rentButton.setBackgroundTintList(ColorStateList.valueOf(
                                getResources().getColor(R.color.gray)));
                    } else {
                        String error = response.startsWith("ERROR:") ?
                                response.substring(6) : response;
                        Log.e(TAG, "Error en alquiler: " + error);
                        Toast.makeText(PropertyDetailActivity.this,
                                "Error al alquilar: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error en alquiler: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PropertyDetailActivity.this,
                            "Error de conexión: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showSuccessAnimation() {
        SuccessAnimationDialog successDialog = new SuccessAnimationDialog();
        successDialog.setDismissListener(() -> {
            setResult(RESULT_OK);
            finish();
        });
        successDialog.show(getSupportFragmentManager(), "success_dialog");
    }

    private String getUsernameFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getString("username", "");
    }

    private void displayPropertyDetails(Property property) {
        // Título
        titleTextView.setText(property.getTitle());

        // Precio
        priceFormatter.setMaximumFractionDigits(0);
        String formattedPrice = priceFormatter.format(property.getPricePerNight());
        priceTextView.setText(formattedPrice + " por noche");

        // Propietario
        if (property.getOwnerName() != null) {
            ownerNameTextView.setText(property.getOwnerName());
            ownerNameTextView.setVisibility(View.VISIBLE);
        } else {
            ownerNameTextView.setVisibility(View.GONE);
        }

        // Ubicación
        if (property.getLocation() != null && !property.getLocation().isEmpty()) {
            locationTextView.setText("📍 " + property.getLocation());
            locationTextView.setVisibility(View.VISIBLE);
        } else {
            locationTextView.setVisibility(View.GONE);
        }

        // Capacidad
        capacityTextView.setText("👥 " + property.getCapacity() + " personas");

        // Tipo de propiedad
        if (property.getPropertyType() != null && !property.getPropertyType().isEmpty()) {
            typeTextView.setText("🏠 " + property.getPropertyType());
            typeTextView.setVisibility(View.VISIBLE);
        } else {
            typeTextView.setVisibility(View.GONE);
        }

        // Descripción
        if (property.getDescription() != null && !property.getDescription().isEmpty()) {
            descriptionTextView.setText(property.getDescription());
            descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            descriptionTextView.setVisibility(View.GONE);
        }

        setupAmenities(property);
        setupRules(property);
        setupPhotos(property);
    }

    private void setupAmenities(Property property) {
        if (property.hasAmenities()) {
            StringBuilder amenitiesText = new StringBuilder();
            List<String> amenities = property.getAmenities();

            for (int i = 0; i < amenities.size(); i++) {
                String amenity = amenities.get(i).trim();
                if (!amenity.isEmpty()) {
                    amenitiesText.append("• ").append(amenity);
                    if (i < amenities.size() - 1) {
                        amenitiesText.append("\n");
                    }
                }
            }

            if (amenitiesText.length() > 0) {
                amenitiesTextView.setText(amenitiesText.toString());
                amenitiesTextView.setVisibility(View.VISIBLE);
            } else {
                amenitiesTextView.setVisibility(View.GONE);
            }
        } else {
            amenitiesTextView.setVisibility(View.GONE);
        }
    }

    private void setupRules(Property property) {
        if (property.hasRules()) {
            StringBuilder rulesText = new StringBuilder();
            List<String> rules = property.getRules();

            for (int i = 0; i < rules.size(); i++) {
                String rule = rules.get(i).trim();
                if (!rule.isEmpty()) {
                    rulesText.append("• ").append(rule);
                    if (i < rules.size() - 1) {
                        rulesText.append("\n");
                    }
                }
            }

            if (rulesText.length() > 0) {
                rulesTextView.setText(rulesText.toString());
                rulesTextView.setVisibility(View.VISIBLE);
            } else {
                rulesTextView.setVisibility(View.GONE);
            }
        } else {
            rulesTextView.setVisibility(View.GONE);
        }
    }

    private void setupPhotos(Property property) {
        try {
            List<String> photos = property.getPhotoUrls();
            if (photos != null && !photos.isEmpty()) {
                displayPhoto(photos.get(0)); // Mostrar solo la primera imagen
                photoCountText.setVisibility(View.GONE); // Ocultar contador de fotos
            } else {
                propertyImageView.setImageResource(R.drawable.placeholder_home);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up photos: " + e.getMessage());
            propertyImageView.setImageResource(R.drawable.placeholder_home);
        }
    }

}