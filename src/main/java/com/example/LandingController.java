package com.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class LandingController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label taglineLabel;
    @FXML private Button getStartedButton;
    
    @FXML
    public void initialize() {
        // No special initialization needed
    }
    
    @FXML
    private void handleGetStarted() {
        try {
            // Load the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            // Get current stage and switch to login scene
            Stage stage = (Stage) getStartedButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
