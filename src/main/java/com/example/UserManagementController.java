package com.example;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import com.example.User.UserRole;

public class UserManagementController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;  // Added email field
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    @FXML
    public void initialize() {
        // Populate role dropdown
        roleComboBox.setItems(FXCollections.observableArrayList(
            "ADMIN", "CASHIER", "MECHANIC"
        ));
        roleComboBox.setValue("MECHANIC"); // Default selection
    }
    
    @FXML
    private void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();  // Get email value
        String roleStr = roleComboBox.getValue();
        
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            statusLabel.setText("Username, password, and email are required");
            return;
        }
        
        UserRole role = UserRole.fromString(roleStr);
        boolean success = UserService.getInstance().addUser(username, password, email, role);
        
        if (success) {
            statusLabel.setText("User added successfully");
            usernameField.clear();
            passwordField.clear();
            emailField.clear();  // Clear email field too
        } else {
            statusLabel.setText("Failed to add user");
        }
    }
}
