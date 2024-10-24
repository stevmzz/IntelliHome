package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Intent;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class AlquiladorActivity extends AppCompatActivity {

    // Vistas principales
    private ImageView logoImageView;
    private Button topButton;
    private AutoCompleteTextView searchEditText;
    private Button filterButton;
    private ScrollView scrollView;
    private LinearLayout scrollContentLayout;

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
        setupSearchAutocomplete();
        setupListeners();
    }

    private void initializeViews() {
        // Vistas principales
        logoImageView = findViewById(R.id.logoImageView);
        topButton = findViewById(R.id.topButton);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        scrollView = findViewById(R.id.scrollView);
        scrollContentLayout = findViewById(R.id.scrollContentLayout);

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
        // Inicializar la lista de ubicaciones
        locations = new ArrayList<>();
        initializeLocations();

        // Crear y configurar el adaptador con el layout personalizado
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

        // Manejar la selección de ubicación y ocultar teclado
        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = (String) parent.getItemAtPosition(position);
            Toast.makeText(AlquiladorActivity.this,
                    "Ubicación seleccionada: " + selectedLocation,
                    Toast.LENGTH_SHORT).show();
            hideKeyboard(searchEditText);
        });
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
                // Implementar navegación al perfil
                Toast.makeText(this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_monitoring) {
                // Implementar navegación al monitoreo
                Toast.makeText(this, "Monitoreo seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_settings) {
                // Implementar navegación a ajustes
                Toast.makeText(this, "Ajustes seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_history) {
                // Implementar navegación al historial
                Toast.makeText(this, "Historial seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void initializeLocations() {
        // Agregar provincias y lugares principales de Costa Rica
        locations.add("San José, Costa Rica");
        locations.add("San José, Escazú");
        locations.add("San José, Santa Ana");
        locations.add("San José, Moravia");
        locations.add("San José, Curridabat");
        locations.add("Alajuela, Costa Rica");
        locations.add("Alajuela, La Fortuna");
        locations.add("Alajuela, Poás");
        locations.add("Cartago, Costa Rica");
        locations.add("Cartago, Tres Ríos");
        locations.add("Heredia, Costa Rica");
        locations.add("Heredia, San Pablo");
        locations.add("Heredia, Santo Domingo");
        locations.add("Guanacaste, Liberia");
        locations.add("Guanacaste, Tamarindo");
        locations.add("Guanacaste, Nosara");
        locations.add("Guanacaste, Flamingo");
        locations.add("Puntarenas, Costa Rica");
        locations.add("Puntarenas, Jacó");
        locations.add("Puntarenas, Manuel Antonio");
        locations.add("Puntarenas, Drake");
        locations.add("Limón, Costa Rica");
        locations.add("Limón, Puerto Viejo");
        locations.add("Limón, Cahuita");
        locations.add("Limón, Pueblo Nuevo");
        locations.add("Limón, Cieneguita");
    }

    private void filterLocations(String text) {
        if (text == null || text.isEmpty()) {
            searchAdapter.clear();
            searchAdapter.notifyDataSetChanged();
            return;
        }

        List<String> filteredLocations = new ArrayList<>();
        String searchText = text.toLowerCase();

        // Filtrar ubicaciones que coincidan
        for (String location : locations) {
            if (location.toLowerCase().contains(searchText)) {
                filteredLocations.add(location);
                if (filteredLocations.size() >= 4) { // Limitar a 4 sugerencias
                    break;
                }
            }
        }

        // Actualizar el adaptador
        searchAdapter.clear();
        searchAdapter.addAll(filteredLocations);
        searchAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        // Logo click
        logoImageView.setOnClickListener(v -> {
            hideKeyboard(v);
            Intent intent = new Intent(this, AlquiladorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });

        // Botón superior con menú popup
        topButton.setOnClickListener(v -> {
            hideKeyboard(v);
            showPopupMenu(v);
        });

        // Botón de filtros
        filterButton.setOnClickListener(v -> {
            hideKeyboard(v);
            toggleFilterMenu();
        });

        // Botones del menú de filtros
        clearFiltersButton.setOnClickListener(v -> clearFilters());
        applyFiltersButton.setOnClickListener(v -> {
            applyFilters();
            hideKeyboard(v);
        });

        // Ocultar teclado cuando se toca fuera de los campos de texto
        scrollView.setOnTouchListener((v, event) -> {
            hideKeyboard(v);
            return false;
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
        // Obtener valores de los filtros
        String price = priceRangeInput.getText().toString();
        String people = peopleCountInput.getText().toString();
        String rooms = roomsCountInput.getText().toString();
        boolean allowsPets = petsSwitch.isChecked();

        // Crear resumen de filtros
        String filterSummary = "Filtros aplicados:\n" +
                "Precio: " + (price.isEmpty() ? "No especificado" : price) + "\n" +
                "Personas: " + (people.isEmpty() ? "No especificado" : people) + "\n" +
                "Habitaciones: " + (rooms.isEmpty() ? "No especificado" : rooms) + "\n" +
                "Mascotas: " + (allowsPets ? "Sí" : "No");

        Toast.makeText(this, filterSummary, Toast.LENGTH_LONG).show();
        toggleFilterMenu(); // Ocultar el menú después de aplicar
    }
}