package com.budgetplanner.budget;

import com.budgetplanner.budget.model.AuditLog;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.AvatarHelper;
import com.budgetplanner.budget.service.AuditLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vaadin view for displaying audit history and change logs
 */
@Route(value = "history")
@PageTitle("History | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class HistoryView extends VerticalLayout {

    private final AuditLogService auditLogService;
    private final UserSessionService userSessionService;
    private VerticalLayout logsContainer;
    private Tabs filterTabs;
    private TextField searchField;

    @Autowired
    public HistoryView(AuditLogService auditLogService, UserSessionService userSessionService) {
        this.auditLogService = auditLogService;
        this.userSessionService = userSessionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("history-view");

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
        createFilterTabs(mainContent);
        createLogsArea(mainContent);

        splitLayout.addToSecondary(mainContent);
        add(splitLayout);

        // Load initial data
        loadLogs("ALL");
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
        
        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", true); // Active!
        
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
        
        H2 title = new H2("Change History");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        // Search field
        searchField = new TextField();
        searchField.setPlaceholder("Search history...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setWidth("300px");
        searchField.getStyle()
            .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
            .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)");
        
        searchField.addValueChangeListener(e -> {
            String keyword = e.getValue();
            if (keyword == null || keyword.trim().isEmpty()) {
                loadLogs("ALL");
            } else {
                searchLogs(keyword);
            }
        });
        
        header.add(title, searchField);
        container.add(header);
    }

    private void createSummaryCards(VerticalLayout container) {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.getStyle()
            .set("margin-bottom", "30px")
            .set("gap", "20px");

        // Total Logs Card
        Long totalLogs = (long) auditLogService.getAllLogs().size();
        cardsLayout.add(createSummaryCard("Total Logs", String.valueOf(totalLogs), 
            "#00d4ff", VaadinIcon.DATABASE));

        // Financial Actions Card
        Long financialLogs = auditLogService.getCountByCategory("FINANCIAL");
        cardsLayout.add(createSummaryCard("Financial", String.valueOf(financialLogs), 
            "#4ade80", VaadinIcon.DOLLAR));

        // User Actions Card
        Long userLogs = auditLogService.getCountByCategory("USER_ACTION");
        cardsLayout.add(createSummaryCard("User Actions", String.valueOf(userLogs), 
            "#fbbf24", VaadinIcon.USER));

        // Security Logs Card
        Long securityLogs = auditLogService.getCountByCategory("SECURITY");
        cardsLayout.add(createSummaryCard("Security", String.valueOf(securityLogs), 
            "#f87171", VaadinIcon.LOCK));

        container.add(cardsLayout);
    }

    private Div createSummaryCard(String label, String value, String color, VaadinIcon icon) {
        Div card = new Div();
        card.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "20px")
            .set("flex", "1")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "10px")
            .set("border-left", "4px solid " + color);

        HorizontalLayout iconRow = new HorizontalLayout();
        iconRow.setWidthFull();
        iconRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        iconRow.setAlignItems(Alignment.CENTER);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "14px");

        Icon iconElement = new Icon(icon);
        iconElement.setColor(color);
        iconElement.setSize("24px");

        iconRow.add(labelSpan, iconElement);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
            .set("color", "white")
            .set("font-size", "24px")
            .set("font-weight", "600");

        card.add(iconRow, valueSpan);
        return card;
    }

    private void createFilterTabs(VerticalLayout container) {
        filterTabs = new Tabs();
        filterTabs.setWidthFull();
        filterTabs.getStyle()
            .set("margin-bottom", "20px")
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "10px 0px 10px 0px");

        Tab allTab = new Tab("All");
        Tab transactionsTab = new Tab("Transactions");
        Tab budgetTab = new Tab("Budget");
        Tab userTab = new Tab("User Actions");
        Tab securityTab = new Tab("Security");
        Tab systemTab = new Tab("System");

        filterTabs.add(allTab, transactionsTab, budgetTab, userTab, securityTab, systemTab);

        filterTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            String category = selectedTab.getLabel();
            loadLogs(category);
        });

        container.add(filterTabs);
    }

    private void createLogsArea(VerticalLayout container) {
        logsContainer = new VerticalLayout();
        logsContainer.setPadding(false);
        logsContainer.setSpacing(true);
        logsContainer.getStyle().set("gap", "15px");
        logsContainer.getStyle().set("display", "grid");

        container.add(logsContainer);

    }

    private void loadLogs(String category) {
        logsContainer.removeAll();

        List<AuditLog> logs;
        switch (category) {
            case "Transactions":
                logs = auditLogService.getLogsByEntityType("TRANSACTION");
                break;
            case "Budget":
                logs = auditLogService.getLogsByEntityType("BUDGET_ITEM");
                break;
            case "User Actions":
                logs = auditLogService.getLogsByCategory("USER_ACTION");
                break;
            case "Security":
                logs = auditLogService.getLogsByCategory("SECURITY");
                break;
            case "System":
                logs = auditLogService.getLogsByCategory("SYSTEM");
                break;
            default:
                logs = auditLogService.getAllLogs();
        }

        if (logs.isEmpty()) {
            Div emptyState = createEmptyState();
            logsContainer.add(emptyState);
        } else {
            for (AuditLog log : logs) {
                Div logCard = createLogCard(log);
                logsContainer.add(logCard);
            }
        }
    }

    private void searchLogs(String keyword) {
        logsContainer.removeAll();
        List<AuditLog> logs = auditLogService.searchLogs(keyword);

        if (logs.isEmpty()) {
            Div emptyState = createEmptyState();
            logsContainer.add(emptyState);
        } else {
            for (AuditLog log : logs) {
                Div logCard = createLogCard(log);
                logsContainer.add(logCard);
            }
        }
    }

    private Div createLogCard(AuditLog log) {
        Div card = new Div();
        card.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "20px")
            .set("border-left", "4px solid " + getCategoryColor(log.getCategory()));

        // Header row with icon, action, and timestamp
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        HorizontalLayout leftSection = new HorizontalLayout();
        leftSection.setSpacing(true);
        leftSection.setAlignItems(Alignment.CENTER);
        leftSection.getStyle().set("gap", "15px");

        Icon icon = new Icon(getActionIcon(log.getAction()));
        icon.setColor(getCategoryColor(log.getCategory()));
        icon.setSize("20px");

        Span actionBadge = new Span(log.getAction());
        actionBadge.getStyle()
            .set("background", getCategoryColor(log.getCategory()) + "20")
            .set("color", getCategoryColor(log.getCategory()))
            .set("padding", "5px 12px")
            .set("border-radius", "8px")
            .set("font-size", "12px")
            .set("font-weight", "600");

        leftSection.add(icon, actionBadge);

        Span timestamp = new Span(getRelativeTime(log.getTimestamp()));
        timestamp.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "13px");

        headerRow.add(leftSection, timestamp);

        // Description
        Div description = new Div();
        description.setText(log.getDescription());
        description.getStyle()
            .set("color", "white")
            .set("font-size", "15px")
            .set("margin", "15px 0");

        // Details section (if old/new values exist)
        VerticalLayout detailsSection = new VerticalLayout();
        detailsSection.setPadding(false);
        detailsSection.setSpacing(true);
        detailsSection.getStyle()
            .set("margin-top", "10px")
            .set("gap", "8px");

        if (log.getOldValue() != null && !log.getOldValue().isEmpty()) {
            Div oldValue = new Div();
            oldValue.getStyle()
                .set("padding", "10px")
                .set("background", "rgba(248, 113, 113, 0.1)")
                .set("border-radius", "8px")
                .set("border-left", "3px solid #f87171");
            
            Span oldLabel = new Span("Before: ");
            oldLabel.getStyle().set("color", "#f87171").set("font-weight", "600");
            Span oldText = new Span(log.getOldValue());
            oldText.getStyle().set("color", "#9ca3af").set("font-size", "13px");
            oldValue.add(oldLabel, oldText);
            
            detailsSection.add(oldValue);
        }

        if (log.getNewValue() != null && !log.getNewValue().isEmpty()) {
            Div newValue = new Div();
            newValue.getStyle()
                .set("padding", "10px")
                .set("background", "rgba(74, 222, 128, 0.1)")
                .set("border-radius", "8px")
                .set("border-left", "3px solid #4ade80");
            
            Span newLabel = new Span("After: ");
            newLabel.getStyle().set("color", "#4ade80").set("font-weight", "600");
            Span newText = new Span(log.getNewValue());
            newText.getStyle().set("color", "#9ca3af").set("font-size", "13px");
            newValue.add(newLabel, newText);
            
            detailsSection.add(newValue);
        }

        // Footer with metadata
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footer.setAlignItems(Alignment.CENTER);
        footer.getStyle().set("margin-top", "15px");

        HorizontalLayout tags = new HorizontalLayout();
        tags.setSpacing(true);
        tags.getStyle().set("gap", "10px");

        Span entityTag = new Span("📦 " + log.getEntityType());
        entityTag.getStyle()
            .set("background", "rgba(255, 255, 255, 0.1)")
            .set("color", "#9ca3af")
            .set("padding", "4px 10px")
            .set("border-radius", "6px")
            .set("font-size", "11px");

        Span userTag = new Span("👤 " + (log.getUserName() != null ? log.getUserName() : "Unknown"));
        userTag.getStyle()
            .set("background", "rgba(255, 255, 255, 0.1)")
            .set("color", "#9ca3af")
            .set("padding", "4px 10px")
            .set("border-radius", "6px")
            .set("font-size", "11px");

        tags.add(entityTag, userTag);

        if (log.getSeverity() != null && !log.getSeverity().equals("INFO")) {
            Span severityBadge = new Span(log.getSeverity());
            severityBadge.getStyle()
                .set("background", getSeverityColor(log.getSeverity()) + "30")
                .set("color", getSeverityColor(log.getSeverity()))
                .set("padding", "4px 10px")
                .set("border-radius", "6px")
                .set("font-size", "11px")
                .set("font-weight", "600");
            tags.add(severityBadge);
        }

        footer.add(tags);

        card.add(headerRow, description);
        if (detailsSection.getComponentCount() > 0) {
            card.add(detailsSection);
        }
        card.add(footer);

        return card;
    }

    private Div createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "60px")
            .set("text-align", "center");

        Icon icon = new Icon(VaadinIcon.INFO_CIRCLE);
        icon.setSize("48px");
        icon.setColor("#9ca3af");

        Span text = new Span("No history records found");
        text.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "16px")
            .set("display", "block")
            .set("margin-top", "15px");

        emptyState.add(icon, text);
        return emptyState;
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "FINANCIAL":
                return "#4ade80";
            case "USER_ACTION":
                return "#fbbf24";
            case "SECURITY":
                return "#f87171";
            case "SYSTEM":
                return "#00d4ff";
            default:
                return "#9ca3af";
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity) {
            case "ERROR":
            case "CRITICAL":
                return "#f87171";
            case "WARNING":
                return "#fbbf24";
            default:
                return "#00d4ff";
        }
    }

    private VaadinIcon getActionIcon(String action) {
        switch (action) {
            case "CREATE":
                return VaadinIcon.PLUS_CIRCLE;
            case "UPDATE":
                return VaadinIcon.EDIT;
            case "DELETE":
                return VaadinIcon.TRASH;
            case "SYNC":
                return VaadinIcon.REFRESH;
            case "LOGIN":
                return VaadinIcon.SIGN_IN;
            case "PASSWORD_CHANGE":
                return VaadinIcon.LOCK;
            default:
                return VaadinIcon.INFO_CIRCLE;
        }
    }

    private String getRelativeTime(LocalDateTime timestamp) {
        Duration duration = Duration.between(timestamp, LocalDateTime.now());
        
        if (duration.toMinutes() < 1) {
            return "Just now";
        } else if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " min ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + " hours ago";
        } else if (duration.toDays() < 7) {
            return duration.toDays() + " days ago";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return timestamp.format(formatter);
        }
    }
}
