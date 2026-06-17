package com.example.AavieApp.repository;

import com.example.AavieApp.model.SupplementOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplementOrderRepository
        extends JpaRepository<SupplementOrder, Long> {

    // All orders for a user, newest first
    List<SupplementOrder> findByUserIdOrderByOrderedAtDesc(Long userId);

    // Most recent order for a user
    Optional<SupplementOrder> findTopByUserIdOrderByOrderedAtDesc(Long userId);
    
    boolean existsByPaymentId(String paymentId);

    // All orders linked to a specific plan
    List<SupplementOrder> findByPlanIdOrderByOrderedAtDesc(Long planId);
}