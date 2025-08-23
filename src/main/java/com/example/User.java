package com.example;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private UserRole role;

    public enum UserRole {
        ADMIN,
        CASHIER,
        MECHANIC;
        
        public static UserRole fromString(String roleStr) {
            try {
                return valueOf(roleStr.toUpperCase());
            } catch (Exception e) {
                System.err.println("Invalid role: " + roleStr);
                return MECHANIC; // Default role if parsing fails
            }
        }
    }

    // Make sure this constructor exists and matches what UserService is calling
    public User(int id, String username, String password, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    
    public User(String username, String password, String email, UserRole role) {
        this(0, username, password, email, role);
    }

    // Getters
    public int getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public UserRole getRole() {
        return role;
    }
}
