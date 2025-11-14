package com.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StockMonitorService {
    private static StockMonitorService instance;
    private Timer timer;
    private boolean isMonitoring = false;
    
    // Check interval: every 6 hours (in milliseconds)
    private static final long CHECK_INTERVAL = 6 * 60 * 60 * 1000; // 6 hours
    // Or for testing: 5 minutes = 5 * 60 * 1000
    
    private StockMonitorService() {
        // Private constructor
    }
    
    public static StockMonitorService getInstance() {
        if (instance == null) {
            instance = new StockMonitorService();
        }
        return instance;
    }
    
    /**
     * Start monitoring stock levels
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("Stock monitoring is already running.");
            return;
        }
        
        timer = new Timer("StockMonitorTimer", true); // daemon thread
        
        // Schedule the task to run immediately and then every CHECK_INTERVAL
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkStockLevels();
            }
        }, 0, CHECK_INTERVAL);
        
        isMonitoring = true;
        System.out.println("Stock monitoring started. Checking every " + (CHECK_INTERVAL / 1000 / 60) + " minutes.");
    }
    
    /**
     * Stop monitoring stock levels
     */
    public void stopMonitoring() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isMonitoring = false;
        System.out.println("Stock monitoring stopped.");
    }
    
    /**
     * Check stock levels and send alerts if needed
     */
    public void checkStockLevels() {
        System.out.println("Checking stock levels...");
        
        try {
            List<InventoryItem> allItems = InventoryService.getInstance().getAllItems();
            List<InventoryItem> lowStockItems = new ArrayList<>();
            List<InventoryItem> lowAvailableItems = new ArrayList<>();
            
            // Find items with low stock or low available stock
            for (InventoryItem item : allItems) {
                if (item.isLowStock()) {
                    lowStockItems.add(item);
                } else if (item.isLowAvailableStock()) {
                    lowAvailableItems.add(item);
                }
            }
            
            // Send urgent low stock alerts
            if (!lowStockItems.isEmpty()) {
                System.out.println("Found " + lowStockItems.size() + " items with low stock.");
                
                boolean emailSent = EmailService.getInstance().sendLowStockAlert(lowStockItems);
                
                if (emailSent) {
                    System.out.println("Low stock alert email sent successfully.");
                } else {
                    System.err.println("Failed to send low stock alert email.");
                }
            }
            
            // Send advisory for low available stock (heavy reservations)
            if (!lowAvailableItems.isEmpty()) {
                System.out.println("Found " + lowAvailableItems.size() + " items with low available stock.");
                
                boolean emailSent = EmailService.getInstance().sendLowAvailableStockAlert(lowAvailableItems);
                
                if (emailSent) {
                    System.out.println("Low available stock notice sent successfully.");
                } else {
                    System.err.println("Failed to send low available stock notice.");
                }
            }
            
            if (lowStockItems.isEmpty() && lowAvailableItems.isEmpty()) {
                System.out.println("All items are at adequate stock levels.");
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking stock levels: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Perform an immediate stock check (manual trigger)
     */
    public void checkStockNow() {
        new Thread(() -> checkStockLevels()).start();
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
}
