package com.budgetplanner.budget;

import com.budgetplanner.budget.model.NotificationPreference;
import com.budgetplanner.budget.model.UserProfile;
import com.budgetplanner.budget.service.UserProfileService;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.server.StreamResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

/**
 * Vaadin view for user profile and settings management
 */
@Route(value = "user-settings")
@PageTitle("User Settings | Budget Planner")
@CssImport("./styles/modern-dashboard.css")
@CssImport("./styles/budget-dashboard.css")
@CssImport("./styles/notifications.css")
@CssImport("./styles/mobile-responsive.css")
@CssImport("./styles/accordion-styles.css")
public class UserSettingsView extends VerticalLayout {

    private final UserProfileService userProfileService;
    
    private UserProfile currentProfile;
    private NotificationPreference currentPreferences;
    
    // Profile fields
    private Image avatarImage;
    private Div avatarPlaceholder;
    private TextField fullNameField;
    private EmailField emailField;
    private TextField phoneField;
    private TextArea bioField;
    private ComboBox<String> timezoneField;
    private ComboBox<String> currencyField;
    
    // Notification preference fields
    private Checkbox emailEnabledCheckbox;
    private Checkbox smsEnabledCheckbox;
    private Checkbox aiInsightsCheckbox;
    private Checkbox budgetAlertsCheckbox;
    private Checkbox savingsTipsCheckbox;
    private Checkbox recurringRemindersCheckbox;

    @Autowired
    public UserSettingsView(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("user-settings-view");
        
        // Apply modern dashboard dark theme
        getStyle()
            .set("background", "#0f0a1e")
            .set("color", "white");
        
        // Load current profile and preferences
        loadUserData();
        
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
        createProfileSection(mainContent);
        createSecuritySection(mainContent);
        createNotificationSection(mainContent);
        
        splitLayout.addToSecondary(mainContent);
        add(splitLayout);
    }

    private void loadUserData() {
        currentProfile = userProfileService.getOrCreateDefaultProfile();
        currentPreferences = userProfileService.getOrCreateNotificationPreferences();
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
        Div logo = createAvatarLogo();

        // Navigation icons container
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setPadding(false);
        navContainer.setSpacing(false);
        navContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        navContainer.getStyle().set("gap", "30px");

        Button homeBtn = createNavButton(VaadinIcon.HOME, "Home", false);
        homeBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("modern-dashboard")));
        
        Button trendsBtn = createNavButton(VaadinIcon.TRENDING_UP, "Trends", false);
        trendsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("trends")));
        
        Button recurringBtn = createNavButton(VaadinIcon.REFRESH, "Recurring", false);
        recurringBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("recurring-transactions")));
        
        Button savingsBtn = createNavButton(VaadinIcon.PIGGY_BANK, "Savings", false);
        savingsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("savings")));
        
        Button notificationsBtn = createNavButton(VaadinIcon.STAR, "Notifications", false);
        notificationsBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("notifications")));
        
        Button userBtn = createNavButton(VaadinIcon.USER, "Profile", true); // Active!
        
        Button historyBtn = createNavButton(VaadinIcon.CLOCK, "History", false);
        historyBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("history")));
        
        Button settingsBtn = createNavButton(VaadinIcon.COG, "Settings", false);

        navContainer.add(homeBtn, trendsBtn, recurringBtn, savingsBtn, notificationsBtn, userBtn, historyBtn, settingsBtn);
        
        sidebar.add(logo, navContainer);
        return sidebar;
    }

    private Div createAvatarLogo() {
        Div logo = new Div();
        logo.getStyle()
            .set("width", "45px")
            .set("height", "45px")
            .set("border-radius", "50%")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("overflow", "hidden")
            .set("margin", "20px auto 0");
        
        if (currentProfile.hasAvatar()) {
            Image img = new Image();
            img.setSrc(createAvatarResource());
            img.setWidth("45px");
            img.setHeight("45px");
            img.getStyle().set("object-fit", "cover");
            logo.add(img);
        } else {
            logo.getStyle()
                .set("background", "#01a1be")
                .set("color", "white")
                .set("font-size", "18px")
                .set("font-weight", "bold");
            Span initials = new Span(currentProfile.getInitials());
            logo.add(initials);
        }
        
        return logo;
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
        
        H2 title = new H2("User Profile & Settings");
        title.addClassName("view-title");
        title.getStyle()
            .set("color", "white")
            .set("margin", "0")
            .set("font-size", "28px")
            .set("font-weight", "600");
        
        header.add(title);
        container.add(header);
    }

    private void createProfileSection(VerticalLayout container) {
        Accordion accordion = new Accordion();
        accordion.getStyle()
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        // Avatar upload section
        Div avatarSection = createAvatarUploadSection();
        
        // Profile form
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.getStyle().set("gap", "15px");
        
        fullNameField = new TextField("Full Name");
        fullNameField.setValue(currentProfile.getFullName());
        fullNameField.setWidthFull();
        styleTextField(fullNameField);
        
        emailField = new EmailField("Email Address");
        emailField.setValue(currentProfile.getEmail());
        emailField.setWidthFull();
        styleTextField(emailField);
        
        phoneField = new TextField("Phone Number");
        phoneField.setValue(currentProfile.getPhoneNumber() != null ? currentProfile.getPhoneNumber() : "");
        phoneField.setPlaceholder("+1 (555) 123-4567");
        phoneField.setWidthFull();
        styleTextField(phoneField);
        
        bioField = new TextArea("Bio");
        bioField.setValue(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        bioField.setPlaceholder("Tell us about yourself...");
        bioField.setWidthFull();
        bioField.setHeight("100px");
        styleTextField(bioField);
        
        HorizontalLayout additionalFields = new HorizontalLayout();
        additionalFields.setWidthFull();
        additionalFields.setSpacing(true);
        
        // Timezone ComboBox with common timezones
        timezoneField = new ComboBox<>("Timezone");
        timezoneField.setItems(
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
            "America/Phoenix", "America/Anchorage", "Pacific/Honolulu",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Madrid",
            "Asia/Tokyo", "Asia/Shanghai", "Asia/Dubai", "Asia/Kolkata",
            "Australia/Sydney", "UTC"
        );
        timezoneField.setValue(currentProfile.getTimezone() != null ? currentProfile.getTimezone() : "America/New_York");
        timezoneField.setPlaceholder("Select timezone");
        timezoneField.setWidth("50%");
        styleTextField(timezoneField);
        
        // Currency ComboBox with common currencies
        currencyField = new ComboBox<>("Currency");
        currencyField.setItems(
            "USD - US Dollar", "EUR - Euro", "GBP - British Pound", "JPY - Japanese Yen",
            "AUD - Australian Dollar", "CAD - Canadian Dollar", "CHF - Swiss Franc",
            "CNY - Chinese Yuan", "INR - Indian Rupee", "MXN - Mexican Peso",
            "BRL - Brazilian Real", "ZAR - South African Rand", "AED - UAE Dirham"
        );
        String currentCurrency = currentProfile.getCurrency();
        // Try to find and set the matching currency item
        String matchingCurrency = currencyField.getListDataView().getItems()
            .filter(item -> item.startsWith(currentCurrency))
            .findFirst()
            .orElse("USD - US Dollar");
        currencyField.setValue(matchingCurrency);
        currencyField.setPlaceholder("Select currency");
        currencyField.setWidth("50%");
        styleTextField(currencyField);
        
        additionalFields.add(timezoneField, currencyField);
        
        Button saveProfileBtn = new Button("Save Profile", new Icon(VaadinIcon.CHECK));
        saveProfileBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfileBtn.getStyle()
            .set("background", "linear-gradient(135deg, #00d4ff 0%, #009bb8 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px")
            .set("margin-top", "10px");
        saveProfileBtn.addClickListener(e -> saveProfile());
        
        formLayout.add(fullNameField, emailField, phoneField, bioField, additionalFields, saveProfileBtn);
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle().set("padding", "20px");
        content.add(avatarSection, formLayout);
        
        AccordionPanel panel = accordion.add("Profile Information", content);
        panel.addThemeVariants(DetailsVariant.FILLED);
        styleAccordionPanel(panel);
        panel.setOpened(true);
        
        container.add(accordion);
    }

    private Div createAvatarUploadSection() {
        Div avatarSection = new Div();
        avatarSection.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "20px")
            .set("margin-bottom", "30px")
            .set("padding", "20px")
            .set("background", "rgba(0, 212, 255, 0.1)")
            .set("border-radius", "10px")
            .set("border", "2px dashed rgba(0, 212, 255, 0.3)");
        
        // Avatar display
        Div avatarContainer = new Div();
        avatarContainer.getStyle()
            .set("width", "80px")
            .set("height", "80px")
            .set("border-radius", "50%")
            .set("overflow", "hidden")
            .set("flex-shrink", "0");
        
        if (currentProfile.hasAvatar()) {
            avatarImage = new Image();
            avatarImage.setSrc(createAvatarResource());
            avatarImage.setWidth("80px");
            avatarImage.setHeight("80px");
            avatarImage.getStyle().set("object-fit", "cover");
            avatarContainer.add(avatarImage);
        } else {
            avatarPlaceholder = new Div();
            avatarPlaceholder.getStyle()
                .set("width", "80px")
                .set("height", "80px")
                .set("background", "#01a1be")
                .set("border-radius", "50%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "white")
                .set("font-size", "28px")
                .set("font-weight", "bold");
            avatarPlaceholder.add(new Span(currentProfile.getInitials()));
            avatarContainer.add(avatarPlaceholder);
        }
        
        // Upload controls
        VerticalLayout uploadControls = new VerticalLayout();
        uploadControls.setPadding(false);
        uploadControls.setSpacing(true);
        uploadControls.getStyle().set("gap", "10px");
        
        Span uploadLabel = new Span("Profile Picture");
        uploadLabel.getStyle()
            .set("color", "white")
            .set("font-weight", "600")
            .set("font-size", "16px");
        
        Span uploadHint = new Span("Upload a profile picture. Max size: 1MB. Formats: JPEG, PNG, GIF, WebP");
        uploadHint.getStyle()
            .set("color", "#9ca3af")
            .set("font-size", "12px");
        
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(1048576); // 1MB
        
        upload.addSucceededListener(event -> {
            try {
                userProfileService.uploadAvatar(buffer.getInputStream(), event.getMIMEType());
                loadUserData();
                getUI().ifPresent(ui -> ui.getPage().reload());
                Notification.show("Avatar uploaded successfully!", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error uploading avatar: " + ex.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button removeAvatarBtn = new Button("Remove Avatar", new Icon(VaadinIcon.TRASH));
        removeAvatarBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        removeAvatarBtn.setVisible(currentProfile.hasAvatar());
        removeAvatarBtn.addClickListener(e -> {
            userProfileService.removeAvatar();
            loadUserData();
            getUI().ifPresent(ui -> ui.getPage().reload());
            Notification.show("Avatar removed successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        
        uploadControls.add(uploadLabel, uploadHint, upload, removeAvatarBtn);
        
        avatarSection.add(avatarContainer, uploadControls);
        return avatarSection;
    }

    private void createNotificationSection(VerticalLayout container) {
        Accordion accordion = new Accordion();
        accordion.getStyle()
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.getStyle().set("gap", "20px");
        
        // Channel preferences
        Div channelSection = new Div();
        channelSection.getStyle()
            .set("padding", "15px")
            .set("background", "rgba(0, 212, 255, 0.1)")
            .set("border-radius", "10px")
            .set("margin-bottom", "10px");
        
        H4 channelTitle = new H4("Notification Channels");
        channelTitle.getStyle()
            .set("color", "#00d4ff")
            .set("margin", "0 0 15px 0")
            .set("font-size", "16px");
        
        emailEnabledCheckbox = new Checkbox("Enable Email Notifications");
        emailEnabledCheckbox.setValue(currentPreferences.getEmailEnabled());
        styleCheckbox(emailEnabledCheckbox);
        
        smsEnabledCheckbox = new Checkbox("Enable SMS Notifications");
        smsEnabledCheckbox.setValue(currentPreferences.getSmsEnabled());
        styleCheckbox(smsEnabledCheckbox);
        
        channelSection.add(channelTitle, emailEnabledCheckbox, smsEnabledCheckbox);
        
        // Category preferences
        Div categorySection = new Div();
        categorySection.getStyle()
            .set("padding", "15px")
            .set("background", "rgba(74, 222, 128, 0.1)")
            .set("border-radius", "10px");
        
        H4 categoryTitle = new H4("Notification Categories");
        categoryTitle.getStyle()
            .set("color", "#4ade80")
            .set("margin", "0 0 15px 0")
            .set("font-size", "16px");
        
        aiInsightsCheckbox = new Checkbox("💡 AI Insights & Recommendations");
        aiInsightsCheckbox.setValue(currentPreferences.getAiInsightsEnabled());
        styleCheckbox(aiInsightsCheckbox);
        
        budgetAlertsCheckbox = new Checkbox("⚠️ Budget Alerts & Warnings");
        budgetAlertsCheckbox.setValue(currentPreferences.getBudgetAlertsEnabled());
        styleCheckbox(budgetAlertsCheckbox);
        
        savingsTipsCheckbox = new Checkbox("💰 Savings Tips & Progress Updates");
        savingsTipsCheckbox.setValue(currentPreferences.getSavingsTipsEnabled());
        styleCheckbox(savingsTipsCheckbox);
        
        recurringRemindersCheckbox = new Checkbox("🔔 Recurring Payment Reminders");
        recurringRemindersCheckbox.setValue(currentPreferences.getRecurringRemindersEnabled());
        styleCheckbox(recurringRemindersCheckbox);
        
        categorySection.add(categoryTitle, aiInsightsCheckbox, budgetAlertsCheckbox, 
                          savingsTipsCheckbox, recurringRemindersCheckbox);
        
        Button savePrefsBtn = new Button("Save Notification Preferences", new Icon(VaadinIcon.CHECK));
        savePrefsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePrefsBtn.getStyle()
            .set("background", "linear-gradient(135deg, #4ade80 0%, #22c55e 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px")
            .set("margin-top", "10px");
        savePrefsBtn.addClickListener(e -> saveNotificationPreferences());
        
        formLayout.add(channelSection, categorySection, savePrefsBtn);
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle().set("padding", "20px");
        content.add(formLayout);
        
        AccordionPanel panel = accordion.add("Notification Preferences", content);
        panel.addThemeVariants(DetailsVariant.FILLED);
        styleAccordionPanel(panel);
        panel.setOpened(true);
        
        container.add(accordion);
    }

    private void saveProfile() {
        try {
            currentProfile.setFullName(fullNameField.getValue());
            currentProfile.setEmail(emailField.getValue());
            currentProfile.setPhoneNumber(phoneField.getValue());
            currentProfile.setBio(bioField.getValue());
            currentProfile.setTimezone(timezoneField.getValue());
            
            // Extract currency code from "USD - US Dollar" format
            String currencyValue = currencyField.getValue();
            if (currencyValue != null && currencyValue.contains(" - ")) {
                currencyValue = currencyValue.split(" - ")[0];
            }
            currentProfile.setCurrency(currencyValue != null ? currencyValue : "USD");
            
            userProfileService.updateProfile(currentProfile);
            
            // Sync email and phone to notification preferences
            userProfileService.syncEmail(emailField.getValue());
            if (!phoneField.getValue().isEmpty()) {
                userProfileService.syncPhoneNumber(phoneField.getValue());
            }
            
            Notification.show("Profile updated successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception e) {
            Notification.show("Error saving profile: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void saveNotificationPreferences() {
        try {
            currentPreferences.setEmailEnabled(emailEnabledCheckbox.getValue());
            currentPreferences.setSmsEnabled(smsEnabledCheckbox.getValue());
            currentPreferences.setAiInsightsEnabled(aiInsightsCheckbox.getValue());
            currentPreferences.setBudgetAlertsEnabled(budgetAlertsCheckbox.getValue());
            currentPreferences.setSavingsTipsEnabled(savingsTipsCheckbox.getValue());
            currentPreferences.setRecurringRemindersEnabled(recurringRemindersCheckbox.getValue());
            
            userProfileService.updateNotificationPreferences(currentPreferences);
            
            Notification.show("Notification preferences saved successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        } catch (Exception e) {
            Notification.show("Error saving preferences: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private StreamResource createAvatarResource() {
        return new StreamResource("avatar.png", () -> 
            new ByteArrayInputStream(currentProfile.getAvatarImage()));
    }

    private void styleTextField(Object field) {
        // Field labels and placeholders are now styled via accordion-styles.css
        // Just set the Lumo theme variables for consistency
        if (field instanceof TextField) {
            TextField tf = (TextField) field;
            tf.getStyle()
                .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
                .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
                .set("--lumo-contrast-90pct", "white");
        } else if (field instanceof EmailField) {
            EmailField ef = (EmailField) field;
            ef.getStyle()
                .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
                .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
                .set("--lumo-contrast-90pct", "white");
        } else if (field instanceof TextArea) {
            TextArea ta = (TextArea) field;
            ta.getStyle()
                .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
                .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
                .set("--lumo-contrast-90pct", "white");
        } else if (field instanceof PasswordField) {
            PasswordField pf = (PasswordField) field;
            pf.getStyle()
                .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
                .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
                .set("--lumo-contrast-90pct", "white");
        } else if (field instanceof ComboBox) {
            ComboBox<?> cb = (ComboBox<?>) field;
            cb.getStyle()
                .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
                .set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.2)")
                .set("--lumo-contrast-90pct", "white");
        }
    }

    private void createSecuritySection(VerticalLayout container) {
        Accordion accordion = new Accordion();
        accordion.getStyle()
            .set("width", "100%")
            .set("margin-bottom", "20px");
        
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.getStyle().set("gap", "15px");
        
        // Password reset fields
        PasswordField currentPasswordField = new PasswordField("Current Password");
        currentPasswordField.setWidthFull();
        currentPasswordField.setPlaceholder("Enter current password");
        styleTextField(currentPasswordField);
        
        PasswordField newPasswordField = new PasswordField("New Password");
        newPasswordField.setWidthFull();
        newPasswordField.setPlaceholder("Enter new password");
        styleTextField(newPasswordField);
        
        PasswordField confirmPasswordField = new PasswordField("Confirm New Password");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setPlaceholder("Re-enter new password");
        styleTextField(confirmPasswordField);
        
        // Password strength indicator
        Div passwordHint = new Div();
        passwordHint.getStyle()
            .set("padding", "10px 15px")
            .set("background", "rgba(251, 191, 36, 0.1)")
            .set("border-left", "3px solid #fbbf24")
            .set("border-radius", "5px")
            .set("margin", "5px 0");
        
        Span hintText = new Span("💡 Password must be at least 8 characters with uppercase, lowercase, and numbers.");
        hintText.getStyle()
            .set("color", "#fbbf24")
            .set("font-size", "12px");
        passwordHint.add(hintText);
        
        Button changePasswordBtn = new Button("Change Password", new Icon(VaadinIcon.LOCK));
        changePasswordBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordBtn.getStyle()
            .set("background", "linear-gradient(135deg, #f87171 0%, #dc2626 100%)")
            .set("border", "none")
            .set("color", "white")
            .set("font-weight", "500")
            .set("border-radius", "10px")
            .set("margin-top", "10px");
        
        changePasswordBtn.addClickListener(e -> {
            String current = currentPasswordField.getValue();
            String newPass = newPasswordField.getValue();
            String confirm = confirmPasswordField.getValue();
            
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Notification.show("All password fields are required", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            if (!newPass.equals(confirm)) {
                Notification.show("New passwords do not match", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            if (newPass.length() < 8) {
                Notification.show("Password must be at least 8 characters", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // TODO: Implement actual password change logic with Spring Security
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            
            Notification.show("Password changed successfully!", 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        
        formLayout.add(currentPasswordField, newPasswordField, confirmPasswordField, passwordHint, changePasswordBtn);
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle().set("padding", "20px");
        content.add(formLayout);
        
        AccordionPanel panel = accordion.add("Security Settings", content);
        panel.addThemeVariants(DetailsVariant.FILLED);
        styleAccordionPanel(panel);
        panel.setOpened(true);
        
        container.add(accordion);
    }

    private void styleCheckbox(Checkbox checkbox) {
        checkbox.getStyle()
            .set("color", "white")
            .set("--lumo-primary-color", "#00d4ff");
    }

    private void styleAccordionPanel(AccordionPanel panel) {
        // All accordion styling is now done via accordion-styles.css
        // No JavaScript needed - CSS handles everything including chevron rotation!
    }
}
