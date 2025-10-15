package com.example;

import java.sql.*;
import java.util.*;

public class MechanicService {
    
    public List<Mechanic> getAllMechanics() throws SQLException {
        List<Mechanic> mechanics = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.id, m.user_id, u.username AS name, m.specialty " +
                "FROM mechanics m " +
                "JOIN users u ON m.user_id = u.id " +
                "ORDER BY u.username")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int userId = rs.getInt("user_id");
                String name = rs.getString("name");
                String specialty = rs.getString("specialty");
                
                mechanics.add(new Mechanic(id, userId, name, specialty));
            }
        }
        
        return mechanics;
    }
    
    public List<Mechanic> searchMechanics(String searchTerm, String statusFilter) throws SQLException {
        List<Mechanic> mechanics = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT m.id, m.user_id, u.username AS name, m.specialty " +
            "FROM mechanics m " +
            "JOIN users u ON m.user_id = u.id " +
            "WHERE 1=1 ");
        
        // Add search condition if search term is provided
        if (!searchTerm.isEmpty()) {
            query.append("AND (u.username LIKE ? OR m.specialty LIKE ?) ");
        }
        
        // Add status filter if not "All"
        // Note: In a real implementation, you'd have a status field in the database
        if (!statusFilter.equals("All")) {
            query.append("AND m.status = ? ");
        }
        
        query.append("ORDER BY u.username");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            int paramIndex = 1;
            
            if (!searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            if (!statusFilter.equals("All")) {
                stmt.setString(paramIndex, statusFilter);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int userId = rs.getInt("user_id");
                String name = rs.getString("name");
                String specialty = rs.getString("specialty");
                
                mechanics.add(new Mechanic(id, userId, name, specialty));
            }
        } catch (SQLException e) {
            // If the mechanics table doesn't have a status column yet
            if (e.getMessage().contains("status")) {
                return getAllMechanics();
            }
            throw e;
        }
        
        return mechanics;
    }
    
    public boolean addMechanic(int userId, String name, String specialty) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO mechanics (user_id, specialty) VALUES (?, ?)")) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, specialty);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateMechanic(int id, int userId, String name, String specialty) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE mechanics SET user_id = ?, specialty = ? WHERE id = ?")) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, specialty);
            stmt.setInt(3, id);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateMechanicStatus(int mechanicId, String status) throws SQLException {
        // This would be implemented with a proper status field in the mechanics table
        // For now, we'll return true to simulate success
        return true;
    }
    
    public List<User> getUsersWithMechanicRole() throws SQLException {
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT u.id, u.username, u.email " +
                "FROM users u " +
                "JOIN roles r ON u.role_id = r.id " +
                "WHERE r.name = 'MECHANIC' AND u.active = TRUE " +
                "ORDER BY u.username")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                
                users.add(new User(id, username, email));
            }
        }
        
        return users;
    }
    
    public Map<Integer, Integer> getCurrentJobCounts() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT mechanic_id, COUNT(*) as job_count " +
                 "FROM service_bookings " +
                 "WHERE status = 'scheduled' OR status = 'in_progress' " +
                 "GROUP BY mechanic_id")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int mechanicId = rs.getInt("mechanic_id");
                int count = rs.getInt("job_count");
                counts.put(mechanicId, count);
            }
        } catch (SQLException e) {
            // If the service_bookings table doesn't exist yet, return empty map
            if (e.getMessage().contains("service_bookings")) {
                return counts;
            }
            throw e;
        }
        
        return counts;
    }
    
    public String getMechanicAvailability(int mechanicId) throws SQLException {
        // In a real application, you would determine availability based on:
        // 1. Explicit status set in the database
        // 2. Number of current jobs
        // 3. Working hours/schedule
        
        // For demo purposes, we'll check job count
        Map<Integer, Integer> jobCounts = getCurrentJobCounts();
        int jobCount = jobCounts.getOrDefault(mechanicId, 0);
        
        if (jobCount == 0) {
            return "Available";
        } else if (jobCount < 3) {
            return "Busy";
        } else {
            return "Off Duty";
        }
    }
    
    public List<MechanicController.ServiceOrder> getMechanicJobs(int mechanicId) throws SQLException {
        List<MechanicController.ServiceOrder> jobs = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT sb.id, sb.booking_date, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                "sb.status, sb.service_description as description " +
                "FROM service_bookings sb " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "JOIN customers c ON sb.customer_id = c.id " +
                "WHERE sb.mechanic_id = ? " +
                "ORDER BY sb.booking_date DESC")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                jobs.add(new MechanicController.ServiceOrder(
                    rs.getInt("id"),
                    rs.getString("booking_date"),
                    rs.getString("customer_name"),
                    rs.getString("vehicle_info"),
                    rs.getString("status"),
                    rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            // If service_bookings table doesn't exist yet, return empty list
            if (e.getMessage().contains("service_bookings")) {
                return jobs;
            }
            throw e;
        }
        
        return jobs;
    }
    
    // User class for mechanic-user association
    public static class User {
        private int id;
        private String username;
        private String email;
        
        public User(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }
        
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        
        @Override
        public String toString() {
            return username;
        }
    }
}
