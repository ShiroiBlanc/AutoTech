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
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServiceBookingController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private DatePicker filterDatePicker;
    @FXML private TableView<ServiceBookingViewModel> bookingTable;
    @FXML private TableColumn<ServiceBookingViewModel, Integer> idColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> dateColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> timeColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> customerNameColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> vehicleColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> mechanicColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> serviceTypeColumn;
    @FXML private TableColumn<ServiceBookingViewModel, String> statusColumn;
    @FXML private TableColumn<ServiceBookingViewModel, Void> actionsColumn;
    @FXML private Label statusLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private TabPane bookingTabPane;

    private ObservableList<ServiceBookingViewModel> bookingList = FXCollections.observableArrayList();
    private ServiceBookingService bookingService = new ServiceBookingService();
    private CustomerService customerService = CustomerService.getInstance();
    private VehicleService vehicleService = VehicleService.getInstance();
    private MechanicService mechanicService = new MechanicService();
    
    // ViewModel for service bookings
    public static class ServiceBookingViewModel {
        private int id;
        private int customerId;
        private int vehicleId;
        private int mechanicId;
        private String date;
        private String time;
        private String customerName;
        private String vehicleInfo;
        private String mechanicName;
        private String serviceType;
        private String serviceDescription;
        private String status;
        
        public ServiceBookingViewModel(int id, int customerId, int vehicleId, int mechanicId, String date, 
                                       String time, String customerName, String vehicleInfo, 
                                       String mechanicName, String serviceType, String serviceDescription, String status) {
            this.id = id;
            this.customerId = customerId;
            this.vehicleId = vehicleId;
            this.mechanicId = mechanicId;
            this.date = date;
            this.time = time;
            this.customerName = customerName;
            this.vehicleInfo = vehicleInfo;
            this.mechanicName = mechanicName;
            this.serviceType = serviceType;
            this.serviceDescription = serviceDescription;
            this.status = status;
        }
        
        // Getters
        public int getId() { return id; }
        public int getCustomerId() { return customerId; }
        public int getVehicleId() { return vehicleId; }
        public int getMechanicId() { return mechanicId; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getCustomerName() { return customerName; }
        public String getVehicleInfo() { return vehicleInfo; }
        public String getMechanicName() { return mechanicName; }
        public String getServiceType() { return serviceType; }
        public String getServiceDescription() { return serviceDescription; }
        public String getStatus() { return status; }
        
        // Setters
        public void setStatus(String status) { this.status = status; }
    }
    
    @FXML
    public void initialize() {
        System.out.println("Initializing ServiceBookingController...");
        
        // Setup filter components
        filterStatusComboBox.getItems().addAll("All", "Scheduled", "In Progress", "Completed", "Cancelled");
        filterStatusComboBox.setValue("All");
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
        mechanicColumn.setCellValueFactory(new PropertyValueFactory<>("mechanicName"));
        serviceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style the status column with colors
        statusColumn.setCellFactory(column -> {
            return new TableCell<ServiceBookingViewModel, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        switch (item.toLowerCase()) {
                            case "scheduled":
                                setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                                break;
                            case "in_progress":
                            case "in progress":
                                setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                break;
                            case "completed":
                                setStyle("-fx-text-fill: green;");
                                break;
                            case "cancelled":
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
        bookingTable.setItems(bookingList);
        
        // Load initial data
        loadBookings();
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> {
            return new TableCell<ServiceBookingViewModel, Void>() {
                private final Button viewButton = new Button("View");
                private final Button editButton = new Button("Edit");
                private final Button statusButton = new Button("Status");
                private final HBox buttonBox = new HBox(5, viewButton, editButton, statusButton);
                
                {
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
    private void handleSearchBookings() {
        String searchTerm = searchField.getText().trim();
        String statusFilter = filterStatusComboBox.getValue();
        LocalDate dateFilter = filterDatePicker.getValue();
        
        loadBookingsFiltered(searchTerm, statusFilter, dateFilter);
    }
    
    @FXML
    private void handleRefreshBookings() {
        searchField.clear();
        filterStatusComboBox.setValue("All");
        filterDatePicker.setValue(null);
        loadBookings();
    }
    
    @FXML
    private void handleAddBooking() {
        showBookingDialog(null);
    }
    
    private void loadBookings() {
        try {
            List<ServiceBookingViewModel> bookings = bookingService.getAllBookings();
            bookingList.clear();
            bookingList.addAll(bookings);
            updateTotalBookingsLabel();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, 
                     "Load Error", 
                     "Error loading bookings: " + e.getMessage());
        }
    }
    
    private void loadBookingsFiltered(String searchTerm, String statusFilter, LocalDate dateFilter) {
        try {
            List<ServiceBookingViewModel> bookings = bookingService.searchBookings(searchTerm, statusFilter, dateFilter);
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
    
    private void viewBookingDetails(ServiceBookingViewModel booking) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Booking Details");
            dialog.setHeaderText("Booking #" + booking.getId() + " Details");
            
            // Create the content layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            // Add booking details
            int row = 0;
            grid.add(new Label("Booking ID:"), 0, row);
            grid.add(new Label(String.valueOf(booking.getId())), 1, row++);
            
            grid.add(new Label("Date:"), 0, row);
            grid.add(new Label(booking.getDate()), 1, row++);
            
            grid.add(new Label("Time:"), 0, row);
            grid.add(new Label(booking.getTime()), 1, row++);
            
            grid.add(new Label("Customer:"), 0, row);
            grid.add(new Label(booking.getCustomerName()), 1, row++);
            
            grid.add(new Label("Vehicle:"), 0, row);
            grid.add(new Label(booking.getVehicleInfo()), 1, row++);
            
            grid.add(new Label("Mechanic:"), 0, row);
            grid.add(new Label(booking.getMechanicName()), 1, row++);
            
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
            
            // Add close button
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            dialog.getDialogPane().setContent(grid);
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
        dialog.setHeaderText("Update status for booking #" + booking.getId());
        
        // Set button types
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Add status combo box
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Scheduled", "In Progress", "Completed", "Cancelled");
        statusComboBox.setValue(booking.getStatus());
        
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
                boolean success = bookingService.updateBookingStatus(booking.getId(), newStatus);
                if (success) {
                    booking.setStatus(newStatus);
                    bookingTable.refresh();
                    statusLabel.setText("Booking status updated successfully");
                    
                    // If the status is changed to "Completed", create a bill
                    if ("Completed".equals(newStatus)) {
                        BillingService billingService = BillingService.getInstance();
                        boolean billCreated = billingService.createBillFromService(booking.getId());
                        
                        if (billCreated) {
                            showAlert(Alert.AlertType.INFORMATION, 
                                     "Bill Created", 
                                     "A bill has been created for this service.");
                        }
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update booking status");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update status: " + e.getMessage());
            }
        });
    }
    
    private void showBookingDialog(ServiceBookingViewModel existingBooking) {
        try {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle(existingBooking == null ? "Create New Booking" : "Edit Booking");
            dialog.setHeaderText(existingBooking == null ? "Enter booking details" : "Edit booking details");
            
            // Set button types
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // Create the form grid
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            // Customer selection
            ComboBox<Customer> customerComboBox = new ComboBox<>();
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Vehicle selection (will be populated when customer is selected)
            ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
            vehicleComboBox.setDisable(true); // Initially disabled until customer is selected
            vehicleComboBox.setConverter(new StringConverter<Vehicle>() {
                @Override
                public String toString(Vehicle vehicle) {
                    return vehicle == null ? "" : vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getPlateNumber() + ")";
                }
                
                @Override
                public Vehicle fromString(String string) {
                    return null; // Not needed for ComboBox
                }
            });
            
            // Update vehicles when customer is selected
            customerComboBox.setOnAction(e -> {
                Customer selectedCustomer = customerComboBox.getValue();
                if (selectedCustomer != null) {
                    try {
                        List<Vehicle> vehicles = vehicleService.getCustomerVehicles(selectedCustomer.getId());
                        vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
                        vehicleComboBox.setDisable(false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        vehicleComboBox.setDisable(true);
                    }
                } else {
                    vehicleComboBox.getItems().clear();
                    vehicleComboBox.setDisable(true);
                }
            });
            
            // Mechanic selection
            ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
            try {
                List<Mechanic> mechanics = mechanicService.getAllMechanics();
                mechanicComboBox.setItems(FXCollections.observableArrayList(mechanics));
                mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
                    @Override
                    public String toString(Mechanic mechanic) {
                        return mechanic == null ? "" : mechanic.getName() + 
                               (mechanic.getSpecialty() != null && !mechanic.getSpecialty().isEmpty() ? 
                               " (" + mechanic.getSpecialty() + ")" : "");
                    }
                    
                    @Override
                    public Mechanic fromString(String string) {
                        return null; // Not needed for ComboBox
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Date and Time
            DatePicker datePicker = new DatePicker(LocalDate.now());
            
            ComboBox<String> timeComboBox = new ComboBox<>();
            for (int hour = 8; hour <= 17; hour++) { // 8 AM to 5 PM
                timeComboBox.getItems().add(String.format("%02d:00", hour));
                timeComboBox.getItems().add(String.format("%02d:30", hour));
            }
            timeComboBox.setValue("09:00"); // Default to 9 AM
            
            // Service Type and Description
            ComboBox<String> serviceTypeComboBox = new ComboBox<>();
            serviceTypeComboBox.getItems().addAll(
                "Regular Maintenance", "Oil Change", "Tire Service", "Brake Service", 
                "Engine Repair", "Transmission Service", "Electrical System", "Other"
            );
            
            TextArea descriptionArea = new TextArea();
            descriptionArea.setPrefRowCount(4);
            
            // Set existing values if editing
            if (existingBooking != null) {
                // Find and select customer
                for (Customer customer : customerComboBox.getItems()) {
                    if (customer.getId() == existingBooking.getCustomerId()) {
                        customerComboBox.setValue(customer);
                        break;
                    }
                }
                
                // Load vehicles for the selected customer and select the right one
                try {
                    List<Vehicle> vehicles = vehicleService.getCustomerVehicles(existingBooking.getCustomerId());
                    vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
                    vehicleComboBox.setDisable(false);
                    
                    for (Vehicle vehicle : vehicles) {
                        if (vehicle.getId() == existingBooking.getVehicleId()) {
                            vehicleComboBox.setValue(vehicle);
                            break;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                // Find and select mechanic
                for (Mechanic mechanic : mechanicComboBox.getItems()) {
                    if (mechanic.getId() == existingBooking.getMechanicId()) {
                        mechanicComboBox.setValue(mechanic);
                        break;
                    }
                }
                
                // Set date and time
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                datePicker.setValue(LocalDate.parse(existingBooking.getDate(), dateFormatter));
                timeComboBox.setValue(existingBooking.getTime());
                
                // Set service details
                serviceTypeComboBox.setValue(existingBooking.getServiceType());
                descriptionArea.setText(existingBooking.getServiceDescription());
            }
            
            // Add form elements to grid
            int row = 0;
            grid.add(new Label("Customer:"), 0, row);
            grid.add(customerComboBox, 1, row++);
            
            grid.add(new Label("Vehicle:"), 0, row);
            grid.add(vehicleComboBox, 1, row++);
            
            grid.add(new Label("Mechanic:"), 0, row);
            grid.add(mechanicComboBox, 1, row++);
            
            grid.add(new Label("Date:"), 0, row);
            grid.add(datePicker, 1, row++);
            
            grid.add(new Label("Time:"), 0, row);
            grid.add(timeComboBox, 1, row++);
            
            grid.add(new Label("Service Type:"), 0, row);
            grid.add(serviceTypeComboBox, 1, row++);
            
            grid.add(new Label("Description:"), 0, row);
            grid.add(descriptionArea, 1, row++);
            
            dialog.getDialogPane().setContent(grid);
            
            // Request focus on the customer field by default
            customerComboBox.requestFocus();
            
            // Convert the result to boolean
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Validate required fields
                    if (customerComboBox.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a customer");
                        return false;
                    }
                    
                    if (vehicleComboBox.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a vehicle");
                        return false;
                    }
                    
                    if (mechanicComboBox.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a mechanic");
                        return false;
                    }
                    
                    if (datePicker.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a date");
                        return false;
                    }
                    
                    if (timeComboBox.getValue() == null || timeComboBox.getValue().trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a time");
                        return false;
                    }
                    
                    if (serviceTypeComboBox.getValue() == null || serviceTypeComboBox.getValue().trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Please select a service type");
                        return false;
                    }
                    
                    try {
                        Customer customer = customerComboBox.getValue();
                        Vehicle vehicle = vehicleComboBox.getValue();
                        Mechanic mechanic = mechanicComboBox.getValue();
                        LocalDate date = datePicker.getValue();
                        String time = timeComboBox.getValue();
                        String serviceType = serviceTypeComboBox.getValue();
                        String description = descriptionArea.getText();
                        
                        boolean success;
                        if (existingBooking == null) {
                            // Create new booking
                            success = bookingService.createBooking(
                                customer.getId(), vehicle.getId(), mechanic.getId(), date, time, 
                                serviceType, description, "Scheduled"
                            );
                        } else {
                            // Update existing booking
                            success = bookingService.updateBooking(
                                existingBooking.getId(), customer.getId(), vehicle.getId(), mechanic.getId(), 
                                date, time, serviceType, description, existingBooking.getStatus()
                            );
                        }
                        
                        if (success) {
                            loadBookings();
                            statusLabel.setText("Booking " + (existingBooking == null ? "created" : "updated") + " successfully");
                            return true;
                        } else {
                            showAlert(Alert.AlertType.ERROR, 
                                     "Save Error", 
                                     "Failed to " + (existingBooking == null ? "create" : "update") + " booking");
                            return false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
                        return false;
                    }
                }
                return false;
            });
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open booking dialog: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
