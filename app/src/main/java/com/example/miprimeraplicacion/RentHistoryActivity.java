package com.example.miprimeraplicacion;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RentHistoryActivity extends AppCompatActivity {
    private static final String TAG = "RentHistoryActivity";

    private RecyclerView recyclerView;
    private RentHistoryAdapter adapter;
    private TextView emptyView;
    private ProgressBar loadingView;
    private ImageButton backButton;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rent_history);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadRentHistory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rentHistoryRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        loadingView = findViewById(R.id.loadingView);
        backButton = findViewById(R.id.backButton);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupRecyclerView() {
        adapter = new RentHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Agregar decoración para espaciado entre items
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
                outRect.bottom = 8;
                outRect.left = 8;
                outRect.right = 8;
            }
        });
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void loadRentHistory() {
        showLoading(true);
        String username = getUsernameFromPrefs();

        if (username.isEmpty()) {
            Log.e(TAG, "Username vacío");
            showLoading(false);
            updateEmptyView(true);
            return;
        }

        Log.d(TAG, "Solicitando historial para usuario: " + username);
        String request = "GET_RENT_HISTORY:" + username;

        ServerCommunication.sendToServer(request, new ServerCommunication.ServerResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Respuesta del servidor: " + response);
                runOnUiThread(() -> handleServerResponse(response));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error de servidor: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(RentHistoryActivity.this,
                            "Error de conexión: " + error, Toast.LENGTH_LONG).show();
                    updateEmptyView(true);
                    showLoading(false);
                });
            }
        });
    }

    private void handleServerResponse(String response) {
        try {
            Log.d(TAG, "Procesando respuesta: " + response);
            if (response.startsWith("SUCCESS:")) {
                String data = response.substring(8);
                Log.d(TAG, "Datos después de quitar SUCCESS: " + data);

                if (data.trim().isEmpty()) {
                    Log.d(TAG, "Datos vacíos");
                    updateEmptyView(true);
                    showLoading(false);
                    return;
                }

                String[] rentedProperties = data.split(";");
                Log.d(TAG, "Propiedades encontradas: " + rentedProperties.length);

                List<RentedProperty> properties = new ArrayList<>();
                for (String propertyData : rentedProperties) {
                    if (!propertyData.trim().isEmpty()) {
                        String[] fields = propertyData.split("\\|");
                        if (fields.length >= 3) {
                            try {
                                String title = fields[0].trim();
                                String description = fields[1].trim();
                                double price = Double.parseDouble(fields[2].trim());
                                properties.add(new RentedProperty(title, description, price));
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parseando precio: " + e.getMessage());
                            }
                        }
                    }
                }

                adapter.updateProperties(properties);
                updateEmptyView(properties.isEmpty());

            } else {
                Log.e(TAG, "Respuesta no exitosa: " + response);
                Toast.makeText(this, "Error cargando historial", Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando respuesta: " + e.getMessage(), e);
            Toast.makeText(this, "Error procesando datos", Toast.LENGTH_SHORT).show();
            updateEmptyView(true);
        } finally {
            showLoading(false);
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        Log.d(TAG, "Actualizando vista vacía: isEmpty=" + isEmpty +
                ", adapter count=" + adapter.getItemCount());

        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        Log.d(TAG, "Mostrando loading: " + show);
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!show) {
            boolean hasItems = adapter != null && adapter.getItemCount() > 0;
            Log.d(TAG, "Loading terminado, hasItems: " + hasItems);
            recyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            emptyView.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        }
    }

    private String getUsernameFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        Log.d(TAG, "Username obtenido: " + username);
        return username;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}