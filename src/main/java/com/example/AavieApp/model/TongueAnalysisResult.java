package com.example.AavieApp.model;

/**
 * Aavie — TongueAnalysisResult
 *
 * Computed response from Gemini Vision API.
 * Not a JPA entity — returned directly to the app.
 *
 * Shape consumed by cycle-intelligence.tsx (TongueAnalysis interface):
 * {
 *   dominant_dosha_imbalance: string,
 *   ama_level:  "none" | "mild" | "moderate" | "heavy",
 *   agni_state: "strong" | "moderate" | "low" | "very_low",
 *   one_line_insight: string,
 *   redness_zones: string[],
 *   coating_location: string
 * }
 */
public class TongueAnalysisResult {

    private String   dominantDoshaImbalance;  // "Vata" | "Pitta" | "Kapha" | "balanced"
    private String   amaLevel;                // "none" | "mild" | "moderate" | "heavy"
    private String   agniState;               // "strong" | "moderate" | "low" | "very_low"
    private String   oneLineInsight;
    private String[] rednessZones;            // e.g. ["tip", "left"]
    private String   coatingLocation;         // e.g. "center" | "back" | "none"

 // ── Validation fields (v5) ────────────────────────────────────────────────
 private boolean  valid = true;            // false = image rejected as not-a-tongue
 private String   rejectionReason;         // "not_a_tongue" | "mouth_closed" | "too_blurry" | "too_dark" | "tongue_not_visible"
 private String   userMessage;             // Friendly retry message for the app

 public TongueAnalysisResult() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public String   getDominantDoshaImbalance()              { return dominantDoshaImbalance; }
    public void     setDominantDoshaImbalance(String v)      { this.dominantDoshaImbalance = v; }
    public String   getAmaLevel()                            { return amaLevel; }
    public void     setAmaLevel(String v)                    { this.amaLevel = v; }
    public String   getAgniState()                           { return agniState; }
    public void     setAgniState(String v)                   { this.agniState = v; }
    public String   getOneLineInsight()                      { return oneLineInsight; }
    public void     setOneLineInsight(String v)              { this.oneLineInsight = v; }
    public String[] getRednessZones()                        { return rednessZones; }
    public void     setRednessZones(String[] v)              { this.rednessZones = v; }
    public String   getCoatingLocation()                     { return coatingLocation; }
    public void     setCoatingLocation(String v)             { this.coatingLocation = v; }
    public boolean  isValid()                                { return valid; }
    public void     setValid(boolean v)                      { this.valid = v; }
    public String   getRejectionReason()                     { return rejectionReason; }
    public void     setRejectionReason(String v)             { this.rejectionReason = v; }
    public String   getUserMessage()                         { return userMessage; }
    public void     setUserMessage(String v)                 { this.userMessage = v; }
}