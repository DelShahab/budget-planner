package com.budgetplanner.budget.view;

import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.service.RecurringTransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;

/**
 * Dialog for editing recurring transaction details
 */
public class EditRecurringTransactionDialog extends Dialog {

    private final RecurringTransaction transaction;
    private final RecurringTransactionService service;
    private final Runnable onSaveCallback;

    // Form fields
    private TextField merchantNameField;
    private TextField descriptionField;
    private NumberField amountField;
    private NumberField toleranceField;
    private ComboBox<RecurringTransaction.RecurrenceFrequency> frequencyCombo;
    private NumberField intervalDaysField;
    private TextField categoryTypeField;
    private TextField categoryField;
    private DatePicker nextExpectedDatePicker;
    private ComboBox<RecurringTransaction.RecurringStatus> statusCombo;
    private TextArea notesArea;

    public EditRecurringTransactionDialog(RecurringTransaction transaction, 
                                        RecurringTransactionService service,
                                        Runnable onSaveCallback) {
        this.transaction = transaction;
        this.service = service;
        this.onSaveCallback = onSaveCallback;

        setWidth("600px");
        setHeight("700px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        createContent();
        populateFields();
    }

    private void createContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 header = new H3("Edit Recurring Transaction");
        header.getStyle().set("margin-top", "0");

        FormLayout formLayout = createForm();
        HorizontalLayout buttonLayout = createButtonLayout();

        layout.add(header, formLayout, buttonLayout);
        add(layout);
    }

    private FormLayout createForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        merchantNameField = new TextField("Merchant Name");
        merchantNameField.setRequired(true);
        merchantNameField.setWidthFull();

        descriptionField = new TextField("Description Pattern");
        descriptionField.setWidthFull();

        amountField = new NumberField("Amount ($)");
        amountField.setRequired(true);
        amountField.setMin(0);
        amountField.setStep(0.01);

        toleranceField = new NumberField("Amount Tolerance (%)");
        toleranceField.setMin(0);
        toleranceField.setMax(100);
        toleranceField.setStep(0.1);
        toleranceField.setValue(5.0);

        frequencyCombo = new ComboBox<>("Frequency");
        frequencyCombo.setItems(RecurringTransaction.RecurrenceFrequency.values());
        frequencyCombo.setRequired(true);
        frequencyCombo.setItemLabelGenerator(freq -> {
            switch (freq) {
                case WEEKLY: return "Weekly";
                case BI_WEEKLY: return "Bi-Weekly";
                case MONTHLY: return "Monthly";
                case BI_MONTHLY: return "Bi-Monthly";
                case QUARTERLY: return "Quarterly";
                case SEMI_ANNUALLY: return "Semi-Annually";
                case ANNUALLY: return "Annually";
                case CUSTOM: return "Custom";
                default: return freq.name();
            }
        });

        intervalDaysField = new NumberField("Interval (Days)");
        intervalDaysField.setMin(1);
        intervalDaysField.setStep(1);
        intervalDaysField.setVisible(false);

        frequencyCombo.addValueChangeListener(event -> {
            boolean isCustom = event.getValue() == RecurringTransaction.RecurrenceFrequency.CUSTOM;
            intervalDaysField.setVisible(isCustom);
            intervalDaysField.setRequired(isCustom);
        });

        categoryTypeField = new TextField("Category Type");
        categoryTypeField.setWidthFull();

        categoryField = new TextField("Category");
        categoryField.setWidthFull();

        nextExpectedDatePicker = new DatePicker("Next Expected Date");
        nextExpectedDatePicker.setRequired(true);

        statusCombo = new ComboBox<>("Status");
        statusCombo.setItems(RecurringTransaction.RecurringStatus.values());
        statusCombo.setRequired(true);
        statusCombo.setItemLabelGenerator(status -> {
            switch (status) {
                case ACTIVE: return "Active";
                case PAUSED: return "Paused";
                case ENDED: return "Ended";
                case IRREGULAR: return "Irregular";
                case PENDING_CONFIRMATION: return "Pending Confirmation";
                default: return status.name();
            }
        });

        notesArea = new TextArea("Notes");
        notesArea.setWidthFull();
        notesArea.setHeight("100px");

        formLayout.add(merchantNameField, descriptionField);
        formLayout.add(amountField, toleranceField);
        formLayout.add(frequencyCombo, intervalDaysField);
        formLayout.add(categoryTypeField, categoryField);
        formLayout.add(nextExpectedDatePicker, statusCombo);
        formLayout.add(notesArea, 2);

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save Changes", event -> saveTransaction());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", event -> close());

        Button deleteButton = new Button("Delete", event -> deleteTransaction());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton, deleteButton);
        buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);

        return buttonLayout;
    }

    private void populateFields() {
        merchantNameField.setValue(transaction.getMerchantName() != null ? transaction.getMerchantName() : "");
        descriptionField.setValue(transaction.getDescriptionPattern() != null ? transaction.getDescriptionPattern() : "");
        amountField.setValue(transaction.getAmount() != null ? transaction.getAmount() : 0.0);
        toleranceField.setValue(transaction.getAmountTolerance() != null ? transaction.getAmountTolerance() : 5.0);
        frequencyCombo.setValue(transaction.getFrequency());
        
        if (transaction.getFrequency() == RecurringTransaction.RecurrenceFrequency.CUSTOM) {
            intervalDaysField.setVisible(true);
            intervalDaysField.setRequired(true);
            intervalDaysField.setValue(transaction.getIntervalDays() != null ? transaction.getIntervalDays().doubleValue() : 30.0);
        }
        
        categoryTypeField.setValue(transaction.getBudgetCategoryType() != null ? transaction.getBudgetCategoryType() : "");
        categoryField.setValue(transaction.getBudgetCategory() != null ? transaction.getBudgetCategory() : "");
        nextExpectedDatePicker.setValue(transaction.getNextExpectedDate() != null ? transaction.getNextExpectedDate() : LocalDate.now());
        statusCombo.setValue(transaction.getStatus());
        notesArea.setValue(transaction.getNotes() != null ? transaction.getNotes() : "");
    }

    private void saveTransaction() {
        try {
            if (merchantNameField.isEmpty() || amountField.isEmpty() || 
                frequencyCombo.isEmpty() || nextExpectedDatePicker.isEmpty() || statusCombo.isEmpty()) {
                Notification.show("Please fill in all required fields")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            transaction.setMerchantName(merchantNameField.getValue());
            transaction.setDescriptionPattern(descriptionField.getValue());
            transaction.setAmount(amountField.getValue());
            transaction.setAmountTolerance(toleranceField.getValue());
            transaction.setFrequency(frequencyCombo.getValue());
            
            if (frequencyCombo.getValue() == RecurringTransaction.RecurrenceFrequency.CUSTOM) {
                transaction.setIntervalDays(intervalDaysField.getValue().intValue());
            } else {
                transaction.setIntervalDays(frequencyCombo.getValue().getDefaultDays());
            }
            
            transaction.setBudgetCategoryType(categoryTypeField.getValue());
            transaction.setBudgetCategory(categoryField.getValue());
            transaction.setNextExpectedDate(nextExpectedDatePicker.getValue());
            transaction.setStatus(statusCombo.getValue());
            transaction.setNotes(notesArea.getValue());
            transaction.setUserCustomized(true);

            service.updateRecurringTransaction(transaction.getId(), transaction);

            Notification.show("Transaction updated successfully!")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            close();

        } catch (Exception e) {
            Notification.show("Error saving transaction: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteTransaction() {
        try {
            transaction.setIsActive(false);
            transaction.setStatus(RecurringTransaction.RecurringStatus.ENDED);
            service.updateRecurringTransaction(transaction.getId(), transaction);

            Notification.show("Transaction deleted successfully!")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            close();

        } catch (Exception e) {
            Notification.show("Error deleting transaction: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
