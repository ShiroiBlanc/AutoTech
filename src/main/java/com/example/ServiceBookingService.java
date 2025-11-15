package com.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ServiceBookingService {
    private ServiceBookingViewModel createViewModel(ResultSet rs) throws SQLException {
        ServiceBookingViewModel booking = new ServiceBookingViewModel();
        booking.setId(rs.getInt("id"));
        booking.setHexId(rs.getString("hex_id"));
        
        Customer customer = new Customer();
        customer.setId(rs.getInt("customer_id"));
        customer.setName(rs.getString("customer_name"));
        booking.setCustomer(customer);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setId(rs.getInt("vehicle_id"));
        vehicle.setBrand(rs.getString("vehicle_info")); // Using this as display text since it contains full vehicle info
        vehicle.setModel(rs.getString("vehicle_info")); // Using this for consistency
        booking.setVehicle(vehicle);
        
        // Load mechanic for this booking
        Mechanic mechanic = new Mechanic(
            rs.getInt("mechanic_id"),
            0, // user_id not needed here
            rs.getString("mechanic_name"),
            "" // specialty not needed in listing
        );
        booking.setMechanic(mechanic);
        
        Date bookingDate = rs.getDate("booking_date");
        Time bookingTime = rs.getTime("booking_time");
        
        booking.setDate(bookingDate.toLocalDate());
        // Format time to 24-hour format (HH:mm)
        String time24 = String.format("%02d:%02d", 
            bookingTime.toLocalTime().getHour(),
            bookingTime.toLocalTime().getMinute());
        booking.setTime(time24);
        booking.setServiceType(rs.getString("service_type"));
        booking.setServiceDescription(rs.getString("service_description"));
        booking.setStatus(rs.getString("status"));
        
        return booking;
    }
    public boolean saveBooking(ServiceBookingViewModel booking) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql;
            if (booking.getId() > 0) {
                sql = "UPDATE service_bookings SET customer_id = ?, vehicle_id = ?, mechanic_id = ?, " +
                      "booking_date = ?, booking_time = ?, " +
                      "service_type = ?, service_description = ?, status = ? " +
                      "WHERE id = ?";
            } else {
                sql = "INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, " +
                      "booking_date, booking_time, service_type, service_description, status, original_status, hex_id) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int param = 1;
                stmt.setInt(param++, booking.getCustomer().getId());
                stmt.setInt(param++, booking.getVehicle().getId());
                stmt.setInt(param++, booking.getMechanic().getId());
                stmt.setDate(param++, Date.valueOf(booking.getDate()));
                stmt.setTime(param++, Time.valueOf(booking.getTime() + ":00"));
                stmt.setString(param++, booking.getServiceType());
                stmt.setString(param++, booking.getServiceDescription());
                stmt.setString(param++, booking.getStatus());
                
                if (booking.getId() > 0) {
                    stmt.setInt(param, booking.getId());
                } else {
                    stmt.setString(param++, booking.getStatus());  // original_status = status on creation
                    stmt.setString(param, HexIdGenerator.generateBookingId());
                }
                
                int rowsAffected = stmt.executeUpdate();
                
                // If it's a new booking, get the generated ID
                if (booking.getId() == 0 && rowsAffected > 0) {
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        booking.setId(generatedKeys.getInt(1));
                    }
                }
                
                return rowsAffected > 0;
            }
        }
    }
    
    public ServiceBookingViewModel getBookingById(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT sb.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                "u.username as mechanic_name " +
                "FROM service_bookings sb " +
                "JOIN customers c ON sb.customer_id = c.id " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                "LEFT JOIN users u ON m.user_id = u.id " +
                "WHERE sb.id = ?")) {
            
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return createViewModel(rs);
            }
        }
        
        return null;
    }
    
    public List<ServiceBookingViewModel> getAllBookings() throws SQLException {
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT sb.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                "u.username as mechanic_name " +
                "FROM service_bookings sb " +
                "JOIN customers c ON sb.customer_id = c.id " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                "LEFT JOIN users u ON m.user_id = u.id " +
                "ORDER BY sb.id DESC")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    public List<ServiceBookingViewModel> getBookingsForMechanic(int mechanicId) throws SQLException {
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT sb.*, c.name as customer_name, " +
                "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                "u.username as mechanic_name " +
                "FROM service_bookings sb " +
                "JOIN customers c ON sb.customer_id = c.id " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                "LEFT JOIN users u ON m.user_id = u.id " +
                "WHERE sb.mechanic_id = ? " +
                "ORDER BY sb.id DESC")) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    public List<ServiceBookingViewModel> searchBookings(
            String searchTerm, String statusFilter, LocalDate dateFilter) throws SQLException {
        
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT sb.*, c.name as customer_name, " +
            "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
            "u.username as mechanic_name " +
            "FROM service_bookings sb " +
            "JOIN customers c ON sb.customer_id = c.id " +
            "JOIN vehicles v ON sb.vehicle_id = v.id " +
            "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
            "LEFT JOIN users u ON m.user_id = u.id " +
            "WHERE 1=1 "
        );
        
        List<Object> params = new ArrayList<>();
        
        // Add search condition
        if (searchTerm != null && !searchTerm.isEmpty()) {
            query.append("AND (c.name LIKE ? OR v.plate_number LIKE ? OR u.username LIKE ? OR sb.service_type LIKE ?) ");
            String searchPattern = "%" + searchTerm + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Add status filter
        if (statusFilter != null) {
            if (statusFilter.equals("Active")) {
                // "Active" means all statuses except cancelled
                query.append("AND sb.status != 'cancelled' ");
            } else if (!statusFilter.equals("All")) {
                query.append("AND sb.status = ? ");
                params.add(statusFilter.toLowerCase());
            }
        }
        
        // Add date filter
        if (dateFilter != null) {
            query.append("AND sb.booking_date = ? ");
            params.add(Date.valueOf(dateFilter));
        }
        
        query.append("ORDER BY sb.id DESC");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    public List<ServiceBookingViewModel> searchBookingsForMechanic(
            int mechanicId, String searchTerm, String statusFilter, LocalDate dateFilter) throws SQLException {
        
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT sb.*, c.name as customer_name, " +
            "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
            "u.username as mechanic_name " +
            "FROM service_bookings sb " +
            "JOIN customers c ON sb.customer_id = c.id " +
            "JOIN vehicles v ON sb.vehicle_id = v.id " +
            "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
            "LEFT JOIN users u ON m.user_id = u.id " +
            "WHERE sb.mechanic_id = ? "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(mechanicId);
        
        // Add search condition
        if (searchTerm != null && !searchTerm.isEmpty()) {
            query.append("AND (c.name LIKE ? OR v.plate_number LIKE ? OR u.username LIKE ? OR sb.service_type LIKE ?) ");
            String searchPattern = "%" + searchTerm + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Add status filter
        if (statusFilter != null) {
            if (statusFilter.equals("Active")) {
                // "Active" means all statuses except cancelled
                query.append("AND sb.status != 'cancelled' ");
            } else if (!statusFilter.equals("All")) {
                query.append("AND sb.status = ? ");
                params.add(statusFilter.toLowerCase());
            }
        }
        
        // Add date filter
        if (dateFilter != null) {
            query.append("AND sb.booking_date = ? ");
            params.add(Date.valueOf(dateFilter));
        }
        
        query.append("ORDER BY sb.id DESC");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    /**
     * Get all cancelled bookings
     */
    public List<ServiceBookingViewModel> getCancelledBookings() throws SQLException {
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        String query = "SELECT sb.*, c.name as customer_name, " +
                      "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                      "u.username as mechanic_name " +
                      "FROM service_bookings sb " +
                      "JOIN customers c ON sb.customer_id = c.id " +
                      "JOIN vehicles v ON sb.vehicle_id = v.id " +
                      "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                      "LEFT JOIN users u ON m.user_id = u.id " +
                      "WHERE sb.status = 'cancelled' " +
                      "ORDER BY sb.id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    /**
     * Get cancelled bookings for a specific mechanic
     */
    public List<ServiceBookingViewModel> getCancelledBookingsForMechanic(int mechanicId) throws SQLException {
        List<ServiceBookingViewModel> bookings = new ArrayList<>();
        
        String query = "SELECT sb.*, c.name as customer_name, " +
                      "CONCAT(v.brand, ' ', v.model, ' (', v.plate_number, ')') as vehicle_info, " +
                      "u.username as mechanic_name " +
                      "FROM service_bookings sb " +
                      "JOIN customers c ON sb.customer_id = c.id " +
                      "JOIN vehicles v ON sb.vehicle_id = v.id " +
                      "LEFT JOIN mechanics m ON sb.mechanic_id = m.id " +
                      "LEFT JOIN users u ON m.user_id = u.id " +
                      "WHERE sb.mechanic_id = ? AND sb.status = 'cancelled' " +
                      "ORDER BY sb.id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, mechanicId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(createViewModel(rs));
            }
        }
        
        return bookings;
    }
    
    public boolean createBooking(int customerId, int vehicleId, int mechanicId, 
                               LocalDate bookingDate, String bookingTime, 
                               String serviceType, String serviceDescription, String status) throws SQLException {
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, booking_date, booking_time, " +
                "service_type, service_description, status, hex_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setInt(1, customerId);
            stmt.setInt(2, vehicleId);
            stmt.setInt(3, mechanicId);
            stmt.setDate(4, Date.valueOf(bookingDate));
            stmt.setTime(5, Time.valueOf(bookingTime + ":00")); // Add seconds
            stmt.setString(6, serviceType);
            stmt.setString(7, serviceDescription);
            stmt.setString(8, status);
            stmt.setString(9, HexIdGenerator.generateBookingId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    // Create booking and return the generated ID
    // Also checks stock availability and reserves parts immediately
    public int createBookingAndReturnId(int customerId, int vehicleId, int mechanicId, 
                               LocalDate bookingDate, String bookingTime, 
                               String serviceType, String serviceDescription, String status,
                               List<BookingPart> partsToUse) throws SQLException {
        
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement stmt = null;
        PreparedStatement reserveStmt = null;
        ResultSet rs = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Check if all parts have sufficient available quantity
            boolean hasInsufficientParts = false;
            
            for (BookingPart part : partsToUse) {
                checkStmt = conn.prepareStatement(
                    "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?");
                checkStmt.setInt(1, part.getPartId());
                rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    int availableQty = rs.getInt("quantity_in_stock") - rs.getInt("reserved_quantity");
                    if (availableQty < part.getQuantity()) {
                        hasInsufficientParts = true;
                    }
                }
                rs.close();
                checkStmt.close();
            }
            
            // Check mechanic availability and workload
            boolean mechanicOffDuty = false;
            boolean mechanicOverloaded = false;
            
            checkStmt = conn.prepareStatement(
                "SELECT availability FROM mechanics WHERE id = ?");
            checkStmt.setInt(1, mechanicId);
            rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String availability = rs.getString("availability");
                if ("Off Duty".equalsIgnoreCase(availability)) {
                    mechanicOffDuty = true;
                }
            }
            rs.close();
            checkStmt.close();
            
            // Check actual job count to determine if overloaded (5+ jobs)
            checkStmt = conn.prepareStatement(
                "SELECT COUNT(*) as job_count FROM service_bookings " +
                "WHERE mechanic_id = ? AND status IN ('scheduled', 'in_progress')");
            checkStmt.setInt(1, mechanicId);
            rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                int jobCount = rs.getInt("job_count");
                if (jobCount >= 5) {
                    mechanicOverloaded = true;
                    System.out.println("⚠ Mechanic #" + mechanicId + " has " + jobCount + " active jobs (overloaded). Booking will be delayed.");
                }
            }
            rs.close();
            checkStmt.close();
            
            // Set status to "delayed" if insufficient parts OR mechanic is off duty OR mechanic is overloaded
            String finalStatus = (hasInsufficientParts || mechanicOffDuty || mechanicOverloaded) ? "delayed" : status;
            
            // Create the booking
            stmt = conn.prepareStatement(
                "INSERT INTO service_bookings (customer_id, vehicle_id, mechanic_id, booking_date, booking_time, " +
                "service_type, service_description, status, hex_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, customerId);
            stmt.setInt(2, vehicleId);
            stmt.setInt(3, mechanicId);
            stmt.setDate(4, Date.valueOf(bookingDate));
            stmt.setTime(5, Time.valueOf(bookingTime + ":00"));
            stmt.setString(6, serviceType);
            stmt.setString(7, serviceDescription);
            stmt.setString(8, finalStatus);
            stmt.setString(9, HexIdGenerator.generateBookingId());
            
            int affectedRows = stmt.executeUpdate();
            int bookingId = -1;
            
            if (affectedRows > 0) {
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    bookingId = generatedKeys.getInt(1);
                }
            }
            
            // Reserve parts immediately using the SAME connection
            if (bookingId > 0 && !partsToUse.isEmpty()) {
                reserveStmt = conn.prepareStatement(
                    "UPDATE parts SET reserved_quantity = reserved_quantity + ? WHERE id = ?");
                
                for (BookingPart part : partsToUse) {
                    reserveStmt.setInt(1, part.getQuantity());
                    reserveStmt.setInt(2, part.getPartId());
                    reserveStmt.executeUpdate();
                }
            }
            
            conn.commit();
            
            // Trigger stock check after successfully reserving parts
            if (bookingId > 0 && !partsToUse.isEmpty()) {
                new Thread(() -> {
                    try {
                        InventoryService.getInstance().checkStockAfterReservation();
                    } catch (Exception e) {
                        System.err.println("Error checking stock after reservation: " + e.getMessage());
                    }
                }).start();
            }
            
            // Update mechanic availability after creating booking (if not delayed)
            if (bookingId > 0 && "scheduled".equalsIgnoreCase(finalStatus)) {
                try {
                    MechanicService mechanicService = new MechanicService();
                    mechanicService.updateMechanicAvailability(mechanicId);
                } catch (Exception e) {
                    System.err.println("Error updating mechanic availability: " + e.getMessage());
                }
            }
            
            return bookingId;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            // Close resources in reverse order
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException e) { }
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (reserveStmt != null) try { reserveStmt.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            if (checkStmt != null) try { checkStmt.close(); } catch (SQLException e) { }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public boolean updateBooking(int id, int customerId, int vehicleId, int mechanicId, 
                               LocalDate bookingDate, String bookingTime, 
                               String serviceType, String serviceDescription, String status) throws SQLException {
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE service_bookings SET customer_id = ?, vehicle_id = ?, mechanic_id = ?, " +
                "booking_date = ?, booking_time = ?, service_type = ?, service_description = ?, " +
                "status = ? WHERE id = ?")) {
            
            stmt.setInt(1, customerId);
            stmt.setInt(2, vehicleId);
            stmt.setInt(3, mechanicId);
            stmt.setDate(4, Date.valueOf(bookingDate));
            stmt.setTime(5, Time.valueOf(bookingTime + ":00")); // Add seconds
            stmt.setString(6, serviceType);
            stmt.setString(7, serviceDescription);
            stmt.setString(8, status);
            stmt.setInt(9, id);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateBookingStatus(int bookingId, String newStatus) throws SQLException {
        // Get current status and mechanic ID before updating
        String currentStatus = "";
        String originalStatus = "";
        int mechanicId = -1;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT mechanic_id, status, original_status FROM service_bookings WHERE id = ?")) {
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                mechanicId = rs.getInt("mechanic_id");
                currentStatus = rs.getString("status");
                originalStatus = rs.getString("original_status");
            }
        }
        
        // When changing from delayed to scheduled/completed/cancelled, preserve the original_status if it was delayed
        String statusToSave = originalStatus;
        if ("delayed".equalsIgnoreCase(currentStatus) && !originalStatus.equalsIgnoreCase("delayed")) {
            statusToSave = currentStatus; // Save the delayed status as the original
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE service_bookings SET status = ?, original_status = ? WHERE id = ?")) {
            
            stmt.setString(1, newStatus);
            stmt.setString(2, statusToSave);
            stmt.setInt(3, bookingId);
            
            int rowsAffected = stmt.executeUpdate();
            
            // If status changed to completed, deduct parts from inventory
            if (rowsAffected > 0 && "completed".equalsIgnoreCase(newStatus)) {
                try {
                    deductPartsFromInventory(bookingId);
                    
                    // Trigger inventory check for low stock and expiration alerts
                    System.out.println("Booking completed - triggering inventory check...");
                    InventoryService.getInstance().checkAndSendLowStockAlert();
                } catch (SQLException e) {
                    // Log the error but don't fail the status update
                    System.err.println("Warning: Could not deduct parts from inventory: " + e.getMessage());
                }
            }
            
            // Update mechanic availability when booking status changes
            if (rowsAffected > 0 && mechanicId > 0) {
                try {
                    MechanicService mechanicService = new MechanicService();
                    mechanicService.updateMechanicAvailability(mechanicId);
                    
                    // If a job completed or cancelled, check if delayed bookings can proceed
                    if ("completed".equalsIgnoreCase(newStatus) || "cancelled".equalsIgnoreCase(newStatus)) {
                        checkAndUpdateDelayedBookingsForMechanic(mechanicId, bookingId);  // Pass the booking ID that triggered this
                    }
                } catch (SQLException e) {
                    System.err.println("Warning: Could not update mechanic availability: " + e.getMessage());
                }
            }
            
            return rowsAffected > 0;
        }
    }
    
    public boolean deleteBooking(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM service_bookings WHERE id = ?")) {
            
            stmt.setInt(1, bookingId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // Methods for booking parts
    public boolean addBookingPart(int bookingId, int partId, int quantity, double priceAtTime) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO booking_parts (booking_id, part_id, quantity, price_at_time) VALUES (?, ?, ?, ?)")) {
            
            stmt.setInt(1, bookingId);
            stmt.setInt(2, partId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, priceAtTime);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public List<BookingPart> getBookingParts(int bookingId) throws SQLException {
        List<BookingPart> parts = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT bp.id, bp.booking_id, bp.part_id, p.name as part_name, bp.quantity, bp.price_at_time " +
                "FROM booking_parts bp " +
                "JOIN parts p ON bp.part_id = p.id " +
                "WHERE bp.booking_id = ?")) {
            
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                BookingPart part = new BookingPart(
                    rs.getInt("booking_id"),
                    rs.getInt("part_id"),
                    rs.getString("part_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price_at_time")
                );
                part.setId(rs.getInt("id"));
                parts.add(part);
            }
        }
        
        return parts;
    }
    
    public boolean deleteBookingParts(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM booking_parts WHERE booking_id = ?")) {
            
            stmt.setInt(1, bookingId);
            stmt.executeUpdate();
            return true;
        }
    }
    
    // Deduct parts from inventory when booking is completed
    // Also releases reserved parts
    public boolean deductPartsFromInventory(int bookingId) throws SQLException {
        List<BookingPart> parts = getBookingParts(bookingId);
        
        if (parts.isEmpty()) {
            return true; // No parts to deduct
        }
        
        Connection conn = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            for (BookingPart part : parts) {
                // Check if enough stock available
                PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT quantity_in_stock FROM parts WHERE id = ?");
                checkStmt.setInt(1, part.getPartId());
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    int currentStock = rs.getInt("quantity_in_stock");
                    if (currentStock < part.getQuantity()) {
                        conn.rollback();
                        throw new SQLException("Insufficient stock for part: " + part.getPartName() + 
                                             ". Available: " + currentStock + ", Required: " + part.getQuantity());
                    }
                }
                rs.close();
                checkStmt.close();
                
                // Deduct from inventory and release reserved quantity
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE parts SET quantity_in_stock = quantity_in_stock - ?, " +
                    "reserved_quantity = GREATEST(0, reserved_quantity - ?) WHERE id = ?");
                updateStmt.setInt(1, part.getQuantity());
                updateStmt.setInt(2, part.getQuantity());
                updateStmt.setInt(3, part.getPartId());
                updateStmt.executeUpdate();
                updateStmt.close();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
    
    // Release reserved parts when booking is cancelled
    public void releaseBookingParts(int bookingId) throws SQLException {
        try {
            List<BookingPart> parts = getBookingParts(bookingId);
            InventoryService inventoryService = InventoryService.getInstance();
            
            System.out.println("Releasing " + parts.size() + " parts for booking #" + bookingId);
            
            for (BookingPart part : parts) {
                inventoryService.releaseReservedParts(part.getPartId(), part.getQuantity());
                System.out.println("  Released " + part.getQuantity() + " of " + part.getPartName());
            }
        } catch (SQLException e) {
            System.err.println("Error releasing parts for booking #" + bookingId + ": " + e.getMessage());
            throw e;
        }
    }
    
    // Check and update delayed bookings that can now proceed
    public int checkAndUpdateDelayedBookings() throws SQLException {
        int updatedCount = 0;
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Get all delayed bookings - simplified query without unnecessary joins
            String query = "SELECT id, mechanic_id FROM service_bookings WHERE status = 'delayed'";
            
            System.out.println("=== Checking for delayed bookings ===");
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    int bookingId = rs.getInt("id");
                    int mechanicId = rs.getInt("mechanic_id");
                    
                    System.out.println("\n--- Found delayed booking #" + bookingId + " (mechanic: " + mechanicId + ") ---");
                    
                    // Check if mechanic is now available
                    boolean mechanicAvailable = true;
                    String mechanicQuery = "SELECT availability FROM mechanics WHERE id = ?";
                    try (PreparedStatement mechanicStmt = conn.prepareStatement(mechanicQuery)) {
                        mechanicStmt.setInt(1, mechanicId);
                        ResultSet mechanicRs = mechanicStmt.executeQuery();
                        if (mechanicRs.next()) {
                            String availability = mechanicRs.getString("availability");
                            mechanicAvailable = "Available".equalsIgnoreCase(availability);
                            System.out.println("  Mechanic status: " + availability + " -> " + (mechanicAvailable ? "OK" : "NOT AVAILABLE"));
                        } else {
                            System.out.println("  WARNING: Mechanic #" + mechanicId + " not found!");
                            mechanicAvailable = false;
                        }
                    }
                    
                    // Check if all parts are now available
                    boolean partsAvailable = true;
                    
                    // Get booking parts using the same connection
                    List<BookingPart> parts = new ArrayList<>();
                    String partsQuery = "SELECT bp.id, bp.booking_id, bp.part_id, p.name as part_name, bp.quantity, bp.price_at_time " +
                                       "FROM booking_parts bp " +
                                       "JOIN parts p ON bp.part_id = p.id " +
                                       "WHERE bp.booking_id = ?";
                    try (PreparedStatement partsStmt = conn.prepareStatement(partsQuery)) {
                        partsStmt.setInt(1, bookingId);
                        ResultSet partsRs = partsStmt.executeQuery();
                        while (partsRs.next()) {
                            BookingPart part = new BookingPart(
                                partsRs.getInt("booking_id"),
                                partsRs.getInt("part_id"),
                                partsRs.getString("part_name"),
                                partsRs.getInt("quantity"),
                                partsRs.getDouble("price_at_time")
                            );
                            part.setId(partsRs.getInt("id"));
                            parts.add(part);
                        }
                    }
                    
                    System.out.println("Checking delayed booking #" + bookingId + " with " + parts.size() + " parts");
                    
                    for (BookingPart part : parts) {
                        String partQuery = "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?";
                        try (PreparedStatement partStmt = conn.prepareStatement(partQuery)) {
                            partStmt.setInt(1, part.getPartId());
                            ResultSet partRs = partStmt.executeQuery();
                            if (partRs.next()) {
                                int totalStock = partRs.getInt("quantity_in_stock");
                                int reserved = partRs.getInt("reserved_quantity");
                                
                                // For delayed bookings, the parts ARE already reserved
                                // We need to check if total stock is now enough
                                // Available = total - (reserved - this_booking_reserved)
                                // Which simplifies to: total - reserved + part.quantity
                                int otherReserved = reserved - part.getQuantity();
                                int availableForThisBooking = totalStock - otherReserved;
                                
                                System.out.println("  Part: " + part.getPartName() + 
                                                 ", Need: " + part.getQuantity() + 
                                                 ", Total Stock: " + totalStock + 
                                                 ", Total Reserved: " + reserved + 
                                                 ", Other Reserved: " + otherReserved + 
                                                 ", Available: " + availableForThisBooking);
                                
                                if (part.getQuantity() > availableForThisBooking) {
                                    partsAvailable = false;
                                    System.out.println("  -> INSUFFICIENT");
                                    break;
                                } else {
                                    System.out.println("  -> OK");
                                }
                            }
                        }
                    }
                    
                    // If both mechanic and parts are available, update status to scheduled
                    if (mechanicAvailable && partsAvailable) {
                        System.out.println("  ✓ BOTH CONDITIONS MET - Updating to scheduled");
                        String updateQuery = "UPDATE service_bookings SET status = 'scheduled' WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, bookingId);
                            int updated = updateStmt.executeUpdate();
                            if (updated > 0) {
                                updatedCount++;
                                System.out.println("  ✓ SUCCESS: Auto-updated booking #" + bookingId + " from delayed to scheduled");
                            } else {
                                System.out.println("  ✗ FAILED: Could not update booking #" + bookingId);
                            }
                        }
                    } else {
                        System.out.println("  ✗ NOT READY: mechanicAvailable=" + mechanicAvailable + ", partsAvailable=" + partsAvailable);
                    }
                }
                
                System.out.println("\n=== SUMMARY: Found and checked delayed bookings, updated " + updatedCount + " ===\n");
            }
        }
        
        return updatedCount;
    }
    
    // Set bookings to delayed when mechanic becomes unavailable
    public int setBookingsToDelayedForMechanic(int mechanicId) throws SQLException {
        int delayedCount = 0;
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Get all scheduled bookings for this mechanic
            String query = "SELECT id FROM service_bookings WHERE mechanic_id = ? AND status = 'scheduled'";
            
            System.out.println("=== Checking scheduled bookings for mechanic #" + mechanicId + " ===");
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, mechanicId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    int bookingId = rs.getInt("id");
                    
                    // Update to delayed
                    String updateQuery = "UPDATE service_bookings SET status = 'delayed' WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, bookingId);
                        int updated = updateStmt.executeUpdate();
                        if (updated > 0) {
                            delayedCount++;
                            System.out.println("Set booking #" + bookingId + " to delayed (mechanic unavailable)");
                        }
                    }
                }
                
                System.out.println("=== Total bookings delayed: " + delayedCount + " ===");
            }
        }
        
        return delayedCount;
    }
    
    /**
     * Check and update delayed bookings for a specific mechanic
     * Called when mechanic finishes/cancels a job and may now have capacity
     */
    public int checkAndUpdateDelayedBookingsForMechanic(int mechanicId) throws SQLException {
        return checkAndUpdateDelayedBookingsForMechanic(mechanicId, -1);  // -1 means no specific triggering booking
    }
    
    public int checkAndUpdateDelayedBookingsForMechanic(int mechanicId, int triggeringBookingId) throws SQLException {
        int updatedCount = 0;
        MechanicService mechanicService = new MechanicService();
        
        // Check if mechanic can now accept bookings
        if (!mechanicService.canAcceptNewBooking(mechanicId)) {
            System.out.println("Mechanic #" + mechanicId + " still overloaded, cannot update delayed bookings");
            return 0;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Get delayed bookings for this mechanic only, ordered by booking date/time (earliest first)
            String query = "SELECT id FROM service_bookings WHERE mechanic_id = ? AND status = 'delayed' " +
                          "ORDER BY booking_date ASC, booking_time ASC";
            
            System.out.println("=== Checking delayed bookings for mechanic #" + mechanicId + " (triggered by booking #" + triggeringBookingId + ") ===");
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, mechanicId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    // Re-check capacity before each booking (in case we've scheduled some already)
                    if (!mechanicService.canAcceptNewBooking(mechanicId)) {
                        System.out.println("Mechanic #" + mechanicId + " reached capacity (5+ jobs), stopping delayed booking updates");
                        break;
                    }
                    
                    int bookingId = rs.getInt("id");
                    
                    System.out.println("\n--- Checking delayed booking #" + bookingId + " ---");
                    
                    // Check if all parts are now available
                    boolean partsAvailable = true;
                    
                    // Get booking parts
                    List<BookingPart> parts = new ArrayList<>();
                    String partsQuery = "SELECT bp.id, bp.booking_id, bp.part_id, p.name as part_name, bp.quantity, bp.price_at_time " +
                                       "FROM booking_parts bp " +
                                       "JOIN parts p ON bp.part_id = p.id " +
                                       "WHERE bp.booking_id = ?";
                    try (PreparedStatement partsStmt = conn.prepareStatement(partsQuery)) {
                        partsStmt.setInt(1, bookingId);
                        ResultSet partsRs = partsStmt.executeQuery();
                        while (partsRs.next()) {
                            BookingPart part = new BookingPart(
                                partsRs.getInt("booking_id"),
                                partsRs.getInt("part_id"),
                                partsRs.getString("part_name"),
                                partsRs.getInt("quantity"),
                                partsRs.getDouble("price_at_time")
                            );
                            part.setId(partsRs.getInt("id"));
                            parts.add(part);
                        }
                    }
                    
                    System.out.println("Checking delayed booking #" + bookingId + " with " + parts.size() + " parts");
                    
                    for (BookingPart part : parts) {
                        String partQuery = "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?";
                        try (PreparedStatement partStmt = conn.prepareStatement(partQuery)) {
                            partStmt.setInt(1, part.getPartId());
                            ResultSet partRs = partStmt.executeQuery();
                            if (partRs.next()) {
                                int totalStock = partRs.getInt("quantity_in_stock");
                                int reserved = partRs.getInt("reserved_quantity");
                                int otherReserved = reserved - part.getQuantity();
                                int availableForThisBooking = totalStock - otherReserved;
                                
                                System.out.println("  Part: " + part.getPartName() + 
                                                 ", Need: " + part.getQuantity() + 
                                                 ", Available: " + availableForThisBooking);
                                
                                if (part.getQuantity() > availableForThisBooking) {
                                    partsAvailable = false;
                                    System.out.println("  -> INSUFFICIENT PARTS");
                                    break;
                                } else {
                                    System.out.println("  -> OK");
                                }
                            }
                        }
                    }
                    
                    // If parts are available AND mechanic can accept, update to scheduled
                    if (partsAvailable) {
                        System.out.println("  ✓ Parts available and mechanic has capacity - Updating to scheduled");
                        // Mark this booking as promoted by the triggering booking (so we can revert it if the triggering booking is undone)
                        // If triggeringBookingId is -1, don't track it (manual promotion from controller)
                        // If column doesn't exist, just update status without the tracking
                        try {
                            String updateQuery = "UPDATE service_bookings SET status = 'scheduled'" + 
                                               (triggeringBookingId > 0 ? ", promoted_by_booking_id = ?" : "") + 
                                               " WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                                if (triggeringBookingId > 0) {
                                    updateStmt.setInt(1, triggeringBookingId);  // Store which booking caused this promotion
                                    updateStmt.setInt(2, bookingId);
                                } else {
                                    updateStmt.setInt(1, bookingId);
                                }
                                int updated = updateStmt.executeUpdate();
                                if (updated > 0) {
                                    updatedCount++;
                                    if (triggeringBookingId > 0) {
                                        System.out.println("  ✓ SUCCESS: Auto-updated booking #" + bookingId + " from delayed to scheduled (promoted by booking #" + triggeringBookingId + ")");
                                    } else {
                                        System.out.println("  ✓ SUCCESS: Auto-updated booking #" + bookingId + " from delayed to scheduled");
                                    }
                                    
                                    // Update mechanic availability after each scheduled booking
                                    mechanicService.updateMechanicAvailability(mechanicId);
                                } else {
                                    System.out.println("  ✗ FAILED: Could not update booking #" + bookingId);
                                }
                            }
                        } catch (SQLException e) {
                            // Column might not exist or other error - try simpler update
                            try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE service_bookings SET status = 'scheduled' WHERE id = ?")) {
                                updateStmt.setInt(1, bookingId);
                                int updated = updateStmt.executeUpdate();
                                if (updated > 0) {
                                    updatedCount++;
                                    System.out.println("  ✓ SUCCESS: Auto-updated booking #" + bookingId + " from delayed to scheduled");
                                    mechanicService.updateMechanicAvailability(mechanicId);
                                } else {
                                    System.out.println("  ✗ FAILED: Could not update booking #" + bookingId);
                                }
                            }
                        }
                    } else {
                        System.out.println("  ✗ NOT READY: Parts still insufficient");
                    }
                }
                
                System.out.println("\n=== SUMMARY: Updated " + updatedCount + " delayed booking(s) for mechanic #" + mechanicId + " ===\n");
            }
        }
        
        return updatedCount;
    }
    
    /**
     * Undo a booking status change back to its original status.
     * If a booking was originally delayed and was changed to completed/cancelled,
     * this will revert it back to delayed.
     * Also finds any bookings that were promoted FROM delayed TO scheduled because of this booking,
     * and reverts them back to delayed.
     */
    public boolean undoBookingStatusChange(int bookingId) throws SQLException {
        String originalStatus = "";
        int mechanicId = -1;
        List<BookingPart> undoingBookingParts = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT mechanic_id, original_status FROM service_bookings WHERE id = ?")) {
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                mechanicId = rs.getInt("mechanic_id");
                originalStatus = rs.getString("original_status");
            }
        }
        
        if (originalStatus.isEmpty()) {
            return false;
        }
        
        // Get all parts from this booking to find related bookings
        undoingBookingParts = getBookingParts(bookingId);
        
        // Find all scheduled bookings that were promoted by this booking
        // Use TWO approaches:
        // 1. Primary: Look for promoted_by_booking_id (new tracking method)
        // 2. Fallback: Look for bookings with same parts that have original_status='delayed' (for existing data)
        
        List<Integer> affectedBookingIds = new ArrayList<>();
        
        // Approach 1: Direct tracking via promoted_by_booking_id (if column exists)
        try {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM service_bookings WHERE status = 'scheduled' AND promoted_by_booking_id = ?")) {
                stmt.setInt(1, bookingId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    affectedBookingIds.add(rs.getInt("id"));
                }
                System.out.println("Found " + affectedBookingIds.size() + " bookings with direct promoted_by_booking_id tracking");
            }
        } catch (SQLException e) {
            // Column doesn't exist in older databases - use fallback method
            System.out.println("Note: promoted_by_booking_id column not available, using fallback method");
        }
        
        // Approach 2: Fallback for existing bookings without promoted_by_booking_id set
        // This handles bookings that were promoted before this feature was implemented
        if (!undoingBookingParts.isEmpty() && affectedBookingIds.isEmpty()) {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DISTINCT sb.id FROM service_bookings sb " +
                    "INNER JOIN booking_parts bp ON sb.id = bp.booking_id " +
                    "WHERE sb.status = 'scheduled' " +
                    "AND sb.original_status = 'delayed' " +
                    "AND bp.part_id IN (" + String.join(",", 
                        undoingBookingParts.stream().map(p -> "?").collect(java.util.stream.Collectors.toList())) + ") " +
                    "AND sb.id != ? " +
                    "AND sb.mechanic_id = ?")) {  // Same mechanic to avoid false positives
                
                int paramIndex = 1;
                for (BookingPart part : undoingBookingParts) {
                    stmt.setInt(paramIndex++, part.getPartId());
                }
                stmt.setInt(paramIndex++, bookingId);
                stmt.setInt(paramIndex, mechanicId);
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    affectedBookingIds.add(rs.getInt("id"));
                }
                System.out.println("Found " + affectedBookingIds.size() + " legacy bookings (fallback method)");
            }
        }
        
        boolean success = false;
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Revert the main booking back to original status
            // Try to clear promoted_by_booking_id if column exists
            try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE service_bookings SET status = ?, promoted_by_booking_id = NULL WHERE id = ?")) {
                stmt.setString(1, originalStatus);
                stmt.setInt(2, bookingId);
                success = stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                // Column doesn't exist, try without it
                try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE service_bookings SET status = ? WHERE id = ?")) {
                    stmt.setString(1, originalStatus);
                    stmt.setInt(2, bookingId);
                    success = stmt.executeUpdate() > 0;
                }
            }
            System.out.println("Reverted booking #" + bookingId + " back to " + originalStatus);
            
            // Revert affected bookings back to delayed status
            if (!affectedBookingIds.isEmpty()) {
                try (PreparedStatement revertStmt = conn.prepareStatement(
                    "UPDATE service_bookings SET status = 'delayed', promoted_by_booking_id = NULL WHERE id = ?")) {
                    for (Integer affectedId : affectedBookingIds) {
                        revertStmt.setInt(1, affectedId);
                        int reverted = revertStmt.executeUpdate();
                        if (reverted > 0) {
                            System.out.println("✓ Reverted booking #" + affectedId + " from scheduled back to delayed (was promoted by booking #" + bookingId + ")");
                        }
                    }
                } catch (SQLException e) {
                    // Column doesn't exist, try without it
                    try (PreparedStatement revertStmt = conn.prepareStatement(
                        "UPDATE service_bookings SET status = 'delayed' WHERE id = ?")) {
                        for (Integer affectedId : affectedBookingIds) {
                            revertStmt.setInt(1, affectedId);
                            int reverted = revertStmt.executeUpdate();
                            if (reverted > 0) {
                                System.out.println("✓ Reverted booking #" + affectedId + " from scheduled back to delayed (was promoted by booking #" + bookingId + ")");
                            }
                        }
                    }
                }
            }
            
            // Update mechanic availability
            if (success && mechanicId > 0) {
                try {
                    MechanicService mechanicService = new MechanicService();
                    mechanicService.updateMechanicAvailability(mechanicId);
                } catch (SQLException e) {
                    System.err.println("Warning: Could not update mechanic availability: " + e.getMessage());
                }
            }
        }
        
        return success;
    }
    
    /**
     * Add a service to a booking (supports multiple services per booking)
     */
    public boolean addServiceToBooking(int bookingId, String serviceType, String serviceDescription) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO booking_services (booking_id, service_type, service_description) VALUES (?, ?, ?)")) {
            stmt.setInt(1, bookingId);
            stmt.setString(2, serviceType);
            stmt.setString(3, serviceDescription);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Get all services for a booking
     */
    public List<Map<String, String>> getBookingServices(int bookingId) throws SQLException {
        List<Map<String, String>> services = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, service_type, service_description FROM booking_services WHERE booking_id = ? ORDER BY created_at ASC")) {
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> service = new HashMap<>();
                service.put("id", String.valueOf(rs.getInt("id")));
                service.put("type", rs.getString("service_type"));
                service.put("description", rs.getString("service_description"));
                services.add(service);
            }
        }
        return services;
    }
    
    /**
     * Remove a service from a booking
     */
    public boolean removeServiceFromBooking(int serviceId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM booking_services WHERE id = ?")) {
            stmt.setInt(1, serviceId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Check booking parts for expiration and alert if needed
     */
    public List<String> checkBookingPartsExpiration(int bookingId) throws SQLException {
        List<String> expiringItems = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(30);  // Alert 30 days before expiration
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT p.id, p.name, p.expiration_date FROM booking_parts bp " +
                "INNER JOIN parts p ON bp.part_id = p.id " +
                "WHERE bp.booking_id = ? AND p.expiration_date IS NOT NULL")) {
            stmt.setInt(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String partName = rs.getString("name");
                Date expirationDate = rs.getDate("expiration_date");
                
                if (expirationDate != null) {
                    LocalDate expDate = expirationDate.toLocalDate();
                    
                    if (today.isAfter(expDate)) {
                        expiringItems.add("❌ EXPIRED: " + partName + " (expired on " + expDate + ")");
                    } else if (today.isBefore(alertDate) && today.isBefore(expDate) || today.equals(expDate)) {
                        expiringItems.add("⚠️ EXPIRING SOON: " + partName + " (expires on " + expDate + ")");
                    }
                }
            }
        }
        return expiringItems;
    }
    
    /**
     * Delete all services for a booking (used when editing)
     */
    public void deleteBookingServices(int bookingId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM booking_services WHERE booking_id = ?")) {
            stmt.setInt(1, bookingId);
            stmt.executeUpdate();
        }
    }
}
