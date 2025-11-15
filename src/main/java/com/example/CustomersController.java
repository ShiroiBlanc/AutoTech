package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
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
    @FXML private TableColumn<Customer, String> idColumn;
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
        idColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
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
                        private final Button viewButton = new Button("View");
                        private final Button editButton = new Button("Edit");
                        private final Button deleteButton = new Button("Delete");
                        private final HBox buttonBox = new HBox(5, viewButton, editButton, deleteButton);
                        
                        {
                            buttonBox.setAlignment(Pos.CENTER);
                            
                            viewButton.setOnAction(e -> {
                                Customer customer = getTableRow().getItem();
                                if (customer != null) {
                                    openCustomerTab(customer);
                                }
                            });
                            
                            editButton.setOnAction(e -> {
                                Customer customer = getTableRow().getItem();
                                if (customer != null) {
                                    editCustomer(customer);
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
        dialog.setTitle(customer == null ? "Add New Customer" : "Edit Customer - " + customer.getHexId());
        dialog.setHeaderText(customer == null ? "Enter customer details" : "Customer ID: " + customer.getHexId());
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 250, 10, 10)); // Increased from 150 to 250 for vehicle buttons
        
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        
        // Add listener to phone field to only allow numbers, spaces, dashes, and parentheses
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9\\s\\-\\(\\)\\+]*")) {
                phoneField.setText(oldValue);
            }
        });
        
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
                    
                    // Check only for duplicate email addresses
                    if (!email.isEmpty()) {
                        try {
                            List<Customer> allCustomers = CustomerService.getInstance().getAllCustomers();
                            
                            for (Customer existingCustomer : allCustomers) {
                                // Skip the current customer when editing (don't compare with self)
                                if (customer != null && existingCustomer.getId() == customer.getId()) {
                                    continue;
                                }
                                
                                // Only check for email duplication
                                if (!email.isEmpty() && email.equalsIgnoreCase(existingCustomer.getEmail())) {
                                    showAlert(Alert.AlertType.ERROR, "Duplicate Email", 
                                            "A customer with this email already exists.");
                                    return null;
                                }
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Could not check for duplicate customers: " + ex.getMessage());
                            return null;
                        }
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
    private void loadVehicles(Customer customer, TableView<Vehicle> table) {
        try {
            List<Vehicle> vehicles = VehicleService.getInstance().getCustomerVehicles(customer.getId());
            table.setItems(FXCollections.observableArrayList(vehicles));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load vehicles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Fix the vehicle dialog to properly accept input by using Stage instead of Dialog
    private void showAddVehicleDialog(Customer customer, Vehicle existingVehicle) {
        try {
            // Create a new Stage instead of a Dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle(existingVehicle == null ? "Add Vehicle" : "Edit Vehicle: " + existingVehicle.getHexId());
            dialogStage.initModality(Modality.APPLICATION_MODAL); // Block input to other windows
            
            // Create form layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            // Header
            Label headerLabel = new Label((existingVehicle == null ? "Add" : "Edit") + " vehicle for " + customer.getName());
            headerLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            
            // Create form fields
            ComboBox<String> typeComboBox = new ComboBox<>();
            typeComboBox.setPromptText("Select Vehicle Type");
            typeComboBox.getItems().addAll(
                "Sedan",
                "SUV",
                "Truck",
                "Van",
                "Motorcycle",
                "Other"
            );
            typeComboBox.setMaxWidth(Double.MAX_VALUE);
            
            // Create a text field for "Other" type (initially hidden)
            TextField otherTypeField = new TextField();
            otherTypeField.setPromptText("Enter vehicle type");
            otherTypeField.setVisible(false);
            otherTypeField.setManaged(false); // Don't take up space when hidden
            
            // Show/hide the other type field based on selection
            typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if ("Other".equals(newValue)) {
                    otherTypeField.setVisible(true);
                    otherTypeField.setManaged(true);
                    otherTypeField.requestFocus();
                } else {
                    otherTypeField.setVisible(false);
                    otherTypeField.setManaged(false);
                    otherTypeField.clear();
                }
            });
            
            TextField brandField = new TextField();
            brandField.setPromptText("Brand");
            TextField modelField = new TextField();
            modelField.setPromptText("Model");
            TextField yearField = new TextField();
            yearField.setPromptText("Year");
            
            // Add listener to year field to only allow numbers
            yearField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    yearField.setText(oldValue);
                }
            });
            
            TextField plateNumberField = new TextField();
            plateNumberField.setPromptText("Plate Number");
            
            // Set existing values if editing
            if (existingVehicle != null) {
                String existingType = existingVehicle.getType();
                // Check if existing type is in the dropdown
                if (typeComboBox.getItems().contains(existingType) && !"Other".equals(existingType)) {
                    typeComboBox.setValue(existingType);
                } else if (!"Other".equals(existingType)) {
                    // If it's a custom type, select "Other" and show it in the text field
                    typeComboBox.setValue("Other");
                    otherTypeField.setText(existingType);
                    otherTypeField.setVisible(true);
                    otherTypeField.setManaged(true);
                } else {
                    typeComboBox.setValue(existingType);
                }
                brandField.setText(existingVehicle.getBrand());
                modelField.setText(existingVehicle.getModel());
                yearField.setText(existingVehicle.getYear());
                plateNumberField.setText(existingVehicle.getPlateNumber());
            }
            
            // Add fields to grid
            grid.add(headerLabel, 0, 0, 2, 1);
            grid.add(new Separator(), 0, 1, 2, 1);
            
            grid.add(new Label("Type:"), 0, 2);
            grid.add(typeComboBox, 1, 2);
            grid.add(otherTypeField, 1, 3); // Add the "Other" text field below type dropdown
            grid.add(new Label("Brand:"), 0, 4);
            grid.add(brandField, 1, 4);
            grid.add(new Label("Model:"), 0, 5);
            grid.add(modelField, 1, 5);
            grid.add(new Label("Year:"), 0, 6);
            grid.add(yearField, 1, 6);
            grid.add(new Label("Plate Number:"), 0, 7);
            grid.add(plateNumberField, 1, 7);
            
            // Add buttons
            Button saveButton = new Button("Save");
            Button cancelButton = new Button("Cancel");
            
            HBox buttonBox = new HBox(10, saveButton, cancelButton);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));
            grid.add(buttonBox, 0, 8, 2, 1);
            
            // Set up button actions
            saveButton.setOnAction(e -> {
                try {
                    String selectedType = typeComboBox.getValue() != null ? typeComboBox.getValue().trim() : "";
                    String type;
                    
                    // If "Other" is selected, use the custom type from otherTypeField
                    if ("Other".equals(selectedType)) {
                        type = otherTypeField.getText().trim();
                        if (type.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Please specify the vehicle type");
                            return;
                        }
                    } else {
                        type = selectedType;
                    }
                    
                    String brand = brandField.getText().trim();
                    String model = modelField.getText().trim();
                    String year = yearField.getText().trim();
                    String plateNumber = plateNumberField.getText().trim();
                    
                    // Validation
                    if (type.isEmpty() || brand.isEmpty() || plateNumber.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Type, brand, and plate number are required");
                        return;
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
                        dialogStage.close();
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                 existingVehicle == null ? "Creation Failed" : "Update Failed", 
                                 "Could not save vehicle information. Please try again.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + ex.getMessage());
                }
            });
            
            cancelButton.setOnAction(e -> {
                dialogStage.close();
            });
            
            // Set preferred width for fields
            typeComboBox.setPrefWidth(250);
            otherTypeField.setPrefWidth(250);
            brandField.setPrefWidth(250);
            modelField.setPrefWidth(250);
            yearField.setPrefWidth(250);
            plateNumberField.setPrefWidth(250);
            
            // Create scene and show dialog
            Scene scene = new Scene(grid, 400, 400);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
            
            // Focus on the first field
            typeComboBox.requestFocus();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open vehicle form: " + e.getMessage());
        }
    }
    
    private void showTransferVehicleDialog(Vehicle vehicle, Customer currentOwner) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Transfer Vehicle");
        dialog.setHeaderText("Transfer " + vehicle.getBrand() + " " + vehicle.getModel() + 
                            " (" + vehicle.getHexId() + ") to a different customer");
        
        // Set button types
        ButtonType transferButtonType = new ButtonType("Transfer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferButtonType, ButtonType.CANCEL);
        
        // Create the content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Current owner info
        Label currentOwnerLabel = new Label("Current Owner:");
        Label currentOwnerValue = new Label(currentOwner.getName() + " (" + currentOwner.getHexId() + ")");
        currentOwnerValue.setStyle("-fx-font-weight: bold;");
        
        // Vehicle details
        Label vehicleLabel = new Label("Vehicle:");
        Label vehicleValue = new Label(vehicle.getType() + " - " + vehicle.getBrand() + " " + 
                                       vehicle.getModel() + " (" + vehicle.getYear() + ")");
        vehicleValue.setStyle("-fx-font-weight: bold;");
        
        Label plateLabel = new Label("Plate Number:");
        Label plateValue = new Label(vehicle.getPlateNumber());
        plateValue.setStyle("-fx-font-weight: bold;");
        
        // Customer selection
        Label newOwnerLabel = new Label("Transfer To:");
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        
        // Load all customers except current owner
        try {
            List<Customer> allCustomers = CustomerService.getInstance().getAllCustomers();
            allCustomers.removeIf(c -> c.getId() == currentOwner.getId());
            customerComboBox.setItems(FXCollections.observableArrayList(allCustomers));
            customerComboBox.setConverter(new javafx.util.StringConverter<Customer>() {
                @Override
                public String toString(Customer customer) {
                    return customer == null ? "" : customer.getName() + " (" + customer.getHexId() + ")";
                }
                
                @Override
                public Customer fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load customers: " + e.getMessage());
            return;
        }
        
        grid.add(currentOwnerLabel, 0, 0);
        grid.add(currentOwnerValue, 1, 0);
        grid.add(vehicleLabel, 0, 1);
        grid.add(vehicleValue, 1, 1);
        grid.add(plateLabel, 0, 2);
        grid.add(plateValue, 1, 2);
        grid.add(new Label(), 0, 3); // Spacer
        grid.add(newOwnerLabel, 0, 4);
        grid.add(customerComboBox, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Enable/Disable transfer button depending on whether a customer is selected
        javafx.scene.Node transferButton = dialog.getDialogPane().lookupButton(transferButtonType);
        transferButton.setDisable(true);
        
        customerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            transferButton.setDisable(newValue == null);
        });
        
        // Convert the result to a customer when the transfer button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == transferButtonType) {
                return customerComboBox.getValue();
            }
            return null;
        });
        
        // Show dialog and process result
        dialog.showAndWait().ifPresent(newOwner -> {
            // Confirm transfer
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Transfer");
            confirmAlert.setHeaderText("Confirm Vehicle Transfer");
            confirmAlert.setContentText(String.format(
                "Transfer %s %s (%s)\n" +
                "FROM: %s (%s)\n" +
                "TO: %s (%s)\n\n" +
                "Are you sure you want to proceed?",
                vehicle.getBrand(), vehicle.getModel(), vehicle.getPlateNumber(),
                currentOwner.getName(), currentOwner.getHexId(),
                newOwner.getName(), newOwner.getHexId()
            ));
            
            confirmAlert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        boolean success = VehicleService.getInstance().transferVehicle(
                            vehicle.getId(), 
                            newOwner.getId()
                        );
                        
                        if (success) {
                            statusLabel.setText("Vehicle transferred successfully from " + 
                                              currentOwner.getName() + " to " + newOwner.getName());
                            showAlert(Alert.AlertType.INFORMATION, 
                                     "Transfer Complete", 
                                     "Vehicle has been successfully transferred to " + newOwner.getName());
                            
                            // No need to open another dialog - transfer is complete
                        } else {
                            showAlert(Alert.AlertType.ERROR, 
                                     "Transfer Failed", 
                                     "Could not transfer vehicle. Please try again.");
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, 
                                 "Error", 
                                 "An error occurred during transfer: " + ex.getMessage());
                    }
                }
            });
        });
        
        // Focus on customer combo box
        customerComboBox.requestFocus();
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
            detailsGrid.add(new Label("Customer ID:"), 0, 0);
            detailsGrid.add(new Label(customer.getHexId()), 1, 0);
            
            detailsGrid.add(new Label("Name:"), 0, 1);
            detailsGrid.add(new Label(customer.getName()), 1, 1);
            
            detailsGrid.add(new Label("Phone:"), 0, 2);
            detailsGrid.add(new Label(customer.getPhone() != null ? customer.getPhone() : ""), 1, 2);
            
            detailsGrid.add(new Label("Email:"), 0, 3);
            detailsGrid.add(new Label(customer.getEmail() != null ? customer.getEmail() : ""), 1, 3);
            
            detailsGrid.add(new Label("Address:"), 0, 4);
            detailsGrid.add(new Label(customer.getAddress() != null ? customer.getAddress() : ""), 1, 4);
            
            // Vehicle section
            Label vehiclesLabel = new Label("Vehicles");
            vehiclesLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            
            // Create table for vehicles
            TableView<Vehicle> vehicleTable = new TableView<>();
            vehicleTable.setPrefHeight(200);
            
            TableColumn<Vehicle, String> vehicleIdCol = new TableColumn<>("Vehicle ID");
            vehicleIdCol.setCellValueFactory(new PropertyValueFactory<>("hexId"));
            vehicleIdCol.setPrefWidth(120);
            
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
                private final Button transferButton = new Button("Transfer");
                private final Button deleteButton = new Button("Delete");
                private final HBox box = new HBox(5, editButton, transferButton, deleteButton);
                
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
                    
                    transferButton.setOnAction(e -> {
                        Vehicle vehicle = getTableRow().getItem();
                        if (vehicle != null) {
                            detailStage.close();
                            showTransferVehicleDialog(vehicle, customer);
                        }
                    });
                    
                    deleteButton.setOnAction(e -> {
                        Vehicle vehicle = getTableRow().getItem();
                        if (vehicle != null) {
                            try {
                                // Check if vehicle has unpaid bills
                                if (VehicleService.getInstance().hasUnpaidBills(vehicle.getId())) {
                                    int billCount = VehicleService.getInstance().getUnpaidBillCount(vehicle.getId());
                                    showAlert(Alert.AlertType.WARNING, 
                                             "Cannot Delete Vehicle", 
                                             "This vehicle cannot be deleted because it has " + billCount + 
                                             " unpaid bill(s) associated with it.\n\n" +
                                             "Please pay all outstanding bills before deleting this vehicle.");
                                    return;
                                }
                                
                                // Check if vehicle has active service bookings (not completed or cancelled)
                                if (VehicleService.getInstance().hasActiveServiceBookings(vehicle.getId())) {
                                    int bookingCount = VehicleService.getInstance().getActiveServiceBookingCount(vehicle.getId());
                                    showAlert(Alert.AlertType.WARNING, 
                                             "Cannot Delete Vehicle", 
                                             "This vehicle cannot be deleted because it has " + bookingCount + 
                                             " active service booking(s) associated with it.\n\n" +
                                             "You can delete this vehicle after:\n" +
                                             "• Completing or cancelling the active bookings, OR\n" +
                                             "• Deleting the service bookings\n\n" +
                                             "Alternatively, consider transferring the vehicle to another customer instead.");
                                    return;
                                }
                                
                                // Confirm deletion
                                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                                confirmAlert.setTitle("Confirm Deletion");
                                confirmAlert.setHeaderText("Delete Vehicle");
                                confirmAlert.setContentText("Are you sure you want to delete this vehicle?\n\n" +
                                                           vehicle.getBrand() + " " + vehicle.getModel() + 
                                                           " (" + vehicle.getPlateNumber() + ")\n\n" +
                                                           "Warning: This will also delete:\n" +
                                                           "• All completed or cancelled service bookings\n" +
                                                           "• Associated billing records");
                                
                                confirmAlert.showAndWait().ifPresent(result -> {
                                    if (result == ButtonType.OK) {
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
                            } catch (SQLException ex) {
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to check vehicle status: " + ex.getMessage());
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
            
            vehicleTable.getColumns().addAll(vehicleIdCol, typeCol, brandCol, modelCol, yearCol, plateCol, actionsCol);
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