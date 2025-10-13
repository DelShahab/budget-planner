package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardDataService {
    
    private final BankTransactionRepository transactionRepository;
    private static final double IDR_TO_USD_RATE = 0.000064; // 1 IDR = ~0.000064 USD (approximate rate)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    
    public DashboardDataService(BankTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Convert IDR amount to USD
     */
    public double convertToUSD(double idrAmount) {
        return idrAmount * IDR_TO_USD_RATE;
    }
    
    /**
     * Format USD amount
     */
    public String formatUSD(double usdAmount) {
        return String.format("$%.2f", Math.abs(usdAmount));
    }
    
    /**
     * Format IDR amount
     */
    public String formatIDR(double idrAmount) {
        return String.format("Rp %.0f", Math.abs(idrAmount));
    }
    
    /**
     * Get total earnings for the current month
     */
    public double getTotalEarnings() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(startOfMonth, endOfMonth);
        
        return transactions.stream()
                .filter(t -> t.getAmount() > 0) // Positive amounts are income
                .mapToDouble(BankTransaction::getAmount)
                .sum();
    }
    
    /**
     * Get total spendings for the current month
     */
    public double getTotalSpendings() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(startOfMonth, endOfMonth);
        
        return Math.abs(transactions.stream()
                .filter(t -> t.getAmount() < 0) // Negative amounts are expenses
                .mapToDouble(BankTransaction::getAmount)
                .sum());
    }
    
    /**
     * Get daily expenses
     */
    public double getDailyExpenses() {
        LocalDate today = LocalDate.now();
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(today, today);
        
        return Math.abs(transactions.stream()
                .filter(t -> t.getAmount() < 0)
                .mapToDouble(BankTransaction::getAmount)
                .sum());
    }
    
    /**
     * Get weekly expenses
     */
    public double getWeeklyExpenses() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(weekAgo, today);
        
        return Math.abs(transactions.stream()
                .filter(t -> t.getAmount() < 0)
                .mapToDouble(BankTransaction::getAmount)
                .sum());
    }
    
    /**
     * Get expenses by category for the current month
     */
    public Map<String, Double> getExpensesByCategory() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(startOfMonth, endOfMonth);
        
        return transactions.stream()
                .filter(t -> t.getAmount() < 0 && t.getBudgetCategory() != null)
                .collect(Collectors.groupingBy(
                        BankTransaction::getBudgetCategory,
                        Collectors.summingDouble(t -> Math.abs(t.getAmount()))
                ));
    }
    
    /**
     * Get recent transactions grouped by date
     */
    public Map<String, List<BankTransaction>> getRecentTransactions(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, today);
        
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().format(DATE_FORMATTER),
                        Collectors.toList()
                ));
    }
    
    /**
     * Get activity statistics data for chart (last 30 days)
     */
    public Map<Integer, Double> getActivityStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(30);
        
        List<BankTransaction> transactions = transactionRepository
                .findByTransactionDateBetween(startDate, today);
        
        Map<Integer, Double> dailyTotals = new HashMap<>();
        
        // Group by day of month and sum expenses
        transactions.stream()
                .filter(t -> t.getAmount() < 0)
                .forEach(t -> {
                    int day = t.getTransactionDate().getDayOfMonth();
                    double amount = Math.abs(t.getAmount());
                    dailyTotals.merge(day, amount, Double::sum);
                });
        
        return dailyTotals;
    }
    
    /**
     * Calculate expense percentage by category
     */
    public Map<String, Double> getExpensePercentages() {
        Map<String, Double> expensesByCategory = getExpensesByCategory();
        double total = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (total == 0) {
            return Map.of();
        }
        
        return expensesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (e.getValue() / total) * 100
                ));
    }
    
    /**
     * Get category icon color
     */
    public String getCategoryColor(String category) {
        return switch (category != null ? category.toLowerCase() : "") {
            case "shopping", "retail" -> "#ef4444";
            case "platform", "transfer" -> "#22c55e";
            case "food & drinks", "food", "dining" -> "#f59e0b";
            case "business", "income" -> "#a855f7";
            case "transportation", "travel" -> "#3b82f6";
            default -> "#6b7280";
        };
    }
}
