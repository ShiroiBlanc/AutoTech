package com.example;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EmailHistoryService {
    
    /**
     * Log an email sent to the database
     */
    public static boolean logEmailSent(int billingId, String recipientEmail, String emailType, 
                                       String subject, String status, String notes) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO email_sent_history (billing_id, recipient_email, email_type, subject, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, billingId);
            stmt.setString(2, recipientEmail);
            stmt.setString(3, emailType);
            stmt.setString(4, subject);
            stmt.setString(5, status != null ? status : "sent");
            stmt.setString(6, notes);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error logging email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all email sent history
     */
    public static List<EmailSentRecord> getAllEmailHistory() throws SQLException {
        List<EmailSentRecord> records = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String query = "SELECT esh.id, esh.billing_id, b.hex_id as bill_id, esh.recipient_email, " +
                          "esh.email_type, esh.subject, esh.sent_date, esh.status, esh.notes " +
                          "FROM email_sent_history esh " +
                          "JOIN billing b ON esh.billing_id = b.id " +
                          "ORDER BY esh.sent_date DESC";
            
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                records.add(new EmailSentRecord(
                    rs.getInt("id"),
                    rs.getInt("billing_id"),
                    rs.getString("bill_id"),
                    rs.getString("recipient_email"),
                    rs.getString("email_type"),
                    rs.getString("subject"),
                    rs.getTimestamp("sent_date").toLocalDateTime(),
                    rs.getString("status"),
                    rs.getString("notes")
                ));
            }
        }
        
        return records;
    }
    
    /**
     * Search email history by bill ID or recipient email
     */
    public static List<EmailSentRecord> searchEmailHistory(String searchTerm) throws SQLException {
        List<EmailSentRecord> records = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT esh.id, esh.billing_id, b.hex_id as bill_id, esh.recipient_email, " +
                "esh.email_type, esh.subject, esh.sent_date, esh.status, esh.notes " +
                "FROM email_sent_history esh " +
                "JOIN billing b ON esh.billing_id = b.id " +
                "WHERE b.hex_id LIKE ? OR esh.recipient_email LIKE ? " +
                "ORDER BY esh.sent_date DESC")) {
            
            String likePattern = "%" + searchTerm + "%";
            stmt.setString(1, likePattern);
            stmt.setString(2, likePattern);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                records.add(new EmailSentRecord(
                    rs.getInt("id"),
                    rs.getInt("billing_id"),
                    rs.getString("bill_id"),
                    rs.getString("recipient_email"),
                    rs.getString("email_type"),
                    rs.getString("subject"),
                    rs.getTimestamp("sent_date").toLocalDateTime(),
                    rs.getString("status"),
                    rs.getString("notes")
                ));
            }
        }
        
        return records;
    }
    
    /**
     * Get email history for a specific bill
     */
    public static List<EmailSentRecord> getEmailHistoryForBill(int billingId) throws SQLException {
        List<EmailSentRecord> records = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT esh.id, esh.billing_id, b.hex_id as bill_id, esh.recipient_email, " +
                "esh.email_type, esh.subject, esh.sent_date, esh.status, esh.notes " +
                "FROM email_sent_history esh " +
                "JOIN billing b ON esh.billing_id = b.id " +
                "WHERE esh.billing_id = ? " +
                "ORDER BY esh.sent_date DESC")) {
            
            stmt.setInt(1, billingId);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                records.add(new EmailSentRecord(
                    rs.getInt("id"),
                    rs.getInt("billing_id"),
                    rs.getString("bill_id"),
                    rs.getString("recipient_email"),
                    rs.getString("email_type"),
                    rs.getString("subject"),
                    rs.getTimestamp("sent_date").toLocalDateTime(),
                    rs.getString("status"),
                    rs.getString("notes")
                ));
            }
        }
        
        return records;
    }
}
