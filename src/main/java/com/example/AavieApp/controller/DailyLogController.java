package com.example.AavieApp.controller;

import com.example.AavieApp.service.DailyLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class DailyLogController {

    private final DailyLogService service;

    public DailyLogController(DailyLogService service) {
        this.service = service;
    }

    /**
     * POST /api/logs/save
     * Saves or updates today's mood/body/behaviour log.
     * Called when user taps "Save today →"
     */
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody DailyLogService.SaveLogRequest req) {
        try {
            return ResponseEntity.ok(service.saveLog(req));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/logs/{userId}/date/{date}
     * e.g. GET /api/logs/1/date/2026-04-09
     * Loads a specific day's log (for history sheet).
     */
    @GetMapping("/{userId}/date/{date}")
    public ResponseEntity<?> getByDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        var log = service.getLog(userId, date);
        if (log == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(log);
    }

    /**
     * GET /api/logs/{userId}/range?from=2026-03-01&to=2026-03-31
     * Returns all logs in a date range for the history/calendar view.
     */
    @GetMapping("/{userId}/range")
    public ResponseEntity<?> getRange(
            @PathVariable Long userId,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(service.getLogs(userId, from, to));
    }

    /**
     * GET /api/logs/{userId}/latest
     * Returns the most recent log — consumed by Profile health stats.
     */
    @GetMapping("/{userId}/latest")
    public ResponseEntity<?> getLatest(@PathVariable Long userId) {
        var log = service.getLatestLog(userId);
        if (log == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(log);
    }
}