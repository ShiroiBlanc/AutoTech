package com.example;

import java.time.LocalDateTime;

public class EmailSentRecord {
    private int id;
    private int billingId;
    private String billId;
    private String recipientEmail;
    private String emailType;
    private String subject;
    private LocalDateTime sentDate;
    private String status;
    private String notes;
    
    public EmailSentRecord(int id, int billingId, String billId, String recipientEmail, String emailType,
                          String subject, LocalDateTime sentDate, String status, String notes) {
        this.id = id;
        this.billingId = billingId;
        this.billId = billId;
        this.recipientEmail = recipientEmail;
        this.emailType = emailType;
        this.subject = subject;
        this.sentDate = sentDate;
        this.status = status;
        this.notes = notes;
    }
    
    // Getters
    public int getId() { return id; }
    public int getBillingId() { return billingId; }
    public String getBillId() { return billId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getEmailType() { return emailType; }
    public String getSubject() { return subject; }
    public LocalDateTime getSentDate() { return sentDate; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    
    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
}
