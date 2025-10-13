package com.budgetplanner.budget.view;

import com.budgetplanner.budget.service.AIAdvisoryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.YearMonth;
import java.util.List;

public class NotificationCenterDialog extends Dialog {
    
    private final AIAdvisoryService advisoryService;
    private VerticalLayout notificationsContainer;
    private YearMonth currentPeriod;
    
    public NotificationCenterDialog(AIAdvisoryService advisoryService, YearMonth currentPeriod) {
        this.advisoryService = advisoryService;
        this.currentPeriod = currentPeriod;
        
        initializeDialog();
        loadNotifications();
    }
    
    private void initializeDialog() {
        setWidth("600px");
        setHeight("500px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        
        // Header
        HorizontalLayout header = createHeader();
        
        // Notifications container
        notificationsContainer = new VerticalLayout();
        notificationsContainer.setSpacing(true);
        notificationsContainer.setPadding(false);
        notificationsContainer.setWidthFull();
        
        // Scrollable container
        Div scrollContainer = new Div(notificationsContainer);
        scrollContainer.getStyle().set("overflow-y", "auto")
                                  .set("flex-grow", "1")
                                  .set("padding", "var(--lumo-space-s)")
                                  .set("background-color", "var(--lumo-contrast-5pct)")
                                  .set("border-radius", "var(--lumo-border-radius-m)");
        
        // Main layout
        VerticalLayout mainLayout = new VerticalLayout(header, scrollContainer);
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setSizeFull();
        mainLayout.setFlexGrow(0, header);
        mainLayout.setFlexGrow(1, scrollContainer);
        
        add(mainLayout);
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setPadding(false);
        header.setSpacing(true);
        
        // Title with bell icon
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        Span bellIcon = new Span("🔔");
        bellIcon.getStyle().set("font-size", "1.2em");
        
        H3 title = new H3("AI Financial Advisor");
        title.getStyle().set("margin", "0")
                        .set("color", "var(--lumo-primary-text-color)");
        
        titleLayout.add(bellIcon, title);
        
        // Close button
        Button closeButton = new Button(VaadinIcon.CLOSE.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.getElement().setProperty("title", "Close");
        closeButton.addClickListener(e -> close());
        
        header.add(titleLayout, closeButton);
        return header;
    }
    
    public void updatePeriod(YearMonth period) {
        this.currentPeriod = period;
        loadNotifications();
    }
    
    private void loadNotifications() {
        notificationsContainer.removeAll();
        
        try {
            List<AIAdvisoryService.AdvisoryTip> tips = advisoryService.generatePersonalizedTips(currentPeriod);
            
            if (tips.isEmpty()) {
                showNoNotificationsMessage();
            } else {
                displayNotifications(tips);
            }
        } catch (Exception e) {
            showErrorMessage();
        }
    }
    
    private void displayNotifications(List<AIAdvisoryService.AdvisoryTip> tips) {
        for (int i = 0; i < tips.size(); i++) {
            AIAdvisoryService.AdvisoryTip tip = tips.get(i);
            Div notificationCard = createNotificationCard(tip, i == 0); // First one is highest priority
            notificationsContainer.add(notificationCard);
        }
    }
    
    private Div createNotificationCard(AIAdvisoryService.AdvisoryTip tip, boolean isHighestPriority) {
        Div card = new Div();
        card.addClassName("notification-card");
        card.getStyle().set("background-color", "var(--lumo-base-color)")
                       .set("border-radius", "var(--lumo-border-radius-s)")
                       .set("padding", "var(--lumo-space-m)")
                       .set("margin-bottom", "var(--lumo-space-s)")
                       .set("border-left", "4px solid " + getTipTypeColor(tip.getType()))
                       .set("box-shadow", isHighestPriority ? 
                           "0 2px 8px rgba(0,0,0,0.2)" : "0 1px 3px rgba(0,0,0,0.1)");
        
        // Priority badge for highest priority
        if (isHighestPriority) {
            card.getStyle().set("border", "2px solid var(--lumo-primary-color)")
                           .set("background-color", "var(--lumo-primary-color-10pct)");
        }
        
        // Header with icon, title, and priority
        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setWidthFull();
        cardHeader.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        cardHeader.setAlignItems(HorizontalLayout.Alignment.CENTER);
        cardHeader.setSpacing(true);
        
        // Title section with type icon
        HorizontalLayout titleSection = new HorizontalLayout();
        titleSection.setSpacing(true);
        titleSection.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        Span typeIcon = new Span(getTipTypeIcon(tip.getType()));
        typeIcon.getStyle().set("font-size", "1.1em");
        
        Span title = new Span(tip.getTitle());
        title.getStyle().set("font-weight", "bold")
                        .set("color", "var(--lumo-primary-text-color)");
        
        titleSection.add(typeIcon, title);
        
        // Priority and status indicators
        HorizontalLayout statusSection = new HorizontalLayout();
        statusSection.setSpacing(true);
        statusSection.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        if (isHighestPriority) {
            Span highPriorityBadge = new Span("🔥 TOP PRIORITY");
            highPriorityBadge.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                                        .set("color", "var(--lumo-primary-text-color)")
                                        .set("background-color", "var(--lumo-primary-color)")
                                        .set("padding", "2px 8px")
                                        .set("border-radius", "var(--lumo-border-radius-s)")
                                        .set("font-weight", "bold");
            statusSection.add(highPriorityBadge);
        }
        
        Span priority = new Span(getPriorityText(tip.getPriority()));
        priority.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                           .set("color", "var(--lumo-secondary-text-color)")
                           .set("background-color", "var(--lumo-contrast-10pct)")
                           .set("padding", "2px 6px")
                           .set("border-radius", "var(--lumo-border-radius-s)");
        
        statusSection.add(priority);
        cardHeader.add(titleSection, statusSection);
        
        // Message content
        Span message = new Span(tip.getMessage());
        message.getStyle().set("color", "var(--lumo-body-text-color)")
                          .set("line-height", "1.5")
                          .set("display", "block")
                          .set("margin-top", "var(--lumo-space-s)");
        
        // Category badge
        Span categoryBadge = new Span(tip.getCategory().toString().replace("_", " "));
        categoryBadge.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                                .set("color", "var(--lumo-tertiary-text-color)")
                                .set("background-color", "var(--lumo-contrast-5pct)")
                                .set("padding", "2px 6px")
                                .set("border-radius", "var(--lumo-border-radius-s)")
                                .set("margin-top", "var(--lumo-space-s)")
                                .set("display", "inline-block");
        
        card.add(cardHeader, message, categoryBadge);
        return card;
    }
    
    private String getTipTypeIcon(AIAdvisoryService.AdvisoryTip.TipType type) {
        return switch (type) {
            case SUGGESTION -> "💡";
            case WARNING -> "⚠️";
            case OPPORTUNITY -> "🎯";
            case INSIGHT -> "📊";
        };
    }
    
    private String getTipTypeColor(AIAdvisoryService.AdvisoryTip.TipType type) {
        return switch (type) {
            case SUGGESTION -> "#89b4fa";    // Blue
            case WARNING -> "#f38ba8";       // Pink
            case OPPORTUNITY -> "#a6e3a1";   // Green
            case INSIGHT -> "#fab387";       // Orange
        };
    }
    
    private String getPriorityText(int priority) {
        if (priority >= 80) return "High Priority";
        if (priority >= 60) return "Medium Priority";
        if (priority >= 40) return "Low Priority";
        return "Info";
    }
    
    private void showNoNotificationsMessage() {
        Div noNotificationsDiv = new Div();
        noNotificationsDiv.getStyle().set("text-align", "center")
                                     .set("padding", "var(--lumo-space-xl)")
                                     .set("color", "var(--lumo-secondary-text-color)");
        
        Span icon = new Span("✅");
        icon.getStyle().set("font-size", "3em")
                       .set("display", "block")
                       .set("margin-bottom", "var(--lumo-space-m)");
        
        Span title = new Span("All Good!");
        title.getStyle().set("font-weight", "bold")
                        .set("font-size", "var(--lumo-font-size-l)")
                        .set("display", "block")
                        .set("margin-bottom", "var(--lumo-space-s)");
        
        Span message = new Span("No financial recommendations at this time. Your spending looks healthy!");
        message.getStyle().set("font-size", "var(--lumo-font-size-m)")
                          .set("line-height", "1.5");
        
        noNotificationsDiv.add(icon, title, message);
        notificationsContainer.add(noNotificationsDiv);
    }
    
    private void showErrorMessage() {
        Div errorDiv = new Div();
        errorDiv.getStyle().set("text-align", "center")
                          .set("padding", "var(--lumo-space-xl)")
                          .set("color", "var(--lumo-error-text-color)");
        
        Span icon = new Span("❌");
        icon.getStyle().set("font-size", "2em")
                       .set("display", "block")
                       .set("margin-bottom", "var(--lumo-space-s)");
        
        Span message = new Span("Unable to load financial recommendations. Please try again later.");
        message.getStyle().set("font-size", "var(--lumo-font-size-m)");
        
        errorDiv.add(icon, message);
        notificationsContainer.add(errorDiv);
    }
}
