package com.example.AavieApp.repository;

import com.example.AavieApp.model.ManufacturingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManufacturingRunRepository extends JpaRepository<ManufacturingRun, Long> {

    /** Find manufacturing runs for a user */
    List<ManufacturingRun> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Find a specific manufacturing run */
    Optional<ManufacturingRun> findByUserIdAndAssessmentType(Long userId, String assessmentType);

    /** Find only revealed runs (for premium users) */
    List<ManufacturingRun> findByUserIdAndRevealedTrue(Long userId);
}
