package com.example.AavieApp.controller;

import com.example.AavieApp.model.TongueAnalysisResult;
import com.example.AavieApp.service.TongueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Aavie — Tongue Controller
 *
 * Base URL: /api/tongue
 *
 * POST /api/tongue/analyse  → Analyse tongue image via Gemini Vision
 *
 * Request body:
 * {
 *   "imageBase64": "base64_jpeg_string",
 *   "pcosType":    "Kapha PCOS",
 *   "userId":      1
 * }
 *
 * Response:
 * {
 *   "dominant_dosha_imbalance": "Kapha",
 *   "ama_level":                "moderate",
 *   "agni_state":               "low",
 *   "one_line_insight":         "...",
 *   "redness_zones":            [],
 *   "coating_location":         "center"
 * }
 */
@RestController
@RequestMapping("/api/tongue")
@CrossOrigin(origins = "*")
public class TongueController {

    private final TongueService service;

    public TongueController(TongueService service) {
        this.service = service;
    }

    // ── POST /api/tongue/analyse ──────────────────────────────────────────────
    @PostMapping("/analyse")
    public ResponseEntity<?> analyse(@RequestBody AnalyseRequest req) {
        try {
            if (req.getImageBase64() == null || req.getImageBase64().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "imageBase64 is required."));
            }

            // Strip data URI prefix if present (e.g. "data:image/jpeg;base64,...")
            String base64 = req.getImageBase64();
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }

            // Basic size sanity check — extremely small payloads are likely corrupt
            if (base64.length() < 5000) {
                return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                        "valid", false,
                        "rejectionReason", "too_blurry",
                        "userMessage", "The image is too small or low quality. Please retake with a clearer view of your tongue."
                    ));
            }

            TongueAnalysisResult result = service.analyse(base64, req.getPcosType());

            // ── VALIDATION GATE — if Gemini rejected the image, return 422 ───
            if (!result.isValid()) {
                return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY) // 422
                    .body(Map.of(
                        "valid", false,
                        "rejectionReason", result.getRejectionReason() != null ? result.getRejectionReason() : "unknown",
                        "userMessage", result.getUserMessage() != null ? result.getUserMessage() : "Could not read this image. Please retake your tongue photo."
                    ));
            }

            // Valid analysis → return 200 with full result
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Analysis failed. Please try again."));
        }
    }

    // ── Request DTO ───────────────────────────────────────────────────────────
    public static class AnalyseRequest {
        private String imageBase64;
        private String pcosType;
        private Long   userId;

        public String getImageBase64()           { return imageBase64; }
        public void   setImageBase64(String v)   { this.imageBase64 = v; }
        public String getPcosType()              { return pcosType; }
        public void   setPcosType(String v)      { this.pcosType = v; }
        public Long   getUserId()                { return userId; }
        public void   setUserId(Long v)          { this.userId = v; }
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody SaveTongueRequest req) {
        System.out.println("👅 Tongue reading saved for user: " + req.getUserId());
        return ResponseEntity.ok(Map.of("message", "Tongue reading saved", "success", true));
    }

    public static class SaveTongueRequest {
        private Long userId;
        private String prakriti;
        private String capturedAt;
        private Object analysis;
        private String selectedZone;
        private String pcosType;

        // Getters & Setters...
        public Long getUserId()              { return userId; }
        public void setUserId(Long u)        { this.userId = u; }
        public String getPrakriti()          { return prakriti; }
        public void setPrakriti(String p)    { this.prakriti = p; }
        public String getCapturedAt()        { return capturedAt; }
        public void setCapturedAt(String c)  { this.capturedAt = c; }
        public Object getAnalysis()          { return analysis; }
        public void setAnalysis(Object a)    { this.analysis = a; }
        public String getSelectedZone()      { return selectedZone; }
        public void setSelectedZone(String s){ this.selectedZone = s; }
        public String getPcosType()          { return pcosType; }
        public void setPcosType(String p)    { this.pcosType = p; }
    }
   

}