package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskController {

    public static void addTask(String title, String description, String status) throws SQLException {
        String query = "INSERT INTO tasks (title, description, status) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, status);
            preparedStatement.executeUpdate();
        }
    }

    public static List<Task> getTasksByStatus(String status) throws SQLException {
        String query = "SELECT * FROM tasks WHERE status = ?";
        List<Task> tasks = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, status);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                tasks.add(new Task(
                    resultSet.getInt("id"),
                    resultSet.getString("title"),
                    resultSet.getString("description"),
                    resultSet.getString("status")
                ));
            }
        }
        return tasks;
    }

    public static void updateTaskStatus(int taskId, String newStatus) throws SQLException {
        String query = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newStatus);
            preparedStatement.setInt(2, taskId);
            preparedStatement.executeUpdate();
        }
    }
}