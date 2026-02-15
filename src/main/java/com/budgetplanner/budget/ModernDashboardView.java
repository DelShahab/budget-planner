package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BankAccount;
import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.model.SavingsGoal;
import com.budgetplanner.budget.repository.BankAccountRepository;
import com.budgetplanner.budget.repository.BankTransactionRepository;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.TransactionMetaService;
import com.budgetplanner.budget.service.PlaidService;
import com.budgetplanner.budget.service.SimplifiedEnhancedPlaidService;
import com.budgetplanner.budget.service.RecurringTransactionService;
import com.budgetplanner.budget.service.SavingsGoalService;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.AvatarHelper;
import com.budgetplanner.budget.util.CurrencyFormatter;
import com.budgetplanner.budget.view.BankAccountManagementDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.server.StreamResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modern Financial Dashboard View with real data integration and USD conversion
 */
@Route(value = "")
@PageTitle("Dashboard | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/mobile-responsive.css")
@CssImport(value = "./themes/modern-dashboard/components/vaadin-dialog.css", themeFor = "vaadin-dialog-overlay")
@JsModule("./plaid-link.js")
public class ModernDashboardView extends Div {

    private static final String CATEGORY_INCOME = "INCOME";
    private static final String CATEGORY_EXPENSES = "EXPENSES";
    private static final String CATEGORY_BILLS = "BILLS";
    private static final String CATEGORY_SAVINGS = "SAVINGS";

    private final DashboardDataService dashboardDataService;
    private final BudgetItemRepository budgetItemRepository;
    private final BankAccountService bankAccountService;
    private final PlaidService plaidService;
    private final BankAccountRepository bankAccountRepository;
    private final SavingsGoalService savingsGoalService;
    private final BankTransactionRepository bankTransactionRepository;
    private final TransactionMetaService transactionMetaService;
    private final RecurringTransactionService recurringTransactionService;
    private final UserSessionService userSessionService;
    
    private Div creditCardSection;
    private Div savingsSectionContainer;
    private Div summaryCardsContainer;
    private Div activitySectionContainer;
    private Div activityStatsContainer;
    private Div dailyExpensesContainer;
    private Div allExpensesContainer;

    @Autowired
    public ModernDashboardView(DashboardDataService dashboardDataService,
                               BudgetItemRepository budgetItemRepository,
                               BankAccountService bankAccountService,
                               PlaidService plaidService,
                               BankAccountRepository bankAccountRepository,
                               SavingsGoalService savingsGoalService,
                               BankTransactionRepository bankTransactionRepository,
                               UserSessionService userSessionService,
                               TransactionMetaService transactionMetaService,
                               RecurringTransactionService recurringTransactionService) {
        this.plaidService = plaidService;
        this.bankAccountService = bankAccountService;
        this.dashboardDataService = dashboardDataService;
        this.budgetItemRepository = budgetItemRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.savingsGoalService = savingsGoalService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.userSessionService = userSessionService;
        this.transactionMetaService = transactionMetaService;
        this.recurringTransactionService = recurringTransactionService;
        
        setSizeFull();
        addClassName("modern-dashboard");

        createLayout();
        addFloatingActionButton();
    }
    
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Refresh savings section when navigating back to dashboard
        if (savingsSectionContainer != null) {
            refreshSavingsSection();
        }
    }

    private void createLayout() {
        // Create split layout with sidebar on left and content on right
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(5); // 20% for sidebar, 80% for content
        splitLayout.addClassName("dashboard-split");

        // Create sidebar
        VerticalLayout sidebar = createSidebar();
        sidebar.addClassName("dashboard-sidebar");
        sidebar.setWidthFull();

        // Create main content wrapper
        HorizontalLayout mainWrapper = new HorizontalLayout();
        mainWrapper.addClassName("dashboard-main-content");
        mainWrapper.setSizeFull();
        mainWrapper.setPadding(false);
        mainWrapper.setSpacing(false);

        // Add 3 columns
        Div leftColumn = createLeftColumn();
        leftColumn.addClassName("dashboard-left-column");

        Div middleColumn = createMiddleColumn();
        middleColumn.addClassName("dashboard-middle-column");

        Div rightColumn = createRightColumn();
        rightColumn.addClassName("dashboard-right-column");

        mainWrapper.add(leftColumn, middleColumn, rightColumn);

        // Add to split layout
        splitLayout.addToPrimary(sidebar);
        splitLayout.addToSecondary(mainWrapper);

        add(splitLayout);
    }

    private VerticalLayout createSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.getStyle().set("gap", "35px");

        // Logo at top - show user avatar if available
        Div logo = AvatarHelper.createAvatarLogo(userSessionService);

        // Navigation icons container
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle()
        .set("gap", "30px");


        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", true);
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        Button planBtn = createNavButton(VaadinIcon.CALENDAR, "Monthly Plan", false);
        Button notificationsBtn = createNavButton(VaadinIcon.STAR, "Notifications", false);
        Button userBtn = createNavButton(VaadinIcon.USER, "Profile", false);
        
        // Add click listeners for navigation
        trendsBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("trends"));
        });
        
        userBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("user-settings"));
        });
        
        recurringBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("recurring-transactions"));
        });
        
        savingsBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("savings"));
        });
        
        notificationsBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("notifications"));
        });
        
        planBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("monthly-plan"));
        });

        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", false);
        historyBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("history"));
        });
        
        Button settingsBtn = createNavButton(VaadinIcon.COG, "Settings", false);
        
        // Open Bank Account Management when settings clicked
        settingsBtn.addClickListener(e -> openBankAccountManagement());

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
    
    private Button createUserAvatarButton() {
        Button btn = new Button();
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btn.addClassName("nav-button");
        btn.getElement().setProperty("title", "Profile");
        
        // Check if user has avatar in session
        if (userSessionService.hasAvatar()) {
            // Show avatar image
            Image avatarImg = new Image();
            byte[] avatarData = userSessionService.getAvatarFromSession();
            StreamResource resource = new StreamResource("avatar.png", 
                () -> new ByteArrayInputStream(avatarData));
            avatarImg.setSrc(resource);
            avatarImg.setWidth("32px");
            avatarImg.setHeight("32px");
            avatarImg.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover");
            btn.setIcon(avatarImg);
        } else {
            // Show initials placeholder
            String initials = userSessionService.getUserInitials();
            Div initialsDiv = new Div();
            initialsDiv.getStyle()
                .set("width", "32px")
                .set("height", "32px")
                .set("border-radius", "50%")
                .set("background", "#01a1be")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "white")
                .set("font-size", "14px")
                .set("font-weight", "bold");
            initialsDiv.add(new Span(initials));
            btn.getElement().appendChild(initialsDiv.getElement());
        }
        
        return btn;
    }

    private Div createLeftColumn() {
        Div column = new Div();

        // User greeting
        HorizontalLayout greeting = createUserGreeting();
        
        // Credit card - store reference for updates
        creditCardSection = createCreditCard();
        
        // Activity Statistics - store reference for updates
        activityStatsContainer = createActivityStatistics();
        
        // Goals, Monthly Plan, Shopping, Settings sections
        Div goalsSection = createGoalsSection();
        Div monthlyPlanSection = createMonthlyPlanSection();
        Div shoppingSection = createShoppingSection();
        Div settingsSection = createSettingsSection();

        column.add(greeting, creditCardSection, activityStatsContainer, goalsSection, monthlyPlanSection, shoppingSection, settingsSection);
        return column;
    }

    private Div createMiddleColumn() {
        Div column = new Div();

        // Summary cards - store reference for updates
        summaryCardsContainer = createSummaryCards();
        
        // Activity section - store reference for updates
        activitySectionContainer = createActivitySection();

        column.add(summaryCardsContainer, activitySectionContainer);
        return column;
    }

    private Div createRightColumn() {
        Div column = new Div();

        // Daily Expenses - store reference for updates
        dailyExpensesContainer = createDailyExpenses();
        
        // All Expenses - store reference for updates
        allExpensesContainer = createAllExpenses();
        
        // Savings section - store reference for updates
        savingsSectionContainer = createSavingsSection();

        column.add(dailyExpensesContainer, allExpensesContainer, savingsSectionContainer);
        return column;
    }

    private Div createSummaryCards() {
        Div summaryContainer = new Div();
        summaryContainer.addClassName("summary-cards-grid");

        // Get real data
        double earnings = dashboardDataService.getTotalEarnings();
        double spendings = dashboardDataService.getTotalSpendings();
        
        // Total Earnings (USD)
        String earningsUSD = dashboardDataService.formatUSD(dashboardDataService.convertToUSD(earnings));
        Div totalEarnings = createSummaryCard("Total Earnings", earningsUSD, VaadinIcon.TRENDING_UP, "total-earnings");
        
        // Total Spendings (USD)
        String spendingsUSD = dashboardDataService.formatUSD(dashboardDataService.convertToUSD(spendings));
        Div totalSpendings = createSummaryCard("Total Spendings", spendingsUSD, VaadinIcon.TRENDING_DOWN, "total-spendings");
        
        // Goal for This Month (static for now)
        Div goalMonth = createSummaryCard("Goal for This Month", "$1,632.00", VaadinIcon.BULLSEYE, "goal-month");
        
        // Spending Goal (static for now)
        Div spendingGoal = createSummaryCard("Spending Goal", "$144.00", VaadinIcon.WALLET, "spending-goal");

        summaryContainer.add(totalEarnings, totalSpendings, goalMonth, spendingGoal);
        return summaryContainer;
    }

    private HorizontalLayout createUserGreeting() {
        HorizontalLayout greeting = new HorizontalLayout();
        greeting.addClassName("user-greeting");
        // Greeting text
        Span goodMorning = new Span("Good Morning Del Shahab!");
        goodMorning.addClassName("greeting-text");

        greeting.add(goodMorning);
        return greeting;
    }

    private Div createCreditCard() {
        Div card = new Div();
        card.addClassName("credit-card");

        // Get first active bank account
        List<BankAccount> accounts = bankAccountService.getActiveBankAccounts();
        BankAccount primaryAccount = accounts.isEmpty() ? null : accounts.get(0);

        // Card name section
        Div nameSection = new Div();
        nameSection.addClassName("card-name-section");
        
        Span nameLabel = new Span("Bank Account");
        nameLabel.addClassName("card-name-label");
        
        Span cardName = new Span(primaryAccount != null ? primaryAccount.getAccountName() : "No Account Linked");
        cardName.addClassName("card-name");
        
        nameSection.add(nameLabel, cardName);

        // Account type logo
        Span accountTypeLogo = new Span(primaryAccount != null ? primaryAccount.getAccountType().toUpperCase() : "");
        accountTypeLogo.addClassName("visa-logo");
        accountTypeLogo.setVisible(false);

        // Account/Card number (last 4 digits)
        HorizontalLayout cardNumber = new HorizontalLayout();
        cardNumber.addClassName("card-number");
        if (primaryAccount != null && primaryAccount.getMask() != null) {
            cardNumber.add(
                new Span("••••"),
                new Span("••••"),
                new Span("••••"),
                new Span(primaryAccount.getMask())
            );
        } else {
            cardNumber.add(new Span("No account connected"));
        }

        // Card footer
        HorizontalLayout cardFooter = new HorizontalLayout();
        cardFooter.addClassName("card-footer");

        Div accountInfo = new Div();
        accountInfo.addClassName("card-exp-date");
        Span infoLabel = new Span("STATUS");
        infoLabel.addClassName("card-label");
        Span infoValue = new Span(primaryAccount != null && primaryAccount.getIsActive() ? "Active" : "Inactive");
        infoValue.addClassName("card-value");
        accountInfo.add(infoLabel, infoValue);

        Div institutionInfo = new Div();
        institutionInfo.addClassName("card-cvv");
        Span institutionLabel = new Span("BANK");
        institutionLabel.addClassName("card-label");
        Span institutionValue = new Span(primaryAccount != null ? 
            primaryAccount.getInstitutionName() : "N/A");
        institutionValue.addClassName("card-value");
        institutionInfo.add(institutionLabel, institutionValue);

        cardFooter.add(accountInfo, institutionInfo);

        card.add(nameSection, accountTypeLogo, cardNumber, cardFooter);
        return card;
    }


    private Div createActivityStatistics() {
        Div container = new Div();
        container.addClassName("activity-statistics");

        // Header with title and stats (amount + date range)
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        // header.getStyle().set("margin-bottom", "20px");

        Span title = new Span("Activity Statistics");
        title.addClassName("section-title");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "18px")
            .set("font-weight", "600");
        
        // Stats display (amount and date range text) on the right
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setPadding(false);
        statsLayout.setSpacing(true);
        statsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        statsLayout.getStyle().set("gap", "10px");
        
        Span amount = new Span();
        amount.getStyle()
            .set("font-size", "28px")
            .set("font-weight", "700")
            .set("color", "white")
            .set("line-height", "1");
        
        Span dateRangeText = new Span();
        dateRangeText.getStyle()
            .set("font-size", "12px")
            .set("color", "var(--secondary-color)")
            .set("font-weight", "500");
        
        statsLayout.add(amount);
        header.add(title, statsLayout);

        // Date range picker section
        HorizontalLayout datePickerLayout = new HorizontalLayout();
        datePickerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        datePickerLayout.setSpacing(true);
        datePickerLayout.getStyle().set("margin-bottom", "15px");
        
        LocalDate today = LocalDate.now();
        LocalDate defaultStartDate = today.minusDays(30);
        
        DatePicker startDatePicker = new DatePicker("From");
        startDatePicker.setValue(defaultStartDate);
        startDatePicker.setWidth("140px");
        startDatePicker.getStyle()
            .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
            .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
            .set("--lumo-contrast-90pct", "white");
        
        DatePicker endDatePicker = new DatePicker("To");
        endDatePicker.setValue(today);
        endDatePicker.setWidth("140px");
        endDatePicker.getStyle()
            .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
            .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
            .set("--lumo-contrast-90pct", "white");
        
        datePickerLayout.add(startDatePicker, endDatePicker);

        // Chart placeholder
        Div chartContainer = new Div();
        chartContainer.getStyle().set("margin-top", "10px");
        
        // Initial load
        updateActivityStatistics(chartContainer, amount, dateRangeText, defaultStartDate, today);
        
        // Add listeners for date changes
        startDatePicker.addValueChangeListener(event -> {
            LocalDate startDate = event.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                updateActivityStatistics(chartContainer, amount, dateRangeText, startDate, endDate);
            }
        });
        
        endDatePicker.addValueChangeListener(event -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = event.getValue();
            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                updateActivityStatistics(chartContainer, amount, dateRangeText, startDate, endDate);
            }
        });

        container.add(header, datePickerLayout, chartContainer);
        return container;
    }
    
    private void updateActivityStatistics(Div chartContainer, Span amountSpan, Span dateRangeSpan, 
                                         LocalDate startDate, LocalDate endDate) {
        chartContainer.removeAll();
        
        // Get activity statistics for date range
        Map<LocalDate, Double> activityData = dashboardDataService.getActivityStatisticsByDateRange(startDate, endDate);
        
        // Calculate total
        double totalExpenses = activityData.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalExpensesUSD = dashboardDataService.convertToUSD(totalExpenses);
        
        // Update amount display
        String formattedAmount = CurrencyFormatter.formatCompactUSD(totalExpensesUSD);
        amountSpan.setText(formattedAmount);
        
        // Update date range text
        String dateRangeStr = startDate.format(DateTimeFormatter.ofPattern("MMM d")) + " - " + 
                             endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        dateRangeSpan.setText(dateRangeStr);
        
        // Create chart
        Chart chart = createActivityChartForDateRange(activityData, startDate, endDate);
        chart.setHeight("180px");
        chartContainer.add(chart);
    }

    private Chart createActivityChartForDateRange(Map<LocalDate, Double> activityData, 
                                                   LocalDate startDate, LocalDate endDate) {
        Chart chart = new Chart(ChartType.AREASPLINE);
        
        // Configure chart appearance
        Configuration conf = chart.getConfiguration();
        conf.setTitle("");
        conf.getChart().setBackgroundColor(new SolidColor(0, 0, 0, 0)); // Transparent
        
        // Configure tooltip
        Tooltip tooltip = conf.getTooltip();
        tooltip.setEnabled(true);
        tooltip.setValuePrefix("$");
        tooltip.setBackgroundColor(new SolidColor("#1f2937"));
        tooltip.setBorderColor(new SolidColor("#374151"));
        com.vaadin.flow.component.charts.model.style.Style tooltipStyle = new com.vaadin.flow.component.charts.model.style.Style();
        tooltipStyle.setColor(new SolidColor("#ffffff"));
        tooltip.setStyle(tooltipStyle);
        
        // Disable legend
        conf.getLegend().setEnabled(false);
        
        // Prepare data for chart
        List<String> categories = new ArrayList<>();
        List<Number> dataPoints = new ArrayList<>();
        double maxValue = 0;
        
        // Sample data points based on range size
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int sampleInterval = daysBetween > 30 ? 3 : (daysBetween > 14 ? 2 : 1);
        
        LocalDate currentDate = startDate;
        int dayCounter = 0;
        while (!currentDate.isAfter(endDate)) {
            if (dayCounter % sampleInterval == 0) {
                categories.add(currentDate.format(DateTimeFormatter.ofPattern("MMM d")));
                double amountIDR = activityData.getOrDefault(currentDate, 0.0);
                double amountUSD = dashboardDataService.convertToUSD(amountIDR);
                dataPoints.add(amountUSD);
                maxValue = Math.max(maxValue, amountUSD);
            }
            currentDate = currentDate.plusDays(1);
            dayCounter++;
        }
        
        // X-Axis Configuration (dates)
        XAxis xAxis = conf.getxAxis();
        xAxis.setCategories(categories.toArray(new String[0]));
        xAxis.setGridLineWidth(0);
        xAxis.setLineColor(new SolidColor(255, 255, 255, 0.1));
        com.vaadin.flow.component.charts.model.style.Style xStyle = new com.vaadin.flow.component.charts.model.style.Style();
        xStyle.setColor(new SolidColor("#9CA3AF"));
        xStyle.setFontSize("11px");
        xAxis.getLabels().setStyle(xStyle);
        
        // Y-Axis Configuration (values) - dynamic max based on data
        YAxis yAxis = conf.getyAxis();
        yAxis.setTitle("");
        yAxis.setMin(0);
        double yMax = maxValue > 0 ? Math.ceil(maxValue / 10) * 10 + 10 : 100;
        yAxis.setMax(yMax);
        yAxis.setTickInterval(Math.max(10, yMax / 10));
        yAxis.setGridLineColor(new SolidColor(255, 255, 255, 0.1));
        yAxis.setGridLineWidth(1);
        com.vaadin.flow.component.charts.model.style.Style yStyle = new com.vaadin.flow.component.charts.model.style.Style();
        yStyle.setColor(new SolidColor("#9CA3AF"));
        yStyle.setFontSize("11px");
        yAxis.getLabels().setStyle(yStyle);
        yAxis.getLabels().setFormatter("function() { return this.value + 'k'; }");
        
        // Data series with real data
        DataSeries series = new DataSeries();
        series.setName("Activity");
        series.setData(dataPoints.toArray(new Number[0]));
        
        // Style the area and line
        PlotOptionsAreaspline plotOptions = new PlotOptionsAreaspline();
        plotOptions.setColor(new SolidColor("#f59e0b")); // Orange line
        plotOptions.setFillColor(new SolidColor(245, 158, 11, 0.3)); // Orange fill with transparency
        plotOptions.setLineWidth(2);
        plotOptions.setMarker(new Marker(false)); // Hide markers
        
        // Smooth line
        series.setPlotOptions(plotOptions);
        conf.addSeries(series);
        
        return chart;
    }

    private Div createActivitySection() {
        Div container = new Div();
        container.addClassName("activity-section");

        // Header with title and date filter
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "20px");

        H3 title = new H3("Recents Activity");
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        // Date filter ComboBox
        ComboBox<Integer> dateFilter = new ComboBox<>();
        dateFilter.setItems(7, 14, 21, 30, 60, 90);
        dateFilter.setValue(21);
        dateFilter.setPlaceholder("Filter by days");
        dateFilter.setWidth("150px");
        dateFilter.getStyle()
            .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
            .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
            .set("--lumo-contrast-90pct", "white");
        dateFilter.setItemLabelGenerator(days -> "Last " + days + " days");

        header.add(title, dateFilter);

        // Activity list with real transaction data (with scrolling)
        Div activityList = new Div();
        activityList.addClassName("activity-list");
        activityList.getStyle()
            .set("max-height", "600px")
            .set("overflow-y", "auto")
            .set("overflow-x", "hidden")
            .set("padding-right", "10px");

        // Initial load with default 21 days
        loadActivityTransactions(activityList, 21);
        
        // Add change listener to filter
        dateFilter.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadActivityTransactions(activityList, event.getValue());
            }
        });

        container.add(header, activityList);
        return container;
    }
    
    private void loadActivityTransactions(Div activityList, int days) {
        activityList.removeAll();
        
        // Get real transactions grouped by date
        Map<String, List<BankTransaction>> transactionsByDate = dashboardDataService.getRecentTransactions(days);
        
        if (transactionsByDate.isEmpty()) {
            // Show message when no transactions
            Span noData = new Span("No transactions found for the selected period");
            noData.getStyle()
                .set("color", "var(--secondary-color)")
                .set("font-size", "14px")
                .set("padding", "40px 20px")
                .set("text-align", "center")
                .set("display", "block");
            activityList.add(noData);
        } else {
            // Add date sections with real transactions
            transactionsByDate.forEach((date, transactions) -> {
                List<Div> transactionDivs = transactions.stream()
                    .limit(5) // Limit to 5 transactions per date
                    .map(this::createActivityTransactionFromData)
                    .toList();
                
                if (!transactionDivs.isEmpty()) {
                    activityList.add(createDateSection(date, transactionDivs.toArray(new Div[0])));
                }
            });
        }
    }
    
    private Div createActivityTransactionFromData(BankTransaction transaction) {
        String merchant = transaction.getMerchantName();
        String category = transaction.getBudgetCategory() != null ? transaction.getBudgetCategory() : "Other";
        double amountUSD = dashboardDataService.convertToUSD(transaction.getAmount());
        String amountStr = (transaction.getAmount() >= 0 ? "+" : "") + dashboardDataService.formatUSD(amountUSD);
        String categoryColor = dashboardDataService.getCategoryColor(category);
        
        // Determine icon based on category
        VaadinIcon icon = getIconForCategory(category);
        
        // Format date and time
        String dateTime = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("d MMM, yyyy"));
        
        return createActivityTransaction(merchant, dateTime, category, amountStr, icon, categoryColor, transaction);
    }
    
    private VaadinIcon getIconForCategory(String category) {
        return switch (category != null ? category.toLowerCase() : "") {
            case "shopping", "retail" -> VaadinIcon.CART;
            case "platform", "transfer" -> VaadinIcon.EXCHANGE;
            case "food & drinks", "food", "dining" -> VaadinIcon.COFFEE;
            case "business", "income" -> VaadinIcon.BRIEFCASE;
            case "transportation", "travel" -> VaadinIcon.CAR;
            default -> VaadinIcon.MONEY;
        };
    }

    private Div createDateSection(String date, Div... transactions) {
        Div section = new Div();
        section.getStyle().set("margin-bottom", "25px");

        Span dateLabel = new Span(date);
        dateLabel.getStyle()
            .set("font-size", "12px")
            .set("font-weight", "500")
            .set("color", "var(--secondary-color)")
            .set("display", "block")
            .set("margin-bottom", "15px");

        section.add(dateLabel);
        for (Div transaction : transactions) {
            section.add(transaction);
        }

        return section;
    }

    private Div createActivityTransaction(String merchant, String dateTime, String category, String amount, VaadinIcon icon, String categoryColor, BankTransaction bankTransaction) {
        Div transaction = new Div();
        transaction.addClassName("activity-item");
        
        // Make the entire transaction clickable with hover effect
        transaction.getStyle()
            .set("cursor", "pointer")
            .set("transition", "all 0.2s ease");
        
        // Add click listener to open transaction details dialog
        transaction.getElement().addEventListener("click", e -> {
            openTransactionDetailsDialog(bankTransaction, amount, categoryColor);
        }).addEventData("event.stopPropagation()");
        
        // Add hover effect
        transaction.getElement().addEventListener("mouseenter", e -> {
            transaction.getStyle().set("background", "rgba(255, 255, 255, 0.05)");
        });
        
        transaction.getElement().addEventListener("mouseleave", e -> {
            transaction.getStyle().set("background", "transparent");
        });

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        // Icon
        Icon transactionIcon = new Icon(icon);
        transactionIcon.getStyle()
            .set("width", "40px")
            .set("height", "40px")
            .set("padding", "10px")
            .set("background", categoryColor + "33")
            .set("border-radius", "10px")
            .set("color", categoryColor);

        // Transaction info
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.getStyle().set("flex", "1");

        Span merchantName = new Span(merchant);
        merchantName.getStyle()
            .set("font-size", "14px")
            .set("font-weight", "600")
            .set("color", "white");

        Span time = new Span(dateTime);
        time.getStyle()
            .set("font-size", "11px")
            .set("color", "var(--secondary-color)");

        info.add(merchantName, time);

        // Category badge
        Span categoryBadge = new Span(category);
        categoryBadge.getStyle()
            .set("padding", "4px 10px")
            .set("background", categoryColor + "33")
            .set("color", categoryColor)
            .set("border-radius", "6px")
            .set("font-size", "10px")
            .set("font-weight", "600")
            .set("text-transform", "uppercase");

        // Amount
        Span amountSpan = new Span(amount);
        amountSpan.getStyle()
            .set("font-size", "14px")
            .set("font-weight", "700")
            .set("color", amount.startsWith("+") ? "#22c55e" : "#ef4444")
            .set("min-width", "120px")
            .set("text-align", "right");

        // File icon button
        Icon fileIcon = new Icon(VaadinIcon.FILE_TEXT);
        fileIcon.getStyle()
            .set("width", "30px")
            .set("height", "30px")
            .set("padding", "6px")
            .set("background", "#1f1c2d")
            .set("border-radius", "6px")
            .set("color", "var(--secondary-color)")
            .set("cursor", "pointer");

        layout.add(transactionIcon, info, categoryBadge, amountSpan, fileIcon);

        transaction.add(layout);
        return transaction;
    }
    
    private void openTransactionDetailsDialog(BankTransaction transaction, String formattedAmount, String categoryColor) {
        com.budgetplanner.budget.view.TransactionDetailsDialog dialog =
            new com.budgetplanner.budget.view.TransactionDetailsDialog(
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

    private Div createSettingsSection() {
        Div container = new Div();
        container.addClassName("settings-section");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.COG);
        icon.getStyle().set("color", "#00d4ff");

        Span title = new Span("Settings");
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        Icon arrowIcon = new Icon(VaadinIcon.ARROW_RIGHT);
        arrowIcon.addClassName("section-arrow");

        HorizontalLayout leftSide = new HorizontalLayout(icon, title);
        leftSide.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSide.setSpacing(true);

        header.add(leftSide, arrowIcon);
        container.add(header);
        return container;
    }

    private Div createAllExpenses() {
        Div container = new Div();
        container.addClassName("all-expenses-section");

        Span title = new Span("All Expenses");
        title.addClassName("section-title");

        // Daily, Weekly, Monthly amounts with real data
        HorizontalLayout amounts = new HorizontalLayout();
        amounts.setWidthFull();
        amounts.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Get real expense data
        double dailyExp = dashboardDataService.getDailyExpenses();
        double weeklyExp = dashboardDataService.getWeeklyExpenses();
        double monthlyExp = dashboardDataService.getTotalSpendings();

        VerticalLayout daily = new VerticalLayout();
        daily.setPadding(false);
        daily.setSpacing(false);
        Span dailyLabel = new Span("Daily");
        dailyLabel.getStyle().set("font-size", "12px").set("color", "var(--secondary-color)");
        Span dailyAmount = new Span(dashboardDataService.formatUSD(dashboardDataService.convertToUSD(dailyExp)));
        dailyAmount.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "white");
        daily.add(dailyLabel, dailyAmount);

        VerticalLayout weekly = new VerticalLayout();
        weekly.setPadding(false);
        weekly.setSpacing(false);
        Span weeklyLabel = new Span("Weekly");
        weeklyLabel.getStyle().set("font-size", "12px").set("color", "var(--secondary-color)");
        Span weeklyAmount = new Span(dashboardDataService.formatUSD(dashboardDataService.convertToUSD(weeklyExp)));
        weeklyAmount.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "white");
        weekly.add(weeklyLabel, weeklyAmount);

        VerticalLayout monthly = new VerticalLayout();
        monthly.setPadding(false);
        monthly.setSpacing(false);
        Span monthlyLabel = new Span("Monthly");
        monthlyLabel.getStyle().set("font-size", "12px").set("color", "var(--secondary-color)");
        Span monthlyAmount = new Span(dashboardDataService.formatUSD(dashboardDataService.convertToUSD(monthlyExp)));
        monthlyAmount.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "white");
        monthly.add(monthlyLabel, monthlyAmount);

        amounts.add(daily, weekly, monthly);

        // Donut Chart visualization with legend
        Div chartContainer = new Div();
        chartContainer.getStyle()
            .set("background", "rgba(255,255,255,0.03)")
            .set("border-radius", "10px")
            .set("margin-top", "15px")
            .set("padding", "20px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "20px");

        // Chart and legend wrapper
        HorizontalLayout chartWrapper = new HorizontalLayout();
        chartWrapper.setWidthFull();
        chartWrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        chartWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        chartWrapper.getStyle().set("gap", "30px");

        // Create donut chart using border trick
        Div donutChart = new Div();
        donutChart.getStyle()
            .set("width", "140px")
            .set("height", "140px")
            .set("border-radius", "50%")
            .set("position", "relative")
            .set("flex-shrink", "0")
            .set("background", 
                "conic-gradient(from 0deg, " +
                "#ef4444 0deg 130deg, " +      // Shopping 36%
                "#22c55e 130deg 190deg, " +    // Platform 17%
                "#f59e0b 190deg 280deg, " +    // Food & Drinks 25%
                "#6b7280 280deg 360deg)");     // Other 22%

        // Inner white circle
        Div innerCircle = new Div();
        innerCircle.getStyle()
            .set("position", "absolute")
            .set("top", "25px")
            .set("left", "25px")
            .set("width", "90px")
            .set("height", "90px")
            .set("border-radius", "50%")
            .set("background", "#1b1829")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("flex-direction", "column");

        Span centerText = new Span("Total");
        centerText.getStyle()
            .set("font-size", "10px")
            .set("color", "var(--secondary-color)")
            .set("font-weight", "500");

        Span centerAmount = new Span("Rp 918k");
        centerAmount.getStyle()
            .set("font-size", "14px")
            .set("color", "white")
            .set("font-weight", "700");

        innerCircle.add(centerText, centerAmount);
        donutChart.add(innerCircle);

        // Get real expense percentages by category
        Map<String, Double> expensePercentages = dashboardDataService.getExpensePercentages();
        Map<String, Double> expensesByCategory = dashboardDataService.getExpensesByCategory();
        
        // Calculate total for center display
        double totalExpensesIDR = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        String totalExpensesUSD = dashboardDataService.formatUSD(dashboardDataService.convertToUSD(totalExpensesIDR));
        
        // Update center amount with real data
        centerAmount.setText(totalExpensesUSD);
        
        // Legend with real data
        VerticalLayout legend = new VerticalLayout();
        legend.setPadding(false);
        legend.setSpacing(false);
        legend.getStyle().set("gap", "12px");

        if (!expensePercentages.isEmpty()) {
            // Add legend items for each category with real percentages
            expensePercentages.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4) // Show top 4 categories
                .forEach(entry -> {
                    String category = entry.getKey();
                    String percentage = String.format("%.0f%%", entry.getValue());
                    String color = dashboardDataService.getCategoryColor(category);
                    legend.add(createLegendItem(category, percentage, color));
                });
        } else {
            // Fallback to default if no data
            legend.add(
                createLegendItem("No Data", "0%", "#6b7280")
            );
        }

        chartWrapper.add(donutChart, legend);
        chartContainer.add(chartWrapper);

        container.add(title, amounts, chartContainer);
        return container;
    }

    private HorizontalLayout createLegendItem(String label, String percentage, String color) {
        HorizontalLayout item = new HorizontalLayout();
        item.setAlignItems(FlexComponent.Alignment.CENTER);
        item.setSpacing(false);
        item.getStyle().set("gap", "8px");

        // Color dot
        Div dot = new Div();
        dot.getStyle()
            .set("width", "12px")
            .set("height", "12px")
            .set("border-radius", "50%")
            .set("background", color)
            .set("flex-shrink", "0");

        // Label and percentage
        VerticalLayout textContainer = new VerticalLayout();
        textContainer.setPadding(false);
        textContainer.setSpacing(false);
        textContainer.getStyle().set("gap", "2px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-size", "11px")
            .set("color", "white")
            .set("font-weight", "500");

        Span percentSpan = new Span(percentage);
        percentSpan.getStyle()
            .set("font-size", "10px")
            .set("color", "var(--secondary-color)");

        textContainer.add(labelSpan, percentSpan);
        item.add(dot, textContainer);

        return item;
    }

    private Div createDailyExpenses() {
        Div container = new Div();
        container.addClassName("daily-expenses");

        // Title
        Span title = new Span("Daily Expenses");
        title.addClassName("daily-expenses-title");

        // Daily expenses card
        Div expensesCard = new Div();
        expensesCard.addClassName("daily-expenses-card");

        // Get real daily expense data
        double todayExpenses = dashboardDataService.getTodayExpenses();
        double dailyBudget = dashboardDataService.getDailyBudgetLimit();
        double todayExpensesUSD = dashboardDataService.convertToUSD(todayExpenses);
        double dailyBudgetUSD = dashboardDataService.convertToUSD(dailyBudget);
        
        // Calculate progress percentage
        double progressPercentage = dailyBudgetUSD > 0 ? (todayExpensesUSD / dailyBudgetUSD) * 100 : 0;
        progressPercentage = Math.min(progressPercentage, 100); // Cap at 100%
        
        // Goal expenses section
        Div goalExpenses = new Div();
        goalExpenses.getStyle().set("position", "absolute")
                   .set("top", "19px")
                   .set("left", "96px")
                   .set("width", "220px")
                   .set("height", "24px");
        
        String expenseText = dashboardDataService.formatUSD(todayExpensesUSD) + " / " + dashboardDataService.formatUSD(dailyBudgetUSD);
        Span rangeText = new Span(expenseText);
        rangeText.addClassName("range-text");
        goalExpenses.add(rangeText);

        // Progress bar
        Div progressBar = new Div();
        progressBar.getStyle().set("position", "absolute")
                   .set("top", "49px")
                   .set("left", "96px")
                   .set("width", "200px")
                   .set("height", "6px")
                   .set("background-color", "#e1925233")
                   .set("border-radius", "10px");
        
        // Dynamic progress fill based on actual spending
        int fillWidth = (int) ((progressPercentage / 100.0) * 200);
        Div progressFill = new Div();
        progressFill.getStyle().set("width", fillWidth + "px")
                    .set("height", "6px")
                    .set("background-color", progressPercentage > 90 ? "#ef4444" : (progressPercentage > 75 ? "#f59e0b" : "#e19252"))
                    .set("border-radius", "10px")
                    .set("transition", "all 0.3s ease");
        progressBar.add(progressFill);

        // Subtext - dynamic based on spending
        Div subtext = new Div();
        subtext.getStyle().set("position", "absolute")
               .set("top", "63px")
               .set("left", "96px")
               .set("width", "200px")
               .set("height", "15px");
        
        String subtextMessage;
        if (progressPercentage >= 100) {
            subtextMessage = "Budget limit exceeded!";
        } else if (progressPercentage >= 90) {
            subtextMessage = "Almost reached the spending limit!";
        } else if (progressPercentage >= 75) {
            subtextMessage = "Getting close to your limit";
        } else if (progressPercentage >= 50) {
            subtextMessage = "On track with your budget";
        } else {
            subtextMessage = "Great! Well within budget";
        }
        
        Span rangeSubtext = new Span(subtextMessage);
        rangeSubtext.addClassName("range-subtext");
        subtext.add(rangeSubtext);

        // Icon
        Icon expenseIcon = new Icon(VaadinIcon.WALLET);
        expenseIcon.getStyle().set("position", "absolute")
                   .set("top", "18px")
                   .set("left", "18px")
                   .set("width", "60px")
                   .set("height", "60px");

        expensesCard.add(goalExpenses, progressBar, subtext, expenseIcon);
        container.add(title, expensesCard);
        return container;
    }

    private Div createGoalsSection() {
        Div container = new Div();
        container.addClassName("goals-section");
        container.getStyle().set("cursor", "pointer");
        
        // Add click listener to navigate to Savings Goals
        container.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("savings"));
        });

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span("Goals");
        title.addClassName("section-title");

        Icon arrowIcon = new Icon(VaadinIcon.ARROW_RIGHT);
        arrowIcon.addClassName("section-arrow");

        header.add(title, arrowIcon);
        container.add(header);
        return container;
    }

    private Div createMonthlyPlanSection() {
        Div container = new Div();
        container.addClassName("monthly-plan-section");
        container.getStyle().set("cursor", "pointer");
        
        // Add click listener to navigate to Monthly Plan
        container.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("monthly-plan"));
        });

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span("Monthly Plan");
        title.addClassName("section-title");

        Icon arrowIcon = new Icon(VaadinIcon.ARROW_RIGHT);
        arrowIcon.addClassName("section-arrow");

        header.add(title, arrowIcon);
        container.add(header);
        return container;
    }

    private Div createShoppingSection() {
        Div container = new Div();
        container.addClassName("shopping-section");
        container.getStyle().set("cursor", "pointer");
        
        // Add click listener to navigate to Trends & Activity
        container.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("trends"));
        });

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span("Shopping");
        title.addClassName("section-title");

        Icon arrowIcon = new Icon(VaadinIcon.ARROW_RIGHT);
        arrowIcon.addClassName("section-arrow");

        header.add(title, arrowIcon);
        container.add(header);
        return container;
    }

    private Div createSavingsSection() {
        Div container = new Div();
        container.addClassName("savings-section");

        // Header with title and view all
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "20px");

        Span title = new Span("My Savings");
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        Span viewAll = new Span("view all");
        viewAll.getStyle()
            .set("color", "#00d4ff")
            .set("font-size", "12px")
            .set("cursor", "pointer")
            .set("font-weight", "600");
        viewAll.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("savings"));
        });

        header.add(title, viewAll);

        // Get real savings goals
        List<SavingsGoal> activeGoals = savingsGoalService.getAllActiveGoals();
        
        // Savings summary card
        Div summaryCard = createSavingsSummaryCard(activeGoals);

        // Savings goals list
        Div savingsGoals = new Div();
        savingsGoals.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "15px");
        
        if (activeGoals.isEmpty()) {
            // Show message if no goals
            Span noGoals = new Span("No savings goals yet. Create one to start tracking!");
            noGoals.getStyle()
                .set("color", "var(--secondary-color)")
                .set("font-size", "14px")
                .set("text-align", "center")
                .set("padding", "20px");
            savingsGoals.add(noGoals);
        } else {
            // Display real savings goals (latest 3)
            activeGoals.stream()
                .limit(2) // Show latest 2 goals in dashboard
                .forEach(goal -> {
                    double currentUSD = dashboardDataService.convertToUSD(goal.getCurrentAmount());
                    double targetUSD = dashboardDataService.convertToUSD(goal.getTargetAmount());
                    String currentFormatted = dashboardDataService.formatUSD(currentUSD);
                    String targetFormatted = dashboardDataService.formatUSD(targetUSD);
                    VaadinIcon icon = getIconForGoal(goal.getIconName());
                    savingsGoals.add(createSavingsGoalWithIcon(
                        goal.getGoalName(),
                        currentFormatted,
                        targetFormatted,
                        goal.getProgressPercentage(),
                        icon
                    ));
                });
        }

        container.add(header, summaryCard, savingsGoals);
        return container;
    }
    
    private Div createSavingsSummaryCard(List<SavingsGoal> activeGoals) {
        Div card = new Div();
        card.getStyle()
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("margin-bottom", "20px");
        
        // Calculate totals
        double totalCurrent = savingsGoalService.getTotalCurrentSavings();
        double totalTarget = savingsGoalService.getTotalTargetSavings();
        int overallProgress = savingsGoalService.getOverallProgressPercentage();
        int goalCount = activeGoals.size();
        
        // Convert to USD
        double totalCurrentUSD = dashboardDataService.convertToUSD(totalCurrent);
        double totalTargetUSD = dashboardDataService.convertToUSD(totalTarget);
        
        // Top row: Label and goal count
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Span label = new Span("Total Savings");
        label.getStyle()
            .set("font-size", "12px")
            .set("color", "rgba(255, 255, 255, 0.8)")
            .set("font-weight", "500")
            .set("text-transform", "uppercase");
        
        Span goalCountBadge = new Span(goalCount + " Goal" + (goalCount != 1 ? "s" : ""));
        goalCountBadge.getStyle()
            .set("font-size", "11px")
            .set("color", "rgba(255, 255, 255, 0.9)")
            .set("background", "rgba(255, 255, 255, 0.2)")
            .set("padding", "4px 10px")
            .set("border-radius", "12px")
            .set("font-weight", "600");
        
        topRow.add(label, goalCountBadge);
        
        // Amount display
        HorizontalLayout amountRow = new HorizontalLayout();
        amountRow.setWidthFull();
        amountRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        amountRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        amountRow.getStyle().set("margin-top", "8px").set("margin-bottom", "8px");
        
        Span currentAmount = new Span(dashboardDataService.formatUSD(totalCurrentUSD));
        currentAmount.getStyle()
            .set("font-size", "23px")
            .set("font-weight", "700")
            .set("color", "white");
        
        Span targetAmount = new Span("of " + dashboardDataService.formatUSD(totalTargetUSD));
        targetAmount.getStyle()
            .set("font-size", "16px")
            .set("color", "rgba(255, 255, 255, 0.8)")
            .set("font-weight", "500");
        
        amountRow.add(currentAmount, targetAmount);
        
        // Progress bar
        Div progressBar = new Div();
        progressBar.getStyle()
            .set("width", "100%")
            .set("height", "8px")
            .set("background", "rgba(255, 255, 255, 0.2)")
            .set("border-radius", "10px")
            .set("overflow", "hidden")
            .set("margin-top", "12px");
        
        Div progressFill = new Div();
        progressFill.getStyle()
            .set("width", overallProgress + "%")
            .set("height", "100%")
            .set("background", "white")
            .set("border-radius", "10px")
            .set("transition", "width 0.3s ease");
        
        progressBar.add(progressFill);
        
        // Progress percentage text
        Span progressText = new Span(overallProgress + "% Complete");
        progressText.getStyle()
            .set("font-size", "11px")
            .set("color", "rgba(255, 255, 255, 0.9)")
            .set("margin-top", "8px")
            .set("display", "block")
            .set("font-weight", "600");
        
        card.add(topRow, amountRow, progressBar, progressText);
        return card;
    }

    private Div createSavingsGoalWithIcon(String goalName, String current, String target, int progress, VaadinIcon iconType) {
        Div goal = new Div();
        goal.addClassName("savings-goal");

        // Goal header with icon and info
        HorizontalLayout goalHeader = new HorizontalLayout();
        goalHeader.setWidthFull();
        goalHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        goalHeader.setSpacing(true);
        goalHeader.getStyle().set("margin-bottom", "10px");

        // Icon
        Icon goalIcon = new Icon(iconType);
        goalIcon.getStyle()
            .set("width", "32px")
            .set("height", "32px")
            .set("padding", "6px")
            .set("background", "rgba(0, 157, 184, 0.15)")
            .set("border-radius", "8px")
            .set("color", "#00d4ff");

        // Goal info
        VerticalLayout goalInfo = new VerticalLayout();
        goalInfo.setPadding(false);
        goalInfo.setSpacing(false);
        goalInfo.getStyle().set("flex", "1");

        Span name = new Span(goalName);
        name.getStyle()
            .set("font-size", "14px")
            .set("font-weight", "600")
            .set("color", "white");

        Span amounts = new Span(current + " - " + target);
        amounts.getStyle()
            .set("font-size", "11px")
            .set("color", "var(--secondary-color)");

        goalInfo.add(name, amounts);

        goalHeader.add(goalIcon, goalInfo);

        // Progress bar
        Div progressBar = new Div();
        progressBar.getStyle()
            .set("width", "100%")
            .set("height", "6px")
            .set("background", "#e1925233")
            .set("border-radius", "10px")
            .set("overflow", "hidden");

        Div progressFill = new Div();
        progressFill.getStyle()
            .set("width", progress + "%")
            .set("height", "100%")
            .set("background", "#e19252")
            .set("border-radius", "10px")
            .set("transition", "width 0.3s ease");

        progressBar.add(progressFill);

        goal.add(goalHeader, progressBar);
        return goal;
    }

    private Div createSummaryCard(String title, String value, VaadinIcon icon, String cssClass) {
        Div card = new Div();
        card.addClassName("modern-summary-card");
        card.addClassName(cssClass);
        
        // Card icon (60x60px as per CSS)
        Icon cardIcon = new Icon(icon);
        cardIcon.addClassName("card-icon");
        
        // Card label container
        Div cardLabel = new Div();
        cardLabel.addClassName("card-label");
        
        Span cardTitle = new Span(title);
        cardTitle.addClassName("card-title-small");
        
        Span cardValue = new Span(value);
        cardValue.addClassName("card-value-large");
        
        cardLabel.add(cardTitle, cardValue);
        card.add(cardIcon, cardLabel);
        return card;
    }
    
    private void addFloatingActionButton() {
        Button fab = new Button(new Icon(VaadinIcon.PLUS));
        fab.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        fab.addClassName("modern-fab");
        fab.getStyle()
            .set("position", "fixed")
            .set("bottom", "30px")
            .set("right", "30px")
            .set("width", "60px")
            .set("height", "60px")
            .set("border-radius", "50%")
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("cursor", "pointer")
            .set("z-index", "1000")
            .set("transition", "all 0.3s ease");
        
        fab.getElement().addEventListener("mouseenter", e -> {
            fab.getStyle().set("transform", "scale(1.1)");
            fab.getStyle().set("box-shadow", "0 12px 30px rgba(0, 212, 255, 0.6)");
        });
        
        fab.getElement().addEventListener("mouseleave", e -> {
            fab.getStyle().set("transform", "scale(1)");
            fab.getStyle().set("box-shadow", "0 8px 20px rgba(0, 212, 255, 0.4)");
        });
        
        fab.addClickListener(e -> openAddItemDialog());
        
        add(fab);
    }
    
    private void openAddItemDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("modern-dialog");
        dialog.setWidth("500px");
        
        // Apply modern dialog theme
        dialog.getElement().getThemeList().add("modern-dialog");
        
        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
            .set("padding", "20px 24px")
            .set("border-bottom", "1px solid rgba(255,255,255,0.1)");
        
        H2 dialogTitle = new H2("Add Budget Item");
        dialogTitle.getStyle()
            .set("margin", "0")
            .set("font-size", "24px")
            .set("font-weight", "700")
            .set("color", "white");
        
        Button closeButton = new Button(new Icon(VaadinIcon.CLOSE));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.getStyle()
            .set("color", "#9CA3AF")
            .set("cursor", "pointer");
        closeButton.addClickListener(e -> dialog.close());
        
        header.add(dialogTitle, closeButton);
        
        // Form content
        VerticalLayout formContent = new VerticalLayout();
        formContent.setSpacing(true);
        formContent.setPadding(true);
        formContent.getStyle().set("padding", "24px");
        
        // Category field
        TextField categoryField = new TextField("Category");
        categoryField.setPlaceholder("Enter category name");
        categoryField.setWidthFull();
        styleModernField(categoryField);
        
        // Category type select
        Select<String> categoryTypeSelect = new Select<>();
        categoryTypeSelect.setLabel("Category Type");
        categoryTypeSelect.setItems(CATEGORY_INCOME, CATEGORY_EXPENSES, CATEGORY_BILLS, CATEGORY_SAVINGS);
        categoryTypeSelect.setPlaceholder("Select type");
        categoryTypeSelect.setWidthFull();
        styleModernField(categoryTypeSelect);
        
        // Planned amount
        NumberField plannedField = new NumberField("Planned Amount");
        plannedField.setPlaceholder("0.00");
        plannedField.setPrefixComponent(new Span("$"));
        plannedField.setMin(0);
        plannedField.setWidthFull();
        styleModernField(plannedField);
        
        // Actual amount
        NumberField actualField = new NumberField("Actual Amount");
        actualField.setPlaceholder("0.00");
        actualField.setPrefixComponent(new Span("$"));
        actualField.setMin(0);
        actualField.setWidthFull();
        styleModernField(actualField);
        
        formContent.add(categoryField, categoryTypeSelect, plannedField, actualField);
        
        // Button layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("padding-top", "16px");
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.getStyle()
            .set("color", "#9CA3AF")
            .set("border-radius", "10px");
        cancelButton.addClickListener(e -> dialog.close());
        
        Button saveButton = new Button("Save Item");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border-radius", "10px")
            .set("color", "white")
            .set("font-weight", "600")
            .set("padding", "10px 24px");
        
        saveButton.addClickListener(e -> {
            if (categoryField.isEmpty() || categoryTypeSelect.isEmpty() || 
                plannedField.isEmpty() || actualField.isEmpty()) {
                Notification.show("Please fill all fields", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            saveBudgetItem(categoryField.getValue(), categoryTypeSelect.getValue(),
                         plannedField.getValue(), actualField.getValue());
            dialog.close();
        });
        
        buttonLayout.add(cancelButton, saveButton);
        
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.add(header, formContent, buttonLayout);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void styleModernField(com.vaadin.flow.component.Component field) {
        field.getElement().getStyle()
            .set("--lumo-contrast-10pct", "rgba(255,255,255,0.05)")
            .set("--lumo-contrast-20pct", "rgba(255,255,255,0.1)")
            .set("--lumo-contrast-30pct", "rgba(255,255,255,0.15)")
            .set("--lumo-body-text-color", "white")
            .set("--lumo-secondary-text-color", "#9CA3AF")
            .set("--lumo-primary-color", "#00d4ff");
    }
    
    private void saveBudgetItem(String category, String categoryType, Double planned, Double actual) {
        LocalDate now = LocalDate.now();
        BudgetItem item = new BudgetItem(
            category,
            planned.doubleValue(),
            actual.doubleValue(),
            categoryType,
            now.getYear(),
            now.getMonthValue()
        );
        
        budgetItemRepository.save(item);
        
        Notification notification = Notification.show(
            "Budget item '" + category + "' added successfully!",
            3000,
            Notification.Position.TOP_END
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        
        // Refresh the page to show new data
        getUI().ifPresent(ui -> ui.getPage().reload());
    }
    
    private void openBankAccountManagement() {
        BankAccountManagementDialog dialog = new BankAccountManagementDialog(
            bankAccountService,
            plaidService,
            bankAccountRepository
        );
        // When dialog closes, reload the entire page so all views re-query fresh data
        dialog.addDialogCloseActionListener(e -> {
            new Thread(() -> {
                try {
                    // Small delay to ensure database changes are persisted
                    Thread.sleep(300);

                    getUI().ifPresent(ui -> ui.access(() -> ui.getPage().reload()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
        dialog.open();
    }
    
    @ClientCallable
    public void onPlaidSuccess(String publicToken, String metadataJson) {
        try {
            // Exchange public token for access token and create bank account
            plaidService.exchangePublicToken(publicToken);
            
            // Refresh the credit card section to show new account
            updateCreditCardSection();
            
            // Show success notification
            Notification successNotif = Notification.show(
                "Bank account linked successfully! Your dashboard has been updated.", 
                5000, 
                Notification.Position.TOP_CENTER
            );
            successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (Exception e) {
            Notification.show("Error linking account: " + e.getMessage(),
                    5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    @ClientCallable
    public void onPlaidExit(String message) {
        Notification.show("Plaid Link closed: " + message,
                2000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }
    
    private void updateCreditCardSection() {
        if (creditCardSection != null) {
            Div newCard = createCreditCard();
            creditCardSection.removeAll();
            newCard.getChildren().forEach(creditCardSection::add);
        }
    }
    
    /**
     * Refresh the savings section with latest data
     */
    public void refreshSavingsSection() {
        if (savingsSectionContainer == null) {
            return;
        }
        
        // Clear existing content
        savingsSectionContainer.removeAll();
        
        // Recreate the savings section content
        Div newSection = createSavingsSection();
        newSection.getChildren().forEach(savingsSectionContainer::add);
    }
    
    /**
     * Comprehensive dashboard refresh - updates all sections with latest data
     */
    public void refreshDashboard() {
        // Refresh credit card section (bank accounts)
        if (creditCardSection != null) {
            creditCardSection.removeAll();
            Div newCreditCard = createCreditCard();
            newCreditCard.getChildren().forEach(creditCardSection::add);
        }
        
        // Refresh summary cards (earnings, spendings, savings)
        if (summaryCardsContainer != null) {
            summaryCardsContainer.removeAll();
            Div newSummaryCards = createSummaryCards();
            newSummaryCards.getChildren().forEach(summaryCardsContainer::add);
        }
        
        // Refresh activity section (recent transactions)
        if (activitySectionContainer != null) {
            activitySectionContainer.removeAll();
            Div newActivitySection = createActivitySection();
            newActivitySection.getChildren().forEach(activitySectionContainer::add);
        }
        
        // Refresh activity statistics
        if (activityStatsContainer != null) {
            activityStatsContainer.removeAll();
            Div newActivityStats = createActivityStatistics();
            newActivityStats.getChildren().forEach(activityStatsContainer::add);
        }
        
        // Refresh daily expenses
        if (dailyExpensesContainer != null) {
            dailyExpensesContainer.removeAll();
            Div newDailyExpenses = createDailyExpenses();
            newDailyExpenses.getChildren().forEach(dailyExpensesContainer::add);
        }
        
        // Refresh all expenses
        if (allExpensesContainer != null) {
            allExpensesContainer.removeAll();
            Div newAllExpenses = createAllExpenses();
            newAllExpenses.getChildren().forEach(allExpensesContainer::add);
        }
        
        // Refresh savings section
        refreshSavingsSection();
    }
    
    /**
     * Get VaadinIcon based on icon name string
     */
    private VaadinIcon getIconForGoal(String iconName) {
        if (iconName == null) {
            return VaadinIcon.PIGGY_BANK_COIN; // Default icon
        }
        
        try {
            return VaadinIcon.valueOf(iconName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback mapping for common icons
            return switch (iconName.toLowerCase()) {
                case "home", "house" -> VaadinIcon.HOME;
                case "desktop", "computer", "pc" -> VaadinIcon.DESKTOP;
                case "airplane", "flight", "travel", "trip" -> VaadinIcon.AIRPLANE;
                case "shield", "emergency", "safety" -> VaadinIcon.SHIELD;
                case "car", "vehicle" -> VaadinIcon.CAR;
                case "education", "school", "book" -> VaadinIcon.BOOK;
                case "health", "medical" -> VaadinIcon.HEART;
                case "gift" -> VaadinIcon.GIFT;
                default -> VaadinIcon.PIGGY_BANK_COIN;
            };
        }
    }
}
