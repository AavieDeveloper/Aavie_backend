package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aavie — UserAssessment JPA Entity
 * Table: user_assessments
 *
 * Stores results for all three assessments:
 *   type = "PRAKRITI"  → body type quiz result
 *   type = "PCOS"      → cycle intelligence result
 *   type = "VIKRITI"   → vikriti / body intelligence result
 *
 * One row per assessment per user.
 * On retake, the existing row is UPDATED (not duplicated).
 */
@Entity
@Table(
    name = "user_assessments",
    uniqueConstraints = {
        // One assessment of each type per user
        @UniqueConstraint(columnNames = { "user_id", "assessment_type" })
    }
)
public class UserAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Who took it ───────────────────────────────────────────────────────────
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ── Which assessment ──────────────────────────────────────────────────────
    /**
     * "PRAKRITI" | "PCOS" | "VIKRITI"
     */
    @Column(name = "assessment_type", nullable = false, length = 20)
    private String assessmentType;

    // ── Common result fields ──────────────────────────────────────────────────

    /**
     * Primary result label.
     * PRAKRITI → e.g. "Vata", "Pitta-Kapha"
     * PCOS     → e.g. "Vata PCOS", "Kapha PCOS"
     * VIKRITI  → e.g. "Vata Vikriti", "Pitta-Kapha Vikriti"
     */
    @Column(nullable = false)
    private String resultType;

    /**
     * Severity / confidence level.
     * PRAKRITI → null (not applicable)
     * PCOS     → "subclinical" | "mild" | "moderate" | "severe"
     * VIKRITI  → null (uses health scores instead)
     */
    @Column
    private String severity;

    /**
     * Percentage confidence score (0–100).
     */
    @Column
    private Integer confidenceScore;

    // ── Dosha scores (raw) ────────────────────────────────────────────────────
    @Column private Integer scoreVata;
    @Column private Integer scorePitta;
    @Column private Integer scoreKapha;

    // ── PCOS specific ─────────────────────────────────────────────────────────
    @Column private Integer pcosConfidence;   // 0–100
    @Column private Integer cycleScore;       // 0–100

    // ── Vikriti specific ─────────────────────────────────────────────────────
    @Column private String  agniType;          // "Sama" | "Vishama" | "Tikshna" | "Manda"
    @Column private Integer energyScore;
    @Column private Integer sleepScore;
    @Column private Integer stressScore;
    @Column private Integer metabolicScore;
    @Column private Integer liverScore;

    // ── Prakriti key used when taking PCOS / Vikriti ─────────────────────────
    /**
     * The Prakriti key selected at the start of PCOS / Vikriti assessment.
     * e.g. "V", "P", "K", "VP", "VK", "PK", "T"
     */
    @Column(length = 20)  // Changed from 5 to 20
    private String prakritiKey;

    // ── Full JSON blob for raw answers (optional, for future re-scoring) ──────
    @Column(columnDefinition = "TEXT")
    private String answersJson;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────
    public UserAssessment() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Long getUserId()                      { return userId; }
    public void setUserId(Long userId)           { this.userId = userId; }

    public String getAssessmentType()            { return assessmentType; }
    public void setAssessmentType(String t)      { this.assessmentType = t; }

    public String getResultType()                { return resultType; }
    public void setResultType(String r)          { this.resultType = r; }

    public String getSeverity()                  { return severity; }
    public void setSeverity(String s)            { this.severity = s; }

    public Integer getConfidenceScore()          { return confidenceScore; }
    public void setConfidenceScore(Integer c)    { this.confidenceScore = c; }

    public Integer getScoreVata()                { return scoreVata; }
    public void setScoreVata(Integer v)          { this.scoreVata = v; }

    public Integer getScorePitta()               { return scorePitta; }
    public void setScorePitta(Integer p)         { this.scorePitta = p; }

    public Integer getScoreKapha()               { return scoreKapha; }
    public void setScoreKapha(Integer k)         { this.scoreKapha = k; }

    public Integer getPcosConfidence()           { return pcosConfidence; }
    public void setPcosConfidence(Integer p)     { this.pcosConfidence = p; }

    public Integer getCycleScore()               { return cycleScore; }
    public void setCycleScore(Integer c)         { this.cycleScore = c; }

    public String getAgniType()                  { return agniType; }
    public void setAgniType(String a)            { this.agniType = a; }

    public Integer getEnergyScore()              { return energyScore; }
    public void setEnergyScore(Integer e)        { this.energyScore = e; }

    public Integer getSleepScore()               { return sleepScore; }
    public void setSleepScore(Integer s)         { this.sleepScore = s; }

    public Integer getStressScore()              { return stressScore; }
    public void setStressScore(Integer s)        { this.stressScore = s; }

    public Integer getMetabolicScore()           { return metabolicScore; }
    public void setMetabolicScore(Integer m)     { this.metabolicScore = m; }

    public Integer getLiverScore()               { return liverScore; }
    public void setLiverScore(Integer l)         { this.liverScore = l; }

    public String getPrakritiKey()               { return prakritiKey; }
    public void setPrakritiKey(String p)         { this.prakritiKey = p; }

    public String getAnswersJson()               { return answersJson; }
    public void setAnswersJson(String j)         { this.answersJson = j; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}