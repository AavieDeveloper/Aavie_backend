package com.example.AavieApp.repository;

import com.example.AavieApp.model.SupplementPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupplementPlanRepository
        extends JpaRepository<SupplementPlan, Long> {

    // Most recent plan for a user
    Optional<SupplementPlan> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}