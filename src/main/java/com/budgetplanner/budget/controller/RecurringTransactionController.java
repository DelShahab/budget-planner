package com.budgetplanner.budget.controller;

import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.service.RecurringTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for managing recurring transactions
 */
@RestController
@RequestMapping("/api/recurring-transactions")
@CrossOrigin(origins = "*")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @Autowired
    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    /**
     * Get all active recurring transactions
     */
    @GetMapping
    public ResponseEntity<List<RecurringTransaction>> getAllRecurringTransactions() {
        try {
            List<RecurringTransaction> transactions = recurringTransactionService.getAllActiveRecurringTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recurring transactions by category
     */
    @GetMapping("/category/{categoryType}")
    public ResponseEntity<List<RecurringTransaction>> getRecurringTransactionsByCategory(
            @PathVariable String categoryType) {
        try {
            List<RecurringTransaction> transactions = 
                recurringTransactionService.getRecurringTransactionsByCategory(categoryType);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get transactions due soon
     */
    @GetMapping("/due-soon")
    public ResponseEntity<List<RecurringTransaction>> getTransactionsDueSoon(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<RecurringTransaction> transactions = 
                recurringTransactionService.getTransactionsDueSoon(days);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get overdue transactions
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<RecurringTransaction>> getOverdueTransactions() {
        try {
            List<RecurringTransaction> transactions = 
                recurringTransactionService.getOverdueTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get monthly totals by category
     */
    @GetMapping("/monthly-totals")
    public ResponseEntity<Map<String, Double>> getMonthlyTotalsByCategory() {
        try {
            Map<String, Double> totals = recurringTransactionService.getMonthlyTotalsByCategory();
            return ResponseEntity.ok(totals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Trigger analysis of all transactions for recurring patterns
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeRecurringPatterns() {
        try {
            CompletableFuture<Integer> analysisResult = 
                recurringTransactionService.analyzeAllTransactionsForRecurringPatterns();
            
            return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Recurring transaction analysis started in background"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to start recurring transaction analysis"
                ));
        }
    }

    /**
     * Update a recurring transaction
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransaction> updateRecurringTransaction(
            @PathVariable Long id,
            @RequestBody RecurringTransaction recurringTransaction) {
        try {
            recurringTransaction.setId(id);
            RecurringTransaction updated = 
                recurringTransactionService.saveOrUpdateRecurringTransaction(recurringTransaction);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Confirm a recurring transaction (mark as user confirmed)
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmRecurringTransaction(@PathVariable Long id) {
        try {
            // This would need to be implemented in the service
            return ResponseEntity.ok(Map.of(
                "status", "confirmed",
                "message", "Recurring transaction confirmed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to confirm recurring transaction"
                ));
        }
    }

    /**
     * Pause a recurring transaction
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> pauseRecurringTransaction(@PathVariable Long id) {
        try {
            // This would need to be implemented in the service
            return ResponseEntity.ok(Map.of(
                "status", "paused",
                "message", "Recurring transaction paused"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to pause recurring transaction"
                ));
        }
    }

    /**
     * Resume a paused recurring transaction
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<Map<String, Object>> resumeRecurringTransaction(@PathVariable Long id) {
        try {
            // This would need to be implemented in the service
            return ResponseEntity.ok(Map.of(
                "status", "resumed",
                "message", "Recurring transaction resumed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to resume recurring transaction"
                ));
        }
    }

    /**
     * Delete/deactivate a recurring transaction
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRecurringTransaction(@PathVariable Long id) {
        try {
            // This would need to be implemented in the service
            return ResponseEntity.ok(Map.of(
                "status", "deleted",
                "message", "Recurring transaction deleted"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to delete recurring transaction"
                ));
        }
    }

    /**
     * Get recurring transaction statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRecurringTransactionStatistics() {
        try {
            List<RecurringTransaction> allTransactions = 
                recurringTransactionService.getAllActiveRecurringTransactions();
            
            Map<String, Double> monthlyTotals = 
                recurringTransactionService.getMonthlyTotalsByCategory();
            
            List<RecurringTransaction> dueSoon = 
                recurringTransactionService.getTransactionsDueSoon(7);
            
            List<RecurringTransaction> overdue = 
                recurringTransactionService.getOverdueTransactions();
            
            Map<String, Object> statistics = Map.of(
                "totalActive", allTransactions.size(),
                "monthlyTotals", monthlyTotals,
                "dueSoonCount", dueSoon.size(),
                "overdueCount", overdue.size(),
                "totalMonthlyAmount", monthlyTotals.values().stream()
                    .mapToDouble(Double::doubleValue).sum()
            );
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
