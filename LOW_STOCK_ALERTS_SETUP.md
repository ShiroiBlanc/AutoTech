# Low Stock Alert Setup Guide

## Overview
The system can now automatically monitor inventory levels and send email alerts when items fall below their minimum stock threshold.

## Components Added

### 1. EmailService.java
- Handles sending email notifications using SMTP
- Formats low stock alerts with HTML table of items
- Configurable SMTP settings

### 2. StockMonitorService.java
- Runs background checks every 6 hours (configurable)
- Identifies items below minimum stock
- Triggers email alerts automatically

### 3. Dependencies
- Added JavaMail (javax.mail) to pom.xml for email functionality

## Setup Instructions

### Step 1: Configure Email Settings

Edit `EmailService.java` and update these constants:

```java
private static final String SMTP_HOST = "smtp.gmail.com"; // Your SMTP server
private static final String SMTP_PORT = "587"; // Port (587 for TLS)
private static final String SENDER_EMAIL = "your-email@gmail.com"; // Sender email
private static final String SENDER_PASSWORD = "your-app-password"; // Email password
private static final String ALERT_RECIPIENT = "manager@autotech.com"; // Where to send alerts
```

### Step 2: Gmail Configuration (if using Gmail)

1. Go to your Google Account settings
2. Enable 2-factor authentication
3. Generate an "App Password":
   - Go to Security → 2-Step Verification → App passwords
   - Select "Mail" and your device
   - Copy the generated 16-character password
   - Use this as `SENDER_PASSWORD` (no spaces)

### Step 3: Alternative Email Providers

#### Outlook/Hotmail:
```java
SMTP_HOST = "smtp-mail.outlook.com"
SMTP_PORT = "587"
```

#### Yahoo Mail:
```java
SMTP_HOST = "smtp.mail.yahoo.com"
SMTP_PORT = "587"
```

#### Custom SMTP Server:
```java
SMTP_HOST = "mail.yourdomain.com"
SMTP_PORT = "587" // or 465 for SSL
```

### Step 4: Start Monitoring

Add this to your `App.java` main method or application startup:

```java
// Start stock monitoring on application startup
StockMonitorService.getInstance().startMonitoring();
```

### Step 5: Update Dependencies

Run this command to download the JavaMail library:
```bash
mvn clean install
```

## Usage

### Automatic Monitoring
Once started, the system automatically:
1. Checks inventory every 6 hours
2. Identifies items where `quantity < minimumStock`
3. Sends formatted email alert with low stock items
4. Continues monitoring until application shutdown

### Manual Check
Trigger an immediate stock check:
```java
StockMonitorService.getInstance().checkStockNow();
```

### Stop Monitoring
```java
StockMonitorService.getInstance().stopMonitoring();
```

### Test Email Configuration
```java
boolean success = EmailService.getInstance().testEmailConfiguration();
```

## Email Alert Format

The alert email includes:
- Subject: "⚠️ Low Stock Alert - X Items Need Reordering"
- HTML table with:
  - Part Number
  - Name
  - Category
  - Current Stock (in red if low)
  - Minimum Stock threshold
  - Storage Location
- Action required message

## Customization

### Change Check Interval
Edit `StockMonitorService.java`:
```java
// Check every 3 hours instead of 6
private static final long CHECK_INTERVAL = 3 * 60 * 60 * 1000;

// Or check every 30 minutes for testing
private static final long CHECK_INTERVAL = 30 * 60 * 1000;
```

### Multiple Recipients
Modify `sendLowStockAlert()` to send to multiple people:
```java
String recipients = "manager@autotech.com,admin@autotech.com,warehouse@autotech.com";
message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
```

### Add SMS Alerts
For SMS, integrate with services like:
- **Twilio** (most popular)
- **AWS SNS**
- **Nexmo/Vonage**

## Troubleshooting

### Email Not Sending
1. Check SMTP credentials are correct
2. Verify port is open (587 or 465)
3. Check email provider allows SMTP
4. For Gmail, ensure App Password is used (not regular password)
5. Check firewall/antivirus isn't blocking SMTP

### Authentication Errors
- Gmail: Must use App Password with 2FA enabled
- Corporate email: May need to whitelist IP or use specific authentication method

### Testing
```java
// Test email configuration
EmailService.getInstance().testEmailConfiguration();

// Manually trigger stock check
StockMonitorService.getInstance().checkStockNow();
```

## Security Recommendations

1. **Don't hardcode passwords** in production:
   - Use environment variables
   - Use configuration files (excluded from git)
   - Use secrets management system

2. **Example using environment variables**:
```java
private static final String SENDER_PASSWORD = System.getenv("SMTP_PASSWORD");
```

3. **Add to .gitignore**:
```
# Email configuration
email.properties
```

## Future Enhancements

- SMS notifications via Twilio
- Push notifications
- Configurable alert thresholds per item
- Daily summary reports
- Alert history/audit log
- Multiple notification channels
- Alert escalation (if not addressed within X hours)
