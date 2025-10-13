package com.budgetplanner.budget.repository;

import com.budgetplanner.budget.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // Find user profile by user ID
    Optional<UserProfile> findByUserId(String userId);
    
    // Find user profile by email
    Optional<UserProfile> findByEmail(String email);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Check if user ID exists
    boolean existsByUserId(String userId);
}
