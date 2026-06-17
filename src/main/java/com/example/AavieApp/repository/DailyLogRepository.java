package com.example.AavieApp.repository;
 
import com.example.AavieApp.model.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
 
    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);
 
    /** All logs for a specific cycle — primary query for insights */
    List<DailyLog> findByCycleIdOrderByLogDateAsc(Long cycleId);
 
    /** All logs for a user across multiple cycles — for cross-cycle comparisons */
    @Query("SELECT d FROM DailyLog d WHERE d.userId = :userId " +
           "AND d.cycleId IN :cycleIds ORDER BY d.logDate ASC")
    List<DailyLog> findByUserIdAndCycleIds(Long userId, List<Long> cycleIds);
 
    /** Count logs within a specific cycle — for "X of Y days logged" */
    long countByCycleId(Long cycleId);
 
    /** Logs in a date range within a cycle */
    @Query("SELECT d FROM DailyLog d WHERE d.cycleId = :cycleId " +
           "AND d.logDate >= :from AND d.logDate <= :to ORDER BY d.logDate ASC")
    List<DailyLog> findByCycleIdAndDateRange(Long cycleId, LocalDate from, LocalDate to);
 
    /** Most recent log for a user — used for streak */
    Optional<DailyLog> findTopByUserIdOrderByLogDateDesc(Long userId);
}
 