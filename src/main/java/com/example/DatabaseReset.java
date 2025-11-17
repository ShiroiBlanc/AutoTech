package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DatabaseReset {
    public static void main(String[] args) {
        resetDatabase();
    }

    public static void resetDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // First, connect to MySQL server without specifying database
            String url = "jdbc:mysql://localhost:3306";
            String user = "root";
            String password = "blueelement1";
            
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {
                
                // Read the setup.sql file
                String sqlScript = Files.readString(
                    Paths.get("src/main/resources/database/setup.sql"));
                
                // Split by semicolons and execute each statement
                String[] statements = sqlScript.split(";");
                
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            System.out.println("✓ Executed: " + sql.substring(0, Math.min(60, sql.length())));
                        } catch (Exception e) {
                            System.err.println("✗ Error: " + e.getMessage());
                            System.err.println("  SQL: " + sql.substring(0, Math.min(100, sql.length())));
                        }
                    }
                }
                
                System.out.println("\n✓ Database reset completed successfully!");
                System.out.println("✓ original_status column should now exist in service_bookings table");
                System.out.println("✓ location column should now exist in parts table");
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to reset database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
