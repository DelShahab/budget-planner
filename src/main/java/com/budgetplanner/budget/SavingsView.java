package com.budgetplanner.budget;

import com.budgetplanner.budget.model.BudgetItem;
import com.budgetplanner.budget.model.SavingsGoal;
import com.budgetplanner.budget.repository.BudgetItemRepository;
import com.budgetplanner.budget.service.DashboardDataService;
import com.budgetplanner.budget.service.SavingsGoalService;
import com.budgetplanner.budget.service.UserSessionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import java.util.List;

/**
 * Vaadin view for managing savings goals
 */
@Route(value = "savings")
@PageTitle("Savings Goals | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class SavingsView extends VerticalLayout {

    private final BudgetItemRepository budgetItemRepository;
    private final SavingsGoalService savingsGoalService;
    private final DashboardDataService dashboardDataService;
    private final UserSessionService userSessionService;
    
    private Grid<SavingsGoal> savingsGrid;
    private Div summaryCards;
    private VerticalLayout contentArea;

    @Autowired
    public SavingsView(BudgetItemRepository budgetItemRepository, SavingsGoalService savingsGoalService, 
                       DashboardDataService dashboardDataService, UserSessionService userSessionService) {
        this.budgetItemRepository = budgetItemRepository;
        this.savingsGoalService = savingsGoalService;
        this.dashboardDataService = dashboardDataService;
        this.userSessionService = userSessionService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("savings-view");
        
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
        
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        trendsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("trends")));
        
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));
        
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", true); // Active!
        
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
        
        H2 title = new H2("Savings Goals");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button addGoalButton = new Button("Add Savings Goal", new Icon(VaadinIcon.PLUS));
        addGoalButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addGoalButton.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px");
        addGoalButton.addClickListener(e -> openAddGoalDialog());
        
        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.getStyle()
            .set("border-radius", "10px");
        refreshButton.addClickListener(e -> refreshData());
        
        actions.add(addGoalButton, refreshButton);
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

    private void createContentArea(VerticalLayout container) {
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(true);
        
        // Create savings goals grid
        createSavingsGrid();
        
        contentArea.add(savingsGrid);
        
        container.add(contentArea);
    }

    private void createSavingsGrid() {
        savingsGrid = new Grid<>(SavingsGoal.class, false);
        savingsGrid.addClassName("savings-grid");
        savingsGrid.setSizeFull();
        savingsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        
        // Apply modern dark grid styling
        savingsGrid.getStyle()
            .set("background", "#171521")
            .set("border-radius", "15px")
            .set("padding", "20px");
        
        // Add columns
        savingsGrid.addColumn(SavingsGoal::getGoalName)
            .setHeader("Savings Goal")
            .setSortable(true)
            .setFlexGrow(2);
        
        savingsGrid.addColumn(item -> {
            double usd = dashboardDataService.convertToUSD(item.getTargetAmount());
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Target Amount")
            .setSortable(true)
            .setWidth("150px");
        
        savingsGrid.addColumn(item -> {
            double usd = dashboardDataService.convertToUSD(item.getCurrentAmount());
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Current Amount")
            .setSortable(true)
            .setWidth("150px");
        
        savingsGrid.addColumn(item -> String.format("%d%%", item.getProgressPercentage()))
            .setHeader("Progress")
            .setSortable(true)
            .setWidth("120px");
        
        savingsGrid.addColumn(item -> {
            double usd = dashboardDataService.convertToUSD(item.getRemainingAmount());
            return dashboardDataService.formatUSD(usd);
        })
            .setHeader("Remaining")
            .setSortable(true)
            .setWidth("150px");
        
        // Add action column
        savingsGrid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openEditGoalDialog(item));
            
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> deleteGoal(item));
            
            actions.add(editBtn, deleteBtn);
            return actions;
        }).setHeader("Actions").setWidth("150px");
    }

    private void openAddGoalDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Savings Goal");
        dialog.setModal(true);
        dialog.setWidth("500px");
        
        // Style dialog
        dialog.getElement().getThemeList().add("dark");
        
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        TextField goalNameField = new TextField("Goal Name");
        goalNameField.setPlaceholder("e.g., Emergency Fund, Vacation, New Car");
        goalNameField.setWidthFull();
        
        NumberField targetAmountField = new NumberField("Target Amount");
        targetAmountField.setPlaceholder("0.00");
        targetAmountField.setPrefixComponent(new Span("$"));
        targetAmountField.setWidthFull();
        
        NumberField currentAmountField = new NumberField("Current Amount");
        currentAmountField.setPlaceholder("0.00");
        currentAmountField.setPrefixComponent(new Span("$"));
        currentAmountField.setValue(0.0);
        currentAmountField.setWidthFull();
        
        form.add(goalNameField, targetAmountField, currentAmountField);
        
        Button saveButton = new Button("Save", e -> {
            if (goalNameField.isEmpty() || targetAmountField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            SavingsGoal savingsGoal = new SavingsGoal();
            savingsGoal.setGoalName(goalNameField.getValue());
            savingsGoal.setTargetAmount(targetAmountField.getValue());
            savingsGoal.setCurrentAmount(currentAmountField.getValue() != null ? currentAmountField.getValue() : 0.0);
            savingsGoal.setCategory("Savings");
            savingsGoal.setIconName("PIGGY_BANK_COIN");
            savingsGoal.setStartDate(LocalDate.now());
            savingsGoal.setIsActive(true);
            
            savingsGoalService.createGoal(savingsGoal);
            
            Notification.show("Savings goal added successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            dialog.close();
            refreshData();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(form);
        dialog.open();
    }

    private void openEditGoalDialog(SavingsGoal item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Savings Goal");
        dialog.setModal(true);
        dialog.setWidth("500px");
        
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        TextField goalNameField = new TextField("Goal Name");
        goalNameField.setValue(item.getGoalName());
        goalNameField.setWidthFull();
        
        NumberField targetAmountField = new NumberField("Target Amount");
        targetAmountField.setValue(item.getTargetAmount());
        targetAmountField.setPrefixComponent(new Span("$"));
        targetAmountField.setWidthFull();
        
        NumberField currentAmountField = new NumberField("Current Amount");
        currentAmountField.setValue(item.getCurrentAmount());
        currentAmountField.setPrefixComponent(new Span("$"));
        currentAmountField.setWidthFull();
        
        form.add(goalNameField, targetAmountField, currentAmountField);
        
        Button saveButton = new Button("Save", e -> {
            item.setGoalName(goalNameField.getValue());
            item.setTargetAmount(targetAmountField.getValue());
            item.setCurrentAmount(currentAmountField.getValue());
            
            savingsGoalService.updateGoal(item);
            
            Notification.show("Savings goal updated successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            dialog.close();
            refreshData();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(form);
        dialog.open();
    }

    private void deleteGoal(SavingsGoal item) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirm Delete");
        confirmDialog.setModal(true);
        
        Div content = new Div();
        content.setText("Are you sure you want to delete this savings goal: " + item.getGoalName() + "?");
        content.getStyle().set("padding", "20px");
        
        Button deleteButton = new Button("Delete", e -> {
            savingsGoalService.deleteGoal(item.getId());
            
            Notification.show("Savings goal deleted successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            confirmDialog.close();
            refreshData();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        Button cancelButton = new Button("Cancel", e -> confirmDialog.close());
        
        confirmDialog.add(content);
        confirmDialog.getFooter().add(cancelButton, deleteButton);
        confirmDialog.open();
    }

    private void refreshData() {
        updateSummaryCards();
        loadSavingsGoals();
    }

    private void loadSavingsGoals() {
        List<SavingsGoal> savingsGoals = savingsGoalService.getAllActiveGoals();
        savingsGrid.setItems(savingsGoals);
    }

    private void updateSummaryCards() {
        summaryCards.removeAll();
        
        List<SavingsGoal> savingsGoals = savingsGoalService.getAllActiveGoals();
        
        double totalTarget = savingsGoalService.getTotalTargetSavings();
        double totalSaved = savingsGoalService.getTotalCurrentSavings();
        double totalRemaining = totalTarget - totalSaved;
        double overallProgress = savingsGoalService.getOverallProgressPercentage();
        
        double totalTargetUSD = dashboardDataService.convertToUSD(totalTarget);
        double totalSavedUSD = dashboardDataService.convertToUSD(totalSaved);
        
        summaryCards.add(
            createSummaryCard("Total Goals", String.valueOf(savingsGoals.size()), "#60a5fa", VaadinIcon.PIGGY_BANK),
            createSummaryCard("Total Target", dashboardDataService.formatUSD(totalTargetUSD), "#4ade80", VaadinIcon.DOLLAR),
            createSummaryCard("Total Saved", dashboardDataService.formatUSD(totalSavedUSD), "#00d4ff", VaadinIcon.CHECK_CIRCLE),
            createSummaryCard("Overall Progress", String.format("%.1f%%", overallProgress), "#fbbf24", VaadinIcon.CHART)
        );
    }

    private Div createSummaryCard(String title, String value, String color, VaadinIcon icon) {
        Div card = new Div();
        card.addClassName("summary-card");
        card.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "20px")
            .set("border-left", "4px solid " + color);
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        
        H3 cardTitle = new H3(title);
        cardTitle.getStyle()
            .set("margin", "0")
            .set("color", "#9ca3af")
            .set("font-size", "14px")
            .set("font-weight", "500")
            .set("text-transform", "uppercase");
        
        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("24px");
        cardIcon.getStyle().set("color", color);
        
        header.add(cardTitle, cardIcon);
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
            .set("color", "white")
            .set("font-size", "28px")
            .set("font-weight", "600")
            .set("display", "block")
            .set("margin-top", "10px");
        
        card.add(header, valueSpan);
        return card;
    }
}
