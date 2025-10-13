package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String entityType; // TRANSACTION, USER_PROFILE, BUDGET_ITEM, BANK_ACCOUNT, etc.
    
    @Column(nullable = false)
    private String entityId; // ID of the modified entity
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, SYNC, LOGIN, etc.
    
    @Column(nullable = false)
    private String userId; // User who performed the action
    
    @Column
    private String userName; // User's display name
    
    @Column(length = 2000)
    private String description; // Human-readable description
    
    @Column(length = 5000)
    private String oldValue; // Previous value (JSON or text)
    
    @Column(length = 5000)
    private String newValue; // New value (JSON or text)
    
    @Column
    private String ipAddress; // IP address of the user
    
    @Column
    private String category; // For filtering: FINANCIAL, SECURITY, SYSTEM, USER_ACTION
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column
    private String severity; // INFO, WARNING, ERROR, CRITICAL
    
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.severity = "INFO";
    }
    
    public AuditLog(String entityType, String entityId, String action, String userId) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
