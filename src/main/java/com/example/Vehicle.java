package com.example;

public class Vehicle {
    private int id;
    private String hexId;
    private int customerId;
    private String type;
    private String brand;
    private String model;
    private String year;
    private String plateNumber;

    public Vehicle() {
    }
    
    public Vehicle(int id, int customerId, String type, String brand, String model, String year, String plateNumber) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.plateNumber = plateNumber;
    }
    
    public Vehicle(int id, String hexId, int customerId, String type, String brand, String model, String year, String plateNumber) {
        this.id = id;
        this.hexId = hexId;
        this.customerId = customerId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.plateNumber = plateNumber;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getHexId() {
        return hexId;
    }
    
    public void setHexId(String hexId) {
        this.hexId = hexId;
    }
    
    public int getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public String getPlateNumber() {
        return plateNumber;
    }
    
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }
    
    @Override
    public String toString() {
        return brand + " " + model + " (" + plateNumber + ")";
    }
}
