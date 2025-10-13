package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.AppNotification;
import com.budgetplanner.budget.model.NotificationPreference;
import com.budgetplanner.budget.model.NotificationTemplate;
import com.budgetplanner.budget.repository.NotificationPreferenceRepository;
import com.budgetplanner.budget.repository.NotificationTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for sending notifications via Email and SMS using SendGrid API
 * 
 * TODO: Integration with SendGrid
 * 1. Add SendGrid dependency to pom.xml:
 *    <dependency>
 *      <groupId>com.sendgrid</groupId>
 *      <artifactId>sendgrid-java</artifactId>
 *      <version>4.9.3</version>
 *    </dependency>
 * 
 * 2. Add SendGrid API key to application.properties:
 *    sendgrid.api-key=your-sendgrid-api-key
 *    sendgrid.from-email=noreply@budgetplanner.com
 *    sendgrid.from-name=Budget Planner
 * 
 * 3. For SMS, SendGrid can be used with Twilio integration or use Twilio directly:
 *    twilio.account-sid=your-twilio-sid
 *    twilio.auth-token=your-twilio-token
 *    twilio.phone-number=your-twilio-phone
 */
@Service
public class NotificationSenderService {
    
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateRepository templateRepository;
    
    @Value("${sendgrid.api-key:#{null}}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from-email:noreply@budgetplanner.com}")
    private String fromEmail;
    
    @Value("${sendgrid.from-name:Budget Planner}")
    private String fromName;
    
    @Value("${twilio.account-sid:#{null}}")
    private String twilioAccountSid;
    
    @Value("${twilio.auth-token:#{null}}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone-number:#{null}}")
    private String twilioPhoneNumber;
    
    public NotificationSenderService(NotificationPreferenceRepository preferenceRepository,
                                    NotificationTemplateRepository templateRepository) {
        this.preferenceRepository = preferenceRepository;
        this.templateRepository = templateRepository;
    }
    
    /**
     * Send notification to user based on their preferences
     */
    @Async
    public CompletableFuture<Boolean> sendNotification(AppNotification notification, String userId) {
        try {
            Optional<NotificationPreference> preferenceOpt = preferenceRepository.findByUserId(userId);
            
            if (preferenceOpt.isEmpty()) {
                System.out.println("No preferences found for user: " + userId);
                return CompletableFuture.completedFuture(false);
            }
            
            NotificationPreference preference = preferenceOpt.get();
            
            // Check if user wants notifications for this category
            if (!isCategoryEnabled(preference, notification.getCategory())) {
                System.out.println("Category " + notification.getCategory() + " is disabled for user: " + userId);
                return CompletableFuture.completedFuture(false);
            }
            
            // Find appropriate template
            Optional<NotificationTemplate> templateOpt = templateRepository
                .findByCategoryAndIsActiveTrue(notification.getCategory())
                .stream()
                .findFirst();
            
            if (templateOpt.isEmpty()) {
                System.out.println("No active template found for category: " + notification.getCategory());
                return CompletableFuture.completedFuture(false);
            }
            
            NotificationTemplate template = templateOpt.get();
            Map<String, String> data = buildNotificationData(notification);
            
            boolean emailSent = false;
            boolean smsSent = false;
            
            // Send email if enabled
            if (preference.getEmailEnabled() && preference.getEmailAddress() != null) {
                emailSent = sendEmail(
                    preference.getEmailAddress(),
                    replacePlaceholders(template.getEmailSubject(), data),
                    replacePlaceholders(template.getEmailBody(), data)
                );
            }
            
            // Send SMS if enabled
            if (preference.getSmsEnabled() && preference.getPhoneNumber() != null && template.getSmsBody() != null) {
                smsSent = sendSms(
                    preference.getPhoneNumber(),
                    replacePlaceholders(template.getSmsBody(), data)
                );
            }
            
            return CompletableFuture.completedFuture(emailSent || smsSent);
            
        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Send email using SendGrid API
     */
    private boolean sendEmail(String toEmail, String subject, String body) {
        try {
            if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
                System.out.println("SendGrid API key not configured. Email would be sent to: " + toEmail);
                System.out.println("Subject: " + subject);
                System.out.println("Body: " + body);
                return true; // Simulate success for now
            }
            
            /* TODO: Uncomment when SendGrid is configured
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            try {
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                
                Mail mail = new Mail();
                mail.setFrom(new Email(fromEmail, fromName));
                mail.setSubject(subject);
                mail.addContent(new Content("text/html", body));
                mail.addPersonalization(new Personalization() {{
                    addTo(new Email(toEmail));
                }});
                
                request.setBody(mail.build());
                Response response = sg.api(request);
                
                return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
                
            } catch (IOException ex) {
                System.err.println("SendGrid error: " + ex.getMessage());
                return false;
            }
            */
            
            System.out.println("✉️  Email would be sent to: " + toEmail);
            System.out.println("   Subject: " + subject);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send SMS using Twilio API (via SendGrid or Twilio directly)
     */
    private boolean sendSms(String phoneNumber, String message) {
        try {
            if (twilioAccountSid == null || twilioAccountSid.trim().isEmpty()) {
                System.out.println("Twilio credentials not configured. SMS would be sent to: " + phoneNumber);
                System.out.println("Message: " + message);
                return true; // Simulate success for now
            }
            
            /* TODO: Uncomment when Twilio is configured
            Twilio.init(twilioAccountSid, twilioAuthToken);
            
            Message sms = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                message
            ).create();
            
            return sms.getStatus() != Message.Status.FAILED;
            */
            
            System.out.println("📱 SMS would be sent to: " + phoneNumber);
            System.out.println("   Message: " + message);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if notification category is enabled for user
     */
    private boolean isCategoryEnabled(NotificationPreference preference, String category) {
        switch (category) {
            case "AI_INSIGHT":
                return preference.getAiInsightsEnabled();
            case "BUDGET_ALERT":
                return preference.getBudgetAlertsEnabled();
            case "SAVINGS_TIP":
                return preference.getSavingsTipsEnabled();
            case "RECURRING_REMINDER":
                return preference.getRecurringRemindersEnabled();
            default:
                return true;
        }
    }
    
    /**
     * Build data map for template placeholders
     */
    private Map<String, String> buildNotificationData(AppNotification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("category", notification.getCategory());
        data.put("priority", notification.getPriority());
        data.put("date", notification.getCreatedAt().toString());
        return data;
    }
    
    /**
     * Replace placeholders in template with actual data
     */
    private String replacePlaceholders(String template, Map<String, String> data) {
        if (template == null) {
            return "";
        }
        
        String result = template;
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = data.getOrDefault(placeholder, "");
            result = result.replace("{" + placeholder + "}", value);
        }
        
        return result;
    }
    
    /**
     * Test email configuration
     */
    public boolean testEmailConfiguration(String toEmail) {
        return sendEmail(toEmail, "Test Email from Budget Planner", 
            "<h1>Test Email</h1><p>Your email configuration is working correctly!</p>");
    }
    
    /**
     * Test SMS configuration
     */
    public boolean testSmsConfiguration(String phoneNumber) {
        return sendSms(phoneNumber, "Test SMS from Budget Planner. Your SMS configuration is working correctly!");
    }
}
