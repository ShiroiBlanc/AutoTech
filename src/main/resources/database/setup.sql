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

-- Create users table with role
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Insert default users
INSERT INTO users (username, password, email, role_id) VALUES
    ('admin', 'admin123', 'admin@autotech.com', (SELECT id FROM roles WHERE name = 'ADMIN')),
    ('cashier', 'cash123', 'cashier@autotech.com', (SELECT id FROM roles WHERE name = 'CASHIER')),
    ('mechanic', 'mech123', 'mechanic@autotech.com', (SELECT id FROM roles WHERE name = 'MECHANIC'));

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
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',  -- Added priority field
    due_date DATE,                                  -- Added due date
    assigned_to INT,                                -- Added mechanic assignment
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (assigned_to) REFERENCES mechanics(id)
);

