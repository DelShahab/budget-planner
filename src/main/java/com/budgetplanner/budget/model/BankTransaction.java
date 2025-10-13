package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String plaidTransactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String merchantName;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private LocalDate transactionDate;
    
    @Column(nullable = false)
    private LocalDate authorizedDate;
    
    @Column(nullable = false)
    private String transactionType; // debit, credit
    
    @Column
    private String plaidCategory; // Primary category from Plaid
    
    @Column
    private String plaidSubcategory; // Detailed category from Plaid
    
    @Column
    private String budgetCategory; // Our mapped budget category
    
    @Column
    private String budgetCategoryType; // INCOME, EXPENSES, BILLS, SAVINGS
    
    @Column(nullable = false)
    private Boolean isProcessed = false;
    
    @Column(nullable = false)
    private Boolean isManuallyReviewed = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_transaction_id")
    private RecurringTransaction recurringTransaction;
    
    public BankTransaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public BankTransaction(String plaidTransactionId, BankAccount bankAccount, Double amount,
                          String merchantName, String description, LocalDate transactionDate,
                          LocalDate authorizedDate, String transactionType) {
        this();
        this.plaidTransactionId = plaidTransactionId;
        this.bankAccount = bankAccount;
        this.amount = amount;
        this.merchantName = merchantName;
        this.description = description;
        this.transactionDate = transactionDate;
        this.authorizedDate = authorizedDate;
        this.transactionType = transactionType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPlaidTransactionId() {
        return plaidTransactionId;
    }
    
    public void setPlaidTransactionId(String plaidTransactionId) {
        this.plaidTransactionId = plaidTransactionId;
    }
    
    public BankAccount getBankAccount() {
        return bankAccount;
    }
    
    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getMerchantName() {
        return merchantName;
    }
    
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public LocalDate getAuthorizedDate() {
        return authorizedDate;
    }
    
    public void setAuthorizedDate(LocalDate authorizedDate) {
        this.authorizedDate = authorizedDate;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public String getPlaidCategory() {
        return plaidCategory;
    }
    
    public void setPlaidCategory(String plaidCategory) {
        this.plaidCategory = plaidCategory;
    }
    
    public String getPlaidSubcategory() {
        return plaidSubcategory;
    }
    
    public void setPlaidSubcategory(String plaidSubcategory) {
        this.plaidSubcategory = plaidSubcategory;
    }
    
    public String getBudgetCategory() {
        return budgetCategory;
    }
    
    public void setBudgetCategory(String budgetCategory) {
        this.budgetCategory = budgetCategory;
    }
    
    public String getBudgetCategoryType() {
        return budgetCategoryType;
    }
    
    public void setBudgetCategoryType(String budgetCategoryType) {
        this.budgetCategoryType = budgetCategoryType;
    }
    
    public Boolean getIsProcessed() {
        return isProcessed;
    }
    
    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }
    
    public Boolean getIsManuallyReviewed() {
        return isManuallyReviewed;
    }
    
    public void setIsManuallyReviewed(Boolean isManuallyReviewed) {
        this.isManuallyReviewed = isManuallyReviewed;
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
    
    public RecurringTransaction getRecurringTransaction() {
        return recurringTransaction;
    }
    
    public void setRecurringTransaction(RecurringTransaction recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "BankTransaction{" +
                "id=" + id +
                ", plaidTransactionId='" + plaidTransactionId + '\'' +
                ", amount=" + amount +
                ", merchantName='" + merchantName + '\'' +
                ", transactionDate=" + transactionDate +
                ", transactionType='" + transactionType + '\'' +
                ", budgetCategory='" + budgetCategory + '\'' +
                ", isProcessed=" + isProcessed +
                '}';
    }
}
