package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.service.DashboardDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modern Financial Dashboard View with real data integration and USD conversion
 */
@Route(value = "modern-dashboard")
@PageTitle("Modern Dashboard | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/mobile-responsive.css")
public class ModernDashboardView extends Div {

    private final DashboardDataService dashboardDataService;

    public ModernDashboardView(DashboardDataService dashboardDataService) {
        this.dashboardDataService = dashboardDataService;
        
        setSizeFull();
        addClassName("modern-dashboard");

        createLayout();
    }

    private void createLayout() {
        // Create split layout with sidebar on left and content on right
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(4); // 20% for sidebar, 80% for content
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
        navContainer.getStyle()
        .set("gap", "30px");


        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", true);
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        Button refreshBtn = createNavButton(VaadinIcon.REFRESH, "Refresh", false);
        Button playBtn = createNavButton(VaadinIcon.PLAY, "Play", false);
        Button starBtn = createNavButton(VaadinIcon.STAR, "Favorites", false);
        Button userBtn = createNavButton(VaadinIcon.USER, "Profile", false);
        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", false);
        Button settingsBtn = createNavButton(VaadinIcon.COG, "Settings", false);

        navContainer.add(homeBtn, trendsBtn, refreshBtn, playBtn, starBtn, userBtn, historyBtn, settingsBtn);
        
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

    private Div createLeftColumn() {
        Div column = new Div();

        // User greeting
        HorizontalLayout greeting = createUserGreeting();
        
        // Credit card
        Div creditCard = createCreditCard();
        
        // Activity Statistics
        Div activityStats = createActivityStatistics();
        
        // Goals, Monthly Plan, Shopping, Settings sections
        Div goalsSection = createGoalsSection();
        Div monthlyPlanSection = createMonthlyPlanSection();
        Div shoppingSection = createShoppingSection();
        Div settingsSection = createSettingsSection();

        column.add(greeting, creditCard, activityStats, goalsSection, monthlyPlanSection, shoppingSection, settingsSection);
        return column;
    }

    private Div createMiddleColumn() {
        Div column = new Div();

        // Summary cards
        Div summaryCards = createSummaryCards();
        
        // Activity section
        Div activitySection = createActivitySection();

        column.add(summaryCards, activitySection);
        return column;
    }

    private Div createRightColumn() {
        Div column = new Div();

        // Daily Expenses
        Div dailyExpenses = createDailyExpenses();
        
        // All Expenses
        Div allExpenses = createAllExpenses();
        
        // Savings section
        Div savingsSection = createSavingsSection();

        column.add(dailyExpenses, allExpenses, savingsSection);
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

        // Card name section
        Div nameSection = new Div();
        nameSection.addClassName("card-name-section");
        
        Span nameLabel = new Span("Name Card");
        nameLabel.addClassName("card-name-label");
        
        Span cardName = new Span("Shayna");
        cardName.addClassName("card-name");
        
        nameSection.add(nameLabel, cardName);

        // VISA logo
        Span visaLogo = new Span("VISA");
        visaLogo.addClassName("visa-logo");

        // Card number
        HorizontalLayout cardNumber = new HorizontalLayout();
        cardNumber.addClassName("card-number");
        cardNumber.add(
            new Span("••••"),
            new Span("••••"),
            new Span("••••"),
            new Span("2022")
        );

        // Card footer
        HorizontalLayout cardFooter = new HorizontalLayout();
        cardFooter.addClassName("card-footer");

        Div expDate = new Div();
        expDate.addClassName("card-exp-date");
        Span expLabel = new Span("EXP DATE");
        expLabel.addClassName("card-label");
        Span expValue = new Span("12/23");
        expValue.addClassName("card-value");
        expDate.add(expLabel, expValue);

        Div cvvNumber = new Div();
        cvvNumber.addClassName("card-cvv");
        Span cvvLabel = new Span("CVV NUMBER");
        cvvLabel.addClassName("card-label");
        Span cvvValue = new Span("22");
        cvvValue.addClassName("card-value");
        cvvNumber.add(cvvLabel, cvvValue);

        cardFooter.add(expDate, cvvNumber);

        card.add(nameSection, visaLogo, cardNumber, cardFooter);
        return card;
    }


    private Div createActivityStatistics() {
        Div container = new Div();
        container.addClassName("activity-statistics-section");

        // Header with title and amount
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.START);
        header.getStyle().set("margin-bottom", "10px");

        Span title = new Span("Activity Statistics");
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        VerticalLayout rightSide = new VerticalLayout();
        rightSide.setPadding(false);
        rightSide.setSpacing(false);
        rightSide.getStyle()
            .set("align-items", "flex-end")
            .set("gap", "2px");

        Span amount = new Span("$90k");
        amount.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "700")
            .set("color", "white")
            .set("line-height", "1");

        Span dateRange = new Span("BETWEEN Mar 9 - 22");
        dateRange.getStyle()
            .set("font-size", "11px")
            .set("color", "var(--secondary-color)")
            .set("font-weight", "500");

        rightSide.add(amount, dateRange);
        header.add(title, rightSide);

        // Vaadin Area Chart
        Chart chart = createActivityChart();
        chart.setHeight("180px");
        chart.getStyle()
            .set("margin-top", "15px");

        container.add(header, chart);
        return container;
    }

    private Chart createActivityChart() {
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
        
        // Get real activity statistics (last 30 days by day of month)
        Map<Integer, Double> activityData = dashboardDataService.getActivityStatistics();
        
        // Prepare data for chart (sample every other day for cleaner display)
        List<String> categories = new ArrayList<>();
        List<Number> dataPoints = new ArrayList<>();
        double maxValue = 0;
        
        for (int day = 2; day <= 22; day += 2) {
            categories.add(String.valueOf(day));
            double amountIDR = activityData.getOrDefault(day, 0.0);
            double amountUSD = dashboardDataService.convertToUSD(amountIDR);
            dataPoints.add(amountUSD / 1000); // Convert to thousands for chart
            maxValue = Math.max(maxValue, amountUSD / 1000);
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
        yAxis.setMax(Math.ceil(maxValue / 10) * 10 + 10); // Round up to nearest 10
        yAxis.setTickInterval(10);
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

        // Header with title and date range
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "20px");

        H3 title = new H3("Recents Activity");
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        Span dateRange = new Span("Last 21 days");
        dateRange.getStyle()
            .set("font-size", "11px")
            .set("color", "var(--secondary-color)")
            .set("padding", "8px 16px")
            .set("background", "#1f1c2d")
            .set("border-radius", "8px");

        header.add(title, dateRange);

        // Activity list with real transaction data
        Div activityList = new Div();
        activityList.addClassName("activity-list");

        // Get real transactions grouped by date
        Map<String, List<BankTransaction>> transactionsByDate = dashboardDataService.getRecentTransactions(21);
        
        if (transactionsByDate.isEmpty()) {
            // Show message when no transactions
            Span noData = new Span("No recent transactions");
            noData.getStyle()
                .set("color", "var(--secondary-color)")
                .set("font-size", "14px")
                .set("padding", "20px")
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

        container.add(header, activityList);
        return container;
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
        
        return createActivityTransaction(merchant, dateTime, category, amountStr, icon, categoryColor);
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

    private Div createActivityTransaction(String merchant, String dateTime, String category, String amount, VaadinIcon icon, String categoryColor) {
        Div transaction = new Div();
        transaction.addClassName("activity-item");

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

        // Goal expenses section
        Div goalExpenses = new Div();
        goalExpenses.getStyle().set("position", "absolute")
                   .set("top", "19px")
                   .set("left", "96px")
                   .set("width", "185px")
                   .set("height", "24px");
        
        Span rangeText = new Span("Rp 70.000 - Rp 100.000");
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
        
        Div progressFill = new Div();
        progressFill.getStyle().set("width", "161px")
                    .set("height", "6px")
                    .set("background-color", "#e19252")
                    .set("border-radius", "10px");
        progressBar.add(progressFill);

        // Subtext
        Div subtext = new Div();
        subtext.getStyle().set("position", "absolute")
               .set("top", "63px")
               .set("left", "96px")
               .set("width", "180px")
               .set("height", "15px");
        
        Span rangeSubtext = new Span("Almost reached the spending limit!");
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

        header.add(title, viewAll);

        // Savings goals
        Div savingsGoals = new Div();
        savingsGoals.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "15px");

        // Add sample savings goals
        savingsGoals.add(createSavingsGoalWithIcon("New House", "Rp 250.000.000", "Rp 300.000.000", 83, VaadinIcon.HOME));
        savingsGoals.add(createSavingsGoalWithIcon("PC Gaming", "Rp 10.000.000", "Rp 20.000.000", 50, VaadinIcon.DESKTOP));
        savingsGoals.add(createSavingsGoalWithIcon("Summer Trip", "Rp 100.000", "Rp 1.000.000", 14, VaadinIcon.AIRPLANE));

        container.add(header, savingsGoals);
        return container;
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
}
