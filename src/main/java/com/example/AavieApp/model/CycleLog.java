package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cycle_logs")
public class CycleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate periodStartDate;

    @Column
    private LocalDate periodEndDate;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 28")
    private Integer cycleLengthDays = 28;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean historyOnly = false;

    public boolean isHistoryOnly() { return historyOnly; }
    public void setHistoryOnly(boolean h) { this.historyOnly = h; }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(LocalDate d) { this.periodStartDate = d; }
    public LocalDate getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(LocalDate d) { this.periodEndDate = d; }
    public Integer getCycleLengthDays() { return cycleLengthDays; }
    public void setCycleLengthDays(Integer d) { this.cycleLengthDays = d; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}