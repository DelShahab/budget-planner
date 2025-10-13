package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.RecurringTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for detecting, managing, and analyzing recurring transactions
 */
@Service
@Transactional
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;

    // Configuration constants
    private static final int MIN_OCCURRENCES_FOR_DETECTION = 2;
    private static final int MAX_DAYS_VARIANCE = 7; // Allow 7 days variance in recurring pattern
    private static final double MIN_CONFIDENCE_SCORE = 0.6;
    private static final double AMOUNT_TOLERANCE_PERCENT = 10.0; // 10% tolerance for amount matching

    @Autowired
    public RecurringTransactionService(RecurringTransactionRepository recurringTransactionRepository,
                                     BankTransactionRepository bankTransactionRepository) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    /**
     * Analyze all transactions to detect recurring patterns
     */
    @Async
    public CompletableFuture<Integer> analyzeAllTransactionsForRecurringPatterns() {
        System.out.println("=== STARTING RECURRING TRANSACTION ANALYSIS ===");
        
        try {
            // Get all transactions from the last 12 months
            LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
            List<BankTransaction> transactions = bankTransactionRepository
                .findByCreatedAtAfterOrderByTransactionDateAsc(twelveMonthsAgo);
            
            System.out.printf("Analyzing %d transactions for recurring patterns...%n", transactions.size());
            
            int detectedPatterns = 0;
            
            // Group transactions by merchant name for initial analysis
            Map<String, List<BankTransaction>> transactionsByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(
                    transaction -> normalizedMerchantName(transaction.getMerchantName())
                ));
            
            for (Map.Entry<String, List<BankTransaction>> entry : transactionsByMerchant.entrySet()) {
                String merchantName = entry.getKey();
                List<BankTransaction> merchantTransactions = entry.getValue();
                
                if (merchantTransactions.size() >= MIN_OCCURRENCES_FOR_DETECTION) {
                    List<RecurringTransaction> patterns = detectRecurringPatterns(merchantName, merchantTransactions);
                    detectedPatterns += patterns.size();
                    
                    for (RecurringTransaction pattern : patterns) {
                        saveOrUpdateRecurringTransaction(pattern);
                    }
                }
            }
            
            System.out.printf("Recurring transaction analysis complete: %d patterns detected%n", detectedPatterns);
            return CompletableFuture.completedFuture(detectedPatterns);
            
        } catch (Exception e) {
            System.err.println("Error during recurring transaction analysis: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Detect recurring patterns for a specific merchant
     */
    public List<RecurringTransaction> detectRecurringPatterns(String merchantName, List<BankTransaction> transactions) {
        List<RecurringTransaction> patterns = new ArrayList<>();
        
        if (transactions.size() < MIN_OCCURRENCES_FOR_DETECTION) {
            return patterns;
        }
        
        // Sort transactions by date
        transactions.sort(Comparator.comparing(BankTransaction::getTransactionDate));
        
        // Group by similar amounts (within tolerance)
        Map<Double, List<BankTransaction>> amountGroups = groupTransactionsByAmount(transactions);
        
        for (Map.Entry<Double, List<BankTransaction>> amountGroup : amountGroups.entrySet()) {
            Double amount = amountGroup.getKey();
            List<BankTransaction> amountTransactions = amountGroup.getValue();
            
            if (amountTransactions.size() >= MIN_OCCURRENCES_FOR_DETECTION) {
                RecurringTransaction pattern = analyzeTransactionPattern(merchantName, amount, amountTransactions);
                if (pattern != null && pattern.getConfidenceScore() >= MIN_CONFIDENCE_SCORE) {
                    patterns.add(pattern);
                }
            }
        }
        
        return patterns;
    }

    /**
     * Analyze a group of transactions to determine if they form a recurring pattern
     */
    private RecurringTransaction analyzeTransactionPattern(String merchantName, Double amount, 
                                                         List<BankTransaction> transactions) {
        if (transactions.size() < MIN_OCCURRENCES_FOR_DETECTION) {
            return null;
        }
        
        // Calculate intervals between transactions
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < transactions.size(); i++) {
            LocalDate prevDate = transactions.get(i - 1).getTransactionDate();
            LocalDate currentDate = transactions.get(i).getTransactionDate();
            long daysBetween = ChronoUnit.DAYS.between(prevDate, currentDate);
            intervals.add(daysBetween);
        }
        
        // Analyze interval consistency
        RecurrenceAnalysis analysis = analyzeIntervals(intervals);
        if (analysis.confidence < MIN_CONFIDENCE_SCORE) {
            return null;
        }
        
        // Create recurring transaction pattern
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setMerchantName(merchantName);
        recurringTransaction.setAmount(amount);
        recurringTransaction.setAmountTolerance(AMOUNT_TOLERANCE_PERCENT);
        recurringTransaction.setFrequency(analysis.frequency);
        recurringTransaction.setIntervalDays(analysis.intervalDays);
        recurringTransaction.setConfidenceScore(analysis.confidence);
        recurringTransaction.setDetectionMethod(RecurringTransaction.DetectionMethod.AMOUNT_AND_MERCHANT);
        
        // Set dates and counts
        recurringTransaction.setFirstOccurrence(transactions.get(0).getTransactionDate());
        recurringTransaction.setLastOccurrence(transactions.get(transactions.size() - 1).getTransactionDate());
        recurringTransaction.setNextExpectedDate(recurringTransaction.calculateNextExpectedDate());
        recurringTransaction.setOccurrenceCount(transactions.size());
        
        // Set category from the most recent transaction
        BankTransaction latestTransaction = transactions.get(transactions.size() - 1);
        recurringTransaction.setBudgetCategoryType(latestTransaction.getBudgetCategoryType());
        recurringTransaction.setBudgetCategory(latestTransaction.getBudgetCategory());
        
        // Set status based on recency
        LocalDate lastOccurrence = recurringTransaction.getLastOccurrence();
        LocalDate expectedNext = recurringTransaction.getNextExpectedDate();
        LocalDate today = LocalDate.now();
        
        if (expectedNext != null && today.isAfter(expectedNext.plusDays(analysis.intervalDays))) {
            recurringTransaction.setStatus(RecurringTransaction.RecurringStatus.ENDED);
        } else {
            recurringTransaction.setStatus(RecurringTransaction.RecurringStatus.PENDING_CONFIRMATION);
        }
        
        return recurringTransaction;
    }

    /**
     * Analyze intervals to determine recurrence pattern
     */
    private RecurrenceAnalysis analyzeIntervals(List<Long> intervals) {
        if (intervals.isEmpty()) {
            return new RecurrenceAnalysis(0.0, RecurringTransaction.RecurrenceFrequency.CUSTOM, 0);
        }
        
        // Calculate average interval
        double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // Calculate variance
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - avgInterval, 2))
            .average().orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        
        // Determine confidence based on consistency
        double confidence = calculateConfidence(intervals, avgInterval, standardDeviation);
        
        // Determine frequency
        RecurringTransaction.RecurrenceFrequency frequency = determineFrequency(avgInterval);
        int intervalDays = (int) Math.round(avgInterval);
        
        return new RecurrenceAnalysis(confidence, frequency, intervalDays);
    }

    /**
     * Calculate confidence score based on interval consistency
     */
    private double calculateConfidence(List<Long> intervals, double avgInterval, double standardDeviation) {
        if (intervals.size() < 2) {
            return 0.5;
        }
        
        // Base confidence on standard deviation relative to average
        double coefficientOfVariation = standardDeviation / avgInterval;
        
        // Lower coefficient of variation = higher confidence
        double confidence = Math.max(0.0, 1.0 - coefficientOfVariation);
        
        // Bonus for more occurrences
        double occurrenceBonus = Math.min(0.2, intervals.size() * 0.05);
        confidence += occurrenceBonus;
        
        // Penalty for very irregular intervals
        long maxVariance = intervals.stream()
            .mapToLong(interval -> Math.abs(interval - (long) avgInterval))
            .max().orElse(0);
        
        if (maxVariance > MAX_DAYS_VARIANCE) {
            confidence *= 0.7; // Reduce confidence for high variance
        }
        
        return Math.min(1.0, Math.max(0.0, confidence));
    }

    /**
     * Determine frequency based on average interval
     */
    private RecurringTransaction.RecurrenceFrequency determineFrequency(double avgInterval) {
        if (avgInterval >= 6 && avgInterval <= 8) {
            return RecurringTransaction.RecurrenceFrequency.WEEKLY;
        } else if (avgInterval >= 13 && avgInterval <= 15) {
            return RecurringTransaction.RecurrenceFrequency.BI_WEEKLY;
        } else if (avgInterval >= 28 && avgInterval <= 32) {
            return RecurringTransaction.RecurrenceFrequency.MONTHLY;
        } else if (avgInterval >= 58 && avgInterval <= 62) {
            return RecurringTransaction.RecurrenceFrequency.BI_MONTHLY;
        } else if (avgInterval >= 88 && avgInterval <= 92) {
            return RecurringTransaction.RecurrenceFrequency.QUARTERLY;
        } else if (avgInterval >= 178 && avgInterval <= 182) {
            return RecurringTransaction.RecurrenceFrequency.SEMI_ANNUALLY;
        } else if (avgInterval >= 360 && avgInterval <= 370) {
            return RecurringTransaction.RecurrenceFrequency.ANNUALLY;
        } else {
            return RecurringTransaction.RecurrenceFrequency.CUSTOM;
        }
    }

    /**
     * Group transactions by similar amounts
     */
    private Map<Double, List<BankTransaction>> groupTransactionsByAmount(List<BankTransaction> transactions) {
        Map<Double, List<BankTransaction>> groups = new HashMap<>();
        
        for (BankTransaction transaction : transactions) {
            Double amount = transaction.getAmount();
            boolean foundGroup = false;
            
            // Look for existing group with similar amount
            for (Double groupAmount : groups.keySet()) {
                if (isAmountSimilar(amount, groupAmount, AMOUNT_TOLERANCE_PERCENT)) {
                    groups.get(groupAmount).add(transaction);
                    foundGroup = true;
                    break;
                }
            }
            
            // Create new group if no similar amount found
            if (!foundGroup) {
                groups.put(amount, new ArrayList<>(Arrays.asList(transaction)));
            }
        }
        
        return groups;
    }

    /**
     * Check if two amounts are similar within tolerance
     */
    private boolean isAmountSimilar(Double amount1, Double amount2, double tolerancePercent) {
        if (amount1 == null || amount2 == null) {
            return false;
        }
        
        double tolerance = Math.abs(amount1) * (tolerancePercent / 100.0);
        return Math.abs(amount1 - amount2) <= tolerance;
    }

    /**
     * Normalize merchant name for grouping
     */
    private String normalizedMerchantName(String merchantName) {
        if (merchantName == null) {
            return "Unknown";
        }
        
        return merchantName.trim()
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-zA-Z0-9\\s]", "")
            .toLowerCase();
    }

    /**
     * Save or update a recurring transaction pattern
     */
    public RecurringTransaction saveOrUpdateRecurringTransaction(RecurringTransaction recurringTransaction) {
        // Check for existing similar patterns
        Optional<RecurringTransaction> existing = recurringTransactionRepository
            .findExactMatch(recurringTransaction.getMerchantName(), recurringTransaction.getAmount());
        
        if (existing.isPresent()) {
            // Update existing pattern
            RecurringTransaction existingPattern = existing.get();
            updateExistingPattern(existingPattern, recurringTransaction);
            return recurringTransactionRepository.save(existingPattern);
        } else {
            // Save new pattern
            return recurringTransactionRepository.save(recurringTransaction);
        }
    }

    /**
     * Update an existing recurring transaction pattern with new information
     */
    private void updateExistingPattern(RecurringTransaction existing, RecurringTransaction newPattern) {
        // Update confidence score (weighted average)
        if (newPattern.getConfidenceScore() != null) {
            double existingWeight = 0.7;
            double newWeight = 0.3;
            double updatedConfidence = (existing.getConfidenceScore() * existingWeight) + 
                                     (newPattern.getConfidenceScore() * newWeight);
            existing.setConfidenceScore(updatedConfidence);
        }
        
        // Update occurrence count
        if (newPattern.getOccurrenceCount() > existing.getOccurrenceCount()) {
            existing.setOccurrenceCount(newPattern.getOccurrenceCount());
        }
        
        // Update last occurrence if newer
        if (newPattern.getLastOccurrence() != null && 
            (existing.getLastOccurrence() == null || 
             newPattern.getLastOccurrence().isAfter(existing.getLastOccurrence()))) {
            existing.setLastOccurrence(newPattern.getLastOccurrence());
            existing.setNextExpectedDate(existing.calculateNextExpectedDate());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Check if a new transaction matches any existing recurring patterns
     */
    public List<RecurringTransaction> findMatchingRecurringPatterns(BankTransaction transaction) {
        String merchantName = normalizedMerchantName(transaction.getMerchantName());
        Double amount = transaction.getAmount();
        
        return recurringTransactionRepository.findPotentialMatches(merchantName, amount);
    }

    /**
     * Process a new transaction and update matching recurring patterns
     */
    public void processNewTransaction(BankTransaction transaction) {
        List<RecurringTransaction> matches = findMatchingRecurringPatterns(transaction);
        
        for (RecurringTransaction recurringTransaction : matches) {
            // Update the recurring transaction with this new occurrence
            recurringTransaction.recordNewOccurrence(
                transaction.getTransactionDate(), 
                transaction.getAmount()
            );
            
            // If this was a pending confirmation, mark as active
            if (recurringTransaction.getStatus() == RecurringTransaction.RecurringStatus.PENDING_CONFIRMATION) {
                recurringTransaction.setStatus(RecurringTransaction.RecurringStatus.ACTIVE);
            }
            
            recurringTransactionRepository.save(recurringTransaction);
        }
    }

    /**
     * Get all active recurring transactions
     */
    public List<RecurringTransaction> getAllActiveRecurringTransactions() {
        return recurringTransactionRepository.findByIsActiveTrueOrderByMerchantNameAsc();
    }

    /**
     * Get recurring transactions due soon
     */
    public List<RecurringTransaction> getTransactionsDueSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        return recurringTransactionRepository.findTransactionsDueSoon(today, endDate);
    }

    /**
     * Get overdue recurring transactions
     */
    public List<RecurringTransaction> getOverdueTransactions() {
        return recurringTransactionRepository.findOverdueTransactions(LocalDate.now());
    }

    /**
     * Get recurring transactions by category
     */
    public List<RecurringTransaction> getRecurringTransactionsByCategory(String categoryType) {
        return recurringTransactionRepository.findByBudgetCategoryTypeAndIsActiveTrueOrderByAmountDesc(categoryType);
    }

    /**
     * Get monthly totals by category
     */
    public Map<String, Double> getMonthlyTotalsByCategory() {
        List<Object[]> results = recurringTransactionRepository.getMonthlyTotalsByCategory();
        Map<String, Double> totals = new HashMap<>();
        
        for (Object[] result : results) {
            String category = (String) result[0];
            Double total = (Double) result[1];
            totals.put(category, total);
        }
        
        return totals;
    }

    /**
     * Update an existing recurring transaction
     */
    public RecurringTransaction updateRecurringTransaction(Long id, RecurringTransaction updatedTransaction) {
        RecurringTransaction existingTransaction = recurringTransactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recurring transaction not found with id: " + id));
        
        // Update fields
        existingTransaction.setMerchantName(updatedTransaction.getMerchantName());
        existingTransaction.setDescriptionPattern(updatedTransaction.getDescriptionPattern());
        existingTransaction.setAmount(updatedTransaction.getAmount());
        existingTransaction.setAmountTolerance(updatedTransaction.getAmountTolerance());
        existingTransaction.setFrequency(updatedTransaction.getFrequency());
        existingTransaction.setIntervalDays(updatedTransaction.getIntervalDays());
        existingTransaction.setBudgetCategoryType(updatedTransaction.getBudgetCategoryType());
        existingTransaction.setBudgetCategory(updatedTransaction.getBudgetCategory());
        existingTransaction.setNextExpectedDate(updatedTransaction.getNextExpectedDate());
        existingTransaction.setStatus(updatedTransaction.getStatus());
        existingTransaction.setUserConfirmed(updatedTransaction.getUserConfirmed());
        existingTransaction.setUserCustomized(updatedTransaction.getUserCustomized());
        existingTransaction.setIsActive(updatedTransaction.getIsActive());
        existingTransaction.setNotes(updatedTransaction.getNotes());
        existingTransaction.setLastOccurrence(updatedTransaction.getLastOccurrence());
        existingTransaction.setOccurrenceCount(updatedTransaction.getOccurrenceCount());
        existingTransaction.setUpdatedAt(LocalDateTime.now());
        
        return recurringTransactionRepository.save(existingTransaction);
    }

    /**
     * Scheduled task to update recurring transaction statuses
     */
    @Scheduled(cron = "0 0 6 * * ?") // Daily at 6 AM
    public void updateRecurringTransactionStatuses() {
        System.out.println("=== UPDATING RECURRING TRANSACTION STATUSES ===");
        
        try {
            List<RecurringTransaction> activeTransactions = recurringTransactionRepository
                .findByIsActiveTrueAndStatusOrderByNextExpectedDateAsc(RecurringTransaction.RecurringStatus.ACTIVE);
            
            int updatedCount = 0;
            LocalDate today = LocalDate.now();
            
            for (RecurringTransaction transaction : activeTransactions) {
                boolean updated = false;
                
                // Check if transaction is overdue
                if (transaction.isOverdue()) {
                    // Mark as irregular if significantly overdue
                    LocalDate expectedDate = transaction.getNextExpectedDate();
                    long daysOverdue = ChronoUnit.DAYS.between(expectedDate, today);
                    
                    if (daysOverdue > transaction.getIntervalDays() * 2) {
                        transaction.setStatus(RecurringTransaction.RecurringStatus.IRREGULAR);
                        updated = true;
                    }
                }
                
                // Check if transaction hasn't occurred in a very long time (potentially ended)
                if (transaction.getLastOccurrence() != null) {
                    long daysSinceLastOccurrence = ChronoUnit.DAYS.between(
                        transaction.getLastOccurrence(), today);
                    
                    if (daysSinceLastOccurrence > transaction.getIntervalDays() * 3) {
                        transaction.setStatus(RecurringTransaction.RecurringStatus.ENDED);
                        updated = true;
                    }
                }
                
                if (updated) {
                    recurringTransactionRepository.save(transaction);
                    updatedCount++;
                }
            }
            
            System.out.printf("Updated status for %d recurring transactions%n", updatedCount);
            
        } catch (Exception e) {
            System.err.println("Error updating recurring transaction statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper classes
    private static class RecurrenceAnalysis {
        final double confidence;
        final RecurringTransaction.RecurrenceFrequency frequency;
        final int intervalDays;
        
        RecurrenceAnalysis(double confidence, RecurringTransaction.RecurrenceFrequency frequency, int intervalDays) {
            this.confidence = confidence;
            this.frequency = frequency;
            this.intervalDays = intervalDays;
        }
    }
}
