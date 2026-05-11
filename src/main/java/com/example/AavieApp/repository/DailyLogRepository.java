package com.example.AavieApp.repository;

import com.example.AavieApp.model.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    /** All logs for a user in a date range — used for history view */
    List<DailyLog> findByUserIdAndLogDateBetweenOrderByLogDateDesc(
        Long userId, LocalDate from, LocalDate to);

    /** Most recent log — used for profile health stats */
    Optional<DailyLog> findTopByUserIdOrderByLogDateDesc(Long userId);

    /** Count logged days in a given month */
    long countByUserIdAndLogDateBetween(Long userId, LocalDate from, LocalDate to);
}