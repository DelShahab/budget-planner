package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SavingsGoal entity
 */
@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    
    /**
     * Find all active savings goals ordered by creation date
     */
    List<SavingsGoal> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Find all savings goals (active and inactive) ordered by creation date
     */
    List<SavingsGoal> findAllByOrderByCreatedAtDesc();
    
    /**
     * Find savings goals by category
     */
    List<SavingsGoal> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * Get total of all current amounts in active goals
     */
    @Query("SELECT SUM(s.currentAmount) FROM SavingsGoal s WHERE s.isActive = true")
    Double getTotalCurrentSavings();
    
    /**
     * Get total of all target amounts in active goals
     */
    @Query("SELECT SUM(s.targetAmount) FROM SavingsGoal s WHERE s.isActive = true")
    Double getTotalTargetSavings();
    
    /**
     * Count active savings goals
     */
    Long countByIsActiveTrue();
    
    /**
     * Find completed goals
     */
    @Query("SELECT s FROM SavingsGoal s WHERE s.currentAmount >= s.targetAmount AND s.isActive = true")
    List<SavingsGoal> findCompletedGoals();
}
