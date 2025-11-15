package com.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BillingService {
    private static BillingService instance;
    private static final Random random = new Random();
    
    private BillingService() {
        // Constructor no longer needs to create tables
    }
    
    public static BillingService getInstance() {
        if (instance == null) {
            instance = new BillingService();
        }
        return instance;
    }
    
    public List<Bill> getAllBills() throws SQLException {
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT b.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info " +
                "FROM billing b " +
                "JOIN customers c ON b.customer_id = c.id " +
                "JOIN service_bookings s ON b.service_id = s.id " +
                "JOIN vehicles v ON s.vehicle_id = v.id " +
                "ORDER BY b.bill_date DESC")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                int customerId = rs.getInt("customer_id");
                int serviceId = rs.getInt("service_id");
                String customerName = rs.getString("customer_name");
                String vehicleInfo = rs.getString("vehicle_info");
                double amount = rs.getDouble("amount");
                String paymentStatus = rs.getString("payment_status");
                LocalDate billDate = rs.getDate("bill_date").toLocalDate();
                
                bills.add(new Bill(id, hexId, customerId, serviceId, customerName, vehicleInfo, 
                                  amount, paymentStatus, billDate));
            }
        }
        
        return bills;
    }
    
    public boolean createBillFromService(int serviceBookingId) throws SQLException {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement existsStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;
        ResultSet existsRs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // First, check if the service booking exists and is completed
            checkStmt = conn.prepareStatement(
                "SELECT sb.id, sb.customer_id, sb.service_type, sb.status " +
                "FROM service_bookings sb " +
                "WHERE sb.id = ? AND sb.status = 'completed'");
                
            checkStmt.setInt(1, serviceBookingId);
            rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                return false; // Service booking doesn't exist or is not completed
            }
            
            int customerId = rs.getInt("customer_id");
            String serviceType = rs.getString("service_type");
            
            // Check if bill already exists for this service
            existsStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM billing WHERE service_id = ?");
                
            existsStmt.setInt(1, serviceBookingId);
            existsRs = existsStmt.executeQuery();
            
            if (existsRs.next() && existsRs.getInt(1) > 0) {
                return false; // Bill already exists for this service
            }
            
            // Calculate service charge based on service type
            double serviceCharge = calculateAmountByServiceType(serviceType);
            
            // Get parts used in this booking and calculate parts cost
            double partsCost = 0.0;
            PreparedStatement partsStmt = conn.prepareStatement(
                "SELECT SUM(quantity * price_at_time) as total_parts_cost " +
                "FROM booking_parts WHERE booking_id = ?");
            partsStmt.setInt(1, serviceBookingId);
            ResultSet partsRs = partsStmt.executeQuery();
            if (partsRs.next()) {
                partsCost = partsRs.getDouble("total_parts_cost");
            }
            partsRs.close();
            partsStmt.close();
            
            // Total amount = service charge + parts cost
            double totalAmount = serviceCharge + partsCost;
            
            // Create the bill
            insertStmt = conn.prepareStatement(
                "INSERT INTO billing (customer_id, service_id, amount, payment_status, bill_date, hex_id) " +
                "VALUES (?, ?, ?, 'Unpaid', CURRENT_DATE, ?)");
                
            insertStmt.setInt(1, customerId);
            insertStmt.setInt(2, serviceBookingId);
            insertStmt.setDouble(3, totalAmount);
            insertStmt.setString(4, HexIdGenerator.generateBillId());
            
            int rowsAffected = insertStmt.executeUpdate();
            return rowsAffected > 0;
            
        } finally {
            // Close all resources manually in reverse order
            if (existsRs != null) try { existsRs.close(); } catch (SQLException e) { /* ignore */ }
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (insertStmt != null) try { insertStmt.close(); } catch (SQLException e) { /* ignore */ }
            if (existsStmt != null) try { existsStmt.close(); } catch (SQLException e) { /* ignore */ }
            if (checkStmt != null) try { checkStmt.close(); } catch (SQLException e) { /* ignore */ }
            // Don't close the connection as it's managed by DatabaseUtil
        }
    }
    
    // Separate method to calculate amount based on service type
    private double calculateAmountByServiceType(String serviceType) {
        // Set prices based on service type
        switch (serviceType) {
            case "Regular Maintenance":
                return 1000.00;
            case "Oil Change":
                return 500.00;
            case "Tire Service":
                // Random value from {200, 500, 800}
                int[] tireServicePrices = {200, 500, 800};
                return tireServicePrices[random.nextInt(tireServicePrices.length)];
            case "Brake Service":
                return 500.00;
            case "Engine Repair":
                return 2000.00;
            case "Transmission Service":
                return 4000.00;
            case "Electrical System":
                return 1500.00;
            default:
                // For "Other" - random value ending with 00
                int hundreds = random.nextInt(10) + 1; // 1-10 (hundreds)
                return hundreds * 100.00; // 100, 200, ..., 1000
        }
    }
    
    public Bill getBillByServiceId(int serviceId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT b.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info " +
                "FROM billing b " +
                "JOIN customers c ON b.customer_id = c.id " +
                "JOIN service_bookings s ON b.service_id = s.id " +
                "JOIN vehicles v ON s.vehicle_id = v.id " +
                "WHERE b.service_id = ?")) {
            
            stmt.setInt(1, serviceId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                int customerId = rs.getInt("customer_id");
                int svcId = rs.getInt("service_id");
                String customerName = rs.getString("customer_name");
                String vehicleInfo = rs.getString("vehicle_info");
                double amount = rs.getDouble("amount");
                String paymentStatus = rs.getString("payment_status");
                LocalDate billDate = rs.getDate("bill_date").toLocalDate();
                
                return new Bill(id, hexId, customerId, svcId, customerName, vehicleInfo, 
                              amount, paymentStatus, billDate);
            }
        }
        return null;
    }
    
    public boolean updateBillStatus(int billId, String status) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE billing SET payment_status = ? WHERE id = ?")) {
            
            stmt.setString(1, status);
            stmt.setInt(2, billId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateBillPayment(int billId, String status, String paymentMethod, String referenceNumber) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE billing SET payment_status = ?, payment_method = ?, reference_number = ? WHERE id = ?")) {
            
            stmt.setString(1, status);
            stmt.setString(2, paymentMethod);
            stmt.setString(3, referenceNumber);
            stmt.setInt(4, billId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean deleteBill(int billId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM billing WHERE id = ?")) {
            
            stmt.setInt(1, billId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
