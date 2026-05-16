package com.example.AavieApp.controller;

import com.example.AavieApp.service.UserProfileService.CreateProfileRequest;
import com.example.AavieApp.service.UserProfileService.ProfileResponse;
import com.example.AavieApp.service.UserProfileService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")   // Replace with specific origin in production
public class UserProfileController {
 
    private final UserProfileService service;
 
    public UserProfileController(UserProfileService service) {
        this.service = service;
    }
 
    
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
 
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Aavie User Profile API"));
    }
}
 
