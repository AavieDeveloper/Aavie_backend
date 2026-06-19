package com.example.AavieApp.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import java.util.*;

@RestController
@RequestMapping("/api/detection")
@CrossOrigin(origins = "*")
public class HumanDetectionController {

    // ✅ NEW SINGLE API - Does both human detection + body type
    private final String BODY_TYPE_PREDICT_API = 
        "http://88.222.212.15:8050/predict";

    /**
     * POST /api/detection/validate-and-forward
     * Sends image to single API that does both human detection + body type analysis.
     */
    @PostMapping("/validate-and-forward")
    public ResponseEntity<?> validateAndForward(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "age", required = false) String age,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "height_cm", required = false) String heightCm,
            @RequestParam(value = "weight_kg", required = false) String weightKg) {
        
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║  DEBUG: validate-and-forward called     ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("DEBUG: Image: " + image.getOriginalFilename() + 
            " (" + image.getSize() + " bytes)");
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = image.getBytes();
            
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename() != null ? 
                        image.getOriginalFilename() : "photo.jpg";
                }
            };
            
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imageResource);
            body.add("age", age != null ? age : "25");
            body.add("gender", gender != null ? gender : "0");
            body.add("person_height_cm", heightCm != null ? heightCm : "160");
            body.add("person_weight_kg", weightKg != null ? weightKg : "60");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);
            
            System.out.println("DEBUG: Sending to: " + BODY_TYPE_PREDICT_API);
            
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    BODY_TYPE_PREDICT_API, requestEntity, Map.class);
                
                Map<String, Object> result = response.getBody();
                System.out.println("DEBUG: API Result: " + result);
                
                if (result != null) {
                    String bodyType = (String) result.getOrDefault("body_type", "Unknown");
                    String ayurvedicType = (String) result.getOrDefault("ayurvedic_type", "Unknown");
                    
                    System.out.println("✅ Success - Body: " + bodyType + ", Ayurvedic: " + ayurvedicType);
                    
                    return ResponseEntity.ok()
                        .body(Map.of(
                            "valid", true,
                            "body_type", bodyType,
                            "ayurvedic_type", ayurvedicType,
                            "message", "✅ Body type detected: " + ayurvedicType
                        ));
                }
                
            } catch (HttpClientErrorException e) {
                String responseBody = e.getResponseBodyAsString();
                System.out.println("DEBUG: API rejected with STATUS: " + e.getStatusCode());
                System.out.println("DEBUG: API rejected with BODY: " + responseBody);
                
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = 
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> errorResult = mapper.readValue(responseBody, Map.class);
                    String detail = (String) errorResult.getOrDefault("detail", "Invalid image");
                    
                    System.out.println("❌ API says: " + detail);
                    
                    return ResponseEntity.ok()
                        .body(Map.of(
                            "valid", false,
                            "message", detail
                        ));
                } catch (Exception parseError) {
                    System.out.println("DEBUG: Could not parse error as JSON: " + parseError.getMessage());
                    return ResponseEntity.ok()
                        .body(Map.of(
                            "valid", false,
                            "message", responseBody
                        ));
                }
            }
            return ResponseEntity.ok()
                .body(Map.of("valid", false, "message", "Could not analyze image."));
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("valid", false, "message", "Error: " + e.getMessage()));
        }
    }
}
