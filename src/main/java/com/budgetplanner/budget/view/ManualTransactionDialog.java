package com.budgetplanner.budget.view;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.service.BankAccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.util.function.Consumer;

public class ManualTransactionDialog extends Dialog {
    
    private final BankAccountService bankAccountService;
    private final Consumer<Void> onSaveCallback;
    
    // Form fields
    private TextField merchantNameField;
    private TextField descriptionField;
    private NumberField amountField;
    private DatePicker transactionDatePicker;
    private Select<String> categoryTypeSelect;
    private Select<String> categorySelect;
    
    // Category options
    private static final String[] CATEGORY_TYPES = {"INCOME", "EXPENSES", "BILLS", "SAVINGS"};
    private static final String[][] CATEGORIES = {
        {"Salary", "Freelance", "Investment Returns", "Other Income"}, // INCOME
        {"Groceries", "Gas", "Dining Out", "Shopping", "Entertainment", "Other Expenses"}, // EXPENSES  
        {"Rent", "Utilities", "Internet", "Phone", "Insurance", "Other Bills"}, // BILLS
        {"Emergency Fund", "Retirement", "Vacation", "Other Savings"} // SAVINGS
    };
    
    public ManualTransactionDialog(BankAccountService bankAccountService, Consumer<Void> onSaveCallback) {
        this.bankAccountService = bankAccountService;
        this.onSaveCallback = onSaveCallback;
        
        initializeDialog();
    }
    
    private void initializeDialog() {
        getHeader().add(new H3("Add Manual Transaction"));
        setWidth("500px");
        setModal(true);
        setDraggable(true);
        setResizable(false);
        
        // Create main layout
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        
        // Instructions
        Span instructions = new Span("Enter transaction details to manually add to your budget.");
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)")
                               .set("font-size", "var(--lumo-font-size-s)")
                               .set("margin-bottom", "var(--lumo-space-m)");
        
        // Form layout
        FormLayout formLayout = createForm();
        
        // Button layout
        HorizontalLayout buttonLayout = createButtonLayout();
        
        mainLayout.add(instructions, formLayout, buttonLayout);
        add(mainLayout);
    }
    
    private FormLayout createForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Merchant Name field
        merchantNameField = new TextField("Merchant Name");
        merchantNameField.setPlaceholder("e.g., Walmart, Starbucks, Electric Company");
        merchantNameField.setRequired(true);
        merchantNameField.setRequiredIndicatorVisible(true);
        
        // Description field
        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Optional description");
        
        // Amount field
        amountField = new NumberField("Amount");
        amountField.setPlaceholder("0.00");
        amountField.setPrefixComponent(new Span("$"));
        amountField.setRequired(true);
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Use positive values for income, negative for expenses");
        
        // Transaction Date picker
        transactionDatePicker = new DatePicker("Transaction Date");
        transactionDatePicker.setValue(LocalDate.now());
        transactionDatePicker.setRequired(true);
        transactionDatePicker.setRequiredIndicatorVisible(true);
        
        // Category Type selector
        categoryTypeSelect = new Select<>();
        categoryTypeSelect.setLabel("Category Type");
        categoryTypeSelect.setItems(CATEGORY_TYPES);
        categoryTypeSelect.setPlaceholder("Select category type");
        categoryTypeSelect.setRequiredIndicatorVisible(true);
        
        // Category selector
        categorySelect = new Select<>();
        categorySelect.setLabel("Category");
        categorySelect.setPlaceholder("Select category");
        categorySelect.setRequiredIndicatorVisible(true);
        categorySelect.setEnabled(false);
        
        // Add listener to update categories when category type changes
        categoryTypeSelect.addValueChangeListener(event -> {
            String selectedType = event.getValue();
            if (selectedType != null) {
                int typeIndex = java.util.Arrays.asList(CATEGORY_TYPES).indexOf(selectedType);
                if (typeIndex >= 0) {
                    categorySelect.setItems(CATEGORIES[typeIndex]);
                    categorySelect.setEnabled(true);
                    categorySelect.clear();
                }
            } else {
                categorySelect.setEnabled(false);
                categorySelect.clear();
            }
        });
        
        // Add fields to form layout
        formLayout.add(merchantNameField, descriptionField);
        formLayout.add(amountField, transactionDatePicker);
        formLayout.add(categoryTypeSelect, categorySelect);
        
        // Make merchant name and amount span full width on small screens
        formLayout.setColspan(merchantNameField, 2);
        formLayout.setColspan(amountField, 2);
        
        return formLayout;
    }
    
    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        layout.setSpacing(true);
        
        Button saveButton = new Button("Add Transaction", VaadinIcon.PLUS.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveTransaction());
        
        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(event -> close());
        
        layout.add(cancelButton, saveButton);
        return layout;
    }
    
    private void saveTransaction() {
        // Validate required fields
        if (!validateForm()) {
            return;
        }
        
        try {
            // Create the manual transaction
            bankAccountService.createManualTransaction(
                merchantNameField.getValue().trim(),
                descriptionField.getValue() != null ? descriptionField.getValue().trim() : null,
                amountField.getValue(),
                transactionDatePicker.getValue(),
                categorySelect.getValue(),
                categoryTypeSelect.getValue()
            );
            
            // Show success notification
            Notification notification = Notification.show(
                "Transaction added successfully!", 
                3000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Trigger callback to refresh parent view
            if (onSaveCallback != null) {
                onSaveCallback.accept(null);
            }
            
            // Close dialog
            close();
            
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Error adding transaction: " + e.getMessage(), 
                5000, 
                Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private boolean validateForm() {
        boolean isValid = true;
        
        // Validate merchant name
        if (merchantNameField.getValue() == null || merchantNameField.getValue().trim().isEmpty()) {
            merchantNameField.setErrorMessage("Merchant name is required");
            merchantNameField.setInvalid(true);
            isValid = false;
        } else {
            merchantNameField.setInvalid(false);
        }
        
        // Validate amount
        if (amountField.getValue() == null) {
            amountField.setErrorMessage("Amount is required");
            amountField.setInvalid(true);
            isValid = false;
        } else if (amountField.getValue() == 0.0) {
            amountField.setErrorMessage("Amount cannot be zero");
            amountField.setInvalid(true);
            isValid = false;
        } else {
            amountField.setInvalid(false);
        }
        
        // Validate transaction date
        if (transactionDatePicker.getValue() == null) {
            transactionDatePicker.setErrorMessage("Transaction date is required");
            transactionDatePicker.setInvalid(true);
            isValid = false;
        } else {
            transactionDatePicker.setInvalid(false);
        }
        
        // Validate category type
        if (categoryTypeSelect.getValue() == null) {
            categoryTypeSelect.setErrorMessage("Category type is required");
            categoryTypeSelect.setInvalid(true);
            isValid = false;
        } else {
            categoryTypeSelect.setInvalid(false);
        }
        
        // Validate category
        if (categorySelect.getValue() == null) {
            categorySelect.setErrorMessage("Category is required");
            categorySelect.setInvalid(true);
            isValid = false;
        } else {
            categorySelect.setInvalid(false);
        }
        
        return isValid;
    }
}
