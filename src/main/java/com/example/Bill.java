package com.example;

import java.time.LocalDate;

public class Bill {
    private int id;
    private String hexId;
    private int customerId;
    private int serviceId;
    private String customerName;
    private String vehicleInfo;
    private double amount;
    private String paymentStatus;
    private String paymentMethod;
    private String referenceNumber;
    private LocalDate billDate;
    
    public Bill(int id, String hexId, int customerId, int serviceId, String customerName, String vehicleInfo, 
               double amount, String paymentStatus, LocalDate billDate) {
        this(id, hexId, customerId, serviceId, customerName, vehicleInfo, amount, paymentStatus, null, null, billDate);
    }
    
    public Bill(int id, String hexId, int customerId, int serviceId, String customerName, String vehicleInfo, 
               double amount, String paymentStatus, String paymentMethod, String referenceNumber, LocalDate billDate) {
        this.id = id;
        this.hexId = hexId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.vehicleInfo = vehicleInfo;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.billDate = billDate;
    }
    
    // Getters
    public int getId() { return id; }
    public String getHexId() { return hexId; }
    public void setHexId(String hexId) { this.hexId = hexId; }
    public int getCustomerId() { return customerId; }
    public int getServiceId() { return serviceId; }
    public String getCustomerName() { return customerName; }
    public String getVehicleInfo() { return vehicleInfo; }
    public double getAmount() { return amount; }
    public String getPaymentStatus() { return paymentStatus; }
    public LocalDate getBillDate() { return billDate; }
    
    // Setter for payment status
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
