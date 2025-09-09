package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    
    // Find all budget items for a specific year and month
    List<BudgetItem> findByYearAndMonth(Integer year, Integer month);
    
    // Find all budget items by category type (INCOME, EXPENSES, BILLS, SAVINGS)
    List<BudgetItem> findByCategoryTypeAndYearAndMonth(String categoryType, Integer year, Integer month);
    
    // Find all budget items for a specific year
    List<BudgetItem> findByYear(Integer year);
    
    // Custom query to get total planned amount by category type for a specific year and month
    @Query("SELECT SUM(b.planned) FROM BudgetItem b WHERE b.categoryType = :categoryType AND b.year = :year AND b.month = :month")
    Double getTotalPlannedByCategoryType(@Param("categoryType") String categoryType, @Param("year") Integer year, @Param("month") Integer month);
    
    // Custom query to get total actual amount by category type for a specific year and month
    @Query("SELECT SUM(b.actual) FROM BudgetItem b WHERE b.categoryType = :categoryType AND b.year = :year AND b.month = :month")
    Double getTotalActualByCategoryType(@Param("categoryType") String categoryType, @Param("year") Integer year, @Param("month") Integer month);
    
    // Delete all budget items for a specific year and month
    void deleteByYearAndMonth(Integer year, Integer month);
}
