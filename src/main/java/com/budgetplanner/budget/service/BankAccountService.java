package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankAccountService {
    
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final BudgetItemRepository budgetItemRepository;
    
    public BankAccountService(BankAccountRepository bankAccountRepository,
                             BankTransactionRepository bankTransactionRepository,
                             BudgetItemRepository budgetItemRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.budgetItemRepository = budgetItemRepository;
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
        
        // Group transactions by budget category and category type
        Map<String, Map<String, Double>> categoryTotals = transactions.stream()
                .filter(t -> t.getBudgetCategory() != null && t.getBudgetCategoryType() != null)
                .collect(Collectors.groupingBy(
                        BankTransaction::getBudgetCategoryType,
                        Collectors.groupingBy(
                                BankTransaction::getBudgetCategory,
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
        
        // Group transactions by budget category and category type
        Map<String, Map<String, Double>> categoryTotals = transactions.stream()
                .filter(t -> t.getBudgetCategory() != null && t.getBudgetCategoryType() != null)
                .collect(Collectors.groupingBy(
                        BankTransaction::getBudgetCategoryType,
                        Collectors.groupingBy(
                                BankTransaction::getBudgetCategory,
                                Collectors.summingDouble(BankTransaction::getAmount)
                        )
                ));
        
        // Convert to BudgetItems
        List<BudgetItem> budgetItems = new java.util.ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> categoryTypeEntry : categoryTotals.entrySet()) {
            String categoryType = categoryTypeEntry.getKey();
            
            for (Map.Entry<String, Double> categoryEntry : categoryTypeEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                Double totalAmount = Math.abs(categoryEntry.getValue()); // Use absolute value
                
                // Create BudgetItem with actual amount from transactions
                BudgetItem budgetItem = new BudgetItem(
                    category,
                    0.0, // No planned amount from transactions (primitive double)
                    totalAmount.doubleValue(), // Actual amount from transactions (primitive double)
                    categoryType,
                    yearMonth.getYear(), // primitive int
                    yearMonth.getMonthValue() // primitive int
                );
                
                budgetItems.add(budgetItem);
            }
        }
        
        return budgetItems;
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
     * Get the bank account repository for direct access
     */
    public BankAccountRepository getBankAccountRepository() {
        return bankAccountRepository;
    }
}
