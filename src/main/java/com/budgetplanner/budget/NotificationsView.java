package com.budgetplanner.budget;

import com.budgetplanner.budget.model.AppNotification;
import com.budgetplanner.budget.repository.AppNotificationRepository;
import com.budgetplanner.budget.service.UserSessionService;
import com.budgetplanner.budget.util.AvatarHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vaadin view for displaying AI notifications and messages
 */
@Route(value = "notifications")
@PageTitle("Notifications | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
public class NotificationsView extends VerticalLayout {

    private final AppNotificationRepository notificationRepository;
    private final UserSessionService userSessionService;
    
    private Div summaryCards;
    private Tabs filterTabs;
    private VerticalLayout contentArea;
    private String currentFilter = "ALL";

    @Autowired
    public NotificationsView(AppNotificationRepository notificationRepository, UserSessionService userSessionService) {
        this.notificationRepository = notificationRepository;
        this.userSessionService = userSessionService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("notifications-view");
        
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
        createContentArea(mainContent);
        
        splitLayout.addToSecondary(mainContent);
        add(splitLayout);
        
        // Create sample notifications if none exist
        initializeSampleNotifications();
        
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
        
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));
        
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        savingsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("savings")));
        
        Button planBtn = createNavButton(VaadinIcon.CALENDAR, "Monthly Plan", false);
        planBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("monthly-plan")));
        
        Button notificationsBtn = createNavButton(VaadinIcon.STAR, "Notifications", true); // Active!
        
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
        
        H2 title = new H2("Notifications & AI Insights");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        Button markAllReadButton = new Button("Mark All Read", new Icon(VaadinIcon.CHECK));
        markAllReadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        markAllReadButton.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px");
        markAllReadButton.addClickListener(e -> markAllAsRead());
        
        Button clearAllButton = new Button("Clear All", new Icon(VaadinIcon.TRASH));
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        clearAllButton.getStyle()
            .set("border-radius", "10px");
        clearAllButton.addClickListener(e -> clearAllNotifications());
        
        Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.getStyle()
            .set("border-radius", "10px");
        refreshButton.addClickListener(e -> refreshData());
        
        actions.add(markAllReadButton, clearAllButton, refreshButton);
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

    private void createFilterTabs(VerticalLayout container) {
        filterTabs = new Tabs();
        filterTabs.getStyle()
            .set("background", "rgb(27 23 42)")
            .set("border-radius", "15px")
            .set("padding", "10px 0 10px 0")
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        Tab allTab = new Tab("All");
        Tab unreadTab = new Tab("Unread");
        Tab aiInsightsTab = new Tab("AI Insights");
        Tab budgetAlertsTab = new Tab("Budget Alerts");
        Tab savingsTipsTab = new Tab("Savings Tips");
        Tab remindersTab = new Tab("Reminders");
        
        filterTabs.add(allTab, unreadTab, aiInsightsTab, budgetAlertsTab, savingsTipsTab, remindersTab);
        filterTabs.setSelectedTab(allTab);
        
        filterTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            updateFilter(selectedTab);
        });
        
        container.add(filterTabs);
    }

    private void createContentArea(VerticalLayout container) {
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(true);
        contentArea.getStyle().set("display", "grid");
        container.add(contentArea);
    }

    private void updateFilter(Tab selectedTab) {
        String label = selectedTab.getLabel();
        switch (label) {
            case "All":
                currentFilter = "ALL";
                break;
            case "Unread":
                currentFilter = "UNREAD";
                break;
            case "AI Insights":
                currentFilter = "AI_INSIGHT";
                break;
            case "Budget Alerts":
                currentFilter = "BUDGET_ALERT";
                break;
            case "Savings Tips":
                currentFilter = "SAVINGS_TIP";
                break;
            case "Reminders":
                currentFilter = "RECURRING_REMINDER";
                break;
        }
        loadNotifications();
    }

    private void loadNotifications() {
        contentArea.removeAll();
        
        List<AppNotification> notifications;
        
        switch (currentFilter) {
            case "UNREAD":
                notifications = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
                break;
            case "AI_INSIGHT":
            case "BUDGET_ALERT":
            case "SAVINGS_TIP":
            case "RECURRING_REMINDER":
                notifications = notificationRepository.findByCategoryOrderByCreatedAtDesc(currentFilter);
                break;
            default:
                notifications = notificationRepository.findAllByOrderByCreatedAtDesc();
                break;
        }
        
        if (notifications.isEmpty()) {
            Div emptyState = createEmptyState();
            contentArea.add(emptyState);
        } else {
            for (AppNotification notification : notifications) {
                Div notificationCard = createNotificationCard(notification);
                contentArea.add(notificationCard);
            }
        }
    }

    private Div createNotificationCard(AppNotification notification) {
        Div card = new Div();
        card.addClassName("notification-card");
        card.getStyle()
            .set("background", notification.getIsRead() ? "rgba(255, 255, 255, 0.03)" : "rgba(255, 255, 255, 0.08)")
            .set("border-radius", "15px")
            .set("padding", "20px")
            .set("margin-bottom", "10px")
            .set("border-left", "4px solid " + getCategoryColor(notification.getCategory()))
            .set("cursor", "pointer")
            .set("transition", "all 0.3s ease");
        
        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setWidthFull();
        cardHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
        cardHeader.setAlignItems(Alignment.CENTER);
        
        // Left side - icon and title
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(Alignment.CENTER);
        leftSide.setSpacing(true);
        
        Icon categoryIcon = getCategoryIcon(notification.getCategory());
        categoryIcon.setSize("24px");
        categoryIcon.getStyle().set("color", getCategoryColor(notification.getCategory()));
        
        H3 titleSpan = new H3(notification.getTitle());
        titleSpan.getStyle()
            .set("margin", "0")
            .set("color", "white")
            .set("font-size", "16px")
            .set("font-weight", notification.getIsRead() ? "500" : "600");
        
        leftSide.add(categoryIcon, titleSpan);
        
        // Right side - timestamp and actions
        HorizontalLayout rightSide = new HorizontalLayout();
        rightSide.setAlignItems(Alignment.CENTER);
        rightSide.setSpacing(true);
        
        Span timeSpan = new Span(formatTime(notification.getCreatedAt()));
        timeSpan.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "12px");
        
        if (!notification.getIsRead()) {
            Div unreadBadge = new Div();
            unreadBadge.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("background", "#00d4ff")
                .set("border-radius", "50%");
            rightSide.add(unreadBadge);
        }
        
        Button markReadBtn = new Button(new Icon(notification.getIsRead() ? VaadinIcon.EYE_SLASH : VaadinIcon.EYE));
        markReadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        markReadBtn.getElement().setProperty("title", notification.getIsRead() ? "Mark as unread" : "Mark as read");
        markReadBtn.addClickListener(e -> {
            toggleReadStatus(notification);
            e.getSource().getUI().ifPresent(ui -> refreshData());
        });
        
        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> {
            deleteNotification(notification);
            e.getSource().getUI().ifPresent(ui -> refreshData());
        });
        
        rightSide.add(timeSpan, markReadBtn, deleteBtn);
        
        cardHeader.add(leftSide, rightSide);
        
        // Message content
        Span messageSpan = new Span(notification.getMessage());
        messageSpan.getStyle()
            .set("color", "#d1d5db")
            .set("font-size", "14px")
            .set("display", "block")
            .set("margin-top", "10px")
            .set("line-height", "1.6");
        
        // Priority badge
        if ("HIGH".equals(notification.getPriority())) {
            Span priorityBadge = new Span("High Priority");
            priorityBadge.getStyle()
                .set("background", "#ef4444")
                .set("color", "white")
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-size", "11px")
                .set("font-weight", "500")
                .set("display", "inline-block")
                .set("margin-top", "10px");
            card.add(cardHeader, messageSpan, priorityBadge);
        } else {
            card.add(cardHeader, messageSpan);
        }
        
        return card;
    }

    private Div createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
            .set("background", "rgba(255, 255, 255, 0.05)")
            .set("border-radius", "15px")
            .set("padding", "60px 20px")
            .set("text-align", "center");
        
        Icon icon = new Icon(VaadinIcon.BELL_O);
        icon.setSize("64px");
        icon.getStyle().set("color", "#9ca3af");
        
        H3 emptyTitle = new H3("No notifications");
        emptyTitle.getStyle()
            .set("color", "#9ca3af")
            .set("margin", "20px 0 10px 0");
        
        Span emptyText = new Span("You're all caught up! Check back later for new insights and alerts.");
        emptyText.getStyle()
            .set("color", "#6b7280")
            .set("font-size", "14px");
        
        emptyState.add(icon, emptyTitle, emptyText);
        return emptyState;
    }

    private Icon getCategoryIcon(String category) {
        switch (category) {
            case "AI_INSIGHT":
                return new Icon(VaadinIcon.LIGHTBULB);
            case "BUDGET_ALERT":
                return new Icon(VaadinIcon.WARNING);
            case "SAVINGS_TIP":
                return new Icon(VaadinIcon.PIGGY_BANK);
            case "RECURRING_REMINDER":
                return new Icon(VaadinIcon.CLOCK);
            default:
                return new Icon(VaadinIcon.INFO_CIRCLE);
        }
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "AI_INSIGHT":
                return "#00d4ff";
            case "BUDGET_ALERT":
                return "#f87171";
            case "SAVINGS_TIP":
                return "#4ade80";
            case "RECURRING_REMINDER":
                return "#fbbf24";
            default:
                return "#60a5fa";
        }
    }

    private String formatTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutesAgo < 1) {
            return "Just now";
        } else if (minutesAgo < 60) {
            return minutesAgo + " min ago";
        } else if (minutesAgo < 1440) {
            long hoursAgo = minutesAgo / 60;
            return hoursAgo + (hoursAgo == 1 ? " hour ago" : " hours ago");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return dateTime.format(formatter);
        }
    }

    private void toggleReadStatus(AppNotification notification) {
        notification.setIsRead(!notification.getIsRead());
        notificationRepository.save(notification);
    }

    private void deleteNotification(AppNotification notification) {
        notificationRepository.delete(notification);
        Notification.show("Notification deleted", 2000, Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void markAllAsRead() {
        List<AppNotification> unreadNotifications = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        for (AppNotification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
        Notification.show("All notifications marked as read", 2000, Notification.Position.TOP_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshData();
    }

    private void clearAllNotifications() {
        notificationRepository.deleteAll();
        Notification.show("All notifications cleared", 2000, Notification.Position.TOP_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshData();
    }

    private void refreshData() {
        updateSummaryCards();
        loadNotifications();
    }

    private void updateSummaryCards() {
        summaryCards.removeAll();
        
        long totalNotifications = notificationRepository.count();
        long unreadNotifications = notificationRepository.countByIsReadFalse();
        long aiInsights = notificationRepository.findByCategoryOrderByCreatedAtDesc("AI_INSIGHT").size();
        long budgetAlerts = notificationRepository.findByCategoryOrderByCreatedAtDesc("BUDGET_ALERT").size();
        
        summaryCards.add(
            createSummaryCard("Total Notifications", String.valueOf(totalNotifications), "#60a5fa", VaadinIcon.BELL),
            createSummaryCard("Unread", String.valueOf(unreadNotifications), "#00d4ff", VaadinIcon.ENVELOPE_O),
            createSummaryCard("AI Insights", String.valueOf(aiInsights), "#4ade80", VaadinIcon.LIGHTBULB),
            createSummaryCard("Budget Alerts", String.valueOf(budgetAlerts), "#f87171", VaadinIcon.WARNING)
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

    private void initializeSampleNotifications() {
        // Only create sample data if none exists
        if (notificationRepository.count() == 0) {
            notificationRepository.save(new AppNotification(
                "Budget Overspending Alert",
                "You've spent 92% of your planned budget for Dining Out this month. Consider reducing expenses to stay on track.",
                "BUDGET_ALERT",
                "HIGH"
            ));
            
            notificationRepository.save(new AppNotification(
                "AI Insight: Savings Opportunity",
                "Based on your spending patterns, you could save $250/month by reducing subscription services and dining out expenses.",
                "AI_INSIGHT",
                "MEDIUM"
            ));
            
            notificationRepository.save(new AppNotification(
                "Savings Goal Progress",
                "Great job! You're 65% of the way to your Emergency Fund goal. Keep up the momentum!",
                "SAVINGS_TIP",
                "LOW"
            ));
            
            notificationRepository.save(new AppNotification(
                "Recurring Payment Due",
                "Your Netflix subscription ($15.99) is due in 3 days.",
                "RECURRING_REMINDER",
                "MEDIUM"
            ));
            
            notificationRepository.save(new AppNotification(
                "AI Insight: Spending Pattern",
                "Your grocery spending is 15% lower than last month. This is helping you stay within budget!",
                "AI_INSIGHT",
                "LOW"
            ));
        }
    }
}
