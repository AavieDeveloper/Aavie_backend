package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assessmentType; // "PRAKRITI" | "PCOS" | "VIKRITI"

    @Column(nullable = false)
    private String questionId; // "c1", "p1", "r1" etc — matches frontend id

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    private String subText;        // optional sub-description
    private String subStyle;       // "gold" | "teal" | null
    private String section;        // "Your Cycle", "Body Patterns" etc
    private String sectionIcon;    // "🌸", "⚡" etc
    private String questionType;   // "single" | "multi"
    private Integer questionOrder;
    private Boolean isActive = true;
    private Boolean isBanner = false;
    private String bannerTitle;
    private String bannerDesc;

    @Column(columnDefinition = "LONGTEXT")
    private String optionsJson; // full options array as JSON

    @Column(updatable = false)
    private LocalDateTime createdAt;
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
    public Long getId()                          { return id; }
    public String getAssessmentType()            { return assessmentType; }
    public void setAssessmentType(String v)      { this.assessmentType = v; }
    public String getQuestionId()                { return questionId; }
    public void setQuestionId(String v)          { this.questionId = v; }
    public String getQuestionText()              { return questionText; }
    public void setQuestionText(String v)        { this.questionText = v; }
    public String getSubText()                   { return subText; }
    public void setSubText(String v)             { this.subText = v; }
    public String getSubStyle()                  { return subStyle; }
    public void setSubStyle(String v)            { this.subStyle = v; }
    public String getSection()                   { return section; }
    public void setSection(String v)             { this.section = v; }
    public String getSectionIcon()               { return sectionIcon; }
    public void setSectionIcon(String v)         { this.sectionIcon = v; }
    public String getQuestionType()              { return questionType; }
    public void setQuestionType(String v)        { this.questionType = v; }
    public Integer getQuestionOrder()            { return questionOrder; }
    public void setQuestionOrder(Integer v)      { this.questionOrder = v; }
    public Boolean getIsActive()                 { return isActive; }
    public void setIsActive(Boolean v)           { this.isActive = v; }
    public Boolean getIsBanner()                 { return isBanner; }
    public void setIsBanner(Boolean v)           { this.isBanner = v; }
    public String getBannerTitle()               { return bannerTitle; }
    public void setBannerTitle(String v)         { this.bannerTitle = v; }
    public String getBannerDesc()                { return bannerDesc; }
    public void setBannerDesc(String v)          { this.bannerDesc = v; }
    public String getOptionsJson()               { return optionsJson; }
    public void setOptionsJson(String v)         { this.optionsJson = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}