package com.example;

import java.time.LocalDate;

public class Bill {
    private int id;
    private int customerId;
    private int serviceId;
    private String customerName;
    private String vehicleInfo;
    private double amount;
    private String paymentStatus;
    private LocalDate billDate;
    
    public Bill(int id, int customerId, int serviceId, String customerName, String vehicleInfo, 
               double amount, String paymentStatus, LocalDate billDate) {
        this.id = id;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.vehicleInfo = vehicleInfo;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.billDate = billDate;
    }
    
    // Getters
    public int getId() { return id; }
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
}
