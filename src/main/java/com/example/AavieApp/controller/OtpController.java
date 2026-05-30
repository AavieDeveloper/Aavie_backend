package com.example.AavieApp.controller;

import com.example.AavieApp.service.OtpService;
import com.example.AavieApp.service.OtpService.CompleteProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    // Step 1 — Send OTP to mobile number
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        try {
            String mobileNumber = body.get("mobileNumber");
            if (mobileNumber == null || mobileNumber.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mobile number is required"));
            }
            Map<String, Object> result = otpService.sendOtp(mobileNumber);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Could not send OTP. Please try again."));
        }
    }

    // Step 2 — Verify OTP entered by user
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String mobileNumber = body.get("mobileNumber");
            String otpCode      = body.get("otpCode");

            if (mobileNumber == null || otpCode == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mobile number and OTP are required"));
            }
            Map<String, Object> result = otpService.verifyOtp(mobileNumber, otpCode);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Verification failed. Please try again."));
        }
    }

    // Step 3 — Complete profile after OTP verified
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody CompleteProfileRequest req) {
        try {
            Map<String, Object> result = otpService.completeProfile(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Could not save profile. Please try again."));
        }
    }
}