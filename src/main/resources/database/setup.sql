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


-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    customer_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50),
    year VARCHAR(10),
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Create mechanics table
CREATE TABLE mechanics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    user_id INT NOT NULL,
    specialties TEXT,  -- Changed to TEXT to store multiple specialties as comma-separated values
    availability VARCHAR(20) DEFAULT 'Available' 
        CHECK (availability IN ('Available', 'Busy', 'Off Duty')),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create default mechanic entry with hex_id
INSERT INTO mechanics (user_id, specialties, hex_id) VALUES
    ((SELECT id FROM users WHERE username = 'mechanic'), 'General Repairs', 'MECH-00000001');

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

-- Create service_bookings table for scheduling appointments
CREATE TABLE service_bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    mechanic_id INT NOT NULL,
    booking_date DATE NOT NULL,
    booking_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'scheduled' 
        CHECK (status IN ('scheduled', 'delayed', 'in_progress', 'completed', 'cancelled')),
    original_status VARCHAR(20) DEFAULT 'scheduled'
        CHECK (original_status IN ('scheduled', 'delayed', 'in_progress', 'completed', 'cancelled')),
    promoted_by_booking_id INT DEFAULT NULL,  -- Tracks which booking caused this one to be promoted from delayed to scheduled
    estimated_duration INT,  -- in minutes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanics(id)
);

-- Services table for repair orders
CREATE TABLE services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT,
    vehicle_id INT NOT NULL,
    service_date DATE NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' 
        CHECK (status IN ('pending', 'in_progress', 'completed', 'cancelled')),
    odometer INT,
    technician_id INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES service_bookings(id) ON DELETE SET NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (technician_id) REFERENCES mechanics(id)
);

-- Booking services junction table (multiple services per booking)
CREATE TABLE IF NOT EXISTS booking_services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    service_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES service_bookings(id) ON DELETE CASCADE
);

-- Service items table for labor
CREATE TABLE service_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_id INT NOT NULL,
    description TEXT NOT NULL,
    labor_hours DECIMAL(5,2) DEFAULT 0,
    labor_rate DECIMAL(10,2) DEFAULT 0,
    labor_cost DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- Parts inventory table
CREATE TABLE parts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    part_number VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    cost_price DECIMAL(10,2) NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    quantity_in_stock INT DEFAULT 0,
    reserved_quantity INT DEFAULT 0,
    expiration_date DATE,
    unit VARCHAR(20) DEFAULT 'pieces',
    reorder_level INT DEFAULT 5,
    category VARCHAR(50),
    location VARCHAR(100) DEFAULT 'Bodega',
    supplier VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Booking parts junction table (which parts will be used in which booking)
CREATE TABLE IF NOT EXISTS booking_parts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    part_id INT NOT NULL,
    quantity INT NOT NULL,
    price_at_time DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES service_bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT
);

-- Service parts junction table (which parts were used in which service)
CREATE TABLE service_parts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_id INT NOT NULL,
    part_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT
);

-- Invoices table
CREATE TABLE invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    service_id INT NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    invoice_number VARCHAR(20) UNIQUE,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' 
        CHECK (status IN ('pending', 'partial', 'paid', 'overdue', 'cancelled')),
    subtotal DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    notes TEXT,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Invoice payments table
CREATE TABLE invoice_payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT NOT NULL,
    payment_date DATE NOT NULL,
    payment_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(50),
    notes TEXT,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Create billing table
CREATE TABLE billing (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hex_id VARCHAR(20) UNIQUE,
    customer_id INT NOT NULL,
    service_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'Unpaid'
        CHECK (payment_status IN ('Paid', 'Unpaid', 'Partial', 'Cancelled')),
    payment_method VARCHAR(50) DEFAULT NULL
        CHECK (payment_method IS NULL OR payment_method IN ('Cash', 'Bank Transfer', 'Online Payment')),
    reference_number VARCHAR(100) DEFAULT NULL,
    bill_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_id) REFERENCES service_bookings(id) ON DELETE RESTRICT
);

-- Create email sent history table
CREATE TABLE email_sent_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    billing_id INT NOT NULL,
    recipient_email VARCHAR(100) NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    sent_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'sent' CHECK (status IN ('sent', 'failed', 'pending')),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (billing_id) REFERENCES billing(id) ON DELETE CASCADE,
    INDEX idx_billing_id (billing_id),
    INDEX idx_sent_date (sent_date),
    INDEX idx_recipient_email (recipient_email)
);

-- Add indexes for the billing table
CREATE INDEX idx_billing_customer ON billing(customer_id);
CREATE INDEX idx_billing_service ON billing(service_id);
CREATE INDEX idx_billing_status ON billing(payment_status);

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


-- Add indexes for faster searching
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_user_email ON users(email);

-- Add indexes for service_bookings
CREATE INDEX idx_bookings_customer ON service_bookings(customer_id);
CREATE INDEX idx_bookings_vehicle ON service_bookings(vehicle_id);
CREATE INDEX idx_bookings_mechanic ON service_bookings(mechanic_id);
CREATE INDEX idx_bookings_date ON service_bookings(booking_date);
CREATE INDEX idx_bookings_status ON service_bookings(status);
CREATE INDEX idx_bookings_time ON service_bookings(booking_time);

-- Create indexes for the service tables
CREATE INDEX idx_services_vehicle_id ON services(vehicle_id);
CREATE INDEX idx_services_technician_id ON services(technician_id);
CREATE INDEX idx_services_booking_id ON services(booking_id);
CREATE INDEX idx_service_items_service_id ON service_items(service_id);
CREATE INDEX idx_service_parts_service_id ON service_parts(service_id);
CREATE INDEX idx_service_parts_part_id ON service_parts(part_id);
CREATE INDEX idx_invoices_service_id ON invoices(service_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoice_payments_invoice_id ON invoice_payments(invoice_id);

-- Create a view for service bookings details
CREATE OR REPLACE VIEW service_booking_details AS
SELECT 
    sb.id AS booking_id,
    sb.hex_id AS booking_hex_id,
    c.id AS customer_id,
    c.hex_id AS customer_hex_id,
    c.name AS customer_name,
    c.phone AS customer_phone,
    v.id AS vehicle_id,
    v.hex_id AS vehicle_hex_id,
    CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') AS vehicle,
    m.id AS mechanic_id,
    m.hex_id AS mechanic_hex_id,
    sb.service_type,
    sb.service_description,
    u.username AS mechanic_name,
    sb.booking_date,
    sb.booking_time,
    sb.status,
    sb.estimated_duration
FROM 
    service_bookings sb
JOIN 
    customers c ON sb.customer_id = c.id
JOIN 
    vehicles v ON sb.vehicle_id = v.id
LEFT JOIN 
    mechanics m ON sb.mechanic_id = m.id
LEFT JOIN 
    users u ON m.user_id = u.id;

-- Create view for service details
CREATE OR REPLACE VIEW service_details AS
SELECT 
    s.id AS service_id,
    v.id AS vehicle_id,
    v.hex_id AS vehicle_hex_id,
    c.id AS customer_id,
    c.hex_id AS customer_hex_id,
    c.name AS customer_name,
    v.brand,
    v.model,
    v.plate_number,
    s.service_date,
    s.status AS service_status,
    s.odometer,
    m.hex_id AS mechanic_hex_id,
    u.username AS technician_name,
    s.description,
    (SELECT COALESCE(SUM(si.labor_cost), 0) FROM service_items si WHERE si.service_id = s.id) AS labor_cost,
    (SELECT COALESCE(SUM(sp.price * sp.quantity), 0) FROM service_parts sp WHERE sp.service_id = s.id) AS parts_cost,
    (SELECT COALESCE(SUM(si.labor_cost), 0) FROM service_items si WHERE si.service_id = s.id) +
    (SELECT COALESCE(SUM(sp.price * sp.quantity), 0) FROM service_parts sp WHERE sp.service_id = s.id) AS total_cost
FROM 
    services s
JOIN 
    vehicles v ON s.vehicle_id = v.id
JOIN 
    customers c ON v.customer_id = c.id
LEFT JOIN 
    mechanics m ON s.technician_id = m.id
LEFT JOIN 
    users u ON m.user_id = u.id;

-- Sample data for service_bookings
INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, service_type, service_description, booking_date, booking_time, estimated_duration)
SELECT 
    c.id AS customer_id,
    v.id AS vehicle_id,
    (SELECT id FROM mechanics LIMIT 1) AS mechanic_id,
    'Regular Maintenance' AS service_type,
    'Oil change and multi-point inspection' AS service_description,
    DATE_ADD(CURDATE(), INTERVAL 2 DAY) AS booking_date,
    '10:00:00' AS booking_time,
    60 AS estimated_duration
FROM 
    customers c
JOIN 
    vehicles v ON c.id = v.customer_id
WHERE EXISTS (SELECT 1 FROM mechanics)
LIMIT 1;
