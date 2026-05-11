package com.example.AavieApp.repository;

import com.example.AavieApp.model.CycleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CycleLogRepository extends JpaRepository<CycleLog, Long> {

    /** Most recent period start for a user */
    Optional<CycleLog> findTopByUserIdOrderByPeriodStartDateDesc(Long userId);

    /** All cycle logs for a user, newest first */
    List<CycleLog> findByUserIdOrderByPeriodStartDateDesc(Long userId);
}