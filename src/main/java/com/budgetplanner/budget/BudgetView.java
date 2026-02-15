package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.PlaidService;
import com.budgetplanner.budget.view.BankAccountManagementDialog;
import com.budgetplanner.budget.view.TransactionCategorizationDialog;
import com.budgetplanner.budget.view.ManualTransactionDialog;
import com.budgetplanner.budget.view.NotificationCenterDialog;
import com.budgetplanner.budget.service.AIAdvisoryService;
import com.budgetplanner.budget.util.CurrencyFormatter;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.avatar.Avatar;
import java.io.*;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;

@Route("budget")
@PageTitle("Monthly Budget | Budget Planner")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class BudgetView extends VerticalLayout {

    // Constants for category types
    private static final String CATEGORY_INCOME = "INCOME";
    private static final String CATEGORY_EXPENSES = "EXPENSES";
    private static final String CATEGORY_BILLS = "BILLS";
    private static final String CATEGORY_SAVINGS = "SAVINGS";
    
    // Constants for formatting
    private static final String CURRENCY_FORMAT = "$%.2f";
    private static final String ZERO_CURRENCY = "$0.00";
    private static final String HEADER_PLANNED = "Planned";
    private static final String HEADER_ACTUAL = "Actual";
    private static final String MIN_COLUMN_WIDTH = "300px";
    private static final String DARK_TEXT_COLOR = "#cdd6f4";
    private static final String DARK_BG_COLOR = "#1e1e2e";
    private static final String DARK_GRID_COLOR = "#45475a";
    private static final String CHART_HEIGHT = "250px";
    
    @Serial
    private static final long serialVersionUID = 1L;

    private TreeGrid<NavigationNode> navigationTree;
    private Div sidebar;
    private VerticalLayout mainContent;
    private H2 monthHeader;
    private SplitLayout splitLayout;
    private VerticalLayout formPanel;
    private Dialog uploadDialog;
    // Track form panel visibility state
    
    // Form components
    private TextField categoryField;
    private NumberField plannedField;
    private NumberField actualField;
    private Select<String> categoryTypeSelect;
    private Button saveButton;
    private Button cancelButton;
    private Button addItemButton;
    private transient BudgetItem editingItem;
    
    // Category grids
    private Grid<BudgetItem> incomeGrid;
    private Grid<BudgetItem> expensesGrid;
    private Grid<BudgetItem> billsGrid;
    private Grid<BudgetItem> savingsGrid;
    
    
    // Summary cards
    private Span rolloverTotal;
    private Span incomeTotal;
    private Span expensesTotal;
    private Span billsTotal;
    private Span savingsTotal;
    private Span debtTotal;
    private Span leftoverTotal;

    // Current selected period
    private Year currentYear;
    private Month currentMonth;

    // Bank integration services
    private final BankAccountService bankAccountService;
    private final PlaidService plaidService;
    private final AIAdvisoryService aiAdvisoryService;
    
    // Notification bell button
    private Button notificationBellButton;
    
    // Mobile navigation
    private Button mobileNavToggle;
    private Div sidebarOverlay;

    public BudgetView(BankAccountService bankAccountService, PlaidService plaidService, AIAdvisoryService aiAdvisoryService) {
        this.bankAccountService = bankAccountService;
        this.plaidService = plaidService;
        this.aiAdvisoryService = aiAdvisoryService;
        // Enable dark theme
        getElement().setAttribute("theme", Lumo.DARK);
        
        setSizeFull();
        addClassName("budget-dashboard");
        setSpacing(false);
        setPadding(false);
        
        // Initialize current period
        LocalDate now = LocalDate.now();
        currentYear = Year.of(now.getYear());
        currentMonth = now.getMonth();
        
        // Build the UI components
        buildSidebar();
        buildMainDashboard();
        
        // Create split layout for main content and form panel
        createFormPanel();
        splitLayout = new SplitLayout(mainContent, formPanel);
        splitLayout.setSizeFull();
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(100); // Initially hide bottom panel
        splitLayout.addClassName("budget-split-layout");
        
        // Initially hide the form panel completely
        formPanel.setVisible(false);
        
        // Add components to main layout
        HorizontalLayout mainLayout = new HorizontalLayout(sidebar, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setPadding(false);
        add(mainLayout);
        
        // Setup navigation tree
        setupNavigationTree();
        
        // Initial data load
        refreshDashboard();
    }

    // Sample/demo data removed: BudgetView now uses only real transaction-based data.

    private void buildSidebar() {
        sidebar = new Div();
        sidebar.addClassName("sidebar");
        sidebar.setWidth("180px");
        sidebar.setHeight("100%");
        
        // Add mobile navigation support
        setupMobileNavigation();
        
        // Header layout with month/year and notification bell
        HorizontalLayout headerLayout = createSidebarHeader();
        
        Span budgetLabel = new Span("BUDGET DASHBOARD");
        budgetLabel.addClassName("budget-label");
        
        // Navigation tree
        navigationTree = new TreeGrid<>();
        navigationTree.addClassName("navigation-tree");
        navigationTree.setHeightFull();
        navigationTree.addHierarchyColumn(NavigationNode::getLabel).setHeader("Period");
        
        // User profile section (will be positioned at bottom via CSS)
        Div userProfileSection = createUserProfileSection();
        
        sidebar.add(headerLayout, budgetLabel, navigationTree, userProfileSection);
    }

    private void buildMainDashboard() {
        mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.addClassName("main-dashboard");
        mainContent.setSpacing(true);
        mainContent.setPadding(true);
        mainContent.setAlignItems(Alignment.STRETCH);
        mainContent.setJustifyContentMode(JustifyContentMode.START);
        
        // Top summary cards
        HorizontalLayout summaryCards = createSummaryCards();
        summaryCards.addClassName("responsive-summary");
        
        // Main content area with charts
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setHeight("400px");
        topRow.setSpacing(true);
        topRow.addClassName("responsive-top-row");
        topRow.setAlignItems(Alignment.STRETCH);
        topRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        // Left column - Amount left to spend and Cash flow chart
        VerticalLayout leftColumn = new VerticalLayout();
        leftColumn.setFlexGrow(1);
        leftColumn.setMinWidth(MIN_COLUMN_WIDTH);
        leftColumn.setSpacing(true);
        leftColumn.setPadding(false);
        leftColumn.add(createAmountLeftCard(), createCashFlowChart());
        
        // Right column - Allocation chart
        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.setFlexGrow(1);
        rightColumn.setMinWidth(MIN_COLUMN_WIDTH);
        rightColumn.setSpacing(true);
        rightColumn.setPadding(false);
        rightColumn.add(createAllocationChart());
        
        topRow.add(leftColumn, rightColumn);
        
        // Bottom row - Category grids
        HorizontalLayout bottomRow = createCategoryGrids();
        bottomRow.addClassName("responsive-category-grids");
        bottomRow.setWidthFull();
        bottomRow.setFlexShrink(0);
        
        // Create floating add button with category selection menu
        addItemButton = new Button( VaadinIcon.PLUS.create());
        addItemButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addItemButton.addClassName("floating-add-button");
        addItemButton.addClassName("mobile-button");
        addItemButton.addClassName("touch-target");
        addItemButton.getElement().setProperty("title", "Add Manual Transaction (Ctrl+N)");
        addItemButton.addClickListener(e -> openManualTransactionDialog());
        
        // Add keyboard shortcut for add button
        Shortcuts.addShortcutListener(this, () -> openManualTransactionDialog(), Key.KEY_N, KeyModifier.CONTROL);
        
        mainContent.add(summaryCards, topRow, bottomRow, addItemButton);
        mainContent.setFlexGrow(0, summaryCards);
        mainContent.setFlexGrow(0, topRow);
        mainContent.setFlexGrow(1, bottomRow);
        mainContent.setFlexGrow(0, addItemButton);
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setMargin(false);
        layout.addClassName("summary-cards");
        layout.addClassName("mobile-responsive-cards");
        
        // Create summary cards
        Div rolloverCard = createSummaryCard("ROLLOVER", ZERO_CURRENCY, "rollover-card");
        rolloverTotal = (Span) rolloverCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div incomeCard = createSummaryCard(CATEGORY_INCOME, ZERO_CURRENCY, "income-card");
        incomeTotal = (Span) incomeCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div expensesCard = createSummaryCard(CATEGORY_EXPENSES, ZERO_CURRENCY, "expenses-card");
        expensesTotal = (Span) expensesCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div billsCard = createSummaryCard(CATEGORY_BILLS, ZERO_CURRENCY, "bills-card");
        billsTotal = (Span) billsCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div savingsCard = createSummaryCard(CATEGORY_SAVINGS, ZERO_CURRENCY, "savings-card");
        savingsTotal = (Span) savingsCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div debtCard = createSummaryCard("DEBT", ZERO_CURRENCY, "debt-card");
        debtTotal = (Span) debtCard.getChildren().skip(1).findFirst().orElse(null);
        
        Div leftoverCard = createSummaryCard("LEFT OVER", ZERO_CURRENCY, "leftover-card");
        leftoverTotal = (Span) leftoverCard.getChildren().skip(1).findFirst().orElse(null);
        
        layout.add(rolloverCard, incomeCard, expensesCard, billsCard, savingsCard, debtCard, leftoverCard);
        
        // Make each card take equal width with no gaps
        layout.setFlexGrow(1, rolloverCard);
        layout.setFlexGrow(1, incomeCard);
        layout.setFlexGrow(1, expensesCard);
        layout.setFlexGrow(1, billsCard);
        layout.setFlexGrow(1, savingsCard);
        layout.setFlexGrow(1, debtCard);
        layout.setFlexGrow(1, leftoverCard);
        
        return layout;
    }

    private Div createSummaryCard(String title, String value, String className) {
        Div card = new Div();
        card.addClassName("summary-card");
        card.addClassName(className);
        
        Span titleSpan = new Span(title);
        titleSpan.addClassName("card-title");
        
        Span valueSpan = new Span(value);
        valueSpan.addClassName("card-value");
        
        card.add(titleSpan, valueSpan);
        return card;
    }

    private Chart createAmountLeftCard() {
        Chart chart = new Chart(ChartType.PIE);
        chart.addClassName("amount-left-chart");
        chart.setHeight(CHART_HEIGHT);
        chart.setWidthFull();
        
        Configuration config = chart.getConfiguration();
        config.setTitle("AMOUNT LEFT TO SPEND");
        
        // Apply dark theme
        config.getChart().setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        config.getTitle().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        
        PlotOptionsPie plotOptions = new PlotOptionsPie();
        plotOptions.setInnerSize("70%"); // Make it a donut chart
        plotOptions.getDataLabels().setEnabled(false); // Hide data labels for cleaner look
        config.setPlotOptions(plotOptions);
        
        // Configure tooltip
        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<b>{point.name}</b>: ${point.y:.0f}<br/>Percentage: {point.percentage:.1f}%");
        tooltip.setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        tooltip.getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        config.setTooltip(tooltip);
        
        // Calculate spending data
        List<BudgetItem> currentPeriodItems = getCurrentPeriodItems();
        double rolloverAmount = calculateRolloverAmount();
        
        double totalIncome = rolloverAmount + currentPeriodItems.stream()
            .filter(item -> CATEGORY_INCOME.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();
            
        double totalSpent = currentPeriodItems.stream()
            .filter(item -> !CATEGORY_INCOME.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();
            
        double amountLeft = Math.max(0, totalIncome - totalSpent);
        
        // Create data series for donut chart
        DataSeries series = new DataSeries();
        
        DataSeriesItem spentItem = new DataSeriesItem("Spent", totalSpent);
        spentItem.setColor(new SolidColor("#f38ba8")); // Pink for spent
        series.add(spentItem);
        
        DataSeriesItem leftItem = new DataSeriesItem("Left", amountLeft);
        leftItem.setColor(new SolidColor("#a6e3a1")); // Green for remaining
        series.add(leftItem);
        
        config.addSeries(series);
        
        // The center amount will be shown via CSS styling
        
        return chart;
    }

    private Chart createCashFlowChart() {
        Chart chart = new Chart(ChartType.COLUMN);
        chart.addClassName("cash-flow-chart");
        chart.addClassName("mobile-chart-container");
        chart.addClassName("chart-container");
        chart.setHeight(CHART_HEIGHT);
        chart.setWidthFull();

        Configuration config = chart.getConfiguration();
        config.setTitle("CASH FLOW SUMMARY");

        // Apply dark theme
        config.getChart().setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        config.getTitle().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        
        // Configure tooltip
        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<b>{series.name}</b><br/>{point.name}: ${point.y:.0f}");
        tooltip.setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        tooltip.getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        config.setTooltip(tooltip);

        // Get actual data from current period
        List<BudgetItem> currentPeriodItems = getCurrentPeriodItems();

        double plannedIncome = currentPeriodItems.stream()
            .filter(item -> CATEGORY_INCOME.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getPlanned)
            .sum();
        double actualIncome = currentPeriodItems.stream()
            .filter(item -> CATEGORY_INCOME.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double plannedExpenses = currentPeriodItems.stream()
            .filter(item -> CATEGORY_EXPENSES.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getPlanned)
            .sum();
        double actualExpenses = currentPeriodItems.stream()
            .filter(item -> CATEGORY_EXPENSES.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double plannedBills = currentPeriodItems.stream()
            .filter(item -> CATEGORY_BILLS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getPlanned)
            .sum();
        double actualBills = currentPeriodItems.stream()
            .filter(item -> CATEGORY_BILLS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double plannedSavings = currentPeriodItems.stream()
            .filter(item -> CATEGORY_SAVINGS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getPlanned)
            .sum();
        double actualSavings = currentPeriodItems.stream()
            .filter(item -> CATEGORY_SAVINGS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        // Add series with actual data
        ListSeries plannedSeries = new ListSeries(HEADER_PLANNED, plannedIncome, plannedExpenses, plannedBills, plannedSavings);
        ListSeries actualSeries = new ListSeries(HEADER_ACTUAL, actualIncome, actualExpenses, actualBills, actualSavings);

        config.addSeries(plannedSeries);
        config.addSeries(actualSeries);

        XAxis xAxis = config.getxAxis();
        xAxis.setCategories("Income", "Expenses", "Bills", "Savings");
        xAxis.getLabels().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        
        YAxis yAxis = config.getyAxis();
        yAxis.getLabels().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        yAxis.setGridLineColor(new SolidColor(DARK_GRID_COLOR));

        return chart;
    }

    private List<BudgetItem> getCurrentPeriodItems() {
        try {
            YearMonth yearMonth = YearMonth.of(currentYear.getValue(), currentMonth.getValue());
            List<BudgetItem> realTransactionItems = bankAccountService.generateBudgetItemsFromTransactions(yearMonth);
            return realTransactionItems != null ? realTransactionItems : java.util.Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error getting real transaction data for BudgetView: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    private Chart createAllocationChart() {
        Chart chart = new Chart(ChartType.PIE);
        chart.addClassName("allocation-chart");
        chart.addClassName("mobile-chart-container");
        chart.addClassName("chart-container");
        chart.setHeight(CHART_HEIGHT);
        chart.setWidthFull();

        Configuration config = chart.getConfiguration();
        config.setTitle("ALLOCATION SUMMARY");

        // Apply dark theme
        config.getChart().setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        config.getTitle().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        
        // Configure tooltip
        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<b>{point.name}</b>: ${point.y:.0f}<br/>Percentage: {point.percentage:.1f}%");
        tooltip.setBackgroundColor(new SolidColor(DARK_BG_COLOR));
        tooltip.getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        config.setTooltip(tooltip);
        
        PlotOptionsPie plotOptions = new PlotOptionsPie();
        plotOptions.setInnerSize("50%");
        plotOptions.getDataLabels().getStyle().setColor(new SolidColor(DARK_TEXT_COLOR));
        config.setPlotOptions(plotOptions);

        // Get actual data from current period
        List<BudgetItem> currentPeriodItems = getCurrentPeriodItems();

        double actualIncome = currentPeriodItems.stream()
            .filter(item -> CATEGORY_INCOME.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double actualExpenses = currentPeriodItems.stream()
            .filter(item -> CATEGORY_EXPENSES.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double actualBills = currentPeriodItems.stream()
            .filter(item -> CATEGORY_BILLS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        double actualSavings = currentPeriodItems.stream()
            .filter(item -> CATEGORY_SAVINGS.equals(item.getCategoryType()))
            .mapToDouble(BudgetItem::getActual)
            .sum();

        // Create data series with actual data and custom colors
        DataSeries series = new DataSeries();

        DataSeriesItem incomeItem = new DataSeriesItem("Income", actualIncome);
        incomeItem.setColor(new SolidColor("#a6e3a1"));
        series.add(incomeItem);

        DataSeriesItem expensesItem = new DataSeriesItem("Expenses", actualExpenses);
        expensesItem.setColor(new SolidColor("#f38ba8"));
        series.add(expensesItem);

        DataSeriesItem billsItem = new DataSeriesItem("Bills", actualBills);
        billsItem.setColor(new SolidColor("#fab387"));
        series.add(billsItem);

        DataSeriesItem savingsItem = new DataSeriesItem("Savings", actualSavings);
        savingsItem.setColor(new SolidColor("#89b4fa"));
        series.add(savingsItem);

        config.addSeries(series);

        return chart;
    }

    private HorizontalLayout createCategoryGrids() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);
        layout.addClassName("category-grids");
        layout.addClassName("mobile-responsive-grids");
        
        // Create category grids
        VerticalLayout incomeContainer = createCategoryGridWithHeader(CATEGORY_INCOME);
        VerticalLayout expensesContainer = createCategoryGridWithHeader(CATEGORY_EXPENSES);
        VerticalLayout billsContainer = createCategoryGridWithHeader(CATEGORY_BILLS);
        VerticalLayout savingsContainer = createCategoryGridWithHeader(CATEGORY_SAVINGS);
        
        layout.add(incomeContainer, expensesContainer, billsContainer, savingsContainer);
        return layout;
    }

    private VerticalLayout createCategoryGridWithHeader(String category) {
        VerticalLayout container = new VerticalLayout();
        container.addClassName("category-grid-container");
        container.addClassName("mobile-grid-container");
        container.addClassName(category.toLowerCase() + "-container");
        container.setSpacing(false);
        container.setPadding(false);
        container.setWidth("100%");
        
        // Header
        Div header = new Div();
        header.addClassName("grid-header");
        header.setText(category);
        
        // Grid
        Grid<BudgetItem> grid = new Grid<>(BudgetItem.class, false);
        grid.addClassName("category-grid");
        grid.addClassName(category.toLowerCase() + "-grid");
        grid.setHeight("280px");
        grid.setWidthFull();
        
        // Columns with responsive sizing
        grid.addColumn(BudgetItem::getCategory)
            .setHeader("Category")
            .setFlexGrow(2)
            .setResizable(true);
        
        grid.addColumn(item -> CurrencyFormatter.formatUSD(item.getPlanned())).setHeader(HEADER_PLANNED).setFlexGrow(1).setResizable(true);
        grid.addColumn(item -> CurrencyFormatter.formatUSD(item.getActual())).setHeader(HEADER_ACTUAL).setFlexGrow(1).setResizable(true);
        grid.addColumn(item -> {
            double percentage = item.getPlanned() > 0 ? (item.getActual() / item.getPlanned()) * 100 : 0;
            return String.format("%.0f%%", percentage);
        }).setHeader("Progress").setFlexGrow(1).setResizable(true);
        
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        
        // Add click listener to open transaction categorization modal
        grid.addItemClickListener(event -> {
            BudgetItem item = event.getItem();
            openTransactionCategorizationModal(item.getCategory(), item.getCategoryType());
        });
        
        // Add double-click listener for editing items with category context
        grid.addItemDoubleClickListener(event -> showFormPanel(event.getItem(), category));
        
        // Store grid reference based on category
        switch (category) {
            case CATEGORY_INCOME -> incomeGrid = grid;
            case CATEGORY_EXPENSES -> expensesGrid = grid;
            case CATEGORY_BILLS -> billsGrid = grid;
            case CATEGORY_SAVINGS -> savingsGrid = grid;
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        }
        
        container.add(header, grid);
        return container;
    }

    private void updateMonthHeader() {
        String monthName = currentMonth.name().substring(0, 1) + 
                          currentMonth.name().substring(1).toLowerCase();
        monthHeader.setText(monthName + " " + currentYear.getValue());
    }

    private Div createUserProfileSection() {
        Div profileSection = new Div();
        profileSection.addClassName("user-profile-section");
        
        // User avatar
        Avatar avatar = new Avatar("John Doe");
        avatar.setColorIndex(1);
        avatar.addClassName("user-avatar");
        
        // Username
        Span username = new Span("John Doe");
        username.addClassName("username");
        
        // Three dots menu button
        Button menuButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        menuButton.addClassName("menu-button");
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        
        // Profile container
        HorizontalLayout profileContainer = new HorizontalLayout(avatar, username, menuButton);
        profileContainer.addClassName("profile-container");
        profileContainer.setAlignItems(Alignment.CENTER);
        profileContainer.setJustifyContentMode(JustifyContentMode.BETWEEN);
        profileContainer.setWidthFull();
        
        // Create context menu for dropdown attached to menu button
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(menuButton);
        contextMenu.setOpenOnClick(true); // Enable single click to open
        
        // Add menu items with proper dark theme icons
        MenuItem bankAccountItem = contextMenu.addItem("Manage Bank Accounts", e -> openBankAccountManagement());
        Icon bankIcon = VaadinIcon.INSTITUTION.create();
        bankIcon.setSize("16px");
        bankIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        bankAccountItem.addComponentAsFirst(bankIcon);
        
        MenuItem uploadItem = contextMenu.addItem("Upload Bank Statement", e -> openUploadDialog());
        Icon uploadIcon = VaadinIcon.UPLOAD_ALT.create();
        uploadIcon.setSize("16px");
        uploadIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        uploadItem.addComponentAsFirst(uploadIcon);
        
        MenuItem settingsItem = contextMenu.addItem("Settings", e -> 
            Notification.show("Settings coming soon!", 3000, Notification.Position.TOP_CENTER));
        Icon settingsIcon = VaadinIcon.COG_O.create();
        settingsIcon.setSize("16px");
        settingsIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        settingsItem.addComponentAsFirst(settingsIcon);
        
        MenuItem logoutItem = contextMenu.addItem("Logout", e -> 
            Notification.show("Logout functionality coming soon!", 3000, Notification.Position.TOP_CENTER));
        Icon logoutIcon = VaadinIcon.SIGN_OUT_ALT.create();
        logoutIcon.setSize("16px");
        logoutIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        logoutItem.addComponentAsFirst(logoutIcon);
        
        profileSection.add(profileContainer);
        return profileSection;
    }

    private void openUploadDialog() {
        uploadDialog = new Dialog();
        uploadDialog.setHeaderTitle("Upload Bank Statement");
        uploadDialog.setWidth("500px");
        uploadDialog.setHeight("400px");
        
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setSpacing(true);
        dialogContent.setPadding(false);
        
        // Instructions
        Span instructions = new Span("Upload your bank statement (CSV or Excel format) to automatically populate your budget.");
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");
        instructions.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        // File upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", ".xlsx", ".xls");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Drop bank statement here or click to browse"));
        
        // Upload success handler
        upload.addSucceededListener(event -> {
            try {
                processUploadedFile(buffer.getInputStream(), event.getFileName());
                uploadDialog.close();
                Notification notification = Notification.show("Bank statement processed successfully!", 5000, Notification.Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification notification = Notification.show("Error processing file: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        // Upload failed handler
        upload.addFailedListener(event -> {
            Notification notification = Notification.show("Upload failed: " + event.getReason().getMessage(), 5000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        
        dialogContent.add(instructions, upload);
        
        // Dialog buttons
        Button dialogCancelButton = new Button("Cancel", e -> uploadDialog.close());
        dialogCancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        uploadDialog.getFooter().add(dialogCancelButton);
        uploadDialog.add(dialogContent);
        uploadDialog.open();
    }

    private void processUploadedFile(InputStream inputStream, String fileName) throws IOException {
        int createdCount = 0;

        if (fileName.toLowerCase().endsWith(".csv")) {
            createdCount = parseCsvAndCreateTransactions(inputStream, fileName);
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
            // For now, show a message that Excel support is coming soon
            throw new IOException("Excel file support coming soon. Please convert to CSV format.");
        }

        // Refresh the dashboard to show new data
        refreshDashboard();
        
        Notification.show("Imported " + createdCount + " transactions successfully!", 3000, Notification.Position.TOP_CENTER);
    }

    private int parseCsvAndCreateTransactions(InputStream inputStream, String fileName) throws IOException {
        int createdCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Parse CSV line (simple comma-separated parsing)
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        // Assume format: Date, Description, Amount, [optional: Category]
                        String datePart = parts[0].trim().replaceAll("\"", "");
                        String description = parts[1].trim().replaceAll("\"", "");
                        double amount = Double.parseDouble(parts[2].trim().replaceAll("\"", ""));

                        // Try to parse date; fallback to current date if parsing fails
                        java.time.LocalDate txDate;
                        try {
                            txDate = java.time.LocalDate.parse(datePart);
                        } catch (Exception ex) {
                            txDate = java.time.LocalDate.now();
                        }

                        // Determine budget category type using existing logic
                        String categoryType = bankAccountService.predictCategoryType(description, amount);

                        // Use description as merchant and budget category for now
                        bankAccountService.createManualTransaction(
                                description,               // merchantName
                                description,               // description
                                amount,                    // amount (signed)
                                txDate,                    // transactionDate
                                description,               // budgetCategory
                                categoryType               // budgetCategoryType
                        );

                        createdCount++;
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                        continue;
                    }
                }
            }
        }
        
        return createdCount;
    }

    private void setupNavigationTree() {
        TreeData<NavigationNode> treeData = new TreeData<>();
        
        // Create year nodes (2023 to current year only)
        int startYear = 2023;
        int endYear = currentYear.getValue();
        
        for (int year = startYear; year <= endYear; year++) {
            NavigationNode yearNode = new NavigationNode(Year.of(year));
            treeData.addItem(null, yearNode);
            
            // Add month children only for valid months (not future months)
            for (Month month : Month.values()) {
                // Skip future months beyond current date
                if (year == currentYear.getValue() && month.getValue() > currentMonth.getValue()) {
                    continue;
                }
                
                NavigationNode monthNode = new NavigationNode(Year.of(year), month);
                treeData.addItem(yearNode, monthNode);
                yearNode.addChild(monthNode);
            }
        }
        
        TreeDataProvider<NavigationNode> dataProvider = new TreeDataProvider<>(treeData);
        navigationTree.setDataProvider(dataProvider);
        
        // Expand only the current year, collapse others
        treeData.getRootItems().forEach(yearNode -> {
            if (yearNode.getYear().equals(currentYear)) {
                yearNode.setExpanded(true);
                navigationTree.expand(yearNode);
            } else {
                yearNode.setExpanded(false);
                navigationTree.collapse(yearNode);
            }
        });
        
        // Set initial selection (current month)
        NavigationNode currentMonthNode = findNavigationNode(treeData, currentYear, currentMonth);
        if (currentMonthNode != null) {
            navigationTree.getSelectionModel().select(currentMonthNode);
        }
        
        // Add selection listener
        navigationTree.addSelectionListener(event -> 
            event.getFirstSelectedItem().ifPresent(this::onNavigationNodeSelected)
        );
    }

    private NavigationNode findNavigationNode(TreeData<NavigationNode> treeData, Year year, Month month) {
        return treeData.getRootItems().stream()
                .filter(node -> node.getYear().equals(year))
                .flatMap(yearNode -> treeData.getChildren(yearNode).stream())
                .filter(monthNode -> monthNode.getMonth().equals(month))
                .findFirst()
                .orElse(null);
    }

    private void onNavigationNodeSelected(NavigationNode selectedNode) {
        if (selectedNode.getType() == NavigationNode.NodeType.MONTH) {
            currentYear = selectedNode.getYear();
            currentMonth = selectedNode.getMonth();
            updateMonthHeader();
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        // Get real transaction-based budget items for the current period
        List<BudgetItem> currentPeriodItems = getCurrentPeriodItems();
        
        // Update category grids
        List<BudgetItem> incomeItems = currentPeriodItems.stream()
                .filter(item -> CATEGORY_INCOME.equals(item.getCategoryType()))
                .toList();
        incomeGrid.setItems(incomeItems);
        
        List<BudgetItem> expensesItems = currentPeriodItems.stream()
                .filter(item -> CATEGORY_EXPENSES.equals(item.getCategoryType()))
                .toList();
        expensesGrid.setItems(expensesItems);
        
        List<BudgetItem> billsItems = currentPeriodItems.stream()
                .filter(item -> CATEGORY_BILLS.equals(item.getCategoryType()))
                .toList();
        billsGrid.setItems(billsItems);
        
        List<BudgetItem> savingsItems = currentPeriodItems.stream()
                .filter(item -> CATEGORY_SAVINGS.equals(item.getCategoryType()))
                .toList();
        savingsGrid.setItems(savingsItems);
        
        // Calculate totals
        double incomeSum = calculateTotalByType(currentPeriodItems, CATEGORY_INCOME);
        double expensesSum = calculateTotalByType(currentPeriodItems, CATEGORY_EXPENSES);
        double billsSum = calculateTotalByType(currentPeriodItems, CATEGORY_BILLS);
        double savingsSum = calculateTotalByType(currentPeriodItems, CATEGORY_SAVINGS);
        double debtSum = 0; // Placeholder for debt calculation
        
        // Calculate rollover from previous month
        double rolloverAmount = calculateRolloverAmount();
        
        // Calculate left over for current month
        double totalIncomeWithRollover = rolloverAmount + incomeSum;
        double totalOutgoing = expensesSum + billsSum + savingsSum + debtSum;
        double leftoverAmount = totalIncomeWithRollover - totalOutgoing;
        
        // Update totals
        if (rolloverTotal != null) rolloverTotal.setText(CurrencyFormatter.formatUSD(rolloverAmount));
        if (incomeTotal != null) incomeTotal.setText(CurrencyFormatter.formatUSD(incomeSum));
        if (expensesTotal != null) expensesTotal.setText(CurrencyFormatter.formatUSD(expensesSum));
        if (billsTotal != null) billsTotal.setText(CurrencyFormatter.formatUSD(billsSum));
        if (savingsTotal != null) savingsTotal.setText(CurrencyFormatter.formatUSD(savingsSum));
        if (debtTotal != null) debtTotal.setText(CurrencyFormatter.formatUSD(debtSum));
        if (leftoverTotal != null) leftoverTotal.setText(CurrencyFormatter.formatUSD(leftoverAmount));
        
        // Show AI Advisory notifications
        showAIAdvisoryNotifications();
        
        // Update notification bell badge
        if (notificationBellButton != null) {
            updateNotificationBadge();
        }
    }
    
    private double calculateRolloverAmount() {
        // For demo purposes, simulate rollover based on month
        // In a real application, this would query the database for previous month's data
        double baseRollover = 250.0;
        
        // Add some variation based on the month to make it realistic
        double monthVariation = (currentMonth.getValue() % 3) * 50.0;
        
        return baseRollover + monthVariation;
    }

    private double calculateTotalByType(List<BudgetItem> items, String type) {
        return items.stream()
                .filter(item -> type.equals(item.getCategoryType()))
                .mapToDouble(BudgetItem::getActual)
                .sum();
    }

    private void createFormPanel() {
        formPanel = new VerticalLayout();
        formPanel.addClassName("form-panel");
        formPanel.setSpacing(true);
        formPanel.setPadding(true);
        formPanel.setVisible(true);
        
        // Create form header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        H2 formTitle = new H2("Add Budget Item");
        formTitle.addClassName("form-title");
        
        Button closeButton = new Button(VaadinIcon.CLOSE.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> hideFormPanel());
        
        header.add(formTitle, closeButton);
        
        // Create form layout
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Initialize form fields
        categoryField = new TextField("Category");
        categoryField.setPlaceholder("Enter category name");
        categoryField.setRequired(true);
        
        categoryTypeSelect = new Select<>();
        categoryTypeSelect.setLabel("Category Type");
        categoryTypeSelect.setItems(CATEGORY_INCOME, CATEGORY_EXPENSES, CATEGORY_BILLS, CATEGORY_SAVINGS);
        categoryTypeSelect.setPlaceholder("Select type");
        // categoryTypeSelect.setRequired(true); // Select doesn't have setRequired method
        
        plannedField = new NumberField("Planned Amount");
        plannedField.setPlaceholder("0.00");
        plannedField.setPrefixComponent(new Span("$"));
        plannedField.setMin(0);
        plannedField.setRequired(true);
        
        actualField = new NumberField("Actual Amount");
        actualField.setPlaceholder("0.00");
        actualField.setPrefixComponent(new Span("$"));
        actualField.setMin(0);
        actualField.setRequired(true);
        
        // Add fields to form layout
        formLayout.add(categoryField, categoryTypeSelect, plannedField, actualField);
        
        // Create button layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        
        cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> hideFormPanel());
        
        saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveItem());
        
        buttonLayout.add(cancelButton, saveButton);
        
        // Add components to form panel
        formPanel.add(header, formLayout, buttonLayout);
    }
    
    private void showFormPanel(BudgetItem item, String categoryType) {
        editingItem = item;
        
        // Hide add button when form is open
        addItemButton.setVisible(false);
        
        // Make form panel visible and show it
        formPanel.setVisible(true);
        splitLayout.setSplitterPosition(70);
        
        if (item == null) {
            // Adding new item
            clearForm();
            if (categoryType != null) {
                categoryTypeSelect.setValue(categoryType);
            }
            // Set form title to Add Budget Item
            formPanel.getChildren().findFirst()
                .filter(component -> component instanceof HorizontalLayout)
                .map(component -> (HorizontalLayout) component)
                .ifPresent(header -> {
                    header.getChildren().findFirst()
                        .filter(comp -> comp instanceof H2)
                        .map(comp -> (H2) comp)
                        .ifPresent(title -> title.setText("Add Budget Item"));
                });
        } else {
            // Editing existing item
            populateForm(item);
            formPanel.getChildren().findFirst()
                .filter(component -> component instanceof HorizontalLayout)
                .map(component -> (HorizontalLayout) component)
                .ifPresent(header -> {
                    header.getChildren().findFirst()
                        .filter(comp -> comp instanceof H2)
                        .map(comp -> (H2) comp)
                        .ifPresent(title -> title.setText("Edit Budget Item"));
                });
        }
    }
    
    private void hideFormPanel() {
        formPanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
        editingItem = null;
        
        // Show add button when form is closed
        addItemButton.setVisible(true);
    }
    
    private void clearForm() {
        categoryField.clear();
        categoryTypeSelect.clear();
        plannedField.clear();
        actualField.clear();
    }
    
    private void populateForm(BudgetItem item) {
        categoryField.setValue(item.getCategory());
        categoryTypeSelect.setValue(item.getCategoryType());
        plannedField.setValue(item.getPlanned());
        actualField.setValue(item.getActual());
    }
    
    private void saveItem() {
        // Validate form
        if (categoryField.isEmpty() || categoryTypeSelect.isEmpty() || 
            plannedField.isEmpty() || actualField.isEmpty()) {
            return;
        }
        
        if (editingItem == null) {
            // Create new item
            String category = categoryField.getValue();
            Double amount = actualField.getValue();
            String categoryType = categoryTypeSelect.getValue();

            // Use the actual amount and category to create a real manual transaction
            // Positive amounts are treated as income, negatives as expenses
            bankAccountService.createManualTransaction(
                category,                    // merchantName
                category,                    // description
                amount != null ? amount : 0, // amount
                LocalDate.now(),             // transactionDate
                category,                    // budgetCategory
                categoryType                 // budgetCategoryType
            );
        } else {
            // Update existing item
            editingItem.setCategory(categoryField.getValue());
            editingItem.setCategoryType(categoryTypeSelect.getValue());
            editingItem.setPlanned(plannedField.getValue());
            editingItem.setActual(actualField.getValue());
            
            // Save the updated budget item (Planned amount) to the database
            bankAccountService.saveBudgetItem(editingItem);
        }
        
        // Refresh the dashboard and hide form
        refreshDashboard();
        hideFormPanel();
    }
    
    /**
     * Open the manual transaction dialog for adding new transactions
     */
    private void openManualTransactionDialog() {
        ManualTransactionDialog dialog = new ManualTransactionDialog(
            bankAccountService,
            (result) -> refreshDashboard() // Refresh dashboard when transaction is added
        );
        
        dialog.open();
    }
    
    private void openBankAccountManagement() {
        BankAccountManagementDialog dialog = new BankAccountManagementDialog(bankAccountService, plaidService, bankAccountService.getBankAccountRepository());
        // When dialog closes, reload the entire page so all views re-query fresh data
        dialog.addDialogCloseActionListener(e ->
            getUI().ifPresent(ui -> ui.getPage().reload())
        );
        dialog.open();
    }
    
    /**
     * Public method to refresh dashboard data - can be called externally
     */
    public void refreshBudgetData() {
        refreshDashboard();
    }
    
    /**
     * Open the transaction categorization modal for a specific category
     */
    private void openTransactionCategorizationModal(String category, String categoryType) {
        YearMonth currentPeriod = YearMonth.of(currentYear.getValue(), currentMonth.getValue());
        
        TransactionCategorizationDialog dialog = new TransactionCategorizationDialog(
            bankAccountService,
            category,
            categoryType,
            currentPeriod,
            (result) -> refreshDashboard() // Refresh dashboard when changes are saved
        );
        
        dialog.open();
    }
    
    /**
     * Show AI Advisory notifications with personalized money-saving tips
     */
    private void showAIAdvisoryNotifications() {
        try {
            YearMonth currentPeriod = YearMonth.of(currentYear.getValue(), currentMonth.getValue());
            List<AIAdvisoryService.AdvisoryTip> tips = aiAdvisoryService.generatePersonalizedTips(currentPeriod);
            
            // Show only the highest priority tip as a notification to avoid spam
            if (!tips.isEmpty()) {
                AIAdvisoryService.AdvisoryTip topTip = tips.get(0);
                showAdvisoryNotification(topTip);
            }
        } catch (Exception e) {
            // Silently fail - don't show error notifications for advisory tips
            System.err.println("Failed to generate AI advisory tips: " + e.getMessage());
        }
    }
    
    /**
     * Display a single advisory tip as a persistent notification
     */
    private void showAdvisoryNotification(AIAdvisoryService.AdvisoryTip tip) {
        // Create notification with tip content
        String notificationText = "🤖 " + tip.getTitle() + ": " + tip.getMessage();
        
        Notification notification = new Notification();
        notification.setText(notificationText);
        
        // Make notification persistent - it will stay until user closes it
        notification.setDuration(0); // 0 means it won't auto-close
        
        // Set position
        notification.setPosition(Notification.Position.TOP_END);
        
        // Set theme variant based on tip type
        switch (tip.getType()) {
            case WARNING:
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                break;
            case OPPORTUNITY:
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                break;
            case INSIGHT:
                notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                break;
            case SUGGESTION:
            default:
                notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                break;
        }
        
        // Show the notification
        notification.open();
    }
    
    /**
     * Create sidebar header with month/year and notification bell
     */
    private HorizontalLayout createSidebarHeader() {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);
        
        // Month/Year header
        monthHeader = new H2();
        updateMonthHeader();
        monthHeader.addClassName("month-header");
        monthHeader.getStyle().set("margin", "0")
                               .set("font-size", "1.2em");
        
        // Notification bell button with badge
        notificationBellButton = new Button(VaadinIcon.BELL.create());
        notificationBellButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        notificationBellButton.getElement().setProperty("title", "View AI Financial Recommendations");
        notificationBellButton.addClickListener(e -> openNotificationCenter());
        
        // Add CSS class for styling
        notificationBellButton.addClassName("notification-bell");
        
        headerLayout.add(monthHeader, notificationBellButton);
        return headerLayout;
    }
    
    /**
     * Open the notification center dialog
     */
    private void openNotificationCenter() {
        YearMonth currentPeriod = YearMonth.of(currentYear.getValue(), currentMonth.getValue());
        NotificationCenterDialog dialog = new NotificationCenterDialog(aiAdvisoryService, currentPeriod);
        dialog.open();
    }
    
    /**
     * Update notification bell badge based on available tips
     */
    private void updateNotificationBadge() {
        try {
            YearMonth currentPeriod = YearMonth.of(currentYear.getValue(), currentMonth.getValue());
            List<AIAdvisoryService.AdvisoryTip> tips = aiAdvisoryService.generatePersonalizedTips(currentPeriod);
            
            if (!tips.isEmpty()) {
                // Set notification count for CSS badge
                notificationBellButton.getElement().setAttribute("data-count", String.valueOf(tips.size()));
                
                // Check if there are high priority tips (80+)
                boolean hasHighPriority = tips.stream().anyMatch(tip -> tip.getPriority() >= 80);
                
                // Update CSS classes based on priority and count
                notificationBellButton.removeClassName("no-notifications");
                notificationBellButton.addClassName("has-notifications");
                
                if (hasHighPriority) {
                    notificationBellButton.addClassName("high-priority");
                    notificationBellButton.addClassName("notification-pulse");
                } else {
                    notificationBellButton.removeClassName("high-priority");
                    notificationBellButton.addClassName("notification-pulse");
                }
                
                // Update tooltip with count
                String tooltip = String.format("🔔 %d AI Financial Recommendation%s Available", 
                    tips.size(), tips.size() > 1 ? "s" : "");
                notificationBellButton.getElement().setProperty("title", tooltip);
                
            } else {
                // No notifications - remove badge and update styling
                notificationBellButton.getElement().removeAttribute("data-count");
                notificationBellButton.removeClassName("has-notifications");
                notificationBellButton.removeClassName("high-priority");
                notificationBellButton.removeClassName("notification-pulse");
                notificationBellButton.addClassName("no-notifications");
                
                // Reset tooltip
                notificationBellButton.getElement().setProperty("title", "No financial recommendations at this time");
            }
        } catch (Exception e) {
            // Silently handle errors - set to no notifications state
            notificationBellButton.getElement().removeAttribute("data-count");
            notificationBellButton.removeClassName("has-notifications");
            notificationBellButton.removeClassName("high-priority");
            notificationBellButton.removeClassName("notification-pulse");
            notificationBellButton.addClassName("no-notifications");
        }
    }
    
    /**
     * Setup mobile navigation with hamburger menu and overlay
     */
    private void setupMobileNavigation() {
        // Create mobile navigation toggle button
        mobileNavToggle = new Button(VaadinIcon.MENU.create());
        mobileNavToggle.addClassName("mobile-nav-toggle");
        mobileNavToggle.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ICON);
        mobileNavToggle.getElement().setProperty("title", "Toggle Navigation");
        
        // Create sidebar overlay for mobile
        sidebarOverlay = new Div();
        sidebarOverlay.addClassName("sidebar-overlay");
        
        // Add click handlers
        mobileNavToggle.addClickListener(e -> toggleMobileNavigation());
        sidebarOverlay.addClickListener(e -> closeMobileNavigation());
        
        // Add to main layout
        add(mobileNavToggle, sidebarOverlay);
    }
    
    /**
     * Toggle mobile navigation sidebar
     */
    private void toggleMobileNavigation() {
        if (sidebar.hasClassName("mobile-active")) {
            closeMobileNavigation();
        } else {
            openMobileNavigation();
        }
    }
    
    /**
     * Open mobile navigation sidebar
     */
    private void openMobileNavigation() {
        sidebar.addClassName("mobile-active");
        sidebarOverlay.addClassName("mobile-active");
        
        // Prevent body scroll when sidebar is open
        getElement().executeJs("document.body.style.overflow = 'hidden'");
    }
    
    /**
     * Close mobile navigation sidebar
     */
    private void closeMobileNavigation() {
        sidebar.removeClassName("mobile-active");
        sidebarOverlay.removeClassName("mobile-active");
        
        // Restore body scroll
        getElement().executeJs("document.body.style.overflow = 'auto'");
    }
}
