-- Drop database if it exists and create new one
DROP DATABASE IF EXISTS AutoTech;
CREATE DATABASE AutoTech;
USE AutoTech;

-- Create users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL
);

-- Create tasks table
CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Insert test user (optional)
INSERT INTO users (username, password, email) 
VALUES ('admin', 'password', 'admin@example.com');
