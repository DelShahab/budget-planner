package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.AuditLog;
import com.budgetplanner.budget.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private static final String DEFAULT_USER_ID = "default_user";
    
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Log a transaction-related action
     */
    public void logTransactionAction(String action, String transactionId, String description, String oldValue, String newValue) {
        AuditLog log = new AuditLog("TRANSACTION", transactionId, action, DEFAULT_USER_ID);
        log.setUserName("Budget Planner User");
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setCategory("FINANCIAL");
        log.setSeverity("INFO");
        auditLogRepository.save(log);
    }
    
    /**
     * Log a user profile change
     */
    public void logUserAction(String action, String description, String oldValue, String newValue) {
        AuditLog log = new AuditLog("USER_PROFILE", DEFAULT_USER_ID, action, DEFAULT_USER_ID);
        log.setUserName("Budget Planner User");
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setCategory("USER_ACTION");
        log.setSeverity("INFO");
        auditLogRepository.save(log);
    }
    
    /**
     * Log a budget item change
     */
    public void logBudgetAction(String action, String budgetItemId, String description, String oldValue, String newValue) {
        AuditLog log = new AuditLog("BUDGET_ITEM", budgetItemId, action, DEFAULT_USER_ID);
        log.setUserName("Budget Planner User");
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setCategory("FINANCIAL");
        log.setSeverity("INFO");
        auditLogRepository.save(log);
    }
    
    /**
     * Log a security-related action
     */
    public void logSecurityAction(String action, String description, String severity) {
        AuditLog log = new AuditLog("SECURITY", "N/A", action, DEFAULT_USER_ID);
        log.setUserName("Budget Planner User");
        log.setDescription(description);
        log.setCategory("SECURITY");
        log.setSeverity(severity);
        auditLogRepository.save(log);
    }
    
    /**
     * Log a system action
     */
    public void logSystemAction(String action, String description) {
        AuditLog log = new AuditLog("SYSTEM", "N/A", action, "SYSTEM");
        log.setUserName("System");
        log.setDescription(description);
        log.setCategory("SYSTEM");
        log.setSeverity("INFO");
        auditLogRepository.save(log);
    }
    
    /**
     * Get all audit logs
     */
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
    
    /**
     * Get logs by category
     */
    public List<AuditLog> getLogsByCategory(String category) {
        return auditLogRepository.findByCategoryOrderByTimestampDesc(category);
    }
    
    /**
     * Get logs by entity type
     */
    public List<AuditLog> getLogsByEntityType(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }
    
    /**
     * Get recent logs
     */
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findRecentLogs(limit);
    }
    
    /**
     * Search logs
     */
    public List<AuditLog> searchLogs(String keyword) {
        return auditLogRepository.findByDescriptionContainingIgnoreCaseOrderByTimestampDesc(keyword);
    }
    
    /**
     * Get logs within date range
     */
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
    
    /**
     * Get statistics
     */
    public Long getCountByCategory(String category) {
        return auditLogRepository.countByCategory(category);
    }
    
    public Long getCountBySeverity(String severity) {
        return auditLogRepository.countBySeverity(severity);
    }
}
