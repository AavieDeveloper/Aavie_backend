package com.example.AavieApp.repository;

import com.example.AavieApp.model.AssessmentDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssessmentDraftRepository extends JpaRepository<AssessmentDraft, Long> {
    Optional<AssessmentDraft> findByUserIdAndAssessmentType(Long userId, String assessmentType);
    void deleteByUserIdAndAssessmentType(Long userId, String assessmentType);
}