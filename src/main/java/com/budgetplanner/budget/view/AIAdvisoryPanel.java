package com.budgetplanner.budget.view;

import com.budgetplanner.budget.service.AIAdvisoryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.YearMonth;
import java.util.List;

public class AIAdvisoryPanel extends VerticalLayout {
    
    private final AIAdvisoryService advisoryService;
    private VerticalLayout tipsContainer;
    private Button refreshButton;
    private YearMonth currentPeriod;
    
    public AIAdvisoryPanel(AIAdvisoryService advisoryService) {
        this.advisoryService = advisoryService;
        this.currentPeriod = YearMonth.now();
        
        initializePanel();
        loadAdvisoryTips();
    }
    
    private void initializePanel() {
        addClassName("ai-advisory-panel");
        setSpacing(false);
        setPadding(true);
        setWidth("100%");
        setHeight("400px");
        
        // Apply dark theme styling
        getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                  .set("border-radius", "var(--lumo-border-radius-m)")
                  .set("border", "1px solid var(--lumo-contrast-10pct)");
        
        // Header section
        HorizontalLayout header = createHeader();
        
        // Tips container
        tipsContainer = new VerticalLayout();
        tipsContainer.setSpacing(true);
        tipsContainer.setPadding(false);
        tipsContainer.setWidthFull();
        tipsContainer.addClassName("tips-container");
        
        // Scrollable container for tips
        Div scrollContainer = new Div(tipsContainer);
        scrollContainer.getStyle().set("overflow-y", "auto")
                                  .set("flex-grow", "1")
                                  .set("padding", "var(--lumo-space-xs)");
        
        add(header, scrollContainer);
        setFlexGrow(0, header);
        setFlexGrow(1, scrollContainer);
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.setPadding(false);
        header.setSpacing(true);
        
        // Title with AI icon
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(Alignment.CENTER);
        
        Span aiIcon = new Span("🤖");
        aiIcon.getStyle().set("font-size", "1.2em");
        
        H3 title = new H3("AI Money Advisor");
        title.getStyle().set("margin", "0")
                        .set("color", "var(--lumo-primary-text-color)");
        
        titleLayout.add(aiIcon, title);
        
        // Refresh button
        refreshButton = new Button(VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshButton.getElement().setProperty("title", "Refresh AI insights");
        refreshButton.addClickListener(e -> loadAdvisoryTips());
        
        header.add(titleLayout, refreshButton);
        return header;
    }
    
    public void updatePeriod(YearMonth period) {
        this.currentPeriod = period;
        loadAdvisoryTips();
    }
    
    private void loadAdvisoryTips() {
        // Clear existing tips
        tipsContainer.removeAll();
        
        // Show loading state
        Div loadingDiv = new Div();
        loadingDiv.add(new Span("🔄 Analyzing your spending patterns..."));
        loadingDiv.getStyle().set("text-align", "center")
                             .set("padding", "var(--lumo-space-m)")
                             .set("color", "var(--lumo-secondary-text-color)");
        tipsContainer.add(loadingDiv);
        
        // Simulate async loading (in real implementation, this could be async)
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                List<AIAdvisoryService.AdvisoryTip> tips = advisoryService.generatePersonalizedTips(currentPeriod);
                
                // Remove loading indicator
                tipsContainer.removeAll();
                
                if (tips.isEmpty()) {
                    showNoTipsMessage();
                } else {
                    displayTips(tips);
                }
            } catch (Exception e) {
                tipsContainer.removeAll();
                showErrorMessage();
            }
        }));
    }
    
    private void displayTips(List<AIAdvisoryService.AdvisoryTip> tips) {
        for (AIAdvisoryService.AdvisoryTip tip : tips) {
            Div tipCard = createTipCard(tip);
            tipsContainer.add(tipCard);
        }
    }
    
    private Div createTipCard(AIAdvisoryService.AdvisoryTip tip) {
        Div card = new Div();
        card.addClassName("tip-card");
        card.getStyle().set("background-color", "var(--lumo-base-color)")
                       .set("border-radius", "var(--lumo-border-radius-s)")
                       .set("padding", "var(--lumo-space-m)")
                       .set("margin-bottom", "var(--lumo-space-s)")
                       .set("border-left", "4px solid " + getTipTypeColor(tip.getType()))
                       .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)");
        
        // Header with icon and title
        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setWidthFull();
        cardHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
        cardHeader.setAlignItems(Alignment.CENTER);
        cardHeader.setSpacing(true);
        
        // Title with type icon
        HorizontalLayout titleSection = new HorizontalLayout();
        titleSection.setSpacing(true);
        titleSection.setAlignItems(Alignment.CENTER);
        
        Span typeIcon = new Span(getTipTypeIcon(tip.getType()));
        typeIcon.getStyle().set("font-size", "1.1em");
        
        Span title = new Span(tip.getTitle());
        title.getStyle().set("font-weight", "bold")
                        .set("color", "var(--lumo-primary-text-color)");
        
        titleSection.add(typeIcon, title);
        
        // Priority indicator
        Span priority = new Span(getPriorityText(tip.getPriority()));
        priority.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                           .set("color", "var(--lumo-secondary-text-color)")
                           .set("background-color", "var(--lumo-contrast-10pct)")
                           .set("padding", "2px 6px")
                           .set("border-radius", "var(--lumo-border-radius-s)");
        
        cardHeader.add(titleSection, priority);
        
        // Message content
        Span message = new Span(tip.getMessage());
        message.getStyle().set("color", "var(--lumo-body-text-color)")
                          .set("line-height", "1.4")
                          .set("display", "block")
                          .set("margin-top", "var(--lumo-space-xs)");
        
        card.add(cardHeader, message);
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
    
    private void showNoTipsMessage() {
        Div noTipsDiv = new Div();
        noTipsDiv.getStyle().set("text-align", "center")
                           .set("padding", "var(--lumo-space-l)")
                           .set("color", "var(--lumo-secondary-text-color)");
        
        Span icon = new Span("✅");
        icon.getStyle().set("font-size", "2em")
                       .set("display", "block")
                       .set("margin-bottom", "var(--lumo-space-s)");
        
        Span title = new Span("Great job!");
        title.getStyle().set("font-weight", "bold")
                        .set("display", "block")
                        .set("margin-bottom", "var(--lumo-space-xs)");
        
        Span message = new Span("Your spending looks healthy. Keep up the good financial habits!");
        message.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        noTipsDiv.add(icon, title, message);
        tipsContainer.add(noTipsDiv);
    }
    
    private void showErrorMessage() {
        Div errorDiv = new Div();
        errorDiv.getStyle().set("text-align", "center")
                          .set("padding", "var(--lumo-space-l)")
                          .set("color", "var(--lumo-error-text-color)");
        
        Span icon = new Span("❌");
        icon.getStyle().set("font-size", "2em")
                       .set("display", "block")
                       .set("margin-bottom", "var(--lumo-space-s)");
        
        Span message = new Span("Unable to analyze spending patterns. Please try again later.");
        message.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        errorDiv.add(icon, message);
        tipsContainer.add(errorDiv);
    }
}
