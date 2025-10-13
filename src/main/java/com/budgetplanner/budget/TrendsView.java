package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.service.BankAccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.YearMonth;
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
    
    private Grid<BudgetItem> incomeGrid;
    private Grid<BudgetItem> expensesGrid;
    private Grid<BudgetItem> billsGrid;
    private Grid<BudgetItem> savingsGrid;
    private Div summaryCards;
    private Tabs categoryTabs;
    private VerticalLayout contentArea;

    @Autowired
    public TrendsView(BudgetItemRepository budgetItemRepository,
                      BankAccountService bankAccountService) {
        this.budgetItemRepository = budgetItemRepository;
        this.bankAccountService = bankAccountService;
        
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

        // Logo at top
        Div logo = new Div();
        logo.getStyle()
            .set("width", "45px")
            .set("height", "45px")
            .set("background", "#01a1be")
            .set("border-radius", "50%")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("color", "white")
            .set("font-size", "18px")
            .set("font-weight", "bold")
            .set("margin", "20px auto 0");
        Span logoText = new Span("AM");
        logo.add(logoText);

        // Navigation icons container
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle().set("gap", "30px");

        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", false);
        homeBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("modern-dashboard")));
        
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", true); // Active!
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));
        
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        savingsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("savings")));
        
        Button notificationsBtn = createNavButton(VaadinIcon.STAR, "Notifications", false);
        notificationsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("notifications")));
        
        Button userBtn = createNavButton(VaadinIcon.USER, "Profile", false);
        userBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("user-settings")));
        
        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", false);
        historyBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("history")));
        
        Button settingsBtn = createNavButton(VaadinIcon.COG, "Settings", false);

        navContainer.add(homeBtn, trendsBtn, recurringBtn, savingsBtn, notificationsBtn, userBtn, historyBtn, settingsBtn);
        
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
        
        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        refreshButton.getStyle()
            .set("border-radius", "10px");
        refreshButton.addClickListener(e -> refreshData());
        
        actions.add(refreshButton);
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
        
        Tab incomeTab = new Tab("INCOME");
        Tab expensesTab = new Tab("EXPENSES");
        Tab billsTab = new Tab("BILLS");
        Tab savingsTab = new Tab("SAVINGS");
        
        categoryTabs.add(incomeTab, expensesTab, billsTab, savingsTab);
        categoryTabs.setSelectedTab(expensesTab); // Default to expenses
        
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
        createIncomeGrid();
        createExpensesGrid();
        createBillsGrid();
        createSavingsGrid();
        
        // Initially show expenses grid (most common view)
        contentArea.add(expensesGrid);
        
        container.add(contentArea);
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
        
        grid.addColumn(item -> String.format("$%.2f", item.getPlanned()))
            .setHeader("Planned")
            .setSortable(true)
            .setWidth("150px");
        
        grid.addColumn(item -> String.format("$%.2f", item.getActual()))
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
        Grid<BudgetItem> gridToShow = null;
        
        switch (tabLabel) {
            case "INCOME":
                gridToShow = incomeGrid;
                break;
            case "EXPENSES":
                gridToShow = expensesGrid;
                break;
            case "BILLS":
                gridToShow = billsGrid;
                break;
            case "SAVINGS":
                gridToShow = savingsGrid;
                break;
        }
        
        if (gridToShow != null) {
            contentArea.add(gridToShow);
            loadDataForCategory(tabLabel, gridToShow);
        }
    }

    private void loadDataForCategory(String categoryType, Grid<BudgetItem> grid) {
        YearMonth currentMonth = YearMonth.now();
        List<BudgetItem> items = budgetItemRepository.findByCategoryTypeAndYearAndMonth(
            categoryType, currentMonth.getYear(), currentMonth.getMonthValue());
        
        // If no data, get from bank account service
        if (items.isEmpty()) {
            try {
                List<BudgetItem> transactionItems = bankAccountService
                    .generateBudgetItemsFromTransactions(YearMonth.now());
                items = transactionItems.stream()
                    .filter(item -> categoryType.equals(item.getCategoryType()))
                    .toList();
            } catch (Exception e) {
                // Fallback to empty list
            }
        }
        
        grid.setItems(items);
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
        
        // Calculate totals by category type
        Map<String, Double> plannedTotals = new HashMap<>();
        Map<String, Double> actualTotals = new HashMap<>();
        
        List<BudgetItem> allItems = budgetItemRepository.findAll();
        
        // If no data in repository, try to get from bank transactions
        if (allItems.isEmpty()) {
            try {
                allItems = bankAccountService.generateBudgetItemsFromTransactions(YearMonth.now());
            } catch (Exception e) {
                // Keep empty list
            }
        }
        
        for (BudgetItem item : allItems) {
            String categoryType = item.getCategoryType();
            plannedTotals.put(categoryType, 
                plannedTotals.getOrDefault(categoryType, 0.0) + item.getPlanned());
            actualTotals.put(categoryType, 
                actualTotals.getOrDefault(categoryType, 0.0) + item.getActual());
        }
        
        // Create summary cards for each category
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
}
