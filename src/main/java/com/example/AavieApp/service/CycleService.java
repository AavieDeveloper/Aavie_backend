package com.example.AavieApp.service;

import com.example.AavieApp.model.*;

import com.example.AavieApp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aavie — CycleService (v2)
 *
 * Key changes from v1:
 *  1. Cycle management — creates/closes Cycle rows, finds active cycle
 *  2. All queries are cycle-aware (cycle_id FK)
 *  3. discharge, behaviours, chips are List<String> — no more split/join
 */
@Service
@Transactional
public class CycleService {

	private final CycleRepository       cycleRepo;
    private final CycleDayMarkRepository  markRepo;
    private final CycleSettingsRepository settingsRepo;

    private final UserAssessmentRepository assessRepo;
    private final DailyLogRepository      logRepo;
    
    private static final int MERGE_GAP_DAYS = 10; // marks within this many days = same period
    private static final int MIN_CYCLE_GAP  = 20; // shortest gap counted as a real cycle length
    private static final int MAX_CYCLE_GAP  = 45; // longest gap counted as a real cycle length
    
    private static final Map<String, Integer> MOOD_SCORE_MAP = Map.of(
            "radiant",   95,
            "happy",     80,
            "calm",      70,
            "sensitive", 60,
            "anxious",   40,
            "irritable", 35,
            "low",       30,
            "tired",     25
        );

        private static final Map<String, Integer> ENERGY_SCORE_MAP = Map.of(
            "charged",  95,
            "flowing",  75,
            "steady",   55,
            "low",      35,
            "depleted", 15
        );

    public CycleService(CycleRepository cycleRepo,
                        CycleDayMarkRepository markRepo,
                        CycleSettingsRepository settingsRepo,
                        DailyLogRepository logRepo,
                        UserAssessmentRepository assessRepo) {
        this.cycleRepo    = cycleRepo;
        this.markRepo     = markRepo;
        this.settingsRepo = settingsRepo;
        this.logRepo      = logRepo;
        this.assessRepo   = assessRepo;
    }
    // ═════════════════════════════════════════════════════════════════════════
    //  DTOs
    // ═════════════════════════════════════════════════════════════════════════

    public static class SaveMarksRequest {
        private Long userId;
        /** "YYYY-MM-DD" → markType (1-5), or 0 to delete */
        private Map<String, Integer> marks;
        public Long               getUserId()           { return userId; }
        public void               setUserId(Long v)     { this.userId = v; }
        public Map<String, Integer> getMarks()          { return marks; }
        public void               setMarks(Map<String, Integer> v) { this.marks = v; }
    }

    public static class MarksResponse {
        private Map<String, Integer> marks;
        public MarksResponse(Map<String, Integer> marks) { this.marks = marks; }
        public Map<String, Integer> getMarks() { return marks; }
    }

    public static class CycleStateResponse {
        private long    cycleId;
        private int     cycleNumber;
        private int     cycleDay;
        private int     cycleLength;
        private int     periodLength;
        private String  phase;
        private String  phaseKey;
        private String  cycleStartDate;
        private String  nextPeriodDate;
        private int     daysUntilNext;
        private int     loggedDays;
        private int     totalCycleDays;
        private Map<String, Long> moodCounts;
        private Map<String, Long> dischargeCounts;
        private Map<String, Long> symptomCounts;
        private int avgCycleLength;
        private int patternCycleCount;
        private int cycleVariation;
        private int periodVariation;
        private int actualCycleLength;
        private int periodLengthMin;
        private int periodLengthMax;
        private String pcosResult;
        private String prakritiResult;
        private Map<String, Integer> energyByDay;
        private Map<String, Integer> sleepByDay;
        private Map<String, Integer> stressByDay;

        public long   getCycleId()                         { return cycleId; }
        public void   setCycleId(long v)                   { this.cycleId = v; }
        public int    getCycleNumber()                     { return cycleNumber; }
        public void   setCycleNumber(int v)                { this.cycleNumber = v; }
        public int    getCycleDay()                        { return cycleDay; }
        public void   setCycleDay(int v)                   { this.cycleDay = v; }
        public int    getCycleLength()                     { return cycleLength; }
        public void   setCycleLength(int v)                { this.cycleLength = v; }
        public int    getPeriodLength()                    { return periodLength; }
        public void   setPeriodLength(int v)               { this.periodLength = v; }
        public String getPhase()                           { return phase; }
        public void   setPhase(String v)                   { this.phase = v; }
        public String getPhaseKey()                        { return phaseKey; }
        public void   setPhaseKey(String v)                { this.phaseKey = v; }
        public String getCycleStartDate()                  { return cycleStartDate; }
        public void   setCycleStartDate(String v)          { this.cycleStartDate = v; }
        public String getNextPeriodDate()                  { return nextPeriodDate; }
        public void   setNextPeriodDate(String v)          { this.nextPeriodDate = v; }
        public int    getDaysUntilNext()                   { return daysUntilNext; }
        public void   setDaysUntilNext(int v)              { this.daysUntilNext = v; }
        public int    getLoggedDays()                      { return loggedDays; }
        public void   setLoggedDays(int v)                 { this.loggedDays = v; }
        public int    getTotalCycleDays()                  { return totalCycleDays; }
        public void   setTotalCycleDays(int v)             { this.totalCycleDays = v; }
        public Map<String, Long> getMoodCounts()           { return moodCounts; }
        public void   setMoodCounts(Map<String, Long> v)   { this.moodCounts = v; }
        public Map<String, Long> getDischargeCounts()      { return dischargeCounts; }
        public void   setDischargeCounts(Map<String, Long> v) { this.dischargeCounts = v; }
        public Map<String, Long> getSymptomCounts()        { return symptomCounts; }
        public void   setSymptomCounts(Map<String, Long> v){ this.symptomCounts = v; }
    

        public int  getAvgCycleLength()           { return avgCycleLength; }
        public void setAvgCycleLength(int v)      { this.avgCycleLength = v; }
        public int  getPatternCycleCount()        { return patternCycleCount; }
        public void setPatternCycleCount(int v)   { this.patternCycleCount = v; }
        public int  getCycleVariation()           { return cycleVariation; }
        public void setCycleVariation(int v)      { this.cycleVariation = v; }
        
        public int  getPeriodVariation()          { return periodVariation; }
        public void setPeriodVariation(int v)     { this.periodVariation = v; }
        public int  getActualCycleLength()        { return actualCycleLength; }
        public void setActualCycleLength(int v)   { this.actualCycleLength = v; }
        @com.fasterxml.jackson.annotation.JsonProperty("periodLengthMin")
        public int  getPeriodLengthMin()          { return periodLengthMin; }
        public void setPeriodLengthMin(int v)     { this.periodLengthMin = v; }

        @com.fasterxml.jackson.annotation.JsonProperty("periodLengthMax")
        public int  getPeriodLengthMax()          { return periodLengthMax; }
        public void setPeriodLengthMax(int v)     { this.periodLengthMax = v; }
        public Map<String, Integer> getEnergyByDay()              { return energyByDay; }
        public void setEnergyByDay(Map<String, Integer> v)        { this.energyByDay = v; }
        public Map<String, Integer> getSleepByDay()               { return sleepByDay; }
        public void setSleepByDay(Map<String, Integer> v)         { this.sleepByDay = v; }
        public Map<String, Integer> getStressByDay()              { return stressByDay; }
        public void setStressByDay(Map<String, Integer> v)        { this.stressByDay = v; }
        
        public String getPcosResult()              { return pcosResult; }
        public void   setPcosResult(String v)      { this.pcosResult = v; }
        public String getPrakritiResult()          { return prakritiResult; }
        public void   setPrakritiResult(String v)  { this.prakritiResult = v; }
    } // ← closes CycleStateResponse

    public static class SaveDailyLogRequest {
        private Long         userId;
        private Long         cycleId;   // frontend sends this after getting state
        private String       date;
        private String       day;
        private Integer      cycleDay;
        private String       phase;
        private String       energy;
        private List<String> discharge;
        private String       mood;
        private Map<String, List<String>> zones;
        private List<String> behaviours;
        private String       character;

        public Long         getUserId()              { return userId; }
        public void         setUserId(Long v)        { this.userId = v; }
        public Long         getCycleId()             { return cycleId; }
        public void         setCycleId(Long v)       { this.cycleId = v; }
        public String       getDate()                { return date; }
        public void         setDate(String v)        { this.date = v; }
        public String       getDay()                 { return day; }
        public void         setDay(String v)         { this.day = v; }
        public Integer      getCycleDay()            { return cycleDay; }
        public void         setCycleDay(Integer v)   { this.cycleDay = v; }
        public String       getPhase()               { return phase; }
        public void         setPhase(String v)       { this.phase = v; }
        public String       getEnergy()              { return energy; }
        public void         setEnergy(String v)      { this.energy = v; }
        public List<String> getDischarge()           { return discharge; }
        public void         setDischarge(List<String> v) { this.discharge = v; }
        public String       getMood()                { return mood; }
        public void         setMood(String v)        { this.mood = v; }
        public Map<String, List<String>> getZones()  { return zones; }
        public void         setZones(Map<String, List<String>> v) { this.zones = v; }
        public List<String> getBehaviours()          { return behaviours; }
        public void         setBehaviours(List<String> v) { this.behaviours = v; }
        public String       getCharacter()           { return character; }
        public void         setCharacter(String v)   { this.character = v; }
    }

    public static class DailyLogResponse {
        private Long         id;
        private Long         cycleId;
        private String       logDate;
        private String       dayLabel;
        private Integer      cycleDay;
        private String       phase;
        private String       energy;
        private List<String> discharge;
        private String       mood;
        private Map<String, List<String>> zones;
        private List<String> behaviours;
        private String       characterState;

        public Long         getId()                            { return id; }
        public void         setId(Long v)                      { this.id = v; }
        public Long         getCycleId()                       { return cycleId; }
        public void         setCycleId(Long v)                 { this.cycleId = v; }
        public String       getLogDate()                       { return logDate; }
        public void         setLogDate(String v)               { this.logDate = v; }
        public String       getDayLabel()                      { return dayLabel; }
        public void         setDayLabel(String v)              { this.dayLabel = v; }
        public Integer      getCycleDay()                      { return cycleDay; }
        public void         setCycleDay(Integer v)             { this.cycleDay = v; }
        public String       getPhase()                         { return phase; }
        public void         setPhase(String v)                 { this.phase = v; }
        public String       getEnergy()                        { return energy; }
        public void         setEnergy(String v)                { this.energy = v; }
        public List<String> getDischarge()                     { return discharge; }
        public void         setDischarge(List<String> v)       { this.discharge = v; }
        public String       getMood()                          { return mood; }
        public void         setMood(String v)                  { this.mood = v; }
        public Map<String, List<String>> getZones()            { return zones; }
        public void         setZones(Map<String, List<String>> v) { this.zones = v; }
        public List<String> getBehaviours()                    { return behaviours; }
        public void         setBehaviours(List<String> v)      { this.behaviours = v; }
        public String       getCharacterState()                { return characterState; }
        public void         setCharacterState(String v)        { this.characterState = v; }
    }

    public static class InsightsResponse {
        private CycleStateResponse currentCycle;
        private List<DailyLogResponse> logs;
        private Map<String, Long> moodCounts;
        private Map<String, Long> symptomCounts;
        private Map<String, Long> behaviourCounts;
        private Map<String, Long> dischargeCounts;
        private Map<String, Object> phaseStats;
        private List<Integer> moodArray;    // one value per cycle day for chart
        private List<Integer> energyArray;  // one value per cycle day for chart
        private int totalLoggedDays;
        private int loggedDays;
        private int cycleDay;
        private int cycleLength;
        private int periodLength;
        private int cycleNumber;
        private String cycleStartDate;
        private String nextPeriodDate;
        private List<Integer> missingDays;
        private String prakritiResult;
        private String pcosResult;
        
        private CyclePreviousStats nowCycleStats;   // most recently COMPLETED cycle — shown as "Now"
        private CyclePreviousStats previousCycle1;  // 3 cycles ago
        private CyclePreviousStats previousCycle2;  // 2 cycles ago

        public CyclePreviousStats getNowCycleStats()          { return nowCycleStats; }
        public void setNowCycleStats(CyclePreviousStats v)    { this.nowCycleStats = v; }
        public CyclePreviousStats getPreviousCycle1()         { return previousCycle1; }
        public void setPreviousCycle1(CyclePreviousStats v)   { this.previousCycle1 = v; }
        public CyclePreviousStats getPreviousCycle2()         { return previousCycle2; }
        public void setPreviousCycle2(CyclePreviousStats v)   { this.previousCycle2 = v; }

        public CycleStateResponse getCurrentCycle()           { return currentCycle; }
        public void setCurrentCycle(CycleStateResponse v)     { this.currentCycle = v; }
        public List<DailyLogResponse> getLogs()               { return logs; }
        public void setLogs(List<DailyLogResponse> v)         { this.logs = v; }
        public Map<String, Long> getMoodCounts()              { return moodCounts; }
        public void setMoodCounts(Map<String, Long> v)        { this.moodCounts = v; }
        public Map<String, Long> getSymptomCounts()           { return symptomCounts; }
        public void setSymptomCounts(Map<String, Long> v)     { this.symptomCounts = v; }
        public Map<String, Long> getBehaviourCounts()         { return behaviourCounts; }
        public void setBehaviourCounts(Map<String, Long> v)   { this.behaviourCounts = v; }
        public Map<String, Long> getDischargeCounts()         { return dischargeCounts; }
        public void setDischargeCounts(Map<String, Long> v)   { this.dischargeCounts = v; }
        public Map<String, Object> getPhaseStats()            { return phaseStats; }
        public void setPhaseStats(Map<String, Object> v)      { this.phaseStats = v; }
        public List<Integer> getMoodArray()                   { return moodArray; }
        public void setMoodArray(List<Integer> v)             { this.moodArray = v; }
        public List<Integer> getEnergyArray()                 { return energyArray; }
        public void setEnergyArray(List<Integer> v)           { this.energyArray = v; }
        public int getTotalLoggedDays()                       { return totalLoggedDays; }
        public void setTotalLoggedDays(int v)                 { this.totalLoggedDays = v; }
        public int getLoggedDays()                            { return loggedDays; }
        public void setLoggedDays(int v)                      { this.loggedDays = v; }
        public int getCycleDay()                              { return cycleDay; }
        public void setCycleDay(int v)                        { this.cycleDay = v; }
        public int getCycleLength()                           { return cycleLength; }
        public void setCycleLength(int v)                     { this.cycleLength = v; }
        public int getPeriodLength()                          { return periodLength; }
        public void setPeriodLength(int v)                    { this.periodLength = v; }
        public int getCycleNumber()                           { return cycleNumber; }
        public void setCycleNumber(int v)                     { this.cycleNumber = v; }
        public String getCycleStartDate()                     { return cycleStartDate; }
        public void setCycleStartDate(String v)               { this.cycleStartDate = v; }
        public String getNextPeriodDate()                     { return nextPeriodDate; }
        public void setNextPeriodDate(String v)               { this.nextPeriodDate = v; }
        public List<Integer> getMissingDays()                 { return missingDays; }
        public void setMissingDays(List<Integer> v)           { this.missingDays = v; }
        public String getPrakritiResult()                     { return prakritiResult; }
        public void setPrakritiResult(String v)               { this.prakritiResult = v; }
        public String getPcosResult()                         { return pcosResult; }
        public void setPcosResult(String v)                   { this.pcosResult = v; }
    }
    
    public static class CyclePreviousStats {
        private int cycleLength;
        private int periodLength;
        private int avgMood;
        private int avgEnergy;
        private int symptomDays;
        private int loggedDays;
        private String startDate; // ISO date string, e.g. "2026-05-14"

        public int getCycleLength()          { return cycleLength; }
        public void setCycleLength(int v)     { this.cycleLength = v; }
        public int getPeriodLength()          { return periodLength; }
        public void setPeriodLength(int v)    { this.periodLength = v; }
        public int getAvgMood()               { return avgMood; }
        public void setAvgMood(int v)         { this.avgMood = v; }
        public int getAvgEnergy()             { return avgEnergy; }
        public void setAvgEnergy(int v)       { this.avgEnergy = v; }
        public int getSymptomDays()           { return symptomDays; }
        public void setSymptomDays(int v)     { this.symptomDays = v; }
        public int getLoggedDays()            { return loggedDays; }
        public void setLoggedDays(int v)      { this.loggedDays = v; }
        public String getStartDate()          { return startDate; }
        public void setStartDate(String v)    { this.startDate = v; }
    }
    
    @Transactional(readOnly = true)
    public InsightsResponse getInsights(Long userId) {
        CycleStateResponse state = getCycleState(userId);
        Cycle activeCycle = cycleRepo.findActiveCycleByUserId(userId).orElse(null);

        List<DailyLog> logs = activeCycle != null
            ? logRepo.findByCycleIdOrderByLogDateAsc(activeCycle.getId())
            : new ArrayList<>();

        List<DailyLogResponse> logResponses = logs.stream()
            .map(this::toLogResponse)
            .collect(Collectors.toList());

        int cycleDay    = state.getCycleDay();
        int cycleLength = state.getCycleLength();
        int periodLen   = state.getPeriodLength();

        Map<String, Integer> moodScoreMap   = MOOD_SCORE_MAP;
        Map<String, Integer> energyScoreMap = ENERGY_SCORE_MAP;

        // ── Per-phase accumulators ────────────────────────────────────────────
        Map<String, List<Integer>> moodByPhase   = new LinkedHashMap<>();
        Map<String, List<Integer>> energyByPhase = new LinkedHashMap<>();
        Map<String, Set<String>>   symptomDaysByPhase = new LinkedHashMap<>();
        for (String ph : List.of("menstrual","follicular","ovulation","luteal")) {
            moodByPhase.put(ph,   new ArrayList<>());
            energyByPhase.put(ph, new ArrayList<>());
            symptomDaysByPhase.put(ph, new HashSet<>());
        }

        // ── Counts ────────────────────────────────────────────────────────────
        Map<String, Long> moodCounts       = new LinkedHashMap<>();
        Map<String, Long> symptomCounts    = new LinkedHashMap<>();
        Map<String, Long> behaviourCounts  = new LinkedHashMap<>();
        Map<String, Long> dischargeCounts  = new LinkedHashMap<>();

        // ── Chart arrays ─────────────────────────────────────────────────────
        List<Integer> moodArr   = new ArrayList<>(Collections.nCopies(cycleLength, 0));
        List<Integer> energyArr = new ArrayList<>(Collections.nCopies(cycleLength, 0));

        LocalDate cycleStart = activeCycle != null ? activeCycle.getStartDate() : null;

        for (DailyLog log : logs) {
            String phase = log.getPhase() != null
                ? log.getPhase().toLowerCase() : "luteal";

            // Mood
            if (log.getMood() != null) {
                int mScore = moodScoreMap.getOrDefault(log.getMood(), 50);
                moodCounts.merge(log.getMood(), 1L, Long::sum);
                moodByPhase.computeIfAbsent(phase, k -> new ArrayList<>()).add(mScore);

                // Chart
                if (cycleStart != null) {
                    long idx = ChronoUnit.DAYS.between(cycleStart, log.getLogDate());
                    if (idx >= 0 && idx < cycleLength) {
                        moodArr.set((int) idx, mScore);
                    }
                }
            }

            // Energy
            if (log.getEnergy() != null) {
                int eScore = energyScoreMap.getOrDefault(log.getEnergy(), 50);
                energyByPhase.computeIfAbsent(phase, k -> new ArrayList<>()).add(eScore);

                if (cycleStart != null) {
                    long idx = ChronoUnit.DAYS.between(cycleStart, log.getLogDate());
                    if (idx >= 0 && idx < cycleLength) {
                        energyArr.set((int) idx, eScore);
                    }
                }
            }

            // Symptoms from body zones
            for (DailyLogBodyZone zone : log.getBodyZones()) {
                if (zone.getChips() == null) continue;
                for (String chip : zone.getChips()) {
                    symptomCounts.merge(chip, 1L, Long::sum);
                    symptomDaysByPhase.computeIfAbsent(phase, k -> new HashSet<>())
                        .add(log.getLogDate().toString());
                }
            }

            // Behaviours
            if (log.getBehaviours() != null) {
                for (String b : log.getBehaviours()) {
                    behaviourCounts.merge(b, 1L, Long::sum);
                }
            }

            // Discharge
            if (log.getDischarge() != null) {
                for (String d : log.getDischarge()) {
                    if (!d.equals("none")) {
                        dischargeCounts.merge(d, 1L, Long::sum);
                    }
                }
            }
        }

        // ── Phase stats ───────────────────────────────────────────────────────
        Map<String, Object> phaseStats = new LinkedHashMap<>();
        for (String ph : List.of("menstrual","follicular","ovulation","luteal")) {
            List<Integer> ms = moodByPhase.get(ph);
            List<Integer> es = energyByPhase.get(ph);
            int avgMood   = ms != null && !ms.isEmpty()
                ? ms.stream().mapToInt(i -> i).sum() / ms.size() : 0;
            int avgEnergy = es != null && !es.isEmpty()
                ? es.stream().mapToInt(i -> i).sum() / es.size() : 0;
            int symDays   = symptomDaysByPhase.getOrDefault(ph, new HashSet<>()).size();

            Map<String, Object> ps = new LinkedHashMap<>();
            ps.put("avgMood",     avgMood);
            ps.put("avgEnergy",   avgEnergy);
            ps.put("symptomDays", symDays);
            phaseStats.put(ph, ps);
        }

        // ── Missing days ──────────────────────────────────────────────────────
        Set<String> loggedDates = logs.stream()
            .map(l -> l.getLogDate().toString())
            .collect(Collectors.toSet());
        List<Integer> missingDays = new ArrayList<>();
        if (cycleStart != null) {
            for (int i = 0; i < Math.min(cycleDay, cycleLength); i++) {
                String iso = cycleStart.plusDays(i).toString();
                if (!loggedDates.contains(iso)) missingDays.add(i + 1);
            }
        }

        // ── Mood/energy correlation ───────────────────────────────────────────
        // Simple: count days where both mood and energy were logged
        long bothLogged = logs.stream()
            .filter(l -> l.getMood() != null && l.getEnergy() != null)
            .count();
        int correlation = logs.isEmpty() ? 0
            : (int)((bothLogged * 100) / logs.size());

        // ── Build response ────────────────────────────────────────────────────
        InsightsResponse resp = new InsightsResponse();
        resp.setCurrentCycle(state);
        resp.setLogs(logResponses);
        resp.setMoodCounts(moodCounts);
        resp.setSymptomCounts(symptomCounts);
        resp.setBehaviourCounts(behaviourCounts);
        resp.setDischargeCounts(dischargeCounts);
        resp.setPhaseStats(phaseStats);
        resp.setMoodArray(moodArr);
        resp.setEnergyArray(energyArr);
        resp.setTotalLoggedDays(logs.size());
        resp.setLoggedDays(logs.size());
        resp.setCycleDay(cycleDay);
        resp.setCycleLength(cycleLength);
        resp.setPeriodLength(periodLen);
        resp.setCycleNumber(state.getCycleNumber());
        resp.setCycleStartDate(state.getCycleStartDate());
        resp.setNextPeriodDate(state.getNextPeriodDate());
        resp.setMissingDays(missingDays.stream().limit(10).collect(Collectors.toList()));

        // ── Previous cycle stats for 3-cycle comparison ─────────────────────
     // ── Previous cycle stats for 3-cycle comparison ─────────────────────
        // Get all completed cycles ordered by start date descending
        List<Cycle> completedCycles = cycleRepo.findRecentCompletedCycles(userId, 10)
                .stream()
             // AFTER
                .filter(c -> c.getStartDate() != null)
                // Ensure sorted by startDate DESC — most recent first
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .collect(Collectors.toList());

        System.out.println("🔍 completedCycles after sort:");
        for (int i = 0; i < completedCycles.size(); i++) {
            Cycle c = completedCycles.get(i);
            System.out.println("  [" + i + "] id=" + c.getId()
                + " start=" + c.getStartDate()
                + " length=" + c.getCycleLength());
        }

        // index 0 = most recent completed (1 cycle ago) → previousCycle2
        // index 1 = second most recent (2 cycles ago) → previousCycle1
      

        System.out.println("🔍 completedCycles count=" + completedCycles.size());
        completedCycles.forEach(c -> System.out.println(
            "  cycle id=" + c.getId() 
            + " number=" + c.getCycleNumber()
            + " start=" + c.getStartDate() 
            + " end=" + c.getEndDate()
            + " length=" + c.getCycleLength()));

        // previousCycle2 = most recent completed (1 cycle ago)
        // previousCycle1 = second most recent (2 cycles ago)
        System.out.println("🔍 getInsights completedCycles size=" + completedCycles.size());
        for (Cycle c : completedCycles) {
            System.out.println("  → id=" + c.getId()
                + " start=" + c.getStartDate()
                + " end=" + c.getEndDate()
                + " cycleLength=" + c.getCycleLength()
                + " userId=" + c.getUserId());
        }
     // completedCycles is sorted most-recent-first.
        // index 0 = last completed cycle → shown as "Now"
        // index 1 = 2 completed cycles ago → shown as "Cycle 2"
        // index 2 = 3 completed cycles ago → shown as "Cycle 1"
        if (completedCycles.size() >= 1) {
            resp.setNowCycleStats(computeCycleStatsByCycle(completedCycles.get(0)));
        }
        if (completedCycles.size() >= 2) {
            resp.setPreviousCycle2(computeCycleStatsByCycle(completedCycles.get(1)));
        }
        if (completedCycles.size() >= 3) {
            resp.setPreviousCycle1(computeCycleStatsByCycle(completedCycles.get(2)));
        }

        // ── Prakriti & PCOS results for Insights header ──────────────────────
        assessRepo.findByUserIdAndAssessmentType(userId, "PRAKRITI")
            .ifPresent(a -> resp.setPrakritiResult(a.getResultType()));
        assessRepo.findByUserIdAndAssessmentType(userId, "PCOS")
            .ifPresent(a -> resp.setPcosResult(a.getResultType()));

        return resp;
    }
    // ═════════════════════════════════════════════════════════════════════════
    //  Cycle state
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CycleStateResponse getCycleState(Long userId) {
        LocalDate today = LocalDate.now();

        // Load user settings for defaults
     // Load user settings for defaults
        CycleSettings settings  = settingsRepo.findByUserId(userId).orElse(null);
        int defaultCycleLen     = settings != null ? settings.getCycleLength()  : 28;
        int defaultPeriodLen    = settings != null ? settings.getPeriodLength() : 5;

        Cycle activeCycle = cycleRepo.findActiveCycleByUserId(userId).orElse(null);

        if (activeCycle == null || activeCycle.getStartDate() == null) {
            return buildStateWithHistoricalPattern(userId, today, defaultCycleLen, defaultPeriodLen);
        }
        LocalDate cycleStart = activeCycle.getStartDate();
     // Count period marks per cycle using all period marks for this user
        // Group them by which cycle's date range they fall into
     // Count period marks directly per cycle using date windows
        List<Cycle> allUserCycles = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId)
            .stream()
            .filter(c -> c.getStartDate() != null)
            .sorted(Comparator.comparing(Cycle::getStartDate))
            .collect(Collectors.toList());
     // ── Period length calculation ──────────────────────────────────────
     // Use actual cycle start date — no overlap with previous cycle
        LocalDate activeWStart = cycleStart;
        LocalDate activeWEnd = cycleStart.plusDays(10);
        long activeMarkCount = markRepo.countPeriodMarksByUserIdAndDateRange(
            userId, activeWStart, activeWEnd);
     // Only use actual marked count — do NOT fallback to defaultPeriodLen
        // defaultPeriodLen fallback pollutes pMin/pMax with fake data
        int markedPeriodLength = (activeMarkCount > 0 && activeMarkCount <= 15)
            ? (int) activeMarkCount : 0;
        int periodLength = markedPeriodLength > 0 ? markedPeriodLength : defaultPeriodLen;

        System.out.println("🔍 active cycle period marks=" + activeMarkCount
            + " markedPeriodLength=" + markedPeriodLength);

        // Get period lengths from historical cycles only (endDate != null = closed)
        List<Integer> historicalPeriodLengths = new ArrayList<>();
        for (int ci = 0; ci < allUserCycles.size(); ci++) {
            Cycle c = allUserCycles.get(ci);
            if (c.getEndDate() == null) continue; // skip active cycle

            // Use actual cycle date range — from start to end date
            // This prevents windows overlapping between adjacent cycles
         // Tight 10-day window — same as historicalOnly branch
            // prevents overlap between adjacent cycle windows
            LocalDate wStart = c.getStartDate();
            LocalDate wEnd = c.getStartDate().plusDays(10);

            long count = markRepo.countPeriodMarksByUserIdAndDateRange(userId, wStart, wEnd);
            System.out.println("🔍 historical cycle id=" + c.getId()
                + " start=" + c.getStartDate()
                + " end=" + wEnd + " count=" + count);
            if (count > 0 && count <= 15) {
                historicalPeriodLengths.add((int) count);
            }
        }
          

     // Include active cycle's marked period length in the range calculation
        // This ensures current month is part of min/max/variation
     // pMin/pMax from historical only — active cycle excluded if no marks yet
        // Adding markedPeriodLength=0 or defaultPeriodLen would corrupt the range
        List<Integer> allPeriodLengths = new ArrayList<>(historicalPeriodLengths);
        if (markedPeriodLength > 0) {
            allPeriodLengths.add(markedPeriodLength);
        }

        int pMin = allPeriodLengths.isEmpty()
            ? 0
            : allPeriodLengths.stream().mapToInt(i -> i).min().orElse(0);
        int pMax = allPeriodLengths.isEmpty()
            ? 0
            : allPeriodLengths.stream().mapToInt(i -> i).max().orElse(0);
        int periodVariation = (pMin > 0 && pMax > 0) ? (pMax - pMin) : 0;

        System.out.println("🔍 allPeriodLengths=" + allPeriodLengths
            + " pMin=" + pMin + " pMax=" + pMax + " variation=" + periodVariation);

        System.out.println("🔍 historicalLengths=" + historicalPeriodLengths
            + " pMin=" + pMin + " pMax=" + pMax + " variation=" + periodVariation);

     // Fetch completed cycles — compute actual length from start date gaps
        // instead of stored cycleLength which may be wrong (default 28)
     // Get ALL cycles for this user sorted by start date ASC
        // including active cycle — to compute actual gaps between periods
        List<Cycle> allCyclesSortedAsc = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId)
            .stream()
            .filter(c -> c.getStartDate() != null)
            .sorted(Comparator.comparing(Cycle::getStartDate))
            .collect(Collectors.toList());

        System.out.println("🔍 allCyclesSortedAsc count=" + allCyclesSortedAsc.size());
        allCyclesSortedAsc.forEach(c -> System.out.println(
            "  id=" + c.getId()
            + " start=" + c.getStartDate()
            + " end=" + c.getEndDate()
            + " length=" + c.getCycleLength()));
        
        
        List<Integer> recentLengths = new ArrayList<>();

        // Compute gaps from newest to oldest — include active cycle as endpoint
        for (int i = allCyclesSortedAsc.size() - 1; i >= 1; i--) {
            Cycle current = allCyclesSortedAsc.get(i - 1);
            Cycle next    = allCyclesSortedAsc.get(i);
            int actualLen = (int) ChronoUnit.DAYS.between(
                current.getStartDate(), next.getStartDate());
            System.out.println("🔍 gap: " + current.getStartDate()
                + " → " + next.getStartDate() + " = " + actualLen + " days");
            if (actualLen >= 20 && actualLen <= 45) {
                recentLengths.add(actualLen);
                if (recentLengths.size() >= 3) break;
            }
        }

        System.out.println("🔍 recentLengths after primary=" + recentLengths);

        // Fallback — if gaps not found, scan forward (oldest to newest)
        if (recentLengths.size() < 2 && allCyclesSortedAsc.size() >= 2) {
            List<Integer> forwardGaps = new ArrayList<>();
            for (int i = 1; i < allCyclesSortedAsc.size(); i++) {
                Cycle prev = allCyclesSortedAsc.get(i - 1);
                Cycle curr = allCyclesSortedAsc.get(i);
                int gap = (int) ChronoUnit.DAYS.between(
                    prev.getStartDate(), curr.getStartDate());
                System.out.println("🔍 forward gap: " + prev.getStartDate()
                    + " → " + curr.getStartDate() + " = " + gap + " days");
                if (gap >= 20 && gap <= 45) {
                    forwardGaps.add(gap);
                }
            }
            if (forwardGaps.size() > recentLengths.size()) {
                // Use forward gaps, reverse so index 0 = most recent
                Collections.reverse(forwardGaps);
                recentLengths = forwardGaps.stream()
                    .limit(3)
                    .collect(Collectors.toList());
                System.out.println("🔍 recentLengths from fallback=" + recentLengths);
            }
        }
       

        System.out.println("🔍 recentLengths=" + recentLengths + " for cycle length calc");
        
        System.out.println("🔍 allCyclesSortedAsc size=" + allCyclesSortedAsc.size());
        allCyclesSortedAsc.forEach(c -> System.out.println(
            "  → id=" + c.getId() + " start=" + c.getStartDate() + " end=" + c.getEndDate()));
        System.out.println("🔍 recentLengths=" + recentLengths + " size=" + recentLengths.size());

        System.out.println("🔍 recentLengths=" + recentLengths + " for variation calc");

        int cycleLength = recentLengths.isEmpty()
        	    ? (activeCycle.getCycleLength() != null ? activeCycle.getCycleLength() : defaultCycleLen)
        	    : (int) Math.round(
                recentLengths.stream().mapToInt(i -> i).average().orElse(defaultCycleLen));
        // Cycle day (1-based) from actual cycle start date
        int cycleDay = (int) ChronoUnit.DAYS.between(cycleStart, today) + 1;
        cycleDay = Math.max(1, cycleDay);

       
        
        if (cycleDay > cycleLength) {
            return buildStateWithHistoricalPattern(userId, today, defaultCycleLen, defaultPeriodLen);
        }

        int displayCycleDay = Math.min(cycleDay, cycleLength);

        String phase    = computePhase(cycleDay, periodLength, cycleLength);
        String phaseKey = phaseToKey(phase);

     

        LocalDate nextPeriod = cycleStart.plusDays(cycleLength);
        int daysUntilNext = (int) ChronoUnit.DAYS.between(today, nextPeriod);
        // Negative means overdue — keep negative so frontend can detect it
        // Frontend will show "overdue" instead of a countdown
        System.out.println("🔍 cycleStart=" + cycleStart + " cycleLength=" + cycleLength + " nextPeriod=" + nextPeriod + " daysUntilNext=" + daysUntilNext);

        long loggedDays = logRepo.countByCycleId(activeCycle.getId());
        List<DailyLog> cycleLogs = logRepo.findByCycleIdOrderByLogDateAsc(activeCycle.getId());

        CycleStateResponse resp = new CycleStateResponse();
        resp.setCycleId(activeCycle.getId());
        resp.setCycleNumber(activeCycle.getCycleNumber());
        resp.setCycleDay(displayCycleDay);
        resp.setCycleLength(cycleLength);
        resp.setPeriodLength(markedPeriodLength); // show actual marked days in UI
        // periodLength (padded) is only used for phase computation above
        resp.setPhase(phase);
        resp.setPhaseKey(phaseKey);
        resp.setCycleStartDate(cycleStart.toString());
        resp.setNextPeriodDate(nextPeriod.toString());
        resp.setDaysUntilNext(daysUntilNext);
        resp.setLoggedDays((int) loggedDays);
        resp.setTotalCycleDays(cycleLength);
        resp.setMoodCounts(computeMoodCounts(cycleLogs));
        resp.setDischargeCounts(computeDischargeCounts(cycleLogs));
        resp.setSymptomCounts(computeSymptomCounts(cycleLogs));
        
     // Actual days elapsed in current cycle
        int actualCycleLen = (int) ChronoUnit.DAYS.between(cycleStart, today) + 1;
        resp.setActualCycleLength(Math.max(actualCycleLen, 1));
        resp.setPeriodLengthMin(pMin);
        resp.setPeriodLengthMax(pMax);
        resp.setPeriodVariation(periodVariation);

     // patternCycleCount = number of gaps we found = number of completed cycles
        resp.setPatternCycleCount(recentLengths.size());
        if (recentLengths.size() >= 1) {
            int avg = (int) Math.round(
                recentLengths.stream().mapToInt(i -> i).average().orElse(defaultCycleLen));
            resp.setAvgCycleLength(avg);
        } else {
            resp.setAvgCycleLength(0);
        }
        if (recentLengths.size() >= 2) {
            int mostRecent = recentLengths.get(0);
            List<Integer> others = recentLengths.subList(1, recentLengths.size());
            double avgOthers = others.stream().mapToInt(i -> i).average().orElse(mostRecent);
            resp.setCycleVariation(mostRecent - (int) Math.round(avgOthers));
        } else {
            resp.setCycleVariation(0);
        }

     // ── 7-day trend maps ──────────────────────────────────────────
        try {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
            List<DailyLog> recentLogs = logRepo.findRecentLogs(userId, sevenDaysAgo);

            Map<String, Integer> energyByDay = new LinkedHashMap<>();
            Map<String, Integer> sleepByDay  = new LinkedHashMap<>();
            Map<String, Integer> stressByDay = new LinkedHashMap<>();

            Map<String, Integer> energyScoreMap = new HashMap<>();
            energyScoreMap.put("depleted", 1);
            energyScoreMap.put("low",      2);
            energyScoreMap.put("steady",   3);
            energyScoreMap.put("flowing",  4);
            energyScoreMap.put("charged",  5);

            Map<String, Integer> stressScoreMap = new HashMap<>();
            stressScoreMap.put("radiant",   1); // very low stress
            stressScoreMap.put("happy",     1); // very low stress
            stressScoreMap.put("calm",      2); // low stress
            stressScoreMap.put("sensitive", 3); // moderate stress
            stressScoreMap.put("low",       3); // moderate stress
            stressScoreMap.put("tired",     4); // high stress
            stressScoreMap.put("irritable", 4); // high stress
            stressScoreMap.put("anxious",   5); // very high stress

            for (DailyLog log : recentLogs) {
                String dateKey = log.getLogDate().toString();

                // Energy score
                if (log.getEnergy() != null) {
                    int score = energyScoreMap.getOrDefault(log.getEnergy().toLowerCase(), 0);
                    if (score > 0) energyByDay.put(dateKey, score);
                }

                // Sleep score — from sleep zone chips
                // More sleep symptoms = worse sleep = lower score
                boolean hasSleepZone = log.getBodyZones().stream()
                    .anyMatch(z -> "sleep".equals(z.getZoneId())
                        && z.getChips() != null && !z.getChips().isEmpty());
                List<String> sleepChips = log.getBodyZones().stream()
                        .filter(z -> "sleep".equals(z.getZoneId()) && z.getChips() != null)
                        .flatMap(z -> z.getChips().stream())
                        .collect(Collectors.toList());

                    if (!sleepChips.isEmpty()) {
                        // Severity weights — serious symptoms count more
                        Map<String, Integer> sleepSeverity = new HashMap<>();
                        sleepSeverity.put("poor sleep",        3);
                        sleepSeverity.put("can't fall asleep", 3);
                        sleepSeverity.put("early waking",      2);
                        sleepSeverity.put("restless",          2);
                        sleepSeverity.put("vivid dreams",      1);

                        int totalSeverity = sleepChips.stream()
                            .mapToInt(chip ->
                                sleepSeverity.getOrDefault(chip.toLowerCase().trim(), 1))
                            .sum();

                        // severity 1 = score 4, 2-3 = score 3, 4-5 = score 2, 6+ = score 1
                        int sleepScore;
                        if      (totalSeverity >= 6) sleepScore = 1;
                        else if (totalSeverity >= 4) sleepScore = 2;
                        else if (totalSeverity >= 2) sleepScore = 3;
                        else                         sleepScore = 4;

                        sleepByDay.put(dateKey, sleepScore);
                        System.out.println("🔍 sleep chips=" + sleepChips
                            + " severity=" + totalSeverity + " score=" + sleepScore
                            + " date=" + dateKey);
                    } else if (log.getMood() != null || log.getEnergy() != null) {
                        // Logged but no sleep complaints = good sleep = score 5
                        sleepByDay.put(dateKey, 5);
                    }

                // Stress score — derived from mood
                if (log.getMood() != null) {
                    int score = stressScoreMap.getOrDefault(log.getMood().toLowerCase(), 0);
                    if (score > 0) stressByDay.put(dateKey, score);
                }
            }

            resp.setEnergyByDay(energyByDay);
            resp.setSleepByDay(sleepByDay);
            resp.setStressByDay(stressByDay);
        } catch (Exception e) {
            System.out.println("⚠️ 7-day trend map error: " + e.getMessage());
            resp.setEnergyByDay(new LinkedHashMap<>());
            resp.setSleepByDay(new LinkedHashMap<>());
            resp.setStressByDay(new LinkedHashMap<>());
        }
        
     // Add assessment results for CI type display
        assessRepo.findByUserIdAndAssessmentType(userId, "PCOS")
            .ifPresent(a -> resp.setPcosResult(a.getResultType()));
        assessRepo.findByUserIdAndAssessmentType(userId, "PRAKRITI")
            .ifPresent(a -> resp.setPrakritiResult(a.getResultType()));
        
        return resp;
    }
    
    /**
     * Builds the "no active/current cycle" response, but still populates
     * pattern stats (avg cycle length, cycle variation, period length
     * range, period variation) from historical cycles when at least 2
     * exist. Called both when there's no active cycle at all, and when
     * the most recent cycle has run its full expected length with
     * nothing new marked — both cases should show "mark your period"
     * for the current-cycle card while still keeping "Your Pattern"
     * populated from real history.
     */
    private CycleStateResponse buildStateWithHistoricalPattern(
            Long userId, LocalDate today, int defaultCycleLen, int defaultPeriodLen) {

        List<Cycle> historicalOnly = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId)
            .stream()
            .filter(c -> c.getStartDate() != null)
            .sorted(Comparator.comparing(Cycle::getStartDate))
            .collect(Collectors.toList());

        if (historicalOnly.size() < 2) {
            return buildDefaultState(today, defaultCycleLen, defaultPeriodLen);
        }

        CycleStateResponse partial = buildDefaultState(today, defaultCycleLen, defaultPeriodLen);
        List<Integer> gaps = new ArrayList<>();
        for (int i = historicalOnly.size() - 1; i >= 1; i--) {
            int gap = (int) ChronoUnit.DAYS.between(
                historicalOnly.get(i-1).getStartDate(),
                historicalOnly.get(i).getStartDate());
            if (gap >= 20 && gap <= 45) {
                gaps.add(gap);
                if (gaps.size() >= 3) break;
            }
        }
        if (!gaps.isEmpty()) {
            int avg = (int) Math.round(gaps.stream().mapToInt(i->i).average().orElse(defaultCycleLen));
            partial.setAvgCycleLength(avg);
            partial.setPatternCycleCount(historicalOnly.size());
            if (gaps.size() >= 2) {
                int mostRecent = gaps.get(0);
                double avgOthers = gaps.subList(1, gaps.size()).stream().mapToInt(i->i).average().orElse(mostRecent);
                partial.setCycleVariation(mostRecent - (int) Math.round(avgOthers));
            }
        }

        List<Integer> histPeriodLens = new ArrayList<>();
        for (Cycle c : historicalOnly) {
            LocalDate wS = c.getStartDate();
            LocalDate wE = c.getStartDate().plusDays(10);
            long cnt = markRepo.countPeriodMarksByUserIdAndDateRange(userId, wS, wE);
            if (cnt > 0 && cnt <= 15) histPeriodLens.add((int) cnt);
        }
        if (histPeriodLens.size() >= 2) {
            int hMin = histPeriodLens.stream().mapToInt(i->i).min().orElse(0);
            int hMax = histPeriodLens.stream().mapToInt(i->i).max().orElse(0);
            partial.setPeriodLengthMin(hMin);
            partial.setPeriodLengthMax(hMax);
            partial.setPeriodVariation(hMax - hMin);
        }

        assessRepo.findByUserIdAndAssessmentType(userId, "PCOS")
            .ifPresent(a -> partial.setPcosResult(a.getResultType()));
        assessRepo.findByUserIdAndAssessmentType(userId, "PRAKRITI")
            .ifPresent(a -> partial.setPrakritiResult(a.getResultType()));

        return partial;
    }

    
    
    public void saveMarks(SaveMarksRequest req) {
        Long userId = req.getUserId();

        for (Map.Entry<String, Integer> entry : req.getMarks().entrySet()) {
            LocalDate date     = LocalDate.parse(entry.getKey());
            int       markType = entry.getValue();

            if (markType == 0) {
                markRepo.deleteByUserIdAndMarkDate(userId, date);
            } else {
                CycleDayMark mark = markRepo.findByUserIdAndMarkDate(userId, date)
                    .orElse(new CycleDayMark(userId, null, date, markType));
                mark.setMarkType(markType);
                markRepo.save(mark);
            }
        }
        markRepo.flush();

        rebuildCycles(userId);
    }


    @Transactional(readOnly = true)
    public MarksResponse getMarksForMonth(Long userId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        List<CycleDayMark> rows = markRepo.findByUserIdAndDateRange(userId, from, to);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (CycleDayMark m : rows) {
            result.put(m.getMarkDate().toString(), m.getMarkType());
        }
        return new MarksResponse(result);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Daily log operations
    // ═════════════════════════════════════════════════════════════════════════

    public DailyLogResponse saveDailyLog(SaveDailyLogRequest req) {
        LocalDate logDate = LocalDate.parse(req.getDate());
        Long userId       = req.getUserId();

        // Resolve cycleId — use provided or look up active cycle
        Long cycleId = req.getCycleId();
        if (cycleId == null) {
            // Try to find active cycle
            Optional<Cycle> activeCycleOpt = cycleRepo.findActiveCycleByUserId(userId);
            if (activeCycleOpt.isPresent()) {
                cycleId = activeCycleOpt.get().getId();
            } else {
                // No active cycle exists — check if there's any cycle at all
                // including ones with null startDate (ghost cycles)
                List<Cycle> allCycles = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId);
                Cycle ghostCycle = allCycles.stream()
                    .filter(c -> c.getStartDate() == null && c.getEndDate() == null)
                    .findFirst()
                    .orElse(null);

                if (ghostCycle != null) {
                    // Reuse existing ghost cycle — don't create another
                    cycleId = ghostCycle.getId();
                } else {
                    // No cycle at all — create one with null startDate
                    // This allows logging even before marking a period
                    Cycle newCycle = new Cycle();
                    newCycle.setUserId(userId);
                    newCycle.setCycleNumber(cycleRepo.findMaxCycleNumberByUserId(userId)
                        .map(n -> n + 1).orElse(1));
                    newCycle.setStartDate(null); // no period marked yet
                    CycleSettings settings = settingsRepo.findByUserId(userId).orElse(null);
                    newCycle.setCycleLength(settings != null ? settings.getCycleLength() : 28);
                 // Set to 0 initially — will be updated by saveMarks ifPresent block
                    // after actual marks are saved. Using CycleSettings default (5) here
                    // is wrong because it never gets corrected for historical cycles.
                    newCycle.setPeriodLength(0);
                    Cycle saved = cycleRepo.save(newCycle);
                    cycleId = saved.getId();
                }
            }
        }

        final Long finalCycleId = cycleId;

        // Upsert
        DailyLog log = logRepo.findByUserIdAndLogDate(userId, logDate)
            .orElse(new DailyLog());

        log.setUserId(userId);
        log.setCycleId(finalCycleId);
        log.setLogDate(logDate);
        log.setDayLabel(req.getDay());
        log.setCycleDay(req.getCycleDay());
        log.setPhase(req.getPhase());
        log.setEnergy(req.getEnergy());
        log.setMood(req.getMood());
        log.setCharacterState(req.getCharacter());

        // List<String> directly — no split/join needed anymore
        log.setDischarge(req.getDischarge() != null ? req.getDischarge() : new ArrayList<>());
        log.setBehaviours(req.getBehaviours() != null ? req.getBehaviours() : new ArrayList<>());

        // Body zones — clear old, rebuild
     // Body zones — clear old, rebuild only if zones were provided
        // If zones is null it means the user skipped step 4 — keep existing zones
        if (req.getZones() != null) {
            log.getBodyZones().clear();
            req.getZones().forEach((zoneId, chips) -> {
                if (chips != null && !chips.isEmpty()) {
                    DailyLogBodyZone zone = new DailyLogBodyZone(log, zoneId, chips);
                    zone.setUserId(req.getUserId());  // set user_id explicitly
                    log.getBodyZones().add(zone);
                }
            });
        }

        DailyLog saved = logRepo.save(log);
        return toLogResponse(saved);
    }

    @Transactional(readOnly = true)
    public DailyLogResponse getLogForDate(Long userId, LocalDate date) {
        return logRepo.findByUserIdAndLogDate(userId, date)
            .map(this::toLogResponse)
            .orElse(null);
    }

   
    /**
     * Recomputes every cycle for a user from scratch, based purely on the
     * period marks that currently exist in cycle_day_marks. This is the
     * single source of truth for cycle boundaries — nothing else in this
     * service creates, closes, or reassigns a Cycle row. Because it looks
     * at ALL marks together every time, the order they were saved in never
     * affects the result: mark 5th, then 6th, then 3rd — the outcome is
     * identical to marking 3rd, 5th, 6th in that order.
     */
    private void rebuildCycles(Long userId) {
        List<CycleDayMark> periodMarks = markRepo.findAllPeriodMarksByUserId(userId)
            .stream()
            .sorted(Comparator.comparing(CycleDayMark::getMarkDate))
            .collect(Collectors.toList());

        List<Cycle> existingCycles = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId);

        if (periodMarks.isEmpty()) {
            // No period marks left at all — nothing to rebuild a cycle
            // FROM, but any existing Cycle rows are now stale and must be
            // removed, or the last active cycle stays stuck "active"
            // forever with no marks backing it up. daily_logs.cycle_id is
            // a plain Long column (no FK/cascade), so leaving those rows
            // pointed at a deleted id is harmless — they just won't be
            // picked up by findByCycleIdOrderByLogDateAsc anymore.
            if (!existingCycles.isEmpty()) {
                System.out.println("🗑️ rebuildCycles: no period marks remain for user "
                    + userId + " — removing " + existingCycles.size() + " stale cycle(s)");
                cycleRepo.deleteAll(existingCycles);
                cycleRepo.flush();
            }
            return;
        }

        // 1. Group marks into blocks — consecutive dates (gap <= MERGE_GAP_DAYS)
        //    belong to the same real period, regardless of what order they
        //    were saved in, since we're sorting by date first.
        List<List<LocalDate>> blocks = new ArrayList<>();
        List<LocalDate> current = new ArrayList<>();
        LocalDate lastDate = null;
        for (CycleDayMark m : periodMarks) {
            LocalDate d = m.getMarkDate();
            if (lastDate != null && ChronoUnit.DAYS.between(lastDate, d) > MERGE_GAP_DAYS) {
                blocks.add(current);
                current = new ArrayList<>();
            }
            current.add(d);
            lastDate = d;
        }
        blocks.add(current);

        CycleSettings settings = settingsRepo.findByUserId(userId).orElse(null);
        int defaultCycleLen = settings != null ? settings.getCycleLength() : 28;

        // 2. For each block, reuse an existing Cycle row if one already
        //    exists near this block's start date (preserves the row's ID,
        //    so daily_logs foreign keys stay valid across a rebuild).
        //    Otherwise create a fresh row.
        List<Cycle> rebuilt = new ArrayList<>();
        for (List<LocalDate> block : blocks) {
            LocalDate blockStart = block.get(0);

            Cycle match = existingCycles.stream()
                .filter(c -> c.getStartDate() != null)
                .filter(c -> Math.abs(ChronoUnit.DAYS.between(c.getStartDate(), blockStart)) <= MERGE_GAP_DAYS)
                .min(Comparator.comparingLong(c ->
                    Math.abs(ChronoUnit.DAYS.between(c.getStartDate(), blockStart))))
                .orElse(null);

            Cycle cycle = (match != null) ? match : new Cycle();
            cycle.setUserId(userId);
            cycle.setStartDate(blockStart);
            cycle.setPeriodLength(block.size()); // always the real, current count
            rebuilt.add(cycle);
            if (match != null) existingCycles.remove(match);
        }

        // 3. Close every cycle except the most recent one — the newest
        //    block is always the active cycle. No two cycles can ever
        //    overlap: endDate is always the day before the next start.
        for (int i = 0; i < rebuilt.size(); i++) {
            Cycle c = rebuilt.get(i);
            if (i < rebuilt.size() - 1) {
                LocalDate nextStart = rebuilt.get(i + 1).getStartDate();
                int gap = (int) ChronoUnit.DAYS.between(c.getStartDate(), nextStart);
                c.setEndDate(nextStart.minusDays(1));
                c.setCycleLength(gap >= MIN_CYCLE_GAP && gap <= MAX_CYCLE_GAP ? gap : defaultCycleLen);
            } else {
                c.setEndDate(null);
                c.setCycleLength(c.getCycleLength() != null ? c.getCycleLength() : defaultCycleLen);
            }
            c.setCycleNumber(i + 1); // chronological numbering, always
            cycleRepo.save(c);
        }
        cycleRepo.flush();

        // 4. Reassign every period mark to its block's (possibly reused,
        //    possibly new) cycle ID.
        for (int i = 0; i < blocks.size(); i++) {
            Long cycleId = rebuilt.get(i).getId();
            for (LocalDate d : blocks.get(i)) {
                markRepo.findByUserIdAndMarkDate(userId, d).ifPresent(m -> {
                    if (!cycleId.equals(m.getCycleId())) {
                        m.setCycleId(cycleId);
                        markRepo.save(m);
                    }
                });
            }
        }

        // 5. Non-period marks (spotting, discharge, etc.) get reassigned
        //    to whichever cycle's date range they now fall inside.
        List<CycleDayMark> nonPeriodMarks = markRepo.findByUserIdAndDateRange(
                userId, rebuilt.get(0).getStartDate(), LocalDate.now().plusDays(1))
            .stream()
            .filter(m -> m.getMarkType() != 1)
            .collect(Collectors.toList());
        for (CycleDayMark m : nonPeriodMarks) {
            Cycle owner = rebuilt.stream()
                .filter(c -> !c.getStartDate().isAfter(m.getMarkDate()))
                .filter(c -> c.getEndDate() == null || !c.getEndDate().isBefore(m.getMarkDate()))
                .max(Comparator.comparing(Cycle::getStartDate))
                .orElse(null);
            if (owner != null && !owner.getId().equals(m.getCycleId())) {
                m.setCycleId(owner.getId());
                markRepo.save(m);
            }
        }

        // 6. Any existing Cycle rows that weren't matched to any block are
        //    now obsolete (their marks were merged elsewhere, or deleted
        //    entirely). Move any daily_logs still pointing at them to the
        //    nearest surviving cycle by date, then remove the row.
        for (Cycle orphan : existingCycles) {
            List<DailyLog> orphanedLogs = logRepo.findByCycleIdOrderByLogDateAsc(orphan.getId());
            for (DailyLog log : orphanedLogs) {
                Cycle bestMatch = rebuilt.stream()
                    .filter(c -> !c.getStartDate().isAfter(log.getLogDate()))
                    .max(Comparator.comparing(Cycle::getStartDate))
                    .orElse(rebuilt.get(0));
                log.setCycleId(bestMatch.getId());
                logRepo.save(log);
            }
            cycleRepo.delete(orphan);
        }
        cycleRepo.flush();
    }
   
    
    
    
    
   
    
 
  
   
   
    // ═════════════════════════════════════════════════════════════════════════
    //  Phase computation
    // ═════════════════════════════════════════════════════════════════════════

    private String computePhase(int cycleDay, int periodLength, int cycleLength) {
        // Based on clinical table provided:
        // Menstrual:  Always Days 1–5
        // Follicular: Days 6 to (cycleLength - 15)
        // Ovulation:  Days (cycleLength - 14) to (cycleLength - 12)
        // Luteal:     Days (cycleLength - 11) to cycleLength
        //
        // Examples:
        // 28-day: Follicular 6–13, Ovulation 14–16, Luteal 17–28
        // 31-day: Follicular 6–16, Ovulation 17–19, Luteal 20–31
        // 33-day: Follicular 6–18, Ovulation 19–21, Luteal 22–33

        int menstrualEnd  = 5;
        int follicularEnd = cycleLength - 15;
        int ovulationEnd  = cycleLength - 12;

        // Safety for very short cycles (below 25 days)
        if (follicularEnd <= menstrualEnd) {
            follicularEnd = menstrualEnd + 1;
        }
        if (ovulationEnd <= follicularEnd) {
            ovulationEnd = follicularEnd + 3;
        }

        System.out.println("🔍 computePhase: cycleDay=" + cycleDay
            + " cycleLength=" + cycleLength
            + " menstrualEnd=" + menstrualEnd
            + " follicularEnd=" + follicularEnd
            + " ovulationEnd=" + ovulationEnd);

        if (cycleDay <= menstrualEnd)  return "Menstrual";
        if (cycleDay <= follicularEnd) return "Follicular";
        if (cycleDay <= ovulationEnd)  return "Ovulation";
        return "Luteal";
    }

    private String phaseToKey(String phase) {
        return switch (phase) {
            case "Menstrual"  -> "menstrual";
            case "Follicular" -> "follicular";
            case "Ovulation"  -> "ovulation";
            default           -> "luteal";
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Stats aggregation — all cycle-scoped
    // ═════════════════════════════════════════════════════════════════════════

    private Map<String, Long> computeMoodCounts(List<DailyLog> logs) {
        return logs.stream()
            .filter(l -> l.getMood() != null)
            .collect(Collectors.groupingBy(DailyLog::getMood, Collectors.counting()));
    }

    private Map<String, Long> computeDischargeCounts(List<DailyLog> logs) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DailyLog log : logs) {
            if (log.getDischarge() == null) continue;
            // discharge is now List<String> — no split needed
            for (String id : log.getDischarge()) {
                counts.merge(id, 1L, Long::sum);
            }
        }
        return counts;
    }
    
    /**
     * Computes summary stats for a specific past cycle by cycle_number.
     * Returns null if that cycle doesn't exist (e.g. user only has 1 cycle).
     */
    
    private CyclePreviousStats computeCycleStats(Long userId, int cycleNumber) {
        Optional<Cycle> cycleOpt = cycleRepo.findByUserIdAndCycleNumber(userId, cycleNumber);
        if (cycleOpt.isEmpty()) return null;

        Cycle cycle = cycleOpt.get();
        List<DailyLog> logs = logRepo.findByCycleIdOrderByLogDateAsc(cycle.getId());

        List<Integer> moods   = new ArrayList<>();
        List<Integer> energies = new ArrayList<>();
        Set<String> symptomDays = new HashSet<>();

        for (DailyLog log : logs) {
            if (log.getMood() != null) {
                moods.add(MOOD_SCORE_MAP.getOrDefault(log.getMood(), 50));
            }
            if (log.getEnergy() != null) {
                energies.add(ENERGY_SCORE_MAP.getOrDefault(log.getEnergy(), 50));
            }
            boolean hasSymptom = log.getBodyZones().stream()
                .anyMatch(z -> z.getChips() != null && !z.getChips().isEmpty());
            if (hasSymptom) {
                symptomDays.add(log.getLogDate().toString());
            }
        }

        CyclePreviousStats stats = new CyclePreviousStats();
        stats.setCycleLength(cycle.getCycleLength()  != null ? cycle.getCycleLength()  : 28);
        stats.setPeriodLength(cycle.getPeriodLength() != null ? cycle.getPeriodLength() : 5);
        stats.setAvgMood(moods.isEmpty() ? 0
            : moods.stream().mapToInt(i -> i).sum() / moods.size());
        stats.setAvgEnergy(energies.isEmpty() ? 0
            : energies.stream().mapToInt(i -> i).sum() / energies.size());
        stats.setSymptomDays(symptomDays.size());
        stats.setLoggedDays(logs.size());
        return stats;
    }

    private CyclePreviousStats computeCycleStatsByCycle(Cycle cycle) {
        List<DailyLog> logs = logRepo.findByCycleIdOrderByLogDateAsc(cycle.getId());

        List<Integer> moods    = new ArrayList<>();
        List<Integer> energies = new ArrayList<>();
        Set<String>   symptomDays = new HashSet<>();

        for (DailyLog log : logs) {
            if (log.getMood() != null) {
                moods.add(MOOD_SCORE_MAP.getOrDefault(log.getMood(), 50));
            }
            if (log.getEnergy() != null) {
                energies.add(ENERGY_SCORE_MAP.getOrDefault(log.getEnergy(), 50));
            }
            boolean hasSymptom = log.getBodyZones().stream()
                .anyMatch(z -> z.getChips() != null && !z.getChips().isEmpty());
            if (hasSymptom) {
                symptomDays.add(log.getLogDate().toString());
            }
        }

     // Use actual cycle length from database (already fixed by SQL cleanup)
        int storedLen = cycle.getCycleLength() != null ? cycle.getCycleLength() : 0;

     // Count actual period marks for this cycle instead of using stored default
     // Count actual period marks for this cycle
        // Search from 5 days BEFORE start date to catch marks saved before
        // the cycle start was corrected via SQL
     // Use actual cycle boundaries to prevent overlap with adjacent cycles
        LocalDate searchFrom = cycle.getStartDate();
        LocalDate searchTo = cycle.getEndDate() != null 
            ? cycle.getEndDate() 
            : cycle.getStartDate().plusDays(10);
        long actualPeriodMarks = markRepo.countPeriodMarksByUserIdAndDateRange(
                cycle.getUserId(),
                searchFrom,
                searchTo
            );
            System.out.println("🔍 cycle id=" + cycle.getId()
                + " start=" + cycle.getStartDate()
                + " searchFrom=" + searchFrom
                + " searchTo=" + searchTo
                + " periodMarks=" + actualPeriodMarks);
            // Use actual marks count — if 0 marks found, return 0 not default 5
            // This prevents showing "5d" when user never logged period days
            int actualPeriodLength = actualPeriodMarks > 0 && actualPeriodMarks <= 15
                ? (int) actualPeriodMarks
                : 0; // 0 means no data — frontend will show "—"

            CyclePreviousStats stats = new CyclePreviousStats();
            stats.setCycleLength(storedLen);
            stats.setPeriodLength(actualPeriodLength);
            stats.setAvgMood(moods.isEmpty() ? 0
                : moods.stream().mapToInt(i -> i).sum() / moods.size());
            stats.setAvgEnergy(energies.isEmpty() ? 0
                : energies.stream().mapToInt(i -> i).sum() / energies.size());
            stats.setSymptomDays(symptomDays.size());
            stats.setLoggedDays(logs.size());
            stats.setStartDate(cycle.getStartDate() != null ? cycle.getStartDate().toString() : null);
            return stats;
        }
    
    private Map<String, Long> computeSymptomCounts(List<DailyLog> logs) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DailyLog log : logs) {
            for (DailyLogBodyZone zone : log.getBodyZones()) {
                if (zone.getChips() == null) continue;
                // chips is now List<String> — no split needed
                for (String chip : zone.getChips()) {
                    counts.merge(chip, 1L, Long::sum);
                }
            }
        }
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mapping helpers
    // ═════════════════════════════════════════════════════════════════════════

    private DailyLogResponse toLogResponse(DailyLog log) {
        DailyLogResponse r = new DailyLogResponse();
        r.setId(log.getId());
        r.setCycleId(log.getCycleId());
        r.setLogDate(log.getLogDate().toString());
        r.setDayLabel(log.getDayLabel());
        r.setCycleDay(log.getCycleDay());
        r.setPhase(log.getPhase());
        r.setEnergy(log.getEnergy());
        r.setMood(log.getMood());
        r.setCharacterState(log.getCharacterState());
        // Already List<String> — set directly
        r.setDischarge(log.getDischarge() != null ? log.getDischarge() : new ArrayList<>());
        r.setBehaviours(log.getBehaviours() != null ? log.getBehaviours() : new ArrayList<>());
        // Rebuild zones map
        Map<String, List<String>> zonesMap = new LinkedHashMap<>();
        for (DailyLogBodyZone zone : log.getBodyZones()) {
            zonesMap.put(zone.getZoneId(), zone.getChips() != null ? zone.getChips() : new ArrayList<>());
        }
        r.setZones(zonesMap);
        return r;
    }
    
  

    private CycleStateResponse buildDefaultState(LocalDate today, int cycleLen, int periodLen) {
        CycleStateResponse resp = new CycleStateResponse();
        resp.setCycleId(0);
        resp.setCycleNumber(0);
        resp.setCycleDay(0);
        resp.setCycleLength(cycleLen);
        resp.setPeriodLength(periodLen);
        resp.setPhase("None");
        resp.setPhaseKey("none");
        resp.setCycleStartDate("");
        resp.setNextPeriodDate("");
        resp.setDaysUntilNext(0);
        resp.setLoggedDays(0);
        resp.setTotalCycleDays(cycleLen);
        resp.setMoodCounts(new LinkedHashMap<>());
        resp.setDischargeCounts(new LinkedHashMap<>());
        resp.setSymptomCounts(new LinkedHashMap<>());
        resp.setPeriodLengthMin(0);
        resp.setPeriodLengthMax(0);
        resp.setAvgCycleLength(0);
        resp.setPatternCycleCount(0);
        resp.setCycleVariation(0);
        resp.setPeriodVariation(0);
        resp.setActualCycleLength(0);
        resp.setEnergyByDay(new LinkedHashMap<>());
        resp.setSleepByDay(new LinkedHashMap<>());
        resp.setStressByDay(new LinkedHashMap<>());
        return resp;
    }
}