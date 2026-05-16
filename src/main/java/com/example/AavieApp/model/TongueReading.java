package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tongue_readings")
public class TongueReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private String prakriti;

    @Column
    private String pcosType;

    @Column
    private String dominantDoshaImbalance;

    @Column
    private String amaLevel;

    @Column
    private String agniState;

    @Column(length = 1000)
    private String oneLineInsight;

    @Column
    private String coatingLocation;

    @Column
    private String selectedZone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long u) { this.userId = u; }
    public String getPrakriti() { return prakriti; }
    public void setPrakriti(String p) { this.prakriti = p; }
    public String getPcosType() { return pcosType; }
    public void setPcosType(String p) { this.pcosType = p; }
    public String getDominantDoshaImbalance() { return dominantDoshaImbalance; }
    public void setDominantDoshaImbalance(String d) { this.dominantDoshaImbalance = d; }
    public String getAmaLevel() { return amaLevel; }
    public void setAmaLevel(String a) { this.amaLevel = a; }
    public String getAgniState() { return agniState; }
    public void setAgniState(String a) { this.agniState = a; }
    public String getOneLineInsight() { return oneLineInsight; }
    public void setOneLineInsight(String o) { this.oneLineInsight = o; }
    public String getCoatingLocation() { return coatingLocation; }
    public void setCoatingLocation(String c) { this.coatingLocation = c; }
    public String getSelectedZone() { return selectedZone; }
    public void setSelectedZone(String s) { this.selectedZone = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}