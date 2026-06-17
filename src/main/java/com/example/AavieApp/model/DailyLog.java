package com.example.AavieApp.model;


import jakarta.persistence.*;
import com.example.AavieApp.util.JsonListConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Header record for one daily log entry (the 6-step wizard).
 *
 * Changes from v1:
 *  - cycle_id FK added (links to cycles table)
 *  - discharge and behaviours are now MySQL JSON columns (List<String>)
 *  - body zones still in separate child table (DailyLogBodyZone)
 *
 * Requires: com.vladmihalcea:hibernate-types-60 in pom.xml
 */
@Entity
@Table(
    name = "daily_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "log_date"})
)
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** FK → cycles.id */
    @Column(name = "cycle_id", nullable = false)
    private Long cycleId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    /** "today" or "yesterday" */
    @Column(name = "day_label", length = 16)
    private String dayLabel;

    /** Day number within this cycle e.g. 14 */
    @Column(name = "cycle_day")
    private Integer cycleDay;

    /** Phase name e.g. "Ovulation" */
    @Column(name = "phase", length = 32)
    private String phase;

    // ── Step 1: Energy ────────────────────────────────────────────────────────
    @Column(name = "energy", length = 16)
    private String energy;

    // ── Step 2: Discharge — JSON array ────────────────────────────────────────
    /**
     * Stored as MySQL JSON array.
     * e.g. ["egg-white", "creamy"]
     * Queryable with JSON_CONTAINS(discharge, '"egg-white"')
     */
    @Convert(converter = JsonListConverter.class)
    @Column(name = "discharge", columnDefinition = "json")
    private List<String> discharge = new ArrayList<>();

    // ── Step 3: Mood ──────────────────────────────────────────────────────────
    @Column(name = "mood", length = 16)
    private String mood;

    // ── Step 5: Behaviours — JSON array ───────────────────────────────────────
    /**
     * Stored as MySQL JSON array.
     * e.g. ["Went quiet", "Cried"]
     * Queryable with JSON_CONTAINS(behaviours, '"Went quiet"')
     */
  
    @Convert(converter = JsonListConverter.class)
    @Column(name = "behaviours", columnDefinition = "json")
    private List<String> behaviours = new ArrayList<>();

    // ── Step 6: Character ─────────────────────────────────────────────────────
    @Column(name = "character_state", length = 16)
    private String characterState;

    // ── Step 4: Body zones (child table) ─────────────────────────────────────
    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DailyLogBodyZone> bodyZones = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    public DailyLog() {}

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long      getId()                                    { return id; }
    public Long      getUserId()                                { return userId; }
    public void      setUserId(Long v)                          { this.userId = v; }
    public Long      getCycleId()                               { return cycleId; }
    public void      setCycleId(Long v)                         { this.cycleId = v; }
    public LocalDate getLogDate()                               { return logDate; }
    public void      setLogDate(LocalDate v)                    { this.logDate = v; }
    public String    getDayLabel()                              { return dayLabel; }
    public void      setDayLabel(String v)                      { this.dayLabel = v; }
    public Integer   getCycleDay()                              { return cycleDay; }
    public void      setCycleDay(Integer v)                     { this.cycleDay = v; }
    public String    getPhase()                                 { return phase; }
    public void      setPhase(String v)                         { this.phase = v; }
    public String    getEnergy()                                { return energy; }
    public void      setEnergy(String v)                        { this.energy = v; }
    public List<String> getDischarge()                          { return discharge; }
    public void      setDischarge(List<String> v)               { this.discharge = v; }
    public String    getMood()                                  { return mood; }
    public void      setMood(String v)                          { this.mood = v; }
    public List<String> getBehaviours()                         { return behaviours; }
    public void      setBehaviours(List<String> v)              { this.behaviours = v; }
    public String    getCharacterState()                        { return characterState; }
    public void      setCharacterState(String v)                { this.characterState = v; }
    public List<DailyLogBodyZone> getBodyZones()                { return bodyZones; }
    public void      setBodyZones(List<DailyLogBodyZone> v)     { this.bodyZones = v; }
    public LocalDateTime getCreatedAt()                         { return createdAt; }
    public LocalDateTime getUpdatedAt()                         { return updatedAt; }
}