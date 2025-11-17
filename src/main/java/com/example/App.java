package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        // Load landing page as initial scene
        scene = new Scene(loadFXML("landing"), 900, 650);
        stage.setScene(scene);
        stage.setTitle("AutoTech Service Management");
        stage.centerOnScreen();
        stage.show();
    primaryStage = stage;
        
        // Start stock monitoring service
        StockMonitorService.getInstance().startMonitoring();
        System.out.println("Stock monitoring service started.");
        
        // Check stock and expiration immediately on startup
        System.out.println("Performing initial stock and expiration check...");
        InventoryService.getInstance().checkAndSendLowStockAlert();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void hidePrimaryStage() {
        if (primaryStage != null) {
            primaryStage.hide();
        }
    }

    public static void showPrimaryStage() {
        if (primaryStage != null) {
            primaryStage.show();
            primaryStage.toFront();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void stop() {
        try {
            // Stop stock monitoring
            StockMonitorService.getInstance().stopMonitoring();
            
            // Clean up database connections
            DatabaseUtil.closeConnection();
            System.out.println("Application stopping, resources released.");
        } catch (Exception e) {
            System.err.println("Error during application shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }

}