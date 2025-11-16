package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/AutoTech";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    
    static {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load MySQL JDBC driver");
        }
    }
    
    public static Connection getConnection() throws SQLException {
        // Always create a new connection instead of reusing
        // This prevents "connection closed" errors with background threads
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    // Cleanup method (no longer needed since connections are created per request)
    public static void closeConnection() {
        // Connections are now closed automatically via try-with-resources
        System.out.println("Database connection cleanup completed");
    }
}