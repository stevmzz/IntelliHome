package com.example.miprimeraplicacion;

public class RentedProperty {
    private String title;
    private String description;
    private String checkInDate;
    private String checkOutDate;
    private int totalNights;
    private double totalPrice;
    private String status;

    public RentedProperty(String title,
                          String description,
                          String checkInDate,
                          String checkOutDate,
                          int totalNights,
                          double totalPrice,
                          String status) {  // Constructor actualizado con nuevo parámetro
        this.title = title != null ? title.trim() : "Sin título";
        this.description = description != null ? description.trim() : "Sin descripción";
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalNights = totalNights;
        this.totalPrice = totalPrice;
        this.status = status != null ? status : "INACTIVE";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getTotalNights() {
        return totalNights;
    }

    public void setTotalNights(int totalNights) {
        this.totalNights = totalNights;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        return "RentedProperty{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", checkInDate='" + checkInDate + '\'' +
                ", checkOutDate='" + checkOutDate + '\'' +
                ", totalNights=" + totalNights +
                ", totalPrice=" + totalPrice +
                '}';
    }
}