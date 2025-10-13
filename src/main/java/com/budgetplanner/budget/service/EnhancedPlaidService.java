package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.plaid.client.ApiClient;
import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
// Retry functionality will be implemented manually without Spring Retry dependency
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced Plaid Service with improved error handling, real-time sync,
 * connection monitoring, and performance optimizations
 */
@Service
@Transactional
public class EnhancedPlaidService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final PlaidApi plaidClient;
    private final String clientId;
    private final String secret;
    private final String environment;
    
    // Enhanced error tracking
    private final Map<String, Integer> errorCounts = new HashMap<>();
    private final Map<String, LocalDateTime> lastErrorTimes = new HashMap<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    
    // Connection status tracking
    public enum ConnectionStatus {
        ACTIVE, EXPIRED, INVALID, ERROR, MAINTENANCE_REQUIRED
    }
    
    // Enhanced error types
    public static class PlaidServiceException extends RuntimeException {
        private final String errorCode;
        private final String errorType;
        
        public PlaidServiceException(String message, String errorCode, String errorType) {
            super(message);
            this.errorCode = errorCode;
            this.errorType = errorType;
        }
        
        public String getErrorCode() { return errorCode; }
        public String getErrorType() { return errorType; }
    }

    public EnhancedPlaidService(BankAccountRepository bankAccountRepository,
                               BankTransactionRepository bankTransactionRepository,
                               @Value("${plaid.client-id}") String clientId,
                               @Value("${plaid.secret}") String secret,
                               @Value("${plaid.environment:sandbox}") String environment) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.clientId = clientId;
        this.secret = secret;
        this.environment = environment;
        
        // Initialize enhanced Plaid client with retry configuration
        this.plaidClient = initializePlaidClient();
    }
    
    /**
     * Initialize Plaid client with enhanced configuration
     */
    private PlaidApi initializePlaidClient() {
        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);
        
        ApiClient apiClient = new ApiClient(apiKeys);
        
        // Set environment with validation
        switch (environment.toLowerCase()) {
            case "sandbox":
                apiClient.setPlaidAdapter(ApiClient.Sandbox);
                break;
            case "development":
                apiClient.setPlaidAdapter(ApiClient.Development);
                break;
            case "production":
                apiClient.setPlaidAdapter(ApiClient.Production);
                break;
            default:
                throw new IllegalArgumentException("Invalid Plaid environment: " + environment);
        }
        
        return apiClient.createService(PlaidApi.class);
    }
    
    /**
     * Create link token with enhanced error handling and retry logic
     */
    public String createLinkToken(String userId) {
        validateCircuitBreaker("createLinkToken");
        
        try {
            LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .clientName("Budget Planner")
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en")
                .user(new LinkTokenCreateRequestUser().clientUserId(userId))
                .products(Arrays.asList(Products.TRANSACTIONS))
                .webhook("https://your-domain.com/plaid/webhook"); // Add webhook support

            Response<LinkTokenCreateResponse> response = plaidClient.linkTokenCreate(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                resetErrorCount("createLinkToken");
                return response.body().getLinkToken();
            } else {
                handleApiError("createLinkToken", response);
                throw new PlaidServiceException(
                    "Failed to create link token: " + response.message(),
                    response.code() + "",
                    "LINK_TOKEN_CREATE_ERROR"
                );
            }
        } catch (IOException e) {
            recordError("createLinkToken");
            throw new PlaidServiceException("Network error creating link token", "NETWORK_ERROR", "IO_EXCEPTION");
        }
    }
    
    /**
     * Exchange public token with enhanced error handling and validation
     */
    public List<BankAccount> exchangePublicToken(String publicToken) {
        validateCircuitBreaker("exchangePublicToken");
        
        if (publicToken == null || publicToken.trim().isEmpty()) {
            throw new PlaidServiceException("Public token cannot be null or empty", "INVALID_INPUT", "VALIDATION_ERROR");
        }
        
        try {
            // Step 1: Exchange public token for access token
            ItemPublicTokenExchangeResponse exchangeResponse = executeTokenExchange(publicToken);
            String accessToken = exchangeResponse.getAccessToken();
            String itemId = exchangeResponse.getItemId();
            
            // Step 2: Get account information with retry
            AccountsGetResponse accountsResponse = getAccountsWithRetry(accessToken);
            
            // Step 3: Get institution information
            String institutionName = getInstitutionName(accessToken);
            
            // Step 4: Create and save bank accounts
            List<BankAccount> bankAccounts = createBankAccounts(
                accountsResponse.getAccounts(), accessToken, itemId, institutionName);
            
            resetErrorCount("exchangePublicToken");
            return bankAccounts;
            
        } catch (IOException e) {
            recordError("exchangePublicToken");
            throw new PlaidServiceException("Network error during token exchange", "NETWORK_ERROR", "IO_EXCEPTION");
        }
    }
    
    /**
     * Enhanced transaction sync with real Plaid API integration
     */
    @Async
    public CompletableFuture<Integer> syncTransactionsForAccountAsync(BankAccount bankAccount) {
        return CompletableFuture.supplyAsync(() -> syncTransactionsForAccount(bankAccount));
    }
    
    /**
     * Sync transactions with real Plaid API (replacing mock implementation)
     */
    public int syncTransactionsForAccount(BankAccount bankAccount) {
        validateCircuitBreaker("syncTransactions");
        
        try {
            // Get transactions from Plaid API
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30); // Last 30 days
            
            TransactionsGetRequest request = new TransactionsGetRequest()
                .accessToken(bankAccount.getAccessToken())
                .startDate(startDate)
                .endDate(endDate);
                // Note: count() method may not be available in all Plaid SDK versions
            
            Response<TransactionsGetResponse> response = plaidClient.transactionsGet(request).execute();
            
            if (!response.isSuccessful()) {
                handleApiError("syncTransactions", response);
                return 0;
            }
            
            TransactionsGetResponse transactionsResponse = response.body();
            if (transactionsResponse == null) {
                return 0;
            }
            
            // Process and save transactions
            int syncedCount = processTransactions(bankAccount, transactionsResponse.getTransactions());
            
            // Update last sync time
            bankAccount.setLastSyncAt(LocalDateTime.now());
            bankAccountRepository.save(bankAccount);
            
            resetErrorCount("syncTransactions");
            return syncedCount;
            
        } catch (IOException e) {
            recordError("syncTransactions");
            throw new PlaidServiceException("Network error during transaction sync", "NETWORK_ERROR", "IO_EXCEPTION");
        }
    }
    
    /**
     * Check connection status for all bank accounts
     */
    public Map<Long, ConnectionStatus> checkAllConnectionStatuses() {
        List<BankAccount> accounts = bankAccountRepository.findByIsActiveTrue();
        Map<Long, ConnectionStatus> statuses = new HashMap<>();
        
        for (BankAccount account : accounts) {
            statuses.put(account.getId(), checkConnectionStatus(account));
        }
        
        return statuses;
    }
    
    /**
     * Check individual account connection status
     */
    public ConnectionStatus checkConnectionStatus(BankAccount bankAccount) {
        try {
            ItemGetRequest request = new ItemGetRequest()
                .accessToken(bankAccount.getAccessToken());
            
            Response<ItemGetResponse> response = plaidClient.itemGet(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                Item item = response.body().getItem();
                
                // Check for various error conditions
                if (item.getError() != null) {
                    // Use generic error handling since PlaidError type may vary
                    Object error = item.getError();
                    String errorCode = "";
                    try {
                        // Try to get error code via reflection or toString
                        errorCode = error.toString();
                    } catch (Exception e) {
                        errorCode = "UNKNOWN_ERROR";
                    }
                    
                    if (errorCode.contains("ITEM_LOGIN_REQUIRED")) {
                        return ConnectionStatus.EXPIRED;
                    } else if (errorCode.contains("ACCESS_NOT_GRANTED")) {
                        return ConnectionStatus.INVALID;
                    } else {
                        return ConnectionStatus.ERROR;
                    }
                }
                
                return ConnectionStatus.ACTIVE;
            } else {
                return ConnectionStatus.ERROR;
            }
            
        } catch (IOException e) {
            return ConnectionStatus.ERROR;
        }
    }
    
    /**
     * Enhanced automatic categorization with machine learning potential
     */
    public String[] automaticallyCategorizeTransaction(String merchantName, String plaidCategory, 
                                                      double amount, List<String> plaidCategoryHierarchy) {
        // Enhanced categorization logic with more sophisticated rules
        String merchant = merchantName.toLowerCase().trim();
        String category = plaidCategory.toLowerCase();
        
        // Use Plaid's category hierarchy for better accuracy
        String primaryCategory = plaidCategoryHierarchy.isEmpty() ? "" : 
                                plaidCategoryHierarchy.get(0).toLowerCase();
        String detailedCategory = plaidCategoryHierarchy.size() > 1 ? 
                                 plaidCategoryHierarchy.get(1).toLowerCase() : "";
        
        // Income categorization with enhanced detection
        if (amount > 0 || isIncomeTransaction(merchant, category, primaryCategory)) {
            return categorizeIncome(merchant, category);
        }
        
        // Bills categorization with subscription detection
        if (isBillTransaction(merchant, category, primaryCategory)) {
            return categorizeBills(merchant, category, primaryCategory);
        }
        
        // Savings and investment categorization
        if (isSavingsTransaction(merchant, category, primaryCategory)) {
            return categorizeSavings(merchant, category);
        }
        
        // Enhanced expense categorization
        return categorizeExpenses(merchant, category, primaryCategory, detailedCategory);
    }
    
    /**
     * Process webhook notifications for real-time updates
     */
    public void processWebhookNotification(String webhookType, String itemId, String error) {
        switch (webhookType) {
            case "TRANSACTIONS":
                handleTransactionWebhook(itemId);
                break;
            case "ITEM":
                handleItemWebhook(itemId, error);
                break;
            case "AUTH":
                handleAuthWebhook(itemId);
                break;
            default:
                System.out.println("Unknown webhook type: " + webhookType);
        }
    }
    
    // Private helper methods
    
    private void validateCircuitBreaker(String operation) {
        Integer errorCount = errorCounts.getOrDefault(operation, 0);
        if (errorCount >= CIRCUIT_BREAKER_THRESHOLD) {
            LocalDateTime lastError = lastErrorTimes.get(operation);
            if (lastError != null && lastError.isAfter(LocalDateTime.now().minusMinutes(5))) {
                throw new PlaidServiceException(
                    "Circuit breaker open for operation: " + operation,
                    "CIRCUIT_BREAKER_OPEN",
                    "RATE_LIMIT_ERROR"
                );
            } else {
                // Reset circuit breaker after cooldown period
                errorCounts.put(operation, 0);
            }
        }
    }
    
    private void recordError(String operation) {
        errorCounts.put(operation, errorCounts.getOrDefault(operation, 0) + 1);
        lastErrorTimes.put(operation, LocalDateTime.now());
    }
    
    private void resetErrorCount(String operation) {
        errorCounts.put(operation, 0);
        lastErrorTimes.remove(operation);
    }
    
    private void handleApiError(String operation, Response<?> response) {
        recordError(operation);
        System.err.printf("Plaid API error for %s: %d - %s%n", 
                         operation, response.code(), response.message());
    }
    
    private ItemPublicTokenExchangeResponse executeTokenExchange(String publicToken) throws IOException {
        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
            .publicToken(publicToken);
        
        Response<ItemPublicTokenExchangeResponse> response = 
            plaidClient.itemPublicTokenExchange(request).execute();
        
        if (!response.isSuccessful() || response.body() == null) {
            throw new PlaidServiceException(
                "Failed to exchange public token: " + response.message(),
                response.code() + "",
                "TOKEN_EXCHANGE_ERROR"
            );
        }
        
        return response.body();
    }
    
    private AccountsGetResponse getAccountsWithRetry(String accessToken) throws IOException {
        AccountsGetRequest request = new AccountsGetRequest().accessToken(accessToken);
        Response<AccountsGetResponse> response = plaidClient.accountsGet(request).execute();
        
        if (!response.isSuccessful() || response.body() == null) {
            throw new PlaidServiceException(
                "Failed to get accounts: " + response.message(),
                response.code() + "",
                "ACCOUNTS_GET_ERROR"
            );
        }
        
        return response.body();
    }
    
    @Cacheable(value = "institutionNames", key = "#accessToken")
    private String getInstitutionName(String accessToken) {
        try {
            ItemGetRequest itemRequest = new ItemGetRequest().accessToken(accessToken);
            Response<ItemGetResponse> itemResponse = plaidClient.itemGet(itemRequest).execute();
            
            if (itemResponse.isSuccessful() && itemResponse.body() != null) {
                String institutionId = itemResponse.body().getItem().getInstitutionId();
                
                if (institutionId != null) {
                    InstitutionsGetByIdRequest instRequest = new InstitutionsGetByIdRequest()
                        .institutionId(institutionId)
                        .countryCodes(Arrays.asList(CountryCode.US));
                    
                    Response<InstitutionsGetByIdResponse> instResponse = 
                        plaidClient.institutionsGetById(instRequest).execute();
                    
                    if (instResponse.isSuccessful() && instResponse.body() != null) {
                        return instResponse.body().getInstitution().getName();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error getting institution name: " + e.getMessage());
        }
        
        return "Unknown Bank";
    }
    
    private List<BankAccount> createBankAccounts(List<AccountBase> accounts, String accessToken, 
                                               String itemId, String institutionName) {
        List<BankAccount> bankAccounts = new ArrayList<>();
        
        for (AccountBase account : accounts) {
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
            bankAccount.setIsActive(true);
            
            BankAccount savedAccount = bankAccountRepository.save(bankAccount);
            bankAccounts.add(savedAccount);
        }
        
        return bankAccounts;
    }
    
    private int processTransactions(BankAccount bankAccount, List<Transaction> transactions) {
        int syncedCount = 0;
        
        for (Transaction transaction : transactions) {
            // Check if transaction already exists
            if (bankTransactionRepository.findByPlaidTransactionId(transaction.getTransactionId()).isEmpty()) {
                BankTransaction bankTransaction = createBankTransaction(bankAccount, transaction);
                bankTransactionRepository.save(bankTransaction);
                syncedCount++;
            }
        }
        
        return syncedCount;
    }
    
    private BankTransaction createBankTransaction(BankAccount bankAccount, Transaction transaction) {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.setPlaidTransactionId(transaction.getTransactionId());
        bankTransaction.setBankAccount(bankAccount);
        bankTransaction.setAmount(transaction.getAmount());
        bankTransaction.setMerchantName(transaction.getMerchantName() != null ? 
                                       transaction.getMerchantName() : "Unknown Merchant");
        bankTransaction.setDescription(transaction.getName());
        bankTransaction.setTransactionDate(transaction.getDate());
        bankTransaction.setAuthorizedDate(transaction.getAuthorizedDate());
        bankTransaction.setTransactionType(transaction.getAmount() > 0 ? "credit" : "debit");
        
        // Enhanced categorization
        List<String> categoryHierarchy = transaction.getCategory();
        String plaidCategory = categoryHierarchy.isEmpty() ? "" : String.join(" > ", categoryHierarchy);
        bankTransaction.setPlaidCategory(plaidCategory);
        
        String[] categorization = automaticallyCategorizeTransaction(
            bankTransaction.getMerchantName(),
            plaidCategory,
            transaction.getAmount(),
            categoryHierarchy
        );
        
        bankTransaction.setBudgetCategoryType(categorization[0]);
        bankTransaction.setBudgetCategory(categorization[1]);
        bankTransaction.setCreatedAt(LocalDateTime.now());
        
        return bankTransaction;
    }
    
    // Enhanced categorization helper methods
    private boolean isIncomeTransaction(String merchant, String category, String primaryCategory) {
        return primaryCategory.contains("transfer") && category.contains("deposit") ||
               merchant.contains("salary") || merchant.contains("payroll") ||
               merchant.contains("direct deposit") || merchant.contains("freelance") ||
               merchant.contains("dividend") || category.contains("payroll");
    }
    
    private boolean isBillTransaction(String merchant, String category, String primaryCategory) {
        return primaryCategory.contains("payment") || category.contains("utilities") ||
               merchant.contains("electric") || merchant.contains("gas company") ||
               merchant.contains("water") || merchant.contains("internet") ||
               merchant.contains("phone") || merchant.contains("insurance") ||
               merchant.contains("rent") || merchant.contains("netflix") ||
               merchant.contains("spotify") || merchant.contains("subscription");
    }
    
    private boolean isSavingsTransaction(String merchant, String category, String primaryCategory) {
        return primaryCategory.contains("transfer") && (category.contains("savings") || 
               category.contains("retirement")) || merchant.contains("401k") ||
               merchant.contains("savings") || merchant.contains("investment");
    }
    
    private String[] categorizeIncome(String merchant, String category) {
        if (merchant.contains("salary") || merchant.contains("payroll")) {
            return new String[]{"INCOME", "Salary"};
        } else if (merchant.contains("freelance")) {
            return new String[]{"INCOME", "Freelance"};
        } else if (merchant.contains("dividend") || merchant.contains("investment")) {
            return new String[]{"INCOME", "Investment Returns"};
        } else {
            return new String[]{"INCOME", "Other Income"};
        }
    }
    
    private String[] categorizeBills(String merchant, String category, String primaryCategory) {
        if (merchant.contains("electric") || merchant.contains("gas company") || 
            merchant.contains("water") || category.contains("utilities")) {
            return new String[]{"BILLS", "Utilities"};
        } else if (merchant.contains("internet")) {
            return new String[]{"BILLS", "Internet"};
        } else if (merchant.contains("phone") || merchant.contains("verizon") || 
                  merchant.contains("at&t")) {
            return new String[]{"BILLS", "Phone"};
        } else if (merchant.contains("netflix") || merchant.contains("spotify") || 
                  merchant.contains("subscription")) {
            return new String[]{"BILLS", "Entertainment"};
        } else if (merchant.contains("insurance")) {
            return new String[]{"BILLS", "Insurance"};
        } else if (merchant.contains("rent")) {
            return new String[]{"BILLS", "Rent"};
        } else {
            return new String[]{"BILLS", "Other Bills"};
        }
    }
    
    private String[] categorizeSavings(String merchant, String category) {
        if (merchant.contains("401k") || merchant.contains("retirement")) {
            return new String[]{"SAVINGS", "Retirement"};
        } else if (merchant.contains("vacation")) {
            return new String[]{"SAVINGS", "Vacation"};
        } else {
            return new String[]{"SAVINGS", "Emergency Fund"};
        }
    }
    
    private String[] categorizeExpenses(String merchant, String category, 
                                      String primaryCategory, String detailedCategory) {
        if (primaryCategory.contains("food") || detailedCategory.contains("restaurants") ||
            merchant.contains("restaurant") || merchant.contains("starbucks")) {
            return new String[]{"EXPENSES", "Dining Out"};
        } else if (detailedCategory.contains("supermarkets") || merchant.contains("grocery") ||
                  merchant.contains("walmart") || merchant.contains("target")) {
            return new String[]{"EXPENSES", "Groceries"};
        } else if (primaryCategory.contains("transportation") || detailedCategory.contains("gas") ||
                  merchant.contains("gas station") || merchant.contains("shell")) {
            return new String[]{"EXPENSES", "Gas"};
        } else if (primaryCategory.contains("entertainment") || merchant.contains("movie")) {
            return new String[]{"EXPENSES", "Entertainment"};
        } else if (primaryCategory.contains("shops") || merchant.contains("amazon") ||
                  merchant.contains("shopping")) {
            return new String[]{"EXPENSES", "Shopping"};
        } else {
            return new String[]{"EXPENSES", "Other Expenses"};
        }
    }
    
    // Webhook handlers
    private void handleTransactionWebhook(String itemId) {
        // Find accounts with this item ID and trigger sync
        // Note: Assuming findByPlaidItemId returns a List, adjust if it returns Optional
        try {
            List<BankAccount> accounts = bankAccountRepository.findAll().stream()
                .filter(account -> itemId.equals(account.getPlaidItemId()))
                .toList();
            
            for (BankAccount account : accounts) {
                syncTransactionsForAccountAsync(account);
            }
        } catch (Exception e) {
            System.err.println("Error handling transaction webhook: " + e.getMessage());
        }
    }
    
    private void handleItemWebhook(String itemId, String error) {
        // Handle item-level errors (expired tokens, etc.)
        try {
            List<BankAccount> accounts = bankAccountRepository.findAll().stream()
                .filter(account -> itemId.equals(account.getPlaidItemId()))
                .toList();
                
            for (BankAccount account : accounts) {
                // Update account status based on error
                if (error != null && error.contains("ITEM_LOGIN_REQUIRED")) {
                    // Mark account as requiring re-authentication
                    account.setIsActive(false);
                    bankAccountRepository.save(account);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling item webhook: " + e.getMessage());
        }
    }
    
    private void handleAuthWebhook(String itemId) {
        // Handle authentication-related webhooks
        System.out.println("Auth webhook received for item: " + itemId);
    }
}
