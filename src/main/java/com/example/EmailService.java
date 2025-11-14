package com.example;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.List;

public class EmailService {
    private static EmailService instance;
    
    // Email configuration - UPDATE THESE WITH YOUR SMTP SETTINGS
    private static final String SMTP_HOST = "smtp.gmail.com"; // e.g., smtp.gmail.com for Gmail
    private static final String SMTP_PORT = "587"; // 587 for TLS, 465 for SSL
    private static final String SENDER_EMAIL = "lnlabor@addu.edu.ph"; // Email address that sends alerts
    private static final String SENDER_PASSWORD = "dnwapblnysuauxts"; // Gmail App Password (spaces removed)
    private static final String ALERT_RECIPIENT = "pripcmisa@addu.edu.ph"; // Where to send alerts
    
    private EmailService() {
        // Private constructor
    }
    
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    /**
     * Send a low stock alert email
     */
    public boolean sendLowStockAlert(List<InventoryItem> lowStockItems) {
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            return false;
        }
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<html><body>");
        messageBody.append("<h2>Low Stock Alert - AutoTech Inventory</h2>");
        messageBody.append("<p>The following items have fallen below their minimum stock levels:</p>");
        messageBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
        messageBody.append("<tr style='background-color: #f2f2f2;'>");
        messageBody.append("<th>Part ID</th>");
        messageBody.append("<th>Name</th>");
        messageBody.append("<th>Category</th>");
        messageBody.append("<th>Current Stock</th>");
        messageBody.append("<th>Minimum Stock</th>");
        messageBody.append("<th>Location</th>");
        messageBody.append("</tr>");
        
        for (InventoryItem item : lowStockItems) {
            messageBody.append("<tr>");
            messageBody.append("<td>").append(item.getHexId()).append("</td>");
            messageBody.append("<td>").append(item.getName()).append("</td>");
            messageBody.append("<td>").append(item.getCategory()).append("</td>");
            messageBody.append("<td style='color: red; font-weight: bold;'>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getMinimumStock()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getLocation()).append("</td>");
            messageBody.append("</tr>");
        }
        
        messageBody.append("</table>");
        messageBody.append("<br><p><strong>Action Required:</strong> Please reorder these items as soon as possible.</p>");
        messageBody.append("<p>This is an automated alert from AutoTech Inventory Management System.</p>");
        messageBody.append("</body></html>");
        
        return sendEmail(
            ALERT_RECIPIENT,
            "⚠️ Low Stock Alert - " + lowStockItems.size() + " Items Need Reordering",
            messageBody.toString()
        );
    }
    
    /**
     * Send a low available stock alert email (due to heavy reservations)
     */
    public boolean sendLowAvailableStockAlert(List<InventoryItem> lowAvailableItems) {
        if (lowAvailableItems == null || lowAvailableItems.isEmpty()) {
            return false;
        }
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<html><body>");
        messageBody.append("<h2 style='color: orange;'>⚡ Low Available Stock Notice - AutoTech Inventory</h2>");
        messageBody.append("<p>The following items have low available stock due to heavy reservations:</p>");
        messageBody.append("<p><em>Note: Total stock is adequate, but most units are reserved for existing bookings.</em></p>");
        messageBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
        messageBody.append("<tr style='background-color: #fff3cd;'>");
        messageBody.append("<th>Part ID</th>");
        messageBody.append("<th>Name</th>");
        messageBody.append("<th>Category</th>");
        messageBody.append("<th>Total Stock</th>");
        messageBody.append("<th>Reserved</th>");
        messageBody.append("<th>Available</th>");
        messageBody.append("<th>Minimum Stock</th>");
        messageBody.append("<th>Location</th>");
        messageBody.append("</tr>");
        
        for (InventoryItem item : lowAvailableItems) {
            messageBody.append("<tr>");
            messageBody.append("<td>").append(item.getHexId()).append("</td>");
            messageBody.append("<td>").append(item.getName()).append("</td>");
            messageBody.append("<td>").append(item.getCategory()).append("</td>");
            messageBody.append("<td>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getReservedQuantity()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td style='color: orange; font-weight: bold;'>").append(item.getAvailableQuantity()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getMinimumStock()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getLocation()).append("</td>");
            messageBody.append("</tr>");
        }
        
        messageBody.append("</table>");
        messageBody.append("<br><p><strong>Advisory:</strong> Consider restocking these items to maintain adequate available inventory for new orders.</p>");
        messageBody.append("<p>This is an automated notice from AutoTech Inventory Management System.</p>");
        messageBody.append("</body></html>");
        
        return sendEmail(
            ALERT_RECIPIENT,
            "⚡ Low Available Stock Notice - " + lowAvailableItems.size() + " Items Heavily Reserved",
            messageBody.toString()
        );
    }
    
    /**
     * Send a general email
     */
    public boolean sendEmail(String recipient, String subject, String htmlBody) {
        // Configure mail server properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });
        
        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            
            // Send message
            Transport.send(message);
            
            System.out.println("Email sent successfully to: " + recipient);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test email configuration
     */
    public boolean testEmailConfiguration() {
        String testBody = "<html><body><h2>Test Email</h2><p>If you receive this, your email configuration is working correctly!</p></body></html>";
        return sendEmail(ALERT_RECIPIENT, "AutoTech Email Test", testBody);
    }
}
