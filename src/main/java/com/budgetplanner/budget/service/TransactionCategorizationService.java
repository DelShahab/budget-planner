package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankTransaction;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TransactionCategorizationService {
    
    // Constants for category types
    private static final String CATEGORY_INCOME = "INCOME";
    private static final String CATEGORY_EXPENSES = "EXPENSES";
    private static final String CATEGORY_BILLS = "BILLS";
    private static final String CATEGORY_SAVINGS = "SAVINGS";
    
    // Plaid category to budget category mapping
    private final Map<String, CategoryMapping> plaidCategoryMappings;
    
    // Merchant name patterns for better categorization
    private final Map<Pattern, CategoryMapping> merchantPatterns;
    
    public TransactionCategorizationService() {
        this.plaidCategoryMappings = initializePlaidCategoryMappings();
        this.merchantPatterns = initializeMerchantPatterns();
    }
    
    /**
     * Categorize a bank transaction based on Plaid categories and merchant patterns
     */
    public void categorizeTransaction(BankTransaction transaction) {
        CategoryMapping mapping = null;
        
        // First, try to categorize by merchant name patterns
        mapping = categorizeBySmerchantName(transaction.getMerchantName(), transaction.getDescription());
        
        // If no merchant pattern match, use Plaid category
        if (mapping == null && transaction.getPlaidCategory() != null) {
            mapping = plaidCategoryMappings.get(transaction.getPlaidCategory().toLowerCase());
        }
        
        // Handle income transactions (negative amounts in Plaid)
        if (transaction.getAmount() < 0) {
            mapping = new CategoryMapping("Salary", CATEGORY_INCOME);
            
            // Check for specific income types
            String merchantLower = transaction.getMerchantName().toLowerCase();
            String descLower = transaction.getDescription().toLowerCase();
            
            if (merchantLower.contains("payroll") || descLower.contains("salary") || descLower.contains("paycheck")) {
                mapping = new CategoryMapping("Salary", CATEGORY_INCOME);
            } else if (descLower.contains("freelance") || descLower.contains("contractor")) {
                mapping = new CategoryMapping("Freelance", CATEGORY_INCOME);
            } else if (descLower.contains("dividend") || descLower.contains("interest") || descLower.contains("investment")) {
                mapping = new CategoryMapping("Investment Returns", CATEGORY_INCOME);
            } else if (descLower.contains("refund") || descLower.contains("return")) {
                mapping = new CategoryMapping("Refunds", CATEGORY_INCOME);
            }
        }
        
        // Default categorization if nothing matches
        if (mapping == null) {
            mapping = new CategoryMapping("Other", CATEGORY_EXPENSES);
        }
        
        // Apply the categorization
        transaction.setBudgetCategory(mapping.category);
        transaction.setBudgetCategoryType(mapping.categoryType);
    }
    
    /**
     * Try to categorize by merchant name and description patterns
     */
    private CategoryMapping categorizeBySmerchantName(String merchantName, String description) {
        String searchText = (merchantName + " " + description).toLowerCase();
        
        for (Map.Entry<Pattern, CategoryMapping> entry : merchantPatterns.entrySet()) {
            if (entry.getKey().matcher(searchText).find()) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Initialize Plaid category to budget category mappings
     */
    private Map<String, CategoryMapping> initializePlaidCategoryMappings() {
        Map<String, CategoryMapping> mappings = new HashMap<>();
        
        // Food and Drink
        mappings.put("food and drink", new CategoryMapping("Groceries", CATEGORY_EXPENSES));
        mappings.put("restaurants", new CategoryMapping("Dining Out", CATEGORY_EXPENSES));
        mappings.put("fast food", new CategoryMapping("Dining Out", CATEGORY_EXPENSES));
        mappings.put("coffee shop", new CategoryMapping("Dining Out", CATEGORY_EXPENSES));
        
        // Transportation
        mappings.put("transportation", new CategoryMapping("Gas", CATEGORY_EXPENSES));
        mappings.put("gas stations", new CategoryMapping("Gas", CATEGORY_EXPENSES));
        mappings.put("automotive", new CategoryMapping("Gas", CATEGORY_EXPENSES));
        mappings.put("public transportation", new CategoryMapping("Gas", CATEGORY_EXPENSES));
        
        // Bills and Utilities
        mappings.put("utilities", new CategoryMapping("Utilities", CATEGORY_BILLS));
        mappings.put("telecommunication services", new CategoryMapping("Phone", CATEGORY_BILLS));
        mappings.put("internet", new CategoryMapping("Internet", CATEGORY_BILLS));
        mappings.put("cable", new CategoryMapping("Internet", CATEGORY_BILLS));
        mappings.put("rent", new CategoryMapping("Rent", CATEGORY_BILLS));
        mappings.put("mortgage", new CategoryMapping("Rent", CATEGORY_BILLS));
        mappings.put("insurance", new CategoryMapping("Insurance", CATEGORY_BILLS));
        
        // Shopping
        mappings.put("shops", new CategoryMapping("Shopping", CATEGORY_EXPENSES));
        mappings.put("clothing and accessories", new CategoryMapping("Shopping", CATEGORY_EXPENSES));
        mappings.put("electronics", new CategoryMapping("Shopping", CATEGORY_EXPENSES));
        mappings.put("home improvement", new CategoryMapping("Shopping", CATEGORY_EXPENSES));
        
        // Entertainment
        mappings.put("entertainment", new CategoryMapping("Entertainment", CATEGORY_EXPENSES));
        mappings.put("recreation", new CategoryMapping("Entertainment", CATEGORY_EXPENSES));
        mappings.put("arts and entertainment", new CategoryMapping("Entertainment", CATEGORY_EXPENSES));
        mappings.put("gyms and fitness centers", new CategoryMapping("Entertainment", CATEGORY_EXPENSES));
        
        // Healthcare
        mappings.put("healthcare", new CategoryMapping("Healthcare", CATEGORY_EXPENSES));
        mappings.put("pharmacies", new CategoryMapping("Healthcare", CATEGORY_EXPENSES));
        
        // Transfer and Savings
        mappings.put("transfer", new CategoryMapping("Emergency Fund", CATEGORY_SAVINGS));
        mappings.put("deposit", new CategoryMapping("Emergency Fund", CATEGORY_SAVINGS));
        mappings.put("investment", new CategoryMapping("Retirement", CATEGORY_SAVINGS));
        
        return mappings;
    }
    
    /**
     * Initialize merchant name patterns for better categorization
     */
    private Map<Pattern, CategoryMapping> initializeMerchantPatterns() {
        Map<Pattern, CategoryMapping> patterns = new HashMap<>();
        
        // Grocery stores
        patterns.put(Pattern.compile("walmart|target|costco|safeway|kroger|publix|whole foods|trader joe", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Groceries", CATEGORY_EXPENSES));
        
        // Gas stations
        patterns.put(Pattern.compile("shell|exxon|chevron|bp|mobil|arco|texaco|marathon", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Gas", CATEGORY_EXPENSES));
        
        // Restaurants and fast food
        patterns.put(Pattern.compile("mcdonald|burger king|kfc|taco bell|subway|pizza|starbucks|dunkin", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Dining Out", CATEGORY_EXPENSES));
        
        // Utilities and bills
        patterns.put(Pattern.compile("electric|power|gas company|water|sewer|comcast|verizon|at&t|t-mobile", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Utilities", CATEGORY_BILLS));
        
        // Rent and mortgage
        patterns.put(Pattern.compile("rent|mortgage|property management|landlord", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Rent", CATEGORY_BILLS));
        
        // Insurance
        patterns.put(Pattern.compile("insurance|allstate|geico|progressive|state farm", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Insurance", CATEGORY_BILLS));
        
        // Entertainment
        patterns.put(Pattern.compile("netflix|spotify|amazon prime|hulu|disney|gym|fitness|movie|theater", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Entertainment", CATEGORY_EXPENSES));
        
        // Shopping
        patterns.put(Pattern.compile("amazon|ebay|best buy|home depot|lowes|macy|nordstrom", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Shopping", CATEGORY_EXPENSES));
        
        // Savings and investments
        patterns.put(Pattern.compile("savings|investment|401k|ira|retirement|fidelity|vanguard|schwab", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Retirement", CATEGORY_SAVINGS));
        
        // Emergency fund transfers
        patterns.put(Pattern.compile("emergency|transfer.*saving|saving.*transfer", Pattern.CASE_INSENSITIVE), 
                    new CategoryMapping("Emergency Fund", CATEGORY_SAVINGS));
        
        return patterns;
    }
    
    /**
     * Inner class to hold category mapping information
     */
    private static class CategoryMapping {
        final String category;
        final String categoryType;
        
        CategoryMapping(String category, String categoryType) {
            this.category = category;
            this.categoryType = categoryType;
        }
    }
}
