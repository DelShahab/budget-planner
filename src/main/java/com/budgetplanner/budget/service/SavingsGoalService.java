package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.SavingsGoal;
import com.budgetplanner.budget.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing savings goals
 */
@Service
@Transactional
public class SavingsGoalService {
    
    private final SavingsGoalRepository savingsGoalRepository;
    
    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }
    
    /**
     * Get all active savings goals
     */
    public List<SavingsGoal> getAllActiveGoals() {
        return savingsGoalRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    /**
     * Get all savings goals (including inactive)
     */
    public List<SavingsGoal> getAllGoals() {
        return savingsGoalRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Get savings goal by ID
     */
    public Optional<SavingsGoal> getGoalById(Long id) {
        return savingsGoalRepository.findById(id);
    }
    
    /**
     * Create a new savings goal
     */
    public SavingsGoal createGoal(SavingsGoal goal) {
        return savingsGoalRepository.save(goal);
    }
    
    /**
     * Update an existing savings goal
     */
    public SavingsGoal updateGoal(SavingsGoal goal) {
        return savingsGoalRepository.save(goal);
    }
    
    /**
     * Add amount to a savings goal
     */
    public SavingsGoal addToGoal(Long goalId, Double amount) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isPresent()) {
            SavingsGoal goal = goalOpt.get();
            goal.setCurrentAmount(goal.getCurrentAmount() + amount);
            return savingsGoalRepository.save(goal);
        }
        return null;
    }
    
    /**
     * Withdraw amount from a savings goal
     */
    public SavingsGoal withdrawFromGoal(Long goalId, Double amount) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isPresent()) {
            SavingsGoal goal = goalOpt.get();
            double newAmount = Math.max(0, goal.getCurrentAmount() - amount);
            goal.setCurrentAmount(newAmount);
            return savingsGoalRepository.save(goal);
        }
        return null;
    }
    
    /**
     * Delete/deactivate a savings goal
     */
    public void deleteGoal(Long goalId) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isPresent()) {
            SavingsGoal goal = goalOpt.get();
            goal.setIsActive(false);
            savingsGoalRepository.save(goal);
        }
    }
    
    /**
     * Get total current savings across all active goals
     */
    public Double getTotalCurrentSavings() {
        Double total = savingsGoalRepository.getTotalCurrentSavings();
        return total != null ? total : 0.0;
    }
    
    /**
     * Get total target savings across all active goals
     */
    public Double getTotalTargetSavings() {
        Double total = savingsGoalRepository.getTotalTargetSavings();
        return total != null ? total : 0.0;
    }
    
    /**
     * Get overall progress percentage
     */
    public int getOverallProgressPercentage() {
        Double current = getTotalCurrentSavings();
        Double target = getTotalTargetSavings();
        if (target == null || target == 0) {
            return 0;
        }
        return (int) Math.min((current / target) * 100, 100);
    }
    
    /**
     * Initialize sample savings goals if none exist
     */
    public void initializeSampleGoals() {
        if (savingsGoalRepository.count() == 0) {
            // New House goal
            SavingsGoal house = new SavingsGoal();
            house.setGoalName("New House");
            house.setCategory("Housing");
            house.setIconName("HOME");
            house.setTargetAmount(300000000.0); // 300M IDR
            house.setCurrentAmount(250000000.0); // 250M IDR (83%)
            house.setTargetDate(LocalDate.now().plusYears(2));
            house.setDescription("Down payment for dream house");
            savingsGoalRepository.save(house);
            
            // PC Gaming goal
            SavingsGoal gaming = new SavingsGoal();
            gaming.setGoalName("PC Gaming");
            gaming.setCategory("Electronics");
            gaming.setIconName("DESKTOP");
            gaming.setTargetAmount(20000000.0); // 20M IDR
            gaming.setCurrentAmount(10000000.0); // 10M IDR (50%)
            gaming.setTargetDate(LocalDate.now().plusMonths(6));
            gaming.setDescription("New gaming setup");
            savingsGoalRepository.save(gaming);
            
            // Summer Trip goal
            SavingsGoal trip = new SavingsGoal();
            trip.setGoalName("Summer Trip");
            trip.setCategory("Travel");
            trip.setIconName("AIRPLANE");
            trip.setTargetAmount(1000000.0); // 1M IDR
            trip.setCurrentAmount(140000.0); // 140K IDR (14%)
            trip.setTargetDate(LocalDate.now().plusMonths(3));
            trip.setDescription("Vacation to Bali");
            savingsGoalRepository.save(trip);
            
            // Emergency Fund
            SavingsGoal emergency = new SavingsGoal();
            emergency.setGoalName("Emergency Fund");
            emergency.setCategory("Emergency");
            emergency.setIconName("SHIELD");
            emergency.setTargetAmount(50000000.0); // 50M IDR
            emergency.setCurrentAmount(35000000.0); // 35M IDR (70%)
            emergency.setDescription("6 months expenses");
            savingsGoalRepository.save(emergency);
        }
    }
}
