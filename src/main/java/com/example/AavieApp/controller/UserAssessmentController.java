package com.example.AavieApp.controller;

import com.example.AavieApp.service.UserAssessmentService;
import com.example.AavieApp.service.UserAssessmentService.AssessmentResponse;
import com.example.AavieApp.service.UserAssessmentService.AssessmentStatusResponse;
import com.example.AavieApp.service.UserAssessmentService.SubmitAssessmentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Aavie — Assessment REST Controller
 *
 * Base URL: /api/assessments
 *
 * Endpoints:
 *
 *   POST   /api/assessments/submit            → Submit any assessment result
 *   GET    /api/assessments/status/{userId}   → Get completion status + next required
 *   GET    /api/assessments/{userId}/{type}   → Get one assessment result
 */
@RestController
@RequestMapping("/api/assessments")
@CrossOrigin(origins = "*")
public class UserAssessmentController {

    private final UserAssessmentService service;

    public UserAssessmentController(UserAssessmentService service) {
        this.service = service;
    }

    // ── SUBMIT ────────────────────────────────────────────────────────────────
    /**
     * Submit any assessment result (PRAKRITI, PCOS, or VIKRITI).
     * Enforces strict order — returns 403 if prerequisite is missing.
     *
     * Request body example (PRAKRITI):
     * {
     *   "userId":         1,
     *   "assessmentType": "PRAKRITI",
     *   "resultType":     "Pitta-Kapha",
     *   "confidenceScore": 78,
     *   "scoreVata":      3,
     *   "scorePitta":     8,
     *   "scoreKapha":     6
     * }
     *
     * Request body example (PCOS):
     * {
     *   "userId":          1,
     *   "assessmentType":  "PCOS",
     *   "resultType":      "Kapha PCOS",
     *   "severity":        "moderate",
     *   "pcosConfidence":  68,
     *   "cycleScore":      52,
     *   "prakritiKey":     "PK",
     *   "scoreVata":       4,
     *   "scorePitta":      6,
     *   "scoreKapha":      10
     * }
     *
     * Request body example (VIKRITI):
     * {
     *   "userId":          1,
     *   "assessmentType":  "VIKRITI",
     *   "resultType":      "Kapha Vikriti",
     *   "agniType":        "Manda",
     *   "scoreVata":       3,
     *   "scorePitta":      5,
     *   "scoreKapha":      11,
     *   "energyScore":     65,
     *   "sleepScore":      40,
     *   "stressScore":     55,
     *   "metabolicScore":  72,
     *   "liverScore":      38,
     *   "prakritiKey":     "PK"
     * }
     *
     * Response 201: AssessmentResponse (includes nextAssessment, prakritiDone, pcosDone, vikritiDone)
     * Response 403: { "message": "You must complete Prakriti first." }
     * Response 400: { "message": "Invalid assessment type." }
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(@RequestBody SubmitAssessmentRequest req) {
        try {
            AssessmentResponse response = service.submitAssessment(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            // Order not followed
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage(), "error", "ORDER_VIOLATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to save assessment. Please try again."));
        }
    }

    // ── STATUS ────────────────────────────────────────────────────────────────
    /**
     * Get a user's assessment completion status.
     * Called by the Home screen on load to decide which assessment to show/lock.
     *
     * Response 200:
     * {
     *   "prakritiDone":   true,
     *   "pcosDone":       false,
     *   "vikritiDone":    false,
     *   "nextAssessment": "PCOS",
     *   "completedCount": 1,
     *   "prakritiResult": "Pitta-Kapha",
     *   "pcosResult":     null,
     *   "pcosSeverity":   null,
     *   "vikritiResult":  null,
     *   "agniType":       null
     * }
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getStatus(@PathVariable Long userId) {
        try {
            AssessmentStatusResponse status = service.getStatus(userId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch assessment status."));
        }
    }

    // ── GET ONE ───────────────────────────────────────────────────────────────
    /**
     * Get a specific assessment result for a user.
     * type must be "PRAKRITI", "PCOS", or "VIKRITI".
     *
     * GET /api/assessments/1/PRAKRITI
     * GET /api/assessments/1/PCOS
     * GET /api/assessments/1/VIKRITI
     *
     * Response 200: AssessmentResponse
     * Response 404: { "message": "PCOS assessment not found for user 1" }
     */
    @GetMapping("/{userId}/{type}")
    public ResponseEntity<?> getAssessment(
        @PathVariable Long userId,
        @PathVariable String type
    ) {
        try {
            AssessmentResponse response = service.getAssessment(userId, type.toUpperCase());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    // ── CAN-TAKE CHECK ────────────────────────────────────────────────────────
    /**
     * Quick check — can this user take a given assessment?
     * Used by the frontend to show lock/unlock state without full status fetch.
     *
     * GET /api/assessments/can-take/1/PCOS
     *
     * Response 200: { "canTake": true, "reason": null }
     * Response 200: { "canTake": false, "reason": "Complete Prakriti assessment first." }
     */
    @GetMapping("/can-take/{userId}/{type}")
    public ResponseEntity<?> canTake(
        @PathVariable Long userId,
        @PathVariable String type
    ) {
        try {
            AssessmentStatusResponse status = service.getStatus(userId);
            boolean canTake;
            String reason = null;

            switch (type.toUpperCase()) {
                case "PRAKRITI":
                    canTake = true;
                    break;
                case "PCOS":
                    canTake = status.isPrakritiDone();
                    if (!canTake) reason = "Complete the Prakriti assessment first.";
                    break;
                case "VIKRITI":
                    canTake = status.isPrakritiDone() && status.isPcosDone();
                    if (!canTake) {
                        reason = !status.isPrakritiDone()
                            ? "Complete the Prakriti assessment first."
                            : "Complete the PCOS assessment first.";
                    }
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid type."));
            }

            return ResponseEntity.ok(Map.of("canTake", canTake, "reason", reason != null ? reason : ""));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to check assessment eligibility."));
        }
    }
    
    
    // ── NAVIGATE NEXT ───────────────────────────────────────────────
    @PostMapping("/navigate-next")
    public ResponseEntity<?> navigateNext(@RequestBody NavigateRequest req) {
        System.out.println("🧭 Navigate next: " + req.getCurrentAssessment() + " → " + req.getNextAssessment());
        return ResponseEntity.ok(Map.of(
            "navigatedTo", req.getNextAssessment(),
            "currentAssessment", req.getCurrentAssessment(),
            "success", true
        ));
    }

    // ── DTO ─────────────────────────────────────────────────────────
    public static class NavigateRequest {
        private Long userId;
        private String currentAssessment;
        private String nextAssessment;

        public Long getUserId()                    { return userId; }
        public void setUserId(Long u)              { this.userId = u; }
        public String getCurrentAssessment()       { return currentAssessment; }
        public void setCurrentAssessment(String c) { this.currentAssessment = c; }
        public String getNextAssessment()          { return nextAssessment; }
        public void setNextAssessment(String n)    { this.nextAssessment = n; }
    }
}
    
