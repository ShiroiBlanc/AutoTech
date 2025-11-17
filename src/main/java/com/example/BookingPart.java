package com.example;

public class BookingPart {
    private int id;
    private int bookingId;
    private int partId;
    private String partName;
    private int quantity;
    private double price;
    
    public BookingPart() {
    }
    
    public BookingPart(int bookingId, int partId, String partName, int quantity, double price) {
        this.bookingId = bookingId;
        this.partId = partId;
        this.partName = partName;
        this.quantity = quantity;
        this.price = price;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
    
    public int getPartId() {
        return partId;
    }
    
    public void setPartId(int partId) {
        this.partId = partId;
    }
    
    public String getPartName() {
        return partName;
    }
    
    public void setPartName(String partName) {
        this.partName = partName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public double getTotalCost() {
        return quantity * price;
    }
}
