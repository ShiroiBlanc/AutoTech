package com.example;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class DashboardController {
    @FXML private StackPane contentArea;
    private Stage dashboardStage;

    @FXML private Button customersButton;
    @FXML private Button serviceBookingButton;
    @FXML private Button billingButton;
    @FXML private Button mechanicsButton;
    @FXML private Button inventoryButton;
    @FXML private Button adminButton;

    @FXML
    public void initialize() {
        // Initialize the dashboard
        try {
            System.out.println("Initializing dashboard controller");
            
            // Check if contentArea is properly injected
            if (contentArea == null) {
                System.err.println("Error: contentArea is null");
                return;
            }
            
            // Get current user and role if available
            User currentUser = UserService.getInstance().getCurrentUser();
            if (currentUser != null) {
                System.out.println("Logged in as: " + currentUser.getUsername() + 
                                   ", Role: " + currentUser.getRole());
            }
            
            // Show or hide navigation buttons based on user role
            setupRoleBasedAccess();
            
            // Default view based on role
            showDefaultView();
        } catch (Exception e) {
            System.err.println("Error in initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRoleBasedAccess() {
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.err.println("No user logged in");
            return;
        }
        
        User.UserRole role = currentUser.getRole();
        System.out.println("Setting up access for role: " + role);
        
        // First hide all buttons
        boolean showAllButtons = false;
        
        // Then selectively show them based on role
        switch (role) {
            case ADMIN:
                // Admin has access to everything
                showAllButtons = true;
                break;
                
            case CASHIER:
                // Cashier can see everything except admin panel
                showAllButtons = true;
                adminButton.setDisable(true);
                adminButton.setVisible(false);
                break;
                
            case MECHANIC:
                // Mechanics can ONLY see service bookings - hide everything else
                customersButton.setDisable(true);
                customersButton.setVisible(false);
                billingButton.setDisable(true);
                billingButton.setVisible(false);
                mechanicsButton.setDisable(true);
                mechanicsButton.setVisible(false);
                inventoryButton.setDisable(true);
                inventoryButton.setVisible(false);
                adminButton.setDisable(true);
                adminButton.setVisible(false);
                
                // Make sure service booking button is enabled and visible
                serviceBookingButton.setDisable(false);
                serviceBookingButton.setVisible(true);
                break;
        }
        
        // If not showing all buttons, make sure we've properly hidden the ones we don't want
        if (!showAllButtons) {
            System.out.println("Limited access mode for role: " + role);
        }
    }

    private void showDefaultView() {
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        switch (currentUser.getRole()) {
            case ADMIN:
                showCustomers(); // Default view for admin
                break;
            case CASHIER:
                showBilling();   // Default view for cashier
                break;
            case MECHANIC:
                showBooking();   // Default view for mechanic
                break;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            UserService.getInstance().logout();
            closeDashboardWindow();
            App.setRoot("login");
        } catch (IOException e) {
            showError("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showCustomers() { 
        try {
            // Make sure this matches your FXML filename
            loadView("customers"); 
        } catch (Exception e) {
            showError("Error loading customers view: " + e.getMessage());
        }
    }
    
    @FXML
    private void showBooking() {
        try {
            System.out.println("Loading service booking view");
            loadView("booking");
        } catch (Exception e) {
            showError("Error loading booking view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showBilling() {
        try {
            System.out.println("Loading billing view");
            loadView("billing");
        } catch (Exception e) {
            showError("Error loading billing view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showMechanics() {
        try {
            System.out.println("Loading mechanics view");
            loadView("mechanics");
        } catch (Exception e) {
            showError("Error loading mechanics view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showInventory() {
        try {
            System.out.println("Loading inventory view");
            loadView("inventory");
        } catch (Exception e) {
            showError("Error loading inventory view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showAdmin() {
        try {
            System.out.println("Loading admin view");
            loadView("admin");
        } catch (Exception e) {
            showError("Error loading admin view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void openDashboardWindow() {
        try {
            dashboardStage = new Stage();
            
            System.out.println("Looking for dashboard.fxml...");
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/dashboard.fxml");
            
            if (resourceUrl == null) {
                System.err.println("Cannot find dashboard.fxml");
                resourceUrl = App.class.getResource("/fxml/dashboard.fxml");
                System.out.println("Alternative path result: " + (resourceUrl != null ? "Found" : "Not found"));
                
                if (resourceUrl == null) {
                    throw new IOException("Cannot find dashboard.fxml resource");
                }
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            dashboardStage.setTitle("AutoTech Dashboard");
            dashboardStage.setScene(scene);
            dashboardStage.show();
            
            DashboardController controller = loader.getController();
            controller.dashboardStage = dashboardStage;
        } catch (Exception e) {
            showError("Error opening dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            System.out.println("Trying to load view: " + fxml);
            
            if (contentArea == null) {
                throw new IllegalStateException("contentArea is null, FXML not properly initialized");
            }
            
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/" + fxml + ".fxml");
            if (resourceUrl == null) {
                resourceUrl = App.class.getResource("/fxml/" + fxml + ".fxml");
            }
            
            if (resourceUrl == null) {
                throw new IOException("Cannot find FXML resource: " + fxml);
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error loading " + fxml + " view: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Unexpected error loading " + fxml + " view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeDashboardWindow() {
        if (dashboardStage != null) {
            dashboardStage.close();
            dashboardStage = null;
        }
    }

    @FXML
    private void handleAddTask() {
        System.out.println("Add task button clicked");
    }
}