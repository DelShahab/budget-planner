package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.service.BankAccountService;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.AvatarHelper;
import com.budgetplanner.budget.util.CurrencyFormatter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modern Vaadin view for monthly budget planning
 */
@Route(value = "monthly-plan")
@PageTitle("Monthly Plan | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class MonthlyPlanView extends VerticalLayout {

    private final BankAccountService bankAccountService;
    private final DashboardDataService dashboardDataService;
    private final UserSessionService userSessionService;
    private final BudgetItemRepository budgetItemRepository;
    
    private Grid<BudgetItem> budgetGrid;
    private Div summaryCards;
    private YearMonth currentMonth;
    private ComboBox<Month> monthSelector;
    private ComboBox<Year> yearSelector;

    @Autowired
    public MonthlyPlanView(BankAccountService bankAccountService,
                          DashboardDataService dashboardDataService,
                          UserSessionService userSessionService,
                          BudgetItemRepository budgetItemRepository) {
        this.bankAccountService = bankAccountService;
        this.dashboardDataService = dashboardDataService;
        this.userSessionService = userSessionService;
        this.budgetItemRepository = budgetItemRepository;
        this.currentMonth = YearMonth.now();
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("monthly-plan-view");
        
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
            .set("height", "100vh")
            .set("gap", "35px");

        // Logo at top - use AvatarHelper
        Div logo = AvatarHelper.createAvatarLogo(userSessionService);

        // Navigation icons container (shared pattern across views)
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle().set("gap", "30px");

        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", false);
        homeBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));

        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        trendsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("trends")));

        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));

        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        savingsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("savings")));

        Button planBtn = createNavButton(VaadinIcon.CALENDAR, "Monthly Plan", true); // Active

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
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "30px");

        // Left side: Title and subtitle
        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setPadding(false);
        titleSection.setSpacing(false);
        
        H2 title = new H2("Monthly Budget");
        title.getStyle()
            .set("margin", "0")
            .set("color", "white")
            .set("font-size", "28px")
            .set("font-weight", "700");
        
        Span subtitle = new Span("Plan and track your monthly spending");
        subtitle.getStyle()
            .set("color", "rgba(255, 255, 255, 0.6)")
            .set("font-size", "14px");
        
        titleSection.add(title, subtitle);

        // Right side: Month selector
        HorizontalLayout selectors = new HorizontalLayout();
        selectors.setSpacing(true);
        selectors.setAlignItems(FlexComponent.Alignment.CENTER);
        selectors.getStyle()
            .set("background", "#1e1e2e")
            .set("padding", "10px 15px")
            .set("border-radius", "10px");
        
        monthSelector = new ComboBox<>();
        monthSelector.setItems(Month.values());
        monthSelector.setValue(currentMonth.getMonth());
        monthSelector.setWidth("150px");
        monthSelector.setPlaceholder("Select Month");
        monthSelector.addValueChangeListener(e -> updateMonth());
        
        yearSelector = new ComboBox<>();
        yearSelector.setItems(
            Year.now().minusYears(1),
            Year.now(),
            Year.now().plusYears(1)
        );
        yearSelector.setValue(Year.of(currentMonth.getYear()));
        yearSelector.setWidth("100px");
        yearSelector.setPlaceholder("Year");
        yearSelector.addValueChangeListener(e -> updateMonth());
        
        selectors.add(monthSelector, yearSelector);

        // Add Budget Button
        Button addButton = new Button("Add Budget Item", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.getStyle()
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("border", "none")
            .set("box-shadow", "0 4px 15px rgba(102, 126, 234, 0.4)");
        addButton.addClickListener(e -> showAddBudgetItemDialog());

        header.add(titleSection, selectors, addButton);
        container.add(header);
    }

    private void updateMonth() {
        if (monthSelector.getValue() != null && yearSelector.getValue() != null) {
            currentMonth = YearMonth.of(yearSelector.getValue().getValue(), monthSelector.getValue());
            refreshData();
        }
    }

    private void createSummaryCards(VerticalLayout container) {
        summaryCards = new Div();
        summaryCards.addClassName("summary-cards");
        summaryCards.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
            .set("gap", "20px")
            .set("margin-bottom", "30px");
        
        container.add(summaryCards);
    }

    private void createContentArea(VerticalLayout container) {
        Div contentArea = new Div();
        contentArea.addClassName("content-area");
        contentArea.setHeightFull();
        contentArea.getStyle()
            .set("background", "rgb(30 30 46)")
            .set("border-radius", "15px")
            .set("width", "97%")
            .set("padding", "30px");
        
        // Section header
        HorizontalLayout sectionHeader = new HorizontalLayout();
        sectionHeader.setWidthFull();
        sectionHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        sectionHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        sectionHeader.getStyle().set("margin-bottom", "20px");
        
        Span sectionTitle = new Span("Budget Categories");
        sectionTitle.getStyle()
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("color", "white");
        
        Span itemCount = new Span();
        itemCount.getStyle()
            .set("color", "rgba(255, 255, 255, 0.6)")
            .set("font-size", "14px");
        
        sectionHeader.add(sectionTitle, itemCount);
        
        // Create grid
        budgetGrid = new Grid<>(BudgetItem.class, false);
        budgetGrid.addClassName("budget-grid");
        budgetGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        budgetGrid.setHeight("500px");
        budgetGrid.setSizeFull();
        
        // Add columns
        budgetGrid.addColumn(BudgetItem::getCategory)
            .setHeader("Category")
            .setSortable(true)
            .setFlexGrow(2);
        
        budgetGrid.addColumn(item -> {
            String type = item.getCategoryType();
            String badge = "";
            String color = "";
            switch (type) {
                case "INCOME": badge = "💰 Income"; color = "#4ade80"; break;
                case "EXPENSES": badge = "🛒 Expense"; color = "#f87171"; break;
                case "BILLS": badge = "📄 Bill"; color = "#fb923c"; break;
                case "SAVINGS": badge = "🐷 Saving"; color = "#60a5fa"; break;
                default: badge = type; color = "#9ca3af";
            }
            return badge;
        })
            .setHeader("Type")
            .setSortable(true)
            .setWidth("150px");
        
        budgetGrid.addColumn(item -> {
            double planned = item.getPlanned() != null ? item.getPlanned() : 0.0;
            double usd = dashboardDataService.convertToUSD(planned);
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Planned")
            .setSortable(true)
            .setWidth("130px");
        
        budgetGrid.addColumn(item -> {
            double actual = item.getActual() != null ? item.getActual() : 0.0;
            double usd = dashboardDataService.convertToUSD(actual);
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Actual")
            .setSortable(true)
            .setWidth("130px");
        
        budgetGrid.addComponentColumn(item -> {
            double percentage = item.getPlanned() > 0 ? 
                (item.getActual() / item.getPlanned()) * 100 : 0;
            
            Span progressSpan = new Span(String.format("%.0f%%", percentage));
            String color;
            if (percentage >= 100) color = "#f87171"; // Red - over budget
            else if (percentage >= 80) color = "#fb923c"; // Orange - getting close
            else color = "#4ade80"; // Green - on track
            
            progressSpan.getStyle()
                .set("color", color)
                .set("font-weight", "600");
            return progressSpan;
        })
            .setHeader("Used")
            .setSortable(true)
            .setWidth("100px");
        
        budgetGrid.addComponentColumn(this::createActionButtons)
            .setHeader("Actions")
            .setWidth("120px")
            .setFlexGrow(0);
        
        // Update item count when grid items change
        budgetGrid.addAttachListener(e -> {
            itemCount.setText(budgetGrid.getListDataView().getItemCount() + " items");
        });
        
        contentArea.add(sectionHeader, budgetGrid);
        container.add(contentArea);
    }


    private void refreshData() {
        // First, try to get saved budget items from database
        String userId = userSessionService.getCurrentUserId();
        List<BudgetItem> budgetItems = budgetItemRepository.findByUserIdAndYearAndMonth(
            userId, 
            currentMonth.getYear(), 
            currentMonth.getMonthValue()
        );
        
        // If no saved budget, try to get from transactions
        if (budgetItems.isEmpty()) {
            budgetItems = bankAccountService.generateBudgetItemsFromTransactions(currentMonth);
        }

        budgetGrid.setItems(budgetItems);
        updateSummaryCards(budgetItems);
    }

    private void updateSummaryCards(List<BudgetItem> items) {
        summaryCards.removeAll();
        
        // Group by category type
        Map<String, Double> plannedByType = items.stream()
            .collect(Collectors.groupingBy(
                BudgetItem::getCategoryType,
                Collectors.summingDouble(BudgetItem::getPlanned)
            ));
        
        Map<String, Double> actualByType = items.stream()
            .collect(Collectors.groupingBy(
                BudgetItem::getCategoryType,
                Collectors.summingDouble(BudgetItem::getActual)
            ));
        
        // Calculate totals
        double totalIncome = actualByType.getOrDefault("INCOME", 0.0);
        double totalExpenses = actualByType.getOrDefault("EXPENSES", 0.0);
        double totalBills = actualByType.getOrDefault("BILLS", 0.0);
        double totalSavings = actualByType.getOrDefault("SAVINGS", 0.0);
        double balance = totalIncome - totalExpenses - totalBills - totalSavings;
        
        // Create summary cards
        summaryCards.add(
            createSummaryCard("Total Income", totalIncome, "#4ade80", VaadinIcon.TRENDING_UP),
            createSummaryCard("Total Expenses", totalExpenses, "#f87171", VaadinIcon.TRENDING_DOWN),
            createSummaryCard("Total Bills", totalBills, "#fb923c", VaadinIcon.INVOICE),
            createSummaryCard("Total Savings", totalSavings, "#60a5fa", VaadinIcon.PIGGY_BANK),
            createSummaryCard("Balance", balance, balance >= 0 ? "#4ade80" : "#f87171", VaadinIcon.WALLET)
        );
    }

    private Div createSummaryCard(String title, double amount, String color, VaadinIcon icon) {
        Div card = new Div();
        card.addClassName("summary-card");
        card.getStyle()
            .set("background", "#1e1e2e")
            .set("border-radius", "12px")
            .set("padding", "24px")
            .set("border-left", "4px solid " + color)
            .set("transition", "transform 0.2s")
            .set("cursor", "pointer");
        
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("transform", "translateY(-2px)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("transform", "translateY(0)");
        });
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "12px");
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
            .set("font-size", "13px")
            .set("color", "rgba(255, 255, 255, 0.6)")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "0.5px");
        
        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("20px");
        cardIcon.getStyle().set("color", color);
        
        header.add(titleSpan, cardIcon);
        
        double usd = dashboardDataService.convertToUSD(amount);
        Span valueSpan = new Span(dashboardDataService.formatUSD(usd));
        valueSpan.getStyle()
            .set("font-size", "28px")
            .set("font-weight", "700")
            .set("color", "white")
            .set("display", "block");
        
        card.add(header, valueSpan);
        return card;
    }

    private HorizontalLayout createActionButtons(BudgetItem item) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.getElement().setProperty("title", "Edit budget");
        editBtn.addClickListener(e -> showEditBudgetItemDialog(item));
        
        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.getElement().setProperty("title", "Delete budget");
        deleteBtn.addClickListener(e -> deleteBudgetItem(item));
        
        actions.add(editBtn, deleteBtn);
        return actions;
    }

    private void showAddBudgetItemDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Budget Item");
        dialog.setWidth("450px");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        
        // Category field - use ComboBox bound to existing categories
        java.util.Set<String> categoryOptions = new java.util.TreeSet<>();
        try {
            String userId = userSessionService.getCurrentUserId();
            List<BudgetItem> existingItems = budgetItemRepository.findByUserIdAndYearAndMonth(
                userId,
                currentMonth.getYear(),
                currentMonth.getMonthValue()
            );
            existingItems.stream()
                .map(BudgetItem::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .forEach(categoryOptions::add);
        } catch (Exception ignored) {
            // Ignore and continue with whatever data we can collect
        }

        try {
            List<BudgetItem> actualItems = bankAccountService.generateBudgetItemsFromTransactions(currentMonth);
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
        
        // Type selector with descriptions
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
        
        // Planned amount with currency formatting
        NumberField plannedField = new NumberField("Planned Amount");
        plannedField.setPrefixComponent(new Span("$"));
        plannedField.setPlaceholder("0.00");
        plannedField.setWidthFull();
        plannedField.setRequired(true);
        plannedField.setMin(0);
        plannedField.setStep(0.01);
        
        // Format value with commas as user types
        plannedField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue() >= 1000) {
                String formatted = String.format("%,.2f", e.getValue());
                plannedField.setHelperText("= $" + formatted);
            } else {
                plannedField.setHelperText("");
            }
        });
        
        // Helper text
        Span helperText = new Span("💡 Tip: Set your planned amount to track spending against your budget");
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

    private void showEditBudgetItemDialog(BudgetItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Budget: " + item.getCategory());
        dialog.setWidth("450px");
        
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        
        // Show current vs planned
        Div currentInfo = new Div();
        currentInfo.getStyle()
            .set("color", "#f5f5f5")
            .set("padding", "15px")
            .set("border-radius", "8px");
        
        Span currentSpan = new Span(String.format(
            "Current: %s spent of %s planned",
            CurrencyFormatter.formatUSD(item.getActual()),
            CurrencyFormatter.formatUSD(item.getPlanned())
        ));
        currentSpan.getStyle().set("font-size", "14px");
        currentInfo.add(currentSpan);
        
        // Planned amount field with currency formatting
        NumberField plannedField = new NumberField("Update Planned Amount");
        plannedField.setPrefixComponent(new Span("$"));
        plannedField.setValue(item.getPlanned());
        plannedField.setWidthFull();
        plannedField.setMin(0);
        plannedField.setStep(0.01);
        
        // Show formatted value as helper text
        if (item.getPlanned() >= 1000) {
            plannedField.setHelperText("Currently: " + CurrencyFormatter.formatUSD(item.getPlanned()));
        }
        
        // Update helper text as user types
        plannedField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue() >= 1000) {
                String formatted = String.format("%,.2f", e.getValue());
                plannedField.setHelperText("= $" + formatted);
            } else {
                plannedField.setHelperText("");
            }
        });
        
        layout.add(currentInfo, plannedField);
        
        Button saveButton = new Button("Save Changes", e -> {
            if (plannedField.isEmpty()) {
                Notification.show("Please enter a planned amount", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            item.setPlanned(plannedField.getValue());
            budgetItemRepository.save(item);
            
            Notification.show("✅ Budget updated successfully!", 3000, Notification.Position.BOTTOM_START)
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

    private void deleteBudgetItem(BudgetItem item) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Budget Item");
        confirmDialog.setWidth("400px");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        
        Span message = new Span(String.format(
            "Are you sure you want to delete \"%s\" from your budget?",
            item.getCategory()
        ));
        message.getStyle().set("font-size", "14px");
        
        Span warningSpan = new Span("⚠️ This action cannot be undone.");
        warningSpan.getStyle()
            .set("color", "#fb923c")
            .set("font-size", "12px")
            .set("margin-top", "10px");
        
        layout.add(message, warningSpan);
        
        Button deleteButton = new Button("Delete", e -> {
            budgetItemRepository.delete(item);
            
            Notification.show("✅ Budget item deleted successfully!", 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            confirmDialog.close();
            refreshData();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        Button cancelButton = new Button("Cancel", e -> confirmDialog.close());
        
        confirmDialog.getFooter().add(cancelButton, deleteButton);
        confirmDialog.add(layout);
        confirmDialog.open();
    }

}
