package com.budgetplanner.budget.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a user's savings goal
 */
@Entity
@Table(name = "savings_goals")
public class SavingsGoal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String goalName;
    
    @Column(nullable = false)
    private Double targetAmount;
    
    @Column(nullable = false)
    private Double currentAmount;
    
    @Column
    private String category; // e.g., "House", "Vacation", "Emergency", "Education", "Other"
    
    @Column
    private String iconName; // VaadinIcon name
    
    @Column
    private LocalDate targetDate;
    
    @Column
    private LocalDate startDate;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (currentAmount == null) {
            currentAmount = 0.0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getGoalName() {
        return goalName;
    }
    
    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }
    
    public Double getTargetAmount() {
        return targetAmount;
    }
    
    public void setTargetAmount(Double targetAmount) {
        this.targetAmount = targetAmount;
    }
    
    public Double getCurrentAmount() {
        return currentAmount;
    }
    
    public void setCurrentAmount(Double currentAmount) {
        this.currentAmount = currentAmount;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getIconName() {
        return iconName;
    }
    
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    
    public LocalDate getTargetDate() {
        return targetDate;
    }
    
    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    /**
     * Calculate progress percentage
     */
    public int getProgressPercentage() {
        if (targetAmount == null || targetAmount == 0) {
            return 0;
        }
        double progress = (currentAmount / targetAmount) * 100;
        return (int) Math.min(progress, 100);
    }
    
    /**
     * Get remaining amount to reach goal
     */
    public Double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }
    
    /**
     * Check if goal is completed
     */
    public Boolean isCompleted() {
        return currentAmount >= targetAmount;
    }
}
