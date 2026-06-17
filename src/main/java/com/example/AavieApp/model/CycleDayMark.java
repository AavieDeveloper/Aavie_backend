package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One mark per user per calendar date.
 * Now linked to a specific cycle via cycle_id.
 *
 * mark_type:
 *   1 = Period
 *   2 = Light spotting
 *   3 = White discharge
 *   4 = Bleeding between cycles
 *   5 = Other symptom
 */
@Entity
@Table(
    name = "cycle_day_marks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mark_date"})
)
public class CycleDayMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** FK → cycles.id — which cycle this mark belongs to */
    @Column(name = "cycle_id", nullable = true)
    private Long cycleId;

    @Column(name = "mark_date", nullable = false)
    private LocalDate markDate;

    @Column(name = "mark_type", nullable = false)
    private Integer markType;

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

    public CycleDayMark() {}

    public CycleDayMark(Long userId, Long cycleId, LocalDate markDate, Integer markType) {
        this.userId   = userId;
        this.cycleId  = cycleId;
        this.markDate = markDate;
        this.markType = markType;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long      getId()                        { return id; }
    public Long      getUserId()                    { return userId; }
    public void      setUserId(Long v)              { this.userId = v; }
    public Long      getCycleId()                   { return cycleId; }
    public void      setCycleId(Long v)             { this.cycleId = v; }
    public LocalDate getMarkDate()                  { return markDate; }
    public void      setMarkDate(LocalDate v)       { this.markDate = v; }
    public Integer   getMarkType()                  { return markType; }
    public void      setMarkType(Integer v)         { this.markType = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public LocalDateTime getUpdatedAt()             { return updatedAt; }
}