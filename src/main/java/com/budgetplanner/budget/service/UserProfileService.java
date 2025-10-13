package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.NotificationPreference;
import com.budgetplanner.budget.model.UserProfile;
import com.budgetplanner.budget.repository.NotificationPreferenceRepository;
import com.budgetplanner.budget.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@Transactional
public class UserProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    
    // Default user ID for single-user application
    private static final String DEFAULT_USER_ID = "default_user";
    
    public UserProfileService(UserProfileRepository userProfileRepository,
                            NotificationPreferenceRepository notificationPreferenceRepository) {
        this.userProfileRepository = userProfileRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }
    
    /**
     * Get or create default user profile
     */
    public UserProfile getOrCreateDefaultProfile() {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(DEFAULT_USER_ID);
        
        if (profileOpt.isPresent()) {
            return profileOpt.get();
        }
        
        // Create default profile
        UserProfile profile = new UserProfile(
            DEFAULT_USER_ID,
            "Budget User",
            "user@example.com"
        );
        profile.setInitials("BU");
        
        return userProfileRepository.save(profile);
    }
    
    /**
     * Update user profile
     */
    public UserProfile updateProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }
    
    /**
     * Upload avatar image
     */
    public UserProfile uploadAvatar(InputStream inputStream, String contentType) throws IOException {
        UserProfile profile = getOrCreateDefaultProfile();
        
        // Read image data
        byte[] imageData = readInputStream(inputStream);
        
        // Validate image size (max 1MB)
        if (imageData.length > 1048576) {
            throw new IOException("Image size exceeds 1MB limit");
        }
        
        // Validate content type
        if (!isValidImageType(contentType)) {
            throw new IOException("Invalid image type. Supported: JPEG, PNG, GIF, WebP");
        }
        
        profile.setAvatarImage(imageData);
        profile.setAvatarContentType(contentType);
        
        return userProfileRepository.save(profile);
    }
    
    /**
     * Remove avatar image
     */
    public UserProfile removeAvatar() {
        UserProfile profile = getOrCreateDefaultProfile();
        profile.setAvatarImage(null);
        profile.setAvatarContentType(null);
        return userProfileRepository.save(profile);
    }
    
    /**
     * Get notification preferences for current user
     */
    public NotificationPreference getOrCreateNotificationPreferences() {
        Optional<NotificationPreference> prefOpt = 
            notificationPreferenceRepository.findByUserId(DEFAULT_USER_ID);
        
        if (prefOpt.isPresent()) {
            return prefOpt.get();
        }
        
        // Create default preferences
        NotificationPreference prefs = new NotificationPreference(DEFAULT_USER_ID);
        UserProfile profile = getOrCreateDefaultProfile();
        prefs.setEmailAddress(profile.getEmail());
        prefs.setPhoneNumber(profile.getPhoneNumber());
        
        return notificationPreferenceRepository.save(prefs);
    }
    
    /**
     * Update notification preferences
     */
    public NotificationPreference updateNotificationPreferences(NotificationPreference preferences) {
        // Also update phone number in user profile if changed
        UserProfile profile = getOrCreateDefaultProfile();
        if (preferences.getPhoneNumber() != null && 
            !preferences.getPhoneNumber().equals(profile.getPhoneNumber())) {
            profile.setPhoneNumber(preferences.getPhoneNumber());
            userProfileRepository.save(profile);
        }
        
        return notificationPreferenceRepository.save(preferences);
    }
    
    /**
     * Sync phone number between profile and preferences
     */
    public void syncPhoneNumber(String phoneNumber) {
        UserProfile profile = getOrCreateDefaultProfile();
        profile.setPhoneNumber(phoneNumber);
        userProfileRepository.save(profile);
        
        NotificationPreference prefs = getOrCreateNotificationPreferences();
        prefs.setPhoneNumber(phoneNumber);
        notificationPreferenceRepository.save(prefs);
    }
    
    /**
     * Sync email between profile and preferences
     */
    public void syncEmail(String email) {
        UserProfile profile = getOrCreateDefaultProfile();
        profile.setEmail(email);
        userProfileRepository.save(profile);
        
        NotificationPreference prefs = getOrCreateNotificationPreferences();
        prefs.setEmailAddress(email);
        notificationPreferenceRepository.save(prefs);
    }
    
    // Helper methods
    
    private byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        buffer.flush();
        return buffer.toByteArray();
    }
    
    private boolean isValidImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }
}
