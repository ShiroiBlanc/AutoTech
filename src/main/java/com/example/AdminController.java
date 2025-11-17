package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;  // Added missing import
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;  // Added missing import
import java.sql.*;
import java.util.List;
import com.example.User.UserRole;

public class AdminController {
    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, UserRole> roleColumn;
    @FXML private TableColumn<User, Boolean> statusColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    
    private ObservableList<User> userList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        System.out.println("Initializing AdminController...");
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Format the status column to show "Active" or "Disabled"
        statusColumn.setCellFactory(col -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                } else {
                    setText(active ? "Active" : "Disabled");
                    setStyle(active ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });
        
        // Set up actions column with enable/disable and delete buttons
        setupActionsColumn();
        
        // Set table data
        userTable.setItems(userList);
        
        // Load initial data
        loadUsers();
    }
    
    private void setupActionsColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = 
            new Callback<TableColumn<User, Void>, TableCell<User, Void>>() {
                @Override
                public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                    return new TableCell<User, Void>() {
                        private final Button toggleButton = new Button("Toggle");
                        private final Button deleteButton = new Button("Delete");
                        private final HBox buttonBox = new HBox(5, toggleButton, deleteButton);
                        
                        {
                            buttonBox.setAlignment(Pos.CENTER);
                            
                            toggleButton.setOnAction(e -> {
                                User user = getTableRow().getItem();
                                if (user != null) {
                                    toggleUserStatus(user);
                                }
                            });
                            
                            deleteButton.setOnAction(e -> {
                                User user = getTableRow().getItem();
                                if (user != null) {
                                    deleteUser(user);
                                }
                            });
                        }
                        
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                User user = getTableRow().getItem();
                                if (user != null) {
                                    toggleButton.setText(user.isActive() ? "Disable" : "Enable");
                                    
                                    // Prevent admins from disabling themselves
                                    User currentUser = UserService.getInstance().getCurrentUser();
                                    if (currentUser != null && user.getId() == currentUser.getId()) {
                                        toggleButton.setDisable(true);
                                        deleteButton.setDisable(true);
                                        toggleButton.setTooltip(new Tooltip("Cannot modify your own account"));
                                        deleteButton.setTooltip(new Tooltip("Cannot delete your own account"));
                                    } else {
                                        toggleButton.setDisable(false);
                                        deleteButton.setDisable(false);
                                        toggleButton.setTooltip(null);
                                        deleteButton.setTooltip(null);
                                    }
                                }
                                setGraphic(buttonBox);
                            }
                        }
                    };
                }
            };
        
        actionsColumn.setCellFactory(cellFactory);
    }
    
    private void toggleUserStatus(User user) {
        try {
            boolean newStatus = !user.isActive();
            boolean success = UserService.getInstance().toggleUserStatus(user.getId(), newStatus);
            
            if (success) {
                // Update the user object locally
                user.setActive(newStatus);
                
                // Refresh the table
                userTable.refresh();
                
                showAlert(Alert.AlertType.INFORMATION, 
                        "User Status Updated", 
                        "User " + user.getUsername() + " has been " + 
                        (newStatus ? "enabled" : "disabled") + ".");
            } else {
                showAlert(Alert.AlertType.ERROR, 
                        "Update Failed", 
                        "Could not update user status. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                    "Error", 
                    "An error occurred: " + e.getMessage());
        }
    }
    
    private void deleteUser(User user) {
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete user " + user.getUsername() + "?");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    boolean success = UserService.getInstance().deleteUser(user.getId());
                    
                    if (success) {
                        // Remove from the observable list
                        userList.remove(user);
                        
                        showAlert(Alert.AlertType.INFORMATION, 
                                "User Deleted", 
                                "User " + user.getUsername() + " has been deleted.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                "Deletion Failed", 
                                "Could not delete user. Please try again.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, 
                            "Error", 
                            "An error occurred: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleSearchUsers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadUsers();
        } else {
            try {
                List<User> users = UserService.getInstance().searchUsers(searchTerm);
                userList.clear();
                userList.addAll(users);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, 
                         "Search Error", 
                         "Error searching users: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleRefreshUsers() {
        searchField.clear();
        loadUsers();
    }
    
    @FXML
    private void handleAddUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new user account");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        TextField email = new TextField();
        email.setPromptText("Email");
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(
            "ADMIN", "CASHIER", "MECHANIC"
        ));
        role.setValue("CASHIER");
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(email, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(role, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Focus on the username field by default
        username.requestFocus();
        
        // Convert the result to a User object when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String usernameText = username.getText().trim();
                    String passwordText = password.getText().trim();
                    String emailText = email.getText().trim();
                    String roleText = role.getValue();
                    
                    if (usernameText.isEmpty() || passwordText.isEmpty() || emailText.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "All fields are required");
                        return null;
                    }
                    
                    UserRole userRole = UserRole.fromString(roleText);
                    
                    boolean success = UserService.getInstance().addUser(
                        usernameText, passwordText, emailText, userRole);
                        
                    if (success) {
                        loadUsers();
                        showAlert(Alert.AlertType.INFORMATION, 
                                 "User Created", 
                                 "User " + usernameText + " has been created.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                 "Creation Failed", 
                                 "Could not create user. Username might already exist.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, 
                             "Error", 
                             "An error occurred: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void loadUsers() {
        try {
            List<User> users = UserService.getInstance().getAllUsers();
            userList.clear();
            userList.addAll(users);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Load Error", 
                     "Error loading users: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @FXML
    private void handleManageServiceLaborCosts() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Service Labor Costs");
        dialog.setHeaderText("Set labor costs for different service types");
        
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        mainBox.setPrefWidth(600);
        
        // Instructions
        Label instructions = new Label(
            "Define standard labor costs for common services. These serve as defaults when creating service records."
        );
        instructions.setWrapText(true);
        instructions.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");
        mainBox.getChildren().add(instructions);
        
        // Labor costs will be loaded from database
        
        // Table to display service types
        TableView<ServiceLaborCost> table = new TableView<>();
        ObservableList<ServiceLaborCost> laborCostList = FXCollections.observableArrayList();
        
        TableColumn<ServiceLaborCost, String> serviceCol = new TableColumn<>("Service Type");
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        serviceCol.setPrefWidth(300);
        
        TableColumn<ServiceLaborCost, Double> costCol = new TableColumn<>("Labor Cost (₱)");
        costCol.setCellValueFactory(new PropertyValueFactory<>("laborCost"));
        costCol.setPrefWidth(200);
        
        TableColumn<ServiceLaborCost, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(80);
        
        actionsCol.setCellFactory(col -> new TableCell<ServiceLaborCost, Void>() {
            private final Button editButton = new Button("Edit");
            
            {
                editButton.setOnAction(e -> {
                    ServiceLaborCost cost = getTableRow().getItem();
                    if (cost != null) {
                        showEditLaborCostDialog(cost, laborCostList);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editButton);
            }
        });
        
        table.getColumns().addAll(serviceCol, costCol, actionsCol);
        
        // Load labor costs from database
        loadLaborCostsFromDatabase(laborCostList);
        
        table.setItems(laborCostList);
        table.setPrefHeight(400);
        mainBox.getChildren().add(table);
        
        // Add custom service button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        Button addCustomButton = new Button("Add Custom Service");
        addCustomButton.setOnAction(e -> showAddCustomServiceDialog(laborCostList));
        buttonBox.getChildren().add(addCustomButton);
        mainBox.getChildren().add(buttonBox);
        
        // Note
        Label note = new Label(
            "Note: These labor costs are used when calculating billing amounts for services."
        );
        note.setWrapText(true);
        note.setStyle("-fx-font-size: 11; -fx-text-fill: #888; -fx-font-style: italic;");
        mainBox.getChildren().add(note);
        
        dialog.getDialogPane().setContent(mainBox);
        dialog.showAndWait();
    }
    
    private void loadLaborCostsFromDatabase(ObservableList<ServiceLaborCost> list) {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT service_type, labor_cost FROM service_labor_costs ORDER BY service_type")) {
            
            list.clear();
            while (rs.next()) {
                list.add(new ServiceLaborCost(
                    rs.getString("service_type"),
                    rs.getDouble("labor_cost")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load labor costs: " + e.getMessage());
        }
    }
    
    private void showEditLaborCostDialog(ServiceLaborCost cost, ObservableList<ServiceLaborCost> list) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Labor Cost");
        dialog.setHeaderText("Edit labor cost for: " + cost.getServiceType());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField costField = new TextField(String.valueOf(cost.getLaborCost()));
        
        grid.add(new Label("Labor Cost (₱):"), 0, 0);
        grid.add(costField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double newCost = Double.parseDouble(costField.getText());
                    
                    if (newCost < 0) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", "Cost cannot be negative");
                        return null;
                    }
                    
                    // Update in database
                    try (Connection conn = DatabaseUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE service_labor_costs SET labor_cost = ? WHERE service_type = ?")) {
                        
                        stmt.setDouble(1, newCost);
                        stmt.setString(2, cost.getServiceType());
                        stmt.executeUpdate();
                        
                        // Update UI
                        cost.setLaborCost(newCost);
                        list.set(list.indexOf(cost), cost);
                        
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Labor cost updated successfully");
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update: " + e.getMessage());
                    }
                    
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number");
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void showAddCustomServiceDialog(ObservableList<ServiceLaborCost> list) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Custom Service");
        dialog.setHeaderText("Add a custom service labor cost");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField serviceField = new TextField();
        serviceField.setPromptText("Service Type");
        TextField costField = new TextField();
        costField.setPromptText("Labor Cost");
        
        grid.add(new Label("Service Type:"), 0, 0);
        grid.add(serviceField, 1, 0);
        grid.add(new Label("Labor Cost (₱):"), 0, 1);
        grid.add(costField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String service = serviceField.getText().trim();
                    double laborCost = Double.parseDouble(costField.getText());
                    
                    if (service.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", "Service type cannot be empty");
                        return null;
                    }
                    
                    if (laborCost < 0) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", "Cost cannot be negative");
                        return null;
                    }
                    
                    // Insert into database
                    try (Connection conn = DatabaseUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO service_labor_costs (service_type, labor_cost) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE labor_cost = ?")) {
                        
                        stmt.setString(1, service);
                        stmt.setDouble(2, laborCost);
                        stmt.setDouble(3, laborCost);
                        stmt.executeUpdate();
                        
                        // Reload table
                        loadLaborCostsFromDatabase(list);
                        
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Service type added successfully");
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add service: " + e.getMessage());
                    }
                    
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number for cost");
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    // Inner class for ServiceLaborCost
    public static class ServiceLaborCost {
        private String serviceType;
        private double laborCost;
        
        public ServiceLaborCost(String serviceType, double laborCost) {
            this.serviceType = serviceType;
            this.laborCost = laborCost;
        }
        
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        
        public double getLaborCost() { return laborCost; }
        public void setLaborCost(double laborCost) { this.laborCost = laborCost; }
    }
}