package com.example.AavieApp.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BodyTypeHybridService {

    // Weight configuration
    private static final double QUIZ_WEIGHT = 0.40;  // 40%
    private static final double PHOTO_WEIGHT = 0.60; // 60%

    /**
     * Combine quiz and photo results to get final body type
     */
    public HybridResult combineResults(QuizResult quizResult, PhotoResult photoResult) {
        
        // 1. Calculate weighted dosha scores
        DoshaScores finalScores = calculateWeightedScores(
            quizResult.getScores(), 
            photoResult.getScores()
        );
        
        // 2. Determine body type from combined scores
        String bodyType = determineBodyType(finalScores);
        
        // 3. Calculate confidence
        double confidence = calculateConfidence(quizResult, photoResult, finalScores);
        
        // 4. Generate explanation
        String explanation = generateExplanation(quizResult, photoResult, finalScores);
        
        return new HybridResult(
            bodyType,
            finalScores,
            confidence,
            explanation,
            quizResult.getBodyType(),
            photoResult.getBodyType()
        );
    }

    private DoshaScores calculateWeightedScores(DoshaScores quiz, DoshaScores photo) {
        return new DoshaScores(
            (quiz.getVata() * QUIZ_WEIGHT) + (photo.getVata() * PHOTO_WEIGHT),
            (quiz.getPitta() * QUIZ_WEIGHT) + (photo.getPitta() * PHOTO_WEIGHT),
            (quiz.getKapha() * QUIZ_WEIGHT) + (photo.getKapha() * PHOTO_WEIGHT)
        );
    }

    private String determineBodyType(DoshaScores scores) {
        double total = scores.getVata() + scores.getPitta() + scores.getKapha();
        double vataPct = scores.getVata() / total;
        double pittaPct = scores.getPitta() / total;
        double kaphaPct = scores.getKapha() / total;
        
        double threshold = 0.35; // 35% for dominance
        
        boolean vataDom = vataPct >= threshold;
        boolean pittaDom = pittaPct >= threshold;
        boolean kaphaDom = kaphaPct >= threshold;
        
        if (vataDom && pittaDom && kaphaDom) return "Tridoshic";
        if (vataDom && pittaDom) return "Vata-Pitta";
        if (pittaDom && kaphaDom) return "Pitta-Kapha";
        if (vataDom && kaphaDom) return "Vata-Kapha";
        if (vataDom) return "Vata";
        if (pittaDom) return "Pitta";
        if (kaphaDom) return "Kapha";
        
        // Default to highest
        if (vataPct >= pittaPct && vataPct >= kaphaPct) return "Vata";
        if (pittaPct >= vataPct && pittaPct >= kaphaPct) return "Pitta";
        return "Kapha";
    }

    private double calculateConfidence(QuizResult quiz, PhotoResult photo, DoshaScores finalScores) {
        double quizConf = quiz.getConfidence() / 100.0;
        double photoConf = photo.getConfidence() / 100.0;
        
        // Weighted confidence
        double baseConfidence = (quizConf * QUIZ_WEIGHT) + (photoConf * PHOTO_WEIGHT);
        
        // Agreement bonus
        if (quiz.getBodyType().equals(photo.getBodyType())) {
            baseConfidence += 0.10; // +10% if both agree
        }
        
        return Math.min(baseConfidence * 100, 100); // Cap at 100%
    }

    private String generateExplanation(QuizResult quiz, PhotoResult photo, DoshaScores scores) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Your BodyType was determined using both your questionnaire responses (40%) ");
        sb.append("and physical constitution analysis from your photo (60%). ");
        
        if (quiz.getBodyType().equals(photo.getBodyType())) {
            sb.append("Both assessments aligned, confirming your body type with high confidence.");
        } else {
            sb.append("While your questionnaire suggested ").append(quiz.getBodyType());
            sb.append(", your physical characteristics indicated ").append(photo.getBodyType());
            sb.append(". The combined analysis provides the most accurate result.");
        }
        
        return sb.toString();
    }

    // ── Inner DTOs ─────────────────────────────────────────────────────────

    public static class QuizResult {
        private String bodyType;
        private DoshaScores scores;
        private double confidence;

        public QuizResult(String bodyType, DoshaScores scores, double confidence) {
            this.bodyType = bodyType;
            this.scores = scores;
            this.confidence = confidence;
        }

        public String getBodyType() { return bodyType; }
        public DoshaScores getScores() { return scores; }
        public double getConfidence() { return confidence; }
    }

    public static class PhotoResult {
        private String bodyType;
        private DoshaScores scores;
        private double confidence;

        public PhotoResult(String bodyType, DoshaScores scores, double confidence) {
            this.bodyType = bodyType;
            this.scores = scores;
            this.confidence = confidence;
        }

        public String getBodyType() { return bodyType; }
        public DoshaScores getScores() { return scores; }
        public double getConfidence() { return confidence; }
    }

    public static class DoshaScores {
        private double vata;
        private double pitta;
        private double kapha;

        public DoshaScores(double vata, double pitta, double kapha) {
            this.vata = vata;
            this.pitta = pitta;
            this.kapha = kapha;
        }

        public double getVata() { return vata; }
        public double getPitta() { return pitta; }
        public double getKapha() { return kapha; }
        
        public DoshaScores toPercentage() {
            double total = vata + pitta + kapha;
            return new DoshaScores(
                (vata / total) * 100,
                (pitta / total) * 100,
                (kapha / total) * 100
            );
        }
    }

    public static class HybridResult {
        private String finalBodyType;
        private DoshaScores doshaScores;
        private double confidence;
        private String explanation;
        private String quizBodyType;
        private String photoBodyType;

        public HybridResult(String finalBodyType, DoshaScores doshaScores, 
                          double confidence, String explanation,
                          String quizBodyType, String photoBodyType) {
            this.finalBodyType = finalBodyType;
            this.doshaScores = doshaScores.toPercentage();
            this.confidence = confidence;
            this.explanation = explanation;
            this.quizBodyType = quizBodyType;
            this.photoBodyType = photoBodyType;
        }

        // Getters
        public String getFinalBodyType() { return finalBodyType; }
        public DoshaScores getDoshaScores() { return doshaScores; }
        public double getConfidence() { return confidence; }
        public String getExplanation() { return explanation; }
        public String getQuizBodyType() { return quizBodyType; }
        public String getPhotoBodyType() { return photoBodyType; }
    }
}