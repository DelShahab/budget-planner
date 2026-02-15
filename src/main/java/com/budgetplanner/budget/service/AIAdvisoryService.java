package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.util.CurrencyFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AIAdvisoryService {
    
    private final BankTransactionRepository bankTransactionRepository;
    private final BankAccountService bankAccountService;
    
    public AIAdvisoryService(BankTransactionRepository bankTransactionRepository, 
                           BankAccountService bankAccountService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.bankAccountService = bankAccountService;
    }
    
    /**
     * Analyze spending patterns and generate personalized money-saving tips
     */
    public List<AdvisoryTip> generatePersonalizedTips(YearMonth currentMonth) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        // Get transaction data for analysis
        LocalDate startDate = currentMonth.minusMonths(3).atDay(1); // Last 3 months
        LocalDate endDate = currentMonth.atEndOfMonth();
        List<BankTransaction> transactions = bankTransactionRepository
            .findByTransactionDateBetween(startDate, endDate);
        
        // Analyze different spending patterns
        tips.addAll(analyzeExpenseOverruns(currentMonth));
        tips.addAll(analyzeSpendingTrends(transactions, currentMonth));
        tips.addAll(analyzeFrequentExpenses(transactions));
        tips.addAll(analyzeBudgetVariance(currentMonth));
        tips.addAll(analyzeSavingsOpportunities(transactions));
        tips.addAll(analyzeSeasonalPatterns(transactions, currentMonth));
        
        // Sort tips by priority and limit to top recommendations
        return tips.stream()
            .sorted(Comparator.comparing(AdvisoryTip::getPriority).reversed())
            .limit(6)
            .collect(Collectors.toList());
    }
    
    /**
     * Analyze categories where spending exceeds budget
     */
    private List<AdvisoryTip> analyzeExpenseOverruns(YearMonth currentMonth) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        try {
            List<BudgetItem> budgetItems = bankAccountService.generateBudgetItemsFromTransactions(currentMonth);
            
            for (BudgetItem item : budgetItems) {
                if (item.getActual() > item.getPlanned() && item.getPlanned() > 0) {
                    double overrun = item.getActual() - item.getPlanned();
                    double overrunPercent = (overrun / item.getPlanned()) * 100;
                    
                    if (overrunPercent > 20) { // Significant overrun
                        String message = String.format(
                            "You've overspent on %s by %s (%.0f%% over budget). " +
                            "Consider setting spending alerts or finding alternatives to reduce this category.",
                            item.getCategory(), CurrencyFormatter.formatUSD(overrun), overrunPercent
                        );
                        
                        tips.add(new AdvisoryTip(
                            "Budget Overrun Alert",
                            message,
                            AdvisoryTip.TipType.WARNING,
                            AdvisoryTip.Category.BUDGETING,
                            calculatePriority(overrunPercent, 50)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Handle gracefully if budget analysis fails
        }
        
        return tips;
    }
    
    /**
     * Analyze spending trends over time
     */
    private List<AdvisoryTip> analyzeSpendingTrends(List<BankTransaction> transactions, YearMonth currentMonth) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        // Group transactions by month and category
        Map<String, Map<YearMonth, Double>> categoryMonthlySpending = transactions.stream()
            .filter(t -> t.getBudgetCategoryType() != null && !t.getBudgetCategoryType().equals("INCOME"))
            .collect(Collectors.groupingBy(
                t -> t.getBudgetCategoryType() + ":" + t.getBudgetCategory(),
                Collectors.groupingBy(
                    t -> YearMonth.from(t.getTransactionDate()),
                    Collectors.summingDouble(t -> Math.abs(t.getAmount()))
                )
            ));
        
        for (Map.Entry<String, Map<YearMonth, Double>> categoryEntry : categoryMonthlySpending.entrySet()) {
            String[] parts = categoryEntry.getKey().split(":");
            String category = parts.length > 1 ? parts[1] : parts[0];
            
            Map<YearMonth, Double> monthlySpending = categoryEntry.getValue();
            
            // Check for increasing trend
            List<YearMonth> months = monthlySpending.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
            
            if (months.size() >= 2) {
                YearMonth lastMonth = months.get(months.size() - 1);
                YearMonth previousMonth = months.get(months.size() - 2);
                
                double lastAmount = monthlySpending.get(lastMonth);
                double previousAmount = monthlySpending.get(previousMonth);
                
                if (lastAmount > previousAmount * 1.3) { // 30% increase
                    double increase = lastAmount - previousAmount;
                    String message = String.format(
                        "Your %s spending increased by %s this month. " +
                        "Review recent purchases and consider if this trend aligns with your financial goals.",
                        category, CurrencyFormatter.formatUSD(increase)
                    );
                    
                    tips.add(new AdvisoryTip(
                        "Spending Trend Alert",
                        message,
                        AdvisoryTip.TipType.INSIGHT,
                        AdvisoryTip.Category.SPENDING_ANALYSIS,
                        calculatePriority(increase, 100)
                    ));
                }
            }
        }
        
        return tips;
    }
    
    /**
     * Analyze frequent small expenses that add up
     */
    private List<AdvisoryTip> analyzeFrequentExpenses(List<BankTransaction> transactions) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        // Group by merchant and analyze frequency
        Map<String, List<BankTransaction>> merchantTransactions = transactions.stream()
            .filter(t -> t.getAmount() < 0 && Math.abs(t.getAmount()) < 50) // Small expenses
            .collect(Collectors.groupingBy(BankTransaction::getMerchantName));
        
        for (Map.Entry<String, List<BankTransaction>> entry : merchantTransactions.entrySet()) {
            String merchant = entry.getKey();
            List<BankTransaction> merchantTxns = entry.getValue();
            
            if (merchantTxns.size() >= 8) { // Frequent purchases
                double totalSpent = merchantTxns.stream()
                    .mapToDouble(t -> Math.abs(t.getAmount()))
                    .sum();
                
                if (totalSpent > 100) {
                    String message = String.format(
                        "You've made %d purchases at %s totaling %s. " +
                        "Consider if these frequent small purchases align with your budget priorities.",
                        merchantTxns.size(), merchant, CurrencyFormatter.formatUSD(totalSpent)
                    );
                    
                    tips.add(new AdvisoryTip(
                        "Frequent Small Purchases",
                        message,
                        AdvisoryTip.TipType.SUGGESTION,
                        AdvisoryTip.Category.SPENDING_HABITS,
                        calculatePriority(totalSpent, 200)
                    ));
                }
            }
        }
        
        return tips;
    }
    
    /**
     * Analyze budget variance and suggest improvements
     */
    private List<AdvisoryTip> analyzeBudgetVariance(YearMonth currentMonth) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        try {
            List<BudgetItem> budgetItems = bankAccountService.generateBudgetItemsFromTransactions(currentMonth);
            
            // Find categories with consistently low spending
            for (BudgetItem item : budgetItems) {
                if (item.getPlanned() > 0 && item.getActual() < item.getPlanned() * 0.7) {
                    double savings = item.getPlanned() - item.getActual();
                    
                    if (savings > 50) {
                        String message = String.format(
                            "You're spending %s less than budgeted on %s. " +
                            "Consider reallocating this surplus to savings or other financial goals.",
                            CurrencyFormatter.formatUSD(savings), item.getCategory()
                        );
                        
                        tips.add(new AdvisoryTip(
                            "Budget Reallocation Opportunity",
                            message,
                            AdvisoryTip.TipType.OPPORTUNITY,
                            AdvisoryTip.Category.BUDGETING,
                            calculatePriority(savings, 100)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Handle gracefully
        }
        
        return tips;
    }
    
    /**
     * Identify savings opportunities
     */
    private List<AdvisoryTip> analyzeSavingsOpportunities(List<BankTransaction> transactions) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        // Analyze dining out vs groceries
        double diningOut = transactions.stream()
            .filter(t -> "Dining Out".equals(t.getBudgetCategory()))
            .mapToDouble(t -> Math.abs(t.getAmount()))
            .sum();
        
        double groceries = transactions.stream()
            .filter(t -> "Groceries".equals(t.getBudgetCategory()))
            .mapToDouble(t -> Math.abs(t.getAmount()))
            .sum();
        
        if (diningOut > groceries * 0.8) {
            String message = String.format(
                "You're spending %s on dining out vs %s on groceries. " +
                "Cooking more meals at home could save you approximately %s per month.",
                CurrencyFormatter.formatUSD(diningOut), CurrencyFormatter.formatUSD(groceries), 
                CurrencyFormatter.formatUSD(diningOut * 0.3)
            );
            
            tips.add(new AdvisoryTip(
                "Home Cooking Savings",
                message,
                AdvisoryTip.TipType.SUGGESTION,
                AdvisoryTip.Category.MONEY_SAVING,
                calculatePriority(diningOut * 0.3, 150)
            ));
        }
        
        // Analyze subscription-like expenses
        Map<String, Long> merchantFrequency = transactions.stream()
            .filter(t -> t.getAmount() < 0)
            .collect(Collectors.groupingBy(
                BankTransaction::getMerchantName,
                Collectors.counting()
            ));
        
        for (Map.Entry<String, Long> entry : merchantFrequency.entrySet()) {
            if (entry.getValue() >= 3 && entry.getKey().toLowerCase().contains("subscription")) {
                String message = String.format(
                    "Review your subscription to %s. Cancel unused subscriptions to free up monthly budget.",
                    entry.getKey()
                );
                
                tips.add(new AdvisoryTip(
                    "Subscription Review",
                    message,
                    AdvisoryTip.TipType.SUGGESTION,
                    AdvisoryTip.Category.MONEY_SAVING,
                    70
                ));
            }
        }
        
        return tips;
    }
    
    /**
     * Analyze seasonal spending patterns
     */
    private List<AdvisoryTip> analyzeSeasonalPatterns(List<BankTransaction> transactions, YearMonth currentMonth) {
        List<AdvisoryTip> tips = new ArrayList<>();
        
        int currentMonthValue = currentMonth.getMonthValue();
        
        // Holiday spending analysis (November-December)
        if (currentMonthValue == 11 || currentMonthValue == 12) {
            double holidaySpending = transactions.stream()
                .filter(t -> t.getTransactionDate().getMonthValue() >= 11)
                .filter(t -> "Shopping".equals(t.getBudgetCategory()) || "Entertainment".equals(t.getBudgetCategory()))
                .mapToDouble(t -> Math.abs(t.getAmount()))
                .sum();
            
            if (holidaySpending > 500) {
                String message = "Holiday spending is elevated. Consider setting a holiday budget limit and tracking gift expenses to avoid overspending.";
                
                tips.add(new AdvisoryTip(
                    "Holiday Spending Alert",
                    message,
                    AdvisoryTip.TipType.WARNING,
                    AdvisoryTip.Category.SEASONAL,
                    85
                ));
            }
        }
        
        return tips;
    }
    
    /**
     * Calculate priority score for tips
     */
    private int calculatePriority(double impact, double maxImpact) {
        return Math.min(100, (int) ((impact / maxImpact) * 100));
    }
    
    /**
     * Data class for advisory tips
     */
    public static class AdvisoryTip {
        private final String title;
        private final String message;
        private final TipType type;
        private final Category category;
        private final int priority;
        
        public AdvisoryTip(String title, String message, TipType type, Category category, int priority) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.category = category;
            this.priority = Math.max(0, Math.min(100, priority)); // Clamp between 0-100
        }
        
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public TipType getType() { return type; }
        public Category getCategory() { return category; }
        public int getPriority() { return priority; }
        
        public enum TipType {
            SUGGESTION,    // Blue
            WARNING,       // Pink
            OPPORTUNITY,   // Green
            INSIGHT        // Orange
        }
        
        public enum Category {
            BUDGETING,
            SPENDING_ANALYSIS,
            SPENDING_HABITS,
            MONEY_SAVING,
            SEASONAL,
            INVESTMENT
        }
    }
}
