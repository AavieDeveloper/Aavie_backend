package com.example.AavieApp.controller;

import com.example.AavieApp.model.ManufacturingRun;
import com.example.AavieApp.repository.ManufacturingRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/manufacturing")
@CrossOrigin(origins = "*")
public class ManufacturingController {

    private final ManufacturingRunRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ManufacturingController(ManufacturingRunRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<?> saveManufacturing(@RequestBody Map<String, Object> req) {
        try {
            System.out.println("📦 Manufacturing Request received: " + req);
            
            Long userId = req.get("userId") != null ? 
                Long.valueOf(req.get("userId").toString()) : null;
            String assessmentType = (String) req.getOrDefault("assessmentType", "PCOS");
            
            // Convert formula and doshaPct to JSON strings
            String formulaJson = objectMapper.writeValueAsString(req.get("formula"));
            String doshaPctJson = objectMapper.writeValueAsString(req.get("doshaPct"));
            String severity = (String) req.getOrDefault("severity", "unknown");
            String prakriti = (String) req.getOrDefault("prakriti", "unknown");

            System.out.println("   formulaJson length: " + (formulaJson != null ? formulaJson.length() : 0));
            System.out.println("   doshaPctJson: " + doshaPctJson);

            // Find existing or create new
            ManufacturingRun run = repo
                .findByUserIdAndAssessmentType(userId, assessmentType)
                .orElse(new ManufacturingRun());

            run.setUserId(userId);
            run.setAssessmentType(assessmentType);
            run.setFormulaJson(formulaJson);
            run.setDoshaPct(doshaPctJson);
            run.setSeverity(severity);
            run.setPrakriti(prakriti);
            run.setRevealed(false);

            ManufacturingRun saved = repo.save(run);
            
            System.out.println("✅ Manufacturing data saved with ID: " + saved.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("saved", true, "id", saved.getId(), "revealed", false));
        } catch (Exception e) {
            System.err.println("❌ Failed to save manufacturing data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to save manufacturing data: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}/{type}")
    public ResponseEntity<?> getManufacturing(
        @PathVariable Long userId,
        @PathVariable String type
    ) {
        ManufacturingRun run = repo
            .findByUserIdAndAssessmentType(userId, type.toUpperCase())
            .orElse(null);

        if (run == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No manufacturing data found"));
        }

        if (!run.isRevealed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Premium subscription required"));
        }

        return ResponseEntity.ok(run);
    }

    @PutMapping("/reveal/{userId}")
    public ResponseEntity<?> revealAll(@PathVariable Long userId) {
        var runs = repo.findByUserIdOrderByCreatedAtDesc(userId);
        runs.forEach(run -> run.setRevealed(true));
        repo.saveAll(runs);
        return ResponseEntity.ok(Map.of("message", "Manufacturing data revealed"));
    }
}