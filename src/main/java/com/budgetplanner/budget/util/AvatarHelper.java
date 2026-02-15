package com.budgetplanner.budget.util;

import com.budgetplanner.budget.service.UserSessionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;

/**
 * Helper utility to create avatar logo for sidebars
 */
public class AvatarHelper {
    
    /**
     * Creates a logo div with user avatar or initials
     * @param userSessionService The session service to get avatar data
     * @return Div containing avatar image or initials
     */
    public static Div createAvatarLogo(UserSessionService userSessionService) {
        Div logo = new Div();
        logo.getStyle()
            .set("width", "45px")
            .set("height", "45px")
            .set("border-radius", "50%")
            .set("overflow", "hidden")
            .set("margin", "20px auto 0");
        
        if (userSessionService.hasAvatar()) {
            // Show avatar image
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
            // Show user initials
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
        
        return logo;
    }
}
