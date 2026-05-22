package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;

    private String ageGroup;  // "18-24" | "25-30" | "31-35" | "36-45" | "all"

    @Column(columnDefinition = "TEXT")
    private String body;

    private String imageUrl;

    private String readTime;

    @Column(nullable = false)
    private String status = "draft";  // "live" | "draft"

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
    public Long getId()                      { return id; }
    public String getTitle()                 { return title; }
    public void setTitle(String t)           { this.title = t; }
    public String getCategory()              { return category; }
    public void setCategory(String c)        { this.category = c; }
    public String getAgeGroup()              { return ageGroup; }
    public void setAgeGroup(String a)        { this.ageGroup = a; }
    public String getBody()                  { return body; }
    public void setBody(String b)            { this.body = b; }
    public String getImageUrl()              { return imageUrl; }
    public void setImageUrl(String u)        { this.imageUrl = u; }
    public String getReadTime()              { return readTime; }
    public void setReadTime(String r)        { this.readTime = r; }
    public String getStatus()                { return status; }
    public void setStatus(String s)          { this.status = s; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public LocalDateTime getUpdatedAt()      { return updatedAt; }
}