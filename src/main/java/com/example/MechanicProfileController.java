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
        // Set up status combo box
        statusComboBox.getItems().addAll("Available", "Busy", "Off Duty");
        
        // Load mechanic data
        loadMechanicProfile();
        loadJobStatistics();
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
            nameLabel.setText(currentMechanic.getName());
            usernameLabel.setText(currentUser.getUsername());
            emailLabel.setText(currentUser.getEmail());
            
            // Display specialties
            if (currentMechanic.getSpecialties() != null && !currentMechanic.getSpecialties().isEmpty()) {
                specialtiesLabel.setText(String.join(", ", currentMechanic.getSpecialties()));
            } else {
                specialtiesLabel.setText("No specialties listed");
            }
            
            // Set current status
            String status = currentMechanic.getAvailability();
            currentStatusLabel.setText(status);
            statusComboBox.setValue(status);
            
            // Update status label styling based on availability
            updateStatusLabelStyle(status);
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error loading profile: " + e.getMessage());
        }
    }
    
    private void loadJobStatistics() {
        try {
            if (currentMechanic == null) {
                return;
            }
            
            String query = "SELECT status, COUNT(*) as count FROM service_bookings " +
                          "WHERE mechanic_id = ? GROUP BY status";
            
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
                scheduledCountLabel.setText(String.valueOf(scheduled));
                inProgressCountLabel.setText(String.valueOf(inProgress));
                delayedCountLabel.setText(String.valueOf(delayed));
                completedCountLabel.setText(String.valueOf(completed));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error loading job statistics: " + e.getMessage());
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
                    
                    // If status changed to Available, check for delayed bookings
                    if ("Available".equalsIgnoreCase(newStatus)) {
                        int updatedCount = bookingService.checkAndUpdateDelayedBookings();
                        if (updatedCount > 0) {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Status Updated", 
                                         "Your status has been updated to " + newStatus + ".\n\n" +
                                         "✓ " + updatedCount + " delayed booking(s) automatically updated to scheduled!");
                                loadJobStatistics(); // Refresh counts
                            });
                        } else {
                            showAlert(Alert.AlertType.INFORMATION, "Success", 
                                     "Your status has been updated to " + newStatus);
                        }
                    } 
                    // If status changed to Off Duty or Busy, set bookings to delayed
                    else if ("Off Duty".equalsIgnoreCase(newStatus) || "Busy".equalsIgnoreCase(newStatus)) {
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
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Success", 
                                 "Your status has been updated to " + newStatus);
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
