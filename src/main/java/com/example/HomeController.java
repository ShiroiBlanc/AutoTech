package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.List;

public class HomeController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Label roleLabel;
    @FXML private VBox notificationsContainer;
    @FXML private Label notificationCountLabel;
    
    // Statistics labels
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label lowStockItemsLabel;
    
    private User currentUser;
    private Timeline timeUpdateTimeline;
    
    @FXML
    public void initialize() {
        // Get current logged-in user
        currentUser = UserService.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // Set welcome message
            welcomeLabel.setText("Hello, " + currentUser.getUsername() + "!");
            
            // Set role
            String role = getRoleDisplayName(currentUser.getRole());
            roleLabel.setText("Role: " + role);
            
            // Set current date and time
            updateDateTime();
            
            // Start timeline to update time every second
            startTimeUpdate();
            
            // Load statistics
            loadStatistics();
            
            // Load notifications
            loadNotifications();
        } else {
            welcomeLabel.setText("Hello, Guest!");
            roleLabel.setText("Role: Unknown");
        }
    }
    
    private void startTimeUpdate() {
        timeUpdateTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> updateDateTime())
        );
        timeUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        timeUpdateTimeline.play();
    }
    
    public void cleanup() {
        // Stop the timeline when leaving the page
        if (timeUpdateTimeline != null) {
            timeUpdateTimeline.stop();
        }
    }
    
    private String getRoleDisplayName(User.UserRole role) {
        switch (role) {
            case ADMIN:
                return "Administrator";
            case CASHIER:
                return "Cashier";
            case MECHANIC:
                return "Mechanic";
            default:
                return "User";
        }
    }
    
    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy - hh:mm a");
        dateTimeLabel.setText(now.format(formatter));
    }
    
    private void loadStatistics() {
        try {
            // Load customer count
            CustomerService customerService = CustomerService.getInstance();
            List<Customer> customers = customerService.getAllCustomers();
            totalCustomersLabel.setText(String.valueOf(customers.size()));
            
            // Load booking statistics
            ServiceBookingService bookingService = new ServiceBookingService();
            List<ServiceBookingViewModel> allBookings = bookingService.getAllBookings();
            totalBookingsLabel.setText(String.valueOf(allBookings.size()));
            
            // Count pending bookings
            long pendingCount = allBookings.stream()
                .filter(b -> "scheduled".equalsIgnoreCase(b.getStatus()) || 
                            "in_progress".equalsIgnoreCase(b.getStatus()))
                .count();
            pendingBookingsLabel.setText(String.valueOf(pendingCount));
            
            // Load low stock items
            InventoryService inventoryService = InventoryService.getInstance();
            List<InventoryItem> items = inventoryService.getAllItems();
            long lowStockCount = items.stream()
                .filter(item -> item.getQuantity() <= item.getMinimumStock())
                .count();
            lowStockItemsLabel.setText(String.valueOf(lowStockCount));
            
        } catch (SQLException e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        int notificationCount = 0;
        
        try {
            // Notification 1: Low stock items (actual quantity)
            InventoryService inventoryService = InventoryService.getInstance();
            List<InventoryItem> items = inventoryService.getAllItems();
            List<InventoryItem> lowStockItems = items.stream()
                .filter(item -> item.getQuantity() <= item.getMinimumStock())
                .toList();
            
            if (!lowStockItems.isEmpty()) {
                for (InventoryItem item : lowStockItems) {
                    addNotification(
                        "Low Stock Alert", 
                        "Item '" + item.getName() + "' is running low. Current stock: " + 
                        item.getQuantity() + ", Minimum required: " + item.getMinimumStock(),
                        "warning"
                    );
                    notificationCount++;
                }
            }
            
            // Notification 1b: Low AVAILABLE stock (stock - reserved reaching minimum)
            List<InventoryItem> lowAvailableItems = items.stream()
                .filter(item -> item.getAvailableQuantity() <= item.getMinimumStock() && 
                               item.getReservedQuantity() > 0 &&
                               item.getQuantity() > item.getMinimumStock()) // Stock is OK but available is low
                .toList();
            
            if (!lowAvailableItems.isEmpty()) {
                for (InventoryItem item : lowAvailableItems) {
                    addNotification(
                        "Parts Reserved Alert", 
                        "Item '" + item.getName() + "' has been heavily reserved. " +
                        "Stock: " + item.getQuantity() + ", Reserved: " + item.getReservedQuantity() + 
                        ", Available: " + item.getAvailableQuantity() + ". Consider restocking.",
                        "warning"
                    );
                    notificationCount++;
                }
            }
            
            // Notification 2: Items expiring soon (within 30 days)
            List<InventoryItem> expiringSoonItems = inventoryService.getItemsExpiringSoon();
            if (!expiringSoonItems.isEmpty()) {
                for (InventoryItem item : expiringSoonItems) {
                    addNotification(
                        "Expiration Alert",
                        "Item '" + item.getName() + "' expires on " + item.getExpirationDate() + 
                        ". Please use or dispose before expiration.",
                        "warning"
                    );
                    notificationCount++;
                }
            }
            
            // Notification 3: Expired items
            List<InventoryItem> expiredItems = inventoryService.getExpiredItems();
            if (!expiredItems.isEmpty()) {
                for (InventoryItem item : expiredItems) {
                    addNotification(
                        "EXPIRED Item",
                        "Item '" + item.getName() + "' has EXPIRED on " + item.getExpirationDate() + 
                        ". Remove from inventory immediately!",
                        "error"
                    );
                    notificationCount++;
                }
            }
            
            // Notification 4: Delayed bookings with details
            ServiceBookingService bookingService = new ServiceBookingService();
            List<ServiceBookingViewModel> bookings = bookingService.getAllBookings();
            List<ServiceBookingViewModel> delayedBookings = bookings.stream()
                .filter(b -> "delayed".equalsIgnoreCase(b.getStatus()))
                .toList();
            
            if (!delayedBookings.isEmpty()) {
                for (ServiceBookingViewModel booking : delayedBookings) {
                    String reason = "Booking #" + booking.getId() + " for " + booking.getCustomerName() + 
                                   " (" + booking.getVehicleInfo() + ") is DELAYED. ";
                    
                    // Check the actual reason for delay
                    try {
                        boolean hasInsufficientParts = false;
                        boolean mechanicUnavailable = false;
                        
                        // Check mechanic availability
                        String checkMechanicQuery = "SELECT m.availability FROM mechanics m " +
                                                    "JOIN service_bookings sb ON sb.mechanic_id = m.id " +
                                                    "WHERE sb.id = ?";
                        try (Connection conn = DatabaseUtil.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(checkMechanicQuery)) {
                            stmt.setInt(1, booking.getId());
                            ResultSet rs = stmt.executeQuery();
                            if (rs.next()) {
                                String availability = rs.getString("availability");
                                mechanicUnavailable = availability != null && 
                                                     !"Available".equalsIgnoreCase(availability);
                            }
                        }
                        
                        // Check parts availability
                        List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                        if (!parts.isEmpty()) {
                            for (BookingPart part : parts) {
                                String checkPartsQuery = "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?";
                                try (Connection conn = DatabaseUtil.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(checkPartsQuery)) {
                                    stmt.setInt(1, part.getPartId());
                                    ResultSet rs = stmt.executeQuery();
                                    if (rs.next()) {
                                        int stock = rs.getInt("quantity_in_stock");
                                        int reserved = rs.getInt("reserved_quantity");
                                        int available = stock - (reserved - part.getQuantity());
                                        if (available < part.getQuantity()) {
                                            hasInsufficientParts = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Build specific reason message
                        if (hasInsufficientParts && mechanicUnavailable) {
                            reason += "Reason: Insufficient parts AND mechanic unavailable.";
                        } else if (hasInsufficientParts) {
                            reason += "Reason: Insufficient parts. Parts needed: ";
                            for (int i = 0; i < parts.size() && i < 2; i++) {
                                reason += parts.get(i).getPartName() + " (" + parts.get(i).getQuantity() + ")";
                                if (i < parts.size() - 1 && i < 1) reason += ", ";
                            }
                            if (parts.size() > 2) reason += "...";
                        } else if (mechanicUnavailable) {
                            reason += "Reason: Mechanic " + booking.getMechanicName() + " is unavailable.";
                        } else {
                            reason += "Reason: Unknown. Please check booking details.";
                        }
                    } catch (Exception e) {
                        reason += "Reason: Unable to determine. Please check booking details.";
                        e.printStackTrace();
                    }
                    
                    addNotification("Delayed Booking Alert", reason, "warning");
                    notificationCount++;
                }
            }
            
            // Notification 5: Pending bookings today
            long todayPendingCount = bookings.stream()
                .filter(b -> b.getDate().equals(java.time.LocalDate.now()) &&
                            ("scheduled".equalsIgnoreCase(b.getStatus()) || 
                             "in_progress".equalsIgnoreCase(b.getStatus())))
                .count();
            
            if (todayPendingCount > 0) {
                addNotification(
                    "Today's Schedule", 
                    "You have " + todayPendingCount + " service booking(s) scheduled for today.",
                    "info"
                );
                notificationCount++;
            }
            
            // Notification 6: Overdue bills (if cashier or admin)
            if (currentUser.getRole() == User.UserRole.ADMIN || 
                currentUser.getRole() == User.UserRole.CASHIER) {
                BillingService billingService = BillingService.getInstance();
                List<Bill> bills = billingService.getAllBills();
                long overdueCount = bills.stream()
                    .filter(b -> "Overdue".equalsIgnoreCase(b.getPaymentStatus()))
                    .count();
                
                if (overdueCount > 0) {
                    addNotification(
                        "Overdue Payments", 
                        overdueCount + " bill(s) are overdue. Please follow up with customers.",
                        "error"
                    );
                    notificationCount++;
                }
            }
            
            // If no notifications, show a friendly message
            if (notificationCount == 0) {
                addNotification(
                    "All Clear!", 
                    "No urgent notifications at the moment. Everything is running smoothly.",
                    "success"
                );
                notificationCount = 1;
            }
            
            notificationCountLabel.setText(String.valueOf(notificationCount));
            
        } catch (SQLException e) {
            System.err.println("Error loading notifications: " + e.getMessage());
            e.printStackTrace();
            addNotification(
                "Error", 
                "Could not load some notifications. Please check the system.",
                "error"
            );
        }
    }
    
    private void addNotification(String title, String message, String type) {
        // Create notification card
        VBox notificationCard = new VBox(8);
        notificationCard.setPadding(new Insets(15));
        notificationCard.setStyle(getNotificationStyle(type));
        notificationCard.setMaxWidth(Double.MAX_VALUE);
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");
        
        // Time
        Label timeLabel = new Label("Just now");
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        
        notificationCard.getChildren().addAll(titleLabel, messageLabel, timeLabel);
        notificationsContainer.getChildren().add(notificationCard);
    }
    
    private String getNotificationStyle(String type) {
        String baseStyle = "-fx-background-color: %s; -fx-background-radius: 8; " +
                          "-fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 8;";
        
        switch (type.toLowerCase()) {
            case "error":
                return String.format(baseStyle, "#ffebee", "#ef5350");
            case "warning":
                return String.format(baseStyle, "#fff3e0", "#ff9800");
            case "success":
                return String.format(baseStyle, "#e8f5e9", "#4caf50");
            case "info":
            default:
                return String.format(baseStyle, "#e3f2fd", "#2196f3");
        }
    }
    
    @FXML
    private void handleRefresh() {
        // Refresh all data
        updateDateTime();
        loadStatistics();
        loadNotifications();
    }
}
