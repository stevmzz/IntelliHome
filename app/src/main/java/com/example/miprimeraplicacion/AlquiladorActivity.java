package com.example.miprimeraplicacion;

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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlquiladorActivity extends AppCompatActivity implements PropertyAdapter.OnPropertyClickListener {

    private ImageView logoImageView;
    private Button topButton;
    private AutoCompleteTextView searchEditText;
    private Button filterButton;
    private LinearLayout emptyView;
    private RecyclerView propertiesRecyclerView;
    private PropertyAdapter propertyAdapter;
    private MaterialCardView filterMenu;
    private TextInputEditText priceRangeInput;
    private TextInputEditText peopleCountInput;
    private TextInputEditText roomsCountInput;
    private SwitchMaterial petsSwitch;
    private Button clearFiltersButton;
    private Button applyFiltersButton;
    private boolean isFilterMenuVisible = false;
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
        setupListeners();  // Asegúrate de que este método esté definido en tu clase
        loadProperties();
    }

    private void initializeViews() {
        logoImageView = findViewById(R.id.logoImageView);
        topButton = findViewById(R.id.topButton);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        emptyView = findViewById(R.id.emptyView);
        propertiesRecyclerView = findViewById(R.id.propertiesRecyclerView);
        filterMenu = findViewById(R.id.filterMenu);
        priceRangeInput = findViewById(R.id.priceRangeInput);
        peopleCountInput = findViewById(R.id.peopleCountInput);
        roomsCountInput = findViewById(R.id.roomsCountInput);
        petsSwitch = findViewById(R.id.petsSwitch);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);

        filterMenu.setVisibility(View.GONE); // Menú de filtros oculto al inicio
    }

    private void setupSearchAutocomplete() {
        locationHelper = new LocationAutocompleteHelper(this, searchEditText, location -> {
            currentSearchLocation = location;
            applyFilters();
        });

        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, locations);
        searchEditText.setAdapter(searchAdapter);
        searchEditText.setThreshold(1);  // Empieza a buscar después de 1 carácter

        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = (String) parent.getItemAtPosition(position);
            currentSearchLocation = selectedLocation;

            // Obtener la provincia de la ubicación seleccionada
            String province = CostaRicaLocations.getProvinceByLocation(selectedLocation);
            List<String> provinceLocations = CostaRicaLocations.getLocationsByProvince(province);
            updateSearchSuggestions(provinceLocations);
        });
    }

    private void updateSearchSuggestions(List<String> provinceLocations) {
        searchAdapter.clear();
        searchAdapter.addAll(provinceLocations);
        searchAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        propertyAdapter = new PropertyAdapter(this, this);
        propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        propertiesRecyclerView.setAdapter(propertyAdapter);
    }

    private void setupListeners() {
        // Aquí puedes agregar los listeners para los botones, como el filtro
        filterButton.setOnClickListener(v -> toggleFilterMenu());
        applyFiltersButton.setOnClickListener(v -> applyFilters());
        clearFiltersButton.setOnClickListener(v -> clearFilters());
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
                        applyFilters();
                    } else {
                        Toast.makeText(AlquiladorActivity.this, "Error al cargar propiedades", Toast.LENGTH_SHORT).show();
                        updateEmptyView(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AlquiladorActivity.this, "Error de conexión: " + error, Toast.LENGTH_SHORT).show();
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
        // Método que convierte la respuesta del servidor a una lista de propiedades
        List<Property> properties = new ArrayList<>();
        // (parseo de las propiedades)
        return properties;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showFilterMenu() {

        isFilterMenuVisible = true;
    }

    private void hideFilterMenu() {

        isFilterMenuVisible = false;
    }

    private void applyFilters() {
        List<Property> filteredProperties = new ArrayList<>(allProperties);

        if (currentSearchLocation != null && !currentSearchLocation.isEmpty()) {
            filteredProperties = filteredProperties.stream()
                    .filter(property -> property.getLocation().contains(currentSearchLocation))
                    .collect(Collectors.toList());
        }

        propertyAdapter.updateProperties(filteredProperties);
    }

    @Override
    public void onPropertyClick(Property property) {
        // Acción al seleccionar una propiedad
    }

    private void toggleFilterMenu() {
        if (isFilterMenuVisible) {
            hideFilterMenu();
        } else {
            showFilterMenu();
        }
    }

    private void clearFilters() {
        // Limpiar filtros
    }
}
