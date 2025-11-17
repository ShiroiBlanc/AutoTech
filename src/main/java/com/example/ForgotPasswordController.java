package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import java.io.IOException;
import java.sql.SQLException;

public class ForgotPasswordController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleResetPassword() {
        try {
            boolean success = UserController.resetPassword(
                usernameField.getText(),
                emailField.getText(),
                newPasswordField.getText()
            );
            
            if (success) {
                messageLabel.setText("Password reset successful!");
                App.setRoot("login");
            } else {
                messageLabel.setText("Invalid username or email!");
            }
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void switchToLogin() throws IOException {
        App.setRoot("login");
    }
}
