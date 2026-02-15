package com.budgetplanner.budget.config;

import com.budgetplanner.budget.service.SavingsGoalService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializer for savings goal sample data
 */
@Configuration
public class SavingsGoalDataInitializer {
    
    @Bean
    CommandLineRunner initSavingsGoals(SavingsGoalService savingsGoalService) {
        return args -> {
            savingsGoalService.initializeSampleGoals();
        };
    }
}
