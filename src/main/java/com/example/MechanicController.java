package com.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private TableView<MechanicViewModel> mechanicTable;
    @FXML private TableColumn<MechanicViewModel, Integer> idColumn;
    @FXML private TableColumn<MechanicViewModel, String> nameColumn;
    @FXML private TableColumn<MechanicViewModel, String> specialtyColumn;
    @FXML private TableColumn<MechanicViewModel, String> availabilityColumn;
    @FXML private TableColumn<MechanicViewModel, Integer> currentJobsColumn;
    @FXML private TableColumn<MechanicViewModel, Void> actionsColumn;
    @FXML private Label statusLabel;
    @FXML private Label totalMechanicsLabel;
    @FXML private TabPane mechanicTabPane;

    private ObservableList<MechanicViewModel> mechanicList = FXCollections.observableArrayList();
    private MechanicService mechanicService = new MechanicService();
    
    // ViewModel for mechanics with additional properties
    public static class MechanicViewModel {
        private int id;
        private String name;
        private String specialty;
        private String availability;
        private int currentJobs;
        
        public MechanicViewModel(int id, String name, String specialty, String availability, int currentJobs) {
            this.id = id;
            this.name = name;
            this.specialty = specialty;
            this.availability = availability;
            this.currentJobs = currentJobs;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getSpecialty() { return specialty; }
        public String getAvailability() { return availability; }
        public int getCurrentJobs() { return currentJobs; }
        
        public void setAvailability(String availability) { this.availability = availability; }
        public void setCurrentJobs(int currentJobs) { this.currentJobs = currentJobs; }
    }
    
    @FXML
    public void initialize() {
        System.out.println("Initializing MechanicController...");
        
        // Setup filter combo box
        filterStatusComboBox.getItems().addAll("All", "Available", "Busy", "Off Duty");
        filterStatusComboBox.setValue("All");
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        specialtyColumn.setCellValueFactory(new PropertyValueFactory<>("specialty"));
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("availability"));
        currentJobsColumn.setCellValueFactory(new PropertyValueFactory<>("currentJobs"));
        
        // Style the availability column with colors
        availabilityColumn.setCellFactory(column -> {
            return new TableCell<MechanicViewModel, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        switch (item) {
                            case "Available":
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                break;
                            case "Busy":
                                setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                break;
                            case "Off Duty":
                                setStyle("-fx-text-fill: red;");
                                break;
                            default:
                                setStyle("");
                        }
                    }
                }
            };
        });
        
        // Set up actions column with buttons
        setupActionsColumn();
        
        // Set table data
        mechanicTable.setItems(mechanicList);
        
        // Load initial data
        loadMechanics();
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> {
            return new TableCell<MechanicViewModel, Void>() {
                private final Button viewButton = new Button("View Jobs");
                private final Button statusButton = new Button("Status");
                private final HBox buttonBox = new HBox(5, viewButton, statusButton);
                
                {
                    buttonBox.setAlignment(Pos.CENTER);
                    
                    viewButton.setOnAction(e -> {
                        MechanicViewModel mechanic = getTableRow().getItem();
                        if (mechanic != null) {
                            showJobOrders(mechanic);
                        }
                    });
                    
                    statusButton.setOnAction(e -> {
                        MechanicViewModel mechanic = getTableRow().getItem();
                        if (mechanic != null) {
                            updateMechanicStatus(mechanic);
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
        });
    }
    
    @FXML
    private void handleSearchMechanics() {
        String searchTerm = searchField.getText().trim();
        String statusFilter = filterStatusComboBox.getValue();
        
        loadMechanicsFiltered(searchTerm, statusFilter);
    }
    
    @FXML
    private void handleRefreshMechanics() {
        searchField.clear();
        filterStatusComboBox.setValue("All");
        loadMechanics();
    }
    
    @FXML
    private void handleAddMechanic() {
        showMechanicDialog(null);
    }
    
    private void loadMechanics() {
        try {
            List<Mechanic> mechanics = mechanicService.getAllMechanics();
            updateMechanicViewList(mechanics);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Load Error", 
                     "Error loading mechanics: " + e.getMessage());
        }
    }
    
    private void loadMechanicsFiltered(String searchTerm, String statusFilter) {
        try {
            List<Mechanic> mechanics;
            
            if (searchTerm.isEmpty() && statusFilter.equals("All")) {
                mechanics = mechanicService.getAllMechanics();
            } else {
                // In a real application, you would implement a filtered query
                // This is a simplified example
                mechanics = mechanicService.searchMechanics(searchTerm, statusFilter);
            }
            
            updateMechanicViewList(mechanics);
            statusLabel.setText("Search complete");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Search Error", 
                     "Error searching mechanics: " + e.getMessage());
        }
    }
    
    private void updateMechanicViewList(List<Mechanic> mechanics) throws SQLException {
        mechanicList.clear();
        
        // Get current jobs count for each mechanic
        Map<Integer, Integer> jobCounts = mechanicService.getCurrentJobCounts();
        
        // Create view models with additional data
        for (Mechanic mechanic : mechanics) {
            String availability = mechanicService.getMechanicAvailability(mechanic.getId());
            int jobCount = jobCounts.getOrDefault(mechanic.getId(), 0);
            
            mechanicList.add(new MechanicViewModel(
                mechanic.getId(),
                mechanic.getName(),
                mechanic.getSpecialty(),
                availability,
                jobCount
            ));
        }
        
        updateTotalMechanicsLabel();
    }
    
    private void updateTotalMechanicsLabel() {
        totalMechanicsLabel.setText("Total mechanics: " + mechanicList.size());
    }
    
    private void showMechanicDialog(MechanicViewModel mechanic) {
        Dialog<Mechanic> dialog = new Dialog<>();
        dialog.setTitle(mechanic == null ? "Add New Mechanic" : "Edit Mechanic");
        dialog.setHeaderText(mechanic == null ? "Enter mechanic details" : "Edit mechanic details");
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Change User to MechanicService.User for the ComboBox
        ComboBox<MechanicService.User> userComboBox = new ComboBox<>();
        
        // Only add name field - specialty is managed in admin panel
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        
        // Load users with MECHANIC role for association
        try {
            // Change User to MechanicService.User for the list
            List<MechanicService.User> mechanicUsers = mechanicService.getUsersWithMechanicRole();
            userComboBox.setItems(FXCollections.observableArrayList(mechanicUsers));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load user list: " + e.getMessage());
        }
        
        // Set existing values if editing
        if (mechanic != null) {
            nameField.setText(mechanic.getName());
            // We don't display or edit specialty in this dialog
        }
        
        grid.add(new Label("User:"), 0, 0);
        grid.add(userComboBox, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        
        // Remove specialty field as it's managed in admin panel
        
        dialog.getDialogPane().setContent(grid);
        
        // Process result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                MechanicService.User selectedUser = userComboBox.getValue();
                String name = nameField.getText().trim();
                
                if (selectedUser == null) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please select a user");
                    return null;
                }
                
                if (name.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Name is required");
                    return null;
                }
                
                try {
                    boolean success;
                    if (mechanic == null) {
                        // Add new mechanic - pass empty string for specialty as it will be set in admin panel
                        success = mechanicService.addMechanic(selectedUser.getId(), name, "");
                    } else {
                        // Update existing mechanic - keep existing specialty
                        success = mechanicService.updateMechanic(mechanic.getId(), selectedUser.getId(), name, mechanic.getSpecialty());
                    }
                    
                    if (success) {
                        loadMechanics();
                        statusLabel.setText("Mechanic " + (mechanic == null ? "added" : "updated") + " successfully");
                    } else {
                        showAlert(Alert.AlertType.ERROR, 
                                "Error", 
                                "Failed to " + (mechanic == null ? "add" : "update") + " mechanic");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void showJobOrders(MechanicViewModel mechanic) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Job Orders for " + mechanic.getName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            Label headerLabel = new Label("Current Jobs for " + mechanic.getName());
            headerLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
            
            // Create table for service orders
            TableView<ServiceOrder> jobTable = new TableView<>();
            jobTable.setPrefHeight(400);
            
            TableColumn<ServiceOrder, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            
            TableColumn<ServiceOrder, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            
            TableColumn<ServiceOrder, String> customerCol = new TableColumn<>("Customer");
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            
            TableColumn<ServiceOrder, String> vehicleCol = new TableColumn<>("Vehicle");
            vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
            
            TableColumn<ServiceOrder, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            TableColumn<ServiceOrder, String> descCol = new TableColumn<>("Description");
            descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
            
            jobTable.getColumns().addAll(idCol, dateCol, customerCol, vehicleCol, statusCol, descCol);
            
            // Load job orders for this mechanic
            List<ServiceOrder> jobs = mechanicService.getMechanicJobs(mechanic.getId());
            jobTable.setItems(FXCollections.observableArrayList(jobs));
            
            Label countLabel = new Label("Total jobs: " + jobs.size());
            
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> dialogStage.close());
            
            content.getChildren().addAll(
                headerLabel,
                new Label("───────────────────────────────────────"), // Replace Separator with a simple line
                jobTable,
                countLabel,
                closeButton
            );
            
            Scene scene = new Scene(content, 800, 600);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load job orders: " + e.getMessage());
        }
    }
    
    private void updateMechanicStatus(MechanicViewModel mechanic) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Status");
        dialog.setHeaderText("Update status for " + mechanic.getName());
        
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Available", "Busy", "Off Duty");
        statusComboBox.setValue(mechanic.getAvailability());
        
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusComboBox, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                boolean success = mechanicService.updateMechanicStatus(mechanic.getId(), newStatus);
                if (success) {
                    mechanic.setAvailability(newStatus);
                    mechanicTable.refresh();
                    statusLabel.setText("Status updated successfully");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update status");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update status: " + e.getMessage());
            }
        });
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Service Order model class for the job orders table
    public static class ServiceOrder {
        private int id;
        private String date;
        private String customerName;
        private String vehicleInfo;
        private String status;
        private String description;
        
        public ServiceOrder(int id, String date, String customerName, String vehicleInfo, 
                           String status, String description) {
            this.id = id;
            this.date = date;
            this.customerName = customerName;
            this.vehicleInfo = vehicleInfo;
            this.status = status;
            this.description = description;
        }
        
        public int getId() { return id; }
        public String getDate() { return date; }
        public String getCustomerName() { return customerName; }
        public String getVehicleInfo() { return vehicleInfo; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}