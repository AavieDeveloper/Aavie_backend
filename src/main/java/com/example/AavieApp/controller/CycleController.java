package com.example.AavieApp.controller;

import com.example.AavieApp.service.CycleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/cycle")
@CrossOrigin(origins = "*")
public class CycleController {

    private final CycleService service;

    public CycleController(CycleService service) {
        this.service = service;
    }

    /**
     * GET /api/cycle/{userId}/current
     * Returns current cycle day, phase, next period date.
     * Used by both the cycle calendar AND profile health stats.
     */
    @GetMapping("/{userId}/current")
    public ResponseEntity<?> getCurrent(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(service.getCurrentCycle(userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/cycle/period-start
     * Body: { "userId": 1, "periodStartDate": "2026-04-01" }
     * Called when user taps "My period started today" toggle.
     */
    @PostMapping("/period-start")
    public ResponseEntity<?> markPeriodStart(
            @RequestBody CycleService.PeriodStartRequest req) {
        try {
            var result = service.markPeriodStart(
                req.getUserId(),
                LocalDate.parse(req.getPeriodStartDate()));
            return ResponseEntity.ok(Map.of(
                "message", "Cycle reset successfully",
                "periodStartDate", result.getPeriodStartDate().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", e.getMessage()));
        }
    }
}