package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.NotificationPreference;
import com.budgetplanner.budget.model.UserProfile;
import com.budgetplanner.budget.repository.NotificationPreferenceRepository;
import com.budgetplanner.budget.repository.UserProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Service to initialize default user profile and sync with notification preferences
 */
@Service
public class UserProfileInitializerService {
    
    private static final String DEFAULT_USER_ID = "default_user";
    
    @Bean
    public CommandLineRunner initializeUserProfile(
            UserProfileRepository userProfileRepository,
            NotificationPreferenceRepository preferenceRepository) {
        
        return args -> {
            // Create default user profile if none exists
            if (!userProfileRepository.existsByUserId(DEFAULT_USER_ID)) {
                UserProfile defaultProfile = new UserProfile(
                    DEFAULT_USER_ID,
                    "Budget Planner User",
                    "user@budgetplanner.com"
                );
                defaultProfile.setPhoneNumber("");
                defaultProfile.setBio("Welcome to Budget Planner! Manage your finances with ease.");
                defaultProfile.setTimezone("America/New_York");
                defaultProfile.setCurrency("USD");
                
                userProfileRepository.save(defaultProfile);
                System.out.println("✅ Default user profile created successfully!");
            }
            
            // Sync user profile with notification preferences
            UserProfile profile = userProfileRepository.findByUserId(DEFAULT_USER_ID).orElse(null);
            if (profile != null) {
                NotificationPreference prefs = preferenceRepository.findByUserId(DEFAULT_USER_ID).orElse(null);
                if (prefs != null) {
                    // Sync email and phone
                    if (prefs.getEmailAddress() == null || prefs.getEmailAddress().isEmpty()) {
                        prefs.setEmailAddress(profile.getEmail());
                    }
                    if (prefs.getPhoneNumber() == null || prefs.getPhoneNumber().isEmpty()) {
                        prefs.setPhoneNumber(profile.getPhoneNumber());
                    }
                    preferenceRepository.save(prefs);
                    System.out.println("✅ User profile synced with notification preferences!");
                }
            }
        };
    }
}
