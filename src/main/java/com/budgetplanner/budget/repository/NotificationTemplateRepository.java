package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    // Find template by name
    Optional<NotificationTemplate> findByTemplateName(String templateName);
    
    // Find active templates by category
    List<NotificationTemplate> findByCategoryAndIsActiveTrue(String category);
    
    // Find all active templates
    List<NotificationTemplate> findByIsActiveTrueOrderByCreatedAtDesc();
    
    // Find templates by channel type
    List<NotificationTemplate> findByChannelTypeAndIsActiveTrue(String channelType);
}
