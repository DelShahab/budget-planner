package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    
    Optional<BankTransaction> findByPlaidTransactionId(String plaidTransactionId);
    
    List<BankTransaction> findByBankAccount(BankAccount bankAccount);
    
    List<BankTransaction> findByBankAccountAndTransactionDateBetween(
            BankAccount bankAccount, LocalDate startDate, LocalDate endDate);
    
    List<BankTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<BankTransaction> findByIsProcessedFalse();
    
    List<BankTransaction> findByBudgetCategoryType(String budgetCategoryType);
    
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.transactionDate >= :startDate " +
           "AND bt.transactionDate <= :endDate AND bt.budgetCategoryType = :categoryType")
    List<BankTransaction> findTransactionsByDateRangeAndCategoryType(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryType") String categoryType);
    
    @Query("SELECT SUM(bt.amount) FROM BankTransaction bt WHERE bt.transactionDate >= :startDate " +
           "AND bt.transactionDate <= :endDate AND bt.budgetCategoryType = :categoryType")
    Double sumAmountByDateRangeAndCategoryType(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryType") String categoryType);
    
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.budgetCategory IS NULL OR bt.budgetCategoryType IS NULL")
    List<BankTransaction> findUncategorizedTransactions();
    
    boolean existsByPlaidTransactionId(String plaidTransactionId);
    
    List<BankTransaction> findByCreatedAtAfterOrderByTransactionDateAsc(LocalDateTime createdAt);
    
    List<BankTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate);
}
