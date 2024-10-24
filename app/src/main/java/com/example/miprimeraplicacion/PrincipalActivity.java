package com.example.miprimeraplicacion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class PrincipalActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.principal0); // Aquí cargamos el layout principal0.xml

        Button buttonRegister = findViewById(R.id.buttonRegister);
        Button buttonLogin = findViewById(R.id.buttonLogin);

        // Configura el evento de clic para el botón "Register"
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Abre la actividad que muestra el layout "prueba.xml"
                Intent intent = new Intent(PrincipalActivity.this, PruebaActivity.class);
                startActivity(intent);
            }
        });

        // Configura el evento de clic para el botón "Login"
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Abre la actividad LoginActivity
                Intent intent = new Intent(PrincipalActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
