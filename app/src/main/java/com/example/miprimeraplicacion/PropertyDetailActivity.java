package com.example.miprimeraplicacion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.NumberFormat;
import java.util.Arrays;
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
    private ChipGroup amenitiesChipGroup;
    private TextView rulesTextView;
    private MaterialButton rentButton;
    private ImageButton backButton;

    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        initializeViews();
        setupListeners();

        // Obtener la propiedad del intent
        Property property = (Property) getIntent().getSerializableExtra("property");
        if (property != null) {
            displayPropertyDetails(property);
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
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        rulesTextView = findViewById(R.id.rulesTextView);
        rentButton = findViewById(R.id.rentButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        rentButton.setOnClickListener(v -> {
            // Aquí implementarás la lógica de alquiler
            Toast.makeText(this, "Función de alquiler próximamente", Toast.LENGTH_SHORT).show();
        });
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

        // Amenidades
        amenitiesChipGroup.removeAllViews(); // Limpiar chips existentes
        List<String> amenities = property.getAmenities();
        if (amenities != null && !amenities.isEmpty()) {
            for (String amenity : amenities) {
                if (amenity != null && !amenity.trim().isEmpty()) {
                    Chip chip = new Chip(this);
                    chip.setText(amenity);
                    amenitiesChipGroup.addView(chip);
                }
            }
        }

        // Reglas
        List<String> rules = property.getRules();
        if (rules != null && !rules.isEmpty()) {
            StringBuilder rulesText = new StringBuilder();
            for (String rule : rules) {
                if (rule != null && !rule.trim().isEmpty()) {
                    rulesText.append("• ").append(rule).append("\n");
                }
            }
            rulesTextView.setText(rulesText.toString());
            rulesTextView.setVisibility(View.VISIBLE);
        } else {
            rulesTextView.setVisibility(View.GONE);
        }

        // Fotos
        try {
            List<String> photos = property.getPhotoUrls();
            if (photos != null && !photos.isEmpty()) {
                // Mostrar primera foto
                String firstPhotoBase64 = photos.get(0);
                if (firstPhotoBase64 != null && !firstPhotoBase64.isEmpty()) {
                    byte[] decodedString = Base64.decode(firstPhotoBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (bitmap != null) {
                        propertyImageView.setImageBitmap(bitmap);
                    } else {
                        propertyImageView.setImageResource(R.drawable.placeholder_home);
                    }
                }

                // Mostrar contador si hay más fotos
                if (photos.size() > 1) {
                    photoCountText.setVisibility(View.VISIBLE);
                    photoCountText.setText("+" + (photos.size() - 1) + " fotos");
                } else {
                    photoCountText.setVisibility(View.GONE);
                }
            } else {
                propertyImageView.setImageResource(R.drawable.placeholder_home);
                photoCountText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            propertyImageView.setImageResource(R.drawable.placeholder_home);
            photoCountText.setVisibility(View.GONE);
        }
    }
}