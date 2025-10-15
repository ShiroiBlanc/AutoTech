package com.example;

import java.sql.*;
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
    
    public List<InventoryItem> getAllItems() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM inventory_items ORDER BY name")) {
            
            while (rs.next()) {
                items.add(extractInventoryItemFromResultSet(rs));
            }
        }
        
        return items;
    }
    
    public boolean addItem(String partNumber, String name, String category, int quantity, 
                          double costPrice, double sellingPrice, String location, int minimumStock) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO inventory_items (part_number, name, category, quantity, " +
                 "cost_price, selling_price, location, minimum_stock) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, partNumber);
            stmt.setString(2, name);
            stmt.setString(3, category);
            stmt.setInt(4, quantity);
            stmt.setDouble(5, costPrice);
            stmt.setDouble(6, sellingPrice);
            stmt.setString(7, location);
            stmt.setInt(8, minimumStock);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateItem(InventoryItem item) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE inventory_items SET part_number = ?, name = ?, category = ?, " +
                 "quantity = ?, cost_price = ?, selling_price = ?, location = ?, " +
                 "minimum_stock = ? WHERE id = ?")) {
            
            stmt.setString(1, item.getPartNumber());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getCategory());
            stmt.setInt(4, item.getQuantity());
            stmt.setDouble(5, item.getCostPrice());
            stmt.setDouble(6, item.getSellingPrice());
            stmt.setString(7, item.getLocation());
            stmt.setInt(8, item.getMinimumStock());
            stmt.setInt(9, item.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteItem(int id) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM inventory_items WHERE id = ?")) {
            
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
                 "SELECT * FROM inventory_items " +
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
        String partNumber = rs.getString("part_number");
        String name = rs.getString("name");
        String category = rs.getString("category");
        int quantity = rs.getInt("quantity");
        double costPrice = rs.getDouble("cost_price");
        double sellingPrice = rs.getDouble("selling_price");
        String location = rs.getString("location");
        int minimumStock = rs.getInt("minimum_stock");
        
        return new InventoryItem(id, partNumber, name, category, 
                                quantity, costPrice, sellingPrice, location, minimumStock);
    }
    
    // Method to update item quantity (for restocking)
    public boolean updateItemQuantity(int itemId, int newQuantity) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE inventory_items SET quantity = ? WHERE id = ?")) {
            
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, itemId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
