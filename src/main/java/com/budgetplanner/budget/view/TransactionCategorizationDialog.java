package com.budgetplanner.budget.view;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.service.BankAccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class TransactionCategorizationDialog extends Dialog {
    
    private final BankAccountService bankAccountService;
    private final String originalCategory;
    private final String originalCategoryType;
    private final YearMonth yearMonth;
    private final Consumer<Void> onSaveCallback;
    
    private Grid<BankTransaction> transactionGrid;
    private Span totalAmountSpan;
    private Span transactionCountSpan;
    
    // Category options
    private static final String[] CATEGORY_TYPES = {"INCOME", "EXPENSES", "BILLS", "SAVINGS"};
    private static final String[][] CATEGORIES = {
        {"Salary", "Freelance", "Investment Returns", "Other Income"}, // INCOME
        {"Groceries", "Gas", "Dining Out", "Shopping", "Entertainment", "Other Expenses"}, // EXPENSES  
        {"Rent", "Utilities", "Internet", "Phone", "Insurance", "Other Bills"}, // BILLS
        {"Emergency Fund", "Retirement", "Vacation", "Other Savings"} // SAVINGS
    };
    
    public TransactionCategorizationDialog(BankAccountService bankAccountService, 
                                         String category, 
                                         String categoryType, 
                                         YearMonth yearMonth,
                                         Consumer<Void> onSaveCallback) {
        this.bankAccountService = bankAccountService;
        this.originalCategory = category;
        this.originalCategoryType = categoryType;
        this.yearMonth = yearMonth;
        this.onSaveCallback = onSaveCallback;
        
        initializeDialog();
        loadTransactions();
    }
    
    private void initializeDialog() {
        getHeader().add(new H3("Manage Transactions - " + originalCategory));
        setWidth("900px");
        setHeight("700px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        
        // Create main layout
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        mainLayout.setSizeFull();
        
        // Summary section
        HorizontalLayout summaryLayout = createSummarySection();
        
        // Transaction grid
        createTransactionGrid();
        
        // Action buttons
        HorizontalLayout buttonLayout = createButtonLayout();
        
        mainLayout.add(summaryLayout, transactionGrid, buttonLayout);
        mainLayout.setFlexGrow(0, summaryLayout);
        mainLayout.setFlexGrow(1, transactionGrid);
        mainLayout.setFlexGrow(0, buttonLayout);
        
        add(mainLayout);
    }
    
    private HorizontalLayout createSummarySection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        layout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                         .set("padding", "var(--lumo-space-m)")
                         .set("border-radius", "var(--lumo-border-radius-m)");
        
        // Category info
        VerticalLayout categoryInfo = new VerticalLayout();
        categoryInfo.setSpacing(false);
        categoryInfo.setPadding(false);
        
        H3 categoryTitle = new H3(originalCategory);
        categoryTitle.getStyle().set("margin", "0");
        
        Span categoryTypeSpan = new Span(originalCategoryType);
        categoryTypeSpan.getStyle().set("color", "var(--lumo-secondary-text-color)")
                                   .set("font-size", "var(--lumo-font-size-s)");
        
        categoryInfo.add(categoryTitle, categoryTypeSpan);
        
        // Transaction stats
        VerticalLayout statsInfo = new VerticalLayout();
        statsInfo.setSpacing(false);
        statsInfo.setPadding(false);
        statsInfo.setAlignItems(VerticalLayout.Alignment.END);
        
        transactionCountSpan = new Span("0 transactions");
        transactionCountSpan.getStyle().set("font-weight", "bold");
        
        totalAmountSpan = new Span("$0.00");
        totalAmountSpan.getStyle().set("font-size", "var(--lumo-font-size-l)")
                                  .set("color", "var(--lumo-primary-text-color)");
        
        statsInfo.add(transactionCountSpan, totalAmountSpan);
        
        layout.add(categoryInfo, statsInfo);
        return layout;
    }
    
    private void createTransactionGrid() {
        transactionGrid = new Grid<>(BankTransaction.class, false);
        transactionGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        transactionGrid.setHeight("400px");
        
        // Date column
        transactionGrid.addColumn(transaction -> 
            transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
            .setHeader("Date")
            .setWidth("120px")
            .setFlexGrow(0);
        
        // Merchant column
        transactionGrid.addColumn(BankTransaction::getMerchantName)
            .setHeader("Merchant")
            .setFlexGrow(2);
        
        // Description column
        transactionGrid.addColumn(BankTransaction::getDescription)
            .setHeader("Description")
            .setFlexGrow(2);
        
        // Amount column
        transactionGrid.addColumn(transaction -> 
            String.format("$%.2f", Math.abs(transaction.getAmount())))
            .setHeader("Amount")
            .setWidth("100px")
            .setFlexGrow(0);
        
        // Category Type selector column
        transactionGrid.addColumn(new ComponentRenderer<>(this::createCategoryTypeSelector))
            .setHeader("Category Type")
            .setWidth("150px")
            .setFlexGrow(0);
        
        // Category selector column
        transactionGrid.addColumn(new ComponentRenderer<>(this::createCategorySelector))
            .setHeader("Category")
            .setWidth("150px")
            .setFlexGrow(0);
        
        // Action column
        transactionGrid.addColumn(new ComponentRenderer<>(this::createActionButton))
            .setHeader("Action")
            .setWidth("80px")
            .setFlexGrow(0);
    }
    
    private Select<String> createCategoryTypeSelector(BankTransaction transaction) {
        Select<String> categoryTypeSelect = new Select<>();
        categoryTypeSelect.setItems(CATEGORY_TYPES);
        categoryTypeSelect.setValue(transaction.getBudgetCategoryType());
        categoryTypeSelect.setWidth("140px");
        
        categoryTypeSelect.addValueChangeListener(event -> {
            String newCategoryType = event.getValue();
            transaction.setBudgetCategoryType(newCategoryType);
            
            // Reset category when category type changes
            transaction.setBudgetCategory(null);
            transactionGrid.getDataProvider().refreshItem(transaction);
        });
        
        return categoryTypeSelect;
    }
    
    private Select<String> createCategorySelector(BankTransaction transaction) {
        Select<String> categorySelect = new Select<>();
        categorySelect.setWidth("140px");
        
        // Set items based on category type
        String categoryType = transaction.getBudgetCategoryType();
        if (categoryType != null) {
            int typeIndex = java.util.Arrays.asList(CATEGORY_TYPES).indexOf(categoryType);
            if (typeIndex >= 0) {
                categorySelect.setItems(CATEGORIES[typeIndex]);
            }
        }
        
        categorySelect.setValue(transaction.getBudgetCategory());
        
        categorySelect.addValueChangeListener(event -> {
            transaction.setBudgetCategory(event.getValue());
        });
        
        return categorySelect;
    }
    
    private Button createActionButton(BankTransaction transaction) {
        Button saveButton = new Button(VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        saveButton.getElement().setProperty("title", "Save changes");
        
        saveButton.addClickListener(event -> {
            saveTransactionChanges(transaction);
        });
        
        return saveButton;
    }
    
    private void saveTransactionChanges(BankTransaction transaction) {
        try {
            bankAccountService.updateTransactionCategory(
                transaction.getId(),
                transaction.getBudgetCategory(),
                transaction.getBudgetCategoryType()
            );
            
            Notification notification = Notification.show(
                "Transaction updated successfully!", 
                3000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Refresh the grid to show updated data
            transactionGrid.getDataProvider().refreshItem(transaction);
            updateSummary();
            
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Error updating transaction: " + e.getMessage(), 
                5000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        layout.setSpacing(true);
        
        Button saveAllButton = new Button("Save All Changes", VaadinIcon.DOWNLOAD.create());
        saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveAllButton.addClickListener(event -> saveAllChanges());
        
        Button closeButton = new Button("Close", VaadinIcon.CLOSE.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(event -> close());
        
        layout.add(saveAllButton, closeButton);
        return layout;
    }
    
    private void saveAllChanges() {
        try {
            List<BankTransaction> transactions = transactionGrid.getGenericDataView().getItems().toList();
            int updatedCount = 0;
            
            for (BankTransaction transaction : transactions) {
                if (transaction.getBudgetCategory() != null && transaction.getBudgetCategoryType() != null) {
                    bankAccountService.updateTransactionCategory(
                        transaction.getId(),
                        transaction.getBudgetCategory(),
                        transaction.getBudgetCategoryType()
                    );
                    updatedCount++;
                }
            }
            
            Notification notification = Notification.show(
                "Updated " + updatedCount + " transactions successfully!", 
                3000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Trigger callback to refresh parent view
            if (onSaveCallback != null) {
                onSaveCallback.accept(null);
            }
            
            close();
            
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Error saving changes: " + e.getMessage(), 
                5000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void loadTransactions() {
        try {
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            
            List<BankTransaction> transactions = bankAccountService
                .getTransactionsByDateRangeAndCategory(startDate, endDate, originalCategoryType)
                .stream()
                .filter(t -> originalCategory.equals(t.getBudgetCategory()))
                .toList();
            
            transactionGrid.setItems(transactions);
            updateSummary();
            
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Error loading transactions: " + e.getMessage(), 
                5000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void updateSummary() {
        List<BankTransaction> transactions = transactionGrid.getGenericDataView().getItems().toList();
        
        int count = transactions.size();
        double total = transactions.stream()
            .mapToDouble(t -> Math.abs(t.getAmount()))
            .sum();
        
        transactionCountSpan.setText(count + " transaction" + (count != 1 ? "s" : ""));
        totalAmountSpan.setText(String.format("$%.2f", total));
    }
}
