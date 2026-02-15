package com.budgetplanner.budget.service;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

/**
 * Service to manage user session data including profile picture
 */
@Service
public class UserSessionService {
    
    private static final String AVATAR_KEY = "user_avatar_image";
    private static final String AVATAR_TYPE_KEY = "user_avatar_content_type";
    private static final String USER_INITIALS_KEY = "user_initials";
    private static final String USER_NAME_KEY = "user_full_name";
    
    /**
     * Store avatar image in session
     */
    public void setAvatarInSession(byte[] avatarImage, String contentType) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(AVATAR_KEY, avatarImage);
            session.setAttribute(AVATAR_TYPE_KEY, contentType);
        }
    }
    
    /**
     * Get avatar image from session
     */
    public byte[] getAvatarFromSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            return (byte[]) session.getAttribute(AVATAR_KEY);
        }
        return null;
    }
    
    /**
     * Get avatar content type from session
     */
    public String getAvatarContentType() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            return (String) session.getAttribute(AVATAR_TYPE_KEY);
        }
        return null;
    }
    
    /**
     * Check if user has avatar in session
     */
    public boolean hasAvatar() {
        byte[] avatar = getAvatarFromSession();
        return avatar != null && avatar.length > 0;
    }
    
    /**
     * Remove avatar from session
     */
    public void removeAvatarFromSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(AVATAR_KEY, null);
            session.setAttribute(AVATAR_TYPE_KEY, null);
        }
    }
    
    /**
     * Store user initials in session
     */
    public void setUserInitials(String initials) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_INITIALS_KEY, initials);
        }
    }
    
    /**
     * Get user initials from session
     */
    public String getUserInitials() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            String initials = (String) session.getAttribute(USER_INITIALS_KEY);
            return initials != null ? initials : "BP";
        }
        return "BP";
    }
    
    /**
     * Store user full name in session
     */
    public void setUserName(String name) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_NAME_KEY, name);
        }
    }
    
    /**
     * Get user full name from session
     */
    public String getUserName() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            String name = (String) session.getAttribute(USER_NAME_KEY);
            return name != null ? name : "Budget Planner User";
        }
        return "Budget Planner User";
    }
    
    /**
     * Get current user ID (for multi-user support in future)
     * Currently returns default user ID
     */
    public String getCurrentUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            String userId = (String) session.getAttribute("user_id");
            return userId != null ? userId : "default_user";
        }
        return "default_user";
    }
    
    /**
     * Clear all user session data
     */
    public void clearSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(AVATAR_KEY, null);
            session.setAttribute(AVATAR_TYPE_KEY, null);
            session.setAttribute(USER_INITIALS_KEY, null);
            session.setAttribute(USER_NAME_KEY, null);
        }
    }
}
