package com.example.AavieApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consultations")
@CrossOrigin(origins = "*")
public class ConsultationController {

    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestBody ConsultationRequest req) {
        // TODO: Save consultation booking to database
        System.out.println("📅 Consultation booked for user: " + req.getUserId());
        return ResponseEntity.ok(Map.of(
            "message", "Consultation booked successfully",
            "success", true
        ));
    }

    public static class ConsultationRequest {
        private Long userId;
        private String consultationType;
        private String severity;
        private String assessmentId;

        public Long getUserId()                    { return userId; }
        public void setUserId(Long u)              { this.userId = u; }
        public String getConsultationType()        { return consultationType; }
        public void setConsultationType(String c)  { this.consultationType = c; }
        public String getSeverity()                { return severity; }
        public void setSeverity(String s)          { this.severity = s; }
        public String getAssessmentId()            { return assessmentId; }
        public void setAssessmentId(String a)      { this.assessmentId = a; }
    }
}