package com.example.AavieApp.repository;

import com.example.AavieApp.model.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findByAssessmentTypeAndIsActiveTrueOrderByQuestionOrderAsc(String assessmentType);
    List<AssessmentQuestion> findByAssessmentTypeOrderByQuestionOrderAsc(String assessmentType);
}