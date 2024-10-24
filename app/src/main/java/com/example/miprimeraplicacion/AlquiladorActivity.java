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
import android.text.TextWatcher;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.Gravity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

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

    private void setupSearchAutocomplete() {
        // Inicializar la lista de ubicaciones
        locations = new ArrayList<>();
        initializeLocations();

        // Crear y configurar el adaptador
        searchAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                new ArrayList<>()
        );
        searchEditText.setAdapter(searchAdapter);

        // Configurar el TextWatcher para filtrar ubicaciones
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLocations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Manejar la selección de ubicación
        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = (String) parent.getItemAtPosition(position);
            Toast.makeText(this, "Ubicación seleccionada: " + selectedLocation,
                    Toast.LENGTH_SHORT).show();
            hideKeyboard(searchEditText);
            // Aquí puedes filtrar las propiedades por ubicación
        });
    }

    private void loadProperties() {
        // Mostrar un loading si lo deseas
        emptyView.setVisibility(View.GONE);

        ServerCommunication.sendToServer("GET_ALL_PROPERTIES", new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    if (response.startsWith("SUCCESS:")) {
                        String propertiesData = response.substring(8);
                        List<Property> properties = parsePropertiesResponse(propertiesData);
                        propertyAdapter.updateProperties(properties);
                        updateEmptyView(properties.isEmpty());
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
                Toast.makeText(this, "Monitoreo seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_settings) {
                Toast.makeText(this, "Ajustes seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_history) {
                Toast.makeText(this, "Historial seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void initializeLocations() {
        locations.add("San José, Costa Rica");
        locations.add("San José, Escazú");
        locations.add("San José, Santa Ana");
        // ... resto de las ubicaciones ...
    }

    private void filterLocations(String text) {
        if (text == null || text.isEmpty()) {
            searchAdapter.clear();
            searchAdapter.notifyDataSetChanged();
            return;
        }

        List<String> filteredLocations = new ArrayList<>();
        String searchText = text.toLowerCase();

        for (String location : locations) {
            if (location.toLowerCase().contains(searchText)) {
                filteredLocations.add(location);
                if (filteredLocations.size() >= 4) break;
            }
        }

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

        clearFiltersButton.setOnClickListener(v -> clearFilters());
        applyFiltersButton.setOnClickListener(v -> {
            applyFilters();
            hideKeyboard(v);
        });
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
    }

    private void applyFilters() {
        String price = priceRangeInput.getText().toString();
        String people = peopleCountInput.getText().toString();
        String rooms = roomsCountInput.getText().toString();
        boolean allowsPets = petsSwitch.isChecked();

        String filterSummary = "Filtros aplicados:\n" +
                "Precio: " + (price.isEmpty() ? "No especificado" : price) + "\n" +
                "Personas: " + (people.isEmpty() ? "No especificado" : people) + "\n" +
                "Habitaciones: " + (rooms.isEmpty() ? "No especificado" : rooms) + "\n" +
                "Mascotas: " + (allowsPets ? "Sí" : "No");

        Toast.makeText(this, filterSummary, Toast.LENGTH_LONG).show();
        toggleFilterMenu();
    }

    @Override
    public void onPropertyClick(Property property) {
        Intent intent = new Intent(this, PropertyDetailActivity.class);
        intent.putExtra("property", property);
        startActivity(intent);
    }
}