package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.example.User.UserRole;

public class UserService {
    private static UserService instance;
    private User currentUser;

    private UserService() {
        // Initialize service
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public boolean authenticate(String username, String password) {
        System.out.println("Attempting to authenticate user: " + username);
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.id, u.username, u.password, u.email, r.name as role_name, u.active " +
                 "FROM users u JOIN roles r ON u.role_id = r.id " +
                 "WHERE u.username = ?")) {
            
            // Test connection
            if (conn == null) {
                System.err.println("ERROR: Database connection is null");
                return false;
            }
            System.out.println("Database connection successful");
            
            // Prepare and execute query
            String query = "SELECT u.id, u.username, u.password, u.email, r.name as role_name " +
                          "FROM users u JOIN roles r ON u.role_id = r.id " +
                          "WHERE u.username = ?";
            System.out.println("Executing query: " + query);
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedPass = rs.getString("password");
                boolean active = rs.getBoolean("active");
                
                // Check if account is active
                if (!active) {
                    System.out.println("Login attempt by disabled user: " + username);
                    return false;
                }
                
                if (storedPass.equals(password)) {
                    int userId = rs.getInt("id");
                    String email = rs.getString("email");
                    String roleName = rs.getString("role_name");
                    
                    try {
                        currentUser = new User(
                            userId, 
                            username,
                            password,
                            email,
                            UserRole.fromString(roleName),
                            active
                        );
                        System.out.println("Authentication successful. Role: " + roleName);
                        return true;
                    } catch (Exception e) {
                        System.err.println("Error creating User object: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Password mismatch");
                }
            } else {
                System.out.println("No user found with username: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error during authentication: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }
    
    public boolean addUser(String username, String password, String email, UserRole role) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO users (username, password, email, role_id) " +
                 "VALUES (?, ?, ?, (SELECT id FROM roles WHERE name = ?))")) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, role.toString());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateUserRole(String username, UserRole newRole) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET role_id = (SELECT id FROM roles WHERE name = ?) " +
                 "WHERE username = ?")) {
            
            stmt.setString(1, newRole.toString());
            stmt.setString(2, username);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get all users for admin panel
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.id, u.username, u.password, u.email, r.name as role_name, u.active " +
                 "FROM users u JOIN roles r ON u.role_id = r.id " +
                 "ORDER BY u.id")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String roleName = rs.getString("role_name");
                boolean active = rs.getBoolean("active");
                
                users.add(new User(
                    id, username, password, email, 
                    UserRole.fromString(roleName), active
                ));
            }
        }
        
        return users;
    }

    // Search users by username or email
    public List<User> searchUsers(String searchTerm) throws SQLException {
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.id, u.username, u.password, u.email, r.name as role_name, u.active " +
                 "FROM users u JOIN roles r ON u.role_id = r.id " +
                 "WHERE u.username LIKE ? OR u.email LIKE ? " +
                 "ORDER BY u.id")) {
            
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String roleName = rs.getString("role_name");
                boolean active = rs.getBoolean("active");
                
                users.add(new User(
                    id, username, password, email, 
                    UserRole.fromString(roleName), active
                ));
            }
        }
        
        return users;
    }

    // Toggle user active status
    public boolean toggleUserStatus(int userId, boolean newStatus) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET active = ? WHERE id = ?")) {
            
            stmt.setBoolean(1, newStatus);
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Delete user
    public boolean deleteUser(int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // First delete from mechanics table if exists
            try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM mechanics WHERE user_id = ?")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            // Then delete the user
            try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM users WHERE id = ?")) {
                stmt.setInt(1, userId);
                int rowsAffected = stmt.executeUpdate();
                
                conn.commit();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

