package com.budgetplanner.budget.view;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.TransactionMetaService;
import com.budgetplanner.budget.service.RecurringTransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog to display detailed transaction information
 */
public class TransactionDetailsDialog extends Dialog {
    
    private final BankTransaction transaction;
    private final String formattedAmount;
    private final String categoryColor;
    private final BankAccountService bankAccountService;
    private final DashboardDataService dashboardDataService;
    private final TransactionMetaService transactionMetaService;
    private final RecurringTransactionService recurringTransactionService;

    private TextArea notesField;
    // Goal selection
    private com.budgetplanner.budget.model.SavingsGoal selectedGoal;
    private Span goalValueLabel;
    // Maintain selected tags as a set; UI shows chips and a selector dialog
    private java.util.LinkedHashSet<String> selectedTags = new java.util.LinkedHashSet<>();
    private Div selectedTagsContainer;
    
    public TransactionDetailsDialog(BankTransaction transaction,
                                   String formattedAmount,
                                   String categoryColor,
                                   BankAccountService bankAccountService,
                                   DashboardDataService dashboardDataService,
                                   TransactionMetaService transactionMetaService,
                                   RecurringTransactionService recurringTransactionService) {
        this.transaction = transaction;
        this.formattedAmount = formattedAmount;
        this.categoryColor = categoryColor;
        this.bankAccountService = bankAccountService;
        this.dashboardDataService = dashboardDataService;
        this.transactionMetaService = transactionMetaService;
        this.recurringTransactionService = recurringTransactionService;
        
        initializeDialog();
    }
    
    private void initializeDialog() {
        setMaxWidth("480px");
        setModal(true);
        setDraggable(false);
        setResizable(false);
        
        // Right-side flyover sheet styling
        getElement().getThemeList().add("modern-dialog");
        getElement().getStyle()
            .set("position", "fixed")
            .set("top", "0")
            .set("right", "0")
            .set("height", "100vh")
            .set("margin", "0")
            .set("background", "#050816")
            .set("border-left", "1px solid rgba(148, 163, 184, 0.2)")
            .set("border-radius", "24px 0 0 24px")
            .set("box-sizing", "border-box");
        
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(false);
        mainLayout.getStyle()
            .set("padding", "24px 28px")
            .set("gap", "24px")
            .set("overflow", "auto");
        
        // Header and primary info
        HorizontalLayout header = createHeader();
        Div primaryInfo = createPrimaryInfoSection();
        
        // Existing technical details block
        Div detailsSection = createDetailsSection();
        detailsSection.setWidthFull();
        
        // Extended controls: goal, notes, tags, similar transactions
        Div goalAndMeta = createGoalNotesTagsSection();
        Div similarTransactions = createSimilarTransactionsSection();

        loadMetaValues();
        
        mainLayout.add(header, primaryInfo, detailsSection, goalAndMeta, similarTransactions, createActionButtons());
        add(mainLayout);
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        String headerText = transaction.getRecurringTransaction() != null
                ? "Recurring transaction"
                : "Regular transaction";
        H3 title = new H3(headerText);
        title.getStyle()
            .set("margin", "0")
            .set("color", "#fff")
            .set("font-size", "18px")
            .set("font-weight", "600");

        // Right side controls: Split, Recurring, Close
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);
        controls.setAlignItems(FlexComponent.Alignment.CENTER);

        Button splitButton = new Button("Split");
        splitButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        splitButton.getStyle()
            .set("background", "#020617")
            .set("border-radius", "10px")
            .set("border", "1px solid rgba(148, 163, 184, 0.6)")
            .set("color", "#E5E7EB")
            .set("font-size", "12px")
            .set("padding", "4px 10px");
        splitButton.addClickListener(e -> openSplitDialog());

        Button recurringButton = new Button("Recurring");
        recurringButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        recurringButton.getStyle()
            .set("background", "#020617")
            .set("border-radius", "10px")
            .set("border", "1px solid rgba(148, 163, 184, 0.6)")
            .set("color", "#E5E7EB")
            .set("font-size", "12px")
            .set("padding", "4px 10px");
        recurringButton.addClickListener(e -> openRecurringDialog());

        Button closeButton = new Button(new Icon(VaadinIcon.CLOSE));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.getStyle()
            .set("color", "#fff")
            .set("cursor", "pointer");
        closeButton.addClickListener(e -> close());

        controls.add(splitButton, recurringButton, closeButton);
        header.add(title, controls);
        return header;
    }

    private void openSplitDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Split transaction");
        dialog.setWidth("420px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("gap", "16px");

        // Title: merchant and total amount
        Span merchantSpan = new Span(merchantNameOrFallback());
        merchantSpan.getStyle()
            .set("font-size", "20px")
            .set("font-weight", "600")
            .set("color", "#F9FAFB");

        Span amountSpan = new Span(formattedAmount);
        amountSpan.getStyle()
            .set("font-size", "16px")
            .set("color", "#E5E7EB");

        VerticalLayout headerBlock = new VerticalLayout(merchantSpan, amountSpan);
        headerBlock.setPadding(false);
        headerBlock.setSpacing(false);

        VerticalLayout lines = new VerticalLayout();
        lines.setPadding(false);
        lines.setSpacing(false);
        lines.getStyle().set("gap", "8px");

        // Helper to add a new editable row
        java.util.function.BiConsumer<Double, String> addRow = (amount, category) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            NumberField amountField = new NumberField();
            amountField.setWidth("120px");
            amountField.setStep(0.01);
            amountField.setValue(amount != null ? amount : 0.0);

            TextField categoryField = new TextField();
            categoryField.setPlaceholder("Category");
            categoryField.setWidth("160px");
            if (category != null) {
                categoryField.setValue(category);
            }

            Button removeBtn = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            removeBtn.addClickListener(ev -> lines.remove(row));

            row.add(amountField, categoryField, removeBtn);
            lines.add(row);
        };

        // Load existing splits, or prefill a single line for the full amount
        java.util.List<com.budgetplanner.budget.model.TransactionSplit> existing =
                bankAccountService.getSplitsForTransaction(transaction);
        if (existing.isEmpty()) {
            addRow.accept(Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0.0),
                    transaction.getBudgetCategory());
        } else {
            for (com.budgetplanner.budget.model.TransactionSplit split : existing) {
                addRow.accept(split.getAmount(), split.getBudgetCategory());
            }
        }

        Span addLink = new Span("+ add");
        addLink.getStyle()
            .set("font-size", "13px")
            .set("color", "#60A5FA")
            .set("cursor", "pointer");
        addLink.addClickListener(e -> addRow.accept(0.0, null));

        content.add(headerBlock, lines, addLink);
        dialog.add(content);

        Button saveButton = new Button("Save", e -> {
            try {
                java.util.List<com.budgetplanner.budget.model.TransactionSplit> splits = new java.util.ArrayList<>();
                lines.getChildren().forEach(component -> {
                    if (component instanceof HorizontalLayout) {
                        HorizontalLayout row = (HorizontalLayout) component;
                        NumberField amountField = (NumberField) row.getComponentAt(0);
                        TextField categoryField = (TextField) row.getComponentAt(1);
                        Double value = amountField.getValue();
                        if (value != null && Math.abs(value) > 0.0001) {
                            com.budgetplanner.budget.model.TransactionSplit split =
                                    new com.budgetplanner.budget.model.TransactionSplit();
                            split.setAmount(value);
                            split.setBudgetCategory(categoryField.getValue());
                            // Use existing budgetCategoryType as a default
                            split.setBudgetCategoryType(transaction.getBudgetCategoryType());
                            splits.add(split);
                        }
                    }
                });

                bankAccountService.saveSplitsForTransaction(transaction, splits);
                dialog.close();
            } catch (IllegalArgumentException ex) {
                Notification n = Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void openRecurringDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Recurrings");
        dialog.setWidth("360px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("gap", "10px");

        Span description = new Span("Matching recurring patterns for this transaction:");
        description.getStyle()
            .set("font-size", "13px")
            .set("color", "#9CA3AF");

        VerticalLayout matchesList = new VerticalLayout();
        matchesList.setPadding(false);
        matchesList.setSpacing(false);
        matchesList.getStyle().set("gap", "6px");

        java.util.List<RecurringTransaction> matches =
                recurringTransactionService.findMatchingRecurringPatterns(transaction);

        if (matches.isEmpty()) {
            Span empty = new Span("No recurring patterns found yet.");
            empty.getStyle()
                .set("font-size", "12px")
                .set("color", "#6B7280");
            matchesList.add(empty);
        } else {
            for (RecurringTransaction rt : matches) {
                Div row = new Div();
                row.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("padding", "6px 8px")
                    .set("border-radius", "8px")
                    .set("background", "#020617");

                Span title = new Span(rt.getMerchantName());
                title.getStyle()
                    .set("font-size", "13px")
                    .set("color", "#E5E7EB");

                Span meta = new Span(rt.getRecurrenceDescription());
                meta.getStyle()
                    .set("font-size", "11px")
                    .set("color", "#9CA3AF");

                row.add(title, meta);
                matchesList.add(row);
            }
        }

        Button startNewButton = new Button("Start new from this transaction", e -> {
            try {
                RecurringTransaction rt = new RecurringTransaction(
                        transaction.getMerchantName(),
                        Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0.0),
                        RecurringTransaction.RecurrenceFrequency.MONTHLY
                );
                rt.setDetectionMethod(RecurringTransaction.DetectionMethod.USER_DEFINED);
                rt.setBudgetCategoryType(transaction.getBudgetCategoryType());
                rt.setBudgetCategory(transaction.getBudgetCategory());
                if (transaction.getTransactionDate() != null) {
                    rt.setFirstOccurrence(transaction.getTransactionDate());
                    rt.setLastOccurrence(transaction.getTransactionDate());
                    rt.setNextExpectedDate(transaction.getTransactionDate().plusDays(
                            rt.getFrequency().getDefaultDays()));
                }
                rt.setStatus(RecurringTransaction.RecurringStatus.ACTIVE);
                rt.setOccurrenceCount(1);
                rt.setUserConfirmed(true);
                rt.setUserCustomized(true);
                rt.setIsActive(true);

                RecurringTransaction saved = recurringTransactionService.saveOrUpdateRecurringTransaction(rt);

                // Link this specific transaction to the new recurring pattern
                bankAccountService.setRecurringForTransaction(transaction, saved);

                dialog.close();
                getUI().ifPresent(ui -> ui.navigate(com.budgetplanner.budget.RecurringTransactionView.class));
            } catch (Exception ex) {
                Notification n = Notification.show("Error creating recurring: " + ex.getMessage(),
                        4000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        startNewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button openRecurringView = new Button("Open Recurring Manager", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate(com.budgetplanner.budget.RecurringTransactionView.class));
        });
        openRecurringView.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        content.add(description, matchesList, startNewButton, openRecurringView);
        dialog.add(content);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private Div createPrimaryInfoSection() {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "16px");

        // Row 1: date + simple status
        HorizontalLayout metaRow = new HorizontalLayout();
        metaRow.setWidthFull();
        metaRow.setSpacing(true);
        metaRow.setAlignItems(FlexComponent.Alignment.CENTER);

        String dateStr = transaction.getTransactionDate()
            .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));
        Span dateSpan = new Span(dateStr);
        dateSpan.getStyle()
            .set("color", "#9CA3AF")
            .set("font-size", "13px");

        Span statusSpan = new Span(transaction.getIsProcessed() ? "Processed" : "To review");
        statusSpan.getStyle()
            .set("margin-left", "8px")
            .set("padding", "2px 10px")
            .set("border-radius", "999px")
            .set("background", "rgba(148, 163, 184, 0.18)")
            .set("color", "#E5E7EB")
            .set("font-size", "11px")
            .set("font-weight", "500");

        metaRow.add(dateSpan, statusSpan);

        // Row 2: merchant title + amount
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        String merchantName = transaction.getMerchantName() != null
            ? transaction.getMerchantName()
            : "Transaction";
        Span merchantTitle = new Span(merchantName);
        merchantTitle.getStyle()
            .set("font-size", "22px")
            .set("font-weight", "600")
            .set("color", "#F9FAFB");

        Span amountSpan = new Span(formattedAmount);
        amountSpan.getStyle()
            .set("font-size", "20px")
            .set("font-weight", "600")
            .set("color", "#F9FAFB");

        titleRow.add(merchantTitle, amountSpan);

        // Row 3: category + account pills
        HorizontalLayout pillsRow = new HorizontalLayout();
        pillsRow.setWidthFull();
        pillsRow.setSpacing(true);

        String category = transaction.getBudgetCategory() != null
            ? transaction.getBudgetCategory()
            : "Uncategorized";
        Span categoryPill = new Span(category.toUpperCase());
        categoryPill.getStyle()
            .set("padding", "4px 12px")
            .set("border-radius", "999px")
            .set("background", "rgba(148, 163, 184, 0.18)")
            .set("color", "#E5E7EB")
            .set("font-size", "11px")
            .set("font-weight", "600");

        Span accountPill = new Span();
        if (transaction.getBankAccount() != null) {
            String accountLabel = transaction.getBankAccount().getInstitutionName();
            if (transaction.getBankAccount().getMask() != null) {
                accountLabel += " • " + transaction.getBankAccount().getMask();
            }
            accountPill.setText(accountLabel);
        } else {
            accountPill.setText("Account");
        }
        accountPill.getStyle()
            .set("padding", "4px 12px")
            .set("border-radius", "999px")
            .set("background", "rgba(37, 99, 235, 0.22)")
            .set("color", "#BFDBFE")
            .set("font-size", "11px")
            .set("font-weight", "600");

        pillsRow.add(categoryPill, accountPill);

        container.add(metaRow, titleRow, pillsRow);

        // Optional recurring info line
        if (transaction.getRecurringTransaction() != null) {
            RecurringTransaction rt = transaction.getRecurringTransaction();
            String label = "Part of recurring: " + rt.getMerchantName();
            String desc = rt.getRecurrenceDescription();
            if (desc != null && !desc.isBlank()) {
                label += " (" + desc + ")";
            }

            Span recurringLine = new Span(label);
            recurringLine.getStyle()
                .set("font-size", "12px")
                .set("color", "#9CA3AF");

            container.add(recurringLine);
        }
        return container;
    }
    
    private Div createDetailsSection() {
        Div section = new Div();
        section.setWidthFull();
        section.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "15px")
            .set("width", "100%");
        
        // Merchant/Description
        section.add(createDetailRow(VaadinIcon.SHOP, "Merchant", transaction.getMerchantName()));
        
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            section.add(createDetailRow(VaadinIcon.FILE_TEXT_O, "Description", transaction.getDescription()));
        }
        
        // Date
        String dateStr = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        section.add(createDetailRow(VaadinIcon.CALENDAR, "Transaction Date", dateStr));
        
        // Category
        String category = transaction.getBudgetCategory() != null ? transaction.getBudgetCategory() : "Uncategorized";
        section.add(createDetailRow(VaadinIcon.TAG, "Category", category, categoryColor));
        
        // Category Type
        if (transaction.getBudgetCategoryType() != null) {
            section.add(createDetailRow(VaadinIcon.FOLDER, "Type", transaction.getBudgetCategoryType()));
        }
        
        // Transaction Type
        section.add(createDetailRow(VaadinIcon.EXCHANGE, "Transaction Type", 
            transaction.getTransactionType().toUpperCase()));
        
        // Plaid Category (if available)
        if (transaction.getPlaidCategory() != null) {
            String plaidCat = transaction.getPlaidCategory();
            if (transaction.getPlaidSubcategory() != null) {
                plaidCat += " → " + transaction.getPlaidSubcategory();
            }
            section.add(createDetailRow(VaadinIcon.INFO_CIRCLE, "Plaid Category", plaidCat));
        }
        
        // Status indicators
        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setSpacing(true);
        statusRow.getStyle().set("margin-top", "10px");
        
        if (transaction.getIsProcessed()) {
            Span processedBadge = createStatusBadge("Processed", "#22c55e");
            statusRow.add(processedBadge);
        }
        
        if (transaction.getIsManuallyReviewed()) {
            Span reviewedBadge = createStatusBadge("Reviewed", "#3b82f6");
            statusRow.add(reviewedBadge);
        }
        
        if (transaction.getRecurringTransaction() != null) {
            Span recurringBadge = createStatusBadge("Recurring", "#a855f7");
            statusRow.add(recurringBadge);
        }
        
        if (statusRow.getComponentCount() > 0) {
            section.add(statusRow);
        }
        
        return section;
    }
    
    private HorizontalLayout createDetailRow(VaadinIcon iconType, String label, String value) {
        return createDetailRow(iconType, label, value, "#00d4ff");
    }
    
    private HorizontalLayout createDetailRow(VaadinIcon iconType, String label, String value, String iconColor) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("gap", "12px");
        
        Icon icon = new Icon(iconType);
        icon.getStyle()
            .set("width", "20px")
            .set("height", "20px")
            .set("color", iconColor);
        
        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setWidthFull();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.getStyle()
            .set("flex", "1")
            .set("gap", "2px");
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-size", "11px")
            .set("color", "rgba(255, 255, 255, 0.6)")
            .set("font-weight", "500")
            .set("text-transform", "uppercase");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
            .set("font-size", "14px")
            .set("color", "#fff")
            .set("font-weight", "500");
        
        textLayout.add(labelSpan, valueSpan);
        row.add(icon, textLayout);
        
        return row;
    }
    
    private Span createStatusBadge(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
            .set("padding", "4px 12px")
            .set("background", color + "33")
            .set("color", color)
            .set("border-radius", "6px")
            .set("font-size", "11px")
            .set("font-weight", "600")
            .set("text-transform", "uppercase");
        return badge;
    }

    private Div createGoalNotesTagsSection() {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "16px");

        // Goal row
        HorizontalLayout goalRow = new HorizontalLayout();
        goalRow.setWidthFull();
        goalRow.setAlignItems(FlexComponent.Alignment.CENTER);
        goalRow.setSpacing(true);

        Span goalLabel = new Span("Goal");
        goalLabel.getStyle()
            .set("font-size", "13px")
            .set("color", "#9CA3AF")
            .set("font-weight", "500");

        Div goalButton = new Div();
        goalButton.getStyle()
            .set("width", "28px")
            .set("height", "28px")
            .set("border-radius", "999px")
            .set("border", "1px solid rgba(148, 163, 184, 0.6)")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("color", "#E5E7EB")
            .set("cursor", "pointer")
            .set("font-size", "16px");
        goalButton.setText("+");
        goalButton.addClickListener(e -> openGoalSelectorDialog());

        goalValueLabel = new Span("No goal");
        goalValueLabel.getStyle()
            .set("font-size", "13px")
            .set("color", "#E5E7EB");

        goalRow.add(goalLabel, goalButton, goalValueLabel);

        // Notes box (persisted)
        Div notesContainer = new Div();
        notesContainer.setWidthFull();
        notesContainer.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "8px");

        Span notesLabel = new Span("Notes");
        notesLabel.getStyle()
            .set("font-size", "13px")
            .set("color", "#9CA3AF")
            .set("font-weight", "500");

        notesField = new TextArea();
        notesField.setWidthFull();
        notesField.setMinHeight("80px");
        notesField.getStyle()
            .set("border-radius", "12px")
            .set("border", "1px solid rgba(55, 65, 81, 0.9)")
            .set("background", "rgba(15, 23, 42, 0.7)");

        notesContainer.add(notesLabel, notesField);

        // Tags row: Add tag link + current tags display
        Div tagsContainer = new Div();
        tagsContainer.setWidthFull();
        tagsContainer.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "6px");

        HorizontalLayout tagsHeader = new HorizontalLayout();
        tagsHeader.setWidthFull();
        tagsHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        tagsHeader.setSpacing(true);

        Span tagsLabel = new Span("Tags");
        tagsLabel.getStyle()
            .set("font-size", "13px")
            .set("color", "#9CA3AF")
            .set("font-weight", "500");

        HorizontalLayout addTagLayout = new HorizontalLayout();
        addTagLayout.setSpacing(true);
        addTagLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        addTagLayout.getStyle()
            .set("cursor", "pointer");

        Icon tagIcon = new Icon(VaadinIcon.TAG);
        tagIcon.setSize("14px");
        tagIcon.getStyle().set("color", "#9CA3AF");

        Span addTagLink = new Span("Add tag");
        addTagLink.getStyle()
            .set("font-size", "13px")
            .set("color", "#60A5FA")
            .set("font-weight", "500");

        addTagLayout.add(tagIcon, addTagLink);
        addTagLayout.addClickListener(e -> openTagSelectorDialog());

        tagsHeader.add(tagsLabel, addTagLayout);

        // Container to show currently selected tags as pills
        selectedTagsContainer = new Div();
        selectedTagsContainer.setWidthFull();
        selectedTagsContainer.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "6px");

        tagsContainer.add(tagsHeader, selectedTagsContainer);

        container.add(goalRow, notesContainer, tagsContainer);
        return container;
    }

    private void loadMetaValues() {
        if (transactionMetaService == null) {
            return;
        }
        // Goal
        selectedGoal = bankAccountService.getGoalForTransaction(transaction);
        updateGoalDisplay();

        String note = transactionMetaService.getNoteForTransaction(transaction);
        if (notesField != null) {
            notesField.setValue(note != null ? note : "");
        }

        List<String> tags = transactionMetaService.getTagsForTransaction(transaction);
        selectedTags.clear();
        selectedTags.addAll(tags);
        updateSelectedTagsDisplay();
    }

    private void updateGoalDisplay() {
        if (goalValueLabel == null) {
            return;
        }
        if (selectedGoal == null) {
            goalValueLabel.setText("No goal");
        } else {
            goalValueLabel.setText(selectedGoal.getGoalName());
        }
    }

    private void updateSelectedTagsDisplay() {
        if (selectedTagsContainer == null) {
            return;
        }
        selectedTagsContainer.removeAll();
        if (selectedTags.isEmpty()) {
            return;
        }
        for (String tag : selectedTags) {
            Span pill = new Span(tag);
            pill.getStyle()
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("background", "rgba(55, 65, 81, 0.9)")
                .set("color", "#E5E7EB")
                .set("font-size", "11px");
            selectedTagsContainer.add(pill);
        }
    }

    private void openTagSelectorDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tags");
        dialog.setWidth("320px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("gap", "8px");

        // Input to add a new tag ad-hoc
        TextField addTagField = new TextField();
        addTagField.setPlaceholder("Add tags...");
        addTagField.setWidthFull();
        addTagField.getStyle()
            .set("background", "#020617")
            .set("border-radius", "8px");

        addTagField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> {
            String value = addTagField.getValue();
            if (value != null && !value.trim().isEmpty()) {
                selectedTags.add(value.trim());
                updateSelectedTagsDisplay();
                addTagField.clear();
            }
        });

        content.add(addTagField);

        // Existing tags with checkboxes
        java.util.List<String> allTags = transactionMetaService.getAllTagNames();
        for (String tagName : allTags) {
            Checkbox cb = new Checkbox(tagName);
            cb.setValue(selectedTags.contains(tagName));
            cb.getStyle()
                .set("font-size", "13px")
                .set("color", "#E5E7EB");
            cb.addValueChangeListener(e -> {
                if (e.getValue()) {
                    selectedTags.add(tagName);
                } else {
                    selectedTags.remove(tagName);
                }
                updateSelectedTagsDisplay();
            });
            content.add(cb);
        }

        // View tag settings link (opens simple management dialog)
        Button settingsButton = new Button("View tag settings", e -> openTagSettingsDialog());
        settingsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        settingsButton.getStyle()
            .set("margin-top", "6px")
            .set("color", "#9CA3AF");
        content.add(settingsButton);

        dialog.add(content);

        Button close = new Button("Done", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(close);

        dialog.open();
    }

    private void openGoalSelectorDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Select goal");
        dialog.setWidth("320px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("gap", "8px");

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search...");
        searchField.setWidthFull();
        searchField.getStyle()
            .set("background", "#020617")
            .set("border-radius", "8px");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.getStyle().set("gap", "4px");

        java.util.List<com.budgetplanner.budget.model.SavingsGoal> goals =
                bankAccountService.getAllActiveSavingsGoals();

        java.util.function.Consumer<String> refreshList = filter -> {
            list.removeAll();
            String lower = filter != null ? filter.toLowerCase() : "";
            for (com.budgetplanner.budget.model.SavingsGoal goal : goals) {
                if (!lower.isEmpty() && (goal.getGoalName() == null
                        || !goal.getGoalName().toLowerCase().contains(lower))) {
                    continue;
                }
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setSpacing(true);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.getStyle()
                    .set("padding", "6px 8px")
                    .set("border-radius", "8px")
                    .set("cursor", "pointer");

                // Icon based on SavingsGoal.iconName
                Icon iconComponent = VaadinIcon.FLAG.create();
                String iconName = goal.getIconName();
                if (iconName != null && !iconName.isBlank()) {
                    try {
                        VaadinIcon vi = VaadinIcon.valueOf(iconName);
                        iconComponent = vi.create();
                    } catch (IllegalArgumentException ignored) {
                        // Fallback to default icon
                    }
                }
                iconComponent.setSize("18px");
                iconComponent.getStyle().set("color", "#60A5FA");

                // Name and optional description
                VerticalLayout textCol = new VerticalLayout();
                textCol.setPadding(false);
                textCol.setSpacing(false);

                Span name = new Span(goal.getGoalName());
                name.getStyle()
                    .set("font-size", "13px")
                    .set("color", "#E5E7EB");
                textCol.add(name);

                // Progress percentage
                int pct = goal.getProgressPercentage();
                Span progress = new Span(pct + "%");
                progress.getStyle()
                    .set("font-size", "11px")
                    .set("color", "#9CA3AF");

                row.add(iconComponent, textCol, progress);
                row.addClickListener(e -> {
                    selectedGoal = goal;
                    updateGoalDisplay();
                    dialog.close();
                });

                list.add(row);
            }
        };

        refreshList.accept("");

        searchField.addValueChangeListener(e -> refreshList.accept(e.getValue()));

        content.add(searchField, list);
        dialog.add(content);

        Button clearButton = new Button("Clear", e -> {
            selectedGoal = null;
            updateGoalDisplay();
            dialog.close();
        });
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(clearButton, closeButton);
        dialog.open();
    }

    private void openTagSettingsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tags");
        dialog.setWidth("420px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("gap", "10px");

        Span description = new Span("Use tags to categorize your transactions more granularly.");
        description.getStyle()
            .set("font-size", "13px")
            .set("color", "#9CA3AF");
        content.add(description);

        // List of all tags
        java.util.List<String> allTags = transactionMetaService.getAllTagNames();
        for (String tagName : allTags) {
            Div row = new Div();
            row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("padding", "8px 10px")
                .set("background", "#020617")
                .set("border-radius", "8px");

            Span nameSpan = new Span(tagName);
            nameSpan.getStyle()
                .set("font-size", "13px")
                .set("color", "#E5E7EB");

            row.add(nameSpan);
            content.add(row);
        }

        // New tag input to quickly seed new tags (applied to current tx only on save)
        TextField newTagField = new TextField();
        newTagField.setPlaceholder("New tag");
        newTagField.setWidthFull();

        Button addTagButton = new Button("Add", e -> {
            String value = newTagField.getValue();
            if (value != null && !value.trim().isEmpty()) {
                selectedTags.add(value.trim());
                updateSelectedTagsDisplay();
                newTagField.clear();
                dialog.close();
            }
        });
        addTagButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addRow = new HorizontalLayout(newTagField, addTagButton);
        addRow.setWidthFull();
        addRow.setSpacing(true);
        addRow.setAlignItems(FlexComponent.Alignment.END);
        content.add(addRow);

        dialog.add(content);

        Button close = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(close);

        dialog.open();
    }

    private Div createSimilarTransactionsSection() {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "12px");

        Span title = new Span("Similar transactions");
        title.getStyle()
            .set("font-size", "14px")
            .set("font-weight", "600")
            .set("color", "#E5E7EB");

        VerticalLayout list = new VerticalLayout();
        list.setWidthFull();
        list.setPadding(false);
        list.setSpacing(false);
        list.getStyle().set("gap", "8px");

        List<BankTransaction> similar = bankAccountService.getSimilarTransactions(transaction, 5);

        if (similar.isEmpty()) {
            Span empty = new Span("No similar transactions yet");
            empty.getStyle()
                .set("font-size", "12px")
                .set("color", "#6B7280");
            list.add(empty);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

            for (BankTransaction tx : similar) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.setAlignItems(FlexComponent.Alignment.CENTER);

                String leftText = tx.getTransactionDate().format(formatter) + " • " +
                        (tx.getMerchantName() != null ? tx.getMerchantName() : "Transaction");
                Span left = new Span(leftText);
                left.getStyle()
                    .set("font-size", "13px")
                    .set("color", "#D1D5DB");

                double usd = dashboardDataService.convertToUSD(Math.abs(tx.getAmount()));
                String amountStr = dashboardDataService.formatUSD(usd);
                Span right = new Span(amountStr);
                right.getStyle()
                    .set("font-size", "13px")
                    .set("font-weight", "500")
                    .set("color", "#E5E7EB");

                row.add(left, right);
                list.add(row);
            }
        }

        container.add(title, list);
        return container;
    }

    private String merchantNameOrFallback() {
        return transaction.getMerchantName() != null && !transaction.getMerchantName().isEmpty()
            ? transaction.getMerchantName()
            : "Transaction";
    }

    private HorizontalLayout createActionButtons() {
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setSpacing(true);
        buttons.getStyle()
            .set("margin-top", "10px")
            .set("gap", "10px");
        
        Button closeBtn = new Button("Close", new Icon(VaadinIcon.CLOSE_CIRCLE));
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeBtn.getStyle()
            .set("color", "rgba(255, 255, 255, 0.7)")
            .set("border", "1px solid rgba(255, 255, 255, 0.2)")
            .set("border-radius", "8px");
        closeBtn.addClickListener(e -> {
            if (transactionMetaService != null && notesField != null) {
                transactionMetaService.saveNoteAndTags(
                    transaction,
                    notesField.getValue(),
                    String.join(", ", selectedTags)
                );
                // Persist goal selection
                bankAccountService.setGoalForTransaction(
                        transaction,
                        selectedGoal != null ? selectedGoal.getId() : null
                );
            }
            close();
        });
        
        buttons.add(closeBtn);
        return buttons;
    }
}
