package com.example.AavieApp.service;

import com.example.AavieApp.model.DailyLog;
import com.example.AavieApp.repository.DailyLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyLogService {

    private final DailyLogRepository repo;

    public DailyLogService(DailyLogRepository repo) {
        this.repo = repo;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public static class SaveLogRequest {
        private Long userId;
        private String logDate;
        private List<String> moods;        // Can be null
        private List<String> bodySymptoms; // Can be null
        private List<String> behaviours;   // Can be null
        private Integer cycleDay;
        private String phase;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getLogDate() { return logDate; }
        public void setLogDate(String logDate) { this.logDate = logDate; }
        
        public List<String> getMoods() { return moods; }
        public void setMoods(List<String> moods) { this.moods = moods; }
        
        public List<String> getBodySymptoms() { return bodySymptoms; }
        public void setBodySymptoms(List<String> bodySymptoms) { this.bodySymptoms = bodySymptoms; }
        
        public List<String> getBehaviours() { return behaviours; }
        public void setBehaviours(List<String> behaviours) { this.behaviours = behaviours; }
        
        public Integer getCycleDay() { return cycleDay; }
        public void setCycleDay(Integer cycleDay) { this.cycleDay = cycleDay; }
        
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
    }

    public static class LogResponse {
        public Long id;
        public String logDate;
        public List<String> moods;
        public List<String> bodySymptoms;
        public List<String> behaviours;
        public Integer cycleDay;
        public String phase;
        public String updatedAt;
    }

    // ── Methods ────────────────────────────────────────────────────────────

    /** Save or update today's log (upsert by userId + logDate) */
    public LogResponse saveLog(SaveLogRequest req) {
        LocalDate date = LocalDate.parse(req.getLogDate());
        
        // Find existing or create new
        DailyLog log = repo.findByUserIdAndLogDate(req.getUserId(), date)
            .orElse(new DailyLog());

        // Set basic fields
        log.setUserId(req.getUserId());
        log.setLogDate(date);
        
        // ✅ CRITICAL: Only update fields that are EXPLICITLY provided (not null AND not empty)
        // This preserves existing data for fields not included in the request
        
        if (req.getMoods() != null && !req.getMoods().isEmpty()) {
            log.setMoods(listToString(req.getMoods()));
            System.out.println("✅ Updated moods: " + listToString(req.getMoods()));
        }
        // ❌ DO NOT set empty string for existing records
        
        if (req.getBodySymptoms() != null && !req.getBodySymptoms().isEmpty()) {
            log.setBodySymptoms(listToString(req.getBodySymptoms()));
            System.out.println("✅ Updated bodySymptoms: " + listToString(req.getBodySymptoms()));
        }
        
        if (req.getBehaviours() != null && !req.getBehaviours().isEmpty()) {
            log.setBehaviours(listToString(req.getBehaviours()));
            System.out.println("✅ Updated behaviours: " + listToString(req.getBehaviours()));
        }
        
        // Always update these non-array fields
        if (req.getCycleDay() != null) {
            log.setCycleDay(req.getCycleDay());
        }
        if (req.getPhase() != null) {
            log.setPhase(req.getPhase());
        }

        DailyLog saved = repo.save(log);
        
        // Debug: Log final state
        System.out.println("=== Final Saved Log ===");
        System.out.println("Moods: " + saved.getMoods());
        System.out.println("BodySymptoms: " + saved.getBodySymptoms());
        System.out.println("Behaviours: " + saved.getBehaviours());
        
        return toResponse(saved);
    }
    
    
    /** Get a single day's log */
    @Transactional(readOnly = true)
    public LogResponse getLog(Long userId, String date) {
        return repo.findByUserIdAndLogDate(userId, LocalDate.parse(date))
            .map(this::toResponse)
            .orElse(null);
    }

    /** Get all logs between two dates */
    @Transactional(readOnly = true)
    public List<LogResponse> getLogs(Long userId, String from, String to) {
        return repo.findByUserIdAndLogDateBetweenOrderByLogDateDesc(
                userId, LocalDate.parse(from), LocalDate.parse(to))
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Most recent log — used by profile health stats */
    @Transactional(readOnly = true)
    public LogResponse getLatestLog(Long userId) {
        return repo.findTopByUserIdOrderByLogDateDesc(userId)
            .map(this::toResponse)
            .orElse(null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private List<String> stringToList(String s) {
        if (s == null || s.isBlank()) return List.of();
        return List.of(s.split(","));
    }

    private LogResponse toResponse(DailyLog log) {
        LogResponse r = new LogResponse();
        r.id = log.getId();
        r.logDate = log.getLogDate().toString();
        r.moods = stringToList(log.getMoods());
        r.bodySymptoms = stringToList(log.getBodySymptoms());
        r.behaviours = stringToList(log.getBehaviours());
        r.cycleDay = log.getCycleDay();
        r.phase = log.getPhase();
        r.updatedAt = log.getUpdatedAt().toString();
        return r;
    }
}