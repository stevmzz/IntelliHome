package com.example.miprimeraplicacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostaRicaLocations {
    private static final Map<String, List<String>> locationsByProvince = new HashMap<>();
    private static final List<String> allLocations = new ArrayList<>();

    static {
        // San José
        List<String> sanJoseLocations = new ArrayList<>();
        sanJoseLocations.add("San José Centro");
        sanJoseLocations.add("Escazú");
        sanJoseLocations.add("Desamparados");
        sanJoseLocations.add("Puriscal");
        sanJoseLocations.add("Tarrazú");
        sanJoseLocations.add("Aserrí");
        sanJoseLocations.add("Mora");
        sanJoseLocations.add("Goicoechea");
        sanJoseLocations.add("Santa Ana");
        sanJoseLocations.add("Alajuelita");
        sanJoseLocations.add("Coronado");
        sanJoseLocations.add("Tibás");
        sanJoseLocations.add("Moravia");
        sanJoseLocations.add("Montes de Oca");
        sanJoseLocations.add("Curridabat");
        locationsByProvince.put("San José", sanJoseLocations);

        // Alajuela
        List<String> alajuelaLocations = new ArrayList<>();
        alajuelaLocations.add("Alajuela Centro");
        alajuelaLocations.add("San Ramón");
        alajuelaLocations.add("Grecia");
        alajuelaLocations.add("San Mateo");
        alajuelaLocations.add("Atenas");
        alajuelaLocations.add("Naranjo");
        alajuelaLocations.add("Palmares");
        alajuelaLocations.add("Poás");
        alajuelaLocations.add("Orotina");
        alajuelaLocations.add("San Carlos");
        alajuelaLocations.add("Zarcero");
        alajuelaLocations.add("Sarchí");
        alajuelaLocations.add("Upala");
        alajuelaLocations.add("Los Chiles");
        alajuelaLocations.add("Guatuso");
        locationsByProvince.put("Alajuela", alajuelaLocations);

        // Cartago
        List<String> cartagoLocations = new ArrayList<>();
        cartagoLocations.add("Cartago Centro");
        cartagoLocations.add("Paraíso");
        cartagoLocations.add("La Unión");
        cartagoLocations.add("Jiménez");
        cartagoLocations.add("Turrialba");
        cartagoLocations.add("Alvarado");
        cartagoLocations.add("Oreamuno");
        cartagoLocations.add("El Guarco");
        locationsByProvince.put("Cartago", cartagoLocations);

        // Heredia
        List<String> herediaLocations = new ArrayList<>();
        herediaLocations.add("Heredia Centro");
        herediaLocations.add("Barva");
        herediaLocations.add("Santo Domingo");
        herediaLocations.add("Santa Bárbara");
        herediaLocations.add("San Rafael");
        herediaLocations.add("San Isidro");
        herediaLocations.add("Belén");
        herediaLocations.add("Flores");
        herediaLocations.add("San Pablo");
        herediaLocations.add("Sarapiquí");
        locationsByProvince.put("Heredia", herediaLocations);

        // Guanacaste
        List<String> guanacasteLocations = new ArrayList<>();
        guanacasteLocations.add("Liberia");
        guanacasteLocations.add("Nicoya");
        guanacasteLocations.add("Santa Cruz");
        guanacasteLocations.add("Bagaces");
        guanacasteLocations.add("Carrillo");
        guanacasteLocations.add("Cañas");
        guanacasteLocations.add("Abangares");
        guanacasteLocations.add("Tilarán");
        guanacasteLocations.add("Nandayure");
        guanacasteLocations.add("La Cruz");
        guanacasteLocations.add("Hojancha");
        locationsByProvince.put("Guanacaste", guanacasteLocations);

        // Puntarenas
        List<String> puntarenasLocations = new ArrayList<>();
        puntarenasLocations.add("Puntarenas Centro");
        puntarenasLocations.add("Esparza");
        puntarenasLocations.add("Buenos Aires");
        puntarenasLocations.add("Montes de Oro");
        puntarenasLocations.add("Osa");
        puntarenasLocations.add("Quepos");
        puntarenasLocations.add("Golfito");
        puntarenasLocations.add("Coto Brus");
        puntarenasLocations.add("Parrita");
        puntarenasLocations.add("Corredores");
        puntarenasLocations.add("Garabito");
        locationsByProvince.put("Puntarenas", puntarenasLocations);

        // Limón
        List<String> limonLocations = new ArrayList<>();
        limonLocations.add("Limón Centro");
        limonLocations.add("Pococí");
        limonLocations.add("Siquirres");
        limonLocations.add("Talamanca");
        limonLocations.add("Matina");
        limonLocations.add("Guácimo");
        locationsByProvince.put("Limón", limonLocations);

        // Crear lista completa de ubicaciones
        for (Map.Entry<String, List<String>> entry : locationsByProvince.entrySet()) {
            String province = entry.getKey();
            for (String location : entry.getValue()) {
                allLocations.add(location + ", " + province);
            }
        }
    }

    public static List<String> getAllLocations() {
        return new ArrayList<>(allLocations);
    }

    public static List<String> searchLocations(String query) {
        List<String> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String normalizedQuery = query.toLowerCase().trim();
        for (String location : allLocations) {
            if (location.toLowerCase().contains(normalizedQuery)) {
                results.add(location);
            }
        }
        return results;
    }
}