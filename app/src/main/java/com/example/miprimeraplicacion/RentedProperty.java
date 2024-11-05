package com.example.miprimeraplicacion;

public class RentedProperty {
    private String title;
    private String description;
    private double price;

    public RentedProperty(String title, String description, double price) {
        this.title = title != null ? title.trim() : "Sin título";
        this.description = description != null ? description.trim() : "Sin descripción";
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "RentedProperty{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                '}';
    }
}