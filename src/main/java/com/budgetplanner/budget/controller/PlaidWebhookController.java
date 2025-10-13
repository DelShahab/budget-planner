package com.budgetplanner.budget.controller;

import com.budgetplanner.budget.service.SimplifiedEnhancedPlaidService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Controller to handle Plaid webhook notifications for real-time updates
 * Supports transaction updates, item status changes, and error notifications
 */
@RestController
@RequestMapping("/api/plaid/webhook")
public class PlaidWebhookController {

    private final SimplifiedEnhancedPlaidService enhancedPlaidService;
    private final ObjectMapper objectMapper;

    public PlaidWebhookController(SimplifiedEnhancedPlaidService enhancedPlaidService) {
        this.enhancedPlaidService = enhancedPlaidService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Handle all Plaid webhook notifications
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request) {
        
        try {
            // Parse webhook payload
            JsonNode webhookData = objectMapper.readTree(payload);
            
            // Extract common fields
            String webhookType = webhookData.get("webhook_type").asText();
            String webhookCode = webhookData.get("webhook_code").asText();
            String itemId = webhookData.has("item_id") ? 
                           webhookData.get("item_id").asText() : null;
            
            // Log webhook receipt
            System.out.printf("Received Plaid webhook: %s/%s for item: %s%n", 
                             webhookType, webhookCode, itemId);
            
            // Route to appropriate handler based on webhook type
            switch (webhookType.toUpperCase()) {
                case "TRANSACTIONS":
                    handleTransactionsWebhook(webhookCode, itemId, webhookData);
                    break;
                    
                case "ITEM":
                    handleItemWebhook(webhookCode, itemId, webhookData);
                    break;
                    
                case "AUTH":
                    handleAuthWebhook(webhookCode, itemId, webhookData);
                    break;
                    
                case "IDENTITY":
                    handleIdentityWebhook(webhookCode, itemId, webhookData);
                    break;
                    
                case "ASSETS":
                    handleAssetsWebhook(webhookCode, itemId, webhookData);
                    break;
                    
                default:
                    System.out.printf("Unknown webhook type received: %s%n", webhookType);
                    break;
            }
            
            // Return success response
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Webhook processed successfully",
                "webhook_type", webhookType,
                "webhook_code", webhookCode
            ));
            
        } catch (IOException e) {
            System.err.printf("Error parsing webhook payload: %s%n", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "message", "Invalid webhook payload"
                ));
                
        } catch (Exception e) {
            System.err.printf("Error processing webhook: %s%n", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Internal server error processing webhook"
                ));
        }
    }

    /**
     * Handle transaction-related webhooks
     */
    private void handleTransactionsWebhook(String webhookCode, String itemId, JsonNode data) {
        switch (webhookCode.toUpperCase()) {
            case "INITIAL_UPDATE":
                System.out.printf("Initial transaction data available for item: %s%n", itemId);
                enhancedPlaidService.processWebhookNotification("TRANSACTIONS", itemId, null);
                break;
                
            case "HISTORICAL_UPDATE":
                System.out.printf("Historical transaction data available for item: %s%n", itemId);
                enhancedPlaidService.processWebhookNotification("TRANSACTIONS", itemId, null);
                break;
                
            case "DEFAULT_UPDATE":
                System.out.printf("New transaction data available for item: %s%n", itemId);
                enhancedPlaidService.processWebhookNotification("TRANSACTIONS", itemId, null);
                break;
                
            case "TRANSACTIONS_REMOVED":
                int removedCount = data.has("removed_transactions") ? 
                                  data.get("removed_transactions").size() : 0;
                System.out.printf("Transactions removed for item %s: %d transactions%n", 
                                 itemId, removedCount);
                // Handle transaction removal logic here
                break;
                
            default:
                System.out.printf("Unknown transactions webhook code: %s%n", webhookCode);
                break;
        }
    }

    /**
     * Handle item-related webhooks (connection status, errors, etc.)
     */
    private void handleItemWebhook(String webhookCode, String itemId, JsonNode data) {
        switch (webhookCode.toUpperCase()) {
            case "ERROR":
                JsonNode error = data.get("error");
                String errorCode = error != null ? error.get("error_code").asText() : "UNKNOWN";
                String errorMessage = error != null ? error.get("error_message").asText() : "Unknown error";
                
                System.out.printf("Item error for %s: %s - %s%n", itemId, errorCode, errorMessage);
                enhancedPlaidService.processWebhookNotification("ITEM", itemId, errorCode);
                break;
                
            case "PENDING_EXPIRATION":
                System.out.printf("Item %s access will expire soon%n", itemId);
                // Notify user to re-authenticate
                break;
                
            case "USER_PERMISSION_REVOKED":
                System.out.printf("User revoked permissions for item %s%n", itemId);
                // Deactivate the item
                break;
                
            case "WEBHOOK_UPDATE_ACKNOWLEDGED":
                System.out.printf("Webhook update acknowledged for item %s%n", itemId);
                break;
                
            default:
                System.out.printf("Unknown item webhook code: %s%n", webhookCode);
                break;
        }
    }

    /**
     * Handle auth-related webhooks
     */
    private void handleAuthWebhook(String webhookCode, String itemId, JsonNode data) {
        switch (webhookCode.toUpperCase()) {
            case "AUTOMATICALLY_VERIFIED":
                System.out.printf("Auth automatically verified for item %s%n", itemId);
                break;
                
            case "VERIFICATION_EXPIRED":
                System.out.printf("Auth verification expired for item %s%n", itemId);
                break;
                
            default:
                System.out.printf("Unknown auth webhook code: %s%n", webhookCode);
                break;
        }
    }

    /**
     * Handle identity-related webhooks
     */
    private void handleIdentityWebhook(String webhookCode, String itemId, JsonNode data) {
        switch (webhookCode.toUpperCase()) {
            case "DEFAULT_UPDATE":
                System.out.printf("Identity data updated for item %s%n", itemId);
                break;
                
            default:
                System.out.printf("Unknown identity webhook code: %s%n", webhookCode);
                break;
        }
    }

    /**
     * Handle assets-related webhooks
     */
    private void handleAssetsWebhook(String webhookCode, String itemId, JsonNode data) {
        switch (webhookCode.toUpperCase()) {
            case "PRODUCT_READY":
                System.out.printf("Assets product ready for item %s%n", itemId);
                break;
                
            case "ERROR":
                JsonNode error = data.get("error");
                String errorCode = error != null ? error.get("error_code").asText() : "UNKNOWN";
                System.out.printf("Assets error for item %s: %s%n", itemId, errorCode);
                break;
                
            default:
                System.out.printf("Unknown assets webhook code: %s%n", webhookCode);
                break;
        }
    }

    /**
     * Health check endpoint for webhook URL verification
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "plaid-webhook",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
}
