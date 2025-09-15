package com.example;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel; // Renamed from errorLabel to messageLabel

    private void showError(String message) {
        // Use messageLabel for error messages
        if (messageLabel != null) {
            messageLabel.setText(message);
        } else {
            Alert alert = new Alert(AlertType.ERROR, message);
            alert.setHeaderText("Login Error");
            alert.showAndWait();
        }
    }

    @FXML
    protected void handleLogin() {  // Use the exact method name that your FXML expects
        try {
            String username = usernameField != null ? usernameField.getText() : "";
            String password = passwordField != null ? passwordField.getText() : "";
            
            if (username.isEmpty() || password.isEmpty()) {
                showError("Please enter username and password");
                return;
            }
            
            // Authenticate using UserService
            if (UserService.getInstance().authenticate(username, password)) {
                showError(""); // Clear any error
                DashboardController dashboardController = new DashboardController();
                dashboardController.openDashboardWindow();
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            showError("Login error: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleLoginButtonAction() {
        handleLogin();
    }

    @FXML
    private void switchToSignup() throws IOException {
        App.setRoot("signup");
    }

    @FXML
    private void switchToForgotPassword() throws IOException {
        App.setRoot("forgot-password");
    }

    @FXML
    private void handleSignupLink() {
        try {
            App.setRoot("signup");
        } catch (IOException e) {
            messageLabel.setText("Error loading signup page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("LoginController initialized");
        System.out.println("usernameField: " + (usernameField != null ? "found" : "null"));
        System.out.println("passwordField: " + (passwordField != null ? "found" : "null"));
        System.out.println("messageLabel: " + (messageLabel != null ? "found" : "null"));
    }
}

