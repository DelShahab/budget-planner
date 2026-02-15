package com.budgetplanner.budget.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_tags")
public class TransactionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_transaction_id", nullable = false)
    private BankTransaction bankTransaction;

    @Column(nullable = false)
    private String tag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankTransaction getBankTransaction() {
        return bankTransaction;
    }

    public void setBankTransaction(BankTransaction bankTransaction) {
        this.bankTransaction = bankTransaction;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
