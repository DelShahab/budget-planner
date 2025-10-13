package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String templateName;
    
    @Column(nullable = false)
    private String category; // AI_INSIGHT, BUDGET_ALERT, SAVINGS_TIP, RECURRING_REMINDER
    
    @Column(nullable = false)
    private String channelType; // EMAIL, SMS, BOTH
    
    @Column(nullable = false)
    private String emailSubject;
    
    @Column(nullable = false, length = 2000)
    private String emailBody;
    
    @Column(length = 500)
    private String smsBody;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    // Placeholders for dynamic content: {userName}, {amount}, {category}, {date}, etc.
    @Column(length = 500)
    private String availablePlaceholders;
    
    public NotificationTemplate() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    public NotificationTemplate(String templateName, String category, String channelType) {
        this();
        this.templateName = templateName;
        this.category = category;
        this.channelType = channelType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public String getSmsBody() {
        return smsBody;
    }

    public void setSmsBody(String smsBody) {
        this.smsBody = smsBody;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAvailablePlaceholders() {
        return availablePlaceholders;
    }

    public void setAvailablePlaceholders(String availablePlaceholders) {
        this.availablePlaceholders = availablePlaceholders;
    }
}
