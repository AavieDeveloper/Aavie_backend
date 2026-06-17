package com.example.AavieApp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "intro_slides")
public class IntroSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slide_index", nullable = false)
    private int slideIndex;

    @Column(name = "label")
    private String label;

    @Column(name = "title_json", columnDefinition = "TEXT")
    private String titleJson;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "cta")
    private String cta;

    @Column(name = "tag")
    private String tag;

    @Column(name = "is_active")
    private boolean isActive = true;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getSlideIndex() { return slideIndex; }
    public void setSlideIndex(int slideIndex) { this.slideIndex = slideIndex; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getTitleJson() { return titleJson; }
    public void setTitleJson(String titleJson) { this.titleJson = titleJson; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCta() { return cta; }
    public void setCta(String cta) { this.cta = cta; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    public boolean isActive() { return isActive; }

    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    public void setActive(boolean active) { isActive = active; }
}