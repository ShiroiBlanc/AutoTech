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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MechanicController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Button addMechanicButton;
    @FXML private Pagination mechanicPagination;
    @FXML private TableView<MechanicViewModel> mechanicTable;
    @FXML private TableColumn<MechanicViewModel, String> idColumn;
    @FXML private TableColumn<MechanicViewModel, String> nameColumn;
    @FXML private TableColumn<MechanicViewModel, String> specialtyColumn;
    @FXML private TableColumn<MechanicViewModel, String> availabilityColumn;
    @FXML private TableColumn<MechanicViewModel, Integer> currentJobsColumn;
    @FXML private TableColumn<MechanicViewModel, Void> actionsColumn;
    @FXML private Label statusLabel;
    @FXML private Label totalMechanicsLabel;
    @FXML private TabPane mechanicTabPane;

    private ObservableList<MechanicViewModel> mechanicList = FXCollections.observableArrayList();
    private ObservableList<MechanicViewModel> allMechanics = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 25;
    private MechanicService mechanicService = new MechanicService();
    
    // ViewModel for mechanics with additional properties
    public static class MechanicViewModel {
        private int id;
        private String hexId;
        private String name;
        private String specialty;
        private String availability;
        private int currentJobs;
        
        public MechanicViewModel(int id, String hexId, String name, String specialty, String availability, int currentJobs) {
            this.id = id;
            this.hexId = hexId;
            this.name = name;
            this.specialty = specialty;
            this.availability = availability;
            this.currentJobs = currentJobs;
        }
        
        public int getId() { return id; }
        public String getHexId() { return hexId; }
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
        
        // Check user role and hide button for non-admins
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            if (addMechanicButton != null) {
                addMechanicButton.setVisible(false);
                addMechanicButton.setManaged(false);
            }
        }
        
        // Setup filter combo box
        filterStatusComboBox.getItems().addAll("All", "Available", "Busy", "Overloaded", "Off Duty");
        filterStatusComboBox.setValue("All");
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        specialtyColumn.setCellValueFactory(new PropertyValueFactory<>("specialty"));
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("availability"));
        currentJobsColumn.setCellValueFactory(new PropertyValueFactory<>("currentJobs"));
        
        // Set table resize policy
        mechanicTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Make ID column clickable to show full details
        idColumn.setCellFactory(column -> {
            return new TableCell<MechanicViewModel, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #0066cc; -fx-underline: true; -fx-cursor: hand;");
                        
                        setOnMouseClicked(event -> {
                            MechanicViewModel mechanic = getTableRow().getItem();
                            if (mechanic != null) {
                                showMechanicDetails(mechanic);
                            }
                        });
                    }
                }
            };
        });
        
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
                            case "Overloaded":
                                setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
                                break;
                            case "Off Duty":
                                setStyle("-fx-text-fill: #888888;");
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
        
        // Setup pagination
        setupPagination();
        
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
        // Double-check role permission - admin only
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", 
                     "Only administrators can set mechanic specialties.");
            return;
        }
        showMechanicDialog(null);
    }
    
    private void setupPagination() {
        if (mechanicPagination != null) {
            mechanicPagination.setPageFactory(pageIndex -> {
                updateTablePage(pageIndex);
                return mechanicTable;
            });
        }
    }
    
    private void updateTablePage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allMechanics.size());
        
        mechanicList.clear();
        if (fromIndex < allMechanics.size()) {
            mechanicList.addAll(allMechanics.subList(fromIndex, toIndex));
        }
    }
    
    private void updatePaginationControl() {
        if (mechanicPagination != null) {
            int pageCount = (int) Math.ceil((double) allMechanics.size() / ITEMS_PER_PAGE);
            mechanicPagination.setPageCount(Math.max(1, pageCount));
        }
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
        allMechanics.clear();
        
        // Get current job counts for all mechanics
        Map<Integer, Integer> jobCounts = mechanicService.getAllMechanicsJobCounts();
        
        // Create view models with real-time data
        for (Mechanic mechanic : mechanics) {
            // Calculate availability based on current jobs
            String availability = mechanicService.calculateAvailability(mechanic.getId());
            
            // Get current job count
            int jobCount = jobCounts.getOrDefault(mechanic.getId(), 0);
            
            allMechanics.add(new MechanicViewModel(
                mechanic.getId(),
                mechanic.getHexId(),
                mechanic.getName(),
                mechanic.getSpecialtiesAsString(),
                availability,
                jobCount
            ));
        }
        
        // Update pagination control
        updatePaginationControl();
        
        // Update table - will show first page
        if (mechanicPagination != null) {
            updateTablePage(0);
            mechanicPagination.setCurrentPageIndex(0);
        } else {
            mechanicList.clear();
            mechanicList.addAll(allMechanics);
        }
        
        updateTotalMechanicsLabel();
    }
    
    private void updateTotalMechanicsLabel() {
        totalMechanicsLabel.setText("Total mechanics: " + allMechanics.size());
    }
    
    private void showMechanicDetails(MechanicViewModel mechanicViewModel) {
        try {
            // Fetch full mechanic details
            Mechanic mechanic = mechanicService.getAllMechanics().stream()
                .filter(m -> m.getId() == mechanicViewModel.getId())
                .findFirst()
                .orElse(null);
            
            if (mechanic == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Mechanic not found.");
                return;
            }
            
            // Create dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Mechanic Details");
            dialog.setHeaderText("Full Details for Mechanic " + mechanic.getHexId());
            
            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: white;");
            
            // ID
            Label idLabel = new Label("ID: " + mechanic.getHexId());
            idLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            // Name
            Label nameLabel = new Label("Name: " + mechanic.getName());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            // Get real-time availability
            String currentAvailability = mechanicService.calculateAvailability(mechanic.getId());
            
            // Availability
            Label availabilityLabel = new Label("Availability: " + currentAvailability);
            availabilityLabel.setStyle("-fx-font-size: 14px;");
            switch (currentAvailability) {
                case "Available":
                    availabilityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: green; -fx-font-weight: bold;");
                    break;
                case "Busy":
                    availabilityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: orange; -fx-font-weight: bold;");
                    break;
                case "Overloaded":
                    availabilityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff4444; -fx-font-weight: bold;");
                    break;
                case "Off Duty":
                    availabilityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888; -fx-font-weight: bold;");
                    break;
            }
            
            // Current job count
            int currentJobCount = mechanicService.getCurrentJobCount(mechanic.getId());
            Label jobCountLabel = new Label("Current Active Jobs: " + currentJobCount);
            jobCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            if (currentJobCount > 0) {
                jobCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0066cc;");
            }
            
            // Specialties section
            Label specialtiesHeaderLabel = new Label("Specialties:");
            specialtiesHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-underline: true;");
            
            VBox specialtiesList = new VBox(5);
            List<String> specialties = mechanic.getSpecialties();
            if (specialties.isEmpty()) {
                Label noSpecialties = new Label("• No specialties listed");
                noSpecialties.setStyle("-fx-font-size: 13px; -fx-text-fill: gray; -fx-font-style: italic;");
                specialtiesList.getChildren().add(noSpecialties);
            } else {
                for (String specialty : specialties) {
                    Label specialtyLabel = new Label("• " + specialty);
                    specialtyLabel.setStyle("-fx-font-size: 13px;");
                    specialtyLabel.setWrapText(true);
                    specialtiesList.getChildren().add(specialtyLabel);
                }
            }
            
            ScrollPane specialtiesScrollPane = new ScrollPane(specialtiesList);
            specialtiesScrollPane.setFitToWidth(true);
            specialtiesScrollPane.setPrefHeight(150);
            specialtiesScrollPane.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;");
            
            content.getChildren().addAll(
                idLabel,
                nameLabel,
                availabilityLabel,
                jobCountLabel,
                new Separator(),
                specialtiesHeaderLabel,
                specialtiesScrollPane
            );
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(450);
            
            dialog.showAndWait();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load mechanic details: " + e.getMessage());
        }
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
        
        // Multiple specialties selection with checkboxes
        VBox specialtiesBox = new VBox(5);
        Label specialtiesLabel = new Label("Specialties (select all that apply):");
        specialtiesLabel.setStyle("-fx-font-weight: bold;");
        
        List<CheckBox> specialtyCheckBoxes = new ArrayList<>();
        String[] availableSpecialties = {
            "General Maintenance",
            "Engine Specialist",
            "Transmission Specialist",
            "Electrical Systems",
            "Brake Specialist",
            "Air Conditioning",
            "Diagnostics Expert",
            "Body Work",
            "Tire Specialist"
        };
        
        for (String specialty : availableSpecialties) {
            CheckBox cb = new CheckBox(specialty);
            specialtyCheckBoxes.add(cb);
            specialtiesBox.getChildren().add(cb);
        }
        
        // Add "Other" checkbox with text field
        CheckBox otherCheckBox = new CheckBox("Other");
        TextField otherSpecialtyField = new TextField();
        otherSpecialtyField.setPromptText("Enter custom specialty");
        otherSpecialtyField.setDisable(true);
        otherSpecialtyField.setManaged(false);
        otherSpecialtyField.setVisible(false);
        
        otherCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            otherSpecialtyField.setDisable(!newVal);
            otherSpecialtyField.setManaged(newVal);
            otherSpecialtyField.setVisible(newVal);
        });
        
        specialtyCheckBoxes.add(otherCheckBox);
        specialtiesBox.getChildren().addAll(otherCheckBox, otherSpecialtyField);
        
        ScrollPane specialtiesScrollPane = new ScrollPane(specialtiesBox);
        specialtiesScrollPane.setFitToWidth(true);
        specialtiesScrollPane.setPrefHeight(200);
        specialtiesScrollPane.setStyle("-fx-background-color: white;");
        
        // Load users with MECHANIC role for association
        try {
            List<MechanicService.User> mechanicUsers = mechanicService.getUsersWithMechanicRole();
            userComboBox.setItems(FXCollections.observableArrayList(mechanicUsers));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load user list: " + e.getMessage());
        }
        
        // Set existing values if editing
        if (mechanic != null) {
            // Pre-select existing specialties
            try {
                Mechanic fullMechanic = mechanicService.getAllMechanics().stream()
                    .filter(m -> m.getId() == mechanic.getId())
                    .findFirst()
                    .orElse(null);
                
                if (fullMechanic != null) {
                    List<String> existingSpecialties = fullMechanic.getSpecialties();
                    for (CheckBox cb : specialtyCheckBoxes) {
                        if (existingSpecialties.contains(cb.getText())) {
                            cb.setSelected(true);
                        }
                    }
                    // Check if there are custom specialties
                    for (String specialty : existingSpecialties) {
                        boolean isStandard = false;
                        for (String std : availableSpecialties) {
                            if (std.equals(specialty)) {
                                isStandard = true;
                                break;
                            }
                        }
                        if (!isStandard) {
                            otherCheckBox.setSelected(true);
                            otherSpecialtyField.setText(specialty);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        grid.add(new Label("User:"), 0, 0);
        grid.add(userComboBox, 1, 0);
        grid.add(specialtiesLabel, 0, 1);
        grid.add(specialtiesScrollPane, 1, 1);
        
        // Remove specialty field as it's managed in admin panel
        
        dialog.getDialogPane().setContent(grid);
        
        // Process result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                MechanicService.User selectedUser = userComboBox.getValue();
                
                if (selectedUser == null) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please select a user");
                    return null;
                }
                
                // Collect selected specialties
                List<String> selectedSpecialties = new ArrayList<>();
                for (CheckBox cb : specialtyCheckBoxes) {
                    if (cb.isSelected()) {
                        if (cb == otherCheckBox) {
                            String customSpecialty = otherSpecialtyField.getText().trim();
                            if (!customSpecialty.isEmpty()) {
                                selectedSpecialties.add(customSpecialty);
                            }
                        } else {
                            selectedSpecialties.add(cb.getText());
                        }
                    }
                }
                
                if (selectedSpecialties.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please select at least one specialty");
                    return null;
                }
                
                // Convert list to comma-separated string
                String specialtiesStr = String.join(", ", selectedSpecialties);
                
                try {
                    boolean success;
                    
                    if (mechanic == null) {
                        // Add new mechanic with selected specialties
                        success = mechanicService.addMechanic(selectedUser.getId(), selectedUser.getUsername(), specialtiesStr);
                    } else {
                        // Update existing mechanic with new specialties
                        success = mechanicService.updateMechanic(mechanic.getId(), selectedUser.getId(), selectedUser.getUsername(), specialtiesStr);
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
                    // User-friendly error message instead of stack trace
                    if (e.getMessage().contains("already assigned as a mechanic")) {
                        showAlert(Alert.AlertType.WARNING, "Duplicate Mechanic", 
                                 "This user account is already assigned as a mechanic.\n\n" +
                                 "Each user can only be assigned as a mechanic once.\n" +
                                 "Please select a different user account.");
                    } else {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                    }
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
            
            TableColumn<ServiceOrder, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setCellFactory(col -> new TableCell<ServiceOrder, Void>() {
                private final Button viewBtn = new Button("View Details");
                {
                    viewBtn.setOnAction(e -> {
                        ServiceOrder order = getTableRow().getItem();
                        if (order != null) {
                            viewJobDetails(order);
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : viewBtn);
                }
            });
            
            jobTable.getColumns().addAll(idCol, dateCol, customerCol, vehicleCol, statusCol, descCol, actionsCol);
            
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
        statusComboBox.getItems().addAll("Available (Auto)", "Off Duty");
        
        // Show current status
        String currentStatus = mechanic.getAvailability();
        if ("Available".equals(currentStatus) || "Busy".equals(currentStatus) || "Overloaded".equals(currentStatus)) {
            statusComboBox.setValue("Available (Auto)");
        } else {
            statusComboBox.setValue(currentStatus);
        }
        
        Label noteLabel = new Label("Note: 'Busy' and 'Overloaded' statuses are automatically\ncalculated based on active job count.");
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(300);
        
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusComboBox, 1, 0);
        grid.add(noteLabel, 0, 1, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(selectedStatus -> {
            try {
                // Map "Available (Auto)" back to system-calculated status
                String newStatus;
                if ("Available (Auto)".equals(selectedStatus)) {
                    // Let the system calculate the actual status
                    newStatus = mechanicService.calculateAvailability(mechanic.getId());
                    // But update database to not be "Off Duty"
                    mechanicService.updateMechanicStatus(mechanic.getId(), "Available");
                } else {
                    newStatus = selectedStatus;
                    mechanicService.updateMechanicStatus(mechanic.getId(), newStatus);
                }
                
                mechanic.setAvailability(newStatus);
                mechanicTable.refresh();
                statusLabel.setText("Status updated successfully");
                
                // Check bookings when mechanic status changes
                try {
                    ServiceBookingService bookingService = new ServiceBookingService();
                    
                    if (!"Off Duty".equalsIgnoreCase(newStatus)) {
                        // Mechanic became available - check if delayed bookings can proceed
                        int updatedBookings = bookingService.checkAndUpdateDelayedBookingsForMechanic(mechanic.getId());
                        if (updatedBookings > 0) {
                            statusLabel.setText("Status updated. " + updatedBookings + 
                                              " delayed booking(s) updated to scheduled.");
                            showAlert(Alert.AlertType.INFORMATION, "Bookings Updated",
                                     updatedBookings + " delayed booking(s) have been automatically updated to scheduled " +
                                     "for mechanic " + mechanic.getName() + ".");
                        }
                    } else {
                        // Mechanic went off duty - check if scheduled bookings need to be delayed
                        int delayedBookings = bookingService.setBookingsToDelayedForMechanic(mechanic.getId());
                        if (delayedBookings > 0) {
                            statusLabel.setText("Status updated. " + delayedBookings + 
                                              " booking(s) set to delayed due to mechanic unavailability.");
                            showAlert(Alert.AlertType.WARNING, "Bookings Delayed",
                                     delayedBookings + " scheduled booking(s) have been automatically set to delayed " +
                                     "because mechanic " + mechanic.getName() + " is now " + newStatus + ".");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Could not check bookings: " + e.getMessage());
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
    
    private void viewJobDetails(ServiceOrder order) {
        try {
            // Get full booking details from database
            ServiceBookingService bookingService = new ServiceBookingService();
            ServiceBookingViewModel booking = bookingService.getBookingById(order.getId());
            
            if (booking == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not load booking details.");
                return;
            }
            
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Job Details");
            dialog.setHeaderText("Service Booking Details - " + booking.getHexId());
            
            // Create scrollable content
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(600);
            
            VBox mainContainer = new VBox(15);
            mainContainer.setPadding(new Insets(20));
            mainContainer.setStyle("-fx-background-color: white;");
            
            // === BOOKING INFORMATION SECTION ===
            VBox bookingSection = new VBox(10);
            bookingSection.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15; -fx-background-radius: 5;");
            Label bookingHeader = new Label("📋 BOOKING INFORMATION");
            bookingHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            bookingSection.getChildren().add(bookingHeader);
            
            GridPane bookingGrid = new GridPane();
            bookingGrid.setHgap(15);
            bookingGrid.setVgap(8);
            bookingGrid.setPadding(new Insets(10, 0, 0, 0));
            
            int row = 0;
            addDetailRow(bookingGrid, row++, "Booking ID:", booking.getHexId(), true);
            addDetailRow(bookingGrid, row++, "Service Type:", booking.getServiceType(), false);
            addDetailRow(bookingGrid, row++, "Date:", booking.getDate().toString(), false);
            addDetailRow(bookingGrid, row++, "Time:", booking.getTime(), false);
            addDetailRow(bookingGrid, row++, "Status:", booking.getStatus().toUpperCase(), false);
            
            bookingSection.getChildren().add(bookingGrid);
            mainContainer.getChildren().add(bookingSection);
            
            // === CUSTOMER & VEHICLE SECTION ===
            VBox customerSection = new VBox(10);
            customerSection.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 15; -fx-background-radius: 5;");
            Label customerHeader = new Label("👤 CUSTOMER & VEHICLE");
            customerHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0288D1;");
            customerSection.getChildren().add(customerHeader);
            
            GridPane customerGrid = new GridPane();
            customerGrid.setHgap(15);
            customerGrid.setVgap(8);
            customerGrid.setPadding(new Insets(10, 0, 0, 0));
            
            row = 0;
            addDetailRow(customerGrid, row++, "Customer Name:", booking.getCustomer().getName(), false);
            
            // Get full customer details
            try {
                Customer fullCustomer = CustomerService.getInstance().getCustomerById(booking.getCustomer().getId());
                if (fullCustomer != null) {
                    if (fullCustomer.getPhone() != null && !fullCustomer.getPhone().isEmpty()) {
                        addDetailRow(customerGrid, row++, "Phone:", fullCustomer.getPhone(), false);
                    }
                    if (fullCustomer.getEmail() != null && !fullCustomer.getEmail().isEmpty()) {
                        addDetailRow(customerGrid, row++, "Email:", fullCustomer.getEmail(), false);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Could not load customer details: " + e.getMessage());
            }
            
            addDetailRow(customerGrid, row++, "Vehicle:", booking.getVehicle().getModel(), false);
            
            customerSection.getChildren().add(customerGrid);
            mainContainer.getChildren().add(customerSection);
            
            // === MECHANIC SECTION ===
            VBox mechanicSection = new VBox(10);
            mechanicSection.setStyle("-fx-background-color: #fff3e0; -fx-padding: 15; -fx-background-radius: 5;");
            Label mechanicHeader = new Label("🔧 ASSIGNED MECHANIC");
            mechanicHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E65100;");
            mechanicSection.getChildren().add(mechanicHeader);
            
            GridPane mechanicGrid = new GridPane();
            mechanicGrid.setHgap(15);
            mechanicGrid.setVgap(8);
            mechanicGrid.setPadding(new Insets(10, 0, 0, 0));
            
            row = 0;
            addDetailRow(mechanicGrid, row++, "Name:", booking.getMechanic().getName(), false);
            if (booking.getMechanic().getSpecialties() != null && !booking.getMechanic().getSpecialties().isEmpty()) {
                String specialties = String.join(", ", booking.getMechanic().getSpecialties());
                addDetailRow(mechanicGrid, row++, "Specialties:", specialties, false);
            }
            
            mechanicSection.getChildren().add(mechanicGrid);
            mainContainer.getChildren().add(mechanicSection);
            
            // === SERVICE DESCRIPTION SECTION ===
            if (booking.getServiceDescription() != null && !booking.getServiceDescription().trim().isEmpty()) {
                VBox descSection = new VBox(10);
                descSection.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 15; -fx-background-radius: 5;");
                Label descHeader = new Label("📝 SERVICE DESCRIPTION");
                descHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6A1B9A;");
                descSection.getChildren().add(descHeader);
                
                TextArea descArea = new TextArea(booking.getServiceDescription());
                descArea.setEditable(false);
                descArea.setWrapText(true);
                descArea.setPrefRowCount(4);
                descArea.setStyle("-fx-control-inner-background: white;");
                descSection.getChildren().add(descArea);
                
                mainContainer.getChildren().add(descSection);
            }
            
            // === PARTS & MATERIALS SECTION ===
            try {
                List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                if (!parts.isEmpty()) {
                    VBox partsSection = new VBox(10);
                    partsSection.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-background-radius: 5;");
                    Label partsHeader = new Label("🔩 PARTS & MATERIALS USED");
                    partsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
                    partsSection.getChildren().add(partsHeader);
                    
                    // Create parts table
                    TableView<BookingPart> partsTable = new TableView<>();
                    partsTable.setPrefHeight(200);
                    partsTable.setItems(FXCollections.observableArrayList(parts));
                    partsTable.setStyle("-fx-background-color: white;");
                    
                    TableColumn<BookingPart, String> nameCol = new TableColumn<>("Part Name");
                    nameCol.setCellValueFactory(new PropertyValueFactory<>("partName"));
                    nameCol.setPrefWidth(300);
                    
                    TableColumn<BookingPart, Integer> qtyCol = new TableColumn<>("Quantity");
                    qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
                    qtyCol.setPrefWidth(100);
                    qtyCol.setStyle("-fx-alignment: CENTER;");
                    
                    TableColumn<BookingPart, Double> priceCol = new TableColumn<>("Unit Price");
                    priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
                    priceCol.setPrefWidth(120);
                    priceCol.setCellFactory(col -> new TableCell<BookingPart, Double>() {
                        @Override
                        protected void updateItem(Double price, boolean empty) {
                            super.updateItem(price, empty);
                            if (empty || price == null) {
                                setText(null);
                            } else {
                                setText("₱" + String.format("%.2f", price));
                                setStyle("-fx-alignment: CENTER-RIGHT;");
                            }
                        }
                    });
                    
                    TableColumn<BookingPart, Double> totalCol = new TableColumn<>("Subtotal");
                    totalCol.setCellValueFactory(cellData -> 
                        new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotalCost()).asObject());
                    totalCol.setPrefWidth(120);
                    totalCol.setCellFactory(col -> new TableCell<BookingPart, Double>() {
                        @Override
                        protected void updateItem(Double total, boolean empty) {
                            super.updateItem(total, empty);
                            if (empty || total == null) {
                                setText(null);
                            } else {
                                setText("₱" + String.format("%.2f", total));
                                setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
                            }
                        }
                    });
                    
                    partsTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
                    partsSection.getChildren().add(partsTable);
                    
                    // Calculate total parts cost
                    double totalPartsCost = parts.stream().mapToDouble(BookingPart::getTotalCost).sum();
                    Label totalPartsLabel = new Label("Total Parts Cost: ₱" + String.format("%.2f", totalPartsCost));
                    totalPartsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-padding: 10 0 0 0;");
                    partsSection.getChildren().add(totalPartsLabel);
                    
                    mainContainer.getChildren().add(partsSection);
                } else {
                    VBox noPartsSection = new VBox(10);
                    noPartsSection.setStyle("-fx-background-color: #fafafa; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5;");
                    Label noPartsLabel = new Label("ℹ️  No parts or materials used for this service");
                    noPartsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
                    noPartsSection.getChildren().add(noPartsLabel);
                    mainContainer.getChildren().add(noPartsSection);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // === BILLING SECTION (if completed) ===
            if ("completed".equalsIgnoreCase(booking.getStatus())) {
                try {
                    BillingService billingService = BillingService.getInstance();
                    Bill bill = billingService.getBillByServiceId(booking.getId());
                    
                    if (bill != null) {
                        VBox billingSection = new VBox(10);
                        billingSection.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 5;");
                        Label billingHeader = new Label("💰 BILLING INFORMATION");
                        billingHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
                        billingSection.getChildren().add(billingHeader);
                        
                        GridPane billingGrid = new GridPane();
                        billingGrid.setHgap(15);
                        billingGrid.setVgap(8);
                        billingGrid.setPadding(new Insets(10, 0, 0, 0));
                        
                        row = 0;
                        addDetailRow(billingGrid, row++, "Bill ID:", bill.getHexId(), true);
                        addDetailRow(billingGrid, row++, "Bill Date:", bill.getBillDate().toString(), false);
                        addDetailRow(billingGrid, row++, "Payment Status:", bill.getPaymentStatus().toUpperCase(), false);
                        addDetailRow(billingGrid, row++, "Total Amount:", "₱" + String.format("%.2f", bill.getAmount()), false);
                        
                        billingSection.getChildren().add(billingGrid);
                        
                        Label totalAmountLabel = new Label("TOTAL AMOUNT DUE: ₱" + String.format("%.2f", bill.getAmount()));
                        totalAmountLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1B5E20; -fx-padding: 10 0 0 0;");
                        billingSection.getChildren().add(totalAmountLabel);
                        
                        mainContainer.getChildren().add(billingSection);
                    }
                } catch (SQLException e) {
                    System.err.println("Could not load billing information: " + e.getMessage());
                }
            }
            
            scrollPane.setContent(mainContainer);
            
            // Add close button
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().setMinWidth(750);
            dialog.getDialogPane().setMinHeight(650);
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load job details: " + e.getMessage());
        }
    }
    
    // Helper method to add detail rows consistently
    private void addDetailRow(GridPane grid, int row, String label, String value, boolean bold) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");
        
        Label valueNode = new Label(value != null ? value : "N/A");
        if (bold) {
            valueNode.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        }
        
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
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