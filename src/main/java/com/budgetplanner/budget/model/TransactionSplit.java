package com.budgetplanner.budget.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a split line for a parent BankTransaction.
 */
@Entity
@Table(name = "transaction_splits")
public class TransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private BankTransaction parentTransaction;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "budget_category")
    private String budgetCategory;

    @Column(name = "budget_category_type")
    private String budgetCategoryType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TransactionSplit() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankTransaction getParentTransaction() {
        return parentTransaction;
    }

    public void setParentTransaction(BankTransaction parentTransaction) {
        this.parentTransaction = parentTransaction;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
