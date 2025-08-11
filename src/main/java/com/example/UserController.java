package com.example;

import java.sql.*;

public class UserController {
    
    public static boolean signup(String username, String password, String email) throws SQLException {
        // Check if username already exists
        if (userExists(username)) {
            return false;
        }

        String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // Note: In production, password should be hashed
            stmt.setString(3, email);
            stmt.executeUpdate();
            return true;
        }
    }

    private static boolean userExists(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }
}
