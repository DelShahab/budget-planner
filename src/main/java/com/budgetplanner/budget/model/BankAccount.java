package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String plaidAccountId;
    
    @Column(nullable = false)
    private String plaidItemId;
    
    @Column(nullable = false)
    private String accountName;
    
    @Column(nullable = false)
    private String accountType; // checking, savings, credit, etc.
    
    @Column(nullable = false)
    private String institutionName;
    
    @Column(nullable = false)
    private String mask; // Last 4 digits of account number
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastSyncAt;
    
    @Column
    private String accessToken; // Encrypted Plaid access token
    
    public BankAccount() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BankAccount(String plaidAccountId, String plaidItemId, String accountName, 
                      String accountType, String institutionName, String mask, String accessToken) {
        this();
        this.plaidAccountId = plaidAccountId;
        this.plaidItemId = plaidItemId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.institutionName = institutionName;
        this.mask = mask;
        this.accessToken = accessToken;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPlaidAccountId() {
        return plaidAccountId;
    }
    
    public void setPlaidAccountId(String plaidAccountId) {
        this.plaidAccountId = plaidAccountId;
    }
    
    public String getPlaidItemId() {
        return plaidItemId;
    }
    
    public void setPlaidItemId(String plaidItemId) {
        this.plaidItemId = plaidItemId;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public String getInstitutionName() {
        return institutionName;
    }
    
    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }
    
    public String getMask() {
        return mask;
    }
    
    public void setMask(String mask) {
        this.mask = mask;
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
    
    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }
    
    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", plaidAccountId='" + plaidAccountId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", institutionName='" + institutionName + '\'' +
                ", mask='" + mask + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
