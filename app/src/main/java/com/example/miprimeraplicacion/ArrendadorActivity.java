package com.example.miprimeraplicacion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.android.material.button.MaterialButton;

public class ArrendadorActivity extends AppCompatActivity implements PropertyAdapter.OnPropertyClickListener {
    private static final String TAG = "ArrendadorActivity";
    private static final int ADD_PROPERTY_REQUEST = 1;
    private RecyclerView propertiesRecyclerView;
    private PropertyAdapter propertyAdapter;
    private TextView emptyView;
    private FloatingActionButton addPropertyButton;
    private MaterialButton topButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate iniciado");
        try {
            setContentView(R.layout.activity_arrendador);
            initializeViews();
            setupRecyclerView();
            loadProperties();
            setupListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar la actividad", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.toparrend, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                Toast.makeText(this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_settings) {
                Toast.makeText(this, "Ajustes seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            } return false;
        });

        popup.show();
    }

    private void setupListeners() {
        topButton.setOnClickListener(v -> {
            showPopupMenu(v);
        });

    }

    private void initializeViews() {
        try {
            Log.d(TAG, "Inicializando vistas");
            propertiesRecyclerView = findViewById(R.id.propertiesRecyclerView);
            emptyView = findViewById(R.id.emptyView);
            addPropertyButton = findViewById(R.id.addPropertyButton);
            topButton = findViewById(R.id.topButton);

            addPropertyButton.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Click en addPropertyButton");
                    Intent intent = new Intent(this, AddPropertyActivity.class);
                    startActivityForResult(intent, ADD_PROPERTY_REQUEST);
                } catch (Exception e) {
                    Log.e(TAG, "Error al iniciar AddPropertyActivity: " + e.getMessage());
                    Toast.makeText(this, "Error al abrir el formulario", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error en initializeViews: " + e.getMessage());
            throw e;
        }
    }

    private List<Property> parsePropertiesResponse(String response) {
        List<Property> properties = new ArrayList<>();
        Log.d(TAG, "Parseando respuesta del servidor: " + response);

        if (response == null || response.trim().isEmpty()) {
            Log.d(TAG, "Respuesta vacía, retornando lista vacía");
            return properties;
        }

        try {
            // Si la respuesta solo contiene SUCCESS:, retornar lista vacía
            if (response.trim().equals("SUCCESS:")) {
                Log.d(TAG, "Respuesta SUCCESS sin datos, retornando lista vacía");
                return properties;
            }

            String[] propertyStrings = response.split(";");
            Log.d(TAG, "Propiedades encontradas: " + propertyStrings.length);

            for (String propertyStr : propertyStrings) {
                if (propertyStr.trim().isEmpty()) {
                    Log.d(TAG, "Propiedad vacía, continuando con la siguiente");
                    continue;
                }

                try {
                    Log.d(TAG, "Procesando propiedad: " + propertyStr);
                    Property property = parsePropertyString(propertyStr);
                    if (property != null) {
                        properties.add(property);
                        Log.d(TAG, "Propiedad añadida correctamente: " + property.getTitle());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando propiedad individual: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error general parseando propiedades: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "Total de propiedades parseadas: " + properties.size());
        return properties;
    }

    private Property parsePropertyString(String propertyStr) {
        try {
            String[] data = propertyStr.split("\\|");
            Log.d(TAG, "Campos de la propiedad: " + Arrays.toString(data));

            Property property = new Property();

            // ID y título son obligatorios
            if (data.length < 3) {
                Log.e(TAG, "Datos insuficientes para crear propiedad");
                return null;
            }

            property.setId(data[0].trim());
            property.setTitle(data[2].trim());

            // Campos opcionales con validación
            if (data.length > 3) property.setDescription(data[3].trim());

            if (data.length > 4) {
                try {
                    property.setPricePerNight(Double.parseDouble(data[4].trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error en precio: " + e.getMessage());
                    property.setPricePerNight(0.0);
                }
            }

            if (data.length > 5) property.setLocation(data[5].trim());

            if (data.length > 6) {
                try {
                    property.setCapacity(Integer.parseInt(data[6].trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error en capacidad: " + e.getMessage());
                    property.setCapacity(0);
                }
            }

            if (data.length > 7) property.setPropertyType(data[7].trim());

            // Listas
            if (data.length > 8) {
                try {
                    String[] amenities = data[8].split("\\|");
                    property.setAmenities(Arrays.asList(amenities));
                } catch (Exception e) {
                    Log.e(TAG, "Error en amenities: " + e.getMessage());
                }
            }

            if (data.length > 9) {
                try {
                    String[] photos = data[9].split("\\|");
                    property.setPhotoUrls(Arrays.asList(photos));
                } catch (Exception e) {
                    Log.e(TAG, "Error en fotos: " + e.getMessage());
                }
            }

            if (data.length > 10) {
                try {
                    String[] rules = data[10].split("\\|");
                    property.setRules(Arrays.asList(rules));
                } catch (Exception e) {
                    Log.e(TAG, "Error en reglas: " + e.getMessage());
                }
            }

            if (data.length > 11) property.setCreationDate(data[11].trim());

            return property;
        } catch (Exception e) {
            Log.e(TAG, "Error parseando propiedad: " + e.getMessage());
            return null;
        }
    }

    private void setupRecyclerView() {
        try {
            Log.d(TAG, "Configurando RecyclerView");
            propertyAdapter = new PropertyAdapter(this, this);
            propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            propertiesRecyclerView.setAdapter(propertyAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error en setupRecyclerView: " + e.getMessage());
            throw e;
        }
    }

    private void loadProperties() {
        try {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = prefs.getString("username", "");
            Log.d(TAG, "Cargando propiedades para usuario: " + username);

            if (username.isEmpty()) {
                Log.e(TAG, "Username vacío");
                Toast.makeText(this, "Error: No se pudo identificar el usuario", Toast.LENGTH_LONG).show();
                return;
            }

            String request = "GET_PROPERTIES:" + username;
            Log.d(TAG, "Enviando petición: " + request);

            ServerCommunication.sendToServer(request, new ServerCommunication.ServerResponseListener() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Respuesta del servidor: " + response);
                    handleServerResponse(response);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error de servidor: " + error);
                    handleServerError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error en loadProperties: " + e.getMessage());
            handleServerError(e.getMessage());
        }
    }

    private void handleServerResponse(String response) {
        runOnUiThread(() -> {
            try {
                if (response.startsWith("SUCCESS:")) {
                    String propertiesData = response.substring(8);
                    List<Property> properties = parsePropertiesResponse(propertiesData);
                    propertyAdapter.updateProperties(properties);
                    updateEmptyView(properties.isEmpty());
                } else {
                    String error = response.startsWith("ERROR:") ? response.substring(6) : response;
                    Log.e(TAG, "Error en respuesta: " + error);
                    Toast.makeText(this, "Error al cargar propiedades: " + error, Toast.LENGTH_LONG).show();
                    updateEmptyView(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error procesando respuesta: " + e.getMessage());
                Toast.makeText(this, "Error al procesar las propiedades", Toast.LENGTH_LONG).show();
                updateEmptyView(true);
            }
        });
    }

    private void handleServerError(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "Error de conexión: " + error);
            Toast.makeText(this, "Error de conexión: " + error, Toast.LENGTH_LONG).show();
            updateEmptyView(true);
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        try {
            Log.d(TAG, "Actualizando vista vacía: " + isEmpty);
            if (emptyView != null && propertiesRecyclerView != null) {
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                propertiesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en updateEmptyView: " + e.getMessage());
        }
    }

    @Override
    public void onPropertyClick(Property property) {
        try {
            Log.d(TAG, "Propiedad seleccionada: " + property.getTitle());
            Toast.makeText(this, "Propiedad: " + property.getTitle(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error en onPropertyClick: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == ADD_PROPERTY_REQUEST) {
            loadProperties();
        }
    }
}