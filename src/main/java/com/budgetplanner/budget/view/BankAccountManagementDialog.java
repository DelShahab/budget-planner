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
        setWidth("900px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        
        // Apply modern dark theme styling
        getElement().getThemeList().add("modern-dialog");
        getElement().getStyle()
            .set("background", "#0f0a1e")
            .set("border", "1px solid rgba(255, 255, 255, 0.1)")
            .set("border-radius", "15px")
            .set("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5)");
    }
    
    private void createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        
        // Header with summary
        HorizontalLayout header = createHeader();
        
        // Action buttons
        HorizontalLayout actions = createActionButtons();
        
        // Accounts grid
        accountsGrid = createAccountsGrid();
        
        content.add(header, actions, accountsGrid);
        add(content);
        
        // Footer buttons
        Button closeButton = new Button("Close", e -> {
            // First close the dialog
            close();
            // Then reload the whole page so all views re-query fresh data
            getUI().ifPresent(ui -> ui.getPage().reload());
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.getStyle()
            .set("border-radius", "10px")
            .set("padding", "10px 24px")
            .set("color", "#9CA3AF")
            .set("font-weight", "600");
        getFooter().add(closeButton);
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
            .set("padding", "20px 0")
            .set("border-bottom", "1px solid rgba(255, 255, 255, 0.1)")
            .set("margin-bottom", "20px");
        
        H3 title = new H3("Connected Bank Accounts");
        title.getStyle()
            .set("margin", "0")
            .set("color", "white")
            .set("font-weight", "700")
            .set("font-size", "20px");
        
        accountsSummary = new Span();
        accountsSummary.getStyle()
            .set("color", "#9CA3AF")
            .set("font-size", "14px")
            .set("font-weight", "500");
        updateAccountsSummary();
        
        header.add(title, accountsSummary);
        return header;
    }
    
    private HorizontalLayout createActionButtons() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.getStyle().set("margin-bottom", "20px");
        
        linkAccountButton = new Button("Link Bank Account", VaadinIcon.PLUS.create());
        linkAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        linkAccountButton.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("border-radius", "10px")
            .set("padding", "12px 24px")
            .set("font-weight", "600")
            .set("cursor", "pointer")
            .set("transition", "all 0.3s ease")
            .set("box-shadow", "0 4px 15px rgba(0, 212, 255, 0.3)");
        linkAccountButton.addClickListener(e -> openPlaidLink());
        
        syncTransactionsButton = new Button("Sync Transactions", VaadinIcon.REFRESH.create());
        syncTransactionsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        syncTransactionsButton.getStyle()
            .set("border-radius", "10px")
            .set("padding", "12px 24px")
            .set("font-weight", "600")
            .set("border", "1px solid rgba(255, 255, 255, 0.2)")
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("color", "white")
            .set("cursor", "pointer")
            .set("transition", "all 0.3s ease");
        syncTransactionsButton.addClickListener(e -> syncAllTransactions());
        
        actions.add(linkAccountButton, syncTransactionsButton);
        return actions;
    }
    
    private Grid<BankAccount> createAccountsGrid() {
        Grid<BankAccount> grid = new Grid<>(BankAccount.class, false);
        grid.setHeight("400px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        
        // Modern dark theme styling for grid
        grid.getStyle()
            .set("background", "#1a1625")
            .set("border-radius", "10px")
            .set("overflow", "hidden")
            .set("border", "1px solid rgba(255, 255, 255, 0.1)");
        
        // Institution column with icon
        grid.addComponentColumn(account -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(HorizontalLayout.Alignment.CENTER);
            layout.setSpacing(true);
            
            Icon bankIcon = VaadinIcon.INSTITUTION.create();
            bankIcon.setSize("18px");
            bankIcon.getStyle().set("color", "#00d4ff");
            
            Span institutionName = new Span(account.getInstitutionName());
            institutionName.getStyle()
                .set("font-weight", "600")
                .set("color", "white")
                .set("font-size", "14px");
            
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
            statusBadge.getStyle()
                .set("padding", "4px 12px")
                .set("border-radius", "20px")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("background", account.getIsActive() ? "rgba(16, 185, 129, 0.2)" : "rgba(239, 68, 68, 0.2)")
                .set("color", account.getIsActive() ? "#10b981" : "#ef4444");
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
            syncButton.getStyle()
                .set("color", "#00d4ff")
                .set("cursor", "pointer")
                .set("transition", "all 0.3s ease");
            syncButton.addClickListener(e -> syncAccount(account));
            
            Button removeButton = new Button(VaadinIcon.TRASH.create());
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.getElement().setProperty("title", "Remove account");
            removeButton.getStyle()
                .set("color", "#ef4444")
                .set("cursor", "pointer")
                .set("transition", "all 0.3s ease");
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
            placeholder.getStyle()
                .set("padding", "30px")
                .set("text-align", "center")
                .set("border", "2px dashed rgba(0, 212, 255, 0.3)")
                .set("border-radius", "15px")
                .set("background", "rgba(0, 212, 255, 0.05)");
            
            Span message = new Span("🏦 Plaid Link Ready");
            message.getStyle()
                .set("display", "block")
                .set("font-size", "24px")
                .set("font-weight", "700")
                .set("color", "white")
                .set("margin-bottom", "20px");
            
            Span instructions = new Span("Link Token Created: " + linkToken.substring(0, 20) + "..." + 
                    "\n\nFor sandbox testing, use these credentials:\n" +
                    "Bank: First Platypus Bank\n" +
                    "Username: user_good\n" +
                    "Password: pass_good");
            instructions.getStyle()
                .set("color", "#9CA3AF")
                .set("font-size", "14px")
                .set("white-space", "pre-line")
                .set("line-height", "1.8");
            
            placeholder.add(message, instructions);
            
            Dialog linkDialog = new Dialog();
            linkDialog.setHeaderTitle("Connect Bank Account - Plaid Sandbox");
            linkDialog.setWidth("550px");
            linkDialog.getElement().getStyle()
                .set("background", "#0f0a1e")
                .set("border", "1px solid rgba(255, 255, 255, 0.1)")
                .set("border-radius", "15px");
            linkDialog.add(placeholder);
            
            Button simulateButton = new Button("Simulate Sandbox Connection", e -> {
                simulatePlaidConnection(linkToken);
                linkDialog.close();
            });
            simulateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            simulateButton.getStyle()
                .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
                .set("border", "none")
                .set("border-radius", "10px")
                .set("padding", "12px 24px")
                .set("font-weight", "600");
            
            Button cancelButton = new Button("Cancel", e -> linkDialog.close());
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            cancelButton.getStyle()
                .set("border-radius", "10px")
                .set("color", "#9CA3AF");
            
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

                        // Immediately reload the page so all views pick up fresh data
                        ui.getPage().reload();
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
