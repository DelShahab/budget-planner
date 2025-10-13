package com.budgetplanner.budget.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for enhanced Plaid service features including:
 * - Retry mechanisms for failed API calls
 * - Async processing for non-blocking operations
 * - Caching for frequently accessed data
 * - Scheduled tasks for periodic sync
 */
@Configuration
@EnableAsync
@EnableCaching
@EnableScheduling
public class EnhancedPlaidConfig {

    /**
     * Configure thread pool for async Plaid operations
     */
    @Bean(name = "plaidTaskExecutor")
    public Executor plaidTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("PlaidAsync-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Configure cache manager for Plaid data
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Configure caches for different data types
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "institutionNames",    // Institution name lookup cache
            "linkTokens",          // Link token cache (short-lived)
            "accountBalances",     // Account balance cache
            "transactionCategories" // Transaction categorization cache
        ));
        
        return cacheManager;
    }

    /**
     * Configuration properties for enhanced Plaid service
     */
    @Bean
    public PlaidServiceProperties plaidServiceProperties() {
        return new PlaidServiceProperties();
    }

    /**
     * Properties class for configurable Plaid service settings
     */
    public static class PlaidServiceProperties {
        private int maxRetryAttempts = 3;
        private int circuitBreakerThreshold = 5;
        private int circuitBreakerCooldownMinutes = 5;
        private int transactionSyncDays = 30;
        private int maxTransactionsPerRequest = 500;
        private boolean enableWebhooks = true;
        private boolean enableRealTimeSync = true;
        private boolean enableConnectionMonitoring = true;

        // Getters and setters
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

        public int getCircuitBreakerThreshold() { return circuitBreakerThreshold; }
        public void setCircuitBreakerThreshold(int circuitBreakerThreshold) { 
            this.circuitBreakerThreshold = circuitBreakerThreshold; 
        }

        public int getCircuitBreakerCooldownMinutes() { return circuitBreakerCooldownMinutes; }
        public void setCircuitBreakerCooldownMinutes(int circuitBreakerCooldownMinutes) { 
            this.circuitBreakerCooldownMinutes = circuitBreakerCooldownMinutes; 
        }

        public int getTransactionSyncDays() { return transactionSyncDays; }
        public void setTransactionSyncDays(int transactionSyncDays) { 
            this.transactionSyncDays = transactionSyncDays; 
        }

        public int getMaxTransactionsPerRequest() { return maxTransactionsPerRequest; }
        public void setMaxTransactionsPerRequest(int maxTransactionsPerRequest) { 
            this.maxTransactionsPerRequest = maxTransactionsPerRequest; 
        }

        public boolean isEnableWebhooks() { return enableWebhooks; }
        public void setEnableWebhooks(boolean enableWebhooks) { this.enableWebhooks = enableWebhooks; }

        public boolean isEnableRealTimeSync() { return enableRealTimeSync; }
        public void setEnableRealTimeSync(boolean enableRealTimeSync) { 
            this.enableRealTimeSync = enableRealTimeSync; 
        }

        public boolean isEnableConnectionMonitoring() { return enableConnectionMonitoring; }
        public void setEnableConnectionMonitoring(boolean enableConnectionMonitoring) { 
            this.enableConnectionMonitoring = enableConnectionMonitoring; 
        }
    }
}
