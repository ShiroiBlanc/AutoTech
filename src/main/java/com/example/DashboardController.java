package com.example;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class DashboardController {
    @FXML private StackPane contentArea;
    private Stage dashboardStage;

    @FXML private Button homeButton;
    @FXML private Button customersButton;
    @FXML private Button serviceBookingButton;
    @FXML private Button billingButton;
    @FXML private Button mechanicsButton;
    @FXML private Button inventoryButton;
    @FXML private Button adminButton;
    @FXML private Button myProfileButton;

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing dashboard controller");
            
            if (contentArea == null) {
                System.err.println("Error: contentArea is null");
                return;
            }
            
            User currentUser = UserService.getInstance().getCurrentUser();
            if (currentUser != null) {
                System.out.println("Logged in as: " + currentUser.getUsername() + 
                                   ", Role: " + currentUser.getRole());
            }
            
            setupRoleBasedAccess();
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
        
        boolean showAllButtons = false;
        
        switch (role) {
            case ADMIN:
                showAllButtons = true;
                break;
                
            case CASHIER:
                showAllButtons = true;
                adminButton.setDisable(true);
                adminButton.setVisible(false);
                break;
                
            case MECHANIC:
                customersButton.setDisable(true);
                customersButton.setVisible(false);
                customersButton.setManaged(false);
                
                billingButton.setDisable(true);
                billingButton.setVisible(false);
                billingButton.setManaged(false);
                
                mechanicsButton.setDisable(true);
                mechanicsButton.setVisible(false);
                mechanicsButton.setManaged(false);
                
                inventoryButton.setDisable(true);
                inventoryButton.setVisible(false);
                inventoryButton.setManaged(false);
                
                adminButton.setDisable(true);
                adminButton.setVisible(false);
                adminButton.setManaged(false);
                
                serviceBookingButton.setDisable(false);
                serviceBookingButton.setVisible(true);
                
                myProfileButton.setDisable(false);
                myProfileButton.setVisible(true);
                break;
        }
        
        if (role != User.UserRole.MECHANIC) {
            myProfileButton.setDisable(true);
            myProfileButton.setVisible(false);
            myProfileButton.setManaged(false);
        }
    }

    private void showDefaultView() {
        showHome();
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
    private void showHome() {
        setActiveButton(homeButton);
        loadView("home.fxml");  // Changed to use existing home.fxml
    }

    @FXML
    private void showCustomers() {
        setActiveButton(customersButton);
        loadView("customers.fxml");  // Changed to use existing customers.fxml
    }

    @FXML
    private void showBooking() {
        setActiveButton(serviceBookingButton);
        loadView("booking.fxml");  // Changed from servicebooking.fxml to booking.fxml
    }

    @FXML
    private void showBilling() {
        setActiveButton(billingButton);
        loadView("billing.fxml");
    }

    @FXML
    private void showMechanics() {
        setActiveButton(mechanicsButton);
        loadView("mechanics.fxml");
    }

    @FXML
    private void showInventory() {
        setActiveButton(inventoryButton);
        loadView("inventory.fxml");
    }

    @FXML
    private void showAdmin() {
        setActiveButton(adminButton);
        loadView("admin.fxml");
    }

    @FXML
    private void showMyProfile() {
        setActiveButton(myProfileButton);
        loadView("mechanic-profile.fxml");
    }
    
    private void setActiveButton(Button activeButton) {
        homeButton.getStyleClass().remove("nav-button-active");
        customersButton.getStyleClass().remove("nav-button-active");
        serviceBookingButton.getStyleClass().remove("nav-button-active");
        billingButton.getStyleClass().remove("nav-button-active");
        mechanicsButton.getStyleClass().remove("nav-button-active");
        inventoryButton.getStyleClass().remove("nav-button-active");
        adminButton.getStyleClass().remove("nav-button-active");
        myProfileButton.getStyleClass().remove("nav-button-active");
        
        if (!activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    public void openDashboardWindow() {
        try {
            dashboardStage = new Stage();
            
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/dashboard.fxml");
            if (resourceUrl == null) {
                resourceUrl = App.class.getResource("/fxml/dashboard.fxml");
            }
            
            if (resourceUrl == null) {
                throw new IOException("Cannot find dashboard.fxml");
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            dashboardStage.setTitle("AutoTech Dashboard");
            dashboardStage.setScene(scene);
            
            DashboardController controller = loader.getController();
            controller.dashboardStage = dashboardStage;
            
            dashboardStage.show();
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
            
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/" + fxml);
            if (resourceUrl == null) {
                resourceUrl = App.class.getResource("/fxml/" + fxml);
            }
            
            if (resourceUrl == null) {
                System.err.println("ERROR: Cannot find FXML resource: fxml/" + fxml); // Added
                throw new IOException("Cannot find FXML resource: " + fxml);
            }
            
            System.out.println("Loading from URL: " + resourceUrl); // Added
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();
            System.out.println("Successfully loaded view: " + fxml); // Added
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("IOException loading " + fxml + ": " + e.getMessage()); // Added
            showError("Error loading " + fxml + " view: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error loading " + fxml + ": " + e.getMessage()); // Added
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
}
