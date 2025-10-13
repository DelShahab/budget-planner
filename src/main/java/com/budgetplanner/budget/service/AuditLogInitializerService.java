package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.AuditLog;
import com.budgetplanner.budget.repository.AuditLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service to initialize sample audit log data
 */
@Service
public class AuditLogInitializerService {
    
    @Bean
    public CommandLineRunner initializeAuditLogs(AuditLogRepository auditLogRepository) {
        return args -> {
            if (auditLogRepository.count() == 0) {
                createSampleAuditLogs(auditLogRepository);
            }
        };
    }
    
    private void createSampleAuditLogs(AuditLogRepository repository) {
        // Transaction-related logs
        AuditLog log1 = new AuditLog("TRANSACTION", "TXN-001", "CREATE", "default_user");
        log1.setUserName("Budget Planner User");
        log1.setDescription("Added new transaction: Grocery Shopping at Whole Foods");
        log1.setNewValue("Amount: $156.42, Category: Groceries");
        log1.setCategory("FINANCIAL");
        log1.setSeverity("INFO");
        log1.setTimestamp(LocalDateTime.now().minusHours(2));
        repository.save(log1);
        
        AuditLog log2 = new AuditLog("TRANSACTION", "TXN-002", "UPDATE", "default_user");
        log2.setUserName("Budget Planner User");
        log2.setDescription("Updated transaction: Netflix Subscription");
        log2.setOldValue("Amount: $15.99, Category: Entertainment");
        log2.setNewValue("Amount: $17.99, Category: Entertainment");
        log2.setCategory("FINANCIAL");
        log2.setSeverity("INFO");
        log2.setTimestamp(LocalDateTime.now().minusHours(5));
        repository.save(log2);
        
        AuditLog log3 = new AuditLog("TRANSACTION", "TXN-003", "DELETE", "default_user");
        log3.setUserName("Budget Planner User");
        log3.setDescription("Deleted duplicate transaction: Coffee Shop");
        log3.setOldValue("Amount: $4.50, Category: Food & Dining");
        log3.setCategory("FINANCIAL");
        log3.setSeverity("WARNING");
        log3.setTimestamp(LocalDateTime.now().minusHours(8));
        repository.save(log3);
        
        // Budget-related logs
        AuditLog log4 = new AuditLog("BUDGET_ITEM", "BUDGET-001", "CREATE", "default_user");
        log4.setUserName("Budget Planner User");
        log4.setDescription("Created new budget item: Groceries");
        log4.setNewValue("Planned: $500.00, Actual: $0.00");
        log4.setCategory("FINANCIAL");
        log4.setSeverity("INFO");
        log4.setTimestamp(LocalDateTime.now().minusDays(1));
        repository.save(log4);
        
        AuditLog log5 = new AuditLog("BUDGET_ITEM", "BUDGET-002", "UPDATE", "default_user");
        log5.setUserName("Budget Planner User");
        log5.setDescription("Updated budget allocation: Rent");
        log5.setOldValue("Planned: $1200.00");
        log5.setNewValue("Planned: $1250.00");
        log5.setCategory("FINANCIAL");
        log5.setSeverity("INFO");
        log5.setTimestamp(LocalDateTime.now().minusDays(2));
        repository.save(log5);
        
        // User profile logs
        AuditLog log6 = new AuditLog("USER_PROFILE", "default_user", "UPDATE", "default_user");
        log6.setUserName("Budget Planner User");
        log6.setDescription("Updated profile information");
        log6.setOldValue("Email: user@example.com");
        log6.setNewValue("Email: user@budgetplanner.com");
        log6.setCategory("USER_ACTION");
        log6.setSeverity("INFO");
        log6.setTimestamp(LocalDateTime.now().minusDays(3));
        repository.save(log6);
        
        AuditLog log7 = new AuditLog("USER_PROFILE", "default_user", "UPDATE", "default_user");
        log7.setUserName("Budget Planner User");
        log7.setDescription("Uploaded new profile avatar");
        log7.setCategory("USER_ACTION");
        log7.setSeverity("INFO");
        log7.setTimestamp(LocalDateTime.now().minusDays(4));
        repository.save(log7);
        
        // Security logs
        AuditLog log8 = new AuditLog("SECURITY", "N/A", "PASSWORD_CHANGE", "default_user");
        log8.setUserName("Budget Planner User");
        log8.setDescription("Password changed successfully");
        log8.setCategory("SECURITY");
        log8.setSeverity("INFO");
        log8.setTimestamp(LocalDateTime.now().minusDays(5));
        repository.save(log8);
        
        AuditLog log9 = new AuditLog("SECURITY", "N/A", "LOGIN", "default_user");
        log9.setUserName("Budget Planner User");
        log9.setDescription("User logged in successfully");
        log9.setIpAddress("192.168.1.1");
        log9.setCategory("SECURITY");
        log9.setSeverity("INFO");
        log9.setTimestamp(LocalDateTime.now().minusHours(1));
        repository.save(log9);
        
        // Bank account logs
        AuditLog log10 = new AuditLog("BANK_ACCOUNT", "BANK-001", "SYNC", "default_user");
        log10.setUserName("Budget Planner User");
        log10.setDescription("Synced transactions from Chase Checking");
        log10.setNewValue("Downloaded 23 new transactions");
        log10.setCategory("FINANCIAL");
        log10.setSeverity("INFO");
        log10.setTimestamp(LocalDateTime.now().minusHours(3));
        repository.save(log10);
        
        // System logs
        AuditLog log11 = new AuditLog("SYSTEM", "N/A", "DATA_IMPORT", "SYSTEM");
        log11.setUserName("System");
        log11.setDescription("Imported recurring transaction patterns");
        log11.setNewValue("Detected 5 recurring payments");
        log11.setCategory("SYSTEM");
        log11.setSeverity("INFO");
        log11.setTimestamp(LocalDateTime.now().minusDays(1).minusHours(6));
        repository.save(log11);
        
        AuditLog log12 = new AuditLog("SYSTEM", "N/A", "BACKUP", "SYSTEM");
        log12.setUserName("System");
        log12.setDescription("Database backup completed successfully");
        log12.setCategory("SYSTEM");
        log12.setSeverity("INFO");
        log12.setTimestamp(LocalDateTime.now().minusDays(7));
        repository.save(log12);
        
        System.out.println("✅ Sample audit logs created successfully!");
    }
}
