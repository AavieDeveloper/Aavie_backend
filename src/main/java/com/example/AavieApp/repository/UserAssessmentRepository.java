package com.example.AavieApp.repository;

import com.example.AavieApp.model.UserAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Aavie — UserAssessment Repository
 */
@Repository
public interface UserAssessmentRepository extends JpaRepository<UserAssessment, Long> {

    /** Find a specific assessment type for a user */
    Optional<UserAssessment> findByUserIdAndAssessmentType(Long userId, String assessmentType);

    /** Find all assessments for a user (ordered by creation) */
    List<UserAssessment> findByUserIdOrderByCreatedAtAsc(Long userId);

    /** Check if a specific assessment exists for a user */
    boolean existsByUserIdAndAssessmentType(Long userId, String assessmentType);

    /** Count how many assessments a user has completed */
    long countByUserId(Long userId);
    
    long countByAssessmentType(String assessmentType);
    
    
 // ADD these two methods to the existing interface:
    List<UserAssessment> findAllByAssessmentType(String assessmentType);
    List<UserAssessment> findTop10ByOrderByUpdatedAtDesc();
    
    
 // Fetches ALL assessments for a list of users in ONE query
    List<UserAssessment> findByUserIdIn(List<Long> userIds);
}