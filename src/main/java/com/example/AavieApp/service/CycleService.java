package com.example.AavieApp.service;

import com.example.AavieApp.model.CycleLog;

import com.example.AavieApp.repository.CycleLogRepository;
import com.example.AavieApp.repository.DailyLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CycleService {

	// REPLACE WITH:
	private final CycleLogRepository  repo;
	private final DailyLogRepository  logRepo;

	public CycleService(CycleLogRepository repo, DailyLogRepository logRepo) {
	    this.repo    = repo;
	    this.logRepo = logRepo;
	}

    // ── DTOs ──────────────────────────────────────────────────────────────

    public static class PeriodStartRequest {
        private Long userId;
        private String periodStartDate; // "YYYY-MM-DD"
        public Long getUserId() { return userId; }
        public void setUserId(Long u) { this.userId = u; }
        public String getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(String d) { this.periodStartDate = d; }
    }

 // In CycleService.java, replace the CurrentCycleResponse class

    public static class CurrentCycleResponse {
        private int cycleDay;
        private int cycleLengthDays;
        private String phase;
        private double phasePct;
        private String periodStartDate;
        private String nextPeriodDate;
        private int daysUntilNextPeriod;
        private boolean isTracking;
        private int streak;
        private int daysLogged;
        private String lastMood;
        private String lastLogDate;

        // ✅ ADD GETTERS AND SETTERS
        public int getCycleDay() { return cycleDay; }
        public void setCycleDay(int cycleDay) { this.cycleDay = cycleDay; }

        public int getCycleLengthDays() { return cycleLengthDays; }
        public void setCycleLengthDays(int cycleLengthDays) { this.cycleLengthDays = cycleLengthDays; }

        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }

        public double getPhasePct() { return phasePct; }
        public void setPhasePct(double phasePct) { this.phasePct = phasePct; }

        public String getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(String periodStartDate) { this.periodStartDate = periodStartDate; }

        public String getNextPeriodDate() { return nextPeriodDate; }
        public void setNextPeriodDate(String nextPeriodDate) { this.nextPeriodDate = nextPeriodDate; }

        public int getDaysUntilNextPeriod() { return daysUntilNextPeriod; }
        public void setDaysUntilNextPeriod(int daysUntilNextPeriod) { this.daysUntilNextPeriod = daysUntilNextPeriod; }

        public boolean isTracking() { return isTracking; }
        public void setTracking(boolean tracking) { isTracking = tracking; }

        // ✅ THESE ARE THE ONES THAT WERE MISSING
        public int getStreak() { return streak; }
        public void setStreak(int streak) { this.streak = streak; }

        public int getDaysLogged() { return daysLogged; }
        public void setDaysLogged(int daysLogged) { this.daysLogged = daysLogged; }

        public String getLastMood() { return lastMood; }
        public void setLastMood(String lastMood) { this.lastMood = lastMood; }

        public String getLastLogDate() { return lastLogDate; }
        public void setLastLogDate(String lastLogDate) { this.lastLogDate = lastLogDate; }
    }

    
    
    
    // ── Methods ────────────────────────────────────────────────────────────

    /** Mark period start — resets the cycle */
    public CycleLog markPeriodStart(Long userId, LocalDate startDate) {
        System.out.println("🔴 markPeriodStart called: userId=" + userId + " date=" + startDate);
        
        // Check if already exists as a current cycle entry
        List<CycleLog> existing = repo.findByUserIdAndHistoryOnlyFalseOrderByPeriodStartDateDesc(userId);
        System.out.println("🔴 existing size: " + existing.size());
        
        // Check for duplicate
        for (CycleLog c : existing) {
            if (c.getPeriodStartDate().equals(startDate)) {
                System.out.println("🔴 Already exists, returning existing");
                return c;
            }
        }

        // Always create a new current cycle entry (historyOnly = false)
        CycleLog log = new CycleLog();
        log.setUserId(userId);
        log.setPeriodStartDate(startDate);
        log.setCycleLengthDays(28);
        log.setHistoryOnly(false); // ← explicitly set to false
        System.out.println("🔴 Saving new cycle with historyOnly=false");
        return repo.save(log);
    }
    
 // Saves period start as history only — never affects current cycle
    public CycleLog savePeriodHistory(Long userId, LocalDate startDate) {
        // Check if already exists
    	System.out.println("📅 savePeriodHistory called: userId=" + userId + " date=" + startDate);
    	
    	List<CycleLog> existing = repo.findByUserIdAndHistoryOnlyFalseOrderByPeriodStartDateDesc(userId);
        for (CycleLog c : existing) {
            if (c.getPeriodStartDate().equals(startDate)) {
                return c; // already saved
            }
        }
        // Get current latest period start
        LocalDate latestStart = existing.isEmpty() ? null : existing.get(0).getPeriodStartDate();

        // Only save if this date is OLDER than current latest
        // This prevents overwriting the current cycle
        if (latestStart != null && !startDate.isBefore(latestStart)) {
            // Don't save — this would affect current cycle
            return existing.get(0);
        }

        CycleLog log = new CycleLog();
        log.setUserId(userId);
        log.setPeriodStartDate(startDate);
        log.setCycleLengthDays(28);
        log.setHistoryOnly(true);
        System.out.println("📅 Saving historyOnly=true for date=" + startDate);
        return repo.save(log);
    }
    @Transactional(readOnly = true)
    public List<CycleLog> getCycleHistory(Long userId) {
        return repo.findByUserIdOrderByPeriodStartDateDesc(userId); // returns all including history
    }

    /** Mark period end */
    public CycleLog markPeriodEnd(Long userId, LocalDate endDate) {
        CycleLog latest = repo.findTopByUserIdOrderByPeriodStartDateDesc(userId)
            .orElseThrow(() -> new RuntimeException("No cycle found for user: " + userId));
        latest.setPeriodEndDate(endDate);
        return repo.save(latest);
    }

    /** Get current cycle state */
    /** Get current cycle state */
    @Transactional(readOnly = true)
    public CurrentCycleResponse getCurrentCycle(Long userId) {
    	Optional<CycleLog> opt = repo.findTopByUserIdAndHistoryOnlyFalseOrderByPeriodStartDateDesc(userId);
    	System.out.println("📅 getCurrentCycle: found cycle = " + (opt.isPresent() ? opt.get().getPeriodStartDate() : "NONE"));
        CurrentCycleResponse res = new CurrentCycleResponse();

        if (opt.isEmpty()) {
            res.setTracking(false);  // ✅ Use setter
            LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            LocalDate monthAgo = today.minusDays(30);
            List<com.example.AavieApp.model.DailyLog> allLogs =
                logRepo.findByUserIdAndLogDateBetweenOrderByLogDateDesc(
                    userId, monthAgo, today);
            res.setDaysLogged(allLogs.size());  // ✅ Use setter

            System.out.println("=== Streak Debug (no period) ===");
            System.out.println("Today: " + today);
            System.out.println("Total logs: " + allLogs.size());
            allLogs.forEach(l -> System.out.println("  Log date: " + l.getLogDate()));

            int streak = 0;
            LocalDate check = today;
            while (true) {
                LocalDate finalCheck = check;
                boolean logged = allLogs.stream()
                    .anyMatch(l -> l.getLogDate().equals(finalCheck));
                System.out.println("Checking: " + finalCheck + " logged: " + logged);
                if (logged) { streak++; check = check.minusDays(1); }
                else break;
            }
            System.out.println("Final streak: " + streak);
            res.setStreak(streak);  // ✅ Use setter
            return res;
        }
        CycleLog cycle = opt.get();
        LocalDate today = LocalDate.now();
        LocalDate start = cycle.getPeriodStartDate();
        int totalDays = cycle.getCycleLengthDays();

        long daysSinceStart = ChronoUnit.DAYS.between(start, today);
        int cycleDay = (int)(daysSinceStart % totalDays) + 1;
        if (cycleDay < 1) cycleDay = 1;

        LocalDate nextPeriod = start.plusDays(totalDays);
        while (nextPeriod.isBefore(today)) {
            nextPeriod = nextPeriod.plusDays(totalDays);
        }

        // ✅ Use setters for ALL fields
        res.setTracking(true);
        res.setCycleDay(cycleDay);
        res.setCycleLengthDays(totalDays);
        res.setPhase(calculatePhase(cycleDay));
        res.setPhasePct((double) cycleDay / totalDays);
        res.setPeriodStartDate(start.toString());
        res.setNextPeriodDate(nextPeriod.toString());
        res.setDaysUntilNextPeriod((int) ChronoUnit.DAYS.between(today, nextPeriod));

        // ── Streak + daysLogged + lastMood from daily_logs ──
        LocalDate cycleStart = start;
        List<com.example.AavieApp.model.DailyLog> logs =
                logRepo.findByUserIdAndLogDateBetweenOrderByLogDateDesc(
                    userId, cycleStart, today);

        System.out.println("=== CycleService Debug ===");
        System.out.println("userId: " + userId);
        System.out.println("cycleStart: " + cycleStart);
        System.out.println("today: " + today);
        System.out.println("logs found: " + logs.size());
        for (com.example.AavieApp.model.DailyLog l : logs) {
            System.out.println("  logDate: " + l.getLogDate() +
                               " userId: " + l.getUserId() +
                               " moods: " + l.getMoods());
        }

        res.setDaysLogged(logs.size());  // ✅ Use setter

        // Streak — consecutive logged days ending today
        int streak = 0;
        LocalDate check = today;
        while (true) {
            LocalDate finalCheck = check;
            boolean logged = logs.stream()
                .anyMatch(l -> l.getLogDate().equals(finalCheck));
            if (logged) { streak++; check = check.minusDays(1); }
            else break;
        }
        res.setStreak(streak);  // ✅ Use setter

        // Last mood
        if (!logs.isEmpty() && logs.get(0).getMoods() != null
                && !logs.get(0).getMoods().isBlank()) {
            String[] moods = logs.get(0).getMoods().split(",");
            res.setLastMood(moods[0].trim());  // ✅ Use setter
            res.setLastLogDate(logs.get(0).getLogDate().toString());  // ✅ Use setter
        }

        // ✅ Debug final values
        System.out.println("=== Final Response ===");
        System.out.println("streak: " + res.getStreak());
        System.out.println("daysLogged: " + res.getDaysLogged());
        System.out.println("lastMood: " + res.getLastMood());

        return res;
    }

    private String calculatePhase(int day) {
        if (day <= 5)  return "menstrual";
        if (day <= 13) return "follicular";
        if (day <= 16) return "ovulation";
        return "luteal";
    }
}