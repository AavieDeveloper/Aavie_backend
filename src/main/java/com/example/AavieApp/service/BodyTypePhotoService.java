package com.example.AavieApp.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class BodyTypePhotoService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PHOTO_API_URL = "http://88.222.212.15:8050/predict";

    /**
     * Analyze body photo and return dosha scores
     */
    public BodyTypeHybridService.PhotoResult analyzePhoto(
            MultipartFile image, int age, int gender, 
            double heightCm, double weightKg) {
        
        try {
            // Prepare multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            body.add("age", age);
            body.add("gender", gender);
            body.add("person_height_cm", heightCm);
            body.add("person_weight_kg", weightKg);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);

            // Call external API
            ResponseEntity<Map> response = restTemplate.exchange(
                PHOTO_API_URL,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            // Parse response
            Map<String, Object> result = response.getBody();
            return parsePhotoResponse(result);

        } catch (Exception e) {
            // If photo analysis fails, return neutral scores
            return new BodyTypeHybridService.PhotoResult(
                "Unknown",
                new BodyTypeHybridService.DoshaScores(33.33, 33.33, 33.34),
                50.0
            );
        }
    }

    private BodyTypeHybridService.PhotoResult parsePhotoResponse(Map<String, Object> result) {
        // Adjust this based on actual API response format
        String bodyType = (String) result.getOrDefault("body_type", "Unknown");
        double confidence = ((Number) result.getOrDefault("confidence", 50)).doubleValue();
        
        Map<String, Number> scores = (Map<String, Number>) result.get("dosha_scores");
        
        BodyTypeHybridService.DoshaScores doshaScores;
        if (scores != null) {
            doshaScores = new BodyTypeHybridService.DoshaScores(
                scores.getOrDefault("vata", 33.33).doubleValue(),
                scores.getOrDefault("pitta", 33.33).doubleValue(),
                scores.getOrDefault("kapha", 33.34).doubleValue()
            );
        } else {
            // Fallback mapping based on body type
            doshaScores = mapBodyTypeToScores(bodyType);
        }
        
        return new BodyTypeHybridService.PhotoResult(bodyType, doshaScores, confidence);
    }

    private BodyTypeHybridService.DoshaScores mapBodyTypeToScores(String bodyType) {
        switch (bodyType.toLowerCase()) {
            case "vata": return new BodyTypeHybridService.DoshaScores(60, 20, 20);
            case "pitta": return new BodyTypeHybridService.DoshaScores(20, 60, 20);
            case "kapha": return new BodyTypeHybridService.DoshaScores(20, 20, 60);
            case "vata-pitta": return new BodyTypeHybridService.DoshaScores(45, 45, 10);
            case "pitta-kapha": return new BodyTypeHybridService.DoshaScores(10, 45, 45);
            case "vata-kapha": return new BodyTypeHybridService.DoshaScores(45, 10, 45);
            default: return new BodyTypeHybridService.DoshaScores(33.33, 33.33, 33.34);
        }
    }
}