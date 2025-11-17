package com.example;

public class BookingServiceTableItem {
    private int id;
    private String serviceType;
    private String serviceDescription;
    
    public BookingServiceTableItem(int id, String serviceType, String serviceDescription) {
        this.id = id;
        this.serviceType = serviceType;
        this.serviceDescription = serviceDescription;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
}
