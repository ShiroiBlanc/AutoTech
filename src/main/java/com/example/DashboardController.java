package com.example;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Add this missing import
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;  // Add this import
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

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
            
            // Add more detailed debugging
            System.out.println("Looking for dashboard.fxml...");
            
            // Check the working directory
            System.out.println("Working directory: " + System.getProperty("user.dir"));
            
            // Try first path
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/dashboard.fxml");
            System.out.println("First attempt path: " + (resourceUrl != null ? resourceUrl.toString() : "Not found"));
            
            if (resourceUrl == null) {
                System.err.println("Cannot find dashboard.fxml in first location");
                // Try alternative paths
                String[] paths = {
                    "/fxml/dashboard.fxml",
                    "dashboard.fxml",
                    "/dashboard.fxml",
                    "../resources/fxml/dashboard.fxml"
                };
                
                for (String path : paths) {
                    System.out.println("Trying path: " + path);
                    resourceUrl = App.class.getResource(path);
                    if (resourceUrl != null) {
                        System.out.println("Found at: " + resourceUrl);
                        break;
                    }
                }
                
                if (resourceUrl == null) {
                    throw new IOException("Cannot find dashboard.fxml resource");
                }
            }
            
            try {
                FXMLLoader loader = new FXMLLoader(resourceUrl);
                Parent root = loader.load();
                
                Scene scene = new Scene(root, 1200, 800);
                dashboardStage.setTitle("AutoTech Dashboard");
                dashboardStage.setScene(scene);
                dashboardStage.show();
                
                DashboardController controller = loader.getController();
                controller.dashboardStage = dashboardStage;
            } catch (Exception fxmlException) {
                // Emergency workaround - create dashboard programmatically
                System.err.println("Error loading dashboard.fxml: " + fxmlException.getMessage());
                fxmlException.printStackTrace();
                
                // Create a minimal dashboard layout
                HBox root = new HBox();
                
                // Create sidebar
                VBox sidebar = new VBox(10);
                sidebar.setPrefWidth(200);
                sidebar.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10;");
                
                // Add elements to sidebar
                javafx.scene.control.Label title = new javafx.scene.control.Label("AutoTech System"); // Use fully qualified name
                title.setStyle("-fx-font-size: 20; -fx-text-fill: white;");
                
                // Create all navigation buttons
                customersButton = new Button("Customers");
                customersButton.setMaxWidth(Double.MAX_VALUE);
                customersButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                customersButton.setOnAction(e -> showCustomers());
                
                serviceBookingButton = new Button("Service Bookings");
                serviceBookingButton.setMaxWidth(Double.MAX_VALUE);
                serviceBookingButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                serviceBookingButton.setOnAction(e -> showBooking());
                
                billingButton = new Button("Billing");
                billingButton.setMaxWidth(Double.MAX_VALUE);
                billingButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                billingButton.setOnAction(e -> showBilling());
                
                mechanicsButton = new Button("Mechanics");
                mechanicsButton.setMaxWidth(Double.MAX_VALUE);
                mechanicsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                mechanicsButton.setOnAction(e -> showMechanics());
                
                inventoryButton = new Button("Inventory");
                inventoryButton.setMaxWidth(Double.MAX_VALUE);
                inventoryButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                inventoryButton.setOnAction(e -> showInventory());
                
                adminButton = new Button("Admin Panel");
                adminButton.setMaxWidth(Double.MAX_VALUE);
                adminButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                adminButton.setOnAction(e -> showAdmin());
                
                // Create a spacer region to push logout to bottom
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                
                // Add logout button
                Button logoutButton = new Button("Logout");
                logoutButton.setMaxWidth(Double.MAX_VALUE);
                logoutButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
                logoutButton.setOnAction(e -> handleLogout());
                
                // Add all buttons to sidebar
                sidebar.getChildren().addAll(
                    title, 
                    customersButton, 
                    serviceBookingButton,
                    billingButton,
                    mechanicsButton,
                    inventoryButton,
                    adminButton,
                    spacer,  // Add spacer before logout button
                    logoutButton
                );
                
                // Create content area
                contentArea = new StackPane();
                contentArea.setStyle("-fx-background-color: white;");
                StackPane.setMargin(contentArea, new Insets(20));
                HBox.setHgrow(contentArea, Priority.ALWAYS);
                
                // Add both to root
                root.getChildren().addAll(sidebar, contentArea);
                
                // Create scene with programmatically created layout
                Scene scene = new Scene(root, 1200, 800);
                dashboardStage.setTitle("AutoTech Dashboard (Emergency Mode)");
                dashboardStage.setScene(scene);
                dashboardStage.show();
                
                // Setup user access
                setupRoleBasedAccess();
                showDefaultView();
                
                return; // Exit early since we handled it programmatically
            }
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
            
            // Replace "service" with "booking" since we're removing the duplicate file
            String actualFxml = fxml;
            if ("service".equals(fxml)) {
                actualFxml = "booking";
                System.out.println("Redirecting service view to booking.fxml");
            }
            
            URL resourceUrl = getClass().getClassLoader().getResource("fxml/" + actualFxml + ".fxml");
            if (resourceUrl == null) {
                resourceUrl = App.class.getResource("/fxml/" + actualFxml + ".fxml");
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
            dashboardStage = null;
        }
    }

    public void closeDashboardWindow() {
        if (dashboardStage != null) {
            dashboardStage.close();
            dashboardStage = null;
        }
    }
}
