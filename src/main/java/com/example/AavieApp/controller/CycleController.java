package com.example.AavieApp.controller;

import com.example.AavieApp.service.CycleService;
import com.example.AavieApp.service.CycleService.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Aavie — CycleController (v2)
 *
 * Endpoints:
 *
 *  GET  /api/cycle/state/{userId}
 *       Returns full live cycle state including cycleId, cycleNumber,
 *       cycleDay, phase, nextPeriodDate, loggedDays, symptom stats.
 *       Frontend uses cycleId from this response for all subsequent calls.
 *
 *  GET  /api/cycle/marks/{userId}?year=2026&month=6
 *       Returns all marks for a calendar month.
 *
 *  POST /api/cycle/marks
 *       Batch upsert/delete marks. Automatically creates/closes cycles
 *       when a period mark is saved.
 *
 *  POST /api/cycle/daily-log
 *       Save the 6-step daily log. Requires cycleId in the body
 *       (get it from GET /state first).
 *
 *  GET  /api/cycle/daily-log/{userId}?date=2026-06-04
 *       Fetch the log for a specific date. Returns 204 if none exists.
 */
@RestController
@RequestMapping("/api/cycle")
@CrossOrigin(origins = "*")
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/cycle/state/{userId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/state/{userId}")
    public ResponseEntity<?> getCycleState(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(cycleService.getCycleState(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to load cycle state: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/cycle/marks/{userId}?year=2026&month=6
    //  month is 1-based (June = 6)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/marks/{userId}")
    public ResponseEntity<?> getMarksForMonth(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            return ResponseEntity.ok(cycleService.getMarksForMonth(userId, year, month));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to load marks: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/cycle/marks
    //  Body: { "userId": 1, "marks": { "2026-06-04": 1, "2026-06-05": 0 } }
    //  Value 0 = delete that date's mark
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/marks")
    public ResponseEntity<?> saveMarks(@RequestBody SaveMarksRequest req) {
        try {
            if (req.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "userId is required"));
            }
            cycleService.saveMarks(req);
            return ResponseEntity.ok(Map.of("message", "Marks saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to save marks: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/cycle/daily-log
    //  Body: { userId, cycleId, date, day, cycleDay, phase,
    //          energy, discharge[], mood, zones{}, behaviours[], character }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/daily-log")
    public ResponseEntity<?> saveDailyLog(@RequestBody SaveDailyLogRequest req) {
        try {
            if (req.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "userId is required"));
            }
            if (req.getDate() == null || req.getDate().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "date is required"));
            }
            DailyLogResponse saved = cycleService.saveDailyLog(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            // e.g. "No active cycle found"
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to save daily log: " + e.getMessage()));
        }
    }
    
 // GET /api/cycle/insights/{userId}
    @GetMapping("/insights/{userId}")
    public ResponseEntity<?> getInsights(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(cycleService.getInsights(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to load insights: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/cycle/daily-log/{userId}?date=2026-06-04
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/daily-log/{userId}")
    public ResponseEntity<?> getLogForDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailyLogResponse log = cycleService.getLogForDate(userId, date);
            if (log == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to load log: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Health check
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Aavie Cycle API v2"));
    }
}