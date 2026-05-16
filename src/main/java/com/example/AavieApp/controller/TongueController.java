package com.example.AavieApp.controller;

import com.example.AavieApp.model.TongueAnalysisResult;
import com.example.AavieApp.model.TongueReading;
import com.example.AavieApp.repository.TongueReadingRepository;
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
    private final TongueReadingRepository tongueRepo;

    public TongueController(TongueService service, TongueReadingRepository tongueRepo) {
        this.service = service;
        this.tongueRepo = tongueRepo;
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
        try {
            System.out.println("👅 Saving tongue reading for user: " + req.getUserId());

            TongueReading reading = new TongueReading();
            reading.setUserId(req.getUserId());
            reading.setPrakriti(req.getPrakriti());
            reading.setPcosType(req.getPcosType());
            reading.setSelectedZone(req.getSelectedZone());

            // Extract analysis fields
            if (req.getAnalysis() instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> analysis = (java.util.Map<String, Object>) req.getAnalysis();
                reading.setDominantDoshaImbalance((String) analysis.get("dominant_dosha_imbalance"));
                reading.setAmaLevel((String) analysis.get("ama_level"));
                reading.setAgniState((String) analysis.get("agni_state"));
                reading.setOneLineInsight((String) analysis.get("one_line_insight"));
                reading.setCoatingLocation((String) analysis.get("coating_location"));
            }

            tongueRepo.save(reading);
            System.out.println("✅ Tongue reading saved successfully");
            return ResponseEntity.ok(Map.of("message", "Tongue reading saved", "success", true));
        } catch (Exception e) {
            System.err.println("❌ Tongue save error: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Failed to save tongue reading"));
        }
    }

    @GetMapping("/latest/{userId}")
    public ResponseEntity<?> getLatest(@PathVariable Long userId) {
        return tongueRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
            .map(t -> ResponseEntity.ok(t))
            .orElse(ResponseEntity.notFound().build());
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