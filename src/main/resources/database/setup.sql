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
    active BOOLEAN NOT NULL DEFAULT TRUE,  -- Added active status column
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

-- Create a view for user management in the admin panel
CREATE OR REPLACE VIEW user_management_view AS
SELECT 
    u.id,
    u.username,
    u.email,
    r.name AS role_name,
    u.active
FROM 
    users u
JOIN 
    roles r ON u.role_id = r.id;

-- Create stored procedure to toggle user active status
DELIMITER //
CREATE PROCEDURE toggle_user_status(IN user_id INT, IN new_status BOOLEAN)
BEGIN
    UPDATE users SET active = new_status WHERE id = user_id;
END //
DELIMITER ;

-- Create stored procedure to delete a user
DELIMITER //
CREATE PROCEDURE delete_user(IN user_id INT)
BEGIN
    -- Check if user exists
    DECLARE user_exists INT;
    SELECT COUNT(*) INTO user_exists FROM users WHERE id = user_id;
    
    IF user_exists > 0 THEN
        -- Delete any foreign key references first
        DELETE FROM mechanics WHERE user_id = user_id;
        
        -- Then delete the user
        DELETE FROM users WHERE id = user_id;
    END IF;
END //
DELIMITER ;

-- Add indexes for faster searching
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_user_email ON users(email);

