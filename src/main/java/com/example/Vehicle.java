package com.example;

public class Vehicle {
    private int id;
    private int customerId;
    private String type;
    private String brand;
    private String model;
    private String year;
    private String plateNumber;
    
    public Vehicle(int id, int customerId, String type, String brand, String model, String year, String plateNumber) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.plateNumber = plateNumber;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public int getCustomerId() {
        return customerId;
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
}
