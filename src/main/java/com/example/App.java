package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;


public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Load initial scene
        scene = new Scene(loadFXML("login"), 640, 480);
        stage.setScene(scene);
        stage.setTitle("AutoTech Service Management");
        stage.show();
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
        // Properly close database connections on application shutdown
        try {
            // Access HikariDataSource to close it properly
            Field dataSourceField = DatabaseUtil.class.getDeclaredField("dataSource");
            dataSourceField.setAccessible(true);
            HikariDataSource dataSource = (HikariDataSource) dataSourceField.get(null);

            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("Database connections closed");
            }
        } catch (Exception e) {
            System.err.println("Error closing database connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}