package com.example;

import java.time.LocalDate;

public class Service {
    private int id;
    private int vehicleId;
    private String serviceType;
    private String description;
    private LocalDate serviceDate;
    private LocalDate nextServiceDate;
    private double cost;
    private String status; // "Scheduled", "In Progress", "Completed", "Cancelled"
    private String technicianName;
    
    public Service(int id, int vehicleId, String serviceType, String description, 
                  LocalDate serviceDate, LocalDate nextServiceDate, double cost, 
                  String status, String technicianName) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.serviceType = serviceType;
        this.description = description;
        this.serviceDate = serviceDate;
        this.nextServiceDate = nextServiceDate;
        this.cost = cost;
        this.status = status;
        this.technicianName = technicianName;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public int getVehicleId() {
        return vehicleId;
    }
    
    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getServiceDate() {
        return serviceDate;
    }
    
    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }
    
    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }
    
    public void setNextServiceDate(LocalDate nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTechnicianName() {
        return technicianName;
    }
    
    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }
}
