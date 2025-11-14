package com.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.util.StringConverter;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceBookingController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private DatePicker filterDatePicker;
    @FXML private TableView<ServiceBookingViewModel> bookingTable;
    @FXML private TableColumn<ServiceBookingViewModel, Boolean> selectColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> idColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> dateColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> timeColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> customerNameColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> vehicleColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> mechanicColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> serviceTypeColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> statusColumn;
    @FXML private TableColumn<ServiceBookingViewModel, Void> actionsColumn;
    @FXML private Button deleteSelectedButton;
    @FXML private Label statusLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private TabPane bookingTabPane;

    private ObservableList<ServiceBookingViewModel> bookingList;
    private final ServiceBookingService bookingService;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final MechanicService mechanicService;
    
    // Booking dialog components
    private Dialog<Boolean> bookingDialog;
    private ComboBox<Customer> customerComboBox;
    private ComboBox<Vehicle> vehicleComboBox;
    private ComboBox<Mechanic> mechanicComboBox;
    private DatePicker bookingDatePicker;
    private ComboBox<String> timeComboBox;
    private ComboBox<String> serviceTypeComboBox;
    private TextArea serviceDescriptionArea;
    
    // Parts selection components
    private TableView<BookingPart> selectedPartsTable;
    private ObservableList<BookingPart> selectedPartsList;
    private ComboBox<InventoryItem> availablePartsComboBox;
    private Spinner<Integer> partQuantitySpinner;
    
    // Track booking being edited
    private ServiceBookingViewModel currentEditingBooking;
    
    public ServiceBookingController() {
        bookingList = FXCollections.observableArrayList();
        bookingService = new ServiceBookingService();
        customerService = CustomerService.getInstance();
        vehicleService = VehicleService.getInstance();
        mechanicService = new MechanicService();
    }
    
    // Service booking methods
    private boolean updateTable() {
        try {
            // Load bookings
            List<ServiceBookingViewModel> bookings = bookingService.getAllBookings();
            bookingList.setAll(bookings);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bookings: " + e.getMessage());
            return false;
        }
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getHexId()));
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate().toString()));
        timeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTime()));
        customerNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCustomer() != null ? cellData.getValue().getCustomer().getName() : ""
        ));
        vehicleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getVehicle() != null ? cellData.getValue().getVehicle().getBrand() : ""
        ));
        mechanicColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getMechanic() != null ? cellData.getValue().getMechanic().getName() : ""
        ));
        serviceTypeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getServiceType()));
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
    }
    
    @FXML
    private String convertToDisplayStatus(String dbStatus) {
        if (dbStatus == null) return "";
        switch (dbStatus.toLowerCase()) {
            case "scheduled": return "Scheduled";
            case "delayed": return "Delayed";
            case "in_progress": return "In Progress";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            default: return dbStatus;
        }
    }

    private String convertToDatabaseStatus(String displayStatus) {
        if (displayStatus == null) return "";
        switch (displayStatus) {
            case "Scheduled": return "scheduled";
            case "Delayed": return "delayed";
            case "In Progress": return "in_progress";
            case "Completed": return "completed";
            case "Cancelled": return "cancelled";
            default: return displayStatus.toLowerCase();
        }
    }

    public void initialize() {
        // Initialize the booking controller
        bookingList = FXCollections.observableArrayList();
        
        // Setup filter components
        filterStatusComboBox.getItems().addAll("Active", "Scheduled", "Delayed", "In Progress", "Completed");
        filterStatusComboBox.setValue("Active");
        
        // Configure table columns
        // Add checkbox column for multi-select
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(col -> new TableCell<ServiceBookingViewModel, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        booking.setSelected(isSelected);
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        checkBox.setSelected(booking.isSelected());
                    }
                    setGraphic(checkBox);
                }
            }
        });
        
        idColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
        mechanicColumn.setCellValueFactory(new PropertyValueFactory<>("mechanicName"));
        serviceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style the status column with colors
        statusColumn.setCellFactory(column -> new TableCell<ServiceBookingViewModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String displayStatus = convertToDisplayStatus(item);
                    setText(displayStatus);
                    
                    switch(displayStatus) {
                        case "Scheduled":
                            setStyle("-fx-text-fill: #1a75ff;"); // Blue
                            break;
                        case "Delayed":
                            setStyle("-fx-text-fill: #ff6600; -fx-font-weight: bold;"); // Bold Orange
                            break;
                        case "In Progress":
                            setStyle("-fx-text-fill: #ff8c1a;"); // Light Orange
                            break;
                        case "Completed":
                            setStyle("-fx-text-fill: #2eb82e;"); // Green
                            break;
                        case "Cancelled":
                            setStyle("-fx-text-fill: #cc0000;"); // Red
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Set up actions column with buttons
        setupActionsColumn();
        
        // Set table data
        bookingTable.setItems(bookingList);
        
        // Setup cancelled bookings tab
        setupCancelledBookingsTab();
        
        // Add tab change listener to load appropriate data
        bookingTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getText().equals("Cancelled Bookings")) {
                loadCancelledBookings();
            } else {
                // Active bookings tab
                handleRefreshBookings();
            }
        });
        
        // Load initial data
        loadBookings();
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<ServiceBookingViewModel, Void>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button statusButton = new Button("Status");
            private final HBox buttonBox = new HBox(5);
            
            {
                buttonBox.getChildren().addAll(viewButton, editButton, statusButton);
                buttonBox.setAlignment(Pos.CENTER);
                
                viewButton.setOnAction(e -> {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        viewBookingDetails(booking);
                    }
                });
                
                editButton.setOnAction(e -> {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        editBooking(booking);
                    }
                });
                
                statusButton.setOnAction(e -> {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        updateBookingStatus(booking);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });
    }
    
    @FXML
    private void handleSearchBookings() {
        String searchTerm = searchField.getText().trim();
        String statusFilter = filterStatusComboBox.getValue();
        LocalDate dateFilter = filterDatePicker.getValue();
        
        loadBookingsFiltered(searchTerm, statusFilter, dateFilter);
    }
    
    @FXML
    private void handleRefreshBookings() {
        searchField.clear();
        filterStatusComboBox.setValue("Active");
        filterDatePicker.setValue(null);
        loadBookings();
    }
    
    @FXML
    private void handleAddBooking() {
        showBookingDialog(null);
    }
    
    private void loadBookings() {
        // Load bookings with "Active" filter (excludes cancelled)
        loadBookingsFiltered("", "Active", null);
    }
    
    private void loadBookingsFiltered(String searchTerm, String statusFilter, LocalDate dateFilter) {
        try {
            List<ServiceBookingViewModel> bookings;
            
            // Get the current logged-in user
            User currentUser = UserService.getInstance().getCurrentUser();
            
            // If user is a mechanic, only show their bookings
            if (currentUser != null && currentUser.getRole() == User.UserRole.MECHANIC) {
                // Get the mechanic ID for this user
                Mechanic mechanic = mechanicService.getMechanicByUserId(currentUser.getId());
                if (mechanic != null) {
                    bookings = bookingService.searchBookingsForMechanic(mechanic.getId(), searchTerm, statusFilter, dateFilter);
                } else {
                    // User is a mechanic but doesn't have a mechanic record yet
                    bookings = new ArrayList<>();
                }
            } else {
                // Admin or other roles see all bookings
                bookings = bookingService.searchBookings(searchTerm, statusFilter, dateFilter);
            }
            
            bookingList.clear();
            bookingList.addAll(bookings);
            updateTotalBookingsLabel();
            statusLabel.setText("Search complete");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Search Error", 
                     "Error searching bookings: " + e.getMessage());
        }
    }
    
    private void updateTotalBookingsLabel() {
        totalBookingsLabel.setText("Total bookings: " + bookingList.size());
    }
    
    /**
     * Setup the cancelled bookings tab with its own table
     */
    private void setupCancelledBookingsTab() {
        // Create cancelled bookings tab
        Tab cancelledTab = new Tab("Cancelled Bookings");
        cancelledTab.setClosable(false);
        
        // Create table for cancelled bookings
        TableView<ServiceBookingViewModel> cancelledTable = new TableView<>();
        
        // Setup columns (same as main table but without select column)
        TableColumn<ServiceBookingViewModel, String> cIdCol = new TableColumn<>("Booking ID");
        cIdCol.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        cIdCol.setPrefWidth(120);
        
        TableColumn<ServiceBookingViewModel, String> cDateCol = new TableColumn<>("Date");
        cDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        cDateCol.setPrefWidth(100);
        
        TableColumn<ServiceBookingViewModel, String> cTimeCol = new TableColumn<>("Time");
        cTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        cTimeCol.setPrefWidth(80);
        
        TableColumn<ServiceBookingViewModel, String> cCustomerCol = new TableColumn<>("Customer");
        cCustomerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        cCustomerCol.setPrefWidth(150);
        
        TableColumn<ServiceBookingViewModel, String> cVehicleCol = new TableColumn<>("Vehicle");
        cVehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
        cVehicleCol.setPrefWidth(150);
        
        TableColumn<ServiceBookingViewModel, String> cMechanicCol = new TableColumn<>("Mechanic");
        cMechanicCol.setCellValueFactory(new PropertyValueFactory<>("mechanicName"));
        cMechanicCol.setPrefWidth(120);
        
        TableColumn<ServiceBookingViewModel, String> cServiceCol = new TableColumn<>("Service Type");
        cServiceCol.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        cServiceCol.setPrefWidth(120);
        
        TableColumn<ServiceBookingViewModel, String> cStatusCol = new TableColumn<>("Status");
        cStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStatusCol.setPrefWidth(100);
        cStatusCol.setCellFactory(column -> new TableCell<ServiceBookingViewModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #888888;"); // Gray for cancelled
                }
            }
        });
        
        TableColumn<ServiceBookingViewModel, Void> cActionsCol = new TableColumn<>("Actions");
        cActionsCol.setPrefWidth(120);
        cActionsCol.setCellFactory(column -> new TableCell<ServiceBookingViewModel, Void>() {
            private final Button viewButton = new Button("View");
            
            {
                viewButton.setOnAction(e -> {
                    ServiceBookingViewModel booking = getTableRow().getItem();
                    if (booking != null) {
                        viewBookingDetails(booking);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
        
        cancelledTable.getColumns().addAll(cIdCol, cDateCol, cTimeCol, cCustomerCol, 
                                          cVehicleCol, cMechanicCol, cServiceCol, cStatusCol, cActionsCol);
        
        cancelledTab.setContent(cancelledTable);
        bookingTabPane.getTabs().add(cancelledTab);
    }
    
    /**
     * Load cancelled bookings into the cancelled tab
     */
    private void loadCancelledBookings() {
        try {
            List<ServiceBookingViewModel> cancelledBookings;
            
            // Get the current logged-in user
            User currentUser = UserService.getInstance().getCurrentUser();
            
            // If user is a mechanic, only show their cancelled bookings
            if (currentUser != null && currentUser.getRole() == User.UserRole.MECHANIC) {
                Mechanic mechanic = mechanicService.getMechanicByUserId(currentUser.getId());
                if (mechanic != null) {
                    cancelledBookings = bookingService.getCancelledBookingsForMechanic(mechanic.getId());
                } else {
                    cancelledBookings = new ArrayList<>();
                }
            } else {
                // Admin sees all cancelled bookings
                cancelledBookings = bookingService.getCancelledBookings();
            }
            
            // Get the cancelled tab and its table
            Tab cancelledTab = bookingTabPane.getTabs().stream()
                    .filter(tab -> tab.getText().equals("Cancelled Bookings"))
                    .findFirst()
                    .orElse(null);
            
            if (cancelledTab != null && cancelledTab.getContent() instanceof TableView) {
                @SuppressWarnings("unchecked")
                TableView<ServiceBookingViewModel> cancelledTable = 
                        (TableView<ServiceBookingViewModel>) cancelledTab.getContent();
                
                ObservableList<ServiceBookingViewModel> cancelledList = 
                        FXCollections.observableArrayList(cancelledBookings);
                cancelledTable.setItems(cancelledList);
                
                statusLabel.setText("Loaded " + cancelledBookings.size() + " cancelled booking(s)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Load Error", 
                     "Error loading cancelled bookings: " + e.getMessage());
        }
    }
    
    private void viewBookingDetails(ServiceBookingViewModel booking) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Booking Details");
            dialog.setHeaderText("Booking " + booking.getHexId() + " Details");
            
            // Create the content layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            // Add booking details
            int row = 0;
            grid.add(new Label("Booking ID:"), 0, row);
            grid.add(new Label(booking.getHexId()), 1, row++);
            
            grid.add(new Label("Date:"), 0, row);
            grid.add(new Label(booking.getDate().toString()), 1, row++);
            
            grid.add(new Label("Time:"), 0, row);
            grid.add(new Label(booking.getTime()), 1, row++);
            
            grid.add(new Label("Customer:"), 0, row);
            grid.add(new Label(booking.getCustomer().getName()), 1, row++);
            
            grid.add(new Label("Vehicle:"), 0, row);
            grid.add(new Label(booking.getVehicle().getModel()), 1, row++);
            
            grid.add(new Label("Mechanic:"), 0, row);
            grid.add(new Label(booking.getMechanic().getName()), 1, row++);
            
            grid.add(new Label("Service Type:"), 0, row);
            grid.add(new Label(booking.getServiceType()), 1, row++);
            
            grid.add(new Label("Description:"), 0, row);
            TextArea descArea = new TextArea(booking.getServiceDescription());
            descArea.setEditable(false);
            descArea.setPrefRowCount(4);
            descArea.setPrefColumnCount(30);
            grid.add(descArea, 1, row++);
            
            grid.add(new Label("Status:"), 0, row);
            grid.add(new Label(booking.getStatus()), 1, row++);
            
            // Add Parts section
            try {
                List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                if (!parts.isEmpty()) {
                    grid.add(new Label("Parts Used:"), 0, row++);
                    
                    // Create parts table
                    TableView<BookingPart> partsTable = new TableView<>();
                    partsTable.setPrefHeight(150);
                    partsTable.setItems(FXCollections.observableArrayList(parts));
                    
                    TableColumn<BookingPart, String> nameCol = new TableColumn<>("Part Name");
                    nameCol.setCellValueFactory(new PropertyValueFactory<>("partName"));
                    nameCol.setPrefWidth(200);
                    
                    TableColumn<BookingPart, Integer> qtyCol = new TableColumn<>("Quantity");
                    qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
                    qtyCol.setPrefWidth(80);
                    
                    TableColumn<BookingPart, Double> priceCol = new TableColumn<>("Price");
                    priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
                    priceCol.setPrefWidth(80);
                    
                    TableColumn<BookingPart, Double> totalCol = new TableColumn<>("Total");
                    totalCol.setCellValueFactory(cellData -> 
                        new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotalCost()).asObject());
                    totalCol.setPrefWidth(80);
                    
                    partsTable.getColumns().add(nameCol);
                    partsTable.getColumns().add(qtyCol);
                    partsTable.getColumns().add(priceCol);
                    partsTable.getColumns().add(totalCol);
                    
                    grid.add(partsTable, 0, row++, 2, 1);
                    
                    // Calculate total parts cost
                    double totalCost = parts.stream().mapToDouble(BookingPart::getTotalCost).sum();
                    Label totalLabel = new Label("Total Parts Cost: ₱" + String.format("%.2f", totalCost));
                    totalLabel.setStyle("-fx-font-weight: bold;");
                    grid.add(totalLabel, 1, row++);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Add close button
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setMinWidth(600);
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load booking details: " + e.getMessage());
        }
    }
    
    private void editBooking(ServiceBookingViewModel booking) {
        showBookingDialog(booking);
    }
    
    private void updateBookingStatus(ServiceBookingViewModel booking) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Booking Status");
        dialog.setHeaderText("Update status for booking " + booking.getHexId());
        
        // Set button types
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        // Create layout with better sizing
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setMinWidth(450); // Make dialog wider
        
        // Add status combo box
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Scheduled", "Delayed", "In Progress", "Completed", "Cancelled");
        statusComboBox.setPrefWidth(300); // Make combo box wider
        // Convert database status to display format
        String displayStatus = convertToDisplayStatus(booking.getStatus());
        statusComboBox.setValue(displayStatus);
        
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusComboBox, 1, 0);
        
        // Add a label to explain billing
        Label billingLabel = new Label("");
        billingLabel.setWrapText(true);
        billingLabel.setPrefWidth(400); // Make label wider to fit text
        billingLabel.setMinHeight(50); // Make label taller
        grid.add(billingLabel, 0, 1, 2, 1);
        
        // Update billing label when status changes
        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Completed".equals(newVal)) {
                billingLabel.setText("Note: Marking the service as completed will automatically generate a bill.");
                billingLabel.setStyle("-fx-text-fill: #2eb82e;"); // Green color
            } else {
                billingLabel.setText("");
            }
        });
        
        // Set initial text if current status is completed
        if ("Completed".equals(displayStatus)) {
            billingLabel.setText("Note: Marking the service as completed will automatically generate a bill.");
            billingLabel.setStyle("-fx-text-fill: #2eb82e;");
        }
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500); // Set dialog width
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                String dbStatus = convertToDatabaseStatus(newStatus);
                
                // Require CAPTCHA verification for Cancelled or Completed status
                if ("cancelled".equals(dbStatus) || "completed".equals(dbStatus)) {
                    String action = "cancelled".equals(dbStatus) ? "cancel" : "complete";
                    if (!showCaptchaVerification(action, booking.getHexId())) {
                        return; // User failed verification or cancelled
                    }
                }
                
                // Update the booking status FIRST
                boolean success = bookingService.updateBookingStatus(booking.getId(), dbStatus);
                if (!success) {
                    showAlert(Alert.AlertType.ERROR, "Update Failed", "Failed to update booking status");
                    return;
                }
                
                // Update the local booking object
                booking.setStatus(dbStatus);
                
                // If the status is being changed to "cancelled", release reserved parts
                if ("cancelled".equals(dbStatus)) {
                    try {
                        bookingService.releaseBookingParts(booking.getId());
                        System.out.println("Released reserved parts for cancelled booking #" + booking.getId());
                        
                        // Check if any delayed bookings can now proceed
                        int updatedBookings = bookingService.checkAndUpdateDelayedBookings();
                        if (updatedBookings > 0) {
                            System.out.println("Auto-updated " + updatedBookings + " delayed booking(s) to scheduled");
                            
                            // Show notification about updated bookings
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Bookings Updated",
                                         "Booking cancelled successfully.\n\n" +
                                         "✓ " + updatedBookings + " delayed booking(s) have been automatically updated to scheduled " +
                                         "because parts are now available.");
                            });
                            bookingTable.refresh();
                            statusLabel.setText("Booking status updated successfully");
                            loadBookings();
                            return; // Skip the regular cancelled message since we showed a custom one
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.WARNING, "Parts Release Warning", 
                                 "Booking cancelled but could not release reserved parts: " + e.getMessage());
                    }
                }
                
                // Continue with normal flow
                bookingTable.refresh();
                statusLabel.setText("Booking status updated successfully");
                
                // Reload bookings to show any auto-updated statuses
                loadBookings();
                
                // If the status is changed to "cancelled", show confirmation
                if ("cancelled".equals(dbStatus)) {
                    showAlert(Alert.AlertType.INFORMATION, 
                             "Booking Cancelled", 
                             "Booking has been cancelled and reserved parts have been released back to inventory.");
                }
                    
                // If the status is changed to "completed", create a bill
                if ("completed".equals(dbStatus)) {
                    BillingService billingService = BillingService.getInstance();
                    boolean billCreated = billingService.createBillFromService(booking.getId());
                    
                    if (billCreated) {
                        showAlert(Alert.AlertType.INFORMATION, 
                                 "Bill Created", 
                                 "A bill has been automatically generated for this service.\n" +
                                 "You can view and manage it in the Billing section.");
                    } else {
                        showAlert(Alert.AlertType.WARNING,
                                 "Billing Notice",
                                 "Could not create bill automatically. Please create it manually in the Billing section.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update status: " + e.getMessage());
            }
        });
    }
    
    private void setupCustomerSelection(GridPane grid, int row) {
        Label customerLabel = new Label("Customer:");
        customerLabel.getStyleClass().add("form-label");
        customerComboBox = new ComboBox<>();
        customerComboBox.setMaxWidth(Double.MAX_VALUE);
        customerComboBox.setPromptText("Select a customer");
        
        try {
            List<Customer> customers = customerService.getAllCustomers();
            customerComboBox.setItems(FXCollections.observableArrayList(customers));
            customerComboBox.setConverter(new StringConverter<Customer>() {
                @Override
                public String toString(Customer customer) {
                    return customer == null ? "" : customer.getName() + " (" + customer.getPhone() + ")";
                }
                
                @Override
                public Customer fromString(String string) {
                    return null; // Not needed for ComboBox
                }
            });
            
            grid.add(customerLabel, 0, row);
            grid.add(customerComboBox, 1, row, 2, 1);
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load customers: " + e.getMessage());
        }
    }
    
    private void setupVehicleSelection(GridPane grid, int row) {
        Label vehicleLabel = new Label("Vehicle:");
        vehicleLabel.getStyleClass().add("form-label");
        vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setMaxWidth(Double.MAX_VALUE);
        vehicleComboBox.setPromptText("Select a vehicle");
        vehicleComboBox.setDisable(true);
        
        // Vehicle converter
        vehicleComboBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                if (vehicle == null) return "";
                return String.format("%s %s (%s)", 
                    vehicle.getBrand(), 
                    vehicle.getModel(), 
                    vehicle.getPlateNumber());
            }
            
            @Override
            public Vehicle fromString(String string) {
                return null;
            }
        });
        
        // Update vehicles when customer changes
        customerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    List<Vehicle> vehicles = vehicleService.getCustomerVehicles(newVal.getId());
                    vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
                    vehicleComboBox.setDisable(false);
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             "Could not load vehicles: " + ex.getMessage());
                    vehicleComboBox.setDisable(true);
                }
            } else {
                vehicleComboBox.getItems().clear();
                vehicleComboBox.setDisable(true);
            }
        });
        
        grid.add(vehicleLabel, 0, row);
        grid.add(vehicleComboBox, 1, row, 2, 1);
    }
    
    private void setupMechanicSelection(GridPane grid, int row) {
        Label mechanicLabel = new Label("Mechanic:");
        mechanicLabel.getStyleClass().add("form-label");
        mechanicComboBox = new ComboBox<>();
        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        mechanicComboBox.setPromptText("Select a mechanic");
        
        try {
            List<Mechanic> allMechanics = mechanicService.getAllMechanics();
            // CHANGED: Now showing ALL mechanics including "Off Duty"
            // Booking will be automatically set to "Delayed" if Off Duty mechanic is selected
            
            mechanicComboBox.setItems(FXCollections.observableArrayList(allMechanics));
            mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
                @Override
                public String toString(Mechanic mechanic) {
                    if (mechanic == null) return "";
                    String availability = mechanic.getAvailability();
                    String availText = (availability != null && !"Available".equalsIgnoreCase(availability)) ? 
                                      " [" + availability + "]" : "";
                    return mechanic.getName() + 
                           (mechanic.getSpecialty() != null && !mechanic.getSpecialty().isEmpty() ? 
                           " (" + mechanic.getSpecialty() + ")" : "") + availText;
                }
                
                @Override
                public Mechanic fromString(String string) {
                    return null;
                }
            });
            
            grid.add(mechanicLabel, 0, row);
            grid.add(mechanicComboBox, 1, row, 2, 1);
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load mechanics: " + e.getMessage());
        }
    }
    
    private void setupDateTimeSelectors(GridPane grid, int row) {
        // Add a section label
        Label sectionLabel = new Label("Date and Time Selection");
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(sectionLabel, 0, row, 3, 1);
        
        // Date selection
        Label dateLabel = new Label("Date:");
        dateLabel.getStyleClass().add("form-label");
        bookingDatePicker = new DatePicker(LocalDate.now());
        bookingDatePicker.setMaxWidth(Double.MAX_VALUE);
        bookingDatePicker.setEditable(false);
        
        // Add date components with spacing
        grid.add(dateLabel, 0, row + 1);
        grid.add(bookingDatePicker, 1, row + 1, 2, 1);
        
        // Time selection in a separate clearly defined section
        Label timeLabel = new Label("Time:");
        timeLabel.getStyleClass().add("form-label");
        
        // Initialize time ComboBox with clear styling
        timeComboBox = new ComboBox<>();
        timeComboBox.setMaxWidth(Double.MAX_VALUE);
        timeComboBox.setStyle("-fx-pref-width: 200px;"); // Explicit width
        timeComboBox.setPromptText("Select time");
        
        // Add time slots (08:00 to 17:00) using 24-hour format
        ObservableList<String> timeSlots = FXCollections.observableArrayList();
        for (int hour = 8; hour <= 17; hour++) {
            timeSlots.add(String.format("%02d:00", hour));
            if (hour < 17) {
                timeSlots.add(String.format("%02d:30", hour));
            }
        }
        timeComboBox.setItems(timeSlots);
        timeComboBox.setValue("09:00");
        
        // Ensure the time selection is visible and properly spaced
        timeComboBox.setVisible(true);
        timeComboBox.setManaged(true);
        
        // Add time components with clear spacing
        grid.add(timeLabel, 0, row + 2);
        grid.add(timeComboBox, 1, row + 2, 2, 1);
        
        // Add extra spacing after the date/time section
        Label spacer = new Label();
        spacer.setMinHeight(20); // 20 pixels of space
        grid.add(spacer, 0, row + 3, 3, 1);
    }
    
    private void setupServiceTypeAndDescription(GridPane grid, int row) {
        Label typeLabel = new Label("Service Type:");
        typeLabel.getStyleClass().add("form-label");
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("form-label");
        
        serviceTypeComboBox = new ComboBox<>();
        serviceTypeComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceTypeComboBox.setPromptText("Select service type");
        serviceTypeComboBox.getItems().addAll(
            "Regular Maintenance", 
            "Oil Change", 
            "Tire Service", 
            "Brake Service",
            "Engine Repair", 
            "Transmission Service", 
            "Electrical System",
            "A/C Service",
            "Other"
        );
        
        serviceDescriptionArea = new TextArea();
        serviceDescriptionArea.setPrefRowCount(4); // Increased row count
        serviceDescriptionArea.setPrefWidth(400); // Set preferred width
        serviceDescriptionArea.setMinWidth(300); // Set minimum width
        serviceDescriptionArea.setMaxWidth(Double.MAX_VALUE); // Allow growing
        serviceDescriptionArea.setWrapText(true);
        serviceDescriptionArea.setPromptText("Enter service details");
        
        grid.add(typeLabel, 0, row);
        grid.add(serviceTypeComboBox, 1, row, 2, 1);
        grid.add(descLabel, 0, row + 1);
        grid.add(serviceDescriptionArea, 1, row + 1, 2, 1);
    }
    
    private void setupPartsSelection(GridPane grid, int row) {
        // Section label
        Label sectionLabel = new Label("Parts Required");
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(sectionLabel, 0, row, 3, 1);
        
        // Initialize the parts list
        selectedPartsList = FXCollections.observableArrayList();
        
        // Add parts section
        HBox addPartsBox = new HBox(10);
        addPartsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Available parts combo box
        availablePartsComboBox = new ComboBox<>();
        availablePartsComboBox.setPromptText("Select a part");
        availablePartsComboBox.setPrefWidth(300);
        
        try {
            InventoryService inventoryService = InventoryService.getInstance();
            List<InventoryItem> allParts = inventoryService.getAllItems();
            availablePartsComboBox.setItems(FXCollections.observableArrayList(allParts));
            availablePartsComboBox.setConverter(new javafx.util.StringConverter<InventoryItem>() {
                @Override
                public String toString(InventoryItem item) {
                    if (item == null) return "";
                    return item.getName() + " - ₱" + String.format("%.2f", item.getSellingPrice()) + 
                           " (Stock: " + item.getQuantity() + 
                           ", Reserved: " + item.getReservedQuantity() + 
                           ", Available: " + item.getAvailableQuantity() + ")";
                }
                
                @Override
                public InventoryItem fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load parts: " + e.getMessage());
        }
        
        // Quantity spinner - allow any quantity (no max limit)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        partQuantitySpinner = new Spinner<>(valueFactory);
        partQuantitySpinner.setPrefWidth(80);
        partQuantitySpinner.setEditable(true);
        
        // Add button
        Button addPartButton = new Button("Add Part");
        addPartButton.setOnAction(e -> handleAddPart());
        
        addPartsBox.getChildren().addAll(
            new Label("Part:"), availablePartsComboBox,
            new Label("Qty:"), partQuantitySpinner,
            addPartButton
        );
        
        grid.add(addPartsBox, 0, row + 1, 3, 1);
        
        // Selected parts table
        selectedPartsTable = new TableView<>();
        selectedPartsTable.setItems(selectedPartsList);
        selectedPartsTable.setPrefHeight(200);
        selectedPartsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Part Name Column
        TableColumn<BookingPart, String> partNameCol = new TableColumn<>("Part Name");
        partNameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPartName()));
        partNameCol.setPrefWidth(250);
        
        // Quantity Column
        TableColumn<BookingPart, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getQuantity()));
        quantityCol.setPrefWidth(80);
        
        // Price Column
        TableColumn<BookingPart, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("₱%.2f", cellData.getValue().getPrice())));
        priceCol.setPrefWidth(100);
        
        // Total Column
        TableColumn<BookingPart, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("₱%.2f", cellData.getValue().getTotalCost())));
        totalCol.setPrefWidth(100);
        
        // Actions Column
        TableColumn<BookingPart, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(column -> new TableCell<BookingPart, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Remove");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setOnAction(e -> {
                    BookingPart part = getTableRow().getItem();
                    if (part != null) {
                        handleEditPart(part);
                    }
                });
                
                deleteButton.setOnAction(e -> {
                    BookingPart part = getTableRow().getItem();
                    if (part != null) {
                        selectedPartsList.remove(part);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actionsCol.setPrefWidth(150);
        
        selectedPartsTable.getColumns().addAll(partNameCol, quantityCol, priceCol, totalCol, actionsCol);
        
        grid.add(new Label("Selected Parts:"), 0, row + 2);
        grid.add(selectedPartsTable, 0, row + 3, 3, 1);
        
        // Total cost label
        Label totalLabel = new Label("Total Parts Cost: ₱0.00");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(totalLabel, 0, row + 4, 3, 1);
        
        // Update total when list changes
        selectedPartsList.addListener((javafx.collections.ListChangeListener<BookingPart>) c -> {
            double total = selectedPartsList.stream()
                .mapToDouble(BookingPart::getTotalCost)
                .sum();
            totalLabel.setText(String.format("Total Parts Cost: ₱%.2f", total));
        });
    }
    
    private void handleAddPart() {
        InventoryItem selectedItem = availablePartsComboBox.getValue();
        int quantity = partQuantitySpinner.getValue();
        
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Part Selected", "Please select a part from the list.");
            return;
        }
        
        if (quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Quantity", "Please enter a valid quantity.");
            return;
        }
        
        // REMOVED: Stock validation - now allows over-booking
        // System will automatically set booking to "Delayed" if insufficient stock
        int availableQty = selectedItem.getAvailableQuantity();
        if (quantity > availableQty) {
            // Show warning but allow it
            showAlert(Alert.AlertType.INFORMATION, "Stock Notice", 
                     "Required quantity (" + quantity + ") exceeds available stock (" + availableQty + ").\n" +
                     "The booking will be marked as DELAYED until parts are restocked.");
        }
        
        // Check if part already in list - if so, update quantity instead of adding duplicate
        for (BookingPart existingPart : selectedPartsList) {
            if (existingPart.getPartId() == selectedItem.getId()) {
                // Update the existing part's quantity
                existingPart.setQuantity(existingPart.getQuantity() + quantity);
                selectedPartsTable.refresh(); // Refresh table to show updated values
                
                // Reset selection
                availablePartsComboBox.setValue(null);
                partQuantitySpinner.getValueFactory().setValue(1);
                
                showAlert(Alert.AlertType.INFORMATION, "Quantity Updated", 
                         "Added " + quantity + " more to existing part. Total: " + existingPart.getQuantity());
                return;
            }
        }
        
        // Add part to list
        BookingPart newPart = new BookingPart(
            0, // booking ID will be set later
            selectedItem.getId(),
            selectedItem.getName(),
            quantity,
            selectedItem.getSellingPrice()
        );
        
        selectedPartsList.add(newPart);
        
        // Reset selection
        availablePartsComboBox.setValue(null);
        partQuantitySpinner.getValueFactory().setValue(1);
    }
    
    private void handleEditPart(BookingPart part) {
        // Create a dialog to edit the part quantity
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Edit Part Quantity");
        dialog.setHeaderText("Edit quantity for: " + part.getPartName());
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, part.getQuantity());
        Spinner<Integer> quantitySpinner = new Spinner<>(valueFactory);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(100);
        
        Label availableLabel = new Label();
        try {
            InventoryService inventoryService = InventoryService.getInstance();
            InventoryItem item = inventoryService.getItemById(part.getPartId());
            if (item != null) {
                availableLabel.setText("Available Stock: " + item.getAvailableQuantity());
                
                // Add listener to show warning if exceeds stock
                quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal > item.getAvailableQuantity()) {
                        availableLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        availableLabel.setText("Available Stock: " + item.getAvailableQuantity() + 
                                             " (Exceeds stock - will be DELAYED)");
                    } else {
                        availableLabel.setStyle("-fx-text-fill: black;");
                        availableLabel.setText("Available Stock: " + item.getAvailableQuantity());
                    }
                });
            }
        } catch (SQLException e) {
            availableLabel.setText("Could not load stock information");
        }
        
        grid.add(new Label("Quantity:"), 0, 0);
        grid.add(quantitySpinner, 1, 0);
        grid.add(availableLabel, 0, 1, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        // Show dialog and update part if confirmed
        dialog.showAndWait().ifPresent(newQuantity -> {
            part.setQuantity(newQuantity);
            selectedPartsTable.refresh(); // Refresh table to show updated values
        });
    }

    private void showBookingDialog(ServiceBookingViewModel existingBooking) {
        try {
            // Set the current editing booking
            currentEditingBooking = existingBooking;
            
            bookingDialog = new Dialog<>();
            bookingDialog.setTitle(existingBooking == null ? "Create New Booking" : "Edit Booking");
            bookingDialog.setHeaderText(existingBooking == null ? "Enter booking details" : "Edit booking details");
            
            // Set button types
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            bookingDialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // Create a ScrollPane to handle overflow
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefViewportHeight(600);
            
            // Create the form grid with more generous spacing
            GridPane grid = new GridPane();
            grid.setHgap(20); // Increased horizontal gap
            grid.setVgap(15); // Increased vertical gap
            grid.setPadding(new Insets(30)); // Increased padding
            grid.setMinWidth(800); // Set minimum width
            
            // Set column constraints to make the form wider
            ColumnConstraints labelColumn = new ColumnConstraints();
            labelColumn.setMinWidth(120);
            labelColumn.setPrefWidth(150);
            
            ColumnConstraints inputColumn = new ColumnConstraints();
            inputColumn.setMinWidth(300);
            inputColumn.setPrefWidth(400);
            inputColumn.setHgrow(Priority.ALWAYS);
            
            grid.getColumnConstraints().addAll(labelColumn, inputColumn);
            
            // Add styling
            grid.setStyle("-fx-background-color: white;");
            
            // Set up UI components with correct row spacing
            setupCustomerSelection(grid, 0);
            setupVehicleSelection(grid, 2);  // Start at row 2 to leave space
            setupMechanicSelection(grid, 4);  // Start at row 4 to leave space
            setupDateTimeSelectors(grid, 6);  // Start at row 6 to leave space
            setupServiceTypeAndDescription(grid, 9);  // Start at row 9 to leave space after date/time
            setupPartsSelection(grid, 12);  // Start at row 12 for parts selection
            
            // If editing an existing booking, populate fields
            if (existingBooking != null) {
                populateExistingData(existingBooking);
            }
            
            // Set up the dialog pane with proper sizing
            DialogPane dialogPane = bookingDialog.getDialogPane();
            dialogPane.setContent(grid);
            dialogPane.setPrefSize(900, 900);  // Increased height for parts section
            dialogPane.setMinSize(800, 800);
            dialogPane.setStyle("-fx-background-color: white; -fx-padding: 20;");
            
            // Get the save button to enable/disable it
            Button saveButton = (Button) bookingDialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                if (!validateAndSaveBooking()) {
                    event.consume();
                }
            });
            
            // Show dialog
            bookingDialog.showAndWait();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "An error occurred while showing the booking dialog: " + e.getMessage());
        }
    }
    
    private void populateExistingData(ServiceBookingViewModel booking) {
        // Populate customer
        customerComboBox.getItems().forEach(customer -> {
            if (customer.getId() == booking.getCustomer().getId()) {
                customerComboBox.setValue(customer);
            }
        });
        
        // Vehicle will be populated by customer selection listener
        try {
            List<Vehicle> vehicles = vehicleService.getCustomerVehicles(booking.getCustomer().getId());
            vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
            vehicles.forEach(vehicle -> {
                if (vehicle.getId() == booking.getVehicle().getId()) {
                    vehicleComboBox.setValue(vehicle);
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Could not load vehicles: " + e.getMessage());
        }
        
        // Populate mechanic
        mechanicComboBox.getItems().forEach(mechanic -> {
            if (mechanic.getId() == booking.getMechanic().getId()) {
                mechanicComboBox.setValue(mechanic);
            }
        });
        
        // Date and time
        // Set the booking date picker value
        bookingDatePicker.setValue(booking.getDate());
        timeComboBox.setValue(booking.getTime());
        
        // Service details
        serviceTypeComboBox.setValue(booking.getServiceType());
        serviceDescriptionArea.setText(booking.getServiceDescription());
        
        // Load existing parts for this booking
        try {
            List<BookingPart> existingParts = bookingService.getBookingParts(booking.getId());
            selectedPartsList.clear();
            selectedPartsList.addAll(existingParts);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Could not load booking parts: " + e.getMessage());
        }
    }
    
    private boolean validateAndSaveBooking() {
        System.out.println("Starting validateAndSaveBooking...");
        try {
            // Validate required fields
            if (customerComboBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a customer");
                return false;
            }
            if (vehicleComboBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a vehicle");
                return false;
            }
            if (mechanicComboBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a mechanic");
                return false;
            }
            if (bookingDatePicker.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a date");
                return false;
            }
            if (timeComboBox.getValue() == null || timeComboBox.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a time");
                return false;
            }
            if (serviceTypeComboBox.getValue() == null || serviceTypeComboBox.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a service type");
                return false;
            }

            System.out.println("All validations passed...");

            // Get all form values
            int customerId = customerComboBox.getValue().getId();
            int vehicleId = vehicleComboBox.getValue().getId();
            int mechanicId = mechanicComboBox.getValue().getId();
            LocalDate bookingDate = bookingDatePicker.getValue();
            String bookingTime = timeComboBox.getValue();
            String serviceType = serviceTypeComboBox.getValue();
            String serviceDescription = serviceDescriptionArea.getText();
            System.out.println("Collected form values:");
            System.out.println("Customer ID: " + customerId);
            System.out.println("Vehicle ID: " + vehicleId);
            System.out.println("Mechanic ID: " + mechanicId);
            System.out.println("Date: " + bookingDate);
            System.out.println("Time: " + bookingTime);
            System.out.println("Service Type: " + serviceType);

            try {
                // Prepare parts list (even if empty)
                List<BookingPart> partsToUse = new ArrayList<>(selectedPartsList);
                
                int bookingId;
                boolean isEdit = (currentEditingBooking != null);
                
                if (isEdit) {
                    // EDIT MODE: Update existing booking
                    bookingId = currentEditingBooking.getId();
                    System.out.println("Editing existing booking ID: " + bookingId);
                    
                    // First, release old parts reservations
                    try {
                        bookingService.releaseBookingParts(bookingId);
                        bookingService.deleteBookingParts(bookingId);
                    } catch (SQLException e) {
                        System.err.println("Could not release old parts: " + e.getMessage());
                    }
                    
                    // Check if parts are available and if mechanic is off duty
                    boolean hasInsufficientParts = false;
                    boolean mechanicOffDuty = false;
                    
                    // Check parts availability and mechanic status using try-with-resources
                    try (Connection checkConn = DatabaseUtil.getConnection()) {
                        for (BookingPart part : partsToUse) {
                            try (PreparedStatement checkStmt = checkConn.prepareStatement(
                                "SELECT quantity_in_stock, reserved_quantity FROM parts WHERE id = ?")) {
                                checkStmt.setInt(1, part.getPartId());
                                ResultSet rs = checkStmt.executeQuery();
                                if (rs.next()) {
                                    int totalStock = rs.getInt("quantity_in_stock");
                                    int reserved = rs.getInt("reserved_quantity");
                                    int available = totalStock - reserved;
                                    if (part.getQuantity() > available) {
                                        hasInsufficientParts = true;
                                        System.out.println("Insufficient parts for: " + part.getPartName() + 
                                                         " (needed: " + part.getQuantity() + ", available: " + available + ")");
                                    }
                                }
                            }
                        }
                        
                        // Check mechanic availability
                        try (PreparedStatement mechanicStmt = checkConn.prepareStatement(
                            "SELECT availability FROM mechanics WHERE id = ?")) {
                            mechanicStmt.setInt(1, mechanicId);
                            ResultSet mechanicRs = mechanicStmt.executeQuery();
                            if (mechanicRs.next()) {
                                String availability = mechanicRs.getString("availability");
                                mechanicOffDuty = (availability != null && !"Available".equalsIgnoreCase(availability));
                            }
                        }
                    }
                    
                    // Determine new status - if booking was delayed and now has parts/mechanic, keep as scheduled
                    // If parts/mechanic are now insufficient, set to delayed
                    String newStatus;
                    if (hasInsufficientParts || mechanicOffDuty) {
                        newStatus = "delayed";
                    } else if ("delayed".equals(currentEditingBooking.getStatus())) {
                        // Was delayed but now has parts and mechanic available
                        newStatus = "scheduled";
                    } else {
                        // Keep existing status (scheduled, in progress, etc.)
                        newStatus = currentEditingBooking.getStatus();
                    }
                    
                    // Update the booking with potentially new status
                    boolean updated = bookingService.updateBooking(
                        bookingId,
                        customerId,
                        vehicleId,
                        mechanicId,
                        bookingDate,
                        bookingTime,
                        serviceType,
                        serviceDescription,
                        newStatus
                    );
                    
                    if (!updated) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to update booking");
                        return false;
                    }
                    
                    // Reserve new parts within a transaction using a separate connection
                    try (Connection conn = DatabaseUtil.getConnection()) {
                        conn.setAutoCommit(false);
                        try (PreparedStatement reserveStmt = conn.prepareStatement(
                            "UPDATE parts SET reserved_quantity = reserved_quantity + ? WHERE id = ?")) {
                            
                            for (BookingPart part : partsToUse) {
                                reserveStmt.setInt(1, part.getQuantity());
                                reserveStmt.setInt(2, part.getPartId());
                                reserveStmt.executeUpdate();
                            }
                            conn.commit();
                        } catch (SQLException e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                    }
                } else {
                    // CREATE MODE: New booking
                    bookingId = bookingService.createBookingAndReturnId(
                        customerId,
                        vehicleId,
                        mechanicId,
                        bookingDate,
                        bookingTime,
                        serviceType,
                        serviceDescription,
                        "scheduled",
                        partsToUse
                    );
                }
                
                System.out.println("Booking " + (isEdit ? "update" : "creation") + " result - ID: " + bookingId);

                if (bookingId > 0) {
                    // Save selected parts to booking_parts table
                    if (selectedPartsList != null && !selectedPartsList.isEmpty()) {
                        System.out.println("Saving " + selectedPartsList.size() + " parts...");
                        for (BookingPart part : selectedPartsList) {
                            boolean partSaved = bookingService.addBookingPart(
                                bookingId,
                                part.getPartId(),
                                part.getQuantity(),
                                part.getPrice()
                            );
                            if (!partSaved) {
                                System.err.println("Warning: Could not save part: " + part.getPartName());
                            }
                        }
                    } else {
                        System.out.println("No parts selected for this booking.");
                    }
                    
                    // Check booking status to show appropriate message
                    String statusMessage = isEdit ? "Booking updated successfully" : "Booking created successfully";
                    String query = "SELECT sb.status, m.availability FROM service_bookings sb " +
                                   "LEFT JOIN mechanics m ON sb.mechanic_id = m.id WHERE sb.id = ?";
                    try (Connection conn = DatabaseUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setInt(1, bookingId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if ("delayed".equalsIgnoreCase(status)) {
                                // Check if delay is due to mechanic or parts
                                String mechanicAvailability = rs.getString("availability");
                                boolean mechanicOffDuty = mechanicAvailability != null && 
                                                          !"Available".equalsIgnoreCase(mechanicAvailability);
                                
                                // Check if there are insufficient parts
                                boolean hasInsufficientParts = false;
                                if (!selectedPartsList.isEmpty()) {
                                    String partsQuery = "SELECT bp.quantity, p.quantity_in_stock, p.reserved_quantity " +
                                                       "FROM booking_parts bp JOIN parts p ON bp.part_id = p.id " +
                                                       "WHERE bp.booking_id = ?";
                                    try (PreparedStatement partsStmt = conn.prepareStatement(partsQuery)) {
                                        partsStmt.setInt(1, bookingId);
                                        ResultSet partsRs = partsStmt.executeQuery();
                                        while (partsRs.next()) {
                                            int requiredQty = partsRs.getInt("quantity");
                                            int stock = partsRs.getInt("quantity_in_stock");
                                            int reserved = partsRs.getInt("reserved_quantity");
                                            int available = stock - (reserved - requiredQty);
                                            if (available < requiredQty) {
                                                hasInsufficientParts = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                // Determine the primary reason for delay
                                if (hasInsufficientParts && mechanicOffDuty) {
                                    statusMessage = "Booking created with DELAYED status due to insufficient parts and mechanic unavailability";
                                } else if (hasInsufficientParts) {
                                    statusMessage = "Booking created with DELAYED status due to insufficient parts";
                                } else if (mechanicOffDuty) {
                                    statusMessage = "Booking created with DELAYED status due to mechanic unavailability";
                                } else {
                                    statusMessage = "Booking created with DELAYED status";
                                }
                            } else {
                                statusMessage = "Booking scheduled successfully. Parts have been reserved.";
                            }
                        }
                    }
                    
                    final String finalMessage = statusMessage + 
                        (selectedPartsList.isEmpty() ? "" : " (" + selectedPartsList.size() + " part(s))");
                    
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", finalMessage);
                        loadBookings(); // Refresh the table
                    });
                    return true;
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save booking. Please try again.");
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save booking: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Unexpected Error", "An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }
            
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleDeleteSelected() {
        List<ServiceBookingViewModel> selectedBookings = bookingList.stream()
            .filter(ServiceBookingViewModel::isSelected)
            .toList();
        
        if (selectedBookings.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Selection", "Please select bookings to delete");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete " + selectedBookings.size() + " booking(s)?");
        confirmAlert.setContentText("This action cannot be undone.");
        
        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            int deletedCount = 0;
            for (ServiceBookingViewModel booking : selectedBookings) {
                try {
                    // Release parts first
                    bookingService.releaseBookingParts(booking.getId());
                    // Delete booking
                    if (bookingService.deleteBooking(booking.getId())) {
                        deletedCount++;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            
            loadBookings();
            showAlert(Alert.AlertType.INFORMATION, "Success", deletedCount + " booking(s) deleted");
        }
    }
    
    /**
     * Show a CAPTCHA verification dialog for critical actions
     * @param action The action being performed ("cancel" or "complete")
     * @param bookingHexId The hex ID of the booking
     * @return true if verification passed, false otherwise
     */
    private boolean showCaptchaVerification(String action, String bookingHexId) {
        // Generate random math problem
        java.util.Random random = new java.util.Random();
        int num1 = random.nextInt(20) + 1; // 1-20
        int num2 = random.nextInt(20) + 1; // 1-20
        int correctAnswer = num1 + num2;
        
        // First confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Action");
        confirmAlert.setHeaderText("Are you sure you want to " + action + " this booking?");
        confirmAlert.setContentText("Booking ID: " + bookingHexId + "\n\n" +
                                   "This action will:\n" +
                                   (action.equals("cancel") ? 
                                    "• Release all reserved parts back to inventory\n" +
                                    "• Mark this booking as cancelled\n" +
                                    "• Cannot be undone" :
                                    "• Generate a bill automatically\n" +
                                    "• Mark this booking as completed\n" +
                                    "• Move to billing section"));
        
        ButtonType yesButton = new ButtonType("Yes, Continue", ButtonBar.ButtonData.OK_DONE);
        ButtonType noButton = new ButtonType("No, Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(yesButton, noButton);
        
        var firstConfirmResult = confirmAlert.showAndWait();
        if (firstConfirmResult.isEmpty() || firstConfirmResult.get() != yesButton) {
            return false; // User cancelled
        }
        
        // Second confirmation with CAPTCHA
        Dialog<String> captchaDialog = new Dialog<>();
        captchaDialog.setTitle("Verification Required");
        captchaDialog.setHeaderText("Please solve this math problem to " + action + " the booking");
        
        ButtonType verifyButton = new ButtonType("Verify", ButtonBar.ButtonData.OK_DONE);
        captchaDialog.getDialogPane().getButtonTypes().addAll(verifyButton, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        Label questionLabel = new Label("What is " + num1 + " + " + num2 + " ?");
        questionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TextField answerField = new TextField();
        answerField.setPromptText("Enter your answer");
        answerField.setPrefWidth(200);
        
        Label warningLabel = new Label("⚠ This verification ensures you want to proceed with this critical action.");
        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(350);
        warningLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
        
        grid.add(questionLabel, 0, 0, 2, 1);
        grid.add(new Label("Answer:"), 0, 1);
        grid.add(answerField, 1, 1);
        grid.add(warningLabel, 0, 2, 2, 1);
        
        captchaDialog.getDialogPane().setContent(grid);
        
        // Focus on text field
        Platform.runLater(() -> answerField.requestFocus());
        
        captchaDialog.setResultConverter(dialogButton -> {
            if (dialogButton == verifyButton) {
                return answerField.getText();
            }
            return null;
        });
        
        var result = captchaDialog.showAndWait();
        if (result.isEmpty()) {
            return false; // User cancelled
        }
        
        try {
            int userAnswer = Integer.parseInt(result.get());
            if (userAnswer == correctAnswer) {
                return true; // Verification passed!
            } else {
                showAlert(Alert.AlertType.ERROR, "Verification Failed", 
                         "Incorrect answer. The booking status was not changed.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Verification Failed", 
                     "Invalid input. Please enter a number.");
            return false;
        }
    }
}
