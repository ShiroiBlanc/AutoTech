package com.example;

public class Customer {
    private int id;
    private String hexId;
    private String name;
    private String phone;
    private String email;
    private String address;
    
    public Customer() {
    }
    
    public Customer(int id, String name, String phone, String email, String address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
    
    public Customer(int id, String hexId, String name, String phone, String email, String address) {
        this.id = id;
        this.hexId = hexId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
    
    // Getters and setters
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    @Override
    public String toString() {
        return name + " (" + (hexId != null ? hexId : "ID: " + id) + ")";
    }
}