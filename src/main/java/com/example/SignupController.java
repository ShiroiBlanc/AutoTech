package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import java.io.IOException;
import com.example.User.UserRole;

public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        // Populate role dropdown
        roleComboBox.setItems(FXCollections.observableArrayList(
            "MECHANIC", "CASHIER"
        ));
        roleComboBox.setValue("MECHANIC"); // Default selection
    }

    @FXML
    private void handleSignup() {
        try {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String email = emailField.getText().trim();
            String roleStr = roleComboBox.getValue();
            
            // Validate input
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                messageLabel.setText("All fields are required!");
                return;
            }
            
            // Convert string role to enum
            UserRole role = UserRole.fromString(roleStr);
            
            boolean success = UserService.getInstance().addUser(
                username,
                password,
                email,
                role
            );
            
            if (success) {
                messageLabel.setText("Sign up successful!");
                App.setRoot("login");
            } else {
                messageLabel.setText("Username already exists!");
            }
        } catch (IOException e) {
            messageLabel.setText("Error navigating to login: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            messageLabel.setText("Error returning to login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
