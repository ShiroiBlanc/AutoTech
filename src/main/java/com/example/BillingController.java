package com.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillingController {
    @FXML private TableView<Bill> billTable;
    @FXML private TableColumn<Bill, String> idColumn;
    @FXML private TableColumn<Bill, String> customerColumn;
    @FXML private TableColumn<Bill, String> vehicleColumn;
    @FXML private TableColumn<Bill, Double> amountColumn;
    @FXML private TableColumn<Bill, LocalDate> dateColumn;
    @FXML private TableColumn<Bill, String> statusColumn;
    @FXML private TableColumn<Bill, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    
    private ObservableList<Bill> billList = FXCollections.observableArrayList();
    private BillingService billingService = BillingService.getInstance();
    
    @FXML
    public void initialize() {
        // Initialize columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        
        // Make ID column clickable to show booking details
        idColumn.setCellFactory(column -> {
            return new TableCell<Bill, String>() {
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
                            Bill bill = getTableRow().getItem();
                            if (bill != null) {
                                showBookingDetails(bill);
                            }
                        });
                    }
                }
            };
        });
        
        // Format the amount column
        amountColumn.setCellFactory(column -> {
            return new TableCell<Bill, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("₱%.2f", item));
                    }
                }
            };
        });
        
        // Set up actions column
        setupActionsColumn();
        
        // Load bills from database
        loadBills();
        
        // Set items to table
        billTable.setItems(billList);
    }
    
    private void loadBills() {
        try {
            List<Bill> bills = billingService.getAllBills();
            billList.clear();
            billList.addAll(bills);
            statusLabel.setText("Bills loaded successfully. Total: " + bills.size());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bills: " + e.getMessage());
        }
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<Bill, Void>() {
            private final Button payButton = new Button("Pay");
            private final Button receiptButton = new Button("Receipt");
            private final HBox buttonBox = new HBox(5, payButton, receiptButton);
            
            {
                buttonBox.setAlignment(Pos.CENTER);
                
                payButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem();
                    if (bill != null) {
                        processPayment(bill);
                    }
                });
                
                receiptButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem();
                    if (bill != null) {
                        printReceipt(bill);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Bill bill = getTableRow().getItem();
                    if (bill != null) {
                        // Only show pay button if bill is unpaid
                        payButton.setVisible(!"Paid".equals(bill.getPaymentStatus()));
                        // Only enable receipt button if bill is paid
                        receiptButton.setDisable(!"Paid".equals(bill.getPaymentStatus()));
                    }
                    setGraphic(buttonBox);
                }
            }
        });
    }
    
    private void showBookingDetails(Bill bill) {
        try {
            // Fetch booking details from service_bookings using the service_id
            ServiceBookingService bookingService = new ServiceBookingService();
            ServiceBookingViewModel booking = bookingService.getAllBookings().stream()
                .filter(b -> b.getId() == bill.getServiceId())
                .findFirst()
                .orElse(null);
            
            if (booking == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Booking details not found for this bill.");
                return;
            }
            
            // Create dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Booking Details");
            dialog.setHeaderText("Details for Bill " + bill.getHexId());
            
            // Create content
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(15);
            grid.setPadding(new Insets(20));
            grid.setStyle("-fx-background-color: white;");
            
            int row = 0;
            
            // Bill Information
            Label billHeaderLabel = new Label("Bill Information:");
            billHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-underline: true;");
            grid.add(billHeaderLabel, 0, row++, 2, 1);
            
            grid.add(new Label("Bill ID:"), 0, row);
            Label billIdLabel = new Label(bill.getHexId());
            billIdLabel.setStyle("-fx-font-weight: bold;");
            grid.add(billIdLabel, 1, row++);
            
            grid.add(new Label("Amount:"), 0, row);
            Label amountLabel = new Label(String.format("₱%.2f", bill.getAmount()));
            amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc; -fx-font-size: 14px;");
            grid.add(amountLabel, 1, row++);
            
            grid.add(new Label("Payment Status:"), 0, row);
            Label statusLabel = new Label(bill.getPaymentStatus());
            statusLabel.setStyle("-fx-font-weight: bold;");
            if ("Paid".equals(bill.getPaymentStatus())) {
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            } else if ("Unpaid".equals(bill.getPaymentStatus())) {
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            }
            grid.add(statusLabel, 1, row++);
            
            grid.add(new Label("Bill Date:"), 0, row);
            grid.add(new Label(bill.getBillDate().toString()), 1, row++);
            
            // Booking Information
            row++;
            Label bookingHeaderLabel = new Label("Booking Information:");
            bookingHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-underline: true;");
            grid.add(bookingHeaderLabel, 0, row++, 2, 1);
            
            grid.add(new Label("Booking ID:"), 0, row);
            grid.add(new Label(String.valueOf(booking.getId())), 1, row++);
            
            grid.add(new Label("Customer:"), 0, row);
            Label customerLabel = new Label(booking.getCustomer().getName());
            customerLabel.setStyle("-fx-font-weight: bold;");
            grid.add(customerLabel, 1, row++);
            
            grid.add(new Label("Vehicle:"), 0, row);
            grid.add(new Label(booking.getVehicle().getBrand()), 1, row++);
            
            grid.add(new Label("Mechanic:"), 0, row);
            grid.add(new Label(booking.getMechanic().getName()), 1, row++);
            
            grid.add(new Label("Service Type:"), 0, row);
            Label serviceTypeLabel = new Label(booking.getServiceType());
            serviceTypeLabel.setStyle("-fx-font-weight: bold;");
            grid.add(serviceTypeLabel, 1, row++);
            
            grid.add(new Label("Service Description:"), 0, row);
            Label descLabel = new Label(booking.getServiceDescription());
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(300);
            grid.add(descLabel, 1, row++);
            
            grid.add(new Label("Booking Date:"), 0, row);
            grid.add(new Label(booking.getDate().toString()), 1, row++);
            
            grid.add(new Label("Booking Time:"), 0, row);
            grid.add(new Label(booking.getTime()), 1, row++);
            
            grid.add(new Label("Status:"), 0, row);
            Label bookingStatusLabel = new Label(booking.getStatus());
            bookingStatusLabel.setStyle("-fx-font-weight: bold;");
            grid.add(bookingStatusLabel, 1, row++);
            
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(500);
            
            dialog.showAndWait();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load booking details: " + e.getMessage());
        }
    }
    
    private void processPayment(Bill bill) {
        try {
            // Show payment confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Payment Confirmation");
            confirmDialog.setHeaderText("Confirm Payment");
            confirmDialog.setContentText("Process payment of ₱" + String.format("%.2f", bill.getAmount()) + 
                                         " for bill " + bill.getHexId() + "?");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        boolean success = billingService.updateBillStatus(bill.getId(), "Paid");
                        if (success) {
                            bill.setPaymentStatus("Paid");
                            billTable.refresh();
                            statusLabel.setText("Payment processed for bill " + bill.getHexId());
                            
                            showAlert(Alert.AlertType.INFORMATION, 
                                    "Payment Processed", 
                                    "Payment has been successfully processed.");
                        } else {
                            showAlert(Alert.AlertType.ERROR, 
                                    "Payment Error", 
                                    "Failed to process payment. Please try again.");
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, 
                                "Database Error", 
                                "Failed to update payment status: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to process payment: " + e.getMessage());
        }
    }
    
    private void printReceipt(Bill bill) {
        // Create a dialog to display the receipt
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Receipt");
        dialog.setHeaderText("Receipt for Bill " + bill.getHexId());
        
        // Create the receipt content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Add receipt header
        Label headerLabel = new Label("AutoTech Service Receipt");
        headerLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        grid.add(headerLabel, 0, row++, 2, 1);
        
        // Add horizontal line
        Separator separator = new Separator();
        grid.add(separator, 0, row++, 2, 1);
        
        // Add receipt details
        grid.add(new Label("Receipt #:"), 0, row);
        grid.add(new Label(bill.getHexId()), 1, row++);
        
        grid.add(new Label("Date:"), 0, row);
        grid.add(new Label(bill.getBillDate().toString()), 1, row++);
        
        grid.add(new Label("Customer:"), 0, row);
        grid.add(new Label(bill.getCustomerName()), 1, row++);
        
        grid.add(new Label("Vehicle:"), 0, row);
        grid.add(new Label(bill.getVehicleInfo()), 1, row++);
        
        grid.add(new Label("Service ID:"), 0, row);
        grid.add(new Label(String.valueOf(bill.getServiceId())), 1, row++);
        
        // Add another separator before amount
        grid.add(new Separator(), 0, row++, 2, 1);
        
        grid.add(new Label("Amount:"), 0, row);
        Label amountLabel = new Label(String.format("₱%.2f", bill.getAmount()));
        amountLabel.setStyle("-fx-font-weight: bold;");
        grid.add(amountLabel, 1, row++);
        
        grid.add(new Label("Status:"), 0, row);
        Label statusLabel = new Label(bill.getPaymentStatus());
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        grid.add(statusLabel, 1, row++);
        
        // Add a thank you message
        Label thankYouLabel = new Label("Thank you for your business!");
        thankYouLabel.setStyle("-fx-font-style: italic;");
        grid.add(thankYouLabel, 0, row + 1, 2, 1);
        
        // Add close button
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        // Add print button
        ButtonType printButton = new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(printButton);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == printButton) {
                // In a real application, this would send the receipt to a printer
                statusLabel.setText("Receipt for bill " + bill.getHexId() + " sent to printer");
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            billTable.setItems(billList);
        } else {
            List<Bill> filteredList = new ArrayList<>();
            for (Bill bill : billList) {
                if (bill.getCustomerName().toLowerCase().contains(searchTerm) || 
                    bill.getVehicleInfo().toLowerCase().contains(searchTerm)) {
                    filteredList.add(bill);
                }
            }
            billTable.setItems(FXCollections.observableArrayList(filteredList));
        }
    }
    
    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadBills();
        billTable.setItems(billList);
    }
    
    @FXML
    private void handleGenerateBill() {
        // In a real application, this would open a dialog to manually create a bill
        // For now, we'll just show an information message
        showAlert(Alert.AlertType.INFORMATION, 
                 "Manual Bill Creation", 
                 "Bills are automatically generated when service bookings are completed. " +
                 "This feature would allow manual bill creation.");
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
