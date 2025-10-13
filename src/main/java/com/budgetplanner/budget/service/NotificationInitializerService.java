package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.NotificationPreference;
import com.budgetplanner.budget.model.NotificationTemplate;
import com.budgetplanner.budget.repository.NotificationPreferenceRepository;
import com.budgetplanner.budget.repository.NotificationTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Service to initialize default notification templates and preferences
 */
@Service
public class NotificationInitializerService {
    
    @Bean
    public CommandLineRunner initializeNotificationData(
            NotificationTemplateRepository templateRepository,
            NotificationPreferenceRepository preferenceRepository) {
        
        return args -> {
            // Create default templates if none exist
            if (templateRepository.count() == 0) {
                createDefaultTemplates(templateRepository);
            }
            
            // Create default preferences for default user if none exist
            if (preferenceRepository.count() == 0) {
                createDefaultPreferences(preferenceRepository);
            }
        };
    }
    
    private void createDefaultTemplates(NotificationTemplateRepository repository) {
        // AI Insight Template
        NotificationTemplate aiInsight = new NotificationTemplate(
            "ai_insight_default",
            "AI_INSIGHT",
            "BOTH"
        );
        aiInsight.setEmailSubject("💡 New AI Insight: {title}");
        aiInsight.setEmailBody(
            "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px;'>" +
            "<h2 style='color: #00d4ff; margin-top: 0;'>💡 New AI Insight</h2>" +
            "<h3 style='color: #333;'>{title}</h3>" +
            "<p style='color: #666; line-height: 1.6;'>{message}</p>" +
            "<p style='color: #999; font-size: 12px; margin-top: 30px;'>Received on {date}</p>" +
            "</div></body></html>"
        );
        aiInsight.setSmsBody("💡 AI Insight: {title}. {message}");
        aiInsight.setAvailablePlaceholders("{title}, {message}, {date}");
        repository.save(aiInsight);
        
        // Budget Alert Template
        NotificationTemplate budgetAlert = new NotificationTemplate(
            "budget_alert_default",
            "BUDGET_ALERT",
            "BOTH"
        );
        budgetAlert.setEmailSubject("⚠️ Budget Alert: {title}");
        budgetAlert.setEmailBody(
            "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px;'>" +
            "<h2 style='color: #f87171; margin-top: 0;'>⚠️ Budget Alert</h2>" +
            "<h3 style='color: #333;'>{title}</h3>" +
            "<p style='color: #666; line-height: 1.6; font-weight: bold;'>{message}</p>" +
            "<p style='background: #fff3cd; padding: 15px; border-left: 4px solid #f87171; margin: 20px 0;'>" +
            "Please review your spending to stay on track with your budget.</p>" +
            "<p style='color: #999; font-size: 12px; margin-top: 30px;'>Alert sent on {date}</p>" +
            "</div></body></html>"
        );
        budgetAlert.setSmsBody("⚠️ Budget Alert: {title}. {message}");
        budgetAlert.setAvailablePlaceholders("{title}, {message}, {date}");
        repository.save(budgetAlert);
        
        // Savings Tip Template
        NotificationTemplate savingsTip = new NotificationTemplate(
            "savings_tip_default",
            "SAVINGS_TIP",
            "BOTH"
        );
        savingsTip.setEmailSubject("💰 Savings Tip: {title}");
        savingsTip.setEmailBody(
            "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px;'>" +
            "<h2 style='color: #4ade80; margin-top: 0;'>💰 Savings Tip</h2>" +
            "<h3 style='color: #333;'>{title}</h3>" +
            "<p style='color: #666; line-height: 1.6;'>{message}</p>" +
            "<p style='background: #d1fae5; padding: 15px; border-left: 4px solid #4ade80; margin: 20px 0;'>" +
            "Small changes can lead to big savings over time!</p>" +
            "<p style='color: #999; font-size: 12px; margin-top: 30px;'>Tip sent on {date}</p>" +
            "</div></body></html>"
        );
        savingsTip.setSmsBody("💰 Savings Tip: {title}. {message}");
        savingsTip.setAvailablePlaceholders("{title}, {message}, {date}");
        repository.save(savingsTip);
        
        // Recurring Reminder Template
        NotificationTemplate recurringReminder = new NotificationTemplate(
            "recurring_reminder_default",
            "RECURRING_REMINDER",
            "BOTH"
        );
        recurringReminder.setEmailSubject("🔔 Payment Reminder: {title}");
        recurringReminder.setEmailBody(
            "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px;'>" +
            "<h2 style='color: #fbbf24; margin-top: 0;'>🔔 Payment Reminder</h2>" +
            "<h3 style='color: #333;'>{title}</h3>" +
            "<p style='color: #666; line-height: 1.6;'>{message}</p>" +
            "<p style='background: #fef3c7; padding: 15px; border-left: 4px solid #fbbf24; margin: 20px 0;'>" +
            "Don't forget to make this payment to avoid late fees.</p>" +
            "<p style='color: #999; font-size: 12px; margin-top: 30px;'>Reminder sent on {date}</p>" +
            "</div></body></html>"
        );
        recurringReminder.setSmsBody("🔔 Reminder: {title}. {message}");
        recurringReminder.setAvailablePlaceholders("{title}, {message}, {date}");
        repository.save(recurringReminder);
        
        System.out.println("✅ Default notification templates created successfully!");
    }
    
    private void createDefaultPreferences(NotificationPreferenceRepository repository) {
        NotificationPreference defaultPrefs = new NotificationPreference("default_user");
        defaultPrefs.setEmailAddress("user@example.com"); // User should update this
        defaultPrefs.setPhoneNumber(""); // User should update this
        defaultPrefs.setEmailEnabled(true);
        defaultPrefs.setSmsEnabled(false);
        defaultPrefs.setAiInsightsEnabled(true);
        defaultPrefs.setBudgetAlertsEnabled(true);
        defaultPrefs.setSavingsTipsEnabled(true);
        defaultPrefs.setRecurringRemindersEnabled(true);
        
        repository.save(defaultPrefs);
        
        System.out.println("✅ Default notification preferences created successfully!");
    }
}
