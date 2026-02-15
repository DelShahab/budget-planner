package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.TransactionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTagRepository extends JpaRepository<TransactionTag, Long> {

    List<TransactionTag> findByBankTransaction(BankTransaction bankTransaction);

    void deleteByBankTransaction(BankTransaction bankTransaction);

    @Query("select distinct t.tag from TransactionTag t order by t.tag asc")
    List<String> findDistinctTagNames();
}
