package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mechanic {
    private int id;
    private String hexId;
    private int userId;
    private String name;
    private List<String> specialties;  // Changed from String to List<String>
    private String availability;
    
    public Mechanic() {
        this.specialties = new ArrayList<>();
    }
    
    public Mechanic(int id, int userId, String name, String specialtiesStr) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.specialties = parseSpecialties(specialtiesStr);
        // Don't set default - availability should be loaded from database
    }
    
    // Parse comma-separated specialties string into a list
    private List<String> parseSpecialties(String specialtiesStr) {
        List<String> result = new ArrayList<>();
        if (specialtiesStr != null && !specialtiesStr.trim().isEmpty()) {
            result.addAll(Arrays.asList(specialtiesStr.split(",")));
            // Trim whitespace from each specialty
            for (int i = 0; i < result.size(); i++) {
                result.set(i, result.get(i).trim());
            }
        }
        return result;
    }
    
    // Convert list of specialties to comma-separated string
    public String getSpecialtiesAsString() {
        return String.join(", ", specialties);
    }
    
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

    public void setName(String name) {
        this.name = name;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    // Keep for backward compatibility - returns first specialty or empty string
    public String getSpecialty() {
        return specialties.isEmpty() ? "" : specialties.get(0);
    }
    
    // New methods for multiple specialties
    public List<String> getSpecialties() {
        return new ArrayList<>(specialties);
    }
    
    public void setSpecialties(List<String> specialties) {
        this.specialties = new ArrayList<>(specialties);
    }
    
    public void setSpecialtiesFromString(String specialtiesStr) {
        this.specialties = parseSpecialties(specialtiesStr);
    }
    
    public void addSpecialty(String specialty) {
        if (specialty != null && !specialty.trim().isEmpty() && !specialties.contains(specialty.trim())) {
            specialties.add(specialty.trim());
        }
    }
    
    public void removeSpecialty(String specialty) {
        specialties.remove(specialty);
    }
    
    public String getAvailability() {
        return availability;
    }
    
    public void setAvailability(String availability) {
        this.availability = availability;
    }
    
    @Override
    public String toString() {
        String specialtiesStr = getSpecialtiesAsString();
        return name + (specialtiesStr != null && !specialtiesStr.isEmpty() ? " (" + specialtiesStr + ")" : "");
    }
}
