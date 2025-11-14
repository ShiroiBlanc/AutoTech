package com.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class InventoryService {
    // Singleton pattern
    private static InventoryService instance;
    
    private InventoryService() {
        // Empty constructor
    }
    
    public static InventoryService getInstance() {
        if (instance == null) {
            instance = new InventoryService();
        }
        return instance;
    }
    
    public String generateNextPartNumber() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM parts")) {
            
            if (rs.next()) {
                int count = rs.getInt("count");
                // Generate part number like: PART-0001, PART-0002, etc.
                return String.format("PART-%04d", count + 1);
            }
            return "PART-0001";
        }
    }
    
    // Helper method to determine unit based on category
    public String getUnitForCategory(String category) {
        if (category == null) return "pieces";
        
        switch (category.toLowerCase()) {
            case "fluids":
                return "liters";
            case "filters":
                return "pieces";
            case "brake system":
                return "pieces";
            case "engine parts":
                return "pieces";
            case "electrical":
                return "pieces";
            case "transmission":
                return "pieces";
            case "suspension":
                return "pieces";
            case "cooling system":
                return "pieces";
            case "tools":
                return "pieces";
            default:
                return "pieces";
        }
    }
    
    public List<InventoryItem> getAllItems() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM parts ORDER BY name")) {
            
            while (rs.next()) {
                items.add(extractInventoryItemFromResultSet(rs));
            }
        }
        
        return items;
    }
    
    public InventoryItem getItemById(int id) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM parts WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return extractInventoryItemFromResultSet(rs);
            }
        }
        
        return null;
    }
    
    public boolean addItem(String partNumber, String name, String category, int quantity, 
                          double costPrice, double sellingPrice, String location, int minimumStock) {
        String unit = getUnitForCategory(category);
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO parts (part_number, name, category, quantity_in_stock, unit, " +
                 "cost_price, selling_price, supplier, reorder_level, hex_id) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, partNumber);
            stmt.setString(2, name);
            stmt.setString(3, category);
            stmt.setInt(4, quantity);
            stmt.setString(5, unit);
            stmt.setDouble(6, costPrice);
            stmt.setDouble(7, sellingPrice);
            stmt.setString(8, location);
            stmt.setInt(9, minimumStock);
            stmt.setString(10, HexIdGenerator.generatePartId());
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check if new item is already low stock
            if (rowsAffected > 0 && quantity < minimumStock) {
                checkAndSendLowStockAlert();
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateItem(InventoryItem item) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE parts SET part_number = ?, name = ?, category = ?, " +
                 "quantity_in_stock = ?, unit = ?, cost_price = ?, selling_price = ?, supplier = ?, " +
                 "reorder_level = ? WHERE id = ?")) {
            
            stmt.setString(1, item.getPartNumber());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getCategory());
            stmt.setInt(4, item.getQuantity());
            stmt.setString(5, item.getUnit());
            stmt.setDouble(6, item.getCostPrice());
            stmt.setDouble(7, item.getSellingPrice());
            stmt.setString(8, item.getLocation());
            stmt.setInt(9, item.getMinimumStock());
            stmt.setInt(10, item.getId());
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check if item is now low stock and send immediate alert
            if (rowsAffected > 0 && item.isLowStock()) {
                checkAndSendLowStockAlert();
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteItem(int id) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM parts WHERE id = ?")) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<InventoryItem> searchItems(String searchTerm) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllItems();
        }
        
        List<InventoryItem> items = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM parts " +
                 "WHERE LOWER(part_number) LIKE ? OR LOWER(name) LIKE ? OR LOWER(category) LIKE ? " +
                 "ORDER BY name")) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                items.add(extractInventoryItemFromResultSet(rs));
            }
        }
        
        return items;
    }
    
    // Helper method to create InventoryItem objects from ResultSet
    private InventoryItem extractInventoryItemFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String hexId = rs.getString("hex_id");
        String partNumber = rs.getString("part_number");
        String name = rs.getString("name");
        String category = rs.getString("category");
        int quantity = rs.getInt("quantity_in_stock");
        int reservedQuantity = rs.getInt("reserved_quantity");
        java.sql.Date expirationDateSql = rs.getDate("expiration_date");
        LocalDate expirationDate = expirationDateSql != null ? expirationDateSql.toLocalDate() : null;
        String unit = rs.getString("unit");
        if (unit == null || unit.isEmpty()) {
            unit = getUnitForCategory(category);
        }
        double costPrice = rs.getDouble("cost_price");
        double sellingPrice = rs.getDouble("selling_price");
        String location = rs.getString("supplier"); // Using supplier field as location
        int minimumStock = rs.getInt("reorder_level");
        
        return new InventoryItem(id, hexId, partNumber, name, category, 
                                quantity, reservedQuantity, expirationDate,
                                unit, costPrice, sellingPrice, location, minimumStock);
    }
    
    // Method to update item quantity (for restocking)
    public boolean updateItemQuantity(int itemId, int newQuantity) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE parts SET quantity_in_stock = ? WHERE id = ?")) {
            
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, itemId);
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check if item is now low stock and send immediate alert
            if (rowsAffected > 0) {
                checkAndSendLowStockAlert();
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Public method for external triggering (e.g., after booking creation)
    public void checkStockAfterReservation() {
        checkAndSendLowStockAlert();
    }
    
    // Check for low stock items and send immediate alert
    private void checkAndSendLowStockAlert() {
        new Thread(() -> {
            try {
                System.out.println("=== Stock Check Triggered ===");
                List<InventoryItem> allItems = getAllItems();
                List<InventoryItem> lowStockItems = new ArrayList<>();
                List<InventoryItem> lowAvailableItems = new ArrayList<>();
                
                for (InventoryItem item : allItems) {
                    int qty = item.getQuantity();
                    int reserved = item.getReservedQuantity();
                    int available = item.getAvailableQuantity();
                    int min = item.getMinimumStock();
                    
                    System.out.println("Item: " + item.getName() + 
                                     " | Total: " + qty + 
                                     " | Reserved: " + reserved + 
                                     " | Available: " + available + 
                                     " | Min: " + min);
                    
                    if (item.isLowStock()) {
                        System.out.println("  -> LOW STOCK! (" + qty + " < " + min + ")");
                        lowStockItems.add(item);
                    } else if (item.isLowAvailableStock()) {
                        System.out.println("  -> LOW AVAILABLE! (Available " + available + " < " + min + ")");
                        lowAvailableItems.add(item);
                    }
                }
                
                // Send urgent alert for actual low stock
                if (!lowStockItems.isEmpty()) {
                    System.out.println("Low stock detected! Sending urgent alert for " + lowStockItems.size() + " items.");
                    EmailService.getInstance().sendLowStockAlert(lowStockItems);
                }
                
                // Send advisory notice for low available stock (heavy reservations)
                if (!lowAvailableItems.isEmpty()) {
                    System.out.println("Low available stock detected! Sending advisory for " + lowAvailableItems.size() + " items.");
                    EmailService.getInstance().sendLowAvailableStockAlert(lowAvailableItems);
                }
                
                System.out.println("=== Stock Check Complete ===");
            } catch (SQLException e) {
                System.err.println("Error checking low stock: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    // Reserve parts when booking is created
    public boolean reserveParts(int partId, int quantity) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE parts SET reserved_quantity = reserved_quantity + ? WHERE id = ?")) {
            
            stmt.setInt(1, quantity);
            stmt.setInt(2, partId);
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check if reservation caused low available stock
            if (rowsAffected > 0) {
                checkAndSendLowStockAlert();
            }
            
            return rowsAffected > 0;
        }
    }
    
    // Release reserved parts (when booking is cancelled)
    public boolean releaseReservedParts(int partId, int quantity) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE parts SET reserved_quantity = GREATEST(0, reserved_quantity - ?) WHERE id = ?")) {
            
            stmt.setInt(1, quantity);
            stmt.setInt(2, partId);
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check stock after releasing reservation (usually won't trigger alerts)
            if (rowsAffected > 0) {
                checkAndSendLowStockAlert();
            }
            
            return rowsAffected > 0;
        }
    }
    
    // Get items expiring soon (within 30 days)
    public List<InventoryItem> getItemsExpiringSoon() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM parts WHERE expiration_date IS NOT NULL " +
                 "AND expiration_date <= ? AND expiration_date >= CURDATE() ORDER BY expiration_date")) {
            
            stmt.setDate(1, java.sql.Date.valueOf(thirtyDaysFromNow));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                items.add(extractInventoryItemFromResultSet(rs));
            }
        }
        
        return items;
    }
    
    // Get expired items
    public List<InventoryItem> getExpiredItems() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT * FROM parts WHERE expiration_date IS NOT NULL " +
                 "AND expiration_date < CURDATE() ORDER BY expiration_date")) {
            
            while (rs.next()) {
                items.add(extractInventoryItemFromResultSet(rs));
            }
        }
        
        return items;
    }
    
    // Update expiration date
    public boolean updateExpirationDate(int itemId, LocalDate expirationDate) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE parts SET expiration_date = ? WHERE id = ?")) {
            
            if (expirationDate != null) {
                stmt.setDate(1, java.sql.Date.valueOf(expirationDate));
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setInt(2, itemId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
