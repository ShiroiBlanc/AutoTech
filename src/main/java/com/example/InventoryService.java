package com.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class InventoryService {
    // Singleton pattern
    private static InventoryService instance;
    
    // Track sent alerts to prevent duplicates in the same session
    private Set<String> sentLowStockAlerts = new HashSet<>();
    private Set<String> sentZeroStockAlerts = new HashSet<>();
    private Set<String> sentExpirationAlerts = new HashSet<>();
    
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
                 "cost_price, selling_price, location, reorder_level, hex_id) " +
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
                 "quantity_in_stock = ?, unit = ?, cost_price = ?, selling_price = ?, location = ?, " +
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
    
    // Check for low stock items and send immediate alert (public for manual triggers)
    public void checkAndSendLowStockAlert() {
        new Thread(() -> {
            try {
                System.out.println("=== Stock Check Triggered ===");
                List<InventoryItem> allItems = getAllItems();
                List<InventoryItem> zeroStockItems = new ArrayList<>();
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
                    
                    if (qty == 0) {
                        System.out.println("  -> ZERO STOCK! CRITICAL!");
                        zeroStockItems.add(item);
                    } else if (item.isLowStock()) {
                        System.out.println("  -> LOW STOCK! (" + qty + " <= " + min + ")");
                        lowStockItems.add(item);
                    } else if (item.isLowAvailableStock()) {
                        System.out.println("  -> LOW AVAILABLE! (Available " + available + " < " + min + ")");
                        lowAvailableItems.add(item);
                    }
                }
                
                // Send critical alert for zero stock items
                if (!zeroStockItems.isEmpty()) {
                    List<InventoryItem> newZeroStock = new ArrayList<>();
                    for (InventoryItem item : zeroStockItems) {
                        if (!sentZeroStockAlerts.contains(item.getHexId())) {
                            newZeroStock.add(item);
                            sentZeroStockAlerts.add(item.getHexId());
                        }
                    }
                    if (!newZeroStock.isEmpty()) {
                        System.out.println("ZERO STOCK detected! Sending CRITICAL alert for " + newZeroStock.size() + " items.");
                        EmailService.getInstance().sendZeroStockAlert(newZeroStock);
                    }
                }
                
                // Send urgent alert for actual low stock (not already sent)
                if (!lowStockItems.isEmpty()) {
                    List<InventoryItem> newLowStock = new ArrayList<>();
                    for (InventoryItem item : lowStockItems) {
                        if (!sentLowStockAlerts.contains(item.getHexId())) {
                            newLowStock.add(item);
                            sentLowStockAlerts.add(item.getHexId());
                        }
                    }
                    if (!newLowStock.isEmpty()) {
                        System.out.println("Low stock detected! Sending urgent alert for " + newLowStock.size() + " items.");
                        EmailService.getInstance().sendLowStockAlert(newLowStock);
                    }
                }
                
                // Send advisory notice for low available stock (heavy reservations)
                if (!lowAvailableItems.isEmpty()) {
                    System.out.println("Low available stock detected! Sending advisory for " + lowAvailableItems.size() + " items.");
                    EmailService.getInstance().sendLowAvailableStockAlert(lowAvailableItems);
                }
                
                // Check for expiring/expired items
                List<InventoryItem> expiringSoonItems = new ArrayList<>();
                List<InventoryItem> expiredItems = new ArrayList<>();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate thirtyDaysFromNow = today.plusDays(30);
                
                for (InventoryItem item : allItems) {
                    if (item.getExpirationDate() != null) {
                        if (item.getExpirationDate().isBefore(today)) {
                            System.out.println("Item " + item.getName() + " EXPIRED on " + item.getExpirationDate());
                            expiredItems.add(item);
                        } else if (item.getExpirationDate().isBefore(thirtyDaysFromNow)) {
                            System.out.println("Item " + item.getName() + " expiring soon on " + item.getExpirationDate());
                            expiringSoonItems.add(item);
                        }
                    }
                }
                
                // Send expiration alerts (check for new items only)
                List<InventoryItem> newExpiredItems = new ArrayList<>();
                List<InventoryItem> newExpiringSoonItems = new ArrayList<>();
                
                for (InventoryItem item : expiredItems) {
                    String key = item.getHexId() + "_expired";
                    if (!sentExpirationAlerts.contains(key)) {
                        newExpiredItems.add(item);
                        sentExpirationAlerts.add(key);
                    }
                }
                
                for (InventoryItem item : expiringSoonItems) {
                    String key = item.getHexId() + "_expiring";
                    if (!sentExpirationAlerts.contains(key)) {
                        newExpiringSoonItems.add(item);
                        sentExpirationAlerts.add(key);
                    }
                }
                
                if (!newExpiredItems.isEmpty() || !newExpiringSoonItems.isEmpty()) {
                    try {
                        EmailService.getInstance().sendExpirationAlert(newExpiringSoonItems, newExpiredItems);
                    } catch (Exception e) {
                        System.err.println("Error sending expiration alert: " + e.getMessage());
                    }
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
    
    /**
     * Check if item is expired or expiring soon
     * @param expirationDate the expiration date
     * @param daysThreshold days before expiration to alert (default 30)
     * @return ExpirationStatus: EXPIRED, EXPIRING_SOON, OK
     */
    public enum ExpirationStatus {
        EXPIRED, EXPIRING_SOON, OK
    }
    
    public ExpirationStatus checkExpiration(LocalDate expirationDate, int daysThreshold) {
        if (expirationDate == null) {
            return ExpirationStatus.OK;  // No expiration date means no expiration
        }
        
        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(daysThreshold);
        
        if (today.isAfter(expirationDate)) {
            return ExpirationStatus.EXPIRED;
        } else if (today.isBefore(alertDate) && today.isBefore(expirationDate) || today.equals(expirationDate)) {
            return ExpirationStatus.EXPIRING_SOON;
        }
        return ExpirationStatus.OK;
    }
    
    /**
     * When restocking parts, check if any delayed bookings can now be scheduled
     */
    public void checkAndUpdateDelayedBookingsOnRestock(int partId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Find all delayed bookings that need this part
            String query = "SELECT DISTINCT sb.id, sb.mechanic_id FROM service_bookings sb " +
                          "INNER JOIN booking_parts bp ON sb.id = bp.booking_id " +
                          "WHERE sb.status = 'delayed' AND bp.part_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, partId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    int bookingId = rs.getInt("id");
                    int mechanicId = rs.getInt("mechanic_id");
                    
                    // Check if all parts for this booking are now available
                    if (checkAllPartsAvailable(bookingId)) {
                        // Update booking to scheduled
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE service_bookings SET status = 'scheduled' WHERE id = ?")) {
                            updateStmt.setInt(1, bookingId);
                            int updated = updateStmt.executeUpdate();
                            if (updated > 0) {
                                System.out.println("✓ Restock: Auto-updated booking #" + bookingId + " from delayed to scheduled");
                                
                                // Update mechanic availability
                                MechanicService mechanicService = new MechanicService();
                                mechanicService.updateMechanicAvailability(mechanicId);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if all parts for a booking are available
     */
    private boolean checkAllPartsAvailable(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT bp.part_id, bp.quantity FROM booking_parts bp WHERE bp.booking_id = ?")) {
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int partId = rs.getInt("part_id");
                int needed = rs.getInt("quantity");
                
                try (PreparedStatement partStmt = conn.prepareStatement(
                    "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?")) {
                    partStmt.setInt(1, partId);
                    ResultSet partRs = partStmt.executeQuery();
                    
                    if (partRs.next()) {
                        int inStock = partRs.getInt("quantity_in_stock");
                        int reserved = partRs.getInt("reserved_quantity");
                        int available = inStock - reserved;
                        
                        if (needed > available) {
                            return false;  // This part not available
                        }
                    }
                }
            }
        }
        return true;  // All parts available
    }
}

