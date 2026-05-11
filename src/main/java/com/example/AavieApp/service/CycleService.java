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

 // REPLACE WITH:
    public static class CurrentCycleResponse {
        public int    cycleDay;
        public int    cycleLengthDays;
        public String phase;
        public double phasePct;
        public String periodStartDate;
        public String nextPeriodDate;
        public int    daysUntilNextPeriod;
        public boolean isTracking;
        // ── NEW fields for profile page ──
        public int    streak;
        public int    daysLogged;
        public String lastMood;
        public String lastLogDate;
    }

    // ── Methods ────────────────────────────────────────────────────────────

    /** Mark period start — resets the cycle */
    public CycleLog markPeriodStart(Long userId, LocalDate startDate) {
        CycleLog log = repo.findTopByUserIdOrderByPeriodStartDateDesc(userId)
            .orElse(new CycleLog());
        log.setUserId(userId);
        log.setPeriodStartDate(startDate);
        log.setCycleLengthDays(28);
        return repo.save(log);
    }

    /** Mark period end */
    public CycleLog markPeriodEnd(Long userId, LocalDate endDate) {
        CycleLog latest = repo.findTopByUserIdOrderByPeriodStartDateDesc(userId)
            .orElseThrow(() -> new RuntimeException("No cycle found for user: " + userId));
        latest.setPeriodEndDate(endDate);
        return repo.save(latest);
    }

    /** Get current cycle state */
    @Transactional(readOnly = true)
    public CurrentCycleResponse getCurrentCycle(Long userId) {
        Optional<CycleLog> opt = repo.findTopByUserIdOrderByPeriodStartDateDesc(userId);
        CurrentCycleResponse res = new CurrentCycleResponse();

        if (opt.isEmpty()) {
            res.isTracking = false;
            LocalDate today = LocalDate.now();
            LocalDate monthAgo = today.minusDays(30);
            List<com.example.AavieApp.model.DailyLog> allLogs =
                logRepo.findByUserIdAndLogDateBetweenOrderByLogDateDesc(
                    userId, monthAgo, today);
            res.daysLogged = allLogs.size();

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
            res.streak = streak;
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
        // If nextPeriod is in the past, recalculate
        while (nextPeriod.isBefore(today)) {
            nextPeriod = nextPeriod.plusDays(totalDays);
        }

        res.isTracking = true;
        res.cycleDay = cycleDay;
        res.cycleLengthDays = totalDays;
        res.phase = calculatePhase(cycleDay);
        res.phasePct = (double) cycleDay / totalDays;
        res.periodStartDate = start.toString();
        res.nextPeriodDate = nextPeriod.toString();
     // REPLACE WITH:
        res.daysUntilNextPeriod = (int) ChronoUnit.DAYS.between(today, nextPeriod);

        // ── Streak + daysLogged + lastMood from daily_logs ──
        LocalDate cycleStart = start;
        // Count how many days have been logged since period start
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

            res.daysLogged = logs.size();

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
        res.streak = streak;

        // Last mood
        if (!logs.isEmpty() && logs.get(0).getMoods() != null
                && !logs.get(0).getMoods().isBlank()) {
            String[] moods = logs.get(0).getMoods().split(",");
            res.lastMood    = moods[0].trim();
            res.lastLogDate = logs.get(0).getLogDate().toString();
        }

        return res;
    }

    private String calculatePhase(int day) {
        if (day <= 5)  return "menstrual";
        if (day <= 13) return "follicular";
        if (day <= 16) return "ovulation";
        return "luteal";
    }
}