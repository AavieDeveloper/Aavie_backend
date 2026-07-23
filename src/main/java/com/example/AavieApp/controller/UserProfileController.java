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
    
    
    @GetMapping("/profile/filter-meta")
    public ResponseEntity<?> getUserFilterMeta() {
        try {
            return ResponseEntity.ok(service.getUserFilterMeta());
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch filter data."));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getAllProfiles() {
        try {
            return ResponseEntity.ok(service.getAllProfiles());
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch users."));
        }
    }
 
   
   @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(
        @PathVariable Long id,
        org.springframework.security.core.Authentication authentication,
        jakarta.servlet.http.HttpServletRequest request
    ) {
        try {
            Object requesterIdAttr = request.getAttribute("userId");
            boolean isSelf = requesterIdAttr != null
                && id.equals(((Number) requesterIdAttr).longValue());
            boolean isPrivilegedRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_ORDER_ADMIN"));

            if (!isSelf && !isPrivilegedRole) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You may only view your own profile."));
            }

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
    
    
    @PostMapping("/push-token")
    public ResponseEntity<?> savePushToken(@RequestBody Map<String, Object> body,
                                            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String token = (String) body.get("token");
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token is required"));
            }
            service.savePushToken(userId, token);
            return ResponseEntity.ok(Map.of("saved", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to save token"));
        }
    }
}