package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per cycle per user.
 * Cycle 1 starts on the first period date recorded.
 * Cycle N+1 is created automatically when a new period start is marked.
 *
 * end_date is null until the next cycle starts — that's normal.
 */
@Entity
@Table(name = "cycles")
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 1, 2, 3 ... — increments per user */
    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;

    /** First day of this cycle (period Day 1) */
    @Column(name = "start_date", nullable = true)
    private LocalDate startDate;

    /** Filled when the next cycle starts. Null = current active cycle */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Actual length in days — filled when end_date is set */
    @Column(name = "cycle_length")
    private Integer cycleLength;

    /** How many days the period flow lasted this cycle */
    @Column(name = "period_length")
    private Integer periodLength;

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

    public Cycle() {}

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long      getId()                         { return id; }
    public Long      getUserId()                     { return userId; }
    public void      setUserId(Long v)               { this.userId = v; }
    public Integer   getCycleNumber()                { return cycleNumber; }
    public void      setCycleNumber(Integer v)       { this.cycleNumber = v; }
    public LocalDate getStartDate()                  { return startDate; }
    public void      setStartDate(LocalDate v)       { this.startDate = v; }
    public LocalDate getEndDate()                    { return endDate; }
    public void      setEndDate(LocalDate v)         { this.endDate = v; }
    public Integer   getCycleLength()                { return cycleLength; }
    public void      setCycleLength(Integer v)       { this.cycleLength = v; }
    public Integer   getPeriodLength()               { return periodLength; }
    public void      setPeriodLength(Integer v)      { this.periodLength = v; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }
}