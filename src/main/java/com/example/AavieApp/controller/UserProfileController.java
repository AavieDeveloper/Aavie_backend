package com.example.AavieApp.controller;


import com.example.AavieApp.service.UserProfileService.CreateProfileRequest;
import com.example.AavieApp.service.UserProfileService.ProfileResponse;
import com.example.AavieApp.service.UserProfileService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;
 
/**
 * Aavie — UserProfile REST Controller
 *
 * Base URL:  /api/user
 *
 * Endpoints consumed by the React Native app:
 *
 *   POST   /api/user/profile            → Create profile (from Create Profile screen)
 *   GET    /api/user/profile/{id}       → Load profile (for Profile tab)
 *   PUT    /api/user/profile/{id}       → Update profile (from Edit Profile)
 *   DELETE /api/user/profile/{id}       → Delete account
 *   GET    /api/user/profile/{id}/health→ Health check / ping
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")   // Replace with specific origin in production
public class UserProfileController {
 
    private final UserProfileService service;
 
    public UserProfileController(UserProfileService service) {
        this.service = service;
    }
 
    // ── CREATE ────────────────────────────────────────────────────────────────
    /**
     * Called by Create Profile screen after the user fills name / age / city.
     *
     * Request body (JSON):
     * {
     *   "name":   "Priya Sharma",
     *   "age":    27,
     *   "city":   "Mumbai, Maharashtra",
     *   "gender": "Female"
     * }
     *
     * Response 201:
     * {
     *   "id":                1,
     *   "name":              "Priya Sharma",
     *   "age":               27,
     *   "city":              "Mumbai, Maharashtra",
     *   "gender":            "Female",
     *   "profileCompletion": 50,
     *   "prakruti":          null,
     *   "vikriti":           null,
     *   "createdAt":         "2026-03-26T10:30:00"
     * }
     */
    @PostMapping("/profile")
    public ResponseEntity<?> createProfile(@Valid @RequestBody CreateProfileRequest req) {
        try {
            ProfileResponse response = service.createProfile(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to create profile. Please try again."));
        }
    }
 
    // ── READ ──────────────────────────────────────────────────────────────────
    /**
     * Called by Profile tab on every app open using stored userId.
     *
     * Response 200: ProfileResponse JSON (same shape as create)
     * Response 404: { "message": "Profile not found with id: X" }
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getProfile(id));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
 
    // ── UPDATE ────────────────────────────────────────────────────────────────
    /**
     * Called by Edit Profile button on the Profile tab.
     * Only name / age / city can be updated (gender is immutable).
     *
     * Response 200: Updated ProfileResponse
     * Response 404: profile not found
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(
        @PathVariable Long id,
        @RequestBody CreateProfileRequest req
    ) {
        try {
            return ResponseEntity.ok(service.updateProfile(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to update profile."));
        }
    }
 
    // ── DELETE ────────────────────────────────────────────────────────────────
    /**
     * Delete account. Called from a future account settings screen.
     *
     * Response 200: { "message": "Profile deleted successfully" }
     * Response 404: profile not found
     */
    @DeleteMapping("/profile/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            service.deleteProfile(id);
            return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
 
    // ── HEALTH ────────────────────────────────────────────────────────────────
    /**
     * Simple ping — used by the app to verify backend connectivity on launch.
     * GET /api/user/health → { "status": "ok" }
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Aavie User Profile API"));
    }
}
 
