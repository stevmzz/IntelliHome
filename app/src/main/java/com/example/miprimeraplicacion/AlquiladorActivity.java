package com.example.miprimeraplicacion;

import com.example.miprimeraplicacion.CostaRicaLocations;
import com.example.miprimeraplicacion.LocationAutocompleteHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.Gravity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlquiladorActivity extends AppCompatActivity implements PropertyAdapter.OnPropertyClickListener {

    // Vistas principales
    private ImageView logoImageView;
    private Button topButton;
    private AutoCompleteTextView searchEditText;
    private Button filterButton;
    private LinearLayout emptyView;

    // RecyclerView y adaptador
    private RecyclerView propertiesRecyclerView;
    private PropertyAdapter propertyAdapter;

    // Componentes del menú de filtros
    private MaterialCardView filterMenu;
    private TextInputEditText priceRangeInput;
    private TextInputEditText peopleCountInput;
    private TextInputEditText roomsCountInput;
    private SwitchMaterial petsSwitch;
    private Button clearFiltersButton;
    private Button applyFiltersButton;
    private boolean isFilterMenuVisible = false;

    // Componentes de búsqueda
    private ArrayAdapter<String> searchAdapter;
    private List<String> locations;
    private LocationAutocompleteHelper locationHelper;


    private List<Property> allProperties = new ArrayList<>();
    private double maxPrice = Double.MAX_VALUE;
    private int peopleCount = 0;
    private int roomsCount = 0;
    private boolean allowPets = false;
    private String currentSearchLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alquilador);

        initializeViews();
        setupRecyclerView();
        setupSearchAutocomplete();
        setupListeners();
        loadProperties();
    }

    private void initializeViews() {
        // Vistas principales
        logoImageView = findViewById(R.id.logoImageView);
        topButton = findViewById(R.id.topButton);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        emptyView = findViewById(R.id.emptyView);
        propertiesRecyclerView = findViewById(R.id.propertiesRecyclerView);

        // Vistas del menú de filtros
        filterMenu = findViewById(R.id.filterMenu);
        priceRangeInput = findViewById(R.id.priceRangeInput);
        peopleCountInput = findViewById(R.id.peopleCountInput);
        roomsCountInput = findViewById(R.id.roomsCountInput);
        petsSwitch = findViewById(R.id.petsSwitch);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);

        // Inicialmente ocultamos el menú de filtros
        filterMenu.setVisibility(View.GONE);
    }

    private void setupSearchAutocomplete() {
        locationHelper = new LocationAutocompleteHelper(
                this,
                searchEditText,
                location -> {
                    currentSearchLocation = location;
                    applyFilters();
                }
        );
    }

    private void setupRecyclerView() {
        propertyAdapter = new PropertyAdapter(this, this);
        propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        propertiesRecyclerView.setAdapter(propertyAdapter);
        // Opcional: añadir decoración para espaciado entre items
        propertiesRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
                outRect.bottom = 8;
                outRect.left = 8;
                outRect.right = 8;
            }
        });
    }

    private void loadProperties() {
        emptyView.setVisibility(View.GONE);

        ServerCommunication.sendToServer("GET_ALL_PROPERTIES", new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    if (response.startsWith("SUCCESS:")) {
                        String propertiesData = response.substring(8);
                        allProperties = parsePropertiesResponse(propertiesData);
                        applyFilters(); // Aplicar filtros a las propiedades cargadas
                    } else {
                        Toast.makeText(AlquiladorActivity.this,
                                "Error al cargar propiedades", Toast.LENGTH_SHORT).show();
                        updateEmptyView(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AlquiladorActivity.this,
                            "Error de conexión: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyView(true);
                });
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        propertiesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private List<Property> parsePropertiesResponse(String response) {
        List<Property> properties = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return properties;
        }

        String[] propertyStrings = response.split(";");
        for (String propertyStr : propertyStrings) {
            if (propertyStr.trim().isEmpty()) continue;

            try {
                String[] data = propertyStr.split("\\|");
                Property property = new Property();

                // Campos básicos
                property.setId(data[0].trim());
                property.setOwnerName(data[1].trim());  // username del propietario
                property.setTitle(data[2].trim());
                property.setDescription(data[3].trim());

                // Campos numéricos
                try {
                    property.setPricePerNight(Double.parseDouble(data[4].trim()));
                } catch (NumberFormatException e) {
                    property.setPricePerNight(0.0);
                }

                property.setLocation(data[5].trim());

                try {
                    property.setCapacity(Integer.parseInt(data[6].trim()));
                } catch (NumberFormatException e) {
                    property.setCapacity(0);
                }

                property.setPropertyType(data[7].trim());

                if (data.length > 8 && !data[8].trim().isEmpty()) {
                    List<String> amenities = new ArrayList<>();
                    for (String item : data[8].split("\\|")) {
                        amenities.add(item.trim());
                    }
                    property.setAmenities(amenities);
                }

                if (data.length > 9 && !data[9].trim().isEmpty()) {
                    List<String> photoUrls = new ArrayList<>();
                    for (String url : data[9].split("\\|")) {
                        photoUrls.add(url.trim());
                    }
                    property.setPhotoUrls(photoUrls);
                }

                if (data.length > 10 && !data[10].trim().isEmpty()) {
                    List<String> rules = new ArrayList<>();
                    for (String rule : data[10].split("\\|")) {
                        rules.add(rule.trim());
                    }
                    property.setRules(rules);
                }

                if (data.length > 11 && !data[11].trim().isEmpty()) {
                    property.setCreationDate(data[11].trim());
                }

                properties.add(property);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return properties;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.top_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                Toast.makeText(this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_monitoring) {
                Intent intent = new Intent(this, MonitoreoActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.menu_settings) {
                Toast.makeText(this, "Ajustes seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_history) {
                Intent intent = new Intent(this, RentHistoryActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void initializeLocations() {
        locations = CostaRicaLocations.getAllLocations();
    }

    private void filterLocations(String text) {
        if (text == null || text.isEmpty()) {
            searchAdapter.clear();
            searchAdapter.notifyDataSetChanged();
            return;
        }

        List<String> filteredLocations = CostaRicaLocations.searchLocations(text);
        searchAdapter.clear();
        searchAdapter.addAll(filteredLocations);
        searchAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        logoImageView.setOnClickListener(v -> {
            hideKeyboard(v);
            recreate();
        });

        topButton.setOnClickListener(v -> {
            hideKeyboard(v);
            showPopupMenu(v);
        });

        filterButton.setOnClickListener(v -> {
            hideKeyboard(v);
            toggleFilterMenu();
        });

        clearFiltersButton.setOnClickListener(v -> {
            maxPrice = Double.MAX_VALUE;
            peopleCount = 0;
            roomsCount = 0;
            allowPets = false;
            currentSearchLocation = "";

            // Limpiar los campos de texto
            priceRangeInput.setText("");
            peopleCountInput.setText("");
            roomsCountInput.setText("");
            petsSwitch.setChecked(false);
            searchEditText.setText("");

            // Recargar todas las propiedades
            applyFilters();
        });

        applyFiltersButton.setOnClickListener(v -> {
            try {
                // Obtener valores de los filtros
                String priceText = priceRangeInput.getText().toString();
                maxPrice = priceText.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(priceText);

                String peopleText = peopleCountInput.getText().toString();
                peopleCount = peopleText.isEmpty() ? 0 : Integer.parseInt(peopleText);

                String roomsText = roomsCountInput.getText().toString();
                roomsCount = roomsText.isEmpty() ? 0 : Integer.parseInt(roomsText);

                allowPets = petsSwitch.isChecked();

                // Aplicar los filtros
                applyFilters();
                toggleFilterMenu();

                // Mostrar resumen de filtros aplicados
                showFilterSummary();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Por favor, ingrese valores numéricos válidos",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFilterSummary() {
        StringBuilder summary = new StringBuilder("Filtros aplicados:\n");
        if (maxPrice != Double.MAX_VALUE) {
            summary.append("• Precio máximo: ₡").append(String.format("%,.0f", maxPrice)).append("\n");
        }
        if (peopleCount > 0) {
            summary.append("• Capacidad mínima: ").append(peopleCount).append(" personas\n");
        }
        if (roomsCount > 0) {
            summary.append("• Habitaciones mínimas: ").append(roomsCount).append("\n");
        }
        if (!currentSearchLocation.isEmpty()) {
            summary.append("• Ubicación: ").append(currentSearchLocation).append("\n");
        }
        if (allowPets) {
            summary.append("• Permite mascotas\n");
        }

        Toast.makeText(this, summary.toString(), Toast.LENGTH_LONG).show();
    }

    private void toggleFilterMenu() {
        if (isFilterMenuVisible) {
            Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
            filterMenu.startAnimation(slideUp);
            filterMenu.setVisibility(View.GONE);
        } else {
            filterMenu.setVisibility(View.VISIBLE);
            Animation slideDown = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            filterMenu.startAnimation(slideDown);
        }
        isFilterMenuVisible = !isFilterMenuVisible;
    }

    private void clearFilters() {
        priceRangeInput.setText("");
        peopleCountInput.setText("");
        roomsCountInput.setText("");
        petsSwitch.setChecked(false);
        locationHelper.clearLocation();
        currentSearchLocation = "";
    }

    private void applyFilters() {
        List<Property> filteredProperties = new ArrayList<>(allProperties);

        // Filtrar por precio máximo
        if (maxPrice != Double.MAX_VALUE) {
            filteredProperties = filteredProperties.stream()
                    .filter(property -> property.getPricePerNight() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Filtrar por cantidad de personas
        if (peopleCount > 0) {
            filteredProperties = filteredProperties.stream()
                    .filter(property -> property.getCapacity() >= peopleCount)
                    .collect(Collectors.toList());
        }

        // Filtrar por cantidad de habitaciones (asumiendo que agregarás este campo al modelo Property)
        if (roomsCount > 0) {
            filteredProperties = filteredProperties.stream()
                    .filter(property -> property.getRooms() >= roomsCount)
                    .collect(Collectors.toList());
        }

        // Filtrar por ubicación
        String location = locationHelper.getCurrentLocation();
        if (!location.isEmpty()) {
            filteredProperties = filteredProperties.stream()
                    .filter(property -> property.getLocation().equalsIgnoreCase(location))
                    .collect(Collectors.toList());
        }

        // Filtrar por mascotas (asumiendo que agregarás este campo al modelo Property)
        if (allowPets) {
            filteredProperties = filteredProperties.stream()
                    .filter(Property::getAllowsPets)
                    .collect(Collectors.toList());
        }

        // Actualizar el RecyclerView con las propiedades filtradas
        propertyAdapter.updateProperties(filteredProperties);
        updateEmptyView(filteredProperties.isEmpty());
    }

    @Override
    public void onPropertyClick(Property property) {
        Intent intent = new Intent(this, PropertyDetailActivity.class);
        intent.putExtra("property", property);
        startActivity(intent);
    }
}