package com.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CustomerVehiclesController {

    @FXML
    private TableView<Vehicle> vehicleTable;
    
    @FXML
    private TableColumn<Vehicle, String> idColumn;
    
    @FXML
    private TableColumn<Vehicle, String> typeColumn;
    
    @FXML
    private TableColumn<Vehicle, String> brandColumn;
    
    @FXML
    private TableColumn<Vehicle, String> modelColumn;
    
    @FXML
    private TableColumn<Vehicle, String> yearColumn;
    
    @FXML
    private TableColumn<Vehicle, String> plateNumberColumn;
    
    @FXML
    private TableColumn<Vehicle, Void> actionsColumn;
    
    @FXML
    private Label customerInfoLabel;
    
    @FXML
    private Label statusLabel;
    
    private Customer customer;
    private ObservableList<Vehicle> vehiclesList = FXCollections.observableArrayList();
    private VehicleService vehicleService = VehicleService.getInstance();
    
    public void initData(Customer customer) {
        this.customer = customer;
        customerInfoLabel.setText("Vehicles for: " + customer.getName());
        setupTableColumns();
        loadVehicleData();
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("hexId"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        modelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        plateNumberColumn.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));
        
        setupActionsColumn();
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> {
            return new TableCell<Vehicle, Void>() {
                private final Button editButton = new Button("Edit");
                private final Button deleteButton = new Button("Delete");
                
                {
                    editButton.setOnAction(event -> {
                        Vehicle vehicle = getTableView().getItems().get(getIndex());
                        editVehicle(vehicle);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        Vehicle vehicle = getTableView().getItems().get(getIndex());
                        deleteVehicle(vehicle);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, editButton, deleteButton);
                        setGraphic(buttons);
                    }
                }
            };
        });
    }
    
    private void loadVehicleData() {
        try {
            List<Vehicle> vehicles = vehicleService.getCustomerVehicles(customer.getId());
            vehiclesList.clear();
            vehiclesList.addAll(vehicles);
            vehicleTable.setItems(vehiclesList);
            statusLabel.setText("Loaded " + vehicles.size() + " vehicles");
        } catch (SQLException e) {
            statusLabel.setText("Error loading vehicles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    public void handleAddVehicle() {
        showVehicleDialog(null);
    }
    
    private void editVehicle(Vehicle vehicle) {
        showVehicleDialog(vehicle);
    }
    
    private void showVehicleDialog(Vehicle vehicle) {
        // Dialog implementation for adding/editing vehicles
        // This would be implemented in a real application
        statusLabel.setText(vehicle == null ? "Adding new vehicle" : "Editing vehicle: " + vehicle.getBrand() + " " + vehicle.getModel());
    }
    
    private void deleteVehicle(Vehicle vehicle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Vehicle");
        alert.setHeaderText("Delete Vehicle: " + vehicle.getBrand() + " " + vehicle.getModel());
        alert.setContentText("Are you sure you want to delete this vehicle? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = vehicleService.deleteVehicle(vehicle.getId());
                    if (deleted) {
                        vehiclesList.remove(vehicle);
                        vehicleTable.refresh();
                        statusLabel.setText("Vehicle deleted successfully");
                    } else {
                        statusLabel.setText("Failed to delete vehicle");
                    }
                } catch (SQLException e) {
                    statusLabel.setText("Error deleting vehicle: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    @FXML
    public void handleRefresh() {
        loadVehicleData();
        statusLabel.setText("Vehicle data refreshed");
    }
    
    @FXML
    public void handleClose() {
        Stage stage = (Stage) vehicleTable.getScene().getWindow();
        stage.close();
    }
}
