package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "assessment_drafts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "assessment_type" })
    }
)
public class AssessmentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "assessment_type", nullable = false, length = 20)
    private String assessmentType;

    @Column(columnDefinition = "TEXT")
    private String answersJson;

    @Column
    private Integer currentQuestion;

    @Column
    private Boolean completed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.savedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public AssessmentDraft() {}

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public Long getUserId()                      { return userId; }
    public void setUserId(Long u)                { this.userId = u; }
    public String getAssessmentType()            { return assessmentType; }
    public void setAssessmentType(String t)      { this.assessmentType = t; }
    public String getAnswersJson()               { return answersJson; }
    public void setAnswersJson(String j)         { this.answersJson = j; }
    public Integer getCurrentQuestion()          { return currentQuestion; }
    public void setCurrentQuestion(Integer q)    { this.currentQuestion = q; }
    public Boolean getCompleted()                { return completed; }
    public void setCompleted(Boolean c)          { this.completed = c; }
    public LocalDateTime getSavedAt()            { return savedAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}