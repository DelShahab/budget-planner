package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId; // For multi-user support
    
    @Column(nullable = false)
    private Boolean emailEnabled = true;
    
    @Column(nullable = false)
    private Boolean smsEnabled = false;
    
    @Column
    private String emailAddress;
    
    @Column
    private String phoneNumber;
    
    // Category-specific preferences
    @Column(nullable = false)
    private Boolean aiInsightsEnabled = true;
    
    @Column(nullable = false)
    private Boolean budgetAlertsEnabled = true;
    
    @Column(nullable = false)
    private Boolean savingsTipsEnabled = true;
    
    @Column(nullable = false)
    private Boolean recurringRemindersEnabled = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    public NotificationPreference() {
        this.createdAt = LocalDateTime.now();
        this.emailEnabled = true;
        this.smsEnabled = false;
        this.aiInsightsEnabled = true;
        this.budgetAlertsEnabled = true;
        this.savingsTipsEnabled = true;
        this.recurringRemindersEnabled = true;
    }
    
    public NotificationPreference(String userId) {
        this();
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getAiInsightsEnabled() {
        return aiInsightsEnabled;
    }

    public void setAiInsightsEnabled(Boolean aiInsightsEnabled) {
        this.aiInsightsEnabled = aiInsightsEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getBudgetAlertsEnabled() {
        return budgetAlertsEnabled;
    }

    public void setBudgetAlertsEnabled(Boolean budgetAlertsEnabled) {
        this.budgetAlertsEnabled = budgetAlertsEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getSavingsTipsEnabled() {
        return savingsTipsEnabled;
    }

    public void setSavingsTipsEnabled(Boolean savingsTipsEnabled) {
        this.savingsTipsEnabled = savingsTipsEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getRecurringRemindersEnabled() {
        return recurringRemindersEnabled;
    }

    public void setRecurringRemindersEnabled(Boolean recurringRemindersEnabled) {
        this.recurringRemindersEnabled = recurringRemindersEnabled;
        this.updatedAt = LocalDateTime.now();
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
}
