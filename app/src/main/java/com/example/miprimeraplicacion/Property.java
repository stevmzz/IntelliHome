package com.example.miprimeraplicacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Property implements Serializable {
    private String id;
    private String ownerId;
    private String title;
    private String description;
    private double pricePerNight;
    private String location;
    private int capacity;
    private String propertyType; // RUSTICA, TECNOLOGICA, MODERNA, MANSION
    private List<String> photoUrls;
    private List<String> rules;
    private List<String> amenities;
    private String creationDate;

    public Property() {
        this.photoUrls = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

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

    public void addPhotoUrl(String url) {
        if (photoUrls.size() < 10) {
            photoUrls.add(url);
        }
    }

    public void addRule(String rule) {
        rules.add(rule);
    }

    public void addAmenity(String amenity) {
        amenities.add(amenity);
    }
}