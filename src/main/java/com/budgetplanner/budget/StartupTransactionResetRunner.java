package com.budgetplanner.budget;

import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.RecurringTransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Clears transactional data on application startup so that all views
 * (dashboard, recurring, trends, monthly plan, etc.) start from a
 * consistent, empty state after each restart.
 */
@Component
public class StartupTransactionResetRunner implements CommandLineRunner {

    private final BankTransactionRepository bankTransactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public StartupTransactionResetRunner(BankTransactionRepository bankTransactionRepository,
                                         RecurringTransactionRepository recurringTransactionRepository) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    @Override
    public void run(String... args) {
        // Clear recurring patterns first (they reference transactions)
        recurringTransactionRepository.deleteAll();

        // Then clear all bank transactions so every restart starts fresh
        bankTransactionRepository.deleteAll();
    }
}
