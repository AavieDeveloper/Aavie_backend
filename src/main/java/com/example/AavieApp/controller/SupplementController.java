package com.example.AavieApp.controller;

import com.example.AavieApp.model.SupplementFormula;
import com.example.AavieApp.service.SupplementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Aavie — Supplement REST Controller
 *
 * Base URL: /api/supplement
 *
 * Endpoints consumed by the React Native app:
 *
 *   GET  /api/supplement/my-formula   → Returns the full personalised formula
 *                                        for the user identified by the
 *                                        "user-id" request header.
 *
 * The React Native call (from SupplementProduct.tsx):
 *
 *   fetch(`${API_BASE_URL}/api/supplement/my-formula`, {
 *     headers: { "user-id": storedId }
 *   })
 *
 * Response shape (SupplementFormula.java):
 * {
 *   "prakritiKey":  "PK",
 *   "vikritiKey":   "K",
 *   "conditions":   ["pcos"],
 *   "eyebrow":      "30-day custom formula",
 *   "name":         "Aavie Kapha Balance",
 *   "vikritiFocus": "Kapha",
 *   "tags":         ["Hormonal balance", "Cycle support", "Metabolic support", "PCOS support"],
 *   "ordered":      false,
 *   "herbs":        [ { "icon", "name", "desc", "dose", "adjusted" } ],
 *   "condHerbs":    [ { "name", "dose", "desc" } ],
 *   "dosage":       { "am", "amNote", "pm", "pmNote", "caution" }
 * }
 *
 * Error responses:
 *   400 → missing or invalid user-id header
 *   404 → profile not found, or Prakriti assessment not yet completed
 *   500 → unexpected server error
 */
@RestController
@RequestMapping("/api/supplement")
@CrossOrigin(origins = "*")   // Restrict to your app origin in production
public class SupplementController {

    private final SupplementService service;

    public SupplementController(SupplementService service) {
        this.service = service;
    }

    // ── GET /api/supplement/my-formula ────────────────────────────────────────
    @GetMapping("/my-formula")
    public ResponseEntity<?> getMyFormula(
        @RequestHeader(value = "user-id", required = false) String userIdHeader
    ) {
        // ── Validate header ────────────────────────────────────────────────
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Missing required header: user-id"));
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Header 'user-id' must be a valid numeric ID"));
        }

        // ── Build and return formula ───────────────────────────────────────
        try {
            SupplementFormula formula = service.getFormulaForUser(userId);
            return ResponseEntity.ok(formula);
        } catch (RuntimeException e) {
            // Covers: profile not found, Prakriti not done yet
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to build formula. Please try again."));
        }
    }
}