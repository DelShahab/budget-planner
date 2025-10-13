package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    
    // Find all notifications ordered by creation date (newest first)
    List<AppNotification> findAllByOrderByCreatedAtDesc();
    
    // Find unread notifications
    List<AppNotification> findByIsReadFalseOrderByCreatedAtDesc();
    
    // Find notifications by category
    List<AppNotification> findByCategoryOrderByCreatedAtDesc(String category);
    
    // Find notifications by priority
    List<AppNotification> findByPriorityOrderByCreatedAtDesc(String priority);
    
    // Count unread notifications
    Long countByIsReadFalse();
}
