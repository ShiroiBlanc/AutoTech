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
     * Send a CRITICAL zero stock alert email
     */
    public boolean sendZeroStockAlert(List<InventoryItem> zeroStockItems) {
        if (zeroStockItems == null || zeroStockItems.isEmpty()) {
            return false;
        }
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<html><body>");
        messageBody.append("<div style='border: 3px solid #d32f2f; padding: 20px; background-color: #ffebee;'>");
        messageBody.append("<h2 style='color: #d32f2f; margin-top: 0;'>🚨 CRITICAL: ZERO STOCK ALERT - AutoTech Inventory</h2>");
        messageBody.append("<p style='font-size: 16px; font-weight: bold; color: #c62828;'>URGENT ACTION REQUIRED: The following items are completely OUT OF STOCK!</p>");
        messageBody.append("<p style='color: #d32f2f;'>These items cannot be used for any service bookings until restocked.</p>");
        messageBody.append("</div>");
        messageBody.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; margin-top: 20px; width: 100%;'>");
        messageBody.append("<tr style='background-color: #d32f2f; color: white;'>");
        messageBody.append("<th>Part ID</th>");
        messageBody.append("<th>Name</th>");
        messageBody.append("<th>Category</th>");
        messageBody.append("<th>Stock Status</th>");
        messageBody.append("<th>Minimum Stock</th>");
        messageBody.append("<th>Location</th>");
        messageBody.append("</tr>");
        
        for (InventoryItem item : zeroStockItems) {
            messageBody.append("<tr style='background-color: #ffcdd2;'>");
            messageBody.append("<td style='font-weight: bold;'>").append(item.getHexId()).append("</td>");
            messageBody.append("<td style='font-weight: bold;'>").append(item.getName()).append("</td>");
            messageBody.append("<td>").append(item.getCategory()).append("</td>");
            messageBody.append("<td style='color: #d32f2f; font-weight: bold; font-size: 18px; text-align: center;'>0 ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getMinimumStock()).append(" ").append(item.getUnit()).append("</td>");
            messageBody.append("<td>").append(item.getLocation()).append("</td>");
            messageBody.append("</tr>");
        }
        
        messageBody.append("</table>");
        messageBody.append("<div style='margin-top: 20px; padding: 15px; background-color: #fff3cd; border-left: 4px solid #ff6f00;'>");
        messageBody.append("<p style='margin: 0; font-weight: bold;'>⚠️ IMMEDIATE ACTIONS REQUIRED:</p>");
        messageBody.append("<ul>");
        messageBody.append("<li>Order these items IMMEDIATELY from suppliers</li>");
        messageBody.append("<li>Check if any pending bookings require these parts</li>");
        messageBody.append("<li>Consider expedited shipping if critical for upcoming services</li>");
        messageBody.append("<li>Update customers if their bookings are affected</li>");
        messageBody.append("</ul>");
        messageBody.append("</div>");
        messageBody.append("<p style='margin-top: 20px; color: #666; font-size: 12px;'>This is an automated CRITICAL alert from AutoTech Inventory Management System.</p>");
        messageBody.append("</body></html>");
        
        return sendEmail(
            ALERT_RECIPIENT,
            "🚨 CRITICAL: ZERO STOCK - " + zeroStockItems.size() + " Items OUT OF STOCK",
            messageBody.toString()
        );
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
     * Send expiration alert email for items nearing expiration or already expired
     */
    public boolean sendExpirationAlert(List<InventoryItem> expiringSoonItems, List<InventoryItem> expiredItems) {
        if ((expiringSoonItems == null || expiringSoonItems.isEmpty()) && 
            (expiredItems == null || expiredItems.isEmpty())) {
            return false;
        }
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<html><body>");
        messageBody.append("<h2>⏰ Inventory Expiration Alert - AutoTech</h2>");
        
        // Expired items section (critical)
        if (expiredItems != null && !expiredItems.isEmpty()) {
            messageBody.append("<h3 style='color: red;'>🚨 EXPIRED ITEMS (Immediate Action Required)</h3>");
            messageBody.append("<p style='color: red;'><strong>The following items have EXPIRED and should be removed immediately:</strong></p>");
            messageBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
            messageBody.append("<tr style='background-color: #ffcccc;'>");
            messageBody.append("<th>Part ID</th>");
            messageBody.append("<th>Name</th>");
            messageBody.append("<th>Category</th>");
            messageBody.append("<th>Quantity</th>");
            messageBody.append("<th>Expiration Date</th>");
            messageBody.append("<th>Location</th>");
            messageBody.append("</tr>");
            
            for (InventoryItem item : expiredItems) {
                messageBody.append("<tr>");
                messageBody.append("<td>").append(item.getHexId()).append("</td>");
                messageBody.append("<td>").append(item.getName()).append("</td>");
                messageBody.append("<td>").append(item.getCategory()).append("</td>");
                messageBody.append("<td>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
                messageBody.append("<td style='color: red; font-weight: bold;'>").append(item.getExpirationDate()).append("</td>");
                messageBody.append("<td>").append(item.getLocation()).append("</td>");
                messageBody.append("</tr>");
            }
            messageBody.append("</table>");
            messageBody.append("<br>");
        }
        
        // Expiring soon items section (warning)
        if (expiringSoonItems != null && !expiringSoonItems.isEmpty()) {
            messageBody.append("<h3 style='color: orange;'>⚠️ EXPIRING SOON (Within 30 Days)</h3>");
            messageBody.append("<p><strong>The following items will expire soon. Plan to use or dispose before expiration:</strong></p>");
            messageBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
            messageBody.append("<tr style='background-color: #fff3cd;'>");
            messageBody.append("<th>Part ID</th>");
            messageBody.append("<th>Name</th>");
            messageBody.append("<th>Category</th>");
            messageBody.append("<th>Quantity</th>");
            messageBody.append("<th>Expiration Date</th>");
            messageBody.append("<th>Days Until Expiration</th>");
            messageBody.append("<th>Location</th>");
            messageBody.append("</tr>");
            
            java.time.LocalDate today = java.time.LocalDate.now();
            for (InventoryItem item : expiringSoonItems) {
                long daysUntilExpiration = java.time.temporal.ChronoUnit.DAYS.between(today, item.getExpirationDate());
                messageBody.append("<tr>");
                messageBody.append("<td>").append(item.getHexId()).append("</td>");
                messageBody.append("<td>").append(item.getName()).append("</td>");
                messageBody.append("<td>").append(item.getCategory()).append("</td>");
                messageBody.append("<td>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
                messageBody.append("<td style='color: orange; font-weight: bold;'>").append(item.getExpirationDate()).append("</td>");
                messageBody.append("<td>").append(daysUntilExpiration).append(" days</td>");
                messageBody.append("<td>").append(item.getLocation()).append("</td>");
                messageBody.append("</tr>");
            }
            messageBody.append("</table>");
        }
        
        messageBody.append("<br><p><strong>Action Required:</strong>");
        if (expiredItems != null && !expiredItems.isEmpty()) {
            messageBody.append("<br>• Remove EXPIRED items from inventory immediately");
        }
        if (expiringSoonItems != null && !expiringSoonItems.isEmpty()) {
            messageBody.append("<br>• Use or dispose of items expiring soon");
        }
        messageBody.append("</p>");
        messageBody.append("<p>This is an automated alert from AutoTech Inventory Management System.</p>");
        messageBody.append("</body></html>");
        
        String subject = "⏰ Expiration Alert";
        if (expiredItems != null && !expiredItems.isEmpty()) {
            subject += " - " + expiredItems.size() + " EXPIRED";
        }
        if (expiringSoonItems != null && !expiringSoonItems.isEmpty()) {
            subject += " - " + expiringSoonItems.size() + " Expiring Soon";
        }
        
        return sendEmail(ALERT_RECIPIENT, subject, messageBody.toString());
    }
    
    /**
     * Send bill notification to customer with detailed breakdown
     */
    public boolean sendBillNotification(Customer customer, ServiceBookingViewModel booking, Bill bill) {
        if (customer == null || customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            return false;
        }
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<html><body>");
        messageBody.append("<div style='font-family: Arial, sans-serif; max-width: 700px; margin: 0 auto;'>");
        messageBody.append("<h2 style='color: #2196F3; border-bottom: 3px solid #2196F3; padding-bottom: 10px;'>Service Completed - AutoTech</h2>");
        messageBody.append("<p>Dear ").append(customer.getName()).append(",</p>");
        messageBody.append("<p>Your vehicle service has been completed successfully. Below is a detailed breakdown of the service and charges.</p>");
        
        // Service Details Section
        messageBody.append("<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;'>");
        messageBody.append("<h3 style='margin-top: 0; color: #333;'>📋 Service Details</h3>");
        messageBody.append("<table style='width: 100%; border-collapse: collapse;'>");
        messageBody.append("<tr><td style='padding: 5px 0; width: 40%;'><strong>Booking ID:</strong></td><td>").append(booking.getHexId()).append("</td></tr>");
        messageBody.append("<tr><td style='padding: 5px 0;'><strong>Service Type:</strong></td><td>").append(booking.getServiceType()).append("</td></tr>");
        messageBody.append("<tr><td style='padding: 5px 0;'><strong>Vehicle:</strong></td><td>").append(booking.getVehicle().getBrand()).append("</td></tr>");
        messageBody.append("<tr><td style='padding: 5px 0;'><strong>Service Date:</strong></td><td>").append(booking.getDate()).append("</td></tr>");
        if (booking.getMechanic() != null && booking.getMechanic().getName() != null) {
            messageBody.append("<tr><td style='padding: 5px 0;'><strong>Mechanic:</strong></td><td>").append(booking.getMechanic().getName()).append("</td></tr>");
        }
        if (booking.getServiceDescription() != null && !booking.getServiceDescription().isEmpty()) {
            messageBody.append("<tr><td style='padding: 5px 0; vertical-align: top;'><strong>Description:</strong></td><td>").append(booking.getServiceDescription()).append("</td></tr>");
        }
        messageBody.append("</table>");
        messageBody.append("</div>");
        
        // Get parts used from database
        double serviceCharge = 0.0;
        double partsCost = 0.0;
        java.util.List<BookingPart> parts = new java.util.ArrayList<>();
        
        try {
            ServiceBookingService bookingService = new ServiceBookingService();
            parts = bookingService.getBookingParts(booking.getId());
            
            // Calculate service charge from bill amount minus parts
            for (BookingPart part : parts) {
                partsCost += part.getQuantity() * part.getPrice();
            }
            serviceCharge = bill.getAmount() - partsCost;
        } catch (Exception e) {
            System.err.println("Error getting parts for email: " + e.getMessage());
            serviceCharge = bill.getAmount(); // Fallback to total amount
        }
        
        // Billing Breakdown Section
        messageBody.append("<div style='background-color: #e8f5e9; padding: 15px; border-radius: 5px; border-left: 4px solid #4CAF50; margin: 15px 0;'>");
        messageBody.append("<h3 style='margin-top: 0; color: #2e7d32;'>💰 Billing Breakdown</h3>");
        messageBody.append("<table style='width: 100%; border-collapse: collapse;'>");
        messageBody.append("<tr><td style='padding: 5px 0; width: 40%;'><strong>Bill ID:</strong></td><td>").append(bill.getHexId()).append("</td></tr>");
        messageBody.append("<tr><td style='padding: 5px 0;'><strong>Bill Date:</strong></td><td>").append(bill.getBillDate()).append("</td></tr>");
        messageBody.append("</table>");
        
        // Service Charge
        messageBody.append("<div style='margin-top: 15px;'>");
        messageBody.append("<h4 style='color: #1976D2; margin-bottom: 10px;'>Service Charge</h4>");
        messageBody.append("<table style='width: 100%; border-collapse: collapse; background-color: white;'>");
        messageBody.append("<tr style='border-bottom: 1px solid #ddd;'>");
        messageBody.append("<td style='padding: 8px;'>").append(booking.getServiceType()).append("</td>");
        messageBody.append("<td style='padding: 8px; text-align: right; font-weight: bold;'>₱").append(String.format("%.2f", serviceCharge)).append("</td>");
        messageBody.append("</tr>");
        messageBody.append("</table>");
        messageBody.append("</div>");
        
        // Parts Used
        if (!parts.isEmpty()) {
            messageBody.append("<div style='margin-top: 15px;'>");
            messageBody.append("<h4 style='color: #1976D2; margin-bottom: 10px;'>Parts & Materials Used</h4>");
            messageBody.append("<table style='width: 100%; border-collapse: collapse; background-color: white;'>");
            messageBody.append("<tr style='background-color: #e3f2fd; font-weight: bold;'>");
            messageBody.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Part Name</th>");
            messageBody.append("<th style='padding: 8px; text-align: center; border: 1px solid #ddd;'>Qty</th>");
            messageBody.append("<th style='padding: 8px; text-align: right; border: 1px solid #ddd;'>Unit Price</th>");
            messageBody.append("<th style='padding: 8px; text-align: right; border: 1px solid #ddd;'>Subtotal</th>");
            messageBody.append("</tr>");
            
            for (BookingPart part : parts) {
                double subtotal = part.getQuantity() * part.getPrice();
                messageBody.append("<tr style='border-bottom: 1px solid #ddd;'>");
                messageBody.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(part.getPartName()).append("</td>");
                messageBody.append("<td style='padding: 8px; text-align: center; border: 1px solid #ddd;'>").append(part.getQuantity()).append("</td>");
                messageBody.append("<td style='padding: 8px; text-align: right; border: 1px solid #ddd;'>₱").append(String.format("%.2f", part.getPrice())).append("</td>");
                messageBody.append("<td style='padding: 8px; text-align: right; border: 1px solid #ddd; font-weight: bold;'>₱").append(String.format("%.2f", subtotal)).append("</td>");
                messageBody.append("</tr>");
            }
            
            messageBody.append("<tr style='background-color: #f5f5f5; font-weight: bold;'>");
            messageBody.append("<td colspan='3' style='padding: 8px; text-align: right; border: 1px solid #ddd;'>Total Parts Cost:</td>");
            messageBody.append("<td style='padding: 8px; text-align: right; border: 1px solid #ddd;'>₱").append(String.format("%.2f", partsCost)).append("</td>");
            messageBody.append("</tr>");
            messageBody.append("</table>");
            messageBody.append("</div>");
        }
        
        // Total Amount
        messageBody.append("<div style='background-color: #2e7d32; color: white; padding: 15px; border-radius: 5px; margin-top: 15px;'>");
        messageBody.append("<table style='width: 100%;'>");
        messageBody.append("<tr><td style='font-size: 18px; font-weight: bold;'>TOTAL AMOUNT DUE:</td>");
        messageBody.append("<td style='font-size: 24px; font-weight: bold; text-align: right;'>₱").append(String.format("%.2f", bill.getAmount())).append("</td></tr>");
        messageBody.append("<tr><td style='padding-top: 5px;'>Payment Status:</td>");
        messageBody.append("<td style='text-align: right;'><span style='background-color: #ff9800; padding: 5px 15px; border-radius: 3px; font-weight: bold;'>")
                         .append(bill.getPaymentStatus().toUpperCase()).append("</span></td></tr>");
        messageBody.append("</table>");
        messageBody.append("</div>");
        
        messageBody.append("</div>");
        
        // Payment Instructions
        messageBody.append("<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107; margin: 15px 0;'>");
        messageBody.append("<h4 style='margin-top: 0; color: #856404;'>💳 Payment Instructions</h4>");
        messageBody.append("<p style='margin: 5px 0; color: #856404;'>Please proceed to our office to settle your payment at your earliest convenience.</p>");
        messageBody.append("<p style='margin: 5px 0; color: #856404;'>We accept cash and major credit/debit cards.</p>");
        messageBody.append("</div>");
        
        messageBody.append("<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>");
        messageBody.append("<p style='color: #666; font-size: 14px;'>Thank you for choosing <strong>AutoTech</strong> for your vehicle service needs!</p>");
        messageBody.append("<p style='color: #666; font-size: 12px;'>For inquiries or concerns, please contact us or visit our shop.</p>");
        messageBody.append("<p style='color: #999; font-size: 11px; margin-top: 20px;'>This is an automated message from AutoTech Service Management System.</p>");
        messageBody.append("</div>");
        messageBody.append("</body></html>");
        
        return sendEmail(
            customer.getEmail(),
            "✓ Service Completed - Invoice #" + bill.getHexId() + " (₱" + String.format("%.2f", bill.getAmount()) + ") - AutoTech",
            messageBody.toString()
        );
    }
    
    /**
     * Send receipt email to customer
     */
    public boolean sendReceiptEmail(String customerEmail, String receiptNumber, String customerName, 
                                    String amount, String htmlContent) {
        String subject = "Receipt #" + receiptNumber + " - AutoTech Service Receipt";
        
        try {
            return sendEmail(customerEmail, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send receipt email: " + e.getMessage());
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
