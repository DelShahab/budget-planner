package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    
    // Find preferences by user ID
    Optional<NotificationPreference> findByUserId(String userId);
    
    // Check if email is enabled for user
    boolean existsByUserIdAndEmailEnabledTrue(String userId);
    
    // Check if SMS is enabled for user
    boolean existsByUserIdAndSmsEnabledTrue(String userId);
}
