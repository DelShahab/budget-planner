package com.budgetplanner.budget.model;

import jakarta.persistence.*;

@Entity
@Table(name = "budget_items")
public class BudgetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private String categoryType; // INCOME, EXPENSES, BILLS, SAVINGS
    
    @Column(nullable = false)
    private Double planned;
    
    @Column(nullable = false)
    private Double actual;
    
    @Column(name = "budget_year", nullable = false)
    private Integer year;
    
    @Column(name = "budget_month")
    private Integer month;
    
    @Column(name = "user_id")
    private String userId;

    public BudgetItem() {
        this.category = "";
        this.planned = 0.0;
        this.actual = 0.0;
    }

    public BudgetItem(String category, Double planned, Double actual) {
        this.category = category;
        this.planned = planned;
        this.actual = actual;
    }
    
    // Constructor with all fields for JPA
    public BudgetItem(String category, Double planned, Double actual, String categoryType, Integer year, Integer month) {
        this.category = category;
        this.planned = planned;
        this.actual = actual;
        this.categoryType = categoryType;
        this.year = year;
        this.month = month;
    }
    
    // Constructor with primitive types for convenience
    public BudgetItem(String category, double planned, double actual, String categoryType, int year, int month) {
        this.category = category;
        this.planned = planned;
        this.actual = actual;
        this.categoryType = categoryType;
        this.year = year;
        this.month = month;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPlanned() {
        return planned;
    }

    public void setPlanned(Double planned) {
        this.planned = planned;
    }

    public Double getActual() {
        return actual;
    }

    public void setActual(Double actual) {
        this.actual = actual;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getDelta() {
        return (planned != null && actual != null) ? planned - actual : 0.0;
    }

    @Override
    public String toString() {
        return "BudgetItem{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", categoryType='" + categoryType + '\'' +
                ", planned=" + planned +
                ", actual=" + actual +
                ", year=" + year +
                ", month=" + month +
                '}';
    }
}
