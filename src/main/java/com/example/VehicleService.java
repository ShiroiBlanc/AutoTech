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
                 "INSERT INTO vehicles (customer_id, type, brand, model, year, plate_number, hex_id) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, customerId);
            stmt.setString(2, type);
            stmt.setString(3, brand);
            stmt.setString(4, model);
            stmt.setString(5, year);
            stmt.setString(6, plateNumber);
            stmt.setString(7, HexIdGenerator.generateVehicleId());
            
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
                String hexId = rs.getString("hex_id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String year = rs.getString("year");
                String plateNumber = rs.getString("plate_number");
                
                vehicles.add(new Vehicle(id, hexId, customerId, type, brand, model, year, plateNumber));
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

    // Check if vehicle has active service bookings (not completed or cancelled)
    public boolean hasActiveServiceBookings(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM service_bookings WHERE vehicle_id = ? " +
                 "AND status NOT IN ('completed', 'cancelled')")) {
            
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        
        return false;
    }
    
    // Get count of active service bookings for a vehicle
    public int getActiveServiceBookingCount(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM service_bookings WHERE vehicle_id = ? " +
                 "AND status NOT IN ('completed', 'cancelled')")) {
            
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }
    
    // Check if vehicle has unpaid billing records
    public boolean hasUnpaidBills(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM billing WHERE service_id IN " +
                 "(SELECT id FROM service_bookings WHERE vehicle_id = ?) " +
                 "AND payment_status = 'unpaid'")) {
            
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        
        return false;
    }
    
    // Get count of unpaid bills for a vehicle
    public int getUnpaidBillCount(int vehicleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM billing WHERE service_id IN " +
                 "(SELECT id FROM service_bookings WHERE vehicle_id = ?) " +
                 "AND payment_status = 'unpaid'")) {
            
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }
    
    // Delete vehicle and its completed/cancelled bookings (with billing records)
    public boolean deleteVehicle(int vehicleId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Step 1: Delete billing records for completed/cancelled bookings of this vehicle
            try (PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM billing WHERE service_id IN " +
                 "(SELECT id FROM service_bookings WHERE vehicle_id = ? AND status IN ('completed', 'cancelled'))")) {
                stmt.setInt(1, vehicleId);
                stmt.executeUpdate();
            }
            
            // Step 2: Delete completed and cancelled bookings for this vehicle
            try (PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM service_bookings WHERE vehicle_id = ? AND status IN ('completed', 'cancelled')")) {
                stmt.setInt(1, vehicleId);
                stmt.executeUpdate();
            }
            
            // Step 3: Delete the vehicle
            try (PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM vehicles WHERE id = ?")) {
                stmt.setInt(1, vehicleId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit(); // Commit transaction
                    return true;
                } else {
                    conn.rollback(); // Rollback if vehicle deletion failed
                    return false;
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // Transfer vehicle to a different customer
    public boolean transferVehicle(int vehicleId, int newCustomerId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE vehicles SET customer_id = ? WHERE id = ?")) {
            
            stmt.setInt(1, newCustomerId);
            stmt.setInt(2, vehicleId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // Get vehicle by plate number (useful for finding existing vehicles)
    public Vehicle getVehicleByPlateNumber(String plateNumber) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM vehicles WHERE plate_number = ?")) {
            
            stmt.setString(1, plateNumber);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                int customerId = rs.getInt("customer_id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String year = rs.getString("year");
                
                return new Vehicle(id, hexId, customerId, type, brand, model, year, plateNumber);
            }
        }
        
        return null;
    }
    
    // Get all vehicles (for global search/transfer)
    public List<Vehicle> getAllVehicles() throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vehicles ORDER BY id")) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                int customerId = rs.getInt("customer_id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String year = rs.getString("year");
                String plateNumber = rs.getString("plate_number");
                
                vehicles.add(new Vehicle(id, hexId, customerId, type, brand, model, year, plateNumber));
            }
        }
        
        return vehicles;
    }
}
