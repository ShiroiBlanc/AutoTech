package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/AutoTech";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    
    // Add this to keep track of connections
    private static Connection activeConnection = null;
    
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
        if (activeConnection == null || activeConnection.isClosed()) {
            activeConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return activeConnection;
    }
    
    // Add this missing method
    public static void closeConnection() {
        if (activeConnection != null) {
            try {
                if (!activeConnection.isClosed()) {
                    activeConnection.close();
                }
                activeConnection = null;
                System.out.println("Database connection closed successfully");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}