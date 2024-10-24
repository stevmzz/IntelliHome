package com.example.miprimeraplicacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Property implements Serializable {
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

    public Property() {
        this.photoUrls = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
    public void setRules(List<String> rules) { this.rules = rules; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

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

    public void setAmenitiesFromString(String amenitiesStr) {
        if (amenitiesStr != null && !amenitiesStr.isEmpty()) {
            this.amenities = Arrays.asList(amenitiesStr.split("\\|"));
        }
    }

    public String getRulesString() {
        if (rules != null && !rules.isEmpty()) {
            return String.join("|", rules);
        }
        return "";
    }

    public void setRulesFromString(String rulesStr) {
        if (rulesStr != null && !rulesStr.isEmpty()) {
            this.rules = Arrays.asList(rulesStr.split("\\|"));
        }
    }

    // Métodos de utilidad
    public boolean hasPhotos() {
        return photoUrls != null && !photoUrls.isEmpty();
    }

    public boolean hasRules() {
        return rules != null && !rules.isEmpty();
    }

    public boolean hasAmenities() {
        return amenities != null && !amenities.isEmpty();
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