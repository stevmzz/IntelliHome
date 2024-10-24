package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import android.os.Bundle;
import android.view.View;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Intent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.net.Uri;

public class ArrendadorActivity extends AppCompatActivity {

    private static final int ADD_PROPERTY_REQUEST = 1;
    private ImageView logoImageView;
    private Button topButton;
    private LinearLayout propertiesContainer;
    private FloatingActionButton addPropertyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrendador);

        initializeViews();
        setupListeners();
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
    // Agregar al inicio
    private LinearLayout menuOptionsLayout;
    private Button menuProfileButton;
    private Button menuSettingsButton;
    private Button menuMonitoringButton;
    private Button menuHistoryButton;

    private void initializeViews() {
        logoImageView = findViewById(R.id.logoImageView);
        topButton = findViewById(R.id.topButton);
        propertiesContainer = findViewById(R.id.propertiesContainer);
        addPropertyButton = findViewById(R.id.addPropertyButton);

        // Inicializar las opciones del menú
        menuOptionsLayout = findViewById(R.id.menuOptionsLayout);
        menuProfileButton = findViewById(R.id.menu_profile);
        menuSettingsButton = findViewById(R.id.menu_settings);
        menuMonitoringButton = findViewById(R.id.menu_monitoring);
        menuHistoryButton = findViewById(R.id.menu_history);
    }

    private void setupListeners() {
        // Logo click
        logoImageView.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArrendadorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });

        // Botón de menú
        topButton.setOnClickListener(v -> {
            // Mostrar u ocultar el menú de opciones
            if (menuOptionsLayout.getVisibility() == View.GONE) {
                menuOptionsLayout.setVisibility(View.VISIBLE);
            } else {
                menuOptionsLayout.setVisibility(View.GONE);
            }
        });

        // Opción de Monitoreo
        menuMonitoringButton.setOnClickListener(v -> {
            Intent intent = new Intent(ArrendadorActivity.this, monitoreo_acti.class);
            startActivity(intent);
        });

        // Otros oyentes para las demás opciones si es necesario
    }

}