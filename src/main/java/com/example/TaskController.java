package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskController {
    public static void addTask(String title, String description, String status) throws SQLException {
        String query = "INSERT INTO tasks (title, description, status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, status);
            stmt.executeUpdate();
        }
    }

    public static List<Task> getTasksByStatus(String status) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM tasks WHERE status = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(new Task(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("status")
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