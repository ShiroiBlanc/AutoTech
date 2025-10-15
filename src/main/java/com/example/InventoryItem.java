package com.example;

public class InventoryItem {
    private int id;
    private String partNumber;
    private String name;
    private String category;
    private int quantity;
    private double costPrice;
    private double sellingPrice;
    private String location;
    private int minimumStock;
    
    public InventoryItem(int id, String partNumber, String name, String category, 
                        int quantity, double costPrice, double sellingPrice, 
                        String location, int minimumStock) {
        this.id = id;
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.location = location;
        this.minimumStock = minimumStock;
    }
    
    // Getters and setters
    public int getId() {
        return id;
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
    
    public boolean isLowStock() {
        return quantity < minimumStock;
    }
}
