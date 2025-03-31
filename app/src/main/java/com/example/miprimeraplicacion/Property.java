package com.example.miprimeraplicacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Property implements Serializable {
    private static final String[] AMENITIES = {
            "Cocina equipada", "Aire acondicionado", "Calefacción", "Wi-Fi gratuito",
            "Televisión por cable", "Lavadora y secadora", "Piscina", "Jardín",
            "Barbacoa", "Terraza", "Gimnasio", "Garaje", "Sistema de seguridad",
            "Baño en suite", "Muebles de exterior", "Microondas", "Lavavajillas",
            "Cafetera", "Ropa de cama incluida", "Áreas comunes", "Camas adicionales",
            "Servicio de limpieza", "Transporte público cercano", "Mascotas permitidas",
            "Cerca de comercios", "Suelo radiante", "Área de trabajo", "Sistemas de entretenimiento",
            "Chimenea", "Internet alta velocidad"
    };
    private String id;
    private String ownerName;
    private String title;
    private String description;
    private double pricePerNight;
    private String location;
    private int capacity;
    private String propertyType;
    private List<String> photoUrls;
    private List<String> rules;
    private List<String> amenities;
    private String creationDate;
    private String photos; // Para manejar la cadena base64
    private int rooms;
    private double price;
    private boolean allowsPets;

    public Property() {
        this.photoUrls = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }

    // Getters y Setters
    public boolean getAllowsPets() {
        return allowsPets;
    }

    // Setter para allowsPets
    public void setAllowsPets(boolean allowsPets) {
        this.allowsPets = allowsPets;
    }

    public int getRooms() {
        return rooms;
    }

    public void setRooms(int rooms) {
        this.rooms = rooms;
    }

    public String getId() {
        return id != null ? id.trim().replace(":", "") : "";
    }
    public void setId(String id) {
        this.id = id != null ? id.trim().replace(":", "") : "";
    }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) {
        this.rules = new ArrayList<>();
        if (rules != null) {
            for (String rule : rules) {
                if (rule != null && !rule.trim().isEmpty()) {
                    this.rules.add(rule.trim());
                }
            }
        }
    }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) {
        this.amenities = new ArrayList<>();
        if (amenities != null) {
            for (String amenity : amenities) {
                if (amenity != null && !amenity.trim().isEmpty()) {
                    this.amenities.add(amenity.trim());
                }
            }
        }
    }

    public void setAmenitiesFromString(String amenitiesStr) {
        this.amenities = new ArrayList<>();
        if (amenitiesStr != null && !amenitiesStr.isEmpty() && !amenitiesStr.equals(" ")) {
            String[] amenityArray = amenitiesStr.split("\\|");
            for (String amenity : amenityArray) {
                if (amenity != null && !amenity.trim().isEmpty()) {
                    this.amenities.add(amenity.trim());
                }
            }
        }
    }

    public void setRulesFromString(String rulesStr) {
        this.rules = new ArrayList<>();
        if (rulesStr != null && !rulesStr.isEmpty() && !rulesStr.equals(" ")) {
            String[] ruleArray = rulesStr.split("\\|");
            for (String rule : ruleArray) {
                if (rule != null && !rule.trim().isEmpty()) {
                    this.rules.add(rule.trim());
                }
            }
        }
    }

    public boolean hasAmenities() {
        return amenities != null && !amenities.isEmpty();
    }

    public boolean hasRules() {
        return rules != null && !rules.isEmpty();
    }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    // Nuevos métodos para manejar strings con formato separado por |
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) {
        this.photos = photos;
        if (photos != null && !photos.isEmpty()) {
            this.photoUrls = Arrays.asList(photos.split("\\|"));
        }
    }

    public String getAmenitiesString() {
        if (amenities != null && !amenities.isEmpty()) {
            return String.join("|", amenities);
        }
        return "";
    }

    public String getRulesString() {
        if (rules != null && !rules.isEmpty()) {
            return String.join("|", rules);
        }
        return "";
    }

    // Métodos de utilidad
    public boolean hasPhotos() {
        return photoUrls != null && !photoUrls.isEmpty();
    }

    public String getFirstPhotoUrl() {
        if (hasPhotos()) {
            return photoUrls.get(0);
        }
        return null;
    }

    public int getPhotoCount() {
        return photoUrls != null ? photoUrls.size() : 0;
    }
}