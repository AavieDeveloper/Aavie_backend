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
import java.util.HashSet;

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
        
        private int avgCycleLength;
        private int patternCycleCount;
        private int cycleVariation;
        public int  getAvgCycleLength()           { return avgCycleLength; }
        public void setAvgCycleLength(int v)      { this.avgCycleLength = v; }
        public int  getPatternCycleCount()        { return patternCycleCount; }
        public void setPatternCycleCount(int v)   { this.patternCycleCount = v; }
        public int  getCycleVariation()           { return cycleVariation; }
        public void setCycleVariation(int v)      { this.cycleVariation = v; }
    }

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
        
        private CyclePreviousStats previousCycle1; // 2 cycles ago
        private CyclePreviousStats previousCycle2; // 1 cycle ago

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
        // Skip the active cycle (endDate == null), take the 2 most recent closed ones
        List<Cycle> completedCycles = cycleRepo.findRecentCompletedCycles(userId, 10)
                .stream()
                .filter(c -> c.getStartDate() != null && c.getCycleLength() != null && c.getCycleLength() > 0)
                .collect(Collectors.toList());
            // Already ordered by startDate DESC from the repository query — no re-sort needed

        // previousCycle2 = most recent completed (1 cycle ago)
        // previousCycle1 = second most recent completed (2 cycles ago)
        if (completedCycles.size() >= 1) {
            resp.setPreviousCycle2(computeCycleStatsByCycle(completedCycles.get(0)));
        }
        if (completedCycles.size() >= 2) {
            resp.setPreviousCycle1(computeCycleStatsByCycle(completedCycles.get(1)));
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
            return buildDefaultState(today, defaultCycleLen, defaultPeriodLen);
        }

        LocalDate cycleStart = activeCycle.getStartDate();

        // If cycle started more than 90 days ago, it's stale — treat as no active cycle
        if (cycleStart != null && ChronoUnit.DAYS.between(cycleStart, today) > 90) {
            return buildDefaultState(today, defaultCycleLen, defaultPeriodLen);
        }
        int periodLength     = activeCycle.getPeriodLength() != null
                               ? activeCycle.getPeriodLength() : defaultPeriodLen;

        // Fetch up to 3 completed cycles for average + variation
        List<Cycle> recentCycles = cycleRepo.findRecentCompletedCycles(userId, 3);
        List<Integer> recentLengths = recentCycles.stream()
            .filter(c -> c.getCycleLength() != null && c.getCycleLength() > 0)
            .map(Cycle::getCycleLength)
            .collect(Collectors.toList());

        int cycleLength = recentLengths.isEmpty()
            ? (activeCycle.getCycleLength() != null ? activeCycle.getCycleLength() : defaultCycleLen)
            : (int) Math.round(recentLengths.stream().mapToInt(i -> i).average().orElse(defaultCycleLen));

        // Cycle day (1-based)
        int cycleDay = (int) ChronoUnit.DAYS.between(cycleStart, today) + 1;
        cycleDay = Math.max(1, Math.min(cycleDay, cycleLength));

        String phase    = computePhase(cycleDay, periodLength, cycleLength);
        String phaseKey = phaseToKey(phase);

        // Days until next period
        int daysUntilNext    = cycleLength - cycleDay + 1;
        LocalDate nextPeriod = today.plusDays(daysUntilNext);

        // Logged days this cycle
        long loggedDays = logRepo.countByCycleId(activeCycle.getId());

        // Stats
        List<DailyLog> cycleLogs = logRepo.findByCycleIdOrderByLogDateAsc(activeCycle.getId());

        CycleStateResponse resp = new CycleStateResponse();
        resp.setCycleId(activeCycle.getId());
        resp.setCycleNumber(activeCycle.getCycleNumber());
        resp.setCycleDay(cycleDay);
        resp.setCycleLength(cycleLength);
        resp.setPeriodLength(periodLength);
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

     // Pattern data — avg cycle length, variation, cycle count
        resp.setPatternCycleCount(recentLengths.size());

        if (recentLengths.size() >= 1) {
            // Average across all completed cycles
            int avg = (int) Math.round(
                recentLengths.stream().mapToInt(i -> i).average().orElse(defaultCycleLen));
            resp.setAvgCycleLength(avg);
        } else {
            resp.setAvgCycleLength(0);
        }

        if (recentLengths.size() >= 2) {
            // mostRecent = get(0) because query is now ORDER BY startDate DESC
            int mostRecent = recentLengths.get(0);
            // Average of the OTHER cycles (excluding most recent) for fair comparison
            List<Integer> others = recentLengths.subList(1, recentLengths.size());
            double avgOthers = others.stream().mapToInt(i -> i).average().orElse(mostRecent);
            resp.setCycleVariation(mostRecent - (int) Math.round(avgOthers));
        } else {
            resp.setCycleVariation(0);
        }

        return resp;
    }

    
    
    // ═════════════════════════════════════════════════════════════════════════
    //  Mark operations
    // ═════════════════════════════════════════════════════════════════════════

    public void saveMarks(SaveMarksRequest req) {
        Long userId = req.getUserId();

        // Find the earliest period mark (type=1) in this batch — only process
        // cycle creation once for the earliest date, not for every day
     // Collect all unique period start dates (type=1) in ascending order
        // Each distinct period date may represent a different past cycle
        List<LocalDate> periodDates = req.getMarks().entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(e -> LocalDate.parse(e.getKey()))
            .sorted()
            .distinct()
            .collect(Collectors.toList());

        // Process only the earliest date per batch — the 5 consecutive days
        // all share the same cycle, so we only need to call handlePeriodMark once
        // with the first day of that period
        if (!periodDates.isEmpty()) {
            handlePeriodMark(userId, periodDates.get(0));
        }

     
     // Now save each individual mark
        for (Map.Entry<String, Integer> entry : req.getMarks().entrySet()) {
            LocalDate date     = LocalDate.parse(entry.getKey());
            int       markType = entry.getValue();

            if (markType == 0) {
                markRepo.deleteByUserIdAndMarkDate(userId, date);
                // If no period marks remain, clear the active cycle's start date
                // so the user can re-mark freely
                boolean anyPeriodLeft = !markRepo.findAllPeriodMarksByUserId(userId).isEmpty();
                if (!anyPeriodLeft) {
                    cycleRepo.findActiveCycleByUserId(userId).ifPresent(c -> {
                        c.setStartDate(null);
                        cycleRepo.save(c);
                    });
                }
                continue;
            }

            // Only resolve/create a cycle for period marks (type 1)
            // Non-period marks (spotting, discharge, bleeding, other)
            // should NOT create a cycle if one doesn't exist yet
            Long cycleId = resolveCycleIdForDate(userId, date, markType);
         // cycleId can be null for non-period marks saved before any period
         // has been marked — that's fine, we still persist the mark

         CycleDayMark mark = markRepo.findByUserIdAndMarkDate(userId, date)
             .orElse(new CycleDayMark(userId, cycleId, date, markType));
         mark.setMarkType(markType);
         mark.setCycleId(cycleId);
         markRepo.save(mark);
        }
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
            cycleId = cycleRepo.findActiveCycleByUserId(userId)
                .map(Cycle::getId)
                .orElseThrow(() -> new RuntimeException(
                    "No active cycle found for user. Please mark a period start first."));
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

    // ═════════════════════════════════════════════════════════════════════════
    //  Cycle management helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Called when a period mark (type=1) is saved.
     * Logic:
     *  - If no cycles exist → create Cycle 1 with this start date
     *  - If there's an active cycle and this date is >= 21 days after its start
     *    → close the active cycle, create the next one
     *  - If this date is within 7 days of the active cycle start
     *    → it's the same period, just update the start date if earlier
     */
    private void handlePeriodMark(Long userId, LocalDate periodDate) {
        LocalDate today = LocalDate.now();
        Optional<Cycle> activeCycleOpt = cycleRepo.findActiveCycleByUserId(userId);

        if (activeCycleOpt.isEmpty()) {
            if (periodDate.isBefore(today.minusDays(20))) {
                createHistoricalCycle(userId, periodDate);
                recalculateHistoricalCycleLengths(userId);
            } else {
                createNewCycle(userId, periodDate);
            }
            return;
        }

        Cycle activeCycle = activeCycleOpt.get();

        if (activeCycle.getStartDate() == null) {
            // Active cycle exists but has no start date yet
            if (periodDate.isBefore(today.minusDays(20))) {
                // Past period — save as historical, don't touch active cycle
                createHistoricalCycle(userId, periodDate);
            } else {
                activeCycle.setStartDate(periodDate);
                cycleRepo.save(activeCycle);
            }
            return;
        }

        long daysSinceStart = ChronoUnit.DAYS.between(activeCycle.getStartDate(), periodDate);
        long daysFromToday  = ChronoUnit.DAYS.between(periodDate, today);

        if (daysFromToday > 20) {
            // This is a past period mark — save as historical closed cycle
            // Do NOT disturb the current active cycle
            createHistoricalCycle(userId, periodDate);
            // Recalculate lengths for all historical cycles now that
            // we may have a new reference point
            recalculateHistoricalCycleLengths(userId);
        } else if (daysSinceStart >= 21) {
            // New period that is recent — close current, open next
            activeCycle.setEndDate(periodDate.minusDays(1));
            activeCycle.setCycleLength((int) daysSinceStart);
            cycleRepo.save(activeCycle);
            createNewCycle(userId, periodDate);
            // Recalculate all historical cycle lengths with new reference
            recalculateHistoricalCycleLengths(userId);
        } else {
            // Same cycle window — user correcting the start date
            activeCycle.setStartDate(periodDate);
            cycleRepo.save(activeCycle);
        }
    }

    private Cycle createNewCycle(Long userId, LocalDate startDate) {
        CycleSettings settings = settingsRepo.findByUserId(userId).orElse(null);
        int nextNumber = cycleRepo.findMaxCycleNumberByUserId(userId)
            .map(n -> n + 1).orElse(1);

        int cycleLen  = settings != null ? settings.getCycleLength()  : 28;
        int periodLen = settings != null ? settings.getPeriodLength() : 5;

        Cycle cycle = new Cycle();
        cycle.setUserId(userId);
        cycle.setCycleNumber(nextNumber);
        cycle.setStartDate(startDate);   // ← always set start date
        cycle.setCycleLength(cycleLen);
        cycle.setPeriodLength(periodLen);
        // endDate stays null — this is the active cycle

        Cycle saved = cycleRepo.save(cycle);

        // Auto-create CycleSettings if none exist
        if (settings == null) {
            CycleSettings newSettings = new CycleSettings();
            newSettings.setUserId(userId);
            newSettings.setCycleLength(28);
            newSettings.setPeriodLength(5);
            settingsRepo.save(newSettings);
        }

        // Backfill any orphaned marks (saved before a period was ever marked)
        List<CycleDayMark> orphaned = markRepo.findByUserIdAndCycleIdIsNull(userId);
        for (CycleDayMark orphan : orphaned) {
            orphan.setCycleId(saved.getId());
            markRepo.save(orphan);
        }

        return saved;
    }

    
    /**
     * Creates a completed historical cycle for a past period date.
     * Sets both startDate and endDate so it is treated as closed.
     * The current active cycle is NOT affected.
     */
    private void createHistoricalCycle(Long userId, LocalDate startDate) {
        // Check if a cycle already exists for this date — avoid duplicates
        Optional<Cycle> existing = cycleRepo.findCycleForDate(userId, startDate);
        if (existing.isPresent()) {
            Cycle c = existing.get();
            if (!startDate.equals(c.getStartDate())) {
                c.setStartDate(startDate);
                cycleRepo.save(c);
            }
            return;
        }

        CycleSettings settings = settingsRepo.findByUserId(userId).orElse(null);
        int defaultCycleLen = settings != null ? settings.getCycleLength()  : 28;
        int periodLen       = settings != null ? settings.getPeriodLength() : 5;

        // Calculate real cycle length = days between this period start
        // and the next known period start (or active cycle start)
        // Look for the next cycle that starts after this date
        int realCycleLen = defaultCycleLen;

        // Find all cycles for this user ordered by start date
        List<Cycle> allCycles = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId);

        // Find the next cycle start date after this startDate
        LocalDate nextStart = allCycles.stream()
            .filter(c -> c.getStartDate() != null && c.getStartDate().isAfter(startDate))
            .map(Cycle::getStartDate)
            .min(LocalDate::compareTo)
            .orElse(null);

        if (nextStart != null) {
            int daysBetween = (int) ChronoUnit.DAYS.between(startDate, nextStart);
            // Sanity check — only use if it's a plausible cycle length
            if (daysBetween >= 20 && daysBetween <= 45) {
                realCycleLen = daysBetween;
            }
        }

        int nextNumber = cycleRepo.findMaxCycleNumberByUserId(userId)
            .map(n -> n + 1).orElse(1);

        Cycle historical = new Cycle();
        historical.setUserId(userId);
        historical.setCycleNumber(nextNumber);
        historical.setStartDate(startDate);
        historical.setEndDate(startDate.plusDays(realCycleLen - 1));
        historical.setCycleLength(realCycleLen);
        historical.setPeriodLength(periodLen);

        cycleRepo.save(historical);
    }
    
    
    /**
     * After adding a new historical cycle, recalculate all closed cycle
     * lengths based on actual gaps between consecutive period start dates.
     */
    private void recalculateHistoricalCycleLengths(Long userId) {
        // Get all cycles ordered by start date ascending
        List<Cycle> allCycles = cycleRepo.findByUserIdOrderByCycleNumberAsc(userId)
            .stream()
            .filter(c -> c.getStartDate() != null)
            .sorted(Comparator.comparing(Cycle::getStartDate))
            .collect(Collectors.toList());

        for (int i = 0; i < allCycles.size() - 1; i++) {
            Cycle current = allCycles.get(i);
            Cycle next    = allCycles.get(i + 1);

            if (current.getEndDate() == null) continue; // skip active cycle

            int daysBetween = (int) ChronoUnit.DAYS.between(
                current.getStartDate(), next.getStartDate());

            if (daysBetween >= 20 && daysBetween <= 45) {
                current.setCycleLength(daysBetween);
                current.setEndDate(next.getStartDate().minusDays(1));
                cycleRepo.save(current);
            }
        }
    }
    
    /**
     * Finds the cycle_id that a given date belongs to.
     *
     * markType == 1 (Period): creates a new cycle if none exists
     * markType != 1 (Spotting/Discharge/Bleeding/Other): returns null
     *   if no cycle exists — caller must skip saving the mark
     */
    private Long resolveCycleIdForDate(Long userId, LocalDate date, int markType) {
        // First try to find cycle by date range
        Optional<Cycle> byDate = cycleRepo.findCycleForDate(userId, date);
        if (byDate.isPresent()) return byDate.get().getId();

        // Then try active cycle
        Optional<Cycle> active = cycleRepo.findActiveCycleByUserId(userId);
        if (active.isPresent()) {
            Cycle c = active.get();
            // Only set startDate if this is a period mark (type=1)
            // Non-period marks must NEVER set startDate — that would
            // make getCycleState think a period has started
            if (c.getStartDate() == null && markType == 1) {
                c.setStartDate(date);
                cycleRepo.save(c);
            }
            return c.getId();
        }

        // No cycle exists
        if (markType == 1) {
            return createNewCycle(userId, date).getId();
        }

        // Non-period mark with no existing cycle — return null
        return null;
    }
    // ═════════════════════════════════════════════════════════════════════════
    //  Phase computation
    // ═════════════════════════════════════════════════════════════════════════

    private String computePhase(int cycleDay, int periodLength, int cycleLength) {
        int menstrualEnd  = periodLength;
        int follicularEnd = cycleLength / 2 - 2;
        int ovulationEnd  = cycleLength / 2 + 1;
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
        resp.setCycleId(0);        // 0 = no cycle started
        resp.setCycleNumber(0);    // 0 = no cycle started
        resp.setCycleDay(0);       // 0 = not tracking yet
        resp.setCycleLength(cycleLen);
        resp.setPeriodLength(periodLen);
        resp.setPhase("None");
        resp.setPhaseKey("none");
        resp.setCycleStartDate("");  // empty = not started
        resp.setNextPeriodDate("");
        resp.setDaysUntilNext(0);
        resp.setLoggedDays(0);
        resp.setTotalCycleDays(cycleLen);
        resp.setMoodCounts(new LinkedHashMap<>());
        resp.setDischargeCounts(new LinkedHashMap<>());
        resp.setSymptomCounts(new LinkedHashMap<>());
        return resp;
    }
}