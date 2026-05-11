package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "logDate"}))
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate logDate;

    /** Comma-separated mood labels e.g. "Happy,Calm" */
    @Column(length = 500)
    private String moods;

    /** Comma-separated body symptoms e.g. "Bloating,Cramps" */
    @Column(length = 500)
    private String bodySymptoms;

    /** Comma-separated behaviour labels */
    @Column(length = 500)
    private String behaviours;

    /** Day number in the cycle e.g. 14 */
    @Column
    private Integer cycleDay;

    /** Phase at time of logging: menstrual, follicular, ovulation, luteal */
    @Column(length = 50)
    private String phase;

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

    // Getters and setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate d) { this.logDate = d; }
    public String getMoods() { return moods; }
    public void setMoods(String moods) { this.moods = moods; }
    public String getBodySymptoms() { return bodySymptoms; }
    public void setBodySymptoms(String s) { this.bodySymptoms = s; }
    public String getBehaviours() { return behaviours; }
    public void setBehaviours(String b) { this.behaviours = b; }
    public Integer getCycleDay() { return cycleDay; }
    public void setCycleDay(Integer d) { this.cycleDay = d; }
    public String getPhase() { return phase; }
    public void setPhase(String p) { this.phase = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}