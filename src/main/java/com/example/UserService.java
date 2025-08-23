package com.example;

import java.sql.*;
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
        
        try (Connection conn = DatabaseUtil.getConnection()) {
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
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String storedPass = rs.getString("password");
                    System.out.println("Found user, checking password");
                    
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
                                UserRole.fromString(roleName)
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
}
