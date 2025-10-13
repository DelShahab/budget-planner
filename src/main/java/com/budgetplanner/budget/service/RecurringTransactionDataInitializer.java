package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.repository.RecurringTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Initializes sample recurring transaction data for demonstration
 */
@Component
public class RecurringTransactionDataInitializer implements CommandLineRunner {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only add sample data if no recurring transactions exist
        if (recurringTransactionRepository.count() == 0) {
            System.out.println("Initializing sample recurring transaction data...");
            createSampleRecurringTransactions();
            System.out.println("Sample recurring transaction data initialized successfully!");
        }
    }

    private void createSampleRecurringTransactions() {
        List<RecurringTransaction> sampleTransactions = Arrays.asList(
            // Monthly Subscriptions
            createRecurringTransaction(
                "Netflix Inc.",
                "Netflix Subscription",
                15.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Entertainment",
                "Streaming",
                LocalDate.now().withDayOfMonth(15),
                95.0
            ),
            
            createRecurringTransaction(
                "Spotify USA Inc.",
                "Spotify Premium",
                9.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Entertainment",
                "Music Streaming",
                LocalDate.now().withDayOfMonth(8),
                92.0
            ),
            
            createRecurringTransaction(
                "Amazon Prime",
                "Amazon Prime Membership",
                14.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Shopping",
                "Membership",
                LocalDate.now().withDayOfMonth(22),
                88.0
            ),
            
            // Bills
            createRecurringTransaction(
                "Pacific Gas & Electric",
                "Electricity Bill",
                125.50,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Bills",
                "Utilities",
                LocalDate.now().withDayOfMonth(5),
                96.0
            ),
            
            createRecurringTransaction(
                "Comcast Corporation",
                "Internet & Cable",
                89.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Bills",
                "Utilities",
                LocalDate.now().withDayOfMonth(12),
                94.0
            ),
            
            createRecurringTransaction(
                "T-Mobile USA",
                "Mobile Phone Bill",
                65.00,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Bills",
                "Telecommunications",
                LocalDate.now().withDayOfMonth(18),
                97.0
            ),
            
            // Rent/Housing
            createRecurringTransaction(
                "Property Management Co.",
                "Monthly Rent",
                2200.00,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Housing",
                "Rent",
                LocalDate.now().withDayOfMonth(1),
                99.0
            ),
            
            // Income
            createRecurringTransaction(
                "Tech Company Inc.",
                "Salary Deposit",
                4500.00,
                RecurringTransaction.RecurrenceFrequency.BI_WEEKLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Income",
                "Salary",
                getNextBiWeeklyDate(),
                98.0
            ),
            
            // Fitness & Health
            createRecurringTransaction(
                "24 Hour Fitness",
                "Gym Membership",
                39.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Health & Fitness",
                "Gym",
                LocalDate.now().withDayOfMonth(10),
                91.0
            ),
            
            // Insurance
            createRecurringTransaction(
                "State Farm Insurance",
                "Auto Insurance",
                145.00,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Insurance",
                "Auto",
                LocalDate.now().withDayOfMonth(25),
                96.0
            ),
            
            // Due Soon Examples
            createRecurringTransaction(
                "Adobe Systems",
                "Creative Cloud Subscription",
                52.99,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Software",
                "Design Tools",
                LocalDate.now().plusDays(2), // Due in 2 days
                89.0
            ),
            
            // Overdue Example
            createRecurringTransaction(
                "City Water Department",
                "Water & Sewer Bill",
                78.25,
                RecurringTransaction.RecurrenceFrequency.MONTHLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Bills",
                "Utilities",
                LocalDate.now().minusDays(3), // 3 days overdue
                93.0
            ),
            
            // Weekly Examples
            createRecurringTransaction(
                "Whole Foods Market",
                "Weekly Groceries",
                120.00,
                RecurringTransaction.RecurrenceFrequency.WEEKLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Food & Dining",
                "Groceries",
                getNextWeeklyDate(),
                85.0
            ),
            
            // Quarterly Example
            createRecurringTransaction(
                "IRS",
                "Quarterly Tax Payment",
                1250.00,
                RecurringTransaction.RecurrenceFrequency.QUARTERLY,
                RecurringTransaction.RecurringStatus.ACTIVE,
                "Taxes",
                "Federal",
                getNextQuarterlyDate(),
                87.0
            )
        );

        recurringTransactionRepository.saveAll(sampleTransactions);
    }

    private RecurringTransaction createRecurringTransaction(
            String merchantName,
            String description,
            Double amount,
            RecurringTransaction.RecurrenceFrequency frequency,
            RecurringTransaction.RecurringStatus status,
            String categoryType,
            String category,
            LocalDate nextExpectedDate,
            Double confidenceScore) {
        
        RecurringTransaction transaction = new RecurringTransaction();
        transaction.setMerchantName(merchantName);
        transaction.setDescriptionPattern(description);
        transaction.setAmount(amount);
        transaction.setFrequency(frequency);
        transaction.setStatus(status);
        transaction.setBudgetCategoryType(categoryType);
        transaction.setBudgetCategory(category);
        transaction.setNextExpectedDate(nextExpectedDate);
        transaction.setConfidenceScore(confidenceScore);
        transaction.setDetectionMethod(RecurringTransaction.DetectionMethod.AMOUNT_AND_MERCHANT);
        transaction.setOccurrenceCount(12); // Simulate 12 months of history
        transaction.setCreatedAt(LocalDateTime.now().minusMonths(12));
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setLastOccurrence(calculateLastOccurrence(nextExpectedDate, frequency));
        transaction.setFirstOccurrence(calculateLastOccurrence(nextExpectedDate, frequency).minusMonths(12));
        
        return transaction;
    }

    private LocalDate calculateLastOccurrence(LocalDate nextExpected, RecurringTransaction.RecurrenceFrequency frequency) {
        switch (frequency) {
            case WEEKLY:
                return nextExpected.minusWeeks(1);
            case BI_WEEKLY:
                return nextExpected.minusWeeks(2);
            case MONTHLY:
                return nextExpected.minusMonths(1);
            case QUARTERLY:
                return nextExpected.minusMonths(3);
            case ANNUALLY:
                return nextExpected.minusYears(1);
            default:
                return nextExpected.minusMonths(1);
        }
    }

    private LocalDate getNextBiWeeklyDate() {
        // Calculate next bi-weekly date (assuming payday is every other Friday)
        LocalDate today = LocalDate.now();
        LocalDate nextFriday = today.plusDays((12 - today.getDayOfWeek().getValue()) % 7);
        return nextFriday;
    }

    private LocalDate getNextWeeklyDate() {
        // Next Sunday for weekly groceries
        LocalDate today = LocalDate.now();
        return today.plusDays((7 - today.getDayOfWeek().getValue()) % 7 + 1);
    }

    private LocalDate getNextQuarterlyDate() {
        // Next quarter end
        LocalDate today = LocalDate.now();
        int currentQuarter = (today.getMonthValue() - 1) / 3;
        int nextQuarter = (currentQuarter + 1) % 4;
        int nextQuarterMonth = nextQuarter * 3 + 3;
        
        if (nextQuarterMonth <= today.getMonthValue()) {
            return LocalDate.of(today.getYear() + 1, nextQuarterMonth, 15);
        } else {
            return LocalDate.of(today.getYear(), nextQuarterMonth, 15);
        }
    }
}
