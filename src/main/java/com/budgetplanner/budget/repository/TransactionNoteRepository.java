package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.TransactionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionNoteRepository extends JpaRepository<TransactionNote, Long> {

    Optional<TransactionNote> findByBankTransaction(BankTransaction bankTransaction);

    void deleteByBankTransaction(BankTransaction bankTransaction);
}
