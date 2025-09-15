-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50),
    year VARCHAR(10),
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_customer_name ON customers(name);
CREATE INDEX IF NOT EXISTS idx_customer_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_vehicle_customer ON vehicles(customer_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_plate ON vehicles(plate_number);

-- Insert sample data
INSERT INTO customers (name, phone, email, address) VALUES
('John Smith', '555-1234', 'john@example.com', '123 Main St, Anytown'),
('Sarah Johnson', '555-5678', 'sarah@example.com', '456 Oak Ave, Somewhere'),
('Michael Brown', '555-9012', 'michael@example.com', '789 Pine Rd, Nowhere');

-- Insert sample vehicles
INSERT INTO vehicles (customer_id, type, brand, model, year, plate_number) VALUES
(1, 'Sedan', 'Toyota', 'Camry', '2020', 'ABC123'),
(1, 'SUV', 'Honda', 'CR-V', '2018', 'XYZ789'),
(2, 'Truck', 'Ford', 'F-150', '2021', 'DEF456');
