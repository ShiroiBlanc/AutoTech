package com.example;

import javafx.application.Platform;
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
import java.util.List;

public class InventoryController {
    @FXML private Button addItemButton;
    @FXML private Pagination inventoryPagination;
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> partNumberColumn;
    @FXML private TableColumn<InventoryItem, String> nameColumn;
    @FXML private TableColumn<InventoryItem, String> categoryColumn;
    @FXML private TableColumn<InventoryItem, Integer> quantityColumn;
    @FXML private TableColumn<InventoryItem, Integer> reservedColumn;
    @FXML private TableColumn<InventoryItem, Integer> availableColumn;
    @FXML private TableColumn<InventoryItem, String> expirationColumn;
    @FXML private TableColumn<InventoryItem, Double> priceColumn;
    @FXML private TableColumn<InventoryItem, String> locationColumn;
    @FXML private TableColumn<InventoryItem, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    
    private ObservableList<InventoryItem> inventoryList = FXCollections.observableArrayList();
    private ObservableList<InventoryItem> allInventoryItems = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 25;
    private InventoryService inventoryService = InventoryService.getInstance();

    @FXML
    public void initialize() {
        // Check user role and hide Add button for cashiers
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.CASHIER) {
            if (addItemButton != null) {
                addItemButton.setVisible(false);
                addItemButton.setManaged(false);
            }
        }
        
        // Initialize columns - use hexId instead of partNumber for ID display
        partNumberColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        reservedColumn.setCellValueFactory(new PropertyValueFactory<>("reservedQuantity"));
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        
        // Custom cell factory for expiration date with color coding
        expirationColumn.setCellValueFactory(cellData -> {
            InventoryItem item = cellData.getValue();
            if (item.getExpirationDate() == null) {
                return new javafx.beans.property.SimpleStringProperty("N/A");
            }
            return new javafx.beans.property.SimpleStringProperty(item.getExpirationDate().toString());
        });
        
        expirationColumn.setCellFactory(column -> new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("N/A")) {
                    setText(item);
                    setStyle("");
                } else {
                    setText(item);
                    InventoryItem inventoryItem = getTableView().getItems().get(getIndex());
                    if (inventoryItem.isExpired()) {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (inventoryItem.isNearExpiration()) {
                        setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        // Set up actions column
        setupActionsColumn();
        
        // Set table resize policy
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Load data from database
        loadInventoryData();
        
        // Set items to table
        inventoryTable.setItems(inventoryList);
        
        // Setup pagination
        setupPagination();
    }
    
    private void setupPagination() {
        if (inventoryPagination != null) {
            inventoryPagination.setPageFactory(pageIndex -> {
                updateTablePage(pageIndex);
                return inventoryTable;
            });
        }
    }
    
    private void updateTablePage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allInventoryItems.size());
        
        inventoryList.clear();
        if (fromIndex < allInventoryItems.size()) {
            inventoryList.addAll(allInventoryItems.subList(fromIndex, toIndex));
        }
    }
    
    private void updatePaginationControl() {
        if (inventoryPagination != null) {
            int pageCount = (int) Math.ceil((double) allInventoryItems.size() / ITEMS_PER_PAGE);
            inventoryPagination.setPageCount(Math.max(1, pageCount));
        }
    }

    private void loadInventoryData() {
        try {
            List<InventoryItem> items = inventoryService.getAllItems();
            allInventoryItems.clear();
            allInventoryItems.addAll(items);
            
            // Update pagination control
            updatePaginationControl();
            
            // Update table - will show first page
            if (inventoryPagination != null) {
                updateTablePage(0);
                inventoryPagination.setCurrentPageIndex(0);
            } else {
                inventoryList.clear();
                inventoryList.addAll(items);
            }
            
            statusLabel.setText("Inventory loaded successfully. Total items: " + items.size());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load inventory: " + e.getMessage());
        }
    }
    
    private void setupActionsColumn() {
        // Make part number column clickable
        partNumberColumn.setCellFactory(column -> new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setOnMouseClicked(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #0066cc; -fx-underline: true; -fx-cursor: hand;");
                    setOnMouseClicked(event -> {
                        InventoryItem inventoryItem = getTableView().getItems().get(getIndex());
                        if (inventoryItem != null) {
                            viewItemDetails(inventoryItem);
                        }
                    });
                }
            }
        });
        
        actionsColumn.setCellFactory(col -> new TableCell<InventoryItem, Void>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button restockButton = new Button("Restock");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttonBox = new HBox(5, viewButton, editButton, restockButton, deleteButton);
            
            {
                buttonBox.setAlignment(Pos.CENTER);
                
                viewButton.setOnAction(e -> {
                    InventoryItem item = getTableRow().getItem();
                    if (item != null) {
                        viewItemDetails(item);
                    }
                });
                
                editButton.setOnAction(e -> {
                    InventoryItem item = getTableRow().getItem();
                    if (item != null) {
                        editItem(item);
                    }
                });
                
                restockButton.setOnAction(e -> {
                    InventoryItem item = getTableRow().getItem();
                    if (item != null) {
                        restockItem(item);
                    }
                });

                deleteButton.setOnAction(e -> {
                    InventoryItem item = getTableRow().getItem();
                    if (item == null) return;

                    // Permission check: only non-cashier users may delete
                    User currentUser = UserService.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.getRole() == User.UserRole.CASHIER) {
                        showAlert(Alert.AlertType.ERROR, "Access Denied", "Cashiers do not have permission to delete inventory items.");
                        return;
                    }

                    // First: basic confirmation
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Delete");
                    confirmAlert.setHeaderText("Delete inventory item");
                    confirmAlert.setContentText("Are you sure you want to permanently delete '" + item.getName() + "' (" + item.getHexId() + ")? This action cannot be undone.");

                    ButtonType yesButton = new ButtonType("Yes, Continue", ButtonBar.ButtonData.OK_DONE);
                    ButtonType noButton = new ButtonType("No, Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirmAlert.getButtonTypes().setAll(yesButton, noButton);

                    var first = confirmAlert.showAndWait();
                    if (first.isEmpty() || first.get() != yesButton) {
                        return; // user cancelled
                    }

                    // Second: CAPTCHA-style math verification
                    java.util.Random random = new java.util.Random();
                    int num1 = random.nextInt(9) + 1; // 1-9
                    int num2 = random.nextInt(9) + 1; // 1-9
                    int correctAnswer = num1 + num2;

                    Dialog<String> captchaDialog = new Dialog<>();
                    captchaDialog.setTitle("Verification Required");
                    captchaDialog.setHeaderText("Please solve this math problem to confirm deletion");

                    ButtonType verifyButton = new ButtonType("Verify", ButtonBar.ButtonData.OK_DONE);
                    captchaDialog.getDialogPane().getButtonTypes().addAll(verifyButton, ButtonType.CANCEL);

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(20));

                    Label questionLabel = new Label("What is " + num1 + " + " + num2 + " ?");
                    questionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                    TextField answerField = new TextField();
                    answerField.setPromptText("Enter your answer");
                    answerField.setPrefWidth(200);

                    Label warning = new Label("⚠ This verification helps prevent accidental or automated deletions.");
                    warning.setWrapText(true);
                    warning.setMaxWidth(360);
                    warning.setStyle("-fx-text-fill: #aa0000; -fx-font-size: 11px;");

                    grid.add(questionLabel, 0, 0, 2, 1);
                    grid.add(new Label("Answer:"), 0, 1);
                    grid.add(answerField, 1, 1);
                    grid.add(warning, 0, 2, 2, 1);

                    captchaDialog.getDialogPane().setContent(grid);
                    Platform.runLater(() -> answerField.requestFocus());

                    captchaDialog.setResultConverter(dialogButton -> {
                        if (dialogButton == verifyButton) {
                            return answerField.getText();
                        }
                        return null;
                    });

                    var result = captchaDialog.showAndWait();
                    if (result.isEmpty()) return; // cancelled

                    try {
                        int userAnswer = Integer.parseInt(result.get());
                        if (userAnswer != correctAnswer) {
                            showAlert(Alert.AlertType.ERROR, "Verification Failed", "Incorrect answer. Item was not deleted.");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        showAlert(Alert.AlertType.ERROR, "Verification Failed", "Invalid input. Please enter a number.");
                        return;
                    }

                    // Proceed with deletion
                    try {
                        boolean deleted = inventoryService.deleteItem(item.getId());
                        if (deleted) {
                            loadInventoryData();
                            inventoryTable.refresh();
                            showAlert(Alert.AlertType.INFORMATION, "Deleted", "Item deleted successfully.");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete item.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "An error occurred deleting the item: " + ex.getMessage());
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
                
                if (!empty) {
                    InventoryItem inventoryItem = getTableRow().getItem();
                    if (inventoryItem != null) {
                        // Highlight low stock items
                        if (inventoryItem.isLowStock()) {
                            getTableRow().setStyle("-fx-background-color: #ffcccc;");
                        } else {
                            getTableRow().setStyle("");
                        }
                    }
                }
            }
        });
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        try {
            List<InventoryItem> items = inventoryService.searchItems(searchTerm);
            inventoryTable.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to search inventory: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        searchField.clear();
    loadInventoryData();
    // Re-bind the table to the main inventory list in case a previous search replaced it
    inventoryTable.setItems(inventoryList);
    statusLabel.setText("Inventory refreshed successfully.");
    }
    
    @FXML
    private void handleAddItem() {
        // Double-check role permission
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.CASHIER) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", 
                     "Cashiers do not have permission to add inventory items.");
            return;
        }
        showInventoryItemDialog(null);
    }
    
    private void editItem(InventoryItem item) {
        showInventoryItemDialog(item);
    }
    
    private void viewItemDetails(InventoryItem item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Inventory Item Details");
        dialog.setHeaderText("Part " + item.getHexId());
        
        // Set button types
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");
        
        int row = 0;
        
        // Add details in a nice formatted way
        addDetailRow(grid, row++, "Part ID:", item.getHexId(), "-fx-font-weight: bold; -fx-font-size: 14px;");
        addDetailRow(grid, row++, "Item Name:", item.getName(), "-fx-font-weight: bold; -fx-font-size: 14px;");
        addDetailRow(grid, row++, "Category:", item.getCategory(), "");
        
        // Stock information with color coding
        addDetailRow(grid, row++, "Total Stock:", String.valueOf(item.getQuantity()), "");
        
        String reservedStyle = item.getReservedQuantity() > 0 ? "-fx-text-fill: orange; -fx-font-weight: bold;" : "";
        addDetailRow(grid, row++, "Reserved:", String.valueOf(item.getReservedQuantity()), reservedStyle);
        
        String availableStyle = item.getAvailableQuantity() <= item.getMinimumStock() ? 
                               "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: green; -fx-font-weight: bold;";
        addDetailRow(grid, row++, "Available:", String.valueOf(item.getAvailableQuantity()), availableStyle);
        
        String minStockStyle = item.isLowStock() ? "-fx-text-fill: red;" : "";
        addDetailRow(grid, row++, "Minimum Stock:", String.valueOf(item.getMinimumStock()), minStockStyle);
        
        // Pricing
        addDetailRow(grid, row++, "Cost Price:", String.format("₱%.2f", item.getCostPrice()), "");
        addDetailRow(grid, row++, "Selling Price:", String.format("₱%.2f", item.getSellingPrice()), "-fx-font-weight: bold;");
        
        double margin = item.getSellingPrice() - item.getCostPrice();
        double marginPercent = (margin / item.getCostPrice()) * 100;
        addDetailRow(grid, row++, "Profit Margin:", String.format("₱%.2f (%.1f%%)", margin, marginPercent), 
                    margin > 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        // Other details
        addDetailRow(grid, row++, "Unit:", item.getUnit(), "");
        addDetailRow(grid, row++, "Location:", item.getLocation(), "");
        
        // Expiration date with color coding
        if (item.getExpirationDate() != null) {
            String expirationStyle = "";
            String expirationText = item.getExpirationDate().toString();
            if (item.isExpired()) {
                expirationStyle = "-fx-text-fill: red; -fx-font-weight: bold;";
                expirationText += " (EXPIRED)";
            } else if (item.isNearExpiration()) {
                expirationStyle = "-fx-text-fill: orange; -fx-font-weight: bold;";
                expirationText += " (Expiring Soon)";
            }
            addDetailRow(grid, row++, "Expiration Date:", expirationText, expirationStyle);
        } else {
            addDetailRow(grid, row++, "Expiration Date:", "N/A", "");
        }
        
        // Stock status
        String stockStatus;
        String stockStyle;
        if (item.isLowStock()) {
            stockStatus = "⚠ LOW STOCK - REORDER NEEDED";
            stockStyle = "-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 13px;";
        } else if (item.getAvailableQuantity() == 0) {
            stockStatus = "⚠ OUT OF STOCK (All Reserved)";
            stockStyle = "-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 13px;";
        } else {
            stockStatus = "✓ In Stock";
            stockStyle = "-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 13px;";
        }
        addDetailRow(grid, row++, "Status:", stockStatus, stockStyle);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.showAndWait();
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value, String valueStyle) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");
        
        Label valueNode = new Label(value);
        valueNode.setStyle(valueStyle);
        valueNode.setWrapText(true);
        valueNode.setMaxWidth(300);
        
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
    
    private void showInventoryItemDialog(InventoryItem item) {
        Dialog<InventoryItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Add New Inventory Item" : "Edit Inventory Item - " + item.getHexId());
        dialog.setHeaderText(item == null ? "Enter item details" : "Part ID: " + item.getHexId());
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Create form fields
        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Auto-generated");
        partNumberField.setEditable(false); // Make it read-only
        partNumberField.setStyle("-fx-background-color: #f0f0f0;"); // Gray background to show it's read-only
        
        // Show hex_id for new and existing items
        if (item == null) {
            partNumberField.setText("Will be auto-generated");
        } else {
            partNumberField.setText(item.getHexId());
        }
        
        TextField nameField = new TextField();
        nameField.setPromptText("Item Name (required)");
        
        ComboBox<String> categoryComboBox = new ComboBox<>();
        categoryComboBox.setPromptText("Select Category");
        categoryComboBox.setEditable(false); // Not editable unless "Other" is selected
        categoryComboBox.getItems().addAll(
            "Fluids", "Filters", "Brake System", "Engine Parts", "Electrical", 
            "Transmission", "Suspension", "Cooling System", "Tools", "Other"
        );
        
        // Create a text field for "Other" category (initially hidden)
        TextField otherCategoryField = new TextField();
        otherCategoryField.setPromptText("Enter category");
        otherCategoryField.setVisible(false);
        otherCategoryField.setManaged(false); // Don't take up space when hidden
        
        // Show/hide the other category field based on selection
        categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if ("Other".equals(newValue)) {
                otherCategoryField.setVisible(true);
                otherCategoryField.setManaged(true);
                otherCategoryField.requestFocus();
            } else {
                otherCategoryField.setVisible(false);
                otherCategoryField.setManaged(false);
                otherCategoryField.clear();
            }
        });
        
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, 0, 1);
        quantitySpinner.setEditable(true);
        
        // Make quantity spinner text field accept only numbers
        quantitySpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                quantitySpinner.getEditor().setText(oldValue);
            }
        });
        
        TextField costPriceField = new TextField();
        costPriceField.setPromptText("Cost Price");
        
        // Add listener to cost price field to only allow numbers and decimal point
        costPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                costPriceField.setText(oldValue);
            }
        });
        
        TextField sellingPriceField = new TextField();
        sellingPriceField.setPromptText("Selling Price");
        
        // Add listener to selling price field to only allow numbers and decimal point
        sellingPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                sellingPriceField.setText(oldValue);
            }
        });
        
        ComboBox<String> locationComboBox = new ComboBox<>();
        locationComboBox.setPromptText("Select Location");
        locationComboBox.setEditable(false);
        locationComboBox.getItems().addAll(
            "Bodega", "Warehouse 1", "Warehouse 2", "Warehouse 3", 
            "Storage Room A", "Storage Room B", "Workshop", "Other"
        );
        
        TextField customLocationField = new TextField();
        customLocationField.setPromptText("Enter location");
        customLocationField.setVisible(false);
        customLocationField.setManaged(false);
        
        locationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if ("Other".equals(newValue)) {
                customLocationField.setVisible(true);
                customLocationField.setManaged(true);
                customLocationField.requestFocus();
            } else {
                customLocationField.setVisible(false);
                customLocationField.setManaged(false);
                customLocationField.clear();
            }
        });
        
        Spinner<Integer> minStockSpinner = new Spinner<>(0, 1000, 0, 1);
        minStockSpinner.setEditable(true);
        
        // Make minimum stock spinner text field accept only numbers
        minStockSpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minStockSpinner.getEditor().setText(oldValue);
            }
        });
        
        // Set existing values if editing
        if (item != null) {
            partNumberField.setText(item.getPartNumber());
            nameField.setText(item.getName());
            
            String existingCategory = item.getCategory();
            // Check if existing category is in the dropdown
            if (categoryComboBox.getItems().contains(existingCategory) && !"Other".equals(existingCategory)) {
                categoryComboBox.setValue(existingCategory);
            } else if (!"Other".equals(existingCategory) && !existingCategory.isEmpty()) {
                // If it's a custom category, select "Other" and show it in the text field
                categoryComboBox.setValue("Other");
                otherCategoryField.setText(existingCategory);
                otherCategoryField.setVisible(true);
                otherCategoryField.setManaged(true);
            } else {
                categoryComboBox.setValue(existingCategory);
            }
            
            quantitySpinner.getValueFactory().setValue(item.getQuantity());
            costPriceField.setText(String.valueOf(item.getCostPrice()));
            sellingPriceField.setText(String.valueOf(item.getSellingPrice()));
            
            // Set location dropdown value
            String existingLocation = item.getLocation();
            if (existingLocation != null && locationComboBox.getItems().contains(existingLocation) && !"Other".equals(existingLocation)) {
                locationComboBox.setValue(existingLocation);
            } else if (existingLocation != null && !existingLocation.isEmpty() && !"Other".equals(existingLocation)) {
                locationComboBox.setValue("Other");
                customLocationField.setText(existingLocation);
                customLocationField.setVisible(true);
                customLocationField.setManaged(true);
            } else {
                locationComboBox.setValue(existingLocation);
            }
            
            minStockSpinner.getValueFactory().setValue(item.getMinimumStock());
        }
        
        // Add form fields to grid
        int row = 0;
        grid.add(new Label("Part Number:"), 0, row);
        grid.add(partNumberField, 1, row++);
        
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryComboBox, 1, row++);
        grid.add(otherCategoryField, 1, row++); // Add the "Other" text field
        
        grid.add(new Label("Quantity:"), 0, row);
        
        // Create HBox for quantity spinner with quick adjustment buttons
        HBox quantityBox = new HBox(5);
        quantityBox.getChildren().add(quantitySpinner);
        
        // Quick adjustment buttons
        Button minus10 = new Button("-10");
        Button minus5 = new Button("-5");
        Button minus1 = new Button("-1");
        Button plus1 = new Button("+1");
        Button plus5 = new Button("+5");
        Button plus10 = new Button("+10");
        
        // Set button actions
        minus10.setOnAction(e -> adjustQuantity(quantitySpinner, -10));
        minus5.setOnAction(e -> adjustQuantity(quantitySpinner, -5));
        minus1.setOnAction(e -> adjustQuantity(quantitySpinner, -1));
        plus1.setOnAction(e -> adjustQuantity(quantitySpinner, 1));
        plus5.setOnAction(e -> adjustQuantity(quantitySpinner, 5));
        plus10.setOnAction(e -> adjustQuantity(quantitySpinner, 10));
        
        // Add buttons to HBox
        quantityBox.getChildren().addAll(minus10, minus5, minus1, plus1, plus5, plus10);
        
        grid.add(quantityBox, 1, row++);
        
        grid.add(new Label("Cost Price:"), 0, row);
        grid.add(costPriceField, 1, row++);
        
        grid.add(new Label("Selling Price:"), 0, row);
        grid.add(sellingPriceField, 1, row++);
        
        grid.add(new Label("Location:"), 0, row);
        grid.add(locationComboBox, 1, row++);
        grid.add(customLocationField, 1, row++);
        
        grid.add(new Label("Minimum Stock:"), 0, row);
        grid.add(minStockSpinner, 1, row++);
        
        // Add expiration date picker
        DatePicker expirationDatePicker = new DatePicker();
        expirationDatePicker.setPromptText("Optional");
        if (item != null && item.getExpirationDate() != null) {
            expirationDatePicker.setValue(item.getExpirationDate());
        }
        grid.add(new Label("Expiration Date:"), 0, row);
        grid.add(expirationDatePicker, 1, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default (since part number is auto-generated)
        Platform.runLater(() -> nameField.requestFocus());
        
        // Process the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate inputs
                    String partNumber = partNumberField.getText().trim();
                    String name = nameField.getText().trim();
                    
                    if (name.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Name is required");
                        return null;
                    }
                    
                    if (partNumber.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Part Number generation failed");
                        return null;
                    }
                    
                    String selectedCategory = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : "";
                    String category;
                    
                    // If "Other" is selected, use the custom category from otherCategoryField
                    if ("Other".equals(selectedCategory)) {
                        category = otherCategoryField.getText().trim();
                        if (category.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Please specify the category");
                            return null;
                        }
                    } else {
                        category = selectedCategory;
                    }
                    
                    int quantity = quantitySpinner.getValue();
                    
                    double costPrice = 0;
                    if (!costPriceField.getText().trim().isEmpty()) {
                        try {
                            costPrice = Double.parseDouble(costPriceField.getText().trim());
                        } catch (NumberFormatException e) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Cost Price must be a number");
                            return null;
                        }
                    }
                    
                    double sellingPrice = 0;
                    if (!sellingPriceField.getText().trim().isEmpty()) {
                        try {
                            sellingPrice = Double.parseDouble(sellingPriceField.getText().trim());
                        } catch (NumberFormatException e) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Selling Price must be a number");
                            return null;
                        }
                    }
                    
                    // Validate selling price is not lower than cost price
                    if (sellingPrice > 0 && costPrice > 0 && sellingPrice < costPrice) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Price", 
                                 "Selling price (₱" + String.format("%.2f", sellingPrice) + 
                                 ") cannot be lower than cost price (₱" + String.format("%.2f", costPrice) + ")");
                        return null;
                    }
                    
                    // Get location from dropdown or custom field
                    String selectedLocation = locationComboBox.getValue() != null ? locationComboBox.getValue() : "";
                    String location;
                    if ("Other".equals(selectedLocation)) {
                        location = customLocationField.getText().trim();
                        if (location.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Please specify the location");
                            return null;
                        }
                    } else {
                        location = selectedLocation;
                    }
                    int minimumStock = minStockSpinner.getValue();
                    java.time.LocalDate expirationDate = expirationDatePicker.getValue();
                    
                    // Create or update inventory item
                    boolean success;
                    if (item == null) {
                        // Add new item to database
                        success = inventoryService.addItem(
                            partNumber, name, category, quantity,
                            costPrice, sellingPrice, location, minimumStock
                        );
                        if (success && expirationDate != null) {
                            // Get the newly created item's ID and set expiration date
                            List<InventoryItem> allItems = inventoryService.getAllItems();
                            for (InventoryItem newItem : allItems) {
                                if (newItem.getPartNumber().equals(partNumber)) {
                                    inventoryService.updateExpirationDate(newItem.getId(), expirationDate);
                                    break;
                                }
                            }
                        }
                        if (success) {
                            // Check if new item is at or below minimum stock and send email
                            if (quantity <= minimumStock) {
                                try {
                                    inventoryService.checkAndSendLowStockAlert();
                                } catch (Exception e) {
                                    System.err.println("Failed to send low stock alert: " + e.getMessage());
                                }
                            }
                            loadInventoryData();
                            statusLabel.setText("Item added successfully");
                        }
                    } else {
                        // Update existing item
                        item.setPartNumber(partNumber);
                        item.setName(name);
                        item.setCategory(category);
                        item.setQuantity(quantity);
                        item.setCostPrice(costPrice);
                        item.setSellingPrice(sellingPrice);
                        item.setLocation(location);
                        item.setMinimumStock(minimumStock);
                        item.setExpirationDate(expirationDate);
                        success = inventoryService.updateItem(item);
                        if (success) {
                            inventoryService.updateExpirationDate(item.getId(), expirationDate);
                            inventoryTable.refresh();
                            statusLabel.setText("Item updated successfully");
                        }
                    }
                    
                    if (!success) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save item");
                    }
                    
                    return success ? item : null;
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save item: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void restockItem(InventoryItem item) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Adjust Inventory Stock");
        dialog.setHeaderText("Adjust Stock for " + item.getName());
        
        // Set button types
        ButtonType updateButtonType = new ButtonType("Update Stock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Current quantity display
        Label currentQtyLabel = new Label(String.valueOf(item.getQuantity()));
        currentQtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(new Label("Current Quantity:"), 0, 0);
        grid.add(currentQtyLabel, 1, 0);
        
        // Adjustment type selection
        grid.add(new Label("Adjustment Type:"), 0, 1);
        ComboBox<String> adjustmentTypeComboBox = new ComboBox<>();
        adjustmentTypeComboBox.getItems().addAll("Add Stock", "Reduce Stock");
        adjustmentTypeComboBox.setValue("Add Stock");
        grid.add(adjustmentTypeComboBox, 1, 1);
        
        // Adjustment quantity input
        grid.add(new Label("Quantity:"), 0, 2);
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 10000, 10, 1);
        quantitySpinner.setEditable(true);
        
        // Add numeric validation to spinner
        quantitySpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                quantitySpinner.getEditor().setText(oldValue);
            }
        });
        
        // Create HBox for quantity spinner with quick adjustment buttons
        HBox restockQuantityBox = new HBox(5);
        restockQuantityBox.getChildren().add(quantitySpinner);
        
        // Quick adjustment buttons for restock
        Button restockMinus10 = new Button("-10");
        Button restockMinus5 = new Button("-5");
        Button restockMinus1 = new Button("-1");
        Button restockPlus1 = new Button("+1");
        Button restockPlus5 = new Button("+5");
        Button restockPlus10 = new Button("+10");
        
        // Set button actions
        restockMinus10.setOnAction(e -> adjustQuantity(quantitySpinner, -10));
        restockMinus5.setOnAction(e -> adjustQuantity(quantitySpinner, -5));
        restockMinus1.setOnAction(e -> adjustQuantity(quantitySpinner, -1));
        restockPlus1.setOnAction(e -> adjustQuantity(quantitySpinner, 1));
        restockPlus5.setOnAction(e -> adjustQuantity(quantitySpinner, 5));
        restockPlus10.setOnAction(e -> adjustQuantity(quantitySpinner, 10));
        
        // Add buttons to HBox
        restockQuantityBox.getChildren().addAll(restockMinus10, restockMinus5, restockMinus1, 
                                                 restockPlus1, restockPlus5, restockPlus10);
        
        grid.add(restockQuantityBox, 1, 2);
        
        // Preview of new quantity
        Label newQtyLabel = new Label();
        newQtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0066cc;");
        grid.add(new Label("New Quantity:"), 0, 3);
        grid.add(newQtyLabel, 1, 3);
        
        // Update preview when spinner or type changes
        Runnable updatePreview = () -> {
            int adjustment = quantitySpinner.getValue();
            int newQuantity;
            
            if ("Add Stock".equals(adjustmentTypeComboBox.getValue())) {
                newQuantity = item.getQuantity() + adjustment;
            } else {
                newQuantity = item.getQuantity() - adjustment;
            }
            
            newQtyLabel.setText(String.valueOf(Math.max(0, newQuantity)));
            
            // Color code the preview
            if (newQuantity < 0) {
                newQtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");
            } else if (newQuantity < item.getMinimumStock()) {
                newQtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: orange;");
            } else {
                newQtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: green;");
            }
        };
        
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview.run());
        adjustmentTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview.run());
        updatePreview.run(); // Initial update
        
        dialog.getDialogPane().setContent(grid);
        
        // Process the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                int adjustment = quantitySpinner.getValue();
                if ("Reduce Stock".equals(adjustmentTypeComboBox.getValue())) {
                    adjustment = -adjustment;
                }
                return adjustment;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(adjustment -> {
            try {
                // Calculate new quantity
                int newQuantity = item.getQuantity() + adjustment;
                
                // Validate new quantity
                if (newQuantity < 0) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Quantity", 
                            "Cannot reduce stock below 0. Current stock: " + item.getQuantity());
                    return;
                }
                
                // Update the item's quantity in database
                boolean success = inventoryService.updateItemQuantity(item.getId(), newQuantity);
                
                if (success) {
                    // Update local model
                    item.setQuantity(newQuantity);
                    inventoryTable.refresh();
                    
                    // Display appropriate message
                    String action = (adjustment > 0) ? "Added" : "Reduced";
                    String message = action + " " + Math.abs(adjustment) + " units. New stock: " + newQuantity;
                    statusLabel.setText(message);
                    
                    // Check if any delayed bookings can now proceed
                    if (adjustment > 0) { // Only check when adding stock
                        System.out.println("=== RESTOCK: Checking delayed bookings after adding " + adjustment + " units ===");
                        try {
                            ServiceBookingService bookingService = new ServiceBookingService();
                            int updatedBookings = bookingService.checkAndUpdateDelayedBookings();
                            System.out.println("=== RESTOCK: Updated " + updatedBookings + " bookings ===");
                            if (updatedBookings > 0) {
                                message += "\n\n✓ " + updatedBookings + " delayed booking(s) automatically updated to scheduled!";
                            }
                        } catch (SQLException e) {
                            System.err.println("Could not check delayed bookings: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Stock Updated", 
                            message + " for " + item.getName());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update quantity");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
            }
        });
    }
    
    private void adjustQuantity(Spinner<Integer> spinner, int adjustment) {
        int currentValue = spinner.getValue();
        int newValue = currentValue + adjustment;
        // Ensure value stays within spinner bounds
        if (newValue >= 0 && newValue <= 10000) {
            spinner.getValueFactory().setValue(newValue);
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
