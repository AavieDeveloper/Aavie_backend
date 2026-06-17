package com.example.AavieApp.model;


import jakarta.persistence.*;
import com.example.AavieApp.util.JsonListConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * One row per body zone that has selected symptoms in a daily log.
 *
 * chips is now a MySQL JSON array instead of a comma-separated string.
 * e.g. ["Cramps", "Pelvic pain", "Bloating"]
 *
 * Queryable:
 *   WHERE daily_log_id = X AND JSON_CONTAINS(chips, '"Cramps"')
 */
@Entity
@Table(name = "daily_log_body_zones")
public class DailyLogBodyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    /** Denormalised for direct queries without join */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "zone_id", nullable = false, length = 32)
    private String zoneId;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "chips", nullable = false, columnDefinition = "json")
    private List<String> chips = new ArrayList<>();

    public DailyLogBodyZone() {}

    public DailyLogBodyZone(DailyLog dailyLog, String zoneId, List<String> chips) {
        this.dailyLog = dailyLog;
        this.userId   = dailyLog.getUserId();
        this.zoneId   = zoneId;
        this.chips    = chips;
    }

    public Long getUserId()              { return userId; }
    public void setUserId(Long v)        { this.userId = v; }
    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long         getId()                     { return id; }
    public DailyLog     getDailyLog()               { return dailyLog; }
    public void         setDailyLog(DailyLog v)     { this.dailyLog = v; }
    public String       getZoneId()                 { return zoneId; }
    public void         setZoneId(String v)         { this.zoneId = v; }
    public List<String> getChips()                  { return chips; }
    public void         setChips(List<String> v)    { this.chips = v; }
}