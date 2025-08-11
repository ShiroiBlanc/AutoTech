package com.example;

import java.io.IOException;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {
    @FXML private TextField taskTitleField;
    @FXML private TextField taskDescField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, Integer> idColumn;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> descriptionColumn;
    @FXML private TableColumn<Task, String> statusColumn;

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList("Backlog", "In Progress", "Done"));
        
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        refreshTasks();
    }

    @FXML
    private void handleAddTask() {
        try {
            TaskController.addTask(
                taskTitleField.getText(),
                taskDescField.getText(),
                statusComboBox.getValue()
            );
            refreshTasks();
            clearFields();
        } catch (SQLException e) {
            showError("Error adding task: " + e.getMessage());
        }
    }

    private void refreshTasks() {
        try {
            taskTable.setItems(FXCollections.observableArrayList(
                TaskController.getTasksByStatus(statusComboBox.getValue())
            ));
        } catch (SQLException e) {
            showError("Error loading tasks: " + e.getMessage());
        }
    }

    private void clearFields() {
        taskTitleField.clear();
        taskDescField.clear();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() throws IOException {
        App.setRoot("login");
    }
}
