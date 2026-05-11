package com.example.AavieApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assessments/cycle")
@CrossOrigin(origins = "*")
public class PackageController {

    @PostMapping("/package-add")
    public ResponseEntity<?> addPackage(@RequestBody PackageRequest req) {
        // TODO: Save package intent to database
        System.out.println("📦 Package add request for user: " + req.getUserId());
        return ResponseEntity.ok(Map.of("message", "Package added", "success", true));
    }

    public static class PackageRequest {
        private Long userId;
        private String packageType;
        private String severity;
        private boolean tongueCompleted;

        public Long getUserId()              { return userId; }
        public void setUserId(Long u)        { this.userId = u; }
        public String getPackageType()       { return packageType; }
        public void setPackageType(String p) { this.packageType = p; }
        public String getSeverity()          { return severity; }
        public void setSeverity(String s)    { this.severity = s; }
        public boolean isTongueCompleted()   { return tongueCompleted; }
        public void setTongueCompleted(boolean t) { this.tongueCompleted = t; }
    }
}
