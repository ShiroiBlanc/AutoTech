package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleService {
    private static VehicleService instance;
    
    private VehicleService() {
        // Initialize service
    }
    
    public static VehicleService getInstance() {
        if (instance == null) {
            instance = new VehicleService();
        }
        return instance;
    }
    
    public boolean addVehicle(int customerId, String type, String brand, String model, String year, String plateNumber) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO vehicles (customer_id, type, brand, model, year, plate_number) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, customerId);
            stmt.setString(2, type);
            stmt.setString(3, brand);
            stmt.setString(4, model);
            stmt.setString(5, year);
            stmt.setString(6, plateNumber);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Vehicle> getCustomerVehicles(int customerId) throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM vehicles WHERE customer_id = ? ORDER BY id")) {
            
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String year = rs.getString("year");
                String plateNumber = rs.getString("plate_number");
                
                vehicles.add(new Vehicle(id, customerId, type, brand, model, year, plateNumber));
            }
        }
        
        return vehicles;
    }
    
    // Update vehicle
    public boolean updateVehicle(Vehicle vehicle) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE vehicles SET type = ?, brand = ?, model = ?, year = ?, plate_number = ? " +
                 "WHERE id = ?")) {
            
            stmt.setString(1, vehicle.getType());
            stmt.setString(2, vehicle.getBrand());
            stmt.setString(3, vehicle.getModel());
            stmt.setString(4, vehicle.getYear());
            stmt.setString(5, vehicle.getPlateNumber());
            stmt.setInt(6, vehicle.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete vehicle
    public boolean deleteVehicle(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM vehicles WHERE id = ?")) {
            
            stmt.setInt(1, vehicleId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
