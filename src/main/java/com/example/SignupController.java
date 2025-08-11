package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import java.sql.SQLException;

public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleSignup() {
        try {
            boolean success = UserController.signup(
                usernameField.getText(),
                passwordField.getText(),
                emailField.getText()
            );
            
            if (success) {
                messageLabel.setText("Sign up successful!");
                App.setRoot("login");
            } else {
                messageLabel.setText("Username already exists!");
            }
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }
}
