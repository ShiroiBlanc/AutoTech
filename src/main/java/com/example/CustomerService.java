package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerService {
    private static CustomerService instance;
    
    private CustomerService() {
        // Initialize service
    }
    
    public static CustomerService getInstance() {
        if (instance == null) {
            instance = new CustomerService();
        }
        return instance;
    }
    
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers ORDER BY name")) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String address = rs.getString("address");
                
                customers.add(new Customer(id, hexId, name, phone, email, address));
            }
        }
        
        return customers;
    }
    
    public List<Customer> searchCustomers(String searchTerm) throws SQLException {
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM customers " +
                 "WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? OR address LIKE ? " +
                 "ORDER BY name")) {
            
            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String address = rs.getString("address");
                
                Customer customer = new Customer(id, name, phone, email, address);
                customer.setHexId(hexId);
                customers.add(customer);
            }
        }
        
        return customers;
    }
    
    public boolean addCustomer(String name, String phone, String email, String address) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO customers (hex_id, name, phone, email, address) VALUES (?, ?, ?, ?, ?)")) {
            
            String hexId = HexIdGenerator.generateCustomerId();
            stmt.setString(1, hexId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setString(5, address);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateCustomer(Customer customer) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE customers SET name = ?, phone = ?, email = ?, address = ? WHERE id = ?")) {
            
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getAddress());
            stmt.setInt(5, customer.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteCustomer(int customerId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            
            stmt.setInt(1, customerId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public Customer getCustomerById(int customerId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customers WHERE id = ?")) {
            
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                String hexId = rs.getString("hex_id");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String address = rs.getString("address");
                
                return new Customer(id, hexId, name, phone, email, address);
            }
        }
        return null;
    }
}