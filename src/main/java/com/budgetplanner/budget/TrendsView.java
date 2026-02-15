package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.TransactionMetaService;
import com.budgetplanner.budget.service.RecurringTransactionService;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.CurrencyFormatter;
import com.budgetplanner.budget.view.TransactionDetailsDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.server.StreamResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vaadin view for displaying user trends and activity
 */
@Route(value = "trends")
@PageTitle("Trends & Activity | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class TrendsView extends VerticalLayout {

    private final BudgetItemRepository budgetItemRepository;
    private final BankAccountService bankAccountService;
    private final BankTransactionRepository bankTransactionRepository;
    private final DashboardDataService dashboardDataService;
    private final UserSessionService userSessionService;
    private final TransactionMetaService transactionMetaService;
    private final RecurringTransactionService recurringTransactionService;
    
    private Grid<BankTransaction> allActivityGrid;
    private Grid<BudgetItem> incomeGrid;
    private Grid<BudgetItem> expensesGrid;
    private Grid<BudgetItem> billsGrid;
    private Grid<BudgetItem> savingsGrid;
    private Div summaryCards;
    private Tabs categoryTabs;
    private VerticalLayout contentArea;

    @Autowired
    public TrendsView(BudgetItemRepository budgetItemRepository,
                      BankAccountService bankAccountService,
                      BankTransactionRepository bankTransactionRepository,
                      DashboardDataService dashboardDataService,
                      UserSessionService userSessionService,
                      TransactionMetaService transactionMetaService,
                      RecurringTransactionService recurringTransactionService) {
        this.budgetItemRepository = budgetItemRepository;
        this.bankAccountService = bankAccountService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.dashboardDataService = dashboardDataService;
        this.userSessionService = userSessionService;
        this.transactionMetaService = transactionMetaService;
        this.recurringTransactionService = recurringTransactionService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("trends-view");
        
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
        createCategoryTabs(mainContent);
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
        Div logo = new Div();
        logo.getStyle()
            .set("width", "45px")
            .set("height", "45px")
            .set("border-radius", "50%")
            .set("overflow", "hidden")
            .set("margin", "20px auto 0");
        
        if (userSessionService.hasAvatar()) {
            Image img = new Image();
            byte[] avatarData = userSessionService.getAvatarFromSession();
            StreamResource resource = new StreamResource("avatar.png", 
                () -> new ByteArrayInputStream(avatarData));
            img.setSrc(resource);
            img.setWidth("45px");
            img.setHeight("45px");
            img.getStyle().set("object-fit", "cover");
            logo.add(img);
        } else {
            logo.getStyle()
                .set("background", "#01a1be")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "white")
                .set("font-size", "18px")
                .set("font-weight", "bold");
            Span logoText = new Span(userSessionService.getUserInitials());
            logo.add(logoText);
        }

        // Navigation icons container
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle().set("gap", "30px");

        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", false);
        homeBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));
        
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", true); // Active!
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));
        
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
        
        H2 title = new H2("Trends & Activity");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button addBudgetButton = new Button("Add Budget Item", new Icon(VaadinIcon.PLUS));
        addBudgetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBudgetButton.getStyle()
            .set("border-radius", "10px");
        addBudgetButton.addClickListener(e -> showAddBudgetItemDialog());

        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.getStyle()
            .set("border-radius", "10px");
        refreshButton.addClickListener(e -> refreshData());
        
        actions.add(addBudgetButton, refreshButton);
        header.add(title, actions);
        
        container.add(header);
    }

    private void createSummaryCards(VerticalLayout container) {
        summaryCards = new Div();
        summaryCards.addClassName("summary-cards");
        summaryCards.addClassName("mobile-responsive-cards");
        summaryCards.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(250px, 1fr))")
            .set("gap", "var(--lumo-space-m)")
            .set("margin-bottom", "var(--lumo-space-l)");
        
        container.add(summaryCards);
    }

    private void createCategoryTabs(VerticalLayout container) {
        categoryTabs = new Tabs();
        categoryTabs.getStyle()
            .set("background", "rgb(27 23 42)")
            .set("border-radius", "15px")
            .set("padding", "10px 0 10px 0")
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        Tab allActivityTab = new Tab("ALL ACTIVITY");
        Tab incomeTab = new Tab("INCOME");
        Tab expensesTab = new Tab("EXPENSES");
        Tab billsTab = new Tab("BILLS");
        Tab savingsTab = new Tab("SAVINGS");
        
        categoryTabs.add(allActivityTab, incomeTab, expensesTab, billsTab, savingsTab);
        categoryTabs.setSelectedTab(allActivityTab); // Default to all activity
        
        categoryTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            updateContentForTab(selectedTab);
        });
        
        container.add(categoryTabs);
    }

    private void createContentArea(VerticalLayout container) {
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(true);
        
        // Create grids for each category
        createAllActivityGrid();
        createIncomeGrid();
        createExpensesGrid();
        createBillsGrid();
        createSavingsGrid();
        
        // Initially show all activity grid
        contentArea.add(allActivityGrid);
        
        container.add(contentArea);
    }

    private void createAllActivityGrid() {
        allActivityGrid = createTransactionGrid();
    }
    
    private Grid<BankTransaction> createTransactionGrid() {
        Grid<BankTransaction> grid = new Grid<>(BankTransaction.class, false);
        grid.addClassName("transaction-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        
        // Apply modern dark grid styling
        grid.getStyle()
            .set("background", "#171521")
            .set("border-radius", "15px")
            .set("padding", "20px");
        
        // Add columns for transaction details
        grid.addColumn(transaction -> {
            LocalDate date = transaction.getTransactionDate();
            return date != null ? date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "N/A";
        })
            .setHeader("Date")
            .setSortable(true)
            .setWidth("120px");
        
        grid.addColumn(BankTransaction::getMerchantName)
            .setHeader("Merchant")
            .setSortable(true)
            .setFlexGrow(2);
        
        grid.addColumn(BankTransaction::getPlaidCategory)
            .setHeader("Category")
            .setSortable(true)
            .setWidth("150px");
        
        grid.addColumn(transaction -> {
            double usd = dashboardDataService.convertToUSD(transaction.getAmount());
            return dashboardDataService.formatUSD(Math.abs(usd));
        })
            .setHeader("Amount")
            .setSortable(true)
            .setWidth("120px");
        
        grid.addColumn(BankTransaction::getPlaidSubcategory)
            .setHeader("Subcategory")
            .setSortable(true)
            .setWidth("150px");
        
        // Add action column with view details button
        grid.addComponentColumn(transaction -> {
            Button viewDetailsBtn = new Button("View Details", new Icon(VaadinIcon.EYE));
            viewDetailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewDetailsBtn.addClickListener(e -> {
                double usd = dashboardDataService.convertToUSD(transaction.getAmount());
                String formattedAmount = dashboardDataService.formatUSD(Math.abs(usd));
                String categoryColor = getCategoryColor(transaction.getPlaidCategory());
                openTransactionDetailsDialog(transaction, formattedAmount, categoryColor);
            });
            return viewDetailsBtn;
        })
            .setHeader("Actions")
            .setWidth("150px")
            .setFlexGrow(0);
        
        return grid;
    }

    private void createIncomeGrid() {
        incomeGrid = createCategoryGrid();
    }

    private void createExpensesGrid() {
        expensesGrid = createCategoryGrid();
    }

    private void createBillsGrid() {
        billsGrid = createCategoryGrid();
    }

    private void createSavingsGrid() {
        savingsGrid = createCategoryGrid();
    }

    private Grid<BudgetItem> createCategoryGrid() {
        Grid<BudgetItem> grid = new Grid<>(BudgetItem.class, false);
        grid.addClassName("budget-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        
        // Apply modern dark grid styling
        grid.getStyle()
            .set("background", "#171521")
            .set("border-radius", "15px")
            .set("padding", "20px");
        
        // Add columns
        grid.addColumn(BudgetItem::getCategory)
            .setHeader("Category")
            .setSortable(true)
            .setFlexGrow(2);
        
        grid.addColumn(item -> {
            double planned = item.getPlanned() != null ? item.getPlanned() : 0.0;
            double usd = dashboardDataService.convertToUSD(planned);
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Planned")
            .setSortable(true)
            .setWidth("150px");
        
        grid.addColumn(item -> {
            double actual = item.getActual() != null ? item.getActual() : 0.0;
            double usd = dashboardDataService.convertToUSD(actual);
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Actual")
            .setSortable(true)
            .setWidth("150px");
        
        grid.addColumn(item -> {
            double progress = item.getPlanned() > 0 
                ? (item.getActual() / item.getPlanned()) * 100 
                : 0;
            return String.format("%.0f%%", progress);
        })
            .setHeader("Progress")
            .setSortable(true)
            .setWidth("120px");
        
        return grid;
    }

    private void updateContentForTab(Tab selectedTab) {
        contentArea.removeAll();
        
        String tabLabel = selectedTab.getLabel();
        
        switch (tabLabel) {
            case "ALL ACTIVITY":
                contentArea.add(allActivityGrid);
                loadAllActivityData(allActivityGrid);
                break;
            case "INCOME":
                contentArea.add(incomeGrid);
                loadDataForCategory(tabLabel, incomeGrid);
                break;
            case "EXPENSES":
                contentArea.add(expensesGrid);
                loadDataForCategory(tabLabel, expensesGrid);
                break;
            case "BILLS":
                contentArea.add(billsGrid);
                loadDataForCategory(tabLabel, billsGrid);
                break;
            case "SAVINGS":
                contentArea.add(savingsGrid);
                loadDataForCategory(tabLabel, savingsGrid);
                break;
        }
    }

    private void loadDataForCategory(String categoryType, Grid<BudgetItem> grid) {
        // Derive category totals from combined planned + actual data
        // for the current month (planned from budget items, actual from
        // bank transactions via BankAccountService)
        List<BudgetItem> items;
        try {
            List<BudgetItem> combinedItems = getCombinedBudgetDataForCurrentMonth();
            items = combinedItems.stream()
                .filter(item -> categoryType.equals(item.getCategoryType()))
                .toList();
        } catch (Exception e) {
            // In case of any error, fall back to an empty list to keep UI stable
            items = java.util.Collections.emptyList();
        }
        
        grid.setItems(items);
    }
    
    private void loadAllActivityData(Grid<BankTransaction> grid) {
        // Load all bank transactions, sorted by date descending
        List<BankTransaction> transactions = bankTransactionRepository.findAllByOrderByTransactionDateDesc();
        grid.setItems(transactions);
    }

    private void refreshData() {
        updateSummaryCards();
        
        // Refresh the currently visible grid
        Tab selectedTab = categoryTabs.getSelectedTab();
        if (selectedTab != null) {
            updateContentForTab(selectedTab);
        }
    }

    private void updateSummaryCards() {
        summaryCards.removeAll();
        
        // Calculate totals by category type from combined planned + actual
        // data for the current month
        Map<String, Double> plannedTotals = new HashMap<>();
        Map<String, Double> actualTotals = new HashMap<>();
        
        List<BudgetItem> allItems;
        try {
            allItems = getCombinedBudgetDataForCurrentMonth();
        } catch (Exception e) {
            allItems = java.util.Collections.emptyList();
        }
        
        for (BudgetItem item : allItems) {
            String categoryType = item.getCategoryType();
            // Planned will be 0 for transaction-derived items, but keep logic generic
            plannedTotals.put(categoryType,
                plannedTotals.getOrDefault(categoryType, 0.0) + (item.getPlanned() != null ? item.getPlanned() : 0.0));
            actualTotals.put(categoryType,
                actualTotals.getOrDefault(categoryType, 0.0) + (item.getActual() != null ? item.getActual() : 0.0));
        }
        
        // Create summary cards for each category using aggregated actuals
        summaryCards.add(
            createSummaryCard("INCOME",
                plannedTotals.getOrDefault("INCOME", 0.0),
                actualTotals.getOrDefault("INCOME", 0.0),
                "#4ade80"),
            createSummaryCard("EXPENSES",
                plannedTotals.getOrDefault("EXPENSES", 0.0),
                actualTotals.getOrDefault("EXPENSES", 0.0),
                "#f87171"),
            createSummaryCard("BILLS",
                plannedTotals.getOrDefault("BILLS", 0.0),
                actualTotals.getOrDefault("BILLS", 0.0),
                "#fbbf24"),
            createSummaryCard("SAVINGS",
                plannedTotals.getOrDefault("SAVINGS", 0.0),
                actualTotals.getOrDefault("SAVINGS", 0.0),
                "#60a5fa")
        );
    }

    /**
     * Combine planned budget items (from Monthly Plan / BudgetItemRepository)
     * with actuals derived from real bank transactions for the current month.
     * Planned amounts only exist where the user has explicitly defined a budget;
     * categories that only appear in transactions get planned = 0 and
     * actual = aggregated transaction total.
     */
    private List<BudgetItem> getCombinedBudgetDataForCurrentMonth() {
        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        // Planned budgets saved by the user (via MonthlyPlanView or here)
        String userId = userSessionService.getCurrentUserId();
        List<BudgetItem> plannedItems = budgetItemRepository
            .findByUserIdAndYearAndMonth(userId, year, month);

        // Actuals derived from real transactions for this month
        List<BudgetItem> actualItems;
        try {
            actualItems = bankAccountService.generateBudgetItemsFromTransactions(currentMonth);
        } catch (Exception e) {
            actualItems = java.util.Collections.emptyList();
        }

        Map<String, BudgetItem> combined = new HashMap<>();

        // Start with planned items (keep both planned and any existing actual)
        for (BudgetItem planned : plannedItems) {
            String key = planned.getCategoryType() + ":" + planned.getCategory();
            combined.put(key, planned);
        }

        // Merge in actuals: update existing planned items or create new ones
        for (BudgetItem actual : actualItems) {
            String key = actual.getCategoryType() + ":" + actual.getCategory();
            BudgetItem existing = combined.get(key);
            if (existing != null) {
                existing.setActual(actual.getActual());
            } else {
                BudgetItem fromActual = new BudgetItem(
                    actual.getCategory(),
                    0.0d, // no planned amount defined for this category
                    actual.getActual() != null ? actual.getActual() : 0.0d,
                    actual.getCategoryType(),
                    year,
                    month
                );
                fromActual.setUserId(userId);
                combined.put(key, fromActual);
            }
        }

        return new java.util.ArrayList<>(combined.values());
    }

    /**
     * Simple dialog to add a planned budget item for the current month
     * without leaving the Trends & Activity view.
     */
    private void showAddBudgetItemDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Budget Item");
        dialog.setWidth("450px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);

        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        // Build a list of available categories from both planned budget items
        // and transaction-derived budget items for the current month
        String userId = userSessionService.getCurrentUserId();
        java.util.Set<String> categoryOptions = new java.util.TreeSet<>();
        try {
            List<BudgetItem> plannedItems = budgetItemRepository
                .findByUserIdAndYearAndMonth(userId, year, month);
            plannedItems.stream()
                .map(BudgetItem::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .forEach(categoryOptions::add);
        } catch (Exception ignored) {
            // Ignore and continue with whatever data we can collect
        }

        try {
            List<BudgetItem> actualItems = bankAccountService
                .generateBudgetItemsFromTransactions(currentMonth);
            actualItems.stream()
                .map(BudgetItem::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .forEach(categoryOptions::add);
        } catch (Exception ignored) {
            // Ignore and continue
        }

        ComboBox<String> categoryField = new ComboBox<>("Category");
        categoryField.setItems(categoryOptions);
        categoryField.setWidthFull();
        categoryField.setRequired(true);
        categoryField.setAllowCustomValue(false);

        ComboBox<String> typeField = new ComboBox<>("Budget Type");
        typeField.setItems("INCOME", "EXPENSES", "BILLS", "SAVINGS");
        typeField.setItemLabelGenerator(type -> {
            switch (type) {
                case "INCOME": return "💰 Income - Money coming in";
                case "EXPENSES": return "🛒 Expenses - Variable spending";
                case "BILLS": return "📄 Bills - Fixed recurring costs";
                case "SAVINGS": return "🐷 Savings - Money to save";
                default: return type;
            }
        });
        typeField.setWidthFull();
        typeField.setRequired(true);

        NumberField plannedField = new NumberField("Planned Amount");
        plannedField.setPrefixComponent(new Span("$"));
        plannedField.setPlaceholder("0.00");
        plannedField.setWidthFull();
        plannedField.setRequired(true);
        plannedField.setMin(0);
        plannedField.setStep(0.01);

        plannedField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue() >= 1000) {
                String formatted = String.format("%,.2f", e.getValue());
                plannedField.setHelperText("= $" + formatted);
            } else {
                plannedField.setHelperText("");
            }
        });

        Span helperText = new Span("Set your planned amount to track spending against your budget");
        helperText.getStyle()
            .set("font-size", "12px")
            .set("color", "rgba(255, 255, 255, 0.6)")
            .set("padding", "10px 0");

        layout.add(categoryField, typeField, plannedField, helperText);

        Button saveButton = new Button("Add Budget Item", e -> {
            if (categoryField.isEmpty() || typeField.isEmpty() || plannedField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            BudgetItem newItem = new BudgetItem();
            newItem.setUserId(userSessionService.getCurrentUserId());
            newItem.setCategory(categoryField.getValue());
            newItem.setCategoryType(typeField.getValue());
            newItem.setPlanned(plannedField.getValue());
            newItem.setActual(0.0);
            newItem.setYear(currentMonth.getYear());
            newItem.setMonth(currentMonth.getMonthValue());

            budgetItemRepository.save(newItem);

            Notification.show("✅ Budget item added successfully!", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
            refreshData();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(layout);
        dialog.open();
    }

    private Div createSummaryCard(String title, double planned, double actual, String color) {
        Div card = new Div();
        card.addClassName("summary-card");
        card.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "20px")
            .set("border-left", "4px solid " + color);
        
        H3 cardTitle = new H3(title);
        cardTitle.getStyle()
            .set("margin", "0 0 10px 0")
            .set("color", "white")
            .set("font-size", "14px")
            .set("font-weight", "500")
            .set("text-transform", "uppercase");
        
        Span plannedAmount = new Span("Planned: $" + String.format("%.2f", planned));
        plannedAmount.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "14px")
            .set("display", "block")
            .set("margin-bottom", "5px");
        
        Span actualAmount = new Span("Actual: $" + String.format("%.2f", actual));
        actualAmount.getStyle()
            .set("color", color)
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("display", "block");
        
        double progress = planned > 0 ? (actual / planned) * 100 : 0;
        Span progressText = new Span(String.format("%.0f%%", progress));
        progressText.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "14px")
            .set("display", "block")
            .set("margin-top", "10px");
        
        card.add(cardTitle, plannedAmount, actualAmount, progressText);
        return card;
    }
    
    private String getCategoryColor(String category) {
        return dashboardDataService.getCategoryColor(category != null ? category : "Other");
    }
    
    private void openTransactionDetailsDialog(BankTransaction transaction, String formattedAmount, String categoryColor) {
        TransactionDetailsDialog dialog = new TransactionDetailsDialog(
                transaction,
                formattedAmount,
                categoryColor,
                bankAccountService,
                dashboardDataService,
                transactionMetaService,
                recurringTransactionService
        );
        dialog.open();
    }
}
