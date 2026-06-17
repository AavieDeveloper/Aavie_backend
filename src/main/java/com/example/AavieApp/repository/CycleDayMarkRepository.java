package com.example.AavieApp.repository;

import com.example.AavieApp.model.CycleDayMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CycleDayMarkRepository extends JpaRepository<CycleDayMark, Long> {

    /** All marks for a specific cycle */
    List<CycleDayMark> findByCycleIdOrderByMarkDateAsc(Long cycleId);

    /** All marks for a user in a date range (for calendar month view) */
    @Query("SELECT c FROM CycleDayMark c WHERE c.userId = :userId " +
           "AND c.markDate >= :from AND c.markDate <= :to ORDER BY c.markDate ASC")
    List<CycleDayMark> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to);

    /** Single mark for a specific user + date */
    Optional<CycleDayMark> findByUserIdAndMarkDate(Long userId, LocalDate markDate);

    /** All marks of type=1 (period) for a user — descending, used to detect new cycles */
    @Query("SELECT c FROM CycleDayMark c WHERE c.userId = :userId " +
           "AND c.markType = 1 ORDER BY c.markDate DESC")
    List<CycleDayMark> findAllPeriodMarksByUserId(Long userId);

    List<CycleDayMark> findByUserIdAndCycleIdIsNull(Long userId);

    /** Count marks of a given type within a cycle */
    @Query("SELECT COUNT(c) FROM CycleDayMark c WHERE c.cycleId = :cycleId AND c.markType = :markType")
    long countByCycleIdAndMarkType(Long cycleId, Integer markType);

    void deleteByUserIdAndMarkDate(Long userId, LocalDate markDate);
}