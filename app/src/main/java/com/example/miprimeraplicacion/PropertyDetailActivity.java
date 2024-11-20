package com.example.miprimeraplicacion;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.app.ProgressDialog;

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
    private TextInputEditText checkInDateEdit;
    private TextInputEditText checkOutDateEdit;
    private LinearLayout bookingSummaryLayout;
    private TextView nightsCountText;
    private TextView totalPriceText;

    private static final String TAG = "PropertyDetailActivity";
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
    private Property property;
    private int currentPhotoIndex = 0;

    private Calendar checkInDate;
    private Calendar checkOutDate;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat serverDateFormat;
    private double pricePerNight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        initializeFormatters();
        initializeViews();
        setupListeners();
        initializeDates();

        property = (Property) getIntent().getSerializableExtra("property");
        if (property != null) {
            pricePerNight = property.getPricePerNight();
            displayPropertyDetails(property);
            checkIfPropertyIsRented();
        } else {
            Toast.makeText(this, "Error al cargar los detalles", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeFormatters() {
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private void initializeDates() {
        try {
            checkInDate = Calendar.getInstance();
            checkOutDate = Calendar.getInstance();
            checkOutDate.add(Calendar.DAY_OF_MONTH, 1);

            displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            serverDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            updateDateDisplay(true);
            updateDateDisplay(false);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando fechas: " + e.getMessage());
            Toast.makeText(this, "Error al inicializar fechas",
                    Toast.LENGTH_SHORT).show();
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
        checkInDateEdit = findViewById(R.id.checkInDateEdit);
        checkOutDateEdit = findViewById(R.id.checkOutDateEdit);
        bookingSummaryLayout = findViewById(R.id.bookingSummaryLayout);
        nightsCountText = findViewById(R.id.nightsCountText);
        totalPriceText = findViewById(R.id.totalPriceText);

        rentButton.setEnabled(false);
        rentButton.setText("Selecciona las fechas para alquilar");
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        propertyImageView.setOnClickListener(v -> showNextPhoto());
        rentButton.setOnClickListener(v -> showRentConfirmationDialog());
        checkInDateEdit.setOnClickListener(v -> showDatePicker(true));
        checkOutDateEdit.setOnClickListener(v -> showDatePicker(false));
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
                    propertyImageView.setImageBitmap(bitmap);
                } else {
                    propertyImageView.setImageResource(R.drawable.placeholder_home);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying photo: " + e.getMessage());
            propertyImageView.setImageResource(R.drawable.placeholder_home);
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

    private void showDatePicker(boolean isCheckIn) {
        Calendar minDate = Calendar.getInstance();
        Calendar currentSelectedDate = isCheckIn ? checkInDate : checkOutDate;

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    if (isCheckIn) {
                        if (selectedDate.before(minDate)) {
                            Toast.makeText(this, "No puedes seleccionar fechas pasadas", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (checkOutDate != null && selectedDate.after(checkOutDate)) {
                            checkOutDate.setTime(selectedDate.getTime());
                            checkOutDate.add(Calendar.DAY_OF_MONTH, 1);
                            updateDateDisplay(false); // Actualizar la fecha de salida
                        }

                        checkInDate.setTime(selectedDate.getTime());
                    } else { // Es check-out
                        if (selectedDate.before(checkInDate) || isSameDay(selectedDate, checkInDate)) {
                            Toast.makeText(this, "La fecha de salida debe ser al menos un día después de la entrada",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        checkOutDate.setTime(selectedDate.getTime());
                    }

                    updateDateDisplay(isCheckIn);
                    updateBookingSummary();
                    checkAvailability();
                },
                currentSelectedDate.get(Calendar.YEAR),
                currentSelectedDate.get(Calendar.MONTH),
                currentSelectedDate.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        dialog.show();
    }

    private boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH);
    }

    private void updateDateDisplay(boolean isCheckIn) {
        try {
            if (isCheckIn) {
                String formattedDate = displayDateFormat.format(checkInDate.getTime());
                checkInDateEdit.setText(formattedDate);
            } else {
                String formattedDate = displayDateFormat.format(checkOutDate.getTime());
                checkOutDateEdit.setText(formattedDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando display de fecha: " + e.getMessage());
        }
    }

    private void updateBookingSummary() {
        long diffInMillies = checkOutDate.getTimeInMillis() - checkInDate.getTimeInMillis();
        int nights = (int) (diffInMillies / (1000 * 60 * 60 * 24));
        double totalPrice = nights * pricePerNight;

        nightsCountText.setText(String.format("%d noche%s", nights, nights > 1 ? "s" : ""));
        totalPriceText.setText(String.format("Total: %s", priceFormatter.format(totalPrice)));

        bookingSummaryLayout.setVisibility(View.VISIBLE);
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

    private void checkAvailability() {
        if (property == null) return;

        try {
            String checkIn = serverDateFormat.format(checkInDate.getTime());
            String checkOut = serverDateFormat.format(checkOutDate.getTime());
            String propertyId = property.getId().trim();

            // Log para debug
            Log.d(TAG, "Verificando disponibilidad - PropertyID: " + propertyId +
                    ", Check-in: " + checkIn + ", Check-out: " + checkOut);

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Verificando disponibilidad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String request = String.format("CHECK_PROPERTY_AVAILABILITY:%s,%s,%s",
                    propertyId, checkIn, checkOut);

            ServerCommunication.sendToServer(request, new ServerCommunication.ServerResponseListener() {
                @Override
                public void onResponse(final String response) {
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // Log para debug
                            Log.d(TAG, "Respuesta del servidor: " + response);

                            if (response == null || response.trim().isEmpty()) {
                                throw new Exception("Respuesta vacía del servidor");
                            }

                            // Normalizar la respuesta eliminando espacios y saltos de línea
                            String normalizedResponse = response.trim();

                            if (normalizedResponse.equals("AVAILABLE")) {
                                rentButton.setEnabled(true);
                                rentButton.setText("Alquilar propiedad");
                                rentButton.setBackgroundTintList(ColorStateList.valueOf(
                                        getResources().getColor(R.color.primary_blue)));

                                // Mostrar mensaje de disponibilidad
                                Toast.makeText(PropertyDetailActivity.this,
                                        "Propiedad disponible para las fechas seleccionadas",
                                        Toast.LENGTH_SHORT).show();

                            } else if (normalizedResponse.equals("UNAVAILABLE")) {
                                rentButton.setEnabled(false);
                                rentButton.setText("No disponible para estas fechas");
                                rentButton.setBackgroundTintList(ColorStateList.valueOf(
                                        getResources().getColor(R.color.gray)));

                                // Mostrar mensaje de no disponibilidad
                                Toast.makeText(PropertyDetailActivity.this,
                                        "La propiedad no está disponible para las fechas seleccionadas",
                                        Toast.LENGTH_LONG).show();

                            } else if (normalizedResponse.startsWith("ERROR:")) {
                                throw new Exception(normalizedResponse.substring(6));
                            } else {
                                throw new Exception("Respuesta no reconocida: " + normalizedResponse);
                            }

                            // Actualizar resumen de fechas
                            updateBookingSummary();

                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando respuesta: " + e.getMessage());
                            Toast.makeText(PropertyDetailActivity.this,
                                    "Error verificando disponibilidad: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            rentButton.setEnabled(false);
                            rentButton.setText("Error al verificar disponibilidad");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Log.e(TAG, "Error de conexión: " + error);
                        Toast.makeText(PropertyDetailActivity.this,
                                "Error de conexión al verificar disponibilidad: " + error,
                                Toast.LENGTH_LONG).show();
                        rentButton.setEnabled(false);
                        rentButton.setText("Error de conexión");
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error en checkAvailability: " + e.getMessage());
            Toast.makeText(this, "Error al verificar disponibilidad: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            rentButton.setEnabled(false);
            rentButton.setText("Error al verificar");
            e.printStackTrace();
        }
    }

    private void showRentConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rent_confirmation, null);

        TextView datesText = dialogView.findViewById(R.id.datesText);
        TextView priceText = dialogView.findViewById(R.id.priceText);

        datesText.setText(String.format("Del %s al %s",
                displayDateFormat.format(checkInDate.getTime()),
                displayDateFormat.format(checkOutDate.getTime())));

        long diffInMillies = checkOutDate.getTimeInMillis() - checkInDate.getTimeInMillis();
        int nights = (int) (diffInMillies / (1000 * 60 * 60 * 24));
        double totalPrice = nights * pricePerNight;

        priceText.setText(String.format("Total: %s", priceFormatter.format(totalPrice)));

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

        if (property == null || property.getId() == null || property.getId().trim().isEmpty()) {
            Toast.makeText(this, "Error: Propiedad no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        String propertyId = property.getId().trim().replace(":", "");
        String checkIn = serverDateFormat.format(checkInDate.getTime());
        String checkOut = serverDateFormat.format(checkOutDate.getTime());

        String rentRequest = String.format("RENT_PROPERTY:%s,%s,%s,%s",
                propertyId, username, checkIn, checkOut);

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
        titleTextView.setText(property.getTitle());

        priceFormatter.setMaximumFractionDigits(0);
        String formattedPrice = priceFormatter.format(property.getPricePerNight());
        priceTextView.setText(formattedPrice + " por noche");

        if (property.getOwnerName() != null) {
            ownerNameTextView.setText(property.getOwnerName());
            ownerNameTextView.setVisibility(View.VISIBLE);
        } else {
            ownerNameTextView.setVisibility(View.GONE);
        }

        if (property.getLocation() != null && !property.getLocation().isEmpty()) {
            locationTextView.setText("📍 " + property.getLocation());
            locationTextView.setVisibility(View.VISIBLE);
        } else {
            locationTextView.setVisibility(View.GONE);
        }

        capacityTextView.setText("👥 " + property.getCapacity() + " personas");

        if (property.getPropertyType() != null && !property.getPropertyType().isEmpty()) {
            typeTextView.setText("🏠 " + property.getPropertyType());
            typeTextView.setVisibility(View.VISIBLE);
        } else {
            typeTextView.setVisibility(View.GONE);
        }

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
                displayPhoto(photos.get(0));
                updatePhotoCounter(photos.size());
            } else {
                propertyImageView.setImageResource(R.drawable.placeholder_home);
                photoCountText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up photos: " + e.getMessage());
            propertyImageView.setImageResource(R.drawable.placeholder_home);
            photoCountText.setVisibility(View.GONE);
        }
    }
}