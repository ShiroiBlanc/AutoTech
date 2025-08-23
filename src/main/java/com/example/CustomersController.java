package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CustomersController {
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, Integer> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;

    @FXML
    private void initialize() {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Add some dummy data for testing
        ObservableList<Customer> customers = FXCollections.observableArrayList(
            new Customer(1, "John Doe", "123-456-7890", "john@example.com"),
            new Customer(2, "Jane Smith", "098-765-4321", "jane@example.com")
        );
        customersTable.setItems(customers);
    }

    @FXML
    private void handleAddCustomer() {
        // TODO: Implement customer addition
    }

    @FXML
    private void handleEditCustomer() {
        // TODO: Implement customer editing
    }

    @FXML
    private void handleDeleteCustomer() {
        // TODO: Implement customer deletion
    }
}
