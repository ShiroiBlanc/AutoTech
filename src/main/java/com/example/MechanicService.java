package com.example;

import java.sql.*;
import java.util.*;

public class MechanicService {
    
    public List<Mechanic> getAllMechanics() throws SQLException {
        List<Mechanic> mechanics = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.id, m.hex_id, m.user_id, u.username AS name, m.specialties, m.availability " +
                "FROM mechanics m " +
                "JOIN users u ON m.user_id = u.id " +
                "ORDER BY u.username")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                int userId = rs.getInt("user_id");
                String name = rs.getString("name");
                String specialties = rs.getString("specialties");
                String availability = rs.getString("availability");
                
                Mechanic mechanic = new Mechanic(id, userId, name, specialties);
                mechanic.setHexId(hexId);
                mechanic.setAvailability(availability);
                mechanics.add(mechanic);
            }
        }
        
        return mechanics;
    }
    
    public List<Mechanic> searchMechanics(String searchTerm, String statusFilter) throws SQLException {
        List<Mechanic> mechanics = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT m.id, m.hex_id, m.user_id, u.username AS name, m.specialties, m.availability " +
            "FROM mechanics m " +
            "JOIN users u ON m.user_id = u.id " +
            "WHERE 1=1 ");
        
        // Add search condition if search term is provided
        if (!searchTerm.isEmpty()) {
            query.append("AND (u.username LIKE ? OR m.specialties LIKE ?) ");
        }
        
        // Add status filter if not "All"
        if (!statusFilter.equals("All")) {
            query.append("AND m.availability = ? ");
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
                String hexId = rs.getString("hex_id");
                int userId = rs.getInt("user_id");
                String name = rs.getString("name");
                String specialties = rs.getString("specialties");
                String availability = rs.getString("availability");
                
                Mechanic mechanic = new Mechanic(id, userId, name, specialties);
                mechanic.setHexId(hexId);
                mechanic.setAvailability(availability);
                mechanics.add(mechanic);
            }
        } catch (SQLException e) {
            // If the mechanics table doesn't have an availability column yet
            if (e.getMessage().contains("availability")) {
                return getAllMechanics();
            }
            throw e;
        }
        
        return mechanics;
    }
    
    public Mechanic getMechanicByUserId(int userId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.id, m.user_id, u.username AS name, m.specialties, m.availability " +
                "FROM mechanics m " +
                "JOIN users u ON m.user_id = u.id " +
                "WHERE m.user_id = ?")) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                int uId = rs.getInt("user_id");
                String name = rs.getString("name");
                String specialties = rs.getString("specialties");
                String availability = rs.getString("availability");
                
                Mechanic mechanic = new Mechanic(id, uId, name, specialties);
                mechanic.setAvailability(availability);
                return mechanic;
            }
        }
        
        return null;
    }
    
    public boolean isUserAlreadyMechanic(int userId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM mechanics WHERE user_id = ?")) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }
    
    public boolean addMechanic(int userId, String name, String specialties) throws SQLException {
        // Check if user is already assigned as mechanic
        if (isUserAlreadyMechanic(userId)) {
            throw new SQLException("This user is already assigned as a mechanic.");
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO mechanics (user_id, specialties, hex_id) VALUES (?, ?, ?)")) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, specialties);
            stmt.setString(3, HexIdGenerator.generateMechanicId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateMechanic(int id, int userId, String name, String specialties) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE mechanics SET user_id = ?, specialties = ? WHERE id = ?")) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, specialties);
            stmt.setInt(3, id);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateMechanicStatus(int mechanicId, String status) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE mechanics SET availability = ? WHERE id = ?")) {
            
            stmt.setString(1, status);
            stmt.setInt(2, mechanicId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
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
    
    // Get the count of active jobs for a mechanic (scheduled or in_progress)
    /**
     * Get current active job count for a mechanic
     * Only counts scheduled and in_progress jobs
     * Does NOT count delayed jobs (they're waiting, not active work)
     */
    public int getCurrentJobCount(int mechanicId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) as job_count " +
                 "FROM service_bookings " +
                 "WHERE mechanic_id = ? AND status IN ('scheduled', 'in_progress')")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("job_count");
            }
        }
        
        return 0;
    }
    
    // Get the count of active jobs for all mechanics
    public Map<Integer, Integer> getAllMechanicsJobCounts() throws SQLException {
        Map<Integer, Integer> jobCounts = new HashMap<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT mechanic_id, COUNT(*) as job_count " +
                 "FROM service_bookings " +
                 "WHERE status IN ('scheduled', 'in_progress') " +
                 "GROUP BY mechanic_id")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int mechanicId = rs.getInt("mechanic_id");
                int count = rs.getInt("job_count");
                jobCounts.put(mechanicId, count);
            }
        }
        
        return jobCounts;
    }
    
    /**
     * Automatically determine availability based on current active jobs
     * Rules:
     * - 0 jobs: Available
     * - 1-2 jobs: Available
     * - 3-4 jobs: Busy
     * - 5+ jobs: Overloaded (new bookings will be delayed)
     * - Manual "Off Duty" status is always respected
     */
    public String calculateAvailability(int mechanicId) throws SQLException {
        // First check if manually set to Off Duty
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT availability FROM mechanics WHERE id = ?")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String manualStatus = rs.getString("availability");
                if ("Off Duty".equals(manualStatus)) {
                    return "Off Duty"; // Respect manual off-duty status
                }
            }
        }
        
        // Otherwise, calculate based on job count (excludes delayed jobs)
        int jobCount = getCurrentJobCount(mechanicId);
        
        if (jobCount == 0 || jobCount <= 2) {
            return "Available";
        } else if (jobCount <= 4) {
            return "Busy";
        } else {
            return "Overloaded"; // 5+ jobs
        }
    }
    
    /**
     * Update mechanic's availability based on their current workload
     * Returns the new availability status
     */
    public String updateMechanicAvailability(int mechanicId) throws SQLException {
        String newAvailability = calculateAvailability(mechanicId);
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE mechanics SET availability = ? WHERE id = ?")) {
            
            stmt.setString(1, newAvailability);
            stmt.setInt(2, mechanicId);
            stmt.executeUpdate();
            
            System.out.println("Updated mechanic #" + mechanicId + " availability to: " + newAvailability);
        }
        
        return newAvailability;
    }
    
    /**
     * Check if mechanic can accept new booking
     * Mechanics with 5+ active jobs cannot accept new bookings
     */
    public boolean canAcceptNewBooking(int mechanicId) throws SQLException {
        // Check manual off duty status first
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT availability FROM mechanics WHERE id = ?")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("availability");
                if ("Off Duty".equals(status)) {
                    return false;
                }
            }
        }
        
        int jobCount = getCurrentJobCount(mechanicId);
        return jobCount < 5; // Can accept if less than 5 jobs
    }
    
    // Check if mechanic has a time conflict on a specific date and time
    public boolean hasTimeConflict(int mechanicId, java.sql.Date date, Time time, int durationMinutes) throws SQLException {
        return hasTimeConflict(mechanicId, date, time, durationMinutes, -1);
    }
    
    // Check if mechanic has a time conflict on a specific date and time, excluding a specific booking ID
    public boolean hasTimeConflict(int mechanicId, java.sql.Date date, Time time, int durationMinutes, int excludeBookingId) throws SQLException {
        StringBuilder query = new StringBuilder(
            "SELECT sb.booking_time, sb.estimated_duration " +
            "FROM service_bookings sb " +
            "WHERE sb.mechanic_id = ? AND sb.booking_date = ? AND sb.status != 'cancelled'");
        
        if (excludeBookingId > 0) {
            query.append(" AND sb.id != ?");
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            stmt.setInt(1, mechanicId);
            stmt.setDate(2, date);
            if (excludeBookingId > 0) {
                stmt.setInt(3, excludeBookingId);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Time existingTime = rs.getTime("booking_time");
                int existingDuration = rs.getInt("estimated_duration");
                
                // Calculate time ranges
                long newStartMinutes = time.toLocalTime().toSecondOfDay() / 60;
                long newEndMinutes = newStartMinutes + durationMinutes;
                
                long existingStartMinutes = existingTime.toLocalTime().toSecondOfDay() / 60;
                long existingEndMinutes = existingStartMinutes + existingDuration;
                
                // Check for overlap
                if ((newStartMinutes < existingEndMinutes) && (newEndMinutes > existingStartMinutes)) {
                    return true; // Time conflict exists
                }
            }
        }
        
        return false; // No time conflict
    }
    
    public String determineAvailability(int mechanicId) throws SQLException {
        // Get current availability from database
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT availability FROM mechanics WHERE id = ?")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("availability");
            }
        }
        
        return "Available"; // Default
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
                "AND sb.status != 'cancelled' " +
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
