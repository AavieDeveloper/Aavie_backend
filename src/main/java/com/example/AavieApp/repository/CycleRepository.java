package com.example.AavieApp.repository;
 
import com.example.AavieApp.model.Cycle;
import com.example.AavieApp.model.CycleDayMark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {
 
    /** All cycles for a user ordered by cycle number */
    List<Cycle> findByUserIdOrderByCycleNumberAsc(Long userId);
 
    /** The current active cycle — end_date is null */
    @Query("SELECT c FROM Cycle c WHERE c.userId = :userId AND c.endDate IS NULL ORDER BY c.cycleNumber DESC")
    List<Cycle> findActiveCyclesInternal(@Param("userId") Long userId);

    default Optional<Cycle> findActiveCycleByUserId(Long userId) {
        List<Cycle> cycles = findActiveCyclesInternal(userId);
        if (cycles.isEmpty()) return Optional.empty();
        return Optional.of(cycles.get(0));
    }
 
    /** Most recent N cycles — for comparison features */
    @Query("SELECT c FROM Cycle c WHERE c.userId = :userId ORDER BY c.cycleNumber DESC")
    List<Cycle> findRecentCyclesByUserId(Long userId);
 
    @Query("SELECT c FROM Cycle c WHERE c.userId = :userId " +
            "AND c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date) " +
            "ORDER BY c.startDate DESC")
     List<Cycle> findCyclesForDateInternal(@Param("userId") Long userId, @Param("date") LocalDate date);

     default Optional<Cycle> findCycleForDate(Long userId, LocalDate date) {
         List<Cycle> cycles = findCyclesForDateInternal(userId, date);
         if (cycles.isEmpty()) return Optional.empty();
         // Prefer closed cycle (endDate not null) over active cycle for past dates
         return cycles.stream()
             .filter(c -> c.getEndDate() != null)
             .findFirst()
             .or(() -> Optional.of(cycles.get(0)));
     }
    
    Optional<Cycle> findByUserIdAndCycleNumber(Long userId, Integer cycleNumber);
 
    /** Count total cycles for a user */
    long countByUserId(Long userId);
 
    /** Latest cycle number for a user — used to auto-increment */
    @Query("SELECT MAX(c.cycleNumber) FROM Cycle c WHERE c.userId = :userId")
    Optional<Integer> findMaxCycleNumberByUserId(Long userId);
    
    
    @Query("SELECT c FROM Cycle c WHERE c.userId = :userId AND c.endDate IS NOT NULL " +
    	       "ORDER BY c.startDate DESC")
    	org.springframework.data.domain.Page<Cycle> findRecentCompletedCyclesPage(
    	    @Param("userId") Long userId,
    	    org.springframework.data.domain.Pageable pageable);

    	default List<Cycle> findRecentCompletedCycles(Long userId, int limit) {
    	    return findRecentCompletedCyclesPage(userId,
    	        org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    	}
    	
    
}
 