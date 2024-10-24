package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AlquiladorActivity extends AppCompatActivity {

    // Declarar las vistas que necesitaremos manejar
    private ImageView logoImageView;
    private Button topButton;
    private EditText searchEditText;
    private Button filterButton;
    private ScrollView scrollView;
    private LinearLayout scrollContentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alquilador);

        // Inicializar las vistas
        initializeViews();

        // Configurar los listeners
        setupListeners();
    }

    private void initializeViews() {
        // Vincular las vistas con sus IDs correspondientes
        // Primero necesitas añadir los IDs en tu XML
        logoImageView = findViewById(R.id.logoImageView);
        topButton = findViewById(R.id.topButton);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        scrollView = findViewById(R.id.scrollView);
        scrollContentLayout = findViewById(R.id.scrollContentLayout);
    }

    private void setupListeners() {
        // Configurar el botón superior
        topButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Manejar el clic del botón superior
                Toast.makeText(AlquiladorActivity.this, "Botón superior presionado", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar el botón de filtros
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Manejar el clic del botón de filtros
                Toast.makeText(AlquiladorActivity.this, "Abriendo filtros", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar listener para el EditText de búsqueda
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            // Manejar la acción de búsqueda
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        String searchQuery = searchEditText.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            // Realizar la búsqueda con el texto ingresado
            Toast.makeText(this, "Buscando: " + searchQuery, Toast.LENGTH_SHORT).show();
        }
    }
}