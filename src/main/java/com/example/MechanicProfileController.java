package com.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class MechanicProfileController {
    
    @FXML private Label nameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label specialtiesLabel;
    @FXML private Label currentStatusLabel;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label scheduledCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label delayedCountLabel;
    @FXML private Label completedCountLabel;
    
    private Mechanic currentMechanic;
    private final MechanicService mechanicService;
    private final ServiceBookingService bookingService;
    
    public MechanicProfileController() {
        this.mechanicService = new MechanicService();
        this.bookingService = new ServiceBookingService();
    }
    
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                // Set up status combo box - only Available and Off Duty (Busy/Overloaded are auto-calculated)
                if (statusComboBox != null) {
                    statusComboBox.getItems().addAll("Available", "Off Duty");
                }
                
                // Load mechanic data
                loadMechanicProfile();
                loadJobStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Initialization Error", 
                         "Failed to initialize mechanic profile: " + e.getMessage());
            }
        });
    }
    
    private void loadMechanicProfile() {
        try {
            // Get current logged-in user
            User currentUser = UserService.getInstance().getCurrentUser();
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No user logged in");
                return;
            }
            
            // Get mechanic record for this user
            currentMechanic = mechanicService.getMechanicByUserId(currentUser.getId());
            
            if (currentMechanic == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No mechanic profile found for this user");
                return;
            }
            
            // Populate profile information
            if (nameLabel != null) {
                nameLabel.setText(currentMechanic.getName() != null ? currentMechanic.getName() : "N/A");
            }
            if (usernameLabel != null) {
                usernameLabel.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "N/A");
            }
            if (emailLabel != null) {
                emailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");
            }
            
            // Display specialties
            if (specialtiesLabel != null) {
                if (currentMechanic.getSpecialties() != null && !currentMechanic.getSpecialties().isEmpty()) {
                    specialtiesLabel.setText(String.join(", ", currentMechanic.getSpecialties()));
                } else {
                    specialtiesLabel.setText("No specialties listed");
                }
            }
            
            // Set current status
            String status = currentMechanic.getAvailability();
            if (status == null) {
                status = "Available";
            }
            
            if (currentStatusLabel != null) {
                currentStatusLabel.setText(status);
                updateStatusLabelStyle(status);
            }
            
            // Set combo box value - map auto-calculated statuses to Available
            if (statusComboBox != null) {
                if ("Busy".equals(status) || "Overloaded".equals(status)) {
                    statusComboBox.setValue("Available");
                } else {
                    statusComboBox.setValue(status);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error loading profile: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Unexpected error loading profile: " + e.getMessage());
        }
    }
    
    private void loadJobStatistics() {
        try {
            if (currentMechanic == null) {
                return;
            }
            
            String query = "SELECT status, COUNT(*) as count FROM service_bookings " +
                          "WHERE mechanic_id = ? AND status != 'cancelled' GROUP BY status";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, currentMechanic.getId());
                ResultSet rs = stmt.executeQuery();
                
                // Reset all counts
                int scheduled = 0, inProgress = 0, delayed = 0, completed = 0;
                
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("count");
                    
                    switch (status.toLowerCase()) {
                        case "scheduled":
                            scheduled = count;
                            break;
                        case "in progress":
                        case "in_progress":
                            inProgress = count;
                            break;
                        case "delayed":
                            delayed = count;
                            break;
                        case "completed":
                            completed = count;
                            break;
                    }
                }
                
                // Update labels
                final int fScheduled = scheduled;
                final int fInProgress = inProgress;
                final int fDelayed = delayed;
                final int fCompleted = completed;
                
                Platform.runLater(() -> {
                    if (scheduledCountLabel != null) {
                        scheduledCountLabel.setText(String.valueOf(fScheduled));
                    }
                    if (inProgressCountLabel != null) {
                        inProgressCountLabel.setText(String.valueOf(fInProgress));
                    }
                    if (delayedCountLabel != null) {
                        delayedCountLabel.setText(String.valueOf(fDelayed));
                    }
                    if (completedCountLabel != null) {
                        completedCountLabel.setText(String.valueOf(fCompleted));
                    }
                });
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error loading job statistics: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Unexpected error loading job statistics: " + e.getMessage());
        }
    }
    
    @FXML
    private void updateStatus() {
        String newStatus = statusComboBox.getValue();
        
        if (newStatus == null || newStatus.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                     "Please select a status");
            return;
        }
        
        if (currentMechanic == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No mechanic profile loaded");
            return;
        }
        
        try {
            // If setting to Available, let the system calculate actual status based on workload
            if ("Available".equals(newStatus)) {
                String updateQuery = "UPDATE mechanics SET availability = ? WHERE id = ?";
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    
                    stmt.setString(1, "Available");
                    stmt.setInt(2, currentMechanic.getId());
                    stmt.executeUpdate();
                }
                
                // Let the system recalculate based on actual job count
                mechanicService.updateMechanicAvailability(currentMechanic.getId());
                
                // Reload to show actual calculated status
                User user = UserService.getInstance().getCurrentUser();
                Mechanic updated = mechanicService.getMechanicByUserId(user.getId());
                String actualStatus = updated.getAvailability();
                currentMechanic.setAvailability(actualStatus);
                currentStatusLabel.setText(actualStatus);
                updateStatusLabelStyle(actualStatus);
                
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                         "Status updated to " + actualStatus + " (calculated based on workload)");
                loadJobStatistics();
                return;
            }
            
            // Update mechanic status in database
            String updateQuery = "UPDATE mechanics SET availability = ? WHERE id = ?";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                
                stmt.setString(1, newStatus);
                stmt.setInt(2, currentMechanic.getId());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    currentMechanic.setAvailability(newStatus);
                    currentStatusLabel.setText(newStatus);
                    updateStatusLabelStyle(newStatus);
                    
                    // If status changed to Off Duty, set bookings to delayed
                    if ("Off Duty".equalsIgnoreCase(newStatus)) {
                        int delayedCount = bookingService.setBookingsToDelayedForMechanic(currentMechanic.getId());
                        if (delayedCount > 0) {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.WARNING, "Status Updated", 
                                         "Your status has been updated to " + newStatus + ".\n\n" +
                                         "⚠ " + delayedCount + " scheduled booking(s) have been set to delayed.");
                                loadJobStatistics(); // Refresh counts
                            });
                        } else {
                            showAlert(Alert.AlertType.INFORMATION, "Success", 
                                     "Your status has been updated to " + newStatus);
                        }
                    }
                    
                    loadJobStatistics(); // Refresh the job counts
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             "Failed to update status. Please try again.");
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error updating status: " + e.getMessage());
        }
    }
    
    private void updateStatusLabelStyle(String status) {
        String style = "-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;";
        
        switch (status.toLowerCase()) {
            case "available":
                currentStatusLabel.setStyle(style + " -fx-text-fill: #27ae60; -fx-background-color: #d5f4e6;");
                break;
            case "busy":
                currentStatusLabel.setStyle(style + " -fx-text-fill: #f39c12; -fx-background-color: #fdebd0;");
                break;
            case "off duty":
                currentStatusLabel.setStyle(style + " -fx-text-fill: #7f8c8d; -fx-background-color: #ecf0f1;");
                break;
            default:
                currentStatusLabel.setStyle(style + " -fx-text-fill: #34495e; -fx-background-color: #ecf0f1;");
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
