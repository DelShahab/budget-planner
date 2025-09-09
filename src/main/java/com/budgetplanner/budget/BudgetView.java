package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BudgetItem;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;

import java.io.Serial;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.*;

@Route("")
@CssImport("./styles/budget-dashboard.css")
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
    private SplitLayout splitLayout;
    private VerticalLayout formPanel;
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

    // Sample data for demonstration
    private transient List<BudgetItem> sampleBudgetItems;


    public BudgetView() {
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
        
        // Initialize sample data
        initializeSampleData();
        
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

    private void initializeSampleData() {
        sampleBudgetItems = new ArrayList<>();
        
        // Generate data for multiple years and months
        int startYear = 2023;
        int endYear = currentYear.getValue() + 1;
        
        for (int year = startYear; year <= endYear; year++) {
            for (int monthValue = 1; monthValue <= 12; monthValue++) {
                // Skip future months beyond current date
                if ((year == currentYear.getValue() && monthValue > currentMonth.getValue()) || 
                    (year > currentYear.getValue())) {
                    continue;
                }
                
                // Add variation to make data more realistic
                double salaryVariation = 1.0 + (Math.random() - 0.5) * 0.1; // ±5% variation
                double expenseVariation = 1.0 + (Math.random() - 0.5) * 0.3; // ±15% variation
                
                // Income items
                sampleBudgetItems.add(new BudgetItem("Salary", 5000.0, 5000.0 * salaryVariation, CATEGORY_INCOME, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Freelance", 1000.0, 800.0 + Math.random() * 400, CATEGORY_INCOME, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Investment Returns", 500.0, 300.0 + Math.random() * 400, CATEGORY_INCOME, year, monthValue));
                
                // Expenses items (with seasonal variation)
                double groceryBase = 600.0 + (monthValue == 11 || monthValue == 12 ? 100.0 : 0.0); // Holiday increase
                sampleBudgetItems.add(new BudgetItem("Groceries", groceryBase, groceryBase * expenseVariation, CATEGORY_EXPENSES, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Gas", 200.0, 150.0 + Math.random() * 100, CATEGORY_EXPENSES, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Dining Out", 300.0, 200.0 + Math.random() * 200, CATEGORY_EXPENSES, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Entertainment", 150.0, 100.0 + Math.random() * 100, CATEGORY_EXPENSES, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Shopping", 250.0, 150.0 + Math.random() * 200, CATEGORY_EXPENSES, year, monthValue));
                
                // Bills items (mostly consistent)
                sampleBudgetItems.add(new BudgetItem("Rent", 1500.0, 1500.0, CATEGORY_BILLS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Utilities", 150.0, 120.0 + Math.random() * 60, CATEGORY_BILLS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Internet", 80.0, 80.0, CATEGORY_BILLS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Phone", 60.0, 55.0 + Math.random() * 15, CATEGORY_BILLS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Insurance", 200.0, 200.0, CATEGORY_BILLS, year, monthValue));
                
                // Savings items (with some variation)
                sampleBudgetItems.add(new BudgetItem("Emergency Fund", 500.0, 400.0 + Math.random() * 200, CATEGORY_SAVINGS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Vacation", 300.0, 200.0 + Math.random() * 200, CATEGORY_SAVINGS, year, monthValue));
                sampleBudgetItems.add(new BudgetItem("Retirement", 400.0, 400.0, CATEGORY_SAVINGS, year, monthValue));
            }
        }
    }

    private void buildSidebar() {
        sidebar = new Div();
        sidebar.addClassName("sidebar");
        sidebar.setWidth("180px");
        sidebar.setHeight("100%");
        
        // Month/Year header
        H2 monthHeader = new H2(currentMonth.name().substring(0, 1) + 
                               currentMonth.name().substring(1).toLowerCase());
        monthHeader.addClassName("month-header");
        
        Span budgetLabel = new Span("BUDGET DASHBOARD");
        budgetLabel.addClassName("budget-label");
        
        // Navigation tree
        navigationTree = new TreeGrid<>();
        navigationTree.addClassName("navigation-tree");
        navigationTree.setHeightFull();
        navigationTree.addHierarchyColumn(NavigationNode::getLabel).setHeader("Period");
        
        sidebar.add(monthHeader, budgetLabel, navigationTree);
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
        addItemButton.getElement().setProperty("title", "Add Budget Item (Ctrl+N)");
        addItemButton.addClickListener(e -> showCategorySelectionDialog());
        
        // Add keyboard shortcut for add button
        Shortcuts.addShortcutListener(this, this::showCategorySelectionDialog, Key.KEY_N, KeyModifier.CONTROL);
        
        mainContent.add(summaryCards, topRow, bottomRow, addItemButton);
        mainContent.setFlexGrow(0, summaryCards);
        mainContent.setFlexGrow(0, topRow);
        mainContent.setFlexGrow(1, bottomRow);
        mainContent.setFlexGrow(0, addItemButton);
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);
        layout.addClassName("summary-cards");
        
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
        return sampleBudgetItems.stream()
            .filter(item -> item.getYear().equals(currentYear.getValue()) && 
                           item.getMonth().equals(currentMonth.getValue()))
            .toList();
    }

    private Chart createAllocationChart() {
        Chart chart = new Chart(ChartType.PIE);
        chart.addClassName("allocation-chart");
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
        grid.addColumn(BudgetItem::getCategory).setHeader("Category").setFlexGrow(2).setResizable(true);
        grid.addColumn(item -> String.format(CURRENCY_FORMAT, item.getPlanned())).setHeader(HEADER_PLANNED).setFlexGrow(1).setResizable(true);
        grid.addColumn(item -> String.format(CURRENCY_FORMAT, item.getActual())).setHeader(HEADER_ACTUAL).setFlexGrow(1).setResizable(true);
        grid.addColumn(item -> {
            double percentage = item.getPlanned() > 0 ? (item.getActual() / item.getPlanned()) * 100 : 0;
            return String.format("%.0f%%", percentage);
        }).setHeader("Progress").setFlexGrow(1).setResizable(true);
        
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        
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

    private void setupNavigationTree() {
        TreeData<NavigationNode> treeData = new TreeData<>();
        
        // Create year nodes (2023 to current year + 1)
        int startYear = 2023;
        int endYear = currentYear.getValue() + 1;
        
        for (int year = startYear; year <= endYear; year++) {
            NavigationNode yearNode = new NavigationNode(Year.of(year));
            treeData.addItem(null, yearNode);
            
            // Add month children only for valid months (not future months)
            for (Month month : Month.values()) {
                // Skip future months beyond current date
                if ((year == currentYear.getValue() && month.getValue() > currentMonth.getValue()) || 
                    (year > currentYear.getValue())) {
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
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        // Filter data for current period
        List<BudgetItem> currentPeriodItems = sampleBudgetItems.stream()
                .filter(item -> item.getYear().equals(currentYear.getValue()) && 
                               item.getMonth().equals(currentMonth.getValue()))
                .toList();
        
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
        if (rolloverTotal != null) rolloverTotal.setText(String.format(CURRENCY_FORMAT, rolloverAmount));
        if (incomeTotal != null) incomeTotal.setText(String.format(CURRENCY_FORMAT, incomeSum));
        if (expensesTotal != null) expensesTotal.setText(String.format(CURRENCY_FORMAT, expensesSum));
        if (billsTotal != null) billsTotal.setText(String.format(CURRENCY_FORMAT, billsSum));
        if (savingsTotal != null) savingsTotal.setText(String.format(CURRENCY_FORMAT, savingsSum));
        if (debtTotal != null) debtTotal.setText(String.format(CURRENCY_FORMAT, debtSum));
        if (leftoverTotal != null) leftoverTotal.setText(String.format(CURRENCY_FORMAT, leftoverAmount));
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
            BudgetItem newItem = new BudgetItem();
            newItem.setCategory(categoryField.getValue());
            newItem.setPlanned(plannedField.getValue());
            newItem.setActual(actualField.getValue());
            newItem.setCategoryType(categoryTypeSelect.getValue());
            newItem.setYear(currentYear.getValue());
            newItem.setMonth(currentMonth.getValue());
            sampleBudgetItems.add(newItem);
        } else {
            // Update existing item
            editingItem.setCategory(categoryField.getValue());
            editingItem.setCategoryType(categoryTypeSelect.getValue());
            editingItem.setPlanned(plannedField.getValue());
            editingItem.setActual(actualField.getValue());
        }
        
        // Refresh the dashboard and hide form
        refreshDashboard();
        hideFormPanel();
    }
    
    private void showCategorySelectionDialog() {
        // Create a simple context menu for category selection
        Button incomeBtn = new Button("Add to Income", VaadinIcon.PLUS.create());
        incomeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        incomeBtn.addClickListener(e -> showFormPanel(null, CATEGORY_INCOME));
        
        Button expensesBtn = new Button("Add to Expenses", VaadinIcon.PLUS.create());
        expensesBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        expensesBtn.addClickListener(e -> showFormPanel(null, CATEGORY_EXPENSES));
        
        Button billsBtn = new Button("Add to Bills", VaadinIcon.PLUS.create());
        billsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        billsBtn.addClickListener(e -> showFormPanel(null, CATEGORY_BILLS));
        
        Button savingsBtn = new Button("Add to Savings", VaadinIcon.PLUS.create());
        savingsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        savingsBtn.addClickListener(e -> showFormPanel(null, CATEGORY_SAVINGS));
        
        // For now, just show the form with no pre-selected category
        // In a full implementation, you could create a dialog with these buttons
        showFormPanel(null, null);
    }
}
