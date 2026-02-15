package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.model.SavingsGoal;
import com.budgetplanner.budget.model.TransactionSplit;
import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.repository.TransactionSplitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankAccountService {
    
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final SavingsGoalService savingsGoalService;
    private final TransactionSplitRepository transactionSplitRepository;
    
    public BankAccountService(BankAccountRepository bankAccountRepository,
                             BankTransactionRepository bankTransactionRepository,
                             BudgetItemRepository budgetItemRepository,
                             SavingsGoalService savingsGoalService,
                             TransactionSplitRepository transactionSplitRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.savingsGoalService = savingsGoalService;
        this.transactionSplitRepository = transactionSplitRepository;
    }
    
    /**
     * Get all active bank accounts
     */
    public List<BankAccount> getActiveBankAccounts() {
        return bankAccountRepository.findActiveAccountsOrderByCreatedDesc();
    }
    
    /**
     * Get bank account by ID
     */
    public BankAccount getBankAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
    }
    
    /**
     * Update budget items with actual amounts from bank transactions for a specific month/year
     */
    public void updateBudgetItemsFromTransactions(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Get all transactions for the specified month
        List<BankTransaction> transactions = bankTransactionRepository
                .findByTransactionDateBetween(startDate, endDate);
        
        // Group transactions by resolved budget category type and category
        Map<String, Map<String, Double>> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        this::resolveCategoryType,
                        Collectors.groupingBy(
                                this::resolveCategory,
                                Collectors.summingDouble(BankTransaction::getAmount)
                        )
                ));
        
        // Update or create budget items
        for (Map.Entry<String, Map<String, Double>> categoryTypeEntry : categoryTotals.entrySet()) {
            String categoryType = categoryTypeEntry.getKey();
            
            for (Map.Entry<String, Double> categoryEntry : categoryTypeEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                Double totalAmount = Math.abs(categoryEntry.getValue()); // Use absolute value
                
                // Find existing budget item or create new one
                BudgetItem budgetItem = budgetItemRepository
                        .findByCategoryAndCategoryTypeAndYearAndMonth(category, categoryType, year, month)
                        .orElse(new BudgetItem(category, Double.valueOf(0.0), totalAmount, categoryType, Integer.valueOf(year), Integer.valueOf(month)));
                
                // Update actual amount
                budgetItem.setActual(totalAmount);
                budgetItemRepository.save(budgetItem);
            }
        }
    }
    
    /**
     * Get uncategorized transactions that need manual review
     */
    public List<BankTransaction> getUncategorizedTransactions() {
        return bankTransactionRepository.findUncategorizedTransactions();
    }
    
    /**
     * Update transaction categorization
     */
    public void updateTransactionCategory(Long transactionId, String budgetCategory, String budgetCategoryType) {
        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));
        
        transaction.setBudgetCategory(budgetCategory);
        transaction.setBudgetCategoryType(budgetCategoryType);
        transaction.setIsManuallyReviewed(true);
        
        bankTransactionRepository.save(transaction);
    }
    
    /**
     * Get transactions for a specific date range and category type
     */
    public List<BankTransaction> getTransactionsByDateRangeAndCategory(LocalDate startDate, LocalDate endDate, String categoryType) {
        return bankTransactionRepository.findTransactionsByDateRangeAndCategoryType(startDate, endDate, categoryType);
    }
    
    /**
     * Get total amount for a category type in a specific month
     */
    public Double getTotalAmountForCategoryType(int year, int month, String categoryType) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        Double total = bankTransactionRepository.sumAmountByDateRangeAndCategoryType(startDate, endDate, categoryType);
        return total != null ? Math.abs(total) : 0.0;
    }
    
    /**
     * Check if there are any linked bank accounts
     */
    public boolean hasLinkedAccounts() {
        return !bankAccountRepository.findByIsActiveTrue().isEmpty();
    }
    
    /**
     * Get summary of all bank accounts
     */
    public Map<String, Object> getBankAccountsSummary() {
        List<BankAccount> accounts = getActiveBankAccounts();
        int totalAccounts = accounts.size();
        
        Map<String, Long> accountsByType = accounts.stream()
                .collect(Collectors.groupingBy(
                        BankAccount::getAccountType,
                        Collectors.counting()
                ));
        
        return Map.of(
                "totalAccounts", totalAccounts,
                "accountsByType", accountsByType,
                "accounts", accounts
        );
    }
    
    /**
     * Generate BudgetItems from bank transactions for a specific month
     */
    public List<BudgetItem> generateBudgetItemsFromTransactions(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Get all transactions for the specified month
        List<BankTransaction> transactions = bankTransactionRepository
                .findByTransactionDateBetween(startDate, endDate);
        
        // Group transactions by resolved budget category type and category
        Map<String, Map<String, Double>> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        this::resolveCategoryType,
                        Collectors.groupingBy(
                                this::resolveCategory,
                                Collectors.summingDouble(BankTransaction::getAmount)
                        )
                ));
        
        // Fetch existing persistent budget items (to get Planned amounts)
        // We assume the repository has a method to find by year/month, or we filter manually if needed.
        // Since we can't see the Repo interface, we'll use a safe approach: find by year/month if possible,
        // or rely on the fact that we want to merge transient actuals with persistent planned values.
        
        List<BudgetItem> persistentItems = budgetItemRepository.findByYearAndMonth(yearMonth.getYear(), yearMonth.getMonthValue());
        Map<String, BudgetItem> persistentMap = persistentItems.stream()
                .collect(Collectors.toMap(BudgetItem::getCategory, item -> item, (a, b) -> a));

        List<BudgetItem> resultItems = new ArrayList<>();
        
        // 1. Process categories that have transactions (Actuals)
        for (Map.Entry<String, Map<String, Double>> categoryTypeEntry : categoryTotals.entrySet()) {
            String categoryType = categoryTypeEntry.getKey();
            
            for (Map.Entry<String, Double> categoryEntry : categoryTypeEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                Double totalAmount = Math.abs(categoryEntry.getValue()); // Use absolute value
                
                // Check if we have a persistent item for this category
                if (persistentMap.containsKey(category)) {
                    BudgetItem existing = persistentMap.get(category);
                    existing.setActual(totalAmount);
                    // Ensure type matches current transaction reality
                    if (!existing.getCategoryType().equals(categoryType)) {
                        existing.setCategoryType(categoryType);
                    }
                    resultItems.add(existing);
                    persistentMap.remove(category); // Mark as processed
                } else {
                    // Create new transient item (or we could save it to DB here)
                    BudgetItem budgetItem = new BudgetItem(
                        category,
                        0.0d, // No planned amount yet (primitive double)
                        totalAmount != null ? totalAmount.doubleValue() : 0.0d,
                        categoryType,
                        yearMonth.getYear(),
                        yearMonth.getMonthValue()
                    );
                    resultItems.add(budgetItem);
                }
            }
        }
        
        // 2. Add remaining persistent items that had no transactions this month (Actual = 0)
        for (BudgetItem remaining : persistentMap.values()) {
            remaining.setActual(0.0);
            resultItems.add(remaining);
        }
        
        return resultItems;
    }

    /**
     * Generate BudgetItems from ALL bank transactions (no date filter).
     * This is primarily used for views like Trends & Activity that should
     * always show data regardless of transaction month.
     */
    public List<BudgetItem> generateBudgetItemsFromAllTransactions() {
        List<BankTransaction> transactions = bankTransactionRepository.findAll();

        Map<String, Map<String, Double>> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        this::resolveCategoryType,
                        Collectors.groupingBy(
                                this::resolveCategory,
                                Collectors.summingDouble(BankTransaction::getAmount)
                        )
                ));

        List<BudgetItem> budgetItems = new java.util.ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> categoryTypeEntry : categoryTotals.entrySet()) {
            String categoryType = categoryTypeEntry.getKey();

            for (Map.Entry<String, Double> categoryEntry : categoryTypeEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                Double totalAmount = Math.abs(categoryEntry.getValue());

                BudgetItem budgetItem = new BudgetItem(
                        category,
                        0.0,
                        totalAmount.doubleValue(),
                        categoryType,
                        0, // year not relevant for all-time aggregation
                        0  // month not relevant for all-time aggregation
                );

                budgetItems.add(budgetItem);
            }
        }

        return budgetItems;
    }

    /**
     * Save or update a budget item (e.g. updating Planned amount).
     */
    public BudgetItem saveBudgetItem(BudgetItem item) {
        return budgetItemRepository.save(item);
    }

    /**
     * Resolve a simple category type for a transaction.
     * If an explicit budgetCategoryType is set, use it.
     * Otherwise, infer from the sign of the amount:
     * - amount >= 0 -> INCOME
     * - amount < 0  -> EXPENSES
     */
    private String resolveCategoryType(BankTransaction transaction) {
        // If an explicit budget type is already set, respect it
        String explicitType = transaction.getBudgetCategoryType();
        if (explicitType != null && !explicitType.isBlank()) {
            return explicitType;
        }

        // Use combined category text (budget or Plaid) for heuristic mapping
        String categoryText = resolveCategory(transaction);
        String lower = categoryText != null ? categoryText.toLowerCase() : "";

        return predictCategoryType(lower, transaction.getAmount());
    }

    /**
     * Predict category type based on description and amount.
     * Public so it can be used by UI for CSV imports.
     */
    public String predictCategoryType(String description, Double amount) {
        String lower = description.toLowerCase();
        
        // Income-related keywords
        if (lower.contains("salary") || lower.contains("payroll") || lower.contains("wage")
                || lower.contains("income") || lower.contains("paycheck")
                || lower.contains("deposit") || lower.contains("transfer from")
                || lower.contains("refund")) {
            return "INCOME";
        }

        // Bills-related keywords
        if (lower.contains("rent") || lower.contains("mortgage") || lower.contains("utility")
                || lower.contains("electric") || lower.contains("water") || lower.contains("gas")
                || lower.contains("internet") || lower.contains("phone") || lower.contains("telecom")
                || lower.contains("insurance") || lower.contains("bill") || lower.contains("loan")
                || lower.contains("credit card") || lower.contains("autopay")) {
            return "BILLS";
        }

        // Savings-related keywords
        if (lower.contains("saving") || lower.contains("savings") || lower.contains("invest")
                || lower.contains("investment") || lower.contains("401k")
                || lower.contains("ira") || lower.contains("retirement") || lower.contains("transfer to")) {
            return "SAVINGS";
        }

        // Expenses (Food, Transport, Shopping, etc.)
        if (lower.contains("grocery") || lower.contains("food") || lower.contains("restaurant") ||
            lower.contains("uber") || lower.contains("amazon") || lower.contains("target") ||
            lower.contains("walmart") || lower.contains("netflix") || lower.contains("coffee")) {
            return "EXPENSES";
        }

        // Fallback based on sign of amount
        if (amount == null) {
            return "EXPENSES"; // sensible default
        }
        return amount >= 0 ? "INCOME" : "EXPENSES";
    }

    /**
     * Resolve a simple category label for a transaction.
     * Preference order:
     * 1) budgetCategory
     * 2) plaidCategory
     * 3) "Other"
     */
    private String resolveCategory(BankTransaction transaction) {
        if (transaction.getBudgetCategory() != null && !transaction.getBudgetCategory().isBlank()) {
            return transaction.getBudgetCategory();
        }
        if (transaction.getPlaidCategory() != null && !transaction.getPlaidCategory().isBlank()) {
            String plaid = transaction.getPlaidCategory();
            // Use the top-level Plaid category (e.g. "Transportation" from "Transportation > Gas Stations")
            int separatorIndex = plaid.indexOf('>');
            if (separatorIndex > 0) {
                return plaid.substring(0, separatorIndex).trim();
            }
            return plaid.trim();
        }
        return "Other";
    }
    
    /**
     * Manually create a new transaction
     */
    public BankTransaction createManualTransaction(String merchantName, String description, 
                                                  Double amount, LocalDate transactionDate, 
                                                  String budgetCategory, String budgetCategoryType) {
        // Create a new manual transaction
        BankTransaction transaction = new BankTransaction();
        transaction.setPlaidTransactionId("manual_" + System.currentTimeMillis() + "_" + Math.random());
        transaction.setMerchantName(merchantName);
        transaction.setDescription(description != null ? description : merchantName + " Transaction");
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setAuthorizedDate(transactionDate);
        transaction.setTransactionType(amount >= 0 ? "credit" : "debit");
        transaction.setBudgetCategory(budgetCategory);
        transaction.setBudgetCategoryType(budgetCategoryType);
        transaction.setIsManuallyReviewed(true);
        transaction.setIsProcessed(true);
        
        // Set a default bank account (first active account) or null if no accounts
        List<BankAccount> activeAccounts = getActiveBankAccounts();
        if (!activeAccounts.isEmpty()) {
            transaction.setBankAccount(activeAccounts.get(0));
        }
        
        return bankTransactionRepository.save(transaction);
    }
    
    /**
     * Get recent transactions with the same merchant as the given transaction,
     * excluding the transaction itself.
     */
    public List<BankTransaction> getSimilarTransactions(BankTransaction reference, int limit) {
        if (reference == null || reference.getMerchantName() == null) {
            return java.util.Collections.emptyList();
        }
        // Currently repository is hard-coded to top 5; limit parameter reserved for future tuning
        return bankTransactionRepository
                .findTop5ByMerchantNameAndIdNotOrderByTransactionDateDesc(
                        reference.getMerchantName(), reference.getId());
    }

    /**
     * Goals helper methods for Transaction Details dialog
     */
    public java.util.List<SavingsGoal> getAllActiveSavingsGoals() {
        return savingsGoalService.getAllActiveGoals();
    }

    public SavingsGoal getGoalForTransaction(BankTransaction transaction) {
        if (transaction == null || transaction.getId() == null) {
            return null;
        }
        return bankTransactionRepository.findById(transaction.getId())
                .map(BankTransaction::getSavingsGoal)
                .orElse(null);
    }

    public void setGoalForTransaction(BankTransaction transaction, Long goalId) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        BankTransaction managed = bankTransactionRepository.findById(transaction.getId())
                .orElse(null);
        if (managed == null) {
            return;
        }
        SavingsGoal goal = null;
        if (goalId != null) {
            goal = savingsGoalService.getGoalById(goalId).orElse(null);
        }
        managed.setSavingsGoal(goal);
    }
    
    /**
     * Get all splits for a given transaction.
     */
    public java.util.List<TransactionSplit> getSplitsForTransaction(BankTransaction transaction) {
        if (transaction == null || transaction.getId() == null) {
            return java.util.Collections.emptyList();
        }
        return transactionSplitRepository.findByParentTransaction(transaction);
    }

    /**
     * Save splits for a transaction, ensuring the sum of splits matches the
     * absolute amount of the original transaction (within 1 cent).
     */
    public void saveSplitsForTransaction(BankTransaction transaction, java.util.List<TransactionSplit> splits) {
        if (transaction == null || transaction.getId() == null) {
            throw new IllegalArgumentException("Cannot save splits for a transient transaction");
        }
        if (splits == null || splits.isEmpty()) {
            transactionSplitRepository.deleteByParentTransaction(transaction);
            return;
        }

        double original = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0.0);
        double sum = splits.stream()
                .filter(s -> s.getAmount() != null)
                .mapToDouble(s -> Math.abs(s.getAmount()))
                .sum();

        long originalCents = Math.round(original * 100);
        long sumCents = Math.round(sum * 100);
        if (originalCents != sumCents) {
            throw new IllegalArgumentException("Split amounts must equal the transaction total");
        }

        // Replace existing splits with new ones
        transactionSplitRepository.deleteByParentTransaction(transaction);
        for (TransactionSplit split : splits) {
            split.setParentTransaction(transaction);
        }
        transactionSplitRepository.saveAll(splits);
    }

    /**
     * Link a transaction to a recurring pattern and persist the change.
     */
    public void setRecurringForTransaction(BankTransaction transaction, RecurringTransaction recurringTransaction) {
        if (transaction == null || transaction.getId() == null || recurringTransaction == null || recurringTransaction.getId() == null) {
            return;
        }
        BankTransaction managed = bankTransactionRepository.findById(transaction.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        managed.setRecurringTransaction(recurringTransaction);
        bankTransactionRepository.save(managed);
    }

    /**
     * Get the bank account repository for direct access
     */
    public BankAccountRepository getBankAccountRepository() {
        return bankAccountRepository;
    }
}
