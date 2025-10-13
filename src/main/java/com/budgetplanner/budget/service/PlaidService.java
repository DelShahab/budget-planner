package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.plaid.client.ApiClient;
import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

@Service
@Primary
@Transactional
public class PlaidService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final PlaidApi plaidClient;
    private final String clientId;
    private final String secret;
    private final String environment;
    
    // Inner class for transaction templates
    private static class TransactionTemplate {
        final String merchantName;
        final String plaidCategory;
        final double minAmount;
        final double maxAmount;
        final String budgetCategoryType;
        final String budgetCategory;
        
        TransactionTemplate(String merchantName, String plaidCategory, double minAmount, double maxAmount, 
                          String budgetCategoryType, String budgetCategory) {
            this.merchantName = merchantName;
            this.plaidCategory = plaidCategory;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.budgetCategoryType = budgetCategoryType;
            this.budgetCategory = budgetCategory;
        }
    }

    public PlaidService(BankAccountRepository bankAccountRepository,
                       BankTransactionRepository bankTransactionRepository,
                       @Value("${plaid.client-id}") String clientId,
                       @Value("${plaid.secret}") String secret,
                       @Value("${plaid.environment:sandbox}") String environment) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.clientId = clientId;
        this.secret = secret;
        this.environment = environment;
        
        // Initialize Plaid client
        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);
        
        ApiClient apiClient = new ApiClient(apiKeys);
        // Set environment - sandbox, development, or production
        if ("sandbox".equalsIgnoreCase(environment)) {
            apiClient.setPlaidAdapter(ApiClient.Sandbox);
        } else if ("development".equalsIgnoreCase(environment)) {
            apiClient.setPlaidAdapter(ApiClient.Development);
        } else {
            apiClient.setPlaidAdapter(ApiClient.Production);
        }
        this.plaidClient = apiClient.createService(PlaidApi.class);
    }

    /**
     * Create a link token for Plaid Link initialization
     * 
     * TODO: Fix identity_match error with newer Plaid SDK versions
     * Currently using SDK 9.1.0 to avoid identity_match product requirement.
     * When upgrading to newer SDK (16.0.0+), need to either:
     * 1. Enable identity_match product in Plaid dashboard
     * 2. Configure proper product selection to exclude identity features
     * 3. Update LinkTokenCreateRequest parameters for new API structure
     */
    public String createLinkToken(String userId) {
        try {
            LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .clientName("Budget Planner")
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en")
                .user(new LinkTokenCreateRequestUser().clientUserId(userId))
                .products(Arrays.asList(Products.TRANSACTIONS));
                // Note: redirectUri is optional for sandbox and requires dashboard configuration

            Response<LinkTokenCreateResponse> response = plaidClient.linkTokenCreate(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getLinkToken();
            } else {
                // Better error handling with response body
                String errorBody = "";
                try {
                    if (response.errorBody() != null) {
                        errorBody = response.errorBody().string();
                    }
                } catch (IOException e) {
                    errorBody = "Unable to read error body";
                }
                throw new RuntimeException("Failed to create link token: " + response.message() + " - " + errorBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating link token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error creating link token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Exchange public token for access token and create bank account
     */
    public List<BankAccount> exchangePublicToken(String publicToken) {
        try {
            // Exchange public token for access token
            ItemPublicTokenExchangeRequest exchangeRequest = new ItemPublicTokenExchangeRequest()
                .publicToken(publicToken);
            
            Response<ItemPublicTokenExchangeResponse> exchangeResponse = 
                plaidClient.itemPublicTokenExchange(exchangeRequest).execute();
            
            if (!exchangeResponse.isSuccessful() || exchangeResponse.body() == null) {
                throw new RuntimeException("Failed to exchange public token: " + exchangeResponse.message());
            }
            
            String accessToken = exchangeResponse.body().getAccessToken();
            String itemId = exchangeResponse.body().getItemId();
            
            // Get account information
            AccountsGetRequest accountsRequest = new AccountsGetRequest()
                .accessToken(accessToken);
            
            Response<AccountsGetResponse> accountsResponse = 
                plaidClient.accountsGet(accountsRequest).execute();
            
            if (!accountsResponse.isSuccessful() || accountsResponse.body() == null) {
                throw new RuntimeException("Failed to get accounts: " + accountsResponse.message());
            }
            
            // Get institution information
            ItemGetRequest itemRequest = new ItemGetRequest()
                .accessToken(accessToken);
            
            Response<ItemGetResponse> itemResponse = 
                plaidClient.itemGet(itemRequest).execute();
            
            String institutionId = null;
            if (itemResponse.isSuccessful() && itemResponse.body() != null) {
                institutionId = itemResponse.body().getItem().getInstitutionId();
            }
            
            String institutionName = "Unknown Bank";
            if (institutionId != null) {
                InstitutionsGetByIdRequest instRequest = new InstitutionsGetByIdRequest()
                    .institutionId(institutionId)
                    .countryCodes(Arrays.asList(CountryCode.US));
                
                Response<InstitutionsGetByIdResponse> instResponse = 
                    plaidClient.institutionsGetById(instRequest).execute();
                
                if (instResponse.isSuccessful() && instResponse.body() != null) {
                    institutionName = instResponse.body().getInstitution().getName();
                }
            }
            
            // Create BankAccount entities
            List<BankAccount> bankAccounts = new ArrayList<>();
            for (AccountBase account : accountsResponse.body().getAccounts()) {
                BankAccount bankAccount = new BankAccount();
                bankAccount.setPlaidAccountId(account.getAccountId());
                bankAccount.setAccessToken(accessToken);
                bankAccount.setPlaidItemId(itemId);
                bankAccount.setAccountName(account.getName());
                bankAccount.setAccountType(account.getType().getValue());
                bankAccount.setInstitutionName(institutionName);
                bankAccount.setMask(account.getMask());
                bankAccount.setCreatedAt(LocalDateTime.now());
                bankAccount.setLastSyncAt(LocalDateTime.now());
                
                BankAccount savedAccount = bankAccountRepository.save(bankAccount);
                bankAccounts.add(savedAccount);
            }
            
            return bankAccounts;
            
        } catch (IOException e) {
            throw new RuntimeException("Error exchanging public token", e);
        }
    }
    
    /**
     * Sync transactions for all active bank accounts
     * Mock implementation - creates sample transactions
     */
    public void syncAllTransactions() {
        System.out.println("=== STARTING TRANSACTION SYNC FOR ALL ACCOUNTS ===");
        List<BankAccount> activeAccounts = bankAccountRepository.findByIsActiveTrue();
        System.out.println("Found " + activeAccounts.size() + " active bank accounts to sync");
        
        int totalSyncedTransactions = 0;
        for (BankAccount account : activeAccounts) {
            int accountTransactions = syncTransactionsForAccount(account);
            totalSyncedTransactions += accountTransactions;
        }
        
        System.out.println("=== SYNC COMPLETE ===");
        System.out.println("Total transactions synced across all accounts: " + totalSyncedTransactions);
        System.out.println("======================");
    }
    
    /**
     * Sync transactions for a specific bank account (Mock Implementation)
     * @return number of transactions synced
     */
    public int syncTransactionsForAccount(BankAccount bankAccount) {
        System.out.println("\n--- SYNCING TRANSACTIONS FOR ACCOUNT ---");
        System.out.println("Account: " + bankAccount.getAccountName() + " (" + bankAccount.getAccountType() + ")");
        System.out.println("Account ID: " + bankAccount.getId());
        
        try {
            // Generate mock transactions for the last 30 days
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            System.out.println("Generating transactions for date range: " + startDate + " to " + endDate);
            
            // Create realistic transaction data with proper categorization
            TransactionTemplate[] transactionTemplates = {
                // INCOME transactions
                new TransactionTemplate("Direct Deposit - Salary", "Transfer > Deposit", 2500.0, 3000.0, "INCOME", "Salary"),
                new TransactionTemplate("Freelance Payment", "Transfer > Deposit", 800.0, 1200.0, "INCOME", "Freelance"),
                new TransactionTemplate("Investment Dividend", "Transfer > Deposit", 100.0, 300.0, "INCOME", "Investment Returns"),
                
                // EXPENSES transactions
                new TransactionTemplate("Walmart", "Shops > Supermarkets and Groceries", -50.0, -150.0, "EXPENSES", "Groceries"),
                new TransactionTemplate("Target", "Shops > Supermarkets and Groceries", -40.0, -120.0, "EXPENSES", "Groceries"),
                new TransactionTemplate("Kroger", "Shops > Supermarkets and Groceries", -60.0, -180.0, "EXPENSES", "Groceries"),
                new TransactionTemplate("Shell Gas Station", "Transportation > Gas Stations", -30.0, -80.0, "EXPENSES", "Gas"),
                new TransactionTemplate("Chevron", "Transportation > Gas Stations", -35.0, -75.0, "EXPENSES", "Gas"),
                new TransactionTemplate("Starbucks", "Food and Drink > Coffee Shops", -5.0, -15.0, "EXPENSES", "Dining Out"),
                new TransactionTemplate("McDonald's", "Food and Drink > Restaurants", -8.0, -25.0, "EXPENSES", "Dining Out"),
                new TransactionTemplate("Restaurant", "Food and Drink > Restaurants", -25.0, -80.0, "EXPENSES", "Dining Out"),
                new TransactionTemplate("Amazon", "Shops > Online Retailers", -20.0, -200.0, "EXPENSES", "Shopping"),
                new TransactionTemplate("Best Buy", "Shops > Electronics", -50.0, -500.0, "EXPENSES", "Shopping"),
                new TransactionTemplate("Movie Theater", "Entertainment > Movies", -12.0, -30.0, "EXPENSES", "Entertainment"),
                
                // BILLS transactions
                new TransactionTemplate("Electric Company", "Bills > Utilities", -80.0, -150.0, "BILLS", "Utilities"),
                new TransactionTemplate("Gas Company", "Bills > Utilities", -60.0, -120.0, "BILLS", "Utilities"),
                new TransactionTemplate("Water Department", "Bills > Utilities", -40.0, -80.0, "BILLS", "Utilities"),
                new TransactionTemplate("Internet Provider", "Bills > Internet", -70.0, -90.0, "BILLS", "Internet"),
                new TransactionTemplate("Verizon", "Bills > Phone", -50.0, -80.0, "BILLS", "Phone"),
                new TransactionTemplate("AT&T", "Bills > Phone", -55.0, -85.0, "BILLS", "Phone"),
                new TransactionTemplate("Netflix", "Entertainment > Streaming", -15.0, -20.0, "BILLS", "Entertainment"),
                new TransactionTemplate("Spotify", "Entertainment > Music", -10.0, -15.0, "BILLS", "Entertainment"),
                new TransactionTemplate("Insurance Payment", "Bills > Insurance", -150.0, -250.0, "BILLS", "Insurance"),
                new TransactionTemplate("Rent Payment", "Bills > Rent", -1200.0, -1800.0, "BILLS", "Rent"),
                
                // SAVINGS transactions
                new TransactionTemplate("Transfer to Savings", "Transfer > Savings", -300.0, -600.0, "SAVINGS", "Emergency Fund"),
                new TransactionTemplate("401k Contribution", "Transfer > Retirement", -400.0, -500.0, "SAVINGS", "Retirement"),
                new TransactionTemplate("Vacation Fund", "Transfer > Savings", -200.0, -400.0, "SAVINGS", "Vacation")
            };
            
            Random random = new Random();
            
            // Generate 15-25 transactions per account
            int numTransactions = 15 + random.nextInt(11); // 15-25 transactions
            System.out.println("Generating " + numTransactions + " mock transactions...");
            
            int syncedCount = 0;
            for (int i = 0; i < numTransactions; i++) {
                String transactionId = "mock_txn_" + bankAccount.getId() + "_" + i + "_" + System.currentTimeMillis();
                
                // Check if transaction already exists
                if (bankTransactionRepository.findByPlaidTransactionId(transactionId).isEmpty()) {
                    TransactionTemplate template = transactionTemplates[random.nextInt(transactionTemplates.length)];
                    
                    BankTransaction bankTransaction = new BankTransaction();
                    bankTransaction.setPlaidTransactionId(transactionId);
                    bankTransaction.setBankAccount(bankAccount);
                    
                    // Generate amount within template range
                    double amount = template.minAmount + (random.nextDouble() * (template.maxAmount - template.minAmount));
                    bankTransaction.setAmount(amount);
                    
                    bankTransaction.setMerchantName(template.merchantName);
                    bankTransaction.setDescription(template.merchantName + " Transaction");
                    
                    // Random date within the last 30 days
                    LocalDate transactionDate = startDate.plusDays(random.nextInt(30));
                    bankTransaction.setTransactionDate(transactionDate);
                    bankTransaction.setAuthorizedDate(transactionDate);
                    
                    bankTransaction.setTransactionType(amount > 0 ? "credit" : "debit");
                    bankTransaction.setPlaidCategory(template.plaidCategory);
                    
                    // Automatically categorize the transaction
                    String[] categorization = automaticallyCategorizeTransaction(
                        template.merchantName, 
                        template.plaidCategory, 
                        amount
                    );
                    bankTransaction.setBudgetCategoryType(categorization[0]);
                    bankTransaction.setBudgetCategory(categorization[1]);
                    
                    bankTransaction.setCreatedAt(LocalDateTime.now());
                    bankTransactionRepository.save(bankTransaction);
                    
                    // Log transaction details
                    System.out.printf("  [%d/%d] %s: $%.2f - %s > %s (Date: %s)%n", 
                        i + 1, numTransactions, 
                        template.merchantName, 
                        amount, 
                        categorization[0], 
                        categorization[1], 
                        transactionDate);
                    
                    syncedCount++;
                }
            }
            
            // Update last sync time
            bankAccount.setLastSyncAt(LocalDateTime.now());
            bankAccountRepository.save(bankAccount);
            
            System.out.println("Successfully synced " + syncedCount + " transactions for " + bankAccount.getAccountName());
            System.out.println("Account last sync updated to: " + LocalDateTime.now());
            System.out.println("--- SYNC COMPLETE FOR ACCOUNT ---\n");
            
            return syncedCount;
            
        } catch (Exception e) {
            System.err.println("ERROR syncing transactions for account: " + bankAccount.getAccountName() + ". Error: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Automatically categorize a transaction based on merchant name, Plaid category, and amount
     * Returns [budgetCategoryType, budgetCategory]
     */
    private String[] automaticallyCategorizeTransaction(String merchantName, String plaidCategory, double amount) {
        String merchant = merchantName.toLowerCase();
        String category = plaidCategory.toLowerCase();
        
        // Income categorization
        if (amount > 0 || category.contains("deposit") || category.contains("payroll") || 
            merchant.contains("salary") || merchant.contains("direct deposit") || 
            merchant.contains("freelance") || merchant.contains("dividend")) {
            
            if (merchant.contains("salary") || merchant.contains("direct deposit") || merchant.contains("payroll")) {
                return new String[]{"INCOME", "Salary"};
            } else if (merchant.contains("freelance") || merchant.contains("consulting")) {
                return new String[]{"INCOME", "Freelance"};
            } else if (merchant.contains("dividend") || merchant.contains("investment")) {
                return new String[]{"INCOME", "Investment Returns"};
            } else {
                return new String[]{"INCOME", "Other Income"};
            }
        }
        
        // Bills categorization (recurring/utility payments)
        if (category.contains("bills") || category.contains("utilities") || 
            merchant.contains("electric") || merchant.contains("gas company") || 
            merchant.contains("water") || merchant.contains("internet") || 
            merchant.contains("phone") || merchant.contains("verizon") || 
            merchant.contains("at&t") || merchant.contains("insurance") || 
            merchant.contains("rent") || merchant.contains("netflix") || 
            merchant.contains("spotify")) {
            
            if (merchant.contains("electric") || merchant.contains("gas company") || 
                merchant.contains("water") || category.contains("utilities")) {
                return new String[]{"BILLS", "Utilities"};
            } else if (merchant.contains("internet") || category.contains("internet")) {
                return new String[]{"BILLS", "Internet"};
            } else if (merchant.contains("phone") || merchant.contains("verizon") || 
                      merchant.contains("at&t") || category.contains("phone")) {
                return new String[]{"BILLS", "Phone"};
            } else if (merchant.contains("netflix") || merchant.contains("spotify") || 
                      merchant.contains("streaming") || category.contains("streaming")) {
                return new String[]{"BILLS", "Entertainment"};
            } else if (merchant.contains("insurance") || category.contains("insurance")) {
                return new String[]{"BILLS", "Insurance"};
            } else if (merchant.contains("rent") || category.contains("rent")) {
                return new String[]{"BILLS", "Rent"};
            } else {
                return new String[]{"BILLS", "Other Bills"};
            }
        }
        
        // Savings categorization
        if (category.contains("transfer") && (category.contains("savings") || category.contains("retirement")) ||
            merchant.contains("savings") || merchant.contains("401k") || merchant.contains("retirement") ||
            merchant.contains("vacation fund")) {
            
            if (merchant.contains("401k") || merchant.contains("retirement") || category.contains("retirement")) {
                return new String[]{"SAVINGS", "Retirement"};
            } else if (merchant.contains("vacation")) {
                return new String[]{"SAVINGS", "Vacation"};
            } else {
                return new String[]{"SAVINGS", "Emergency Fund"};
            }
        }
        
        // Expenses categorization (everything else)
        if (category.contains("food") || category.contains("restaurants") || 
            merchant.contains("starbucks") || merchant.contains("mcdonald") || 
            merchant.contains("restaurant") || merchant.contains("coffee")) {
            return new String[]{"EXPENSES", "Dining Out"};
        } else if (category.contains("supermarkets") || category.contains("groceries") || 
                  merchant.contains("walmart") || merchant.contains("target") || 
                  merchant.contains("kroger") || merchant.contains("grocery")) {
            return new String[]{"EXPENSES", "Groceries"};
        } else if (category.contains("gas") || category.contains("transportation") || 
                  merchant.contains("shell") || merchant.contains("chevron") || 
                  merchant.contains("gas station")) {
            return new String[]{"EXPENSES", "Gas"};
        } else if (category.contains("entertainment") || category.contains("movies") || 
                  merchant.contains("movie") || merchant.contains("theater")) {
            return new String[]{"EXPENSES", "Entertainment"};
        } else if (category.contains("shops") || category.contains("retail") || 
                  merchant.contains("amazon") || merchant.contains("best buy") || 
                  merchant.contains("shopping")) {
            return new String[]{"EXPENSES", "Shopping"};
        } else {
            return new String[]{"EXPENSES", "Other Expenses"};
        }
    }
    
    /**
     * Remove/deactivate a bank account
     */
    public void removeBankAccount(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Bank account not found"));
        
        try {
            // Remove the item from Plaid
            ItemRemoveRequest request = new ItemRemoveRequest()
                .accessToken(account.getAccessToken());
            
            Response<ItemRemoveResponse> response = plaidClient.itemRemove(request).execute();
            
            if (!response.isSuccessful()) {
                // Log the error but still deactivate locally
                System.err.println("Failed to remove item from Plaid: " + response.message());
            }
            
        } catch (IOException e) {
            // Log the error but still deactivate locally
            System.err.println("Error removing item from Plaid: " + e.getMessage());
        }
        
        // Deactivate the account locally
        account.setIsActive(false);
        bankAccountRepository.save(account);
    }
}
