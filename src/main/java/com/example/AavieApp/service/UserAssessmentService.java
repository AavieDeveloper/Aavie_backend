package com.example.AavieApp.service;

import com.example.AavieApp.model.UserAssessment;
import com.example.AavieApp.repository.UserAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Aavie — UserAssessment Service
 *
 * Enforces the strict order: PRAKRITI → PCOS → VIKRITI
 * A user cannot submit PCOS without completing PRAKRITI first,
 * and cannot submit VIKRITI without completing PCOS first.
 */
@Service
@Transactional
public class UserAssessmentService {

    // Assessment order constants
    public static final String TYPE_PRAKRITI = "PRAKRITI";
    public static final String TYPE_PCOS     = "PCOS";
    public static final String TYPE_VIKRITI  = "VIKRITI";

    private final UserAssessmentRepository repo;

    public UserAssessmentService(UserAssessmentRepository repo) {
        this.repo = repo;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    /** Common inbound DTO for all three assessment submissions */
    public static class SubmitAssessmentRequest {
        private Long    userId;
        private String  assessmentType;   // "PRAKRITI" | "PCOS" | "VIKRITI"
        private String  resultType;       // e.g. "Vata", "Kapha PCOS", "Vata Vikriti"
        private String  severity;         // PCOS only: "subclinical" | "mild" | "moderate" | "severe"
        private Integer confidenceScore;
        private Integer scoreVata;
        private Integer scorePitta;
        private Integer scoreKapha;
        private Integer pcosConfidence;
        private Integer cycleScore;
        private String  agniType;
        private Integer energyScore;
        private Integer sleepScore;
        private Integer stressScore;
        private Integer metabolicScore;
        private Integer liverScore;
        private String  prakritiKey;
        private String  answersJson;
        private String selectedSignals;
        


        public String getSelectedSignals()      { return selectedSignals; }
        public void setSelectedSignals(String s){ this.selectedSignals = s; }
        
        private Boolean amaDetected;
        public Boolean getAmaDetected()          { return amaDetected; }
        public void setAmaDetected(Boolean a)    { this.amaDetected = a; }


        // Getters & Setters
        public Long    getUserId()           { return userId; }
        public void    setUserId(Long u)     { this.userId = u; }
        public String  getAssessmentType()   { return assessmentType; }
        public void    setAssessmentType(String t) { this.assessmentType = t; }
        public String  getResultType()       { return resultType; }
        public void    setResultType(String r) { this.resultType = r; }
        public String  getSeverity()         { return severity; }
        public void    setSeverity(String s) { this.severity = s; }
        public Integer getConfidenceScore()  { return confidenceScore; }
        public void    setConfidenceScore(Integer c) { this.confidenceScore = c; }
        public Integer getScoreVata()        { return scoreVata; }
        public void    setScoreVata(Integer v) { this.scoreVata = v; }
        public Integer getScorePitta()       { return scorePitta; }
        public void    setScorePitta(Integer p) { this.scorePitta = p; }
        public Integer getScoreKapha()       { return scoreKapha; }
        public void    setScoreKapha(Integer k) { this.scoreKapha = k; }
        public Integer getPcosConfidence()   { return pcosConfidence; }
        public void    setPcosConfidence(Integer p) { this.pcosConfidence = p; }
        public Integer getCycleScore()       { return cycleScore; }
        public void    setCycleScore(Integer c) { this.cycleScore = c; }
        public String  getAgniType()         { return agniType; }
        public void    setAgniType(String a) { this.agniType = a; }
        public Integer getEnergyScore()      { return energyScore; }
        public void    setEnergyScore(Integer e) { this.energyScore = e; }
        public Integer getSleepScore()       { return sleepScore; }
        public void    setSleepScore(Integer s) { this.sleepScore = s; }
        public Integer getStressScore()      { return stressScore; }
        public void    setStressScore(Integer s) { this.stressScore = s; }
        public Integer getMetabolicScore()   { return metabolicScore; }
        public void    setMetabolicScore(Integer m) { this.metabolicScore = m; }
        public Integer getLiverScore()       { return liverScore; }
        public void    setLiverScore(Integer l) { this.liverScore = l; }
        public String  getPrakritiKey()      { return prakritiKey; }
        public void    setPrakritiKey(String p) { this.prakritiKey = p; }
        public String  getAnswersJson()      { return answersJson; }
        public void    setAnswersJson(String j) { this.answersJson = j; }
    }

    /** Outbound DTO returned after saving */
    public static class AssessmentResponse {
        private Long    id;
        private Long    userId;
        private String  assessmentType;
        private String  resultType;
        private String  severity;
        private Integer confidenceScore;
        private Integer scoreVata;
        private Integer scorePitta;
        private Integer scoreKapha;
        private Integer pcosConfidence;
        private Integer cycleScore;
        private String  agniType;
        private Integer energyScore;
        private Integer sleepScore;
        private Integer stressScore;
        private Integer metabolicScore;
        private Integer liverScore;
        private String  prakritiKey;
        private String  completedAt;
        private String  answersJson;
        // Assessment progress
        private boolean prakritiDone;
        private boolean pcosDone;
        private boolean vikritiDone;
        private String  nextAssessment;   // "PCOS" | "VIKRITI" | "COMPLETE"
        
        private String selectedSignals;

        public String getSelectedSignals()      { return selectedSignals; }
        public void setSelectedSignals(String s){ this.selectedSignals = s; }
        
        private Boolean amaDetected;
        public Boolean getAmaDetected()          { return amaDetected; }
        public void setAmaDetected(Boolean a)    { this.amaDetected = a; }

        // Getters & Setters
        public Long    getId()                   { return id; }
        public void    setId(Long id)            { this.id = id; }
        public Long    getUserId()               { return userId; }
        public void    setUserId(Long u)         { this.userId = u; }
        public String  getAssessmentType()       { return assessmentType; }
        public void    setAssessmentType(String t) { this.assessmentType = t; }
        public String  getResultType()           { return resultType; }
        public void    setResultType(String r)   { this.resultType = r; }
        public String  getSeverity()             { return severity; }
        public void    setSeverity(String s)     { this.severity = s; }
        public Integer getConfidenceScore()      { return confidenceScore; }
        public void    setConfidenceScore(Integer c) { this.confidenceScore = c; }
        public Integer getScoreVata()            { return scoreVata; }
        public void    setScoreVata(Integer v)   { this.scoreVata = v; }
        public Integer getScorePitta()           { return scorePitta; }
        public void    setScorePitta(Integer p)  { this.scorePitta = p; }
        public Integer getScoreKapha()           { return scoreKapha; }
        public void    setScoreKapha(Integer k)  { this.scoreKapha = k; }
        public Integer getPcosConfidence()       { return pcosConfidence; }
        public void    setPcosConfidence(Integer p) { this.pcosConfidence = p; }
        public Integer getCycleScore()           { return cycleScore; }
        public void    setCycleScore(Integer c)  { this.cycleScore = c; }
        public String  getAgniType()             { return agniType; }
        public void    setAgniType(String a)     { this.agniType = a; }
        public Integer getEnergyScore()          { return energyScore; }
        public void    setEnergyScore(Integer e) { this.energyScore = e; }
        public Integer getSleepScore()           { return sleepScore; }
        public void    setSleepScore(Integer s)  { this.sleepScore = s; }
        public Integer getStressScore()          { return stressScore; }
        public void    setStressScore(Integer s) { this.stressScore = s; }
        public Integer getMetabolicScore()       { return metabolicScore; }
        public void    setMetabolicScore(Integer m) { this.metabolicScore = m; }
        public Integer getLiverScore()           { return liverScore; }
        public void    setLiverScore(Integer l)  { this.liverScore = l; }
        public String  getPrakritiKey()          { return prakritiKey; }
        public void    setPrakritiKey(String p)  { this.prakritiKey = p; }
      public String  getCompletedAt()          { return completedAt; }
        public void    setCompletedAt(String c)  { this.completedAt = c; }
        public String  getAnswersJson()           { return answersJson; }
        public void    setAnswersJson(String j)   { this.answersJson = j; }
        public boolean isPrakritiDone()          { return prakritiDone; }
        public void    setPrakritiDone(boolean b){ this.prakritiDone = b; }
        public boolean isPcosDone()              { return pcosDone; }
        public void    setPcosDone(boolean b)    { this.pcosDone = b; }
        public boolean isVikritiDone()           { return vikritiDone; }
        public void    setVikritiDone(boolean b) { this.vikritiDone = b; }
        public String  getNextAssessment()       { return nextAssessment; }
        public void    setNextAssessment(String n) { this.nextAssessment = n; }
    }

    /** Status DTO — returned by GET /api/assessments/status/{userId} */
    public static class AssessmentStatusResponse {
        private boolean prakritiDone;
        private boolean pcosDone;
        private boolean vikritiDone;
        private String  nextAssessment;      // "PRAKRITI" | "PCOS" | "VIKRITI" | "COMPLETE"
        private String  prakritiResult;      // e.g. "Pitta-Kapha" or null
        private String  pcosResult;          // e.g. "Kapha PCOS" or null
        private String  pcosSeverity;        // e.g. "moderate" or null
        private String  vikritiResult;       // e.g. "Vata Vikriti" or null
        private String  agniType;            // e.g. "Manda" or null
        private int     completedCount;      // 0, 1, 2, or 3
        private int     pcosRetakeDaysRemaining;
        private String  pcosLastCompletedAt;      // 0, 1, 2, or 3
        

        private String selectedSignals;

        public String getSelectedSignals()      { return selectedSignals; }
        public void setSelectedSignals(String s){ this.selectedSignals = s; }
        
        public boolean isPrakritiDone()          { return prakritiDone; }
        public void    setPrakritiDone(boolean b){ this.prakritiDone = b; }
        public boolean isPcosDone()              { return pcosDone; }
        public void    setPcosDone(boolean b)    { this.pcosDone = b; }
        public boolean isVikritiDone()           { return vikritiDone; }
        public void    setVikritiDone(boolean b) { this.vikritiDone = b; }
        public String  getNextAssessment()       { return nextAssessment; }
        public void    setNextAssessment(String n) { this.nextAssessment = n; }
        public String  getPrakritiResult()       { return prakritiResult; }
        public void    setPrakritiResult(String r) { this.prakritiResult = r; }
        public String  getPcosResult()           { return pcosResult; }
        public void    setPcosResult(String r)   { this.pcosResult = r; }
        public String  getPcosSeverity()         { return pcosSeverity; }
        public void    setPcosSeverity(String s) { this.pcosSeverity = s; }
        public String  getVikritiResult()        { return vikritiResult; }
        public void    setVikritiResult(String r){ this.vikritiResult = r; }
        public String  getAgniType()             { return agniType; }
        public void    setAgniType(String a)     { this.agniType = a; }
        public int     getCompletedCount()              { return completedCount; }
        public void    setCompletedCount(int c)         { this.completedCount = c; }
        public int     getPcosRetakeDaysRemaining()     { return pcosRetakeDaysRemaining; }
        public void    setPcosRetakeDaysRemaining(int d){ this.pcosRetakeDaysRemaining = d; }
        public String  getPcosLastCompletedAt()         { return pcosLastCompletedAt; }
        public void    setPcosLastCompletedAt(String d) { this.pcosLastCompletedAt = d; }
        
        
    }

    // ── Service Methods ───────────────────────────────────────────────────────

    /**
     * Submit an assessment result.
     * Enforces the strict order: PRAKRITI → PCOS → VIKRITI.
     * If the user retakes an assessment, the existing row is updated.
     *
     * Called by POST /api/assessments/submit
     */
    public AssessmentResponse submitAssessment(SubmitAssessmentRequest req) {
        validateType(req.getAssessmentType());
        enforceOrder(req.getUserId(), req.getAssessmentType());

        // Upsert — update if exists, insert if not
        UserAssessment assessment = repo
            .findByUserIdAndAssessmentType(req.getUserId(), req.getAssessmentType())
            .orElse(new UserAssessment());

        mapRequestToEntity(req, assessment);
        UserAssessment saved = repo.save(assessment);

        return buildResponse(saved, req.getUserId());
    }

    /**
     * Get assessment completion status for a user.
     * Called by GET /api/assessments/status/{userId}
     */
    @Transactional(readOnly = true)
    public AssessmentStatusResponse getStatus(Long userId) {
        boolean prakritiDone = repo.existsByUserIdAndAssessmentType(userId, TYPE_PRAKRITI);
        boolean pcosDone     = repo.existsByUserIdAndAssessmentType(userId, TYPE_PCOS);
        boolean vikritiDone  = repo.existsByUserIdAndAssessmentType(userId, TYPE_VIKRITI);

        Optional<UserAssessment> prakritiOpt = repo.findByUserIdAndAssessmentType(userId, TYPE_PRAKRITI);
        Optional<UserAssessment> pcosOpt     = repo.findByUserIdAndAssessmentType(userId, TYPE_PCOS);
        Optional<UserAssessment> vikritiOpt  = repo.findByUserIdAndAssessmentType(userId, TYPE_VIKRITI);

        AssessmentStatusResponse status = new AssessmentStatusResponse();
        status.setPrakritiDone(prakritiDone);
        status.setPcosDone(pcosDone);
        status.setVikritiDone(vikritiDone);
        status.setCompletedCount((prakritiDone ? 1 : 0) + (pcosDone ? 1 : 0) + (vikritiDone ? 1 : 0));

        prakritiOpt.ifPresent(a -> status.setPrakritiResult(a.getResultType()));
        pcosOpt.ifPresent(a -> {
            status.setPcosResult(a.getResultType());
            status.setPcosSeverity(a.getSeverity());
            status.setSelectedSignals(a.getSelectedSignals());
            if (a.getLastCompletedAt() != null) {
                long daysSince = java.time.temporal.ChronoUnit.DAYS.between(
                    a.getLastCompletedAt(),
                    java.time.LocalDateTime.now()
                );
                long daysRemaining = 25 - daysSince;
                status.setPcosRetakeDaysRemaining(daysRemaining > 0 ? (int) daysRemaining : 0);
                status.setPcosLastCompletedAt(a.getLastCompletedAt().toString());
            }
        });
        vikritiOpt.ifPresent(a -> {
            status.setVikritiResult(a.getResultType());
            status.setAgniType(a.getAgniType());
        });

        // Determine next required assessment
        if (!prakritiDone)      status.setNextAssessment(TYPE_PRAKRITI);
        else if (!pcosDone)     status.setNextAssessment(TYPE_PCOS);
        else if (!vikritiDone)  status.setNextAssessment(TYPE_VIKRITI);
        else                    status.setNextAssessment("COMPLETE");

        return status;
    }

    /**
     * Get a single assessment result.
     * Called by GET /api/assessments/{userId}/{type}
     */
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessment(Long userId, String type) {
        validateType(type);
        UserAssessment a = repo.findByUserIdAndAssessmentType(userId, type)
            .orElseThrow(() -> new RuntimeException(type + " assessment not found for user " + userId));
        return buildResponse(a, userId);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Enforce strict order: PRAKRITI → PCOS → VIKRITI.
     * Throws IllegalStateException if prerequisite is missing.
     */
    private void enforceOrder(Long userId, String type) {
        switch (type) {
            case TYPE_PRAKRITI:
                // No prerequisite
                break;
            case TYPE_PCOS:
                if (!repo.existsByUserIdAndAssessmentType(userId, TYPE_PRAKRITI)) {
                    throw new IllegalStateException(
                        "You must complete the Prakriti assessment before taking the PCOS assessment."
                    );
                }
                // 30-day cooldown on retake
                repo.findByUserIdAndAssessmentType(userId, TYPE_PCOS).ifPresent(existing -> {
                    if (existing.getLastCompletedAt() != null) {
                        long daysSince = java.time.temporal.ChronoUnit.DAYS.between(
                            existing.getLastCompletedAt(),
                            java.time.LocalDateTime.now()
                        );
                        if (daysSince < 25) {
                            throw new IllegalStateException(
                                "COOLDOWN:" + (25 - daysSince)
                            );
                        }
                    }
                });
                break;
            case TYPE_VIKRITI:
                if (!repo.existsByUserIdAndAssessmentType(userId, TYPE_PRAKRITI)) {
                    throw new IllegalStateException(
                        "You must complete the Prakriti assessment before taking the Vikriti assessment."
                    );
                }
                if (!repo.existsByUserIdAndAssessmentType(userId, TYPE_PCOS)) {
                    throw new IllegalStateException(
                        "You must complete the PCOS assessment before taking the Vikriti assessment."
                    );
                }
                break;
        }
    }

    private void validateType(String type) {
        if (!TYPE_PRAKRITI.equals(type) && !TYPE_PCOS.equals(type) && !TYPE_VIKRITI.equals(type)) {
            throw new IllegalArgumentException("Invalid assessment type: " + type);
        }
    }

    private void mapRequestToEntity(SubmitAssessmentRequest req, UserAssessment a) {
        a.setUserId(req.getUserId());
        a.setAssessmentType(req.getAssessmentType());
        a.setLastCompletedAt(java.time.LocalDateTime.now());
        a.setResultType(req.getResultType());
        a.setSeverity(req.getSeverity());
        a.setConfidenceScore(req.getConfidenceScore());
        a.setScoreVata(req.getScoreVata());
        a.setScorePitta(req.getScorePitta());
        a.setScoreKapha(req.getScoreKapha());
        a.setPcosConfidence(req.getPcosConfidence());
        a.setCycleScore(req.getCycleScore());
        a.setAgniType(req.getAgniType());
        a.setEnergyScore(req.getEnergyScore());
        a.setSleepScore(req.getSleepScore());
        a.setStressScore(req.getStressScore());
        a.setMetabolicScore(req.getMetabolicScore());
        a.setLiverScore(req.getLiverScore());
        a.setPrakritiKey(req.getPrakritiKey());
        a.setAnswersJson(req.getAnswersJson());
        a.setSelectedSignals(req.getSelectedSignals());
        
        a.setAmaDetected(req.getAmaDetected());
    }

    private AssessmentResponse buildResponse(UserAssessment a, Long userId) {
        AssessmentResponse r = new AssessmentResponse();
        r.setId(a.getId());
        r.setUserId(a.getUserId());
        r.setAssessmentType(a.getAssessmentType());
        r.setResultType(a.getResultType());
        r.setSeverity(a.getSeverity());
        r.setConfidenceScore(a.getConfidenceScore());
        r.setScoreVata(a.getScoreVata());
        r.setScorePitta(a.getScorePitta());
        r.setScoreKapha(a.getScoreKapha());
        r.setPcosConfidence(a.getPcosConfidence());
        r.setCycleScore(a.getCycleScore());
        r.setAgniType(a.getAgniType());
        r.setEnergyScore(a.getEnergyScore());
        r.setSleepScore(a.getSleepScore());
        r.setStressScore(a.getStressScore());
        r.setMetabolicScore(a.getMetabolicScore());
        r.setLiverScore(a.getLiverScore());
        r.setPrakritiKey(a.getPrakritiKey());
        r.setSelectedSignals(a.getSelectedSignals());
       r.setCompletedAt(a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        r.setAnswersJson(a.getAnswersJson());
        
        r.setAmaDetected(a.getAmaDetected());

        // Populate progress flags
        boolean prakritiDone = repo.existsByUserIdAndAssessmentType(userId, TYPE_PRAKRITI);
        boolean pcosDone     = repo.existsByUserIdAndAssessmentType(userId, TYPE_PCOS);
        boolean vikritiDone  = repo.existsByUserIdAndAssessmentType(userId, TYPE_VIKRITI);
        r.setPrakritiDone(prakritiDone);
        r.setPcosDone(pcosDone);
        r.setVikritiDone(vikritiDone);

        if (!prakritiDone)     r.setNextAssessment(TYPE_PRAKRITI);
        else if (!pcosDone)    r.setNextAssessment(TYPE_PCOS);
        else if (!vikritiDone) r.setNextAssessment(TYPE_VIKRITI);
        else                   r.setNextAssessment("COMPLETE");

        return r;
    }
}