package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Callback;
import java.sql.SQLException;
import java.util.List;

// Add missing imports
import javafx.scene.control.Separator;

public class CustomersController {
    @FXML private TextField searchField;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> addressColumn;
    @FXML private TableColumn<Customer, Void> actionsColumn;
    @FXML private Label statusLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private TabPane customerTabPane;

    private ObservableList<Customer> customerList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        System.out.println("Initializing CustomersController...");
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        
        // Set up actions column with edit and delete buttons
        setupActionsColumn();
        
        // Set table data
        customerTable.setItems(customerList);
        
        // Load initial data
        loadCustomers();
        
        // Make name column clickable to open customer details
        nameColumn.setCellFactory(column -> {
            TableCell<Customer, String> cell = new TableCell<Customer, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: blue; -fx-underline: true; -fx-cursor: hand;");
                    }
                }
            };
            
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    Customer customer = customerTable.getItems().get(cell.getIndex());
                    openCustomerTab(customer);
                }
            });
            
            return cell;
        });
    }
    
    private void setupActionsColumn() {
        Callback<TableColumn<Customer, Void>, TableCell<Customer, Void>> cellFactory = 
            new Callback<TableColumn<Customer, Void>, TableCell<Customer, Void>>() {
                @Override
                public TableCell<Customer, Void> call(final TableColumn<Customer, Void> param) {
                    return new TableCell<Customer, Void>() {
                        private final Button editButton = new Button("Edit");
                        private final Button deleteButton = new Button("Delete");
                        private final Button vehicleButton = new Button("Vehicles");
                        private final HBox buttonBox = new HBox(5, editButton, vehicleButton, deleteButton);
                        
                        {
                            buttonBox.setAlignment(Pos.CENTER);
                            
                            editButton.setOnAction(e -> {
                                Customer customer = getTableRow().getItem();
                                if (customer != null) {
                                    editCustomer(customer);
                                }
                            });
                            
                            // Modify vehicle button action to show vehicle management dialog
                            vehicleButton.setOnAction(e -> {
                                Customer customer = getTableRow().getItem();
                                if (customer != null) {
                                    showVehicleManagementDialog(customer);
                                }
                            });
                            
                            deleteButton.setOnAction(e -> {
                                Customer customer = getTableRow().getItem();
                                if (customer != null) {
                                    deleteCustomer(customer);
                                }
                            });
                        }
                        
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                setGraphic(buttonBox);
                            }
                        }
                    };
                }
            };
        
        actionsColumn.setCellFactory(cellFactory);
    }
    
    @FXML
    private void handleSearchCustomers() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadCustomers();
        } else {
            try {
                List<Customer> customers = CustomerService.getInstance().searchCustomers(searchTerm);
                customerList.clear();
                customerList.addAll(customers);
                updateTotalCustomersLabel();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, 
                         "Search Error", 
                         "Error searching customers: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleRefreshCustomers() {
        searchField.clear();
        loadCustomers();
    }
    
    @FXML
    private void handleAddCustomer() {
        showCustomerDialog(null);
    }
    
    private void editCustomer(Customer customer) {
        showCustomerDialog(customer);
    }
    
    private void deleteCustomer(Customer customer) {
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Customer");
        alert.setContentText("Are you sure you want to delete customer: " + customer.getName() + "?");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    boolean success = CustomerService.getInstance().deleteCustomer(customer.getId());
                    if (success) {
                        customerList.remove(customer);
                        updateTotalCustomersLabel();
                        statusLabel.setText("Customer deleted successfully");
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                 "Deletion Failed", 
                                 "Could not delete customer. Please try again.");
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
    
    private void showCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(customer == null ? "Add New Customer" : "Edit Customer");
        dialog.setHeaderText(customer == null ? "Enter customer details" : "Edit customer details");
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        
        // Set existing values if editing
        if (customer != null) {
            nameField.setText(customer.getName());
            phoneField.setText(customer.getPhone());
            emailField.setText(customer.getEmail());
            addressField.setText(customer.getAddress());
        }
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Address:"), 0, 3);
        grid.add(addressField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Focus on name field by default
        nameField.requestFocus();
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String phone = phoneField.getText().trim();
                    String email = emailField.getText().trim();
                    String address = addressField.getText().trim();
                    
                    if (name.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Name is required");
                        return null;
                    }
                    
                    boolean success;
                    if (customer == null) {
                        // Add new customer
                        success = CustomerService.getInstance().addCustomer(name, phone, email, address);
                        if (success) {
                            statusLabel.setText("Customer added successfully");
                        }
                    } else {
                        // Update existing customer
                        customer.setName(name);
                        customer.setPhone(phone);
                        customer.setEmail(email);
                        customer.setAddress(address);
                        success = CustomerService.getInstance().updateCustomer(customer);
                        if (success) {
                            statusLabel.setText("Customer updated successfully");
                            customerTable.refresh();
                        }
                    }
                    
                    if (success) {
                        loadCustomers();
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                 customer == null ? "Creation Failed" : "Update Failed", 
                                 "Could not save customer information. Please try again.");
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
    
    // Replace showAddVehicleDialog with a more comprehensive vehicle management dialog
    private void showVehicleManagementDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vehicle Management");
        dialog.setHeaderText("Vehicles for " + customer.getName());
        
        // Set button types
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Create table for vehicles
        TableView<Vehicle> vehicleTable = new TableView<>();
        TableColumn<Vehicle, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
        
        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));
        
        TableColumn<Vehicle, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        
        TableColumn<Vehicle, String> plateCol = new TableColumn<>("Plate Number");
        plateCol.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));
        
        TableColumn<Vehicle, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(param -> new TableCell<Vehicle, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox box = new HBox(5, editButton, deleteButton);
            
            {
                box.setAlignment(Pos.CENTER);
                
                editButton.setOnAction(e -> {
                    Vehicle vehicle = getTableRow().getItem();
                    if (vehicle != null) {
                        dialog.close();
                        showAddVehicleDialog(customer, vehicle);
                    }
                });
                
                deleteButton.setOnAction(e -> {
                    Vehicle vehicle = getTableRow().getItem();
                    if (vehicle != null) {
                        try {
                            boolean success = VehicleService.getInstance().deleteVehicle(vehicle.getId());
                            if (success) {
                                loadVehicles(customer, vehicleTable);
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle deleted successfully");
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete vehicle");
                            }
                        } catch (SQLException ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete vehicle: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        
        vehicleTable.getColumns().addAll(typeCol, brandCol, modelCol, yearCol, plateCol, actionsCol);
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Load vehicles for this customer
        loadVehicles(customer, vehicleTable);
        
        // Add button to add new vehicle
        Button addButton = new Button("Add New Vehicle");
        addButton.setOnAction(e -> {
            dialog.close();
            showAddVehicleDialog(customer, null);
        });
        
        content.getChildren().addAll(vehicleTable, addButton);
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    private void loadVehicles(Customer customer, TableView<Vehicle> table) {
        try {
            List<Vehicle> vehicles = VehicleService.getInstance().getCustomerVehicles(customer.getId());
            table.setItems(FXCollections.observableArrayList(vehicles));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load vehicles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update the method to handle editing existing vehicles
    private void showAddVehicleDialog(Customer customer, Vehicle existingVehicle) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle(existingVehicle == null ? "Add Vehicle" : "Edit Vehicle");
        dialog.setHeaderText((existingVehicle == null ? "Add" : "Edit") + " vehicle for " + customer.getName());
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField typeField = new TextField();
        typeField.setPromptText("Vehicle Type");
        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        TextField modelField = new TextField();
        modelField.setPromptText("Model");
        TextField yearField = new TextField();
        yearField.setPromptText("Year");
        TextField plateNumberField = new TextField();
        plateNumberField.setPromptText("Plate Number");
        
        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Year:"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Plate Number:"), 0, 4);
        grid.add(plateNumberField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Focus on type field by default
        typeField.requestFocus();
        
        // Set existing values if editing
        if (existingVehicle != null) {
            typeField.setText(existingVehicle.getType());
            brandField.setText(existingVehicle.getBrand());
            modelField.setText(existingVehicle.getModel());
            yearField.setText(existingVehicle.getYear());
            plateNumberField.setText(existingVehicle.getPlateNumber());
        }
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String type = typeField.getText().trim();
                    String brand = brandField.getText().trim();
                    String model = modelField.getText().trim();
                    String year = yearField.getText().trim();
                    String plateNumber = plateNumberField.getText().trim();
                    
                    if (type.isEmpty() || brand.isEmpty() || plateNumber.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Type, brand, and plate number are required");
                        return null;
                    }
                    
                    boolean success;
                    if (existingVehicle == null) {
                        // Add new vehicle
                        success = VehicleService.getInstance().addVehicle(
                            customer.getId(), type, brand, model, year, plateNumber);
                    } else {
                        // Update existing vehicle
                        existingVehicle.setType(type);
                        existingVehicle.setBrand(brand);
                        existingVehicle.setModel(model);
                        existingVehicle.setYear(year);
                        existingVehicle.setPlateNumber(plateNumber);
                        success = VehicleService.getInstance().updateVehicle(existingVehicle);
                    }
                    
                    if (success) {
                        statusLabel.setText("Vehicle " + (existingVehicle == null ? "added" : "updated") + " successfully");
                        // Show the vehicle management dialog again after adding/editing
                        showVehicleManagementDialog(customer);
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                 existingVehicle == null ? "Creation Failed" : "Update Failed", 
                                 "Could not save vehicle information. Please try again.");
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
    
    private void loadCustomers() {
        try {
            List<Customer> customers = CustomerService.getInstance().getAllCustomers();
            customerList.clear();
            customerList.addAll(customers);
            updateTotalCustomersLabel();
            statusLabel.setText("Customers loaded successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Load Error", 
                     "Error loading customers: " + e.getMessage());
        }
    }
    
    private void updateTotalCustomersLabel() {
        totalCustomersLabel.setText("Total customers: " + customerList.size());
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Method to open a customer's details in a new window
    private void openCustomerTab(Customer customer) {
        try {
            // Create a new window
            Stage detailStage = new Stage();
            detailStage.setTitle("Customer: " + customer.getName());
            detailStage.initModality(Modality.NONE); // Allow interaction with main window
            
            // Create the content for the window
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: white;");
            
            // Add a header
            Label headerLabel = new Label("Customer Details: " + customer.getName());
            headerLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
            
            // Customer details section
            GridPane detailsGrid = new GridPane();
            detailsGrid.setHgap(10);
            detailsGrid.setVgap(10);
            detailsGrid.setPadding(new Insets(10, 0, 10, 0));
            
            // Add customer information
            detailsGrid.add(new Label("Name:"), 0, 0);
            detailsGrid.add(new Label(customer.getName()), 1, 0);
            
            detailsGrid.add(new Label("Phone:"), 0, 1);
            detailsGrid.add(new Label(customer.getPhone() != null ? customer.getPhone() : ""), 1, 1);
            
            detailsGrid.add(new Label("Email:"), 0, 2);
            detailsGrid.add(new Label(customer.getEmail() != null ? customer.getEmail() : ""), 1, 2);
            
            detailsGrid.add(new Label("Address:"), 0, 3);
            detailsGrid.add(new Label(customer.getAddress() != null ? customer.getAddress() : ""), 1, 3);
            
            // Vehicle section
            Label vehiclesLabel = new Label("Vehicles");
            vehiclesLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            
            // Create table for vehicles
            TableView<Vehicle> vehicleTable = new TableView<>();
            vehicleTable.setPrefHeight(200);
            
            TableColumn<Vehicle, String> typeCol = new TableColumn<>("Type");
            typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
            
            TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
            brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
            
            TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
            modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));
            
            TableColumn<Vehicle, String> yearCol = new TableColumn<>("Year");
            yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
            
            TableColumn<Vehicle, String> plateCol = new TableColumn<>("Plate Number");
            plateCol.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));
            
            // Add actions column for vehicles
            TableColumn<Vehicle, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setCellFactory(param -> new TableCell<Vehicle, Void>() {
                private final Button editButton = new Button("Edit");
                private final Button deleteButton = new Button("Delete");
                private final HBox box = new HBox(5, editButton, deleteButton);
                
                {
                    box.setAlignment(Pos.CENTER);
                    
                    editButton.setOnAction(e -> {
                        Vehicle vehicle = getTableRow().getItem();
                        if (vehicle != null) {
                            showAddVehicleDialog(customer, vehicle);
                            // Refresh vehicle list after editing
                            loadVehicles(customer, vehicleTable);
                        }
                    });
                    
                    deleteButton.setOnAction(e -> {
                        Vehicle vehicle = getTableRow().getItem();
                        if (vehicle != null) {
                            try {
                                boolean success = VehicleService.getInstance().deleteVehicle(vehicle.getId());
                                if (success) {
                                    loadVehicles(customer, vehicleTable);
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle deleted successfully");
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete vehicle");
                                }
                            } catch (SQLException ex) {
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete vehicle: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            
            vehicleTable.getColumns().addAll(typeCol, brandCol, modelCol, yearCol, plateCol, actionsCol);
            vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            // Load vehicles for this customer
            loadVehicles(customer, vehicleTable);
            
            // Add buttons for actions
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            
            Button addVehicleButton = new Button("Add New Vehicle");
            addVehicleButton.setOnAction(e -> {
                showAddVehicleDialog(customer, null);
                // Refresh vehicle list after adding
                loadVehicles(customer, vehicleTable);
            });
            
            Button editCustomerButton = new Button("Edit Customer");
            editCustomerButton.setOnAction(e -> {
                editCustomer(customer);
                
                // Update the window title and header after editing
                detailStage.setTitle("Customer: " + customer.getName());
                headerLabel.setText("Customer Details: " + customer.getName());
                
                // Update the displayed info
                ((Label)detailsGrid.getChildren().get(1)).setText(customer.getName());
                ((Label)detailsGrid.getChildren().get(3)).setText(customer.getPhone() != null ? customer.getPhone() : "");
                ((Label)detailsGrid.getChildren().get(5)).setText(customer.getEmail() != null ? customer.getEmail() : "");
                ((Label)detailsGrid.getChildren().get(7)).setText(customer.getAddress() != null ? customer.getAddress() : "");
            });
            
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> detailStage.close());
            
            buttonBox.getChildren().addAll(addVehicleButton, editCustomerButton, closeButton);
            
            // Add all components to the content
            content.getChildren().addAll(
                headerLabel,
                new Separator(),
                detailsGrid,
                new Separator(),
                vehiclesLabel,
                vehicleTable,
                buttonBox
            );
            
            // Create scene and show window
            Scene scene = new Scene(content, 800, 600);
            detailStage.setScene(scene);
            detailStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open customer details: " + e.getMessage());
        }
    }
}
