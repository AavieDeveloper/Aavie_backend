package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplement_plans")
public class SupplementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pcos_type")
    private String pcosType;

    @Column(name = "severity")
    private String severity;

    @Column(name = "formula_version")
    private String formulaVersion = "AAVIE_DOCTRINE_v4";

    @Column(name = "review_status")
    private String reviewStatus = "pending";

    @Column(name = "approved_for_user")
    private Boolean approvedForUser = false;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Column(name = "formula_modified")
    private Boolean formulaModified = false;

    @Column(name = "original_herbs", columnDefinition = "TEXT")
    private String originalHerbs;

    @Column(name = "herbs", columnDefinition = "TEXT")
    private String herbs;

    @Column(name = "am_herbs", columnDefinition = "TEXT")
    private String amHerbs;

    @Column(name = "pm_herbs", columnDefinition = "TEXT")
    private String pmHerbs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId()                           { return id; }
    public void setId(Long v)                     { this.id = v; }
    public Long getUserId()                       { return userId; }
    public void setUserId(Long v)                 { this.userId = v; }
    public String getPcosType()                   { return pcosType; }
    public void setPcosType(String v)             { this.pcosType = v; }
    public String getSeverity()                   { return severity; }
    public void setSeverity(String v)             { this.severity = v; }
    public String getFormulaVersion()             { return formulaVersion; }
    public void setFormulaVersion(String v)       { this.formulaVersion = v; }
    public String getReviewStatus()               { return reviewStatus; }
    public void setReviewStatus(String v)         { this.reviewStatus = v; }
    public Boolean getApprovedForUser()           { return approvedForUser; }
    public void setApprovedForUser(Boolean v)     { this.approvedForUser = v; }
    public LocalDateTime getReviewedAt()          { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v)    { this.reviewedAt = v; }
    public Long getReviewedBy()                   { return reviewedBy; }
    public void setReviewedBy(Long v)             { this.reviewedBy = v; }
    public String getDoctorNotes()                { return doctorNotes; }
    public void setDoctorNotes(String v)          { this.doctorNotes = v; }
    public Boolean getFormulaModified()           { return formulaModified; }
    public void setFormulaModified(Boolean v)     { this.formulaModified = v; }
    public String getOriginalHerbs()              { return originalHerbs; }
    public void setOriginalHerbs(String v)        { this.originalHerbs = v; }
    public String getHerbs()                      { return herbs; }
    public void setHerbs(String v)                { this.herbs = v; }
    public String getAmHerbs()                    { return amHerbs; }
    public void setAmHerbs(String v)              { this.amHerbs = v; }
    public String getPmHerbs()                    { return pmHerbs; }
    public void setPmHerbs(String v)              { this.pmHerbs = v; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime v)     { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)     { this.updatedAt = v; }
}