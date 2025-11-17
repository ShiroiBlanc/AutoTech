package com.example;

import java.time.LocalDate;

public class InventoryItem {
    private int id;
    private String hexId;
    private String partNumber;
    private String name;
    private String category;
    private int quantity;
    private int reservedQuantity;
    private LocalDate expirationDate;
    private String unit; // Unit of measurement (e.g., liters, pieces, kg)
    private double costPrice;
    private double sellingPrice;
    private String location;
    private int minimumStock;
    
    public InventoryItem(int id, String partNumber, String name, String category, 
                        int quantity, String unit, double costPrice, double sellingPrice, 
                        String location, int minimumStock) {
        this.id = id;
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.reservedQuantity = 0;
        this.unit = unit;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.location = location;
        this.minimumStock = minimumStock;
    }
    
    public InventoryItem(int id, String hexId, String partNumber, String name, String category, 
                        int quantity, int reservedQuantity, LocalDate expirationDate,
                        String unit, double costPrice, double sellingPrice, 
                        String location, int minimumStock) {
        this.id = id;
        this.hexId = hexId;
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.expirationDate = expirationDate;
        this.unit = unit;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.location = location;
        this.minimumStock = minimumStock;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public String getHexId() {
        return hexId;
    }
    
    public void setHexId(String hexId) {
        this.hexId = hexId;
    }
    
    public String getPartNumber() {
        return partNumber;
    }
    
    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public double getCostPrice() {
        return costPrice;
    }
    
    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }
    
    public double getSellingPrice() {
        return sellingPrice;
    }
    
    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public int getMinimumStock() {
        return minimumStock;
    }
    
    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }
    
    public int getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
    
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }
    
    public boolean isNearExpiration() {
        if (expirationDate == null) return false;
        LocalDate oneMonthFromNow = LocalDate.now().plusMonths(1);
        return expirationDate.isBefore(oneMonthFromNow) && !isExpired();
    }
    
    public boolean isLowStock() {
        return quantity <= minimumStock;
    }
    
    public boolean isLowAvailableStock() {
        return getAvailableQuantity() < minimumStock && quantity >= minimumStock;
    }
}
