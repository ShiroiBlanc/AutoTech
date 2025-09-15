-- Drop database if it exists and create new one
DROP DATABASE IF EXISTS AutoTech;
CREATE DATABASE AutoTech;
USE AutoTech;

-- Create roles table
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Insert default roles
INSERT INTO roles (name) VALUES 
    ('ADMIN'),
    ('CASHIER'),
    ('MECHANIC');

-- Create users table with role and active status
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Insert default users
INSERT INTO users (username, password, email, role_id, active) VALUES
    ('admin', 'admin123', 'admin@autotech.com', (SELECT id FROM roles WHERE name = 'ADMIN'), TRUE),
    ('cashier', 'cash123', 'cashier@autotech.com', (SELECT id FROM roles WHERE name = 'CASHIER'), TRUE),
    ('mechanic', 'mech123', 'mechanic@autotech.com', (SELECT id FROM roles WHERE name = 'MECHANIC'), TRUE);

-- Create mechanics table
CREATE TABLE mechanics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    specialty VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create default mechanic entry
INSERT INTO mechanics (user_id, specialty) VALUES
    ((SELECT id FROM users WHERE username = 'mechanic'), 'General Repairs');

-- Create tasks table
CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,  
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    assigned_to INT,
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (assigned_to) REFERENCES mechanics(id)
);

-- Create customers table
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255)
);

-- Add some sample customers
INSERT INTO customers (name, phone, email, address) VALUES
    ('John Smith', '555-1234', 'john@example.com', '123 Main St, Anytown'),
    ('Jane Doe', '555-5678', 'jane@example.com', '456 Oak Ave, Somewhere'),
    ('Bob Johnson', '555-9012', 'bob@example.com', '789 Pine Rd, Nowhere');