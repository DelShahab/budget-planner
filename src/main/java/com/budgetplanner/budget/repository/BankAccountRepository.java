package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    Optional<BankAccount> findByPlaidAccountId(String plaidAccountId);
    
    Optional<BankAccount> findByPlaidItemId(String plaidItemId);
    
    List<BankAccount> findByIsActiveTrue();
    
    List<BankAccount> findByInstitutionName(String institutionName);
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.isActive = true ORDER BY ba.createdAt DESC")
    List<BankAccount> findActiveAccountsOrderByCreatedDesc();
    
    boolean existsByPlaidAccountId(String plaidAccountId);
}
