package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_sections")
public class HomeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_key", nullable = false, unique = true, length = 50)
    private String sectionKey;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getSectionKey() { return sectionKey; }
    public void setSectionKey(String v) { this.sectionKey = v; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String v) { this.contentJson = v; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean v) { this.isActive = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}