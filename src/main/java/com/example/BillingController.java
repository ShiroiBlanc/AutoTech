package com.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    @FXML private Pagination billPagination;
    
    private ObservableList<Bill> billList = FXCollections.observableArrayList();
    private ObservableList<Bill> allBills = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 25;
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
        
        // Set table resize policy
        billTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Setup pagination
        setupPagination();
        
        // Load bills from database
        loadBills();
        
        // Set items to table
        billTable.setItems(billList);
    }

    
    private void loadBills() {
        try {
            List<Bill> bills = billingService.getAllBills();
            allBills.clear();
            allBills.addAll(bills);
            updatePaginationControl();
            updateTablePage(0);
            statusLabel.setText("Bills loaded successfully. Total: " + bills.size());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bills: " + e.getMessage());
        }
    }
    
    private void setupPagination() {
        if (billPagination != null) {
            billPagination.setPageFactory(pageIndex -> {
                updateTablePage(pageIndex);
                return billTable;
            });
        }
    }
    
    private void updateTablePage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allBills.size());
        
        billList.clear();
        if (fromIndex < allBills.size()) {
            billList.addAll(allBills.subList(fromIndex, toIndex));
        }
    }
    
    private void updatePaginationControl() {
        if (billPagination != null) {
            int pageCount = (int) Math.ceil((double) allBills.size() / ITEMS_PER_PAGE);
            billPagination.setPageCount(Math.max(1, pageCount));
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
            // Fetch booking details using the service_id
            ServiceBookingService bookingService = new ServiceBookingService();
            ServiceBookingViewModel booking = bookingService.getBookingById(bill.getServiceId());
            
            if (booking == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Booking details not found for this bill.");
                return;
            }
            
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Bill Details");
            dialog.setHeaderText("Bill Details - " + bill.getHexId());
            
            // Create scrollable content
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(600);
            
            VBox mainContainer = new VBox(15);
            mainContainer.setPadding(new Insets(20));
            mainContainer.setStyle("-fx-background-color: white;");
            
            // === BILL INFORMATION SECTION ===
            VBox billSection = new VBox(10);
            billSection.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15; -fx-background-radius: 5;");
            Label billHeader = new Label("💰 BILL INFORMATION");
            billHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            billSection.getChildren().add(billHeader);
            
            GridPane billGrid = new GridPane();
            billGrid.setHgap(15);
            billGrid.setVgap(8);
            billGrid.setPadding(new Insets(10, 0, 0, 0));
            
            int row = 0;
            addDetailRow(billGrid, row++, "Bill ID:", bill.getHexId(), true);
            addDetailRow(billGrid, row++, "Bill Date:", bill.getBillDate().toString(), false);
            addDetailRow(billGrid, row++, "Payment Status:", bill.getPaymentStatus().toUpperCase(), false);
            
            billSection.getChildren().add(billGrid);
            mainContainer.getChildren().add(billSection);
            
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
            
            // Set column constraints to prevent overlapping
            ColumnConstraints labelCol = new ColumnConstraints();
            labelCol.setMinWidth(150);
            labelCol.setPrefWidth(150);
            
            ColumnConstraints valueCol = new ColumnConstraints();
            valueCol.setMinWidth(400);
            valueCol.setPrefWidth(500);
            valueCol.setHgrow(Priority.ALWAYS);
            
            customerGrid.getColumnConstraints().addAll(labelCol, valueCol);
            
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
                    if (fullCustomer.getAddress() != null && !fullCustomer.getAddress().isEmpty()) {
                        addDetailRow(customerGrid, row++, "Address:", fullCustomer.getAddress(), false);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Could not load customer details: " + e.getMessage());
            }
            
            addDetailRow(customerGrid, row++, "Vehicle:", booking.getVehicle().getModel(), false);
            
            customerSection.getChildren().add(customerGrid);
            mainContainer.getChildren().add(customerSection);
            
            // === SERVICE SECTION ===
            VBox serviceSection = new VBox(10);
            serviceSection.setStyle("-fx-background-color: #fff3e0; -fx-padding: 15; -fx-background-radius: 5;");
            Label serviceHeader = new Label("🔧 SERVICE DETAILS");
            serviceHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E65100;");
            serviceSection.getChildren().add(serviceHeader);
            
            GridPane serviceGrid = new GridPane();
            serviceGrid.setHgap(15);
            serviceGrid.setVgap(8);
            serviceGrid.setPadding(new Insets(10, 0, 0, 0));
            
            row = 0;
            addDetailRow(serviceGrid, row++, "Booking ID:", booking.getHexId(), false);
            addDetailRow(serviceGrid, row++, "Mechanic:", booking.getMechanic().getName(), false);
            addDetailRow(serviceGrid, row++, "Booking Date:", booking.getDate().toString(), false);
            addDetailRow(serviceGrid, row++, "Booking Time:", booking.getTime(), false);
            addDetailRow(serviceGrid, row++, "Status:", booking.getStatus().toUpperCase(), false);
            
            serviceSection.getChildren().add(serviceGrid);
            
            // Add services table
            try {
                List<java.util.Map<String, String>> services = bookingService.getBookingServices(booking.getId());
                if (!services.isEmpty()) {
                    Label servicesLabel = new Label("Services Performed:");
                    servicesLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
                    serviceSection.getChildren().add(servicesLabel);
                    
                    TableView<java.util.Map<String, String>> servicesTable = new TableView<>();
                    servicesTable.setPrefHeight(150);
                    servicesTable.setItems(FXCollections.observableArrayList(services));
                    servicesTable.setStyle("-fx-background-color: white;");
                    
                    TableColumn<java.util.Map<String, String>, String> serviceTypeCol = new TableColumn<>("Service Type");
                    serviceTypeCol.setCellValueFactory(cellData -> 
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("type")));
                    serviceTypeCol.setPrefWidth(200);
                    
                    TableColumn<java.util.Map<String, String>, String> serviceDescCol = new TableColumn<>("Description");
                    serviceDescCol.setCellValueFactory(cellData -> 
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("description")));
                    serviceDescCol.setPrefWidth(400);
                    serviceDescCol.setCellFactory(col -> new TableCell<java.util.Map<String, String>, String>() {
                        @Override
                        protected void updateItem(String desc, boolean empty) {
                            super.updateItem(desc, empty);
                            if (empty || desc == null || desc.trim().isEmpty()) {
                                setText("No description provided");
                                setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                            } else {
                                setText(desc);
                                setStyle("-fx-text-fill: black;");
                            }
                        }
                    });
                    
                    servicesTable.getColumns().addAll(serviceTypeCol, serviceDescCol);
                    serviceSection.getChildren().add(servicesTable);
                }
            } catch (SQLException e) {
                Label errorLabel = new Label("Could not load services: " + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: #cc0000;");
                serviceSection.getChildren().add(errorLabel);
            }
            
            mainContainer.getChildren().add(serviceSection);
            
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
                    
                    @SuppressWarnings("unchecked")
                    TableColumn<BookingPart, ?>[] columns = new TableColumn[] {nameCol, qtyCol, priceCol, totalCol};
                    partsTable.getColumns().addAll(columns);
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
            
            // === BILLING SUMMARY SECTION ===
            VBox billingSummarySection = new VBox(10);
            billingSummarySection.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 5;");
            Label billingSummaryHeader = new Label("💵 BILLING SUMMARY");
            billingSummaryHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            billingSummarySection.getChildren().add(billingSummaryHeader);
            
            GridPane billingSummaryGrid = new GridPane();
            billingSummaryGrid.setHgap(15);
            billingSummaryGrid.setVgap(8);
            billingSummaryGrid.setPadding(new Insets(10, 0, 0, 0));
            
            row = 0;
            try {
                List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                double partsCost = parts.stream().mapToDouble(BookingPart::getTotalCost).sum();
                
                // Service charge
                double serviceCharge = bill.getAmount() - partsCost;
                
                addDetailRow(billingSummaryGrid, row++, "Service Charge:", "₱" + String.format("%.2f", serviceCharge), false);
                addDetailRow(billingSummaryGrid, row++, "Parts Cost:", "₱" + String.format("%.2f", partsCost), false);
            } catch (SQLException e) {
                System.err.println("Could not calculate parts cost: " + e.getMessage());
            }
            
            addDetailRow(billingSummaryGrid, row++, "Payment Status:", bill.getPaymentStatus(), false);
            
            // Add payment method if available
            if (bill.getPaymentMethod() != null && !bill.getPaymentMethod().isEmpty()) {
                addDetailRow(billingSummaryGrid, row++, "Payment Method:", bill.getPaymentMethod(), false);
            }
            
            // Add reference number if available
            if (bill.getReferenceNumber() != null && !bill.getReferenceNumber().isEmpty()) {
                Label refLabel = new Label("Reference:");
                refLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");
                Label refValue = new Label(bill.getReferenceNumber());
                refValue.setWrapText(true);
                refValue.setMaxWidth(400);
                billingSummaryGrid.add(refLabel, 0, row);
                billingSummaryGrid.add(refValue, 1, row++);
            }
            
            billingSummarySection.getChildren().add(billingSummaryGrid);
            
            Label totalAmountLabel = new Label("TOTAL AMOUNT DUE: ₱" + String.format("%.2f", bill.getAmount()));
            totalAmountLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1B5E20; -fx-padding: 10 0 0 0;");
            billingSummarySection.getChildren().add(totalAmountLabel);
            
            mainContainer.getChildren().add(billingSummarySection);
            
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
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bill details: " + e.getMessage());
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
    
    private void processPayment(Bill bill) {
        try {
            // Create a dialog to choose payment method
            Dialog<String> paymentDialog = new Dialog<>();
            paymentDialog.setTitle("Payment Method");
            paymentDialog.setHeaderText("Select Payment Method");
            paymentDialog.setContentText("Choose how to process the payment:");
            
            VBox dialogContent = new VBox(15);
            dialogContent.setPadding(new Insets(20));
            
            Label amountLabel = new Label("Amount: ₱" + String.format("%.2f", bill.getAmount()));
            amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            dialogContent.getChildren().add(amountLabel);
            
            // Cash Payment Button
            Button cashBtn = new Button("💵 Cash Payment");
            cashBtn.setPrefWidth(250);
            cashBtn.setPrefHeight(50);
            cashBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
            
            // Bank Transfer/Online Payment Button
            Button bankBtn = new Button("🏦 Bank Transfer / Online Payment");
            bankBtn.setPrefWidth(250);
            bankBtn.setPrefHeight(50);
            bankBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
            
            dialogContent.getChildren().addAll(cashBtn, bankBtn);
            
            paymentDialog.getDialogPane().setContent(dialogContent);
            paymentDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            
            // Cash payment handler
            cashBtn.setOnAction(e -> {
                paymentDialog.close();
                processCashPayment(bill);
            });
            
            // Bank transfer handler
            bankBtn.setOnAction(e -> {
                paymentDialog.close();
                processBankTransferPayment(bill);
            });
            
            paymentDialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to process payment: " + e.getMessage());
        }
    }
    
    private void processCashPayment(Bill bill) {
        javafx.application.Platform.runLater(() -> {
            try {
                // Show confirmation dialog for cash payment
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("Cash Payment Confirmation");
                confirmDialog.setHeaderText("Confirm Cash Payment");
                
                // Create custom content with better sizing
                VBox content = new VBox(15);
                content.setPadding(new Insets(20));
                content.setMinWidth(400);
                content.setMinHeight(150);
                
                Label billLabel = new Label("Bill ID: " + bill.getHexId());
                billLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                
                Label amountLabel = new Label("Amount: ₱" + String.format("%.2f", bill.getAmount()));
                amountLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1B5E20;");
                
                Label confirmLabel = new Label("Confirm cash payment received?");
                confirmLabel.setStyle("-fx-font-size: 12px;");
                
                content.getChildren().addAll(billLabel, amountLabel, confirmLabel);
                
                confirmDialog.getDialogPane().setContent(content);
                confirmDialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                
                confirmDialog.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            boolean success = billingService.updateBillPayment(bill.getId(), "Paid", "Cash", null);
                            if (success) {
                                bill.setPaymentStatus("Paid");
                                bill.setPaymentMethod("Cash");
                                billTable.refresh();
                                statusLabel.setText("Cash payment processed for bill " + bill.getHexId());
                                
                                showAlert(Alert.AlertType.INFORMATION, 
                                        "Payment Processed", 
                                        "Cash payment has been successfully processed.");
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
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to process cash payment: " + e.getMessage());
            }
        });
    }
    
    private void processBankTransferPayment(Bill bill) {
        javafx.application.Platform.runLater(() -> {
            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Bank Transfer / Online Payment");
            dialog.setHeaderText("Enter Payment Details");
            
            // Main container
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(15);
            grid.setPadding(new Insets(20));
            
            // Amount
            Label amountLabel = new Label("Amount: ₱" + String.format("%.2f", bill.getAmount()));
            amountLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            grid.add(amountLabel, 0, 0, 2, 1);
            
            // Payment method
            Label methodLabel = new Label("Payment Method:");
            ComboBox<String> methodCombo = new ComboBox<>();
            methodCombo.getItems().addAll("Bank Transfer", "Online Payment", "E-wallet");
            methodCombo.setValue("Bank Transfer");
            methodCombo.setPrefWidth(300);
            methodCombo.setEditable(false);
            grid.add(methodLabel, 0, 1);
            grid.add(methodCombo, 1, 1);
            
            // Reference number
            Label refLabel = new Label("Reference Number: *");
            TextField refField = new TextField();
            refField.setPromptText("Enter transaction ID");
            refField.setPrefWidth(300);
            grid.add(refLabel, 0, 2);
            grid.add(refField, 1, 2);
            
            // Bank/Provider
            Label bankLabel = new Label("Bank/Provider:");
            TextField bankField = new TextField();
            bankField.setPromptText("E.g., BDO, BPI, GCash");
            bankField.setPrefWidth(300);
            grid.add(bankLabel, 0, 3);
            grid.add(bankField, 1, 3);
            
            // Notes
            Label notesLabel = new Label("Notes:");
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Additional notes");
            notesArea.setPrefWidth(300);
            notesArea.setPrefHeight(80);
            notesArea.setWrapText(true);
            grid.add(notesLabel, 0, 4);
            grid.add(notesArea, 1, 4);
            
            // Set dialog content and buttons
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Show and process - this is BLOCKING
            Optional<ButtonType> result = dialog.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String method = methodCombo.getValue();
                String refNumber = refField.getText().trim();
                String bankName = bankField.getText().trim();
                String notes = notesArea.getText().trim();
                
                if (refNumber.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Missing Information", 
                             "Please enter a reference number.");
                    return;
                }
                
                String fullReference = refNumber;
                if (!bankName.isEmpty()) fullReference += " (" + bankName + ")";
                if (!notes.isEmpty()) fullReference += " - " + notes;
                
                try {
                    boolean success = billingService.updateBillPayment(
                        bill.getId(), "Paid", method, fullReference);
                    
                    if (success) {
                        bill.setPaymentStatus("Paid");
                        bill.setPaymentMethod(method);
                        bill.setReferenceNumber(fullReference);
                        billTable.refresh();
                        statusLabel.setText(method + " payment processed for " + bill.getHexId());
                        
                        showAlert(Alert.AlertType.INFORMATION, "Payment Processed", 
                                 method + " payment recorded.\n\nRef: " + refNumber);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Payment Error", 
                                 "Failed to process payment.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error", 
                             "Failed to update payment: " + ex.getMessage());
                }
            }
        });
    }
    
    private void printReceipt(Bill bill) {
        try {
            // Fetch booking details for receipt
            ServiceBookingService bookingService = new ServiceBookingService();
            ServiceBookingViewModel booking = bookingService.getBookingById(bill.getServiceId());
            
            // Create a dialog to display the receipt
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Receipt - " + bill.getHexId());
            dialog.setHeaderText(null);
            
            // Create scrollable receipt content
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(600);
            
            VBox mainContainer = new VBox(0);
            mainContainer.setStyle("-fx-background-color: white;");
            
            // === RECEIPT HEADER ===
            VBox headerBox = new VBox(5);
            headerBox.setStyle("-fx-background-color: #1976D2; -fx-padding: 20; -fx-alignment: center;");
            
            Label companyLabel = new Label("AUTOTECH SERVICE CENTER");
            companyLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            Label taglineLabel = new Label("Professional Vehicle Maintenance & Repair");
            taglineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E3F2FD;");
            
            headerBox.getChildren().addAll(companyLabel, taglineLabel);
            mainContainer.getChildren().add(headerBox);
            
            // === RECEIPT CONTENT ===
            VBox contentBox = new VBox(15);
            contentBox.setPadding(new Insets(20));
            contentBox.setStyle("-fx-background-color: white;");
            
            // Receipt title and number
            HBox titleBox = new HBox();
            titleBox.setSpacing(50);
            Label receiptTitleLabel = new Label("RECEIPT");
            receiptTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            Label receiptNumberLabel = new Label("Receipt #: " + bill.getHexId());
            receiptNumberLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
            titleBox.getChildren().addAll(receiptTitleLabel, receiptNumberLabel);
            contentBox.getChildren().add(titleBox);
            
            // Separator
            Separator sep1 = new Separator();
            sep1.setStyle("-fx-padding: 0;");
            contentBox.getChildren().add(sep1);
            
            // Receipt date and time
            HBox dateBox = new HBox(50);
            Label dateLabel = new Label("Date: " + bill.getBillDate());
            dateLabel.setStyle("-fx-font-size: 11px;");
            Label timeLabel = new Label("Time: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            timeLabel.setStyle("-fx-font-size: 11px;");
            dateBox.getChildren().addAll(dateLabel, timeLabel);
            contentBox.getChildren().add(dateBox);
            
            contentBox.getChildren().add(new Label(" "));
            
            // === CUSTOMER & VEHICLE SECTION ===
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(20);
            infoGrid.setVgap(8);
            
            // Set column constraints to prevent overlapping
            ColumnConstraints leftCol = new ColumnConstraints();
            leftCol.setMinWidth(300);
            leftCol.setPrefWidth(350);
            leftCol.setMaxWidth(400);
            
            ColumnConstraints rightCol = new ColumnConstraints();
            rightCol.setMinWidth(300);
            rightCol.setPrefWidth(350);
            rightCol.setMaxWidth(400);
            
            infoGrid.getColumnConstraints().addAll(leftCol, rightCol);
            
            Label customerTitleLabel = new Label("CUSTOMER INFORMATION");
            customerTitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            infoGrid.add(customerTitleLabel, 0, 0);
            
            Label customerNameLabel = new Label(bill.getCustomerName());
            customerNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            customerNameLabel.setWrapText(true);
            infoGrid.add(customerNameLabel, 0, 1);
            
            Label vehicleLabel = new Label(bill.getVehicleInfo());
            vehicleLabel.setStyle("-fx-font-size: 11px;");
            vehicleLabel.setWrapText(true);
            infoGrid.add(vehicleLabel, 0, 2);
            
            // Service info on right side
            Label serviceTitleLabel = new Label("SERVICE DETAILS");
            serviceTitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            infoGrid.add(serviceTitleLabel, 1, 0);
            
            if (booking != null) {
                // Get services from database instead of using deprecated getServiceType()
                try {
                    List<java.util.Map<String, String>> services = bookingService.getBookingServices(booking.getId());
                    if (!services.isEmpty()) {
                        VBox servicesBox = new VBox(3);
                        for (java.util.Map<String, String> service : services) {
                            Label serviceLabel = new Label("• " + service.get("type"));
                            serviceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                            serviceLabel.setWrapText(true);
                            servicesBox.getChildren().add(serviceLabel);
                        }
                        infoGrid.add(servicesBox, 1, 1);
                    }
                } catch (Exception e) {
                    Label serviceTypeLabel = new Label("Service details unavailable");
                    serviceTypeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                    infoGrid.add(serviceTypeLabel, 1, 1);
                }
                
                Label mechanicLabel = new Label("Mechanic: " + booking.getMechanic().getName());
                mechanicLabel.setStyle("-fx-font-size: 10px;");
                mechanicLabel.setWrapText(true);
                infoGrid.add(mechanicLabel, 1, 2);
            }
            
            contentBox.getChildren().add(infoGrid);
            contentBox.getChildren().add(new Label(" "));
            
            // === SEPARATOR LINE ===
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-padding: 5 0;");
            contentBox.getChildren().add(sep2);
            
            // === BILLING DETAILS ===
            GridPane billingGrid = new GridPane();
            billingGrid.setHgap(20);
            billingGrid.setVgap(8);
            
            int billingRow = 0;
            Label billIdLabel = new Label("Bill ID:");
            billIdLabel.setStyle("-fx-font-weight: bold;");
            billingGrid.add(billIdLabel, 0, billingRow);
            Label billIdValueLabel = new Label(bill.getHexId());
            billIdValueLabel.setStyle("-fx-font-size: 11px;");
            billingGrid.add(billIdValueLabel, 1, billingRow++);
            
            Label amountTitleLabel = new Label("Amount:");
            amountTitleLabel.setStyle("-fx-font-weight: bold;");
            billingGrid.add(amountTitleLabel, 0, billingRow);
            Label amountValueLabel = new Label("₱" + String.format("%.2f", bill.getAmount()));
            amountValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1B5E20;");
            billingGrid.add(amountValueLabel, 1, billingRow++);
            
            Label statusTitleLabel = new Label("Payment Status:");
            statusTitleLabel.setStyle("-fx-font-weight: bold;");
            billingGrid.add(statusTitleLabel, 0, billingRow);
            Label statusValueLabel = new Label(bill.getPaymentStatus());
            statusValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: green;");
            billingGrid.add(statusValueLabel, 1, billingRow++);
            
            // Payment method if available
            if (bill.getPaymentMethod() != null && !bill.getPaymentMethod().isEmpty()) {
                Label methodTitleLabel = new Label("Payment Method:");
                methodTitleLabel.setStyle("-fx-font-weight: bold;");
                billingGrid.add(methodTitleLabel, 0, billingRow);
                Label methodValueLabel = new Label(bill.getPaymentMethod());
                methodValueLabel.setStyle("-fx-font-size: 11px;");
                billingGrid.add(methodValueLabel, 1, billingRow++);
            }
            
            contentBox.getChildren().add(billingGrid);
            contentBox.getChildren().add(new Label(" "));
            
            // === PARTS & MATERIALS (if any) ===
            try {
                List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                if (!parts.isEmpty()) {
                    Label partsHeaderLabel = new Label("PARTS & MATERIALS USED:");
                    partsHeaderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #1976D2;");
                    contentBox.getChildren().add(partsHeaderLabel);
                    
                    GridPane partsGrid = new GridPane();
                    partsGrid.setHgap(15);
                    partsGrid.setVgap(5);
                    partsGrid.setPadding(new Insets(5, 0, 0, 0));
                    
                    // Header row
                    Label partNameHeader = new Label("Part");
                    partNameHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
                    Label qtyHeader = new Label("Qty");
                    qtyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
                    Label priceHeader = new Label("Unit Price");
                    priceHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
                    Label subtotalHeader = new Label("Subtotal");
                    subtotalHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
                    
                    partsGrid.add(partNameHeader, 0, 0);
                    partsGrid.add(qtyHeader, 1, 0);
                    partsGrid.add(priceHeader, 2, 0);
                    partsGrid.add(subtotalHeader, 3, 0);
                    
                    int partRow = 1;
                    double totalPartsCost = 0;
                    for (BookingPart part : parts) {
                        Label partNameLabel = new Label(part.getPartName());
                        partNameLabel.setStyle("-fx-font-size: 10px;");
                        partsGrid.add(partNameLabel, 0, partRow);
                        
                        Label qtyLabel = new Label(String.valueOf(part.getQuantity()));
                        qtyLabel.setStyle("-fx-font-size: 10px; -fx-alignment: center;");
                        partsGrid.add(qtyLabel, 1, partRow);
                        
                        Label priceLabel = new Label("₱" + String.format("%.2f", part.getPrice()));
                        priceLabel.setStyle("-fx-font-size: 10px; -fx-alignment: center-right;");
                        partsGrid.add(priceLabel, 2, partRow);
                        
                        double subtotal = part.getTotalCost();
                        totalPartsCost += subtotal;
                        Label subtotalLabel = new Label("₱" + String.format("%.2f", subtotal));
                        subtotalLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-alignment: center-right;");
                        partsGrid.add(subtotalLabel, 3, partRow);
                        
                        partRow++;
                    }
                    
                    contentBox.getChildren().add(partsGrid);
                    
                    Label totalPartsLabel = new Label("Total Parts: ₱" + String.format("%.2f", totalPartsCost));
                    totalPartsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");
                    contentBox.getChildren().add(totalPartsLabel);
                    contentBox.getChildren().add(new Label(" "));
                }
            } catch (SQLException e) {
                System.err.println("Error loading parts: " + e.getMessage());
            }
            
            // === SEPARATOR ===
            Separator sep3 = new Separator();
            sep3.setStyle("-fx-padding: 5 0;");
            contentBox.getChildren().add(sep3);
            
            // === TOTAL AMOUNT (Prominent Display) ===
            VBox totalBox = new VBox(10);
            totalBox.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 15; -fx-border-color: #4CAF50; -fx-border-width: 2;");
            totalBox.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label totalLabelText = new Label("TOTAL AMOUNT DUE");
            totalLabelText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            
            Label totalAmount = new Label("₱" + String.format("%.2f", bill.getAmount()));
            totalAmount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1B5E20;");
            
            totalBox.getChildren().addAll(totalLabelText, totalAmount);
            contentBox.getChildren().add(totalBox);
            
            contentBox.getChildren().add(new Label(" "));
            contentBox.getChildren().add(new Label(" "));
            
            // === FOOTER MESSAGE ===
            Label footerLabel = new Label("Thank you for choosing AutoTech!");
            footerLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #666;");
            footerLabel.setWrapText(true);
            contentBox.getChildren().add(footerLabel);
            
            Label contactLabel = new Label("For inquiries, please contact us at admin@autotech.com");
            contactLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            contentBox.getChildren().add(contactLabel);
            
            scrollPane.setContent(contentBox);
            
            // === DIALOG BUTTONS ===
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().clear();
            
            ButtonType printButton = new ButtonType("🖨️ Print", ButtonBar.ButtonData.LEFT);
            ButtonType emailButton = new ButtonType("📧 Email Receipt", ButtonBar.ButtonData.LEFT);
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            dialog.getDialogPane().getButtonTypes().addAll(printButton, emailButton, closeButton);
            
            dialog.getDialogPane().setMinWidth(700);
            dialog.getDialogPane().setMinHeight(700);
            
            // Button handlers
            dialog.setOnShown(e -> {
                Button printBtn = (Button) dialog.getDialogPane().lookupButton(printButton);
                printBtn.setOnAction(event -> {
                    printReceiptToPrinter(bill, booking, contentBox);
                    dialog.close();
                });
                
                Button emailBtn = (Button) dialog.getDialogPane().lookupButton(emailButton);
                emailBtn.setOnAction(event -> {
                    sendReceiptEmail(bill, booking);
                    dialog.close();
                });
            });
            
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate receipt: " + e.getMessage());
        }
    }
    
    private void printReceiptToPrinter(Bill bill, ServiceBookingViewModel booking, VBox receiptContent) {
        try {
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            if (printerJob != null && printerJob.showPrintDialog(null)) {
                boolean success = printerJob.printPage(receiptContent);
                if (success) {
                    printerJob.endJob();
                    statusLabel.setText("Receipt printed successfully for bill " + bill.getHexId());
                    showAlert(Alert.AlertType.INFORMATION, "Print Successful", 
                             "Receipt has been sent to the printer.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Print Error", 
                             "Failed to print receipt.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Print Error", 
                     "Error while printing: " + e.getMessage());
        }
    }
    
    private void sendReceiptEmail(Bill bill, ServiceBookingViewModel booking) {
        try {
            // Get customer email
            Customer customer = CustomerService.getInstance().getCustomerById(bill.getCustomerId());
            if (customer == null || customer.getEmail() == null || customer.getEmail().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Email", 
                         "Customer email not found. Please add an email address for this customer.");
                return;
            }
            
            ServiceBookingService bookingService = new ServiceBookingService();
            
            // Build receipt HTML
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><body style='font-family: Arial, sans-serif; background-color: #f5f5f5;'>");
            htmlContent.append("<div style='max-width: 600px; margin: 20px auto; background-color: white; border: 1px solid #ddd;'>");
            
            // Header
            htmlContent.append("<div style='background-color: #1976D2; color: white; padding: 20px; text-align: center;'>");
            htmlContent.append("<h1 style='margin: 0; font-size: 24px;'>AUTOTECH SERVICE CENTER</h1>");
            htmlContent.append("<p style='margin: 5px 0 0 0; font-size: 12px;'>Professional Vehicle Maintenance & Repair</p>");
            htmlContent.append("</div>");
            
            // Content
            htmlContent.append("<div style='padding: 20px;'>");
            
            // Receipt title
            htmlContent.append("<div style='display: flex; justify-content: space-between; margin-bottom: 20px;'>");
            htmlContent.append("<h2 style='margin: 0; color: #333;'>RECEIPT</h2>");
            htmlContent.append("<p style='margin: 0; font-weight: bold;'>Receipt #: ").append(bill.getHexId()).append("</p>");
            htmlContent.append("</div>");
            
            // Date and time
            htmlContent.append("<p style='margin: 5px 0; font-size: 12px; color: #666;'>");
            htmlContent.append("Date: ").append(bill.getBillDate()).append(" | Time: ");
            htmlContent.append(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            htmlContent.append("</p>");
            
            htmlContent.append("<hr style='border: none; border-top: 1px solid #ddd; margin: 15px 0'>");
            
            // Customer and service info
            htmlContent.append("<table style='width: 100%; margin-bottom: 15px;'>");
            htmlContent.append("<tr>");
            htmlContent.append("<td style='vertical-align: top; width: 50%;'>");
            htmlContent.append("<p style='margin: 0 0 5px 0; font-weight: bold; color: #1976D2;'>CUSTOMER INFORMATION</p>");
            htmlContent.append("<p style='margin: 5px 0; font-weight: bold;'>").append(bill.getCustomerName()).append("</p>");
            htmlContent.append("<p style='margin: 5px 0; font-size: 12px;'>").append(bill.getVehicleInfo()).append("</p>");
            htmlContent.append("</td>");
            htmlContent.append("<td style='vertical-align: top; width: 50%;'>");
            htmlContent.append("<p style='margin: 0 0 5px 0; font-weight: bold; color: #1976D2;'>SERVICE DETAILS</p>");
            
            // Get and display services
            if (booking != null) {
                try {
                    List<java.util.Map<String, String>> services = bookingService.getBookingServices(booking.getId());
                    if (!services.isEmpty()) {
                        VBox servicesBox = new VBox(3);
                        for (java.util.Map<String, String> service : services) {
                            htmlContent.append("<p style='margin: 5px 0; font-weight: bold;'>• ").append(service.get("type")).append("</p>");
                        }
                    }
                    htmlContent.append("<p style='margin: 5px 0; font-size: 12px;'>Mechanic: ").append(booking.getMechanic().getName()).append("</p>");
                } catch (Exception e) {
                    htmlContent.append("<p style='margin: 5px 0;'>Service details unavailable</p>");
                }
            }
            htmlContent.append("</td>");
            htmlContent.append("</tr>");
            htmlContent.append("</table>");
            
            htmlContent.append("<hr style='border: none; border-top: 1px solid #ddd; margin: 15px 0'>");
            
            // Billing details
            htmlContent.append("<table style='width: 100%; margin-bottom: 15px; font-size: 13px;'>");
            htmlContent.append("<tr><td style='font-weight: bold;'>Bill ID:</td><td>").append(bill.getHexId()).append("</td></tr>");
            htmlContent.append("<tr><td style='font-weight: bold;'>Amount:</td><td style='font-weight: bold; color: #1B5E20;'>₱").append(String.format("%.2f", bill.getAmount())).append("</td></tr>");
            htmlContent.append("<tr><td style='font-weight: bold;'>Payment Status:</td><td style='color: green;'>").append(bill.getPaymentStatus()).append("</td></tr>");
            if (bill.getPaymentMethod() != null && !bill.getPaymentMethod().isEmpty()) {
                htmlContent.append("<tr><td style='font-weight: bold;'>Payment Method:</td><td>").append(bill.getPaymentMethod()).append("</td></tr>");
            }
            htmlContent.append("</table>");
            
            // Parts (if any)
            try {
                List<BookingPart> parts = bookingService.getBookingParts(booking.getId());
                if (!parts.isEmpty()) {
                    htmlContent.append("<div style='margin-bottom: 15px;'>");
                    htmlContent.append("<p style='margin: 0 0 10px 0; font-weight: bold; color: #1976D2;'>PARTS & MATERIALS USED</p>");
                    htmlContent.append("<table style='width: 100%; border-collapse: collapse; font-size: 12px;'>");
                    htmlContent.append("<tr style='background-color: #f0f0f0;'>");
                    htmlContent.append("<th style='text-align: left; padding: 8px; border: 1px solid #ddd;'>Part</th>");
                    htmlContent.append("<th style='text-align: center; padding: 8px; border: 1px solid #ddd;'>Qty</th>");
                    htmlContent.append("<th style='text-align: right; padding: 8px; border: 1px solid #ddd;'>Unit Price</th>");
                    htmlContent.append("<th style='text-align: right; padding: 8px; border: 1px solid #ddd;'>Subtotal</th>");
                    htmlContent.append("</tr>");
                    
                    double totalPartsCost = 0;
                    for (BookingPart part : parts) {
                        htmlContent.append("<tr>");
                        htmlContent.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(part.getPartName()).append("</td>");
                        htmlContent.append("<td style='text-align: center; padding: 8px; border: 1px solid #ddd;'>").append(part.getQuantity()).append("</td>");
                        htmlContent.append("<td style='text-align: right; padding: 8px; border: 1px solid #ddd;'>₱").append(String.format("%.2f", part.getPrice())).append("</td>");
                        htmlContent.append("<td style='text-align: right; padding: 8px; border: 1px solid #ddd; font-weight: bold;'>₱").append(String.format("%.2f", part.getTotalCost())).append("</td>");
                        htmlContent.append("</tr>");
                        totalPartsCost += part.getTotalCost();
                    }
                    
                    htmlContent.append("</table>");
                    htmlContent.append("<p style='margin: 10px 0 0 0; text-align: right; font-weight: bold;'>Total Parts: ₱").append(String.format("%.2f", totalPartsCost)).append("</p>");
                    htmlContent.append("</div>");
                }
            } catch (SQLException e) {
                System.err.println("Error loading parts: " + e.getMessage());
            }
            
            htmlContent.append("<hr style='border: none; border-top: 2px solid #ddd; margin: 15px 0'>");
            
            // Total amount
            htmlContent.append("<div style='background-color: #E8F5E9; padding: 15px; text-align: center; border: 2px solid #4CAF50; margin-bottom: 15px;'>");
            htmlContent.append("<p style='margin: 0 0 10px 0; font-weight: bold; color: #2E7D32;'>TOTAL AMOUNT DUE</p>");
            htmlContent.append("<p style='margin: 0; font-size: 24px; font-weight: bold; color: #1B5E20;'>₱").append(String.format("%.2f", bill.getAmount())).append("</p>");
            htmlContent.append("</div>");
            
            // Footer
            htmlContent.append("<p style='margin: 15px 0 5px 0; font-size: 12px; color: #666; text-align: center; font-style: italic;'>Thank you for choosing AutoTech!</p>");
            htmlContent.append("<p style='margin: 0; font-size: 11px; color: #999; text-align: center;'>For inquiries: admin@autotech.com</p>");
            
            htmlContent.append("</div>");
            htmlContent.append("</div></body></html>");
            
            // Send email
            EmailService emailService = EmailService.getInstance();
            boolean emailSent = emailService.sendReceiptEmail(customer.getEmail(), bill.getHexId(), bill.getCustomerName(), 
                                        "₱" + String.format("%.2f", bill.getAmount()), htmlContent.toString());
            
            // Log email to database
            EmailHistoryService.logEmailSent(bill.getId(), customer.getEmail(), "Receipt", 
                                            "Receipt #" + bill.getHexId() + " - Service Receipt", 
                                            emailSent ? "sent" : "failed", null);
            
            statusLabel.setText("Receipt emailed successfully to " + customer.getEmail());
            showAlert(Alert.AlertType.INFORMATION, "Email Sent", 
                     "Receipt has been sent to " + customer.getEmail());
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Email Error", 
                     "Error retrieving customer information: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Email Error", 
                     "Error sending receipt: " + e.getMessage());
        }
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
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}