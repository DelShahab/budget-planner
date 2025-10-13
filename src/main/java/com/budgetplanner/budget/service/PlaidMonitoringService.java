package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.budgetplanner.budget.service.SimplifiedEnhancedPlaidService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for monitoring Plaid connections and performing scheduled maintenance tasks
 */
@Service
public class PlaidMonitoringService {

    private final SimplifiedEnhancedPlaidService enhancedPlaidService;
    private final BankAccountRepository bankAccountRepository;

    public PlaidMonitoringService(SimplifiedEnhancedPlaidService enhancedPlaidService,
                                 BankAccountRepository bankAccountRepository) {
        this.enhancedPlaidService = enhancedPlaidService;
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * Scheduled task to check connection status for all bank accounts
     * Runs every hour to detect expired or invalid connections
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void monitorConnectionStatus() {
        System.out.println("=== STARTING SCHEDULED CONNECTION MONITORING ===");
        
        try {
            Map<Long, SimplifiedEnhancedPlaidService.ConnectionStatus> statuses = 
                enhancedPlaidService.checkAllConnectionStatuses();
            
            int activeConnections = 0;
            int expiredConnections = 0;
            int errorConnections = 0;
            
            for (Map.Entry<Long, SimplifiedEnhancedPlaidService.ConnectionStatus> entry : statuses.entrySet()) {
                Long accountId = entry.getKey();
                SimplifiedEnhancedPlaidService.ConnectionStatus status = entry.getValue();
                
                switch (status) {
                    case ACTIVE:
                        activeConnections++;
                        break;
                    case EXPIRED:
                        expiredConnections++;
                        handleExpiredConnection(accountId);
                        break;
                    case INVALID:
                    case ERROR:
                        errorConnections++;
                        handleErrorConnection(accountId);
                        break;
                    case MAINTENANCE_REQUIRED:
                        handleMaintenanceRequired(accountId);
                        break;
                }
            }
            
            System.out.printf("Connection Status Summary: %d active, %d expired, %d errors%n",
                             activeConnections, expiredConnections, errorConnections);
            
        } catch (Exception e) {
            System.err.println("Error during connection monitoring: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== CONNECTION MONITORING COMPLETE ===\n");
    }

    /**
     * Scheduled task to sync transactions for all active accounts
     * Runs every 6 hours to keep transaction data up to date
     */
    @Scheduled(fixedRate = 21600000) // Every 6 hours
    public void scheduledTransactionSync() {
        System.out.println("=== STARTING SCHEDULED TRANSACTION SYNC ===");
        
        try {
            List<BankAccount> activeAccounts = bankAccountRepository.findByIsActiveTrue();
            System.out.printf("Found %d active accounts for scheduled sync%n", activeAccounts.size());
            
            // Process accounts in parallel for better performance
            List<CompletableFuture<Integer>> syncTasks = activeAccounts.stream()
                .map(account -> enhancedPlaidService.syncTransactionsForAccountAsync(account))
                .toList();
            
            // Wait for all sync tasks to complete
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                syncTasks.toArray(new CompletableFuture[0]));
            
            allTasks.join(); // Wait for completion
            
            // Calculate total synced transactions
            int totalSynced = syncTasks.stream()
                .mapToInt(task -> {
                    try {
                        return task.get();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            System.out.printf("Scheduled sync complete: %d transactions synced across %d accounts%n",
                             totalSynced, activeAccounts.size());
            
        } catch (Exception e) {
            System.err.println("Error during scheduled transaction sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== SCHEDULED TRANSACTION SYNC COMPLETE ===\n");
    }

    /**
     * Scheduled task to clean up old transaction data and perform maintenance
     * Runs daily at 2 AM to perform housekeeping tasks
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performMaintenance() {
        System.out.println("=== STARTING SCHEDULED MAINTENANCE ===");
        
        try {
            // Clean up old error logs
            cleanupErrorLogs();
            
            // Update account statistics
            updateAccountStatistics();
            
            // Perform database maintenance
            performDatabaseMaintenance();
            
            System.out.println("Scheduled maintenance completed successfully");
            
        } catch (Exception e) {
            System.err.println("Error during scheduled maintenance: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== SCHEDULED MAINTENANCE COMPLETE ===\n");
    }

    /**
     * On-demand health check for all bank connections
     */
    public Map<String, Object> performHealthCheck() {
        System.out.println("Performing on-demand health check...");
        
        List<BankAccount> allAccounts = bankAccountRepository.findAll();
        Map<Long, SimplifiedEnhancedPlaidService.ConnectionStatus> statuses = 
            enhancedPlaidService.checkAllConnectionStatuses();
        
        long activeCount = statuses.values().stream()
            .mapToLong(status -> status == SimplifiedEnhancedPlaidService.ConnectionStatus.ACTIVE ? 1 : 0)
            .sum();
        
        long totalCount = allAccounts.size();
        double healthPercentage = totalCount > 0 ? (double) activeCount / totalCount * 100 : 0;
        
        return Map.of(
            "totalAccounts", totalCount,
            "activeConnections", activeCount,
            "healthPercentage", healthPercentage,
            "status", healthPercentage >= 80 ? "HEALTHY" : healthPercentage >= 50 ? "WARNING" : "CRITICAL",
            "lastChecked", LocalDateTime.now().toString(),
            "connectionDetails", statuses
        );
    }

    // Private helper methods

    private void handleExpiredConnection(Long accountId) {
        try {
            BankAccount account = bankAccountRepository.findById(accountId).orElse(null);
            if (account != null) {
                System.out.printf("Handling expired connection for account: %s%n", 
                                 account.getAccountName());
                
                // Mark account as requiring re-authentication
                account.setIsActive(false);
                // You could add a field to track the reason for deactivation
                bankAccountRepository.save(account);
                
                // TODO: Send notification to user about expired connection
                // notificationService.sendConnectionExpiredNotification(account);
            }
        } catch (Exception e) {
            System.err.printf("Error handling expired connection for account %d: %s%n", 
                             accountId, e.getMessage());
        }
    }

    private void handleErrorConnection(Long accountId) {
        try {
            BankAccount account = bankAccountRepository.findById(accountId).orElse(null);
            if (account != null) {
                System.out.printf("Handling error connection for account: %s%n", 
                                 account.getAccountName());
                
                // Temporarily deactivate account with error status
                account.setIsActive(false);
                bankAccountRepository.save(account);
                
                // TODO: Send notification to user about connection error
                // notificationService.sendConnectionErrorNotification(account, status);
            }
        } catch (Exception e) {
            System.err.printf("Error handling error connection for account %d: %s%n", 
                             accountId, e.getMessage());
        }
    }

    private void handleMaintenanceRequired(Long accountId) {
        try {
            BankAccount account = bankAccountRepository.findById(accountId).orElse(null);
            if (account != null) {
                System.out.printf("Maintenance required for account: %s%n", account.getAccountName());
                
                // TODO: Send maintenance notification to user
                // notificationService.sendMaintenanceRequiredNotification(account);
            }
        } catch (Exception e) {
            System.err.printf("Error handling maintenance required for account %d: %s%n", 
                             accountId, e.getMessage());
        }
    }

    private void cleanupErrorLogs() {
        // Clean up old error logs and reset error counters
        System.out.println("Cleaning up old error logs...");
        // Implementation would depend on how error logs are stored
    }

    private void updateAccountStatistics() {
        // Update statistics for each account (transaction counts, last sync times, etc.)
        System.out.println("Updating account statistics...");
        
        List<BankAccount> accounts = bankAccountRepository.findAll();
        for (BankAccount account : accounts) {
            try {
                // Update last sync time if needed
                if (account.getLastSyncAt() == null) {
                    account.setLastSyncAt(LocalDateTime.now().minusDays(1));
                    bankAccountRepository.save(account);
                }
            } catch (Exception e) {
                System.err.printf("Error updating statistics for account %d: %s%n", 
                                 account.getId(), e.getMessage());
            }
        }
    }

    private void performDatabaseMaintenance() {
        // Perform database maintenance tasks like cleaning up old data
        System.out.println("Performing database maintenance...");
        
        // Example: Clean up transactions older than 2 years
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(2);
        // This would require a custom repository method
        // bankTransactionRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}
