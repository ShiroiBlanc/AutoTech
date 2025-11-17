# AutoTech Database - Entity Relationship Diagram

## Database Overview
**Database Name:** AutoTech  
**Purpose:** Auto repair shop management system

---

## Tables and Relationships

### 1. **roles**
- **PK:** id (INT, AUTO_INCREMENT)
- **Attributes:**
  - name (VARCHAR(50), UNIQUE, NOT NULL)
- **Data:** ADMIN, CASHIER, MECHANIC

### 2. **users**
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** role_id → roles(id)
- **Attributes:**
  - username (VARCHAR(50), UNIQUE, NOT NULL)
  - password (VARCHAR(255), NOT NULL)
  - email (VARCHAR(100), NOT NULL)
  - active (BOOLEAN, DEFAULT TRUE)
- **Relationships:**
  - ONE role → MANY users

### 3. **customers**
- **PK:** id (INT, AUTO_INCREMENT)
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - name (VARCHAR(100), NOT NULL)
  - phone (VARCHAR(20))
  - email (VARCHAR(100))
  - address (TEXT)
  - created_at (TIMESTAMP)

### 4. **vehicles**
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** customer_id → customers(id) ON DELETE CASCADE
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - type (VARCHAR(50), NOT NULL)
  - brand (VARCHAR(50), NOT NULL)
  - model (VARCHAR(50))
  - year (VARCHAR(10))
  - plate_number (VARCHAR(20), UNIQUE, NOT NULL)
  - created_at (TIMESTAMP)
- **Relationships:**
  - ONE customer → MANY vehicles

### 5. **mechanics**
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** user_id → users(id)
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - specialties (TEXT)
  - availability (VARCHAR(20), CHECK: 'Available', 'Busy', 'Off Duty')
- **Relationships:**
  - ONE user → ONE mechanic

### 6. **service_bookings**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - customer_id → customers(id)
  - vehicle_id → vehicles(id)
  - mechanic_id → mechanics(id)
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - booking_date (DATE, NOT NULL)
  - booking_time (TIME, NOT NULL)
  - status (VARCHAR(20), CHECK: 'scheduled', 'delayed', 'in_progress', 'completed', 'cancelled')
  - original_status (VARCHAR(20))
  - promoted_by_booking_id (INT)
  - estimated_duration (INT)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)
- **Relationships:**
  - ONE customer → MANY bookings
  - ONE vehicle → MANY bookings
  - ONE mechanic → MANY bookings

### 7. **booking_services** (Junction Table)
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** booking_id → service_bookings(id) ON DELETE CASCADE
- **Attributes:**
  - service_type (VARCHAR(100), NOT NULL)
  - service_description (TEXT)
  - created_at (TIMESTAMP)
- **Relationships:**
  - ONE booking → MANY services (Many-to-Many relationship)

### 8. **services**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - booking_id → service_bookings(id) ON DELETE SET NULL
  - vehicle_id → vehicles(id) ON DELETE CASCADE
  - technician_id → mechanics(id)
- **Attributes:**
  - service_date (DATE, NOT NULL)
  - service_type (VARCHAR(100), NOT NULL)
  - description (TEXT)
  - status (VARCHAR(20), CHECK: 'pending', 'in_progress', 'completed', 'cancelled')
  - odometer (INT)
  - notes (TEXT)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

### 9. **service_items**
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** service_id → services(id) ON DELETE CASCADE
- **Attributes:**
  - description (TEXT, NOT NULL)
  - labor_hours (DECIMAL(5,2))
  - labor_rate (DECIMAL(10,2))
  - labor_cost (DECIMAL(10,2))
  - notes (TEXT)
- **Relationships:**
  - ONE service → MANY service_items

### 10. **parts** (Inventory)
- **PK:** id (INT, AUTO_INCREMENT)
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - part_number (VARCHAR(50), UNIQUE)
  - name (VARCHAR(100), NOT NULL)
  - description (TEXT)
  - cost_price (DECIMAL(10,2), NOT NULL)
  - selling_price (DECIMAL(10,2), NOT NULL)
  - quantity_in_stock (INT, DEFAULT 0)
  - reserved_quantity (INT, DEFAULT 0)
  - expiration_date (DATE)
  - unit (VARCHAR(20), DEFAULT 'pieces')
  - reorder_level (INT, DEFAULT 5)
  - category (VARCHAR(50))
  - location (VARCHAR(100), DEFAULT 'Bodega')
  - supplier (VARCHAR(100))
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

### 11. **booking_parts** (Junction Table)
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - booking_id → service_bookings(id) ON DELETE CASCADE
  - part_id → parts(id) ON DELETE RESTRICT
- **Attributes:**
  - quantity (INT, NOT NULL)
  - price_at_time (DECIMAL(10,2), NOT NULL)
  - created_at (TIMESTAMP)
- **Relationships:**
  - Many-to-Many: bookings ↔ parts

### 12. **service_parts** (Junction Table)
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - service_id → services(id) ON DELETE CASCADE
  - part_id → parts(id) ON DELETE RESTRICT
- **Attributes:**
  - quantity (INT, NOT NULL)
  - price (DECIMAL(10,2), NOT NULL)
- **Relationships:**
  - Many-to-Many: services ↔ parts

### 13. **invoices**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - service_id → services(id) ON DELETE RESTRICT (UNIQUE)
  - customer_id → customers(id) ON DELETE RESTRICT
  - created_by → users(id)
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - invoice_number (VARCHAR(20), UNIQUE)
  - invoice_date (DATE, NOT NULL)
  - due_date (DATE, NOT NULL)
  - status (VARCHAR(20), CHECK: 'pending', 'partial', 'paid', 'overdue', 'cancelled')
  - subtotal (DECIMAL(10,2), NOT NULL)
  - tax_rate (DECIMAL(5,2))
  - tax_amount (DECIMAL(10,2))
  - discount_amount (DECIMAL(10,2))
  - total_amount (DECIMAL(10,2), NOT NULL)
  - notes (TEXT)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)
- **Relationships:**
  - ONE service → ONE invoice
  - ONE customer → MANY invoices

### 14. **invoice_payments**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - invoice_id → invoices(id) ON DELETE CASCADE
  - created_by → users(id)
- **Attributes:**
  - payment_date (DATE, NOT NULL)
  - payment_amount (DECIMAL(10,2), NOT NULL)
  - payment_method (VARCHAR(50), NOT NULL)
  - reference_number (VARCHAR(50))
  - notes (TEXT)
  - created_at (TIMESTAMP)
- **Relationships:**
  - ONE invoice → MANY payments

### 15. **billing**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - customer_id → customers(id) ON DELETE RESTRICT
  - service_id → service_bookings(id) ON DELETE RESTRICT
- **Attributes:**
  - hex_id (VARCHAR(20), UNIQUE)
  - amount (DECIMAL(10,2), NOT NULL)
  - payment_status (VARCHAR(20), CHECK: 'Paid', 'Unpaid', 'Partial', 'Cancelled')
  - payment_method (VARCHAR(50), CHECK: 'Cash', 'Bank Transfer', 'Online Payment')
  - reference_number (VARCHAR(100))
  - bill_date (DATE, NOT NULL)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

### 16. **email_sent_history**
- **PK:** id (INT, AUTO_INCREMENT)
- **FK:** billing_id → billing(id) ON DELETE CASCADE
- **Attributes:**
  - recipient_email (VARCHAR(100), NOT NULL)
  - email_type (VARCHAR(50), NOT NULL)
  - subject (VARCHAR(255), NOT NULL)
  - sent_date (TIMESTAMP)
  - status (VARCHAR(20), CHECK: 'sent', 'failed', 'pending')
  - notes (TEXT)
  - created_at (TIMESTAMP)
- **Relationships:**
  - ONE billing → MANY email_sent_history

### 17. **tasks**
- **PK:** id (INT, AUTO_INCREMENT)
- **FKs:**
  - created_by → users(id)
  - assigned_to → mechanics(id)
- **Attributes:**
  - title (VARCHAR(255), NOT NULL)
  - description (TEXT)
  - status (VARCHAR(50), NOT NULL)
  - priority (VARCHAR(20), DEFAULT 'MEDIUM')
  - due_date (DATE)
  - created_at (TIMESTAMP)

---

## Key Relationships Summary

```
roles (1) ──────→ (M) users
users (1) ──────→ (1) mechanics
users (1) ──────→ (M) tasks (created_by)
users (1) ──────→ (M) invoices (created_by)
users (1) ──────→ (M) invoice_payments (created_by)

customers (1) ──→ (M) vehicles
customers (1) ──→ (M) service_bookings
customers (1) ──→ (M) invoices
customers (1) ──→ (M) billing

vehicles (1) ───→ (M) service_bookings
vehicles (1) ───→ (M) services

mechanics (1) ──→ (M) service_bookings
mechanics (1) ──→ (M) services (as technician)
mechanics (1) ──→ (M) tasks (assigned_to)

service_bookings (1) ─→ (M) booking_services
service_bookings (1) ─→ (M) booking_parts
service_bookings (1) ─→ (M) services
service_bookings (1) ─→ (M) billing

services (1) ────→ (M) service_items
services (1) ────→ (M) service_parts
services (1) ────→ (1) invoices

parts (M) ←──────→ (M) service_bookings [via booking_parts]
parts (M) ←──────→ (M) services [via service_parts]

invoices (1) ────→ (M) invoice_payments

billing (1) ─────→ (M) email_sent_history
```

---

## Views

### service_booking_details
Aggregates booking information with customer, vehicle, mechanic, and service details.

### service_details
Comprehensive service information including labor costs, parts costs, and total cost calculations.

### user_management_view
User information with role names for admin panel.

---

## Indexes

**users:** username, email  
**service_bookings:** customer_id, vehicle_id, mechanic_id, booking_date, status, booking_time  
**services:** vehicle_id, technician_id, booking_id  
**service_items:** service_id  
**service_parts:** service_id, part_id  
**invoices:** service_id, customer_id  
**invoice_payments:** invoice_id  
**billing:** customer_id, service_id, payment_status  
**email_sent_history:** billing_id, sent_date, recipient_email

---

## Business Logic Notes

1. **Parts Reservation:** Parts have `reserved_quantity` that tracks parts allocated to bookings
2. **Booking Status Flow:** scheduled → in_progress → completed (or delayed/cancelled)
3. **Payment Methods:** Cash, Bank Transfer, Online Payment
4. **Mechanic Availability:** Automatically calculated based on current job count
5. **Role-Based Access:** ADMIN, CASHIER, MECHANIC with different permissions
6. **Hex IDs:** User-friendly identifiers for customers, vehicles, mechanics, bookings, parts, and bills
