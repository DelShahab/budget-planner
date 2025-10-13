package com.budgetplanner.budget.controller;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.service.PlaidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = "*")
public class BankIntegrationController {
    
    private final PlaidService plaidService;
    
    @Autowired
    public BankIntegrationController(PlaidService plaidService) {
        this.plaidService = plaidService;
    }
    
    /**
     * Create a link token for Plaid Link initialization
     */
    @PostMapping("/link-token")
    public ResponseEntity<?> createLinkToken(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null || userId.isEmpty()) {
                userId = "default_user"; // In production, get from authenticated user
            }
            
            String linkToken = plaidService.createLinkToken(userId);
            return ResponseEntity.ok(Map.of("linkToken", linkToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create link token: " + e.getMessage()));
        }
    }
    
    /**
     * Exchange public token for access token and link bank accounts
     */
    @PostMapping("/exchange-token")
    public ResponseEntity<?> exchangePublicToken(@RequestBody Map<String, String> request) {
        try {
            String publicToken = request.get("publicToken");
            if (publicToken == null || publicToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Public token is required"));
            }
            
            List<BankAccount> accounts = plaidService.exchangePublicToken(publicToken);
            return ResponseEntity.ok(Map.of(
                    "message", "Bank accounts linked successfully",
                    "accounts", accounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to link bank accounts: " + e.getMessage()));
        }
    }
    
    /**
     * Sync transactions for all accounts
     */
    @PostMapping("/sync-transactions")
    public ResponseEntity<?> syncTransactions() {
        try {
            plaidService.syncAllTransactions();
            return ResponseEntity.ok(Map.of("message", "Transactions synced successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to sync transactions: " + e.getMessage()));
        }
    }
    
    /**
     * Remove a bank account
     */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<?> removeBankAccount(@PathVariable Long accountId) {
        try {
            plaidService.removeBankAccount(accountId);
            return ResponseEntity.ok(Map.of("message", "Bank account removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to remove bank account: " + e.getMessage()));
        }
    }
}
