package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Find all logs ordered by timestamp (newest first)
    List<AuditLog> findAllByOrderByTimestampDesc();
    
    // Find logs by entity type
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
    
    // Find logs by user
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);
    
    // Find logs by category
    List<AuditLog> findByCategoryOrderByTimestampDesc(String category);
    
    // Find logs by action
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
    
    // Find logs by severity
    List<AuditLog> findBySeverityOrderByTimestampDesc(String severity);
    
    // Find logs within date range
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    // Find logs for specific entity
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    
    // Count logs by category
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.category = :category")
    Long countByCategory(@Param("category") String category);
    
    // Count logs by severity
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = :severity")
    Long countBySeverity(@Param("severity") String severity);
    
    // Get recent logs (limit)
    @Query(value = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findRecentLogs(@Param("limit") int limit);
    
    // Search logs by description
    List<AuditLog> findByDescriptionContainingIgnoreCaseOrderByTimestampDesc(String keyword);
}
