package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.TransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {

    List<TransactionSplit> findByParentTransaction(BankTransaction parentTransaction);

    void deleteByParentTransaction(BankTransaction parentTransaction);
}
