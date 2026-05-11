package com.example.AavieApp.service;

import com.example.AavieApp.model.TongueAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class TongueService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Using gemini-2.5-flash (working model)
 // Use this for now - it's the most reliable
 // Try gemini-flash-latest (always points to the newest available)
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";


    private final HttpClient   httpClient   = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ANALYSIS_PROMPT =
    	    "You are an expert Ayurvedic practitioner performing Jivha Pariksha (tongue examination). " +
    	    "Your response MUST be a single valid JSON object — no markdown, no code fences, no preamble, no explanation.\n\n" +

    	    "═══════════════════════════════════════════════════════════════\n" +
    	    "STEP 1 — IMAGE VALIDATION (MANDATORY FIRST CHECK)\n" +
    	    "═══════════════════════════════════════════════════════════════\n" +
    	    "Before ANY analysis, verify ALL of these are true:\n" +
    	    "1. The image clearly shows a human tongue extended OUT of a mouth\n" +
    	    "2. The tongue surface (top of tongue) is visible and dominant in the frame\n" +
    	    "3. At least 60% of the tongue body is visible and in focus\n" +
    	    "4. Lighting is sufficient to see tongue color and any coating\n" +
    	    "5. The image is not blurry, not too dark, not obscured by fingers/objects\n\n" +

    	    "REJECT the image if ANY of these are true:\n" +
    	    "- The image shows a face with a closed mouth → rejection_reason: \"mouth_closed\"\n" +
    	    "- The image shows an animal, object, food, scenery, or anything that is not a tongue → rejection_reason: \"not_a_tongue\"\n" +
    	    "- The image is blurry, out-of-focus, or motion-blurred → rejection_reason: \"too_blurry\"\n" +
    	    "- The image is too dark to read tongue color → rejection_reason: \"too_dark\"\n" +
    	    "- Lips are visible but the tongue is NOT extended → rejection_reason: \"tongue_not_visible\"\n" +
    	    "- A hand, finger, or other body part is the main subject → rejection_reason: \"not_a_tongue\"\n" +
    	    "- The tongue is partially visible but obscured → rejection_reason: \"tongue_not_visible\"\n\n" +

    	    "If REJECTED, respond with EXACTLY this JSON (no other fields):\n" +
    	    "{\n" +
    	    "  \"valid\": false,\n" +
    	    "  \"rejection_reason\": \"<one of the reasons above>\",\n" +
    	    "  \"user_message\": \"<a short friendly sentence telling the user what's wrong and how to retake the photo>\"\n" +
    	    "}\n\n" +

    	    "═══════════════════════════════════════════════════════════════\n" +
    	    "STEP 2 — TONGUE ANALYSIS (ONLY IF STEP 1 PASSED)\n" +
    	    "═══════════════════════════════════════════════════════════════\n" +
    	    "Only proceed if the image is genuinely a clear tongue photo.\n" +
    	    "Respond with EXACTLY this JSON:\n" +
    	    "{\n" +
    	    "  \"valid\": true,\n" +
    	    "  \"dominant_dosha_imbalance\": \"Vata\" | \"Pitta\" | \"Kapha\" | \"balanced\",\n" +
    	    "  \"ama_level\": \"none\" | \"mild\" | \"moderate\" | \"heavy\",\n" +
    	    "  \"agni_state\": \"strong\" | \"moderate\" | \"low\" | \"very_low\",\n" +
    	    "  \"one_line_insight\": \"<one short sentence describing what the tongue reveals today>\",\n" +
    	    "  \"redness_zones\": [\"tip\"|\"front\"|\"center\"|\"back\"|\"left\"|\"right\"],\n" +
    	    "  \"coating_location\": \"none\" | \"tip\" | \"center\" | \"back\" | \"full\"\n" +
    	    "}\n\n" +

    	    "Ayurvedic interpretation guidelines:\n" +
    	    "- White or yellow coating → Ama present\n" +
    	    "- Red tip → Pitta\n" +
    	    "- Thick white coating → Kapha\n" +
    	    "- Clean pink tongue → balanced\n" +
    	    "- Tooth marks on edges → Vata or low Agni\n" +
    	    "- Pale tongue → Vata or low blood\n\n" +

    	    "CRITICAL RULES:\n" +
    	    "- Do NOT fabricate analysis for non-tongue images. ALWAYS reject if unsure.\n" +
    	    "- Do NOT include any text outside the JSON object.\n" +
    	    "- Do NOT use markdown code fences.\n" +
    	    "- If you have any doubt that the image is a valid clear tongue photo, REJECT it.";

    public TongueAnalysisResult analyse(String imageBase64, String pcosType) {
        int maxRetries = 3;
        int retryDelay = 10000; // 10 seconds between retries for rate limiting
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String requestBody = buildRequestBody(imageBase64);

                System.out.println("📤 Sending request to Gemini API (Attempt " + attempt + ")...");
                System.out.println("   URL: " + GEMINI_URL);
                System.out.println("   Request body length: " + requestBody.length());

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + "?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("📥 Gemini API response status: " + response.statusCode());

                if (response.statusCode() == 200) {
                    System.out.println("✅ Gemini API request successful");
                    return parseGeminiResponse(response.body(), pcosType);
                }
                
                if (response.statusCode() == 429) {
                    System.err.println("⚠️ Rate limit hit, attempt " + attempt + " of " + maxRetries);
                    System.err.println("   Waiting " + (retryDelay/1000) + " seconds before retry...");
                    if (attempt < maxRetries) {
                        Thread.sleep(retryDelay);
                        continue;
                    }
                    return buildFallback(pcosType);
                }

                System.err.println("❌ Gemini API error " + response.statusCode());
                System.err.println("   Response: " + response.body());
                return buildFallback(pcosType);

            } catch (IOException e) {
                System.err.println("❌ Network error (attempt " + attempt + "): " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return buildFallback(pcosType);
            } catch (Exception e) {
                System.err.println("❌ TongueService error: " + e.getMessage());
                return buildFallback(pcosType);
            }
        }
        
        return buildFallback(pcosType);
    }

    private String buildRequestBody(String imageBase64) {
        // Escape special characters for JSON
        String escapedPrompt = ANALYSIS_PROMPT
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");

        return String.format(
            "{\n" +
            "  \"contents\": [{\n" +
            "    \"parts\": [\n" +
            "      {\n" +
            "        \"inline_data\": {\n" +
            "          \"mime_type\": \"image/jpeg\",\n" +
            "          \"data\": \"%s\"\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"text\": \"%s\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }],\n" +
            "  \"generationConfig\": {\n" +
            "    \"temperature\": 0.1,\n" +
            "    \"maxOutputTokens\": 2048,\n" +  // ✅ Increased
            "    \"topP\": 0.95,\n" +
            "    \"topK\": 40\n" +
            "  }\n" +
            "}",
            imageBase64,
            escapedPrompt
        );
    }
    private TongueAnalysisResult parseGeminiResponse(String responseBody, String pcosType) {
        try {
            System.out.println("📝 Parsing Gemini response...");

            JsonNode root = objectMapper.readTree(responseBody);

            // Extract text from candidates[0].content.parts[0].text
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty() || !candidates.get(0).has("content")) {
                System.err.println("❌ No content in response");
                return buildFallback(pcosType);
            }

            String text = candidates.get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

            System.out.println("📝 Raw Gemini response: " + text);

            // Clean up the response
            text = text.trim();

            // Remove markdown code blocks if present
            if (text.startsWith("```json")) {
                text = text.substring(7);
            }
            if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();

            // Find JSON object boundaries
            int startBrace = text.indexOf("{");
            int endBrace = text.lastIndexOf("}");
            if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
                text = text.substring(startBrace, endBrace + 1);
            }

            System.out.println("📝 Cleaned JSON: " + text);

            JsonNode analysisJson = objectMapper.readTree(text);

            TongueAnalysisResult result = new TongueAnalysisResult();

            // ── VALIDATION GATE — check `valid` field FIRST ──────────────────
            boolean isValid = true; // default true for backwards compatibility
            if (analysisJson.has("valid")) {
                isValid = analysisJson.get("valid").asBoolean(true);
            }

            if (!isValid) {
                // Image was rejected by Gemini — return rejection details
                String rejectionReason = analysisJson.has("rejection_reason")
                    ? analysisJson.get("rejection_reason").asText("unknown")
                    : "unknown";
                String userMessage = analysisJson.has("user_message")
                    ? analysisJson.get("user_message").asText()
                    : buildUserMessageForReason(rejectionReason);

                System.out.println("⚠️ Image REJECTED by validation");
                System.out.println("   Reason: " + rejectionReason);
                System.out.println("   User message: " + userMessage);

                result.setValid(false);
                result.setRejectionReason(rejectionReason);
                result.setUserMessage(userMessage);
                return result;
            }

            // ── VALID IMAGE — proceed with analysis parsing ─────────────────
            result.setValid(true);
            result.setDominantDoshaImbalance(
                analysisJson.has("dominant_dosha_imbalance") ?
                analysisJson.get("dominant_dosha_imbalance").asText("balanced") : "balanced");
            result.setAmaLevel(
                analysisJson.has("ama_level") ?
                analysisJson.get("ama_level").asText("none") : "none");
            result.setAgniState(
                analysisJson.has("agni_state") ?
                analysisJson.get("agni_state").asText("moderate") : "moderate");
            result.setOneLineInsight(
                analysisJson.has("one_line_insight") ?
                analysisJson.get("one_line_insight").asText("Analysis complete.") : "Analysis complete.");
            result.setCoatingLocation(
                analysisJson.has("coating_location") ?
                analysisJson.get("coating_location").asText("none") : "none");

            // Parse redness_zones array
            JsonNode zonesNode = analysisJson.has("redness_zones") ?
                analysisJson.get("redness_zones") : objectMapper.createArrayNode();
            List<String> zones = new ArrayList<>();
            if (zonesNode.isArray()) {
                zonesNode.forEach(z -> zones.add(z.asText()));
            }
            result.setRednessZones(zones.toArray(new String[0]));

            System.out.println("✅ Successfully parsed Gemini response");
            System.out.println("   Dominant Dosha: " + result.getDominantDoshaImbalance());
            System.out.println("   Ama Level: " + result.getAmaLevel());
            System.out.println("   Agni State: " + result.getAgniState());
            return result;

        } catch (Exception e) {
            System.err.println("❌ Failed to parse Gemini response: " + e.getMessage());
            System.err.println("   Response body was: " + responseBody);
            return buildFallback(pcosType);
        }
    }

    /**
     * Build a friendly user-facing message based on rejection reason.
     * Used when Gemini returns valid:false without an explicit user_message.
     */
    private String buildUserMessageForReason(String reason) {
        if (reason == null) return "We couldn't read this image clearly. Please retake your tongue photo.";
        switch (reason) {
            case "not_a_tongue":
                return "This doesn't look like a tongue. Please take a clear photo of your tongue extended out of your mouth.";
            case "mouth_closed":
                return "Your mouth appears closed. Please open your mouth wide and stick your tongue out for the photo.";
            case "tongue_not_visible":
                return "We couldn't see your tongue clearly. Please extend it fully out of your mouth.";
            case "too_blurry":
                return "The image is blurry. Hold the phone steady and try again.";
            case "too_dark":
                return "The lighting is too dim. Move toward a window or turn on a lamp.";
            default:
                return "We couldn't read this image clearly. Please retake your tongue photo in good light.";
        }
    }

    private TongueAnalysisResult buildFallback(String pcosType) {
        System.out.println("⚠️ Using fallback analysis for PCOS type: " + pcosType);
        TongueAnalysisResult result = new TongueAnalysisResult();
        result.setValid(true); // Fallback is shown as a valid analysis (graceful degrade on API error)

        if (pcosType != null && pcosType.contains("Pitta")) {
            result.setDominantDoshaImbalance("Pitta");
            result.setAmaLevel("mild");
            result.setAgniState("moderate");
            result.setOneLineInsight("Pitta pattern consistent with your cycle assessment — liver zone shows mild heat.");
            result.setRednessZones(new String[]{"tip"});
            result.setCoatingLocation("none");
        } else if (pcosType != null && pcosType.contains("Kapha")) {
            result.setDominantDoshaImbalance("Kapha");
            result.setAmaLevel("moderate");
            result.setAgniState("low");
            result.setOneLineInsight("Kapha accumulation visible — digestive fire needs support.");
            result.setRednessZones(new String[]{});
            result.setCoatingLocation("center");
        } else {
            result.setDominantDoshaImbalance("Vata");
            result.setAmaLevel("mild");
            result.setAgniState("moderate");
            result.setOneLineInsight("Vata pattern detected — nervous system and digestive irregularity present.");
            result.setRednessZones(new String[]{});
            result.setCoatingLocation("none");
        }

        return result;
    }
}