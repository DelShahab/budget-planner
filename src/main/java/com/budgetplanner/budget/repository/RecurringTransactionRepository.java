package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing RecurringTransaction entities
 */
@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    /**
     * Find all active recurring transactions
     */
    List<RecurringTransaction> findByIsActiveTrueOrderByMerchantNameAsc();

    /**
     * Find recurring transactions by status
     */
    List<RecurringTransaction> findByStatusOrderByNextExpectedDateAsc(RecurringTransaction.RecurringStatus status);

    /**
     * Find active recurring transactions by status
     */
    List<RecurringTransaction> findByIsActiveTrueAndStatusOrderByNextExpectedDateAsc(RecurringTransaction.RecurringStatus status);

    /**
     * Find recurring transactions by merchant name (case-insensitive)
     */
    List<RecurringTransaction> findByMerchantNameContainingIgnoreCaseAndIsActiveTrue(String merchantName);

    /**
     * Find recurring transactions by category
     */
    List<RecurringTransaction> findByBudgetCategoryTypeAndIsActiveTrueOrderByAmountDesc(String categoryType);

    /**
     * Find recurring transactions by category and subcategory
     */
    List<RecurringTransaction> findByBudgetCategoryTypeAndBudgetCategoryAndIsActiveTrueOrderByAmountDesc(
            String categoryType, String category);

    /**
     * Find overdue recurring transactions
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.status = 'ACTIVE' AND rt.nextExpectedDate < :currentDate")
    List<RecurringTransaction> findOverdueTransactions(@Param("currentDate") LocalDate currentDate);

    /**
     * Find recurring transactions due soon (within specified days)
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.status = 'ACTIVE' AND rt.nextExpectedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY rt.nextExpectedDate ASC")
    List<RecurringTransaction> findTransactionsDueSoon(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    /**
     * Find recurring transactions by frequency
     */
    List<RecurringTransaction> findByFrequencyAndIsActiveTrueOrderByAmountDesc(
            RecurringTransaction.RecurrenceFrequency frequency);

    /**
     * Find recurring transactions with low confidence scores (need review)
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.confidenceScore < :threshold ORDER BY rt.confidenceScore ASC")
    List<RecurringTransaction> findLowConfidenceTransactions(@Param("threshold") Double threshold);

    /**
     * Find unconfirmed recurring transactions
     */
    List<RecurringTransaction> findByUserConfirmedFalseAndIsActiveTrueOrderByConfidenceScoreDesc();

    /**
     * Find recurring transactions by detection method
     */
    List<RecurringTransaction> findByDetectionMethodAndIsActiveTrueOrderByCreatedAtDesc(
            RecurringTransaction.DetectionMethod detectionMethod);

    /**
     * Find potential matches for a new transaction based on merchant and amount
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.status = 'ACTIVE' " +
           "AND LOWER(rt.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%')) " +
           "AND ABS(rt.amount - :amount) <= (rt.amount * rt.amountTolerance / 100.0)")
    List<RecurringTransaction> findPotentialMatches(
            @Param("merchantName") String merchantName, 
            @Param("amount") Double amount);

    /**
     * Find exact match for merchant name and amount (within tolerance)
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND LOWER(rt.merchantName) = LOWER(:merchantName) " +
           "AND ABS(rt.amount - :amount) <= (rt.amount * rt.amountTolerance / 100.0)")
    Optional<RecurringTransaction> findExactMatch(
            @Param("merchantName") String merchantName, 
            @Param("amount") Double amount);

    /**
     * Find recurring transactions created within a date range
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY rt.createdAt DESC")
    List<RecurringTransaction> findByCreatedAtBetween(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    /**
     * Get statistics for recurring transactions by category
     */
    @Query("SELECT rt.budgetCategoryType, COUNT(rt), SUM(rt.amount) FROM RecurringTransaction rt " +
           "WHERE rt.isActive = true AND rt.status = 'ACTIVE' " +
           "GROUP BY rt.budgetCategoryType ORDER BY SUM(rt.amount) DESC")
    List<Object[]> getStatisticsByCategory();

    /**
     * Get monthly recurring transaction totals by category
     */
    @Query("SELECT rt.budgetCategoryType, SUM(CASE " +
           "WHEN rt.frequency = 'WEEKLY' THEN rt.amount * 4.33 " +
           "WHEN rt.frequency = 'BI_WEEKLY' THEN rt.amount * 2.17 " +
           "WHEN rt.frequency = 'MONTHLY' THEN rt.amount " +
           "WHEN rt.frequency = 'BI_MONTHLY' THEN rt.amount * 0.5 " +
           "WHEN rt.frequency = 'QUARTERLY' THEN rt.amount * 0.33 " +
           "WHEN rt.frequency = 'SEMI_ANNUALLY' THEN rt.amount * 0.17 " +
           "WHEN rt.frequency = 'ANNUALLY' THEN rt.amount * 0.083 " +
           "ELSE rt.amount * (30.0 / rt.intervalDays) END) " +
           "FROM RecurringTransaction rt " +
           "WHERE rt.isActive = true AND rt.status = 'ACTIVE' " +
           "GROUP BY rt.budgetCategoryType")
    List<Object[]> getMonthlyTotalsByCategory();

    /**
     * Find similar recurring transactions (for duplicate detection)
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.id != :excludeId " +
           "AND (LOWER(rt.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%')) " +
           "OR LOWER(:merchantName) LIKE LOWER(CONCAT('%', rt.merchantName, '%'))) " +
           "AND ABS(rt.amount - :amount) <= GREATEST(rt.amount * 0.1, 5.0)")
    List<RecurringTransaction> findSimilarTransactions(
            @Param("excludeId") Long excludeId,
            @Param("merchantName") String merchantName, 
            @Param("amount") Double amount);

    /**
     * Count active recurring transactions by status
     */
    @Query("SELECT rt.status, COUNT(rt) FROM RecurringTransaction rt " +
           "WHERE rt.isActive = true GROUP BY rt.status")
    List<Object[]> countByStatus();

    /**
     * Find recurring transactions that haven't occurred in a while (potentially ended)
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.status = 'ACTIVE' " +
           "AND rt.lastOccurrence < :cutoffDate " +
           "ORDER BY rt.lastOccurrence ASC")
    List<RecurringTransaction> findPotentiallyEndedTransactions(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find recurring transactions by amount range
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND rt.amount BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY rt.amount DESC")
    List<RecurringTransaction> findByAmountRange(
            @Param("minAmount") Double minAmount, 
            @Param("maxAmount") Double maxAmount);

    /**
     * Search recurring transactions by multiple criteria
     */
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.isActive = true " +
           "AND (:merchantName IS NULL OR LOWER(rt.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%'))) " +
           "AND (:categoryType IS NULL OR rt.budgetCategoryType = :categoryType) " +
           "AND (:status IS NULL OR rt.status = :status) " +
           "AND (:frequency IS NULL OR rt.frequency = :frequency) " +
           "ORDER BY rt.nextExpectedDate ASC")
    List<RecurringTransaction> searchTransactions(
            @Param("merchantName") String merchantName,
            @Param("categoryType") String categoryType,
            @Param("status") RecurringTransaction.RecurringStatus status,
            @Param("frequency") RecurringTransaction.RecurrenceFrequency frequency);
}
