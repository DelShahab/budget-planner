package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a recurring transaction pattern detected from bank transactions.
 * This includes subscriptions, bills, salary payments, and other regular transactions.
 */
@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "description_pattern")
    private String descriptionPattern;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "amount_tolerance")
    private Double amountTolerance = 5.0; // Default 5% tolerance

    @Column(name = "frequency", nullable = false)
    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency frequency;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "budget_category_type")
    private String budgetCategoryType;

    @Column(name = "budget_category")
    private String budgetCategory;

    @Column(name = "first_occurrence", nullable = false)
    private LocalDate firstOccurrence;

    @Column(name = "last_occurrence")
    private LocalDate lastOccurrence;

    @Column(name = "next_expected_date")
    private LocalDate nextExpectedDate;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RecurringStatus status = RecurringStatus.ACTIVE;

    @Column(name = "detection_method")
    @Enumerated(EnumType.STRING)
    private DetectionMethod detectionMethod;

    @Column(name = "user_confirmed")
    private Boolean userConfirmed = false;

    @Column(name = "user_customized")
    private Boolean userCustomized = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes")
    private String notes;

    // Relationships
    @OneToMany(mappedBy = "recurringTransaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BankTransaction> associatedTransactions = new ArrayList<>();

    // Enums
    public enum RecurrenceFrequency {
        WEEKLY(7),
        BI_WEEKLY(14),
        MONTHLY(30),
        BI_MONTHLY(60),
        QUARTERLY(90),
        SEMI_ANNUALLY(180),
        ANNUALLY(365),
        CUSTOM(0);

        private final int defaultDays;

        RecurrenceFrequency(int defaultDays) {
            this.defaultDays = defaultDays;
        }

        public int getDefaultDays() {
            return defaultDays;
        }
    }

    public enum RecurringStatus {
        ACTIVE,           // Currently recurring
        PAUSED,           // Temporarily stopped
        ENDED,            // Permanently ended
        IRREGULAR,        // Pattern became irregular
        PENDING_CONFIRMATION // Detected but not confirmed
    }

    public enum DetectionMethod {
        AMOUNT_AND_MERCHANT,  // Matched by amount and merchant
        MERCHANT_ONLY,        // Matched by merchant name only
        DESCRIPTION_PATTERN,  // Matched by description pattern
        AMOUNT_AND_DATE,      // Matched by amount and date interval
        MACHINE_LEARNING,     // Detected using ML algorithms
        USER_DEFINED          // Manually created by user
    }

    // Constructors
    public RecurringTransaction() {
        this.createdAt = LocalDateTime.now();
    }

    public RecurringTransaction(String merchantName, Double amount, RecurrenceFrequency frequency) {
        this();
        this.merchantName = merchantName;
        this.amount = amount;
        this.frequency = frequency;
        this.intervalDays = frequency.getDefaultDays();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getDescriptionPattern() {
        return descriptionPattern;
    }

    public void setDescriptionPattern(String descriptionPattern) {
        this.descriptionPattern = descriptionPattern;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getAmountTolerance() {
        return amountTolerance;
    }

    public void setAmountTolerance(Double amountTolerance) {
        this.amountTolerance = amountTolerance;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(RecurrenceFrequency frequency) {
        this.frequency = frequency;
        if (frequency != RecurrenceFrequency.CUSTOM) {
            this.intervalDays = frequency.getDefaultDays();
        }
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public String getBudgetCategoryType() {
        return budgetCategoryType;
    }

    public void setBudgetCategoryType(String budgetCategoryType) {
        this.budgetCategoryType = budgetCategoryType;
    }

    public String getBudgetCategory() {
        return budgetCategory;
    }

    public void setBudgetCategory(String budgetCategory) {
        this.budgetCategory = budgetCategory;
    }

    public LocalDate getFirstOccurrence() {
        return firstOccurrence;
    }

    public void setFirstOccurrence(LocalDate firstOccurrence) {
        this.firstOccurrence = firstOccurrence;
    }

    public LocalDate getLastOccurrence() {
        return lastOccurrence;
    }

    public void setLastOccurrence(LocalDate lastOccurrence) {
        this.lastOccurrence = lastOccurrence;
    }

    public LocalDate getNextExpectedDate() {
        return nextExpectedDate;
    }

    public void setNextExpectedDate(LocalDate nextExpectedDate) {
        this.nextExpectedDate = nextExpectedDate;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public RecurringStatus getStatus() {
        return status;
    }

    public void setStatus(RecurringStatus status) {
        this.status = status;
    }

    public DetectionMethod getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(DetectionMethod detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public Boolean getUserConfirmed() {
        return userConfirmed;
    }

    public void setUserConfirmed(Boolean userConfirmed) {
        this.userConfirmed = userConfirmed;
    }

    public Boolean getUserCustomized() {
        return userCustomized;
    }

    public void setUserCustomized(Boolean userCustomized) {
        this.userCustomized = userCustomized;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<BankTransaction> getAssociatedTransactions() {
        return associatedTransactions;
    }

    public void setAssociatedTransactions(List<BankTransaction> associatedTransactions) {
        this.associatedTransactions = associatedTransactions;
    }

    // Utility methods
    
    /**
     * Check if an amount is within the tolerance range
     */
    public boolean isAmountWithinTolerance(Double transactionAmount) {
        if (transactionAmount == null || this.amount == null) {
            return false;
        }
        
        double tolerance = this.amount * (this.amountTolerance / 100.0);
        double lowerBound = this.amount - tolerance;
        double upperBound = this.amount + tolerance;
        
        return transactionAmount >= lowerBound && transactionAmount <= upperBound;
    }

    /**
     * Calculate the next expected occurrence date
     */
    public LocalDate calculateNextExpectedDate() {
        if (lastOccurrence == null) {
            return firstOccurrence != null ? firstOccurrence.plusDays(intervalDays) : null;
        }
        return lastOccurrence.plusDays(intervalDays);
    }

    /**
     * Update the recurring transaction with a new occurrence
     */
    public void recordNewOccurrence(LocalDate occurrenceDate, Double amount) {
        this.lastOccurrence = occurrenceDate;
        this.nextExpectedDate = calculateNextExpectedDate();
        this.occurrenceCount++;
        
        // Update amount if it's different (within reason)
        if (amount != null && isAmountWithinTolerance(amount)) {
            // Use weighted average to adjust amount
            double weight = Math.min(occurrenceCount, 10) / 10.0; // Cap influence at 10 occurrences
            this.amount = (this.amount * (1 - weight * 0.1)) + (amount * weight * 0.1);
        }
        
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this recurring transaction is overdue
     */
    public boolean isOverdue() {
        if (nextExpectedDate == null || status != RecurringStatus.ACTIVE) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        // Allow for some flexibility (3 days grace period)
        return today.isAfter(nextExpectedDate.plusDays(3));
    }

    /**
     * Get a user-friendly description of the recurrence pattern
     */
    public String getRecurrenceDescription() {
        StringBuilder desc = new StringBuilder();
        
        switch (frequency) {
            case WEEKLY:
                desc.append("Weekly");
                break;
            case BI_WEEKLY:
                desc.append("Every 2 weeks");
                break;
            case MONTHLY:
                desc.append("Monthly");
                break;
            case BI_MONTHLY:
                desc.append("Every 2 months");
                break;
            case QUARTERLY:
                desc.append("Quarterly");
                break;
            case SEMI_ANNUALLY:
                desc.append("Every 6 months");
                break;
            case ANNUALLY:
                desc.append("Annually");
                break;
            case CUSTOM:
                desc.append("Every ").append(intervalDays).append(" days");
                break;
        }
        
        if (amount != null) {
            desc.append(" - $").append(String.format("%.2f", Math.abs(amount)));
        }
        
        return desc.toString();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "RecurringTransaction{" +
                "id=" + id +
                ", merchantName='" + merchantName + '\'' +
                ", amount=" + amount +
                ", frequency=" + frequency +
                ", status=" + status +
                ", occurrenceCount=" + occurrenceCount +
                '}';
    }
}
