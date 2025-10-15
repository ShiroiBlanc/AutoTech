package com.example;

public class Mechanic {
    private int id;
    private int userId;
    private String name;
    private String specialty;
    
    public Mechanic(int id, int userId, String name, String specialty) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.specialty = specialty;
    }
    
    public int getId() {
        return id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSpecialty() {
        return specialty;
    }
    
    @Override
    public String toString() {
        return name + (specialty != null && !specialty.isEmpty() ? " (" + specialty + ")" : "");
    }
}
