package com.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.util.List;

public class InventoryController {
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> partNumberColumn;
    @FXML private TableColumn<InventoryItem, String> nameColumn;
    @FXML private TableColumn<InventoryItem, String> categoryColumn;
    @FXML private TableColumn<InventoryItem, Integer> quantityColumn;
    @FXML private TableColumn<InventoryItem, Double> priceColumn;
    @FXML private TableColumn<InventoryItem, String> locationColumn;
    @FXML private TableColumn<InventoryItem, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    
    private ObservableList<InventoryItem> inventoryList = FXCollections.observableArrayList();
    private InventoryService inventoryService = InventoryService.getInstance();

    @FXML
    public void initialize() {
        // Initialize columns
        partNumberColumn.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        // Set up actions column
        setupActionsColumn();
        
        // Load data from database
        loadInventoryData();
        
        // Set items to table
        inventoryTable.setItems(inventoryList);
    }

    private void loadInventoryData() {
        try {
            List<InventoryItem> items = inventoryService.getAllItems();
            inventoryList.clear();
            inventoryList.addAll(items);
            statusLabel.setText("Inventory loaded successfully. Total items: " + items.size());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load inventory: " + e.getMessage());
        }
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<InventoryItem, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button restockButton = new Button("Restock");
            private final HBox buttonBox = new HBox(5, editButton, restockButton);
            
            {
                buttonBox.setAlignment(Pos.CENTER);
                
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
    }
    
    @FXML
    private void handleAddItem() {
        showInventoryItemDialog(null);
    }
    
    private void editItem(InventoryItem item) {
        showInventoryItemDialog(item);
    }
    
    private void showInventoryItemDialog(InventoryItem item) {
        Dialog<InventoryItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Add New Inventory Item" : "Edit Inventory Item");
        dialog.setHeaderText(item == null ? "Enter item details" : "Edit item details");
        
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
        partNumberField.setPromptText("Part Number (required)");
        
        TextField nameField = new TextField();
        nameField.setPromptText("Item Name (required)");
        
        ComboBox<String> categoryComboBox = new ComboBox<>();
        categoryComboBox.setPromptText("Select Category");
        categoryComboBox.setEditable(true);
        categoryComboBox.getItems().addAll(
            "Fluids", "Filters", "Brake System", "Engine Parts", "Electrical", 
            "Transmission", "Suspension", "Cooling System", "Tools", "Other"
        );
        
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, 0, 1);
        quantitySpinner.setEditable(true);
        
        TextField costPriceField = new TextField();
        costPriceField.setPromptText("Cost Price");
        
        TextField sellingPriceField = new TextField();
        sellingPriceField.setPromptText("Selling Price");
        
        TextField locationField = new TextField();
        locationField.setPromptText("Storage Location");
        
        Spinner<Integer> minStockSpinner = new Spinner<>(0, 1000, 0, 1);
        minStockSpinner.setEditable(true);
        
        // Set existing values if editing
        if (item != null) {
            partNumberField.setText(item.getPartNumber());
            nameField.setText(item.getName());
            categoryComboBox.setValue(item.getCategory());
            quantitySpinner.getValueFactory().setValue(item.getQuantity());
            costPriceField.setText(String.valueOf(item.getCostPrice()));
            sellingPriceField.setText(String.valueOf(item.getSellingPrice()));
            locationField.setText(item.getLocation());
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
        
        grid.add(new Label("Quantity:"), 0, row);
        grid.add(quantitySpinner, 1, row++);
        
        grid.add(new Label("Cost Price:"), 0, row);
        grid.add(costPriceField, 1, row++);
        
        grid.add(new Label("Selling Price:"), 0, row);
        grid.add(sellingPriceField, 1, row++);
        
        grid.add(new Label("Location:"), 0, row);
        grid.add(locationField, 1, row++);
        
        grid.add(new Label("Minimum Stock:"), 0, row);
        grid.add(minStockSpinner, 1, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the part number field by default
        partNumberField.requestFocus();
        
        // Process the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate inputs
                    String partNumber = partNumberField.getText().trim();
                    String name = nameField.getText().trim();
                    
                    if (partNumber.isEmpty() || name.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Part Number and Name are required");
                        return null;
                    }
                    
                    String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : "";
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
                    
                    String location = locationField.getText().trim();
                    int minimumStock = minStockSpinner.getValue();
                    
                    // Create or update inventory item
                    boolean success;
                    if (item == null) {
                        // Add new item to database
                        success = inventoryService.addItem(
                            partNumber, name, category, quantity,
                            costPrice, sellingPrice, location, minimumStock
                        );
                        if (success) {
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
                        success = inventoryService.updateItem(item);
                        if (success) {
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
        dialog.setTitle("Restock Inventory Item");
        dialog.setHeaderText("Restock " + item.getName());
        
        // Set button types
        ButtonType addButtonType = new ButtonType("Add Stock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Current quantity display
        grid.add(new Label("Current Quantity:"), 0, 0);
        grid.add(new Label(String.valueOf(item.getQuantity())), 1, 0);
        
        // Add stock input
        grid.add(new Label("Add Quantity:"), 0, 1);
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 10000, 10, 1);
        quantitySpinner.setEditable(true);
        grid.add(quantitySpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Process the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(quantity -> {
            try {
                // Update the item's quantity in database
                int newQuantity = item.getQuantity() + quantity;
                boolean success = inventoryService.updateItemQuantity(item.getId(), newQuantity);
                
                if (success) {
                    // Update local model
                    item.setQuantity(newQuantity);
                    inventoryTable.refresh();
                    statusLabel.setText("Added " + quantity + " units to " + item.getName());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update quantity");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
            }
        });
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
