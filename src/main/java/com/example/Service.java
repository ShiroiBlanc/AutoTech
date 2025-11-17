package com.example;

public class Service {
    private int id;
    private int customerId;
    private int vehicleId;
    private int mechanicId;
    private String date;
    private String time;
    private String serviceType;
    private String serviceDescription;
    private String status; // "Scheduled", "In Progress", "Completed", "Cancelled"
    private double cost;
    
    public Service() {
        // Default constructor
    }
    
    public Service(int id, int customerId, int vehicleId, int mechanicId, 
                  String date, String time, String serviceType, String serviceDescription, 
                  String status, double cost) {
        this.id = id;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.mechanicId = mechanicId;
        this.date = date;
        this.time = time;
        this.serviceType = serviceType;
        this.serviceDescription = serviceDescription;
        this.status = status;
        this.cost = cost;
    }
    
    // Getters
    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getVehicleId() { return vehicleId; }
    public int getMechanicId() { return mechanicId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getServiceType() { return serviceType; }
    public String getServiceDescription() { return serviceDescription; }
    public String getStatus() { return status; }
    public double getCost() { return cost; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public void setMechanicId(int mechanicId) { this.mechanicId = mechanicId; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    public void setStatus(String status) { this.status = status; }
    public void setCost(double cost) { this.cost = cost; }
}
