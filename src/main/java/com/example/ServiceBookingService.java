package com.example;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ServiceBookingService {

    public ServiceBookingService() {
        // Constructor no longer needs to create tables
    }

    public List<ServiceBookingController.ServiceBookingViewModel> getAllBookings() throws SQLException {
        List<ServiceBookingController.ServiceBookingViewModel> bookings = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT sb.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                "u.username as mechanic_name " +
                "FROM service_bookings sb " +
                "JOIN customers c ON sb.customer_id = c.id " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                "LEFT JOIN users u ON m.user_id = u.id " +
                "ORDER BY sb.booking_date DESC, sb.booking_time DESC")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int customerId = rs.getInt("customer_id");
                int vehicleId = rs.getInt("vehicle_id");
                int mechanicId = rs.getInt("mechanic_id");
                Date bookingDate = rs.getDate("booking_date");
                Time bookingTime = rs.getTime("booking_time");
                String customerName = rs.getString("customer_name");
                String vehicleInfo = rs.getString("vehicle_info");
                String mechanicName = rs.getString("mechanic_name");
                String serviceType = rs.getString("service_type");
                String serviceDescription = rs.getString("service_description");
                String status = rs.getString("status");
                
                String dateStr = bookingDate.toLocalDate().format(DateTimeFormatter.ISO_DATE);
                String timeStr = bookingTime.toLocalTime().toString();
                
                bookings.add(new ServiceBookingController.ServiceBookingViewModel(
                    id, customerId, vehicleId, mechanicId, dateStr, timeStr, 
                    customerName, vehicleInfo, mechanicName, serviceType, serviceDescription, status
                ));
            }
        }
        
        return bookings;
    }
    
    public List<ServiceBookingController.ServiceBookingViewModel> searchBookings(
            String searchTerm, String statusFilter, LocalDate dateFilter) throws SQLException {
        
        List<ServiceBookingController.ServiceBookingViewModel> bookings = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT sb.*, c.name as customer_name, " +
            "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
            "u.username as mechanic_name " +
            "FROM service_bookings sb " +
            "JOIN customers c ON sb.customer_id = c.id " +
            "JOIN vehicles v ON sb.vehicle_id = v.id " +
            "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
            "LEFT JOIN users u ON m.user_id = u.id " +
            "WHERE 1=1 "
        );
        
        List<Object> params = new ArrayList<>();
        
        // Add search condition
        if (searchTerm != null && !searchTerm.isEmpty()) {
            query.append("AND (c.name LIKE ? OR v.plate_number LIKE ? OR u.username LIKE ? OR sb.service_type LIKE ?) ");
            String searchPattern = "%" + searchTerm + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Add status filter
        if (statusFilter != null && !statusFilter.equals("All")) {
            query.append("AND sb.status = ? ");
            params.add(statusFilter);
        }
        
        // Add date filter
        if (dateFilter != null) {
            query.append("AND sb.booking_date = ? ");
            params.add(Date.valueOf(dateFilter));
        }
        
        query.append("ORDER BY sb.booking_date DESC, sb.booking_time DESC");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int customerId = rs.getInt("customer_id");
                int vehicleId = rs.getInt("vehicle_id");
                int mechanicId = rs.getInt("mechanic_id");
                Date bookingDate = rs.getDate("booking_date");
                Time bookingTime = rs.getTime("booking_time");
                String customerName = rs.getString("customer_name");
                String vehicleInfo = rs.getString("vehicle_info");
                String mechanicName = rs.getString("mechanic_name");
                String serviceType = rs.getString("service_type");
                String serviceDescription = rs.getString("service_description");
                String status = rs.getString("status");
                
                String dateStr = bookingDate.toLocalDate().format(DateTimeFormatter.ISO_DATE);
                String timeStr = bookingTime.toLocalTime().toString();
                
                bookings.add(new ServiceBookingController.ServiceBookingViewModel(
                    id, customerId, vehicleId, mechanicId, dateStr, timeStr, 
                    customerName, vehicleInfo, mechanicName, serviceType, serviceDescription, status
                ));
            }
        }
        
        return bookings;
    }
    
    public boolean createBooking(int customerId, int vehicleId, int mechanicId, 
                               LocalDate bookingDate, String bookingTime, 
                               String serviceType, String serviceDescription, String status) throws SQLException {
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, booking_date, booking_time, " +
                "service_type, service_description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, customerId);
            stmt.setInt(2, vehicleId);
            stmt.setInt(3, mechanicId);
            stmt.setDate(4, Date.valueOf(bookingDate));
            stmt.setTime(5, Time.valueOf(bookingTime + ":00")); // Add seconds
            stmt.setString(6, serviceType);
            stmt.setString(7, serviceDescription);
            stmt.setString(8, status);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateBooking(int id, int customerId, int vehicleId, int mechanicId, 
                               LocalDate bookingDate, String bookingTime, 
                               String serviceType, String serviceDescription, String status) throws SQLException {
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE service_bookings SET customer_id = ?, vehicle_id = ?, mechanic_id = ?, " +
                "booking_date = ?, booking_time = ?, service_type = ?, service_description = ?, " +
                "status = ? WHERE id = ?")) {
            
            stmt.setInt(1, customerId);
            stmt.setInt(2, vehicleId);
            stmt.setInt(3, mechanicId);
            stmt.setDate(4, Date.valueOf(bookingDate));
            stmt.setTime(5, Time.valueOf(bookingTime + ":00")); // Add seconds
            stmt.setString(6, serviceType);
            stmt.setString(7, serviceDescription);
            stmt.setString(8, status);
            stmt.setInt(9, id);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateBookingStatus(int bookingId, String newStatus) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE service_bookings SET status = ? WHERE id = ?")) {
            
            stmt.setString(1, newStatus);
            stmt.setInt(2, bookingId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean deleteBooking(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM service_bookings WHERE id = ?")) {
            
            stmt.setInt(1, bookingId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}