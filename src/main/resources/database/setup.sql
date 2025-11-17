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

-- Service items table for labor costs per service task
-- Each service can have different labor costs based on difficulty/complexity
CREATE TABLE service_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_id INT NOT NULL,
    description TEXT NOT NULL,
    labor_hours DECIMAL(5,2) DEFAULT 0,
    labor_rate DECIMAL(10,2) DEFAULT 0,  -- Labor cost per hour for this specific service task
    labor_cost DECIMAL(10,2) DEFAULT 0,  -- Total labor cost (labor_hours * labor_rate)
    notes TEXT,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- Service labor costs reference table
-- Stores standard labor costs for different service types (no hourly calculation)
CREATE TABLE service_labor_costs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(100) NOT NULL UNIQUE,
    labor_cost DECIMAL(10,2) NOT NULL,  -- Direct labor cost for this service type
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default service labor costs
INSERT INTO service_labor_costs (service_type, labor_cost) VALUES
    ('Oil Change', 150.00),
    ('Brake Replacement', 1200.00),
    ('Tire Rotation', 125.00),
    ('Engine Tune-up', 2400.00),
    ('Transmission Service', 4000.00),
    ('Air Conditioning Service', 1400.00),
    ('Battery Replacement', 100.00),
    ('Wheel Alignment', 500.00),
    ('Engine Overhaul', 18000.00),
    ('Electrical System Repair', 2700.00),
    ('Regular Maintenance', 1000.00),
    ('Tire Service', 500.00),
    ('Brake Service', 500.00),
    ('Engine Repair', 2000.00),
    ('Electrical System', 1500.00),
    ('Other', 500.00);

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

-- Create a view for service bookings details (aggregate services from booking_services)
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
    -- aggregated service types and a representative description
    GROUP_CONCAT(DISTINCT bs.service_type ORDER BY bs.id SEPARATOR ', ') AS service_types,
    MAX(bs.service_description) AS service_description,
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
    users u ON m.user_id = u.id
LEFT JOIN
    booking_services bs ON bs.booking_id = sb.id
GROUP BY
    sb.id, sb.hex_id, c.id, c.hex_id, c.name, c.phone,
    v.id, v.hex_id, v.brand, v.model, v.plate_number,
    m.id, m.hex_id, u.username,
    sb.booking_date, sb.booking_time, sb.status, sb.estimated_duration;

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

-- Sample data for service_bookings (insert booking first, then its services)
INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, booking_date, booking_time, estimated_duration)
SELECT 
    c.id AS customer_id,
    v.id AS vehicle_id,
    (SELECT id FROM mechanics LIMIT 1) AS mechanic_id,
    DATE_ADD(CURDATE(), INTERVAL 2 DAY) AS booking_date,
    '10:00:00' AS booking_time,
    60 AS estimated_duration
FROM 
    customers c
JOIN 
    vehicles v ON c.id = v.customer_id
WHERE EXISTS (SELECT 1 FROM mechanics)
LIMIT 1;

-- Attach a service to the newly inserted booking (if any row was inserted)
INSERT INTO booking_services (booking_id, service_type, service_description)
SELECT 
    (SELECT MAX(id) FROM service_bookings) AS booking_id,
    'Regular Maintenance',
    'Oil change and multi-point inspection'
WHERE EXISTS (SELECT 1 FROM service_bookings);

-- ========================================
-- POPULATE WITH TEST DATA
-- ========================================

-- Add more users
INSERT INTO users (username, password, email, role_id, active) VALUES
    ('john_admin', 'pass123', 'john@autotech.com', (SELECT id FROM roles WHERE name = 'ADMIN'), TRUE),
    ('sarah_cashier', 'pass123', 'sarah@autotech.com', (SELECT id FROM roles WHERE name = 'CASHIER'), TRUE),
    ('mike_mechanic', 'pass123', 'mike@autotech.com', (SELECT id FROM roles WHERE name = 'MECHANIC'), TRUE),
    ('lisa_mechanic', 'pass123', 'lisa@autotech.com', (SELECT id FROM roles WHERE name = 'MECHANIC'), TRUE),
    ('tom_mechanic', 'pass123', 'tom@autotech.com', (SELECT id FROM roles WHERE name = 'MECHANIC'), TRUE),
    ('inactive_user', 'pass123', 'inactive@autotech.com', (SELECT id FROM roles WHERE name = 'CASHIER'), FALSE);

-- Add mechanics for the new mechanic users
INSERT INTO mechanics (user_id, specialties, hex_id, availability) VALUES
    ((SELECT id FROM users WHERE username = 'mike_mechanic'), 'Engine Repair, Transmission', 'MECH-00000002', 'Available'),
    ((SELECT id FROM users WHERE username = 'lisa_mechanic'), 'Electrical Systems, Air Conditioning', 'MECH-00000003', 'Busy'),
    ((SELECT id FROM users WHERE username = 'tom_mechanic'), 'Brake Systems, Suspension', 'MECH-00000004', 'Available');

-- Add customers
INSERT INTO customers (hex_id, name, phone, email, address) VALUES
    ('CUST-00000001', 'Roberto Santos', '09171234567', 'roberto.santos@email.com', '123 Rizal St, Manila'),
    ('CUST-00000002', 'Maria Garcia', '09181234567', 'maria.garcia@email.com', '456 Quezon Ave, Quezon City'),
    ('CUST-00000003', 'Juan Dela Cruz', '09191234567', 'juan.delacruz@email.com', '789 Makati Ave, Makati'),
    ('CUST-00000004', 'Ana Reyes', '09201234567', 'ana.reyes@email.com', '321 Ortigas St, Pasig'),
    ('CUST-00000005', 'Carlos Mendoza', '09211234567', 'carlos.mendoza@email.com', '654 EDSA, Mandaluyong'),
    ('CUST-00000006', 'Sofia Aquino', '09221234567', 'sofia.aquino@email.com', '987 Taft Ave, Pasay'),
    ('CUST-00000007', 'Miguel Torres', '09231234567', 'miguel.torres@email.com', '147 España, Sampaloc'),
    ('CUST-00000008', 'Elena Ramos', '09241234567', 'elena.ramos@email.com', '258 Commonwealth, QC'),
    ('CUST-00000009', 'Diego Fernandez', '09251234567', 'diego.fernandez@email.com', '369 Aurora Blvd, San Juan'),
    ('CUST-00000010', 'Isabel Cruz', '09261234567', 'isabel.cruz@email.com', '741 Shaw Blvd, Pasig');

-- Add vehicles
INSERT INTO vehicles (hex_id, customer_id, type, brand, model, year, plate_number) VALUES
    ('VEH-00000001', 1, 'Sedan', 'Toyota', 'Vios', '2020', 'ABC-1234'),
    ('VEH-00000002', 1, 'SUV', 'Toyota', 'Fortuner', '2021', 'XYZ-5678'),
    ('VEH-00000003', 2, 'Sedan', 'Honda', 'Civic', '2019', 'DEF-2468'),
    ('VEH-00000004', 3, 'Hatchback', 'Mitsubishi', 'Mirage', '2022', 'GHI-1357'),
    ('VEH-00000005', 4, 'SUV', 'Ford', 'Everest', '2020', 'JKL-9876'),
    ('VEH-00000006', 5, 'Sedan', 'Mazda', 'Mazda3', '2021', 'MNO-5432'),
    ('VEH-00000007', 6, 'SUV', 'Hyundai', 'Tucson', '2023', 'PQR-8765'),
    ('VEH-00000008', 7, 'Van', 'Toyota', 'Hiace', '2018', 'STU-4321'),
    ('VEH-00000009', 8, 'Sedan', 'Nissan', 'Almera', '2020', 'VWX-6543'),
    ('VEH-00000010', 9, 'SUV', 'Isuzu', 'MU-X', '2022', 'YZA-3210'),
    ('VEH-00000011', 10, 'Sedan', 'Kia', 'Soluto', '2021', 'BCD-7890'),
    ('VEH-00000012', 3, 'Motorcycle', 'Honda', 'Wave', '2019', 'EFG-1111');

-- Add parts inventory
INSERT INTO parts (hex_id, part_number, name, description, cost_price, selling_price, quantity_in_stock, reorder_level, category, location, supplier) VALUES
    ('PART-00000001', 'OIL-001', 'Engine Oil 5W-30', 'Fully synthetic engine oil', 250.00, 400.00, 50, 10, 'Lubricants', 'Bodega', 'Shell Philippines'),
    ('PART-00000002', 'FILTER-001', 'Oil Filter', 'Standard oil filter', 80.00, 150.00, 100, 20, 'Filters', 'Bodega', 'Denso'),
    ('PART-00000003', 'BRAKE-001', 'Brake Pads Front', 'Ceramic brake pads', 800.00, 1500.00, 30, 5, 'Brakes', 'Bodega', 'Brembo'),
    ('PART-00000004', 'BRAKE-002', 'Brake Pads Rear', 'Ceramic brake pads', 700.00, 1300.00, 30, 5, 'Brakes', 'Bodega', 'Brembo'),
    ('PART-00000005', 'TIRE-001', 'Tire 185/65R15', 'All-season tire', 2000.00, 3500.00, 20, 8, 'Tires', 'Bodega', 'Goodyear'),
    ('PART-00000006', 'BATTERY-001', 'Car Battery 12V', '60Ah maintenance-free battery', 2500.00, 4000.00, 15, 5, 'Electrical', 'Bodega', 'Motolite'),
    ('PART-00000007', 'SPARK-001', 'Spark Plugs', 'Iridium spark plugs set of 4', 400.00, 800.00, 40, 10, 'Engine', 'Bodega', 'NGK'),
    ('PART-00000008', 'AIRFILTER-001', 'Air Filter', 'Engine air filter', 150.00, 300.00, 60, 15, 'Filters', 'Bodega', 'Mann Filter'),
    ('PART-00000009', 'COOLANT-001', 'Coolant 1L', 'Engine coolant', 180.00, 350.00, 45, 10, 'Fluids', 'Bodega', 'Prestone'),
    ('PART-00000010', 'WIPER-001', 'Wiper Blades Pair', 'Frameless wiper blades', 200.00, 450.00, 35, 8, 'Accessories', 'Bodega', 'Bosch');

-- Add service bookings (past, current, and future)
INSERT INTO service_bookings (hex_id, customer_id, vehicle_id, mechanic_id, booking_date, booking_time, status, estimated_duration) VALUES
    ('BOOK-00000001', 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:00:00', 'completed', 60),
    ('BOOK-00000002', 2, 3, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '10:30:00', 'completed', 120),
    ('BOOK-00000003', 3, 4, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00', 'completed', 90),
    ('BOOK-00000004', 4, 5, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:00:00', 'in_progress', 180),
    ('BOOK-00000005', 5, 6, 2, CURDATE(), '08:00:00', 'scheduled', 60),
    ('BOOK-00000006', 6, 7, 3, CURDATE(), '13:00:00', 'scheduled', 120),
    ('BOOK-00000007', 7, 8, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'scheduled', 150),
    ('BOOK-00000008', 8, 9, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', 'scheduled', 90),
    ('BOOK-00000009', 9, 10, 3, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '15:00:00', 'scheduled', 120),
    ('BOOK-00000010', 10, 11, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:00:00', 'scheduled', 60);

-- Add booking services
INSERT INTO booking_services (booking_id, service_type, service_description) VALUES
    (1, 'Oil Change', 'Regular oil change with filter replacement'),
    (2, 'Brake Service', 'Front brake pad replacement'),
    (3, 'Regular Maintenance', 'Scheduled 20,000km maintenance'),
    (4, 'Engine Repair', 'Check engine light diagnostics'),
    (5, 'Oil Change', 'Synthetic oil change'),
    (6, 'Tire Service', 'Tire rotation and balancing'),
    (7, 'Air Conditioning Service', 'AC system cleaning and recharge'),
    (8, 'Regular Maintenance', 'Scheduled 10,000km maintenance'),
    (9, 'Transmission Service', 'ATF replacement'),
    (10, 'Battery Replacement', 'Battery replacement and testing');

-- Add booking parts
INSERT INTO booking_parts (booking_id, part_id, quantity, price_at_time) VALUES
    (1, 1, 4, 400.00),  -- Oil Change: Engine Oil
    (1, 2, 1, 150.00),  -- Oil Change: Oil Filter
    (2, 3, 1, 1500.00), -- Brake Service: Front Brake Pads
    (3, 1, 4, 400.00),  -- Maintenance: Engine Oil
    (3, 2, 1, 150.00),  -- Maintenance: Oil Filter
    (3, 7, 1, 800.00),  -- Maintenance: Spark Plugs
    (4, 8, 1, 300.00),  -- Engine Repair: Air Filter
    (5, 1, 4, 400.00),  -- Oil Change: Engine Oil
    (5, 2, 1, 150.00),  -- Oil Change: Oil Filter
    (10, 6, 1, 4000.00); -- Battery Replacement: Car Battery

-- Add completed services
INSERT INTO services (booking_id, vehicle_id, service_date, service_type, description, status, odometer, technician_id, notes) VALUES
    (1, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'Oil Change', 'Regular oil change completed', 'completed', 45000, 1, 'Customer requested synthetic oil next time'),
    (2, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'Brake Service', 'Front brake pads replaced', 'completed', 62000, 2, 'Rear brakes still at 40%'),
    (3, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'Regular Maintenance', '20,000km service completed', 'completed', 20000, 3, 'All systems checked and OK');

-- Add billing for completed services
INSERT INTO billing (hex_id, customer_id, service_id, amount, payment_status, payment_method, reference_number, bill_date) VALUES
    ('BILL-00000001', 1, 1, 1750.00, 'Paid', 'Cash', NULL, DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
    ('BILL-00000002', 2, 2, 2000.00, 'Paid', 'Bank Transfer', 'REF-20250101', DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
    ('BILL-00000003', 3, 3, 2450.00, 'Unpaid', NULL, NULL, DATE_SUB(CURDATE(), INTERVAL 3 DAY));

-- Add tasks
INSERT INTO tasks (title, description, status, priority, due_date, assigned_to, created_by) VALUES
    ('Check brake fluid levels', 'Inspect all vehicles for brake fluid levels', 'pending', 'MEDIUM', CURDATE(), 1, 1),
    ('Order new tire stock', 'Stock running low on 185/65R15 tires', 'in_progress', 'HIGH', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, 1),
    ('Clean workshop area', 'Monthly deep cleaning of workshop', 'pending', 'LOW', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 3, 1),
    ('Update service manual', 'Add new procedures for hybrid vehicles', 'completed', 'MEDIUM', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 1),
    ('Calibrate diagnostic tools', 'Annual calibration of OBD scanners', 'pending', 'HIGH', DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, 1);