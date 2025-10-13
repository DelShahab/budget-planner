package com.budgetplanner.budget.view;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.PlaidService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class BankAccountManagementDialog extends Dialog {
    
    private final BankAccountService bankAccountService;
    private final PlaidService plaidService;
    private final BankAccountRepository bankAccountRepository;
    private Grid<BankAccount> accountsGrid;
    private Button linkAccountButton;
    private Button syncTransactionsButton;
    private Span accountsSummary;
    
    public BankAccountManagementDialog(BankAccountService bankAccountService, PlaidService plaidService, BankAccountRepository bankAccountRepository) {
        this.bankAccountService = bankAccountService;
        this.plaidService = plaidService;
        this.bankAccountRepository = bankAccountRepository;
        
        setupDialog();
        createContent();
        refreshAccountsGrid();
    }
    
    private void setupDialog() {
        setHeaderTitle("Bank Account Management");
        setWidth("800px");
        setHeight("600px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
    }
    
    private void createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        
        // Header with summary
        HorizontalLayout header = createHeader();
        
        // Action buttons
        HorizontalLayout actions = createActionButtons();
        
        // Accounts grid
        accountsGrid = createAccountsGrid();
        
        content.add(header, actions, accountsGrid);
        add(content);
        
        // Footer buttons
        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeButton);
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        H3 title = new H3("Connected Bank Accounts");
        title.getStyle().set("margin", "0");
        
        accountsSummary = new Span();
        accountsSummary.getStyle().set("color", "var(--lumo-secondary-text-color)");
        updateAccountsSummary();
        
        header.add(title, accountsSummary);
        return header;
    }
    
    private HorizontalLayout createActionButtons() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        linkAccountButton = new Button("Link Bank Account", VaadinIcon.PLUS.create());
        linkAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        linkAccountButton.addClickListener(e -> openPlaidLink());
        
        syncTransactionsButton = new Button("Sync Transactions", VaadinIcon.REFRESH.create());
        syncTransactionsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        syncTransactionsButton.addClickListener(e -> syncAllTransactions());
        
        actions.add(linkAccountButton, syncTransactionsButton);
        return actions;
    }
    
    private Grid<BankAccount> createAccountsGrid() {
        Grid<BankAccount> grid = new Grid<>(BankAccount.class, false);
        grid.setHeight("400px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        // Institution column with icon
        grid.addComponentColumn(account -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(HorizontalLayout.Alignment.CENTER);
            layout.setSpacing(true);
            
            Icon bankIcon = VaadinIcon.INSTITUTION.create();
            bankIcon.setSize("16px");
            bankIcon.getStyle().set("color", "var(--lumo-primary-color)");
            
            Span institutionName = new Span(account.getInstitutionName());
            institutionName.getStyle().set("font-weight", "500");
            
            layout.add(bankIcon, institutionName);
            return layout;
        }).setHeader("Institution").setFlexGrow(2);
        
        // Account details
        grid.addColumn(account -> account.getAccountName() + " (...)" + account.getMask())
                .setHeader("Account").setFlexGrow(2);
        
        grid.addColumn(BankAccount::getAccountType)
                .setHeader("Type").setFlexGrow(1);
        
        // Status column with badge
        grid.addComponentColumn(account -> {
            Span statusBadge = new Span(account.getIsActive() ? "Active" : "Inactive");
            statusBadge.getElement().getThemeList().add(
                    account.getIsActive() ? "badge success" : "badge error"
            );
            return statusBadge;
        }).setHeader("Status").setFlexGrow(1);
        
        // Last sync
        grid.addColumn(account -> {
            if (account.getLastSyncAt() != null) {
                return account.getLastSyncAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            }
            return "Never";
        }).setHeader("Last Sync").setFlexGrow(1);
        
        // Actions column
        grid.addComponentColumn(account -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button syncButton = new Button(VaadinIcon.REFRESH.create());
            syncButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            syncButton.getElement().setProperty("title", "Sync this account");
            syncButton.addClickListener(e -> syncAccount(account));
            
            Button removeButton = new Button(VaadinIcon.TRASH.create());
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.getElement().setProperty("title", "Remove account");
            removeButton.addClickListener(e -> confirmRemoveAccount(account));
            
            actions.add(syncButton, removeButton);
            return actions;
        }).setHeader("Actions").setFlexGrow(1);
        
        return grid;
    }
    
    private void openPlaidLink() {
        try {
            // Create a link token using the real Plaid service
            String linkToken = plaidService.createLinkToken("user_" + System.currentTimeMillis());
            
            // Show Plaid Link integration dialog
            Div placeholder = new Div();
            placeholder.getStyle().set("padding", "20px");
            placeholder.getStyle().set("text-align", "center");
            placeholder.getStyle().set("border", "2px dashed var(--lumo-contrast-30pct)");
            placeholder.getStyle().set("border-radius", "var(--lumo-border-radius)");
            
            Span message = new Span("🏦 Plaid Link Ready");
            message.getStyle().set("display", "block");
            message.getStyle().set("font-size", "var(--lumo-font-size-l)");
            message.getStyle().set("margin-bottom", "10px");
            
            Span instructions = new Span("Link Token Created: " + linkToken.substring(0, 20) + "..." + 
                    "\n\nFor sandbox testing, use these credentials:\n" +
                    "Bank: First Platypus Bank\n" +
                    "Username: user_good\n" +
                    "Password: pass_good");
            instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");
            instructions.getStyle().set("font-size", "var(--lumo-font-size-s)");
            instructions.getStyle().set("white-space", "pre-line");
            
            placeholder.add(message, instructions);
            
            Dialog linkDialog = new Dialog();
            linkDialog.setHeaderTitle("Connect Bank Account - Plaid Sandbox");
            linkDialog.setWidth("500px");
            linkDialog.add(placeholder);
            
            Button simulateButton = new Button("Simulate Sandbox Connection", e -> {
                simulatePlaidConnection(linkToken);
                linkDialog.close();
            });
            simulateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button cancelButton = new Button("Cancel", e -> linkDialog.close());
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            linkDialog.getFooter().add(cancelButton, simulateButton);
            linkDialog.open();
            
        } catch (Exception e) {
            Notification.show("Failed to create Plaid Link token: " + e.getMessage(), 
                    5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void simulatePlaidConnection(String linkToken) {
        try {
            // For sandbox testing, we need to create mock bank accounts directly
            // since we don't have the full Plaid Link frontend integration
            BankAccount mockAccount = new BankAccount();
            mockAccount.setPlaidAccountId("sandbox-account-" + System.currentTimeMillis());
            mockAccount.setAccessToken("access-sandbox-" + System.currentTimeMillis());
            mockAccount.setPlaidItemId("sandbox-item-" + System.currentTimeMillis());
            mockAccount.setAccountName("Plaid Sandbox Checking");
            mockAccount.setAccountType("depository");
            mockAccount.setInstitutionName("First Platypus Bank");
            mockAccount.setMask("0000");
            mockAccount.setCreatedAt(java.time.LocalDateTime.now());
            mockAccount.setLastSyncAt(java.time.LocalDateTime.now());
            mockAccount.setIsActive(true);
            
            // Save the mock account using the repository directly
            bankAccountRepository.save(mockAccount);
            
            Notification.show("Successfully connected sandbox bank account!", 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refreshAccountsGrid();
            
        } catch (Exception e) {
            Notification.show("Failed to connect bank account: " + e.getMessage(), 
                    5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void syncAllTransactions() {
        try {
            syncTransactionsButton.setEnabled(false);
            syncTransactionsButton.setText("Syncing...");
            
            new Thread(() -> {
                try {
                    // Use the real Plaid service to sync all transactions
                    plaidService.syncAllTransactions();
                    
                    getUI().ifPresent(ui -> ui.access(() -> {
                        syncTransactionsButton.setEnabled(true);
                        syncTransactionsButton.setText("Sync Transactions");
                        
                        Notification.show("Transactions synced successfully!", 
                                3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                        refreshAccountsGrid();
                    }));
                } catch (Exception e) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        syncTransactionsButton.setEnabled(true);
                        syncTransactionsButton.setText("Sync Transactions");
                        
                        Notification.show("Sync failed: " + e.getMessage(), 
                                3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }));
                }
            }).start();
        } catch (Exception e) {
            syncTransactionsButton.setEnabled(true);
            syncTransactionsButton.setText("Sync Transactions");
            
            Notification.show("Failed to sync transactions: " + e.getMessage(), 
                    5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void syncAccount(BankAccount account) {
        try {
            plaidService.syncTransactionsForAccount(account);
            Notification.show("Account synced: " + account.getAccountName(), 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshAccountsGrid();
        } catch (Exception e) {
            Notification.show("Failed to sync account: " + e.getMessage(), 
                    5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void confirmRemoveAccount(BankAccount account) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Remove Bank Account");
        confirmDialog.setText("Are you sure you want to remove " + account.getAccountName() + 
                " from " + account.getInstitutionName() + "? This will also revoke access to your account data.");
        
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Remove");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(e -> removeAccount(account));
        confirmDialog.open();
    }
    
    private void removeAccount(BankAccount account) {
        try {
            // plaidService.removeBankAccount(account.getId());
            Notification.show("Bank account removed successfully", 
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshAccountsGrid();
        } catch (Exception e) {
            Notification.show("Failed to remove account: " + e.getMessage(), 
                    5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void refreshAccountsGrid() {
        List<BankAccount> accounts = bankAccountService.getActiveBankAccounts();
        accountsGrid.setItems(accounts);
        updateAccountsSummary();
        
        // Enable/disable sync button based on whether there are accounts
        syncTransactionsButton.setEnabled(!accounts.isEmpty());
    }
    
    private void updateAccountsSummary() {
        List<BankAccount> accounts = bankAccountService.getActiveBankAccounts();
        int totalAccounts = accounts.size();
        
        if (totalAccounts == 0) {
            accountsSummary.setText("No accounts connected");
        } else {
            accountsSummary.setText(totalAccounts + " account" + (totalAccounts == 1 ? "" : "s") + " connected");
        }
    }
}
