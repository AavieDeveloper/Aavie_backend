package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores a user's cycle preferences — their default cycle length and
 * period length. One row per user.
 *
 * This is separate from the cycles table which stores actual cycle instances.
 * These values are used as defaults when creating a new cycle.
 */
@Entity
@Table(
    name = "cycle_settings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"})
)
public class CycleSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** User's typical cycle length (default 28) */
    @Column(name = "cycle_length", nullable = false)
    private Integer cycleLength = 28;

    /** User's typical period duration (default 5) */
    @Column(name = "period_length", nullable = false)
    private Integer periodLength = 5;

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

    public CycleSettings() {}

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long    getId()                       { return id; }
    public Long    getUserId()                   { return userId; }
    public void    setUserId(Long v)             { this.userId = v; }
    public Integer getCycleLength()              { return cycleLength; }
    public void    setCycleLength(Integer v)     { this.cycleLength = v; }
    public Integer getPeriodLength()             { return periodLength; }
    public void    setPeriodLength(Integer v)    { this.periodLength = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}