package com.budgetplanner.budget;

import com.budgetplanner.budget.model.RecurringTransaction;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.AvatarHelper;
import com.budgetplanner.budget.util.CurrencyFormatter;
import com.budgetplanner.budget.service.RecurringTransactionService;
import com.budgetplanner.budget.view.EditRecurringTransactionDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Vaadin view for managing recurring transactions
 */
@Route(value = "recurring-transactions")
@PageTitle("Recurring Transactions | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class RecurringTransactionView extends VerticalLayout {

    private final RecurringTransactionService recurringTransactionService;
    private final DashboardDataService dashboardDataService;
    private final UserSessionService userSessionService;
    
    private Grid<RecurringTransaction> activeGrid;
    private Grid<RecurringTransaction> dueSoonGrid;
    private Grid<RecurringTransaction> overdueGrid;
    private Div summaryCards;
    private Tabs tabSheet;
    private VerticalLayout contentArea;

    @Autowired
    public RecurringTransactionView(RecurringTransactionService recurringTransactionService,
                                     DashboardDataService dashboardDataService,
                                     UserSessionService userSessionService) {
        this.recurringTransactionService = recurringTransactionService;
        this.dashboardDataService = dashboardDataService;
        this.userSessionService = userSessionService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("recurring-transaction-view");
        
        // Apply modern dashboard dark theme
        getStyle()
            .set("background", "#0f0a1e")
            .set("color", "white");
        
        // Create split layout with sidebar and content
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(5);
        splitLayout.addToPrimary(createSidebar());
        
        // Create main content area
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        mainContent.getStyle().set("padding", "40px");
        
        createHeader(mainContent);
        createSummaryCards(mainContent);
        createTabSheet(mainContent);
        createContentArea(mainContent);
        
        splitLayout.addToSecondary(mainContent);
        add(splitLayout);
        
        refreshData();
    }

    private Div createSidebar() {
        Div sidebar = new Div();
        sidebar.addClassName("sidebar");
        sidebar.getStyle()
            .set("background", "#171521")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center")
            .set("padding", "20px 0")
            .set("width", "90px")
            .set("height", "100vh");
        sidebar.getStyle().set("gap", "35px");

        // Logo at top - show user avatar if available
        Div logo = AvatarHelper.createAvatarLogo(userSessionService);

        // Navigation icons container
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle().set("gap", "30px");

        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", false);
        homeBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));
        
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        trendsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("trends")));
        
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", true); // Active!
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        savingsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("savings")));
        
        Button planBtn = createNavButton(VaadinIcon.CALENDAR, "Monthly Plan", false);
        planBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("monthly-plan")));

        Button notificationsBtn = createNavButton(VaadinIcon.STAR, "Notifications", false);
        notificationsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("notifications")));
        
        Button userBtn = createNavButton(VaadinIcon.USER, "Profile", false);
        userBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("user-settings")));
        
        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", false);
        historyBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("history")));
        
        Button settingsBtn = createNavButton(VaadinIcon.COG, "Settings", false);

        navContainer.add(homeBtn, trendsBtn, recurringBtn, savingsBtn, planBtn, notificationsBtn, userBtn, historyBtn, settingsBtn);
        
        sidebar.add(logo, navContainer);
        return sidebar;
    }

    private Button createNavButton(VaadinIcon icon, String title, boolean active) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        btn.addClassName("nav-button");
        btn.getElement().setProperty("title", title);
        
        if (active) {
            btn.addClassName("active");
        }

        return btn;
    }

    private void createHeader(VerticalLayout container) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("margin-bottom", "30px")
            .set("padding", "20px")
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px");
        
        H2 title = new H2("Recurring Transactions");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button analyzeButton = new Button("Analyze Patterns", new Icon(VaadinIcon.SEARCH));
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px");
        analyzeButton.addClickListener(e -> analyzePatterns());
        
        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.getStyle()
            .set("border-radius", "10px");
        refreshButton.addClickListener(e -> refreshData());
        
        actions.add(analyzeButton, refreshButton);
        header.add(title, actions);
        
        container.add(header);
    }

    private void createSummaryCards(VerticalLayout container) {
        summaryCards = new Div();
        summaryCards.addClassName("summary-cards"); // Use same class as BudgetView
        summaryCards.addClassName("mobile-responsive-cards"); // Use same responsive class
        summaryCards.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(250px, 1fr))")
            .set("gap", "var(--lumo-space-m)")
            .set("margin-bottom", "var(--lumo-space-l)");
        
        container.add(summaryCards);
    }

    private void createTabSheet(VerticalLayout container) {
        tabSheet = new Tabs();
        tabSheet.getStyle()
            .set("background", "rgb(27 23 42)")
            .set("border-radius", "15px")
            .set("padding", "10px 0 10px 0px")
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        Tab activeTab = new Tab("Active Recurring");
        Tab dueSoonTab = new Tab("Due Soon");
        Tab overdueTab = new Tab("Overdue");
        
        // Style tabs
        // activeTab.getStyle().set("color", "white");
        // dueSoonTab.getStyle().set("color", "white");
        // overdueTab.getStyle().set("color", "white");
        
        tabSheet.add(activeTab, dueSoonTab, overdueTab);
        tabSheet.setSelectedTab(activeTab);
        
        tabSheet.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            updateContentForTab(selectedTab);
        });
        
        container.add(tabSheet);
    }

    private void createContentArea(VerticalLayout container) {
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(true);
        
        // Create grids
        createActiveGrid();
        createDueSoonGrid();
        createOverdueGrid();
        
        // Initially show active grid
        contentArea.add(activeGrid);
        
        container.add(contentArea);
    }

    private void createActiveGrid() {
        activeGrid = new Grid<>(RecurringTransaction.class, false);
        activeGrid.addClassName("recurring-transactions-grid");
        activeGrid.setSizeFull();
        
        // Apply modern dark grid styling
        activeGrid.getStyle()
            .set("background", "#171521")
            .set("border-radius", "15px")
            .set("padding", "20px");
        
        activeGrid.addColumn(RecurringTransaction::getMerchantName)
            .setHeader("Merchant")
            .setSortable(true)
            .setFlexGrow(2);
        
        activeGrid.addColumn(transaction -> {
            double usd = dashboardDataService.convertToUSD(Math.abs(transaction.getAmount()));
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Amount")
            .setSortable(true)
            .setWidth("120px");
        
        activeGrid.addColumn(RecurringTransaction::getRecurrenceDescription)
            .setHeader("Frequency")
            .setSortable(true)
            .setWidth("150px");
        
        activeGrid.addColumn(transaction -> transaction.getBudgetCategoryType())
            .setHeader("Category")
            .setSortable(true)
            .setWidth("120px");
        
        activeGrid.addColumn(transaction -> 
            transaction.getNextExpectedDate() != null ? 
            transaction.getNextExpectedDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A")
            .setHeader("Next Expected")
            .setSortable(true)
            .setWidth("140px");
        
        activeGrid.addColumn(transaction -> String.format("%.1f%%", transaction.getConfidenceScore() * 100))
            .setHeader("Confidence")
            .setSortable(true)
            .setWidth("100px");
        
        activeGrid.addColumn(new ComponentRenderer<>(this::createStatusBadge))
            .setHeader("Status")
            .setWidth("120px");
        
        activeGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
            .setHeader("Actions")
            .setWidth("150px");
    }

    private void createDueSoonGrid() {
        dueSoonGrid = new Grid<>(RecurringTransaction.class, false);
        dueSoonGrid.addClassName("due-soon-grid");
        dueSoonGrid.setSizeFull();
        
        dueSoonGrid.addColumn(RecurringTransaction::getMerchantName)
            .setHeader("Merchant")
            .setSortable(true)
            .setFlexGrow(2);
        
        dueSoonGrid.addColumn(transaction -> {
            double usd = dashboardDataService.convertToUSD(Math.abs(transaction.getAmount()));
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Amount")
            .setSortable(true)
            .setWidth("120px");
        
        dueSoonGrid.addColumn(transaction -> 
            transaction.getNextExpectedDate() != null ? 
            transaction.getNextExpectedDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A")
            .setHeader("Due Date")
            .setSortable(true)
            .setWidth("140px");
        
        dueSoonGrid.addColumn(RecurringTransaction::getRecurrenceDescription)
            .setHeader("Frequency")
            .setSortable(true)
            .setWidth("150px");
        
        dueSoonGrid.addColumn(transaction -> transaction.getBudgetCategoryType())
            .setHeader("Category")
            .setSortable(true)
            .setWidth("120px");
        
        dueSoonGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
            .setHeader("Actions")
            .setWidth("150px");
    }

    private void createOverdueGrid() {
        overdueGrid = new Grid<>(RecurringTransaction.class, false);
        overdueGrid.addClassName("overdue-grid");
        overdueGrid.setSizeFull();
        
        overdueGrid.addColumn(RecurringTransaction::getMerchantName)
            .setHeader("Merchant")
            .setSortable(true)
            .setFlexGrow(2);
        
        overdueGrid.addColumn(transaction -> {
            double usd = dashboardDataService.convertToUSD(Math.abs(transaction.getAmount()));
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Amount")
            .setSortable(true)
            .setWidth("120px");
        
        overdueGrid.addColumn(transaction -> 
            transaction.getNextExpectedDate() != null ? 
            transaction.getNextExpectedDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A")
            .setHeader("Expected Date")
            .setSortable(true)
            .setWidth("140px");
        
        overdueGrid.addColumn(RecurringTransaction::getRecurrenceDescription)
            .setHeader("Frequency")
            .setSortable(true)
            .setWidth("150px");
        
        overdueGrid.addColumn(transaction -> transaction.getBudgetCategoryType())
            .setHeader("Category")
            .setSortable(true)
            .setWidth("120px");
        
        overdueGrid.addColumn(new ComponentRenderer<>(this::createOverdueActionButtons))
            .setHeader("Actions")
            .setWidth("180px");
    }

    private Span createStatusBadge(RecurringTransaction transaction) {
        Span badge = new Span(transaction.getStatus().toString());
        badge.getElement().getThemeList().add("badge");
        
        switch (transaction.getStatus()) {
            case ACTIVE:
                badge.getElement().getThemeList().add("success");
                break;
            case PENDING_CONFIRMATION:
                badge.getElement().getThemeList().add("contrast");
                break;
            case PAUSED:
                badge.getElement().getThemeList().add("primary");
                break;
            case ENDED:
            case IRREGULAR:
                badge.getElement().getThemeList().add("error");
                break;
        }
        
        return badge;
    }

    private HorizontalLayout createActionButtons(RecurringTransaction transaction) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "Edit");
        editButton.addClickListener(e -> editTransaction(transaction));
        
        Button pauseButton = new Button(new Icon(VaadinIcon.PAUSE));
        pauseButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        pauseButton.getElement().setAttribute("title", "Pause");
        pauseButton.addClickListener(e -> pauseTransaction(transaction));
        
        actions.add(editButton, pauseButton);
        return actions;
    }

    private HorizontalLayout createOverdueActionButtons(RecurringTransaction transaction) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button markPaidButton = new Button("Mark Paid", new Icon(VaadinIcon.CHECK));
        markPaidButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        markPaidButton.addClickListener(e -> markAsPaid(transaction));
        
        Button skipButton = new Button("Skip", new Icon(VaadinIcon.FORWARD));
        skipButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        skipButton.addClickListener(e -> skipOccurrence(transaction));
        
        actions.add(markPaidButton, skipButton);
        return actions;
    }

    private void updateContentForTab(Tab selectedTab) {
        contentArea.removeAll();
        
        String tabText = selectedTab.getLabel();
        switch (tabText) {
            case "Active Recurring":
                contentArea.add(activeGrid);
                break;
            case "Due Soon":
                contentArea.add(dueSoonGrid);
                break;
            case "Overdue":
                contentArea.add(overdueGrid);
                break;
        }
    }

    private void refreshData() {
        try {
            // Load active transactions
            List<RecurringTransaction> activeTransactions = 
                recurringTransactionService.getAllActiveRecurringTransactions();
            activeGrid.setItems(activeTransactions);
            
            // Load due soon transactions
            List<RecurringTransaction> dueSoonTransactions = 
                recurringTransactionService.getTransactionsDueSoon(7);
            dueSoonGrid.setItems(dueSoonTransactions);
            
            // Load overdue transactions
            List<RecurringTransaction> overdueTransactions = 
                recurringTransactionService.getOverdueTransactions();
            overdueGrid.setItems(overdueTransactions);
            
            // Update summary cards
            updateSummaryCards(activeTransactions, dueSoonTransactions, overdueTransactions);
            
        } catch (Exception e) {
            Notification.show("Error loading recurring transactions: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateSummaryCards(List<RecurringTransaction> active, 
                                  List<RecurringTransaction> dueSoon, 
                                  List<RecurringTransaction> overdue) {
        summaryCards.removeAll();
        
        // Total Active Card
        Div activeCard = createSummaryCard(
            "Active Recurring", 
            String.valueOf(active.size()),
            VaadinIcon.REFRESH,
            "var(--lumo-success-color)"
        );
        
        // Due Soon Card
        Div dueSoonCard = createSummaryCard(
            "Due Soon (7 days)", 
            String.valueOf(dueSoon.size()),
            VaadinIcon.CLOCK,
            "var(--lumo-warning-color)"
        );
        
        // Overdue Card
        Div overdueCard = createSummaryCard(
            "Overdue", 
            String.valueOf(overdue.size()),
            VaadinIcon.WARNING,
            "var(--lumo-error-color)"
        );
        
        // Monthly Total Card
        Map<String, Double> monthlyTotals = recurringTransactionService.getMonthlyTotalsByCategory();
        double totalMonthlyIdr = monthlyTotals.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalMonthlyUsd = dashboardDataService.convertToUSD(totalMonthlyIdr);
        
        Div monthlyCard = createSummaryCard(
            "Monthly Total", 
            dashboardDataService.formatUSD(totalMonthlyUsd),
            VaadinIcon.DOLLAR,
            "var(--lumo-primary-color)"
        );
        
        summaryCards.add(activeCard, dueSoonCard, overdueCard, monthlyCard);
    }

    private Div createSummaryCard(String title, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.addClassName("summary-card"); // Use same class as BudgetView
        
        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("2em");
        cardIcon.setColor(color);
        
        Span cardTitle = new Span(title);
        cardTitle.addClassName("card-title"); // Use same class as BudgetView
        
        Span cardValue = new Span(value);
        cardValue.addClassName("card-value"); // Use same class as BudgetView
        cardValue.getStyle().set("color", color);
        
        card.add(cardIcon, cardTitle, cardValue);
        return card;
    }

    private void analyzePatterns() {
        try {
            recurringTransactionService.analyzeAllTransactionsForRecurringPatterns();
            Notification.show("Pattern analysis started in background. Refresh in a few moments to see results.")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error starting pattern analysis: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void editTransaction(RecurringTransaction transaction) {
        EditRecurringTransactionDialog dialog = new EditRecurringTransactionDialog(
            transaction, 
            recurringTransactionService,
            this::refreshData
        );
        dialog.open();
    }

    private void pauseTransaction(RecurringTransaction transaction) {
        try {
            if (transaction.getStatus() == RecurringTransaction.RecurringStatus.ACTIVE) {
                transaction.setStatus(RecurringTransaction.RecurringStatus.PAUSED);
                recurringTransactionService.updateRecurringTransaction(transaction.getId(), transaction);
                
                Notification.show("Transaction paused: " + transaction.getMerchantName())
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else if (transaction.getStatus() == RecurringTransaction.RecurringStatus.PAUSED) {
                transaction.setStatus(RecurringTransaction.RecurringStatus.ACTIVE);
                recurringTransactionService.updateRecurringTransaction(transaction.getId(), transaction);
                
                Notification.show("Transaction resumed: " + transaction.getMerchantName())
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            refreshData();
        } catch (Exception e) {
            Notification.show("Error updating transaction: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void markAsPaid(RecurringTransaction transaction) {
        try {
            // Record the payment and calculate next expected date
            LocalDate today = LocalDate.now();
            transaction.setLastOccurrence(today);
            transaction.setOccurrenceCount(transaction.getOccurrenceCount() + 1);
            
            // Calculate next expected date based on frequency
            LocalDate nextDate = calculateNextExpectedDate(transaction, today);
            transaction.setNextExpectedDate(nextDate);
            
            // Update status if it was overdue
            if (transaction.getStatus() == RecurringTransaction.RecurringStatus.PENDING_CONFIRMATION) {
                transaction.setStatus(RecurringTransaction.RecurringStatus.ACTIVE);
            }
            
            recurringTransactionService.updateRecurringTransaction(transaction.getId(), transaction);
            
            Notification.show(String.format("Payment recorded for %s. Next due: %s", 
                transaction.getMerchantName(), 
                nextDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))))
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
            refreshData();
        } catch (Exception e) {
            Notification.show("Error recording payment: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void skipOccurrence(RecurringTransaction transaction) {
        try {
            // Skip this occurrence and calculate next expected date
            LocalDate currentExpected = transaction.getNextExpectedDate();
            LocalDate nextDate = calculateNextExpectedDate(transaction, currentExpected);
            transaction.setNextExpectedDate(nextDate);
            
            // Add note about skipped occurrence
            String skipNote = String.format("Skipped occurrence on %s", 
                currentExpected.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
            String existingNotes = transaction.getNotes();
            if (existingNotes != null && !existingNotes.isEmpty()) {
                transaction.setNotes(existingNotes + "; " + skipNote);
            } else {
                transaction.setNotes(skipNote);
            }
            
            recurringTransactionService.updateRecurringTransaction(transaction.getId(), transaction);
            
            Notification.show(String.format("Skipped %s. Next due: %s", 
                transaction.getMerchantName(), 
                nextDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))))
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
            refreshData();
        } catch (Exception e) {
            Notification.show("Error skipping occurrence: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private LocalDate calculateNextExpectedDate(RecurringTransaction transaction, LocalDate fromDate) {
        switch (transaction.getFrequency()) {
            case WEEKLY:
                return fromDate.plusWeeks(1);
            case BI_WEEKLY:
                return fromDate.plusWeeks(2);
            case MONTHLY:
                return fromDate.plusMonths(1);
            case BI_MONTHLY:
                return fromDate.plusMonths(2);
            case QUARTERLY:
                return fromDate.plusMonths(3);
            case SEMI_ANNUALLY:
                return fromDate.plusMonths(6);
            case ANNUALLY:
                return fromDate.plusYears(1);
            case CUSTOM:
                return fromDate.plusDays(transaction.getIntervalDays());
            default:
                return fromDate.plusMonths(1);
        }
    }
}
