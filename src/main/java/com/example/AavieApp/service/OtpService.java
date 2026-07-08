package com.example.AavieApp.service;

import com.example.AavieApp.model.OtpVerification;
import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.OtpVerificationRepository;
import com.example.AavieApp.repository.UserProfileRepository;
import com.example.AavieApp.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpService {

    @Value("${fast2sms.api.key}")
    private String fast2smsApiKey;

    // ── Test number for Play Store review ─────────────────────────────────────
    // Google Play reviewers cannot receive real OTP — this bypasses SMS for them
    private static final String TEST_NUMBER  = "+911234567890";
    private static final String TEST_OTP     = "123456";

    private final OtpVerificationRepository otpRepo;
    private final UserProfileRepository     userRepo;
    private final JwtTokenProvider          tokenProvider;
    private final PasswordEncoder           passwordEncoder;

    public OtpService(
        OtpVerificationRepository otpRepo,
        UserProfileRepository userRepo,
        JwtTokenProvider tokenProvider,
        PasswordEncoder passwordEncoder
    ) {
        this.otpRepo         = otpRepo;
        this.userRepo        = userRepo;
        this.tokenProvider   = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Step 1 — Send OTP ─────────────────────────────────────────────────────
    public Map<String, Object> sendOtp(String mobileNumber) {
        String clean = cleanNumber(mobileNumber);

        // ── Play Store test account bypass ────────────────────────────────────
        // This test number always gets OTP 123456 without sending real SMS
        // Used by Google Play reviewers to test the app
        if (TEST_NUMBER.equals(clean)) {
            otpRepo.deleteAllByMobileNumber(clean);
            OtpVerification testOtp = new OtpVerification();
            testOtp.setMobileNumber(clean);
            testOtp.setOtpCode(TEST_OTP);
            testOtp.setExpiresAt(LocalDateTime.now().plusMinutes(60));
            otpRepo.save(testOtp);

            Map<String, Object> result = new HashMap<>();
            result.put("isNewUser",       false);
            result.put("profileComplete", true);
            result.put("message",         "Test OTP ready");
            return result;
        }
        // ── End test bypass ───────────────────────────────────────────────────

        // Check if number exists
        Optional<UserProfile> existing = userRepo.findByMobileNumber(clean);

        boolean isNewUser       = existing.isEmpty();
        boolean profileComplete = existing.map(u ->
            u.getName() != null && u.getAge() != null &&
            u.getCity() != null && u.getGender() != null
        ).orElse(false);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Delete old OTPs for this number
        otpRepo.deleteAllByMobileNumber(clean);

        // Save new OTP
        OtpVerification otpRecord = new OtpVerification();
        otpRecord.setMobileNumber(clean);
        otpRecord.setOtpCode(otp);
        otpRecord.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otpRepo.save(otpRecord);

        // Send OTP via 2Factor
        boolean sent = sendVisFast2SMS(clean, otp);

        if (!sent) {
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isNewUser",       isNewUser);
        result.put("profileComplete", profileComplete);
        result.put("message",         "OTP sent successfully");
        return result;
    }

    // ── Step 2 — Verify OTP ───────────────────────────────────────────────────
    public Map<String, Object> verifyOtp(String mobileNumber, String otpCode) {
        String clean = cleanNumber(mobileNumber);

        // Find valid OTP
        Optional<OtpVerification> otpOpt = otpRepo
            .findTopByMobileNumberAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                clean, LocalDateTime.now()
            );

        if (otpOpt.isEmpty()) {
            throw new RuntimeException("OTP expired or not found. Please request a new one.");
        }

        OtpVerification otpRecord = otpOpt.get();

        if (!otpRecord.getOtpCode().equals(otpCode.trim())) {
            throw new RuntimeException("Incorrect OTP. Please try again.");
        }

        // Mark OTP as used
        otpRecord.setIsUsed(true);
        otpRepo.save(otpRecord);

        // Check user profile
        Optional<UserProfile> existing = userRepo.findByMobileNumber(clean);

        Map<String, Object> result = new HashMap<>();

        if (existing.isEmpty()) {
            // New user — create placeholder
            UserProfile placeholder = new UserProfile();
            placeholder.setMobileNumber(clean);
            placeholder.setRole("USER");
            placeholder.setProfileCompletion(0);
            userRepo.save(placeholder);

            result.put("isNewUser",       true);
            result.put("profileComplete", false);
            result.put("mobileNumber",    clean);
            result.put("message",         "OTP verified — please complete your profile");

        } else {
            UserProfile profile = existing.get();
            boolean complete = profile.getName()   != null
                && profile.getAge()    != null
                && profile.getCity()   != null
                && profile.getGender() != null;

            if (complete) {
                // Existing complete user — login directly
                String identifier = profile.getEmail() != null
                    ? profile.getEmail() : clean;
                String token = tokenProvider.generateToken(
                    profile.getId(), identifier, profile.getRole()
                );

                result.put("isNewUser",       false);
                result.put("profileComplete", true);
                result.put("token",           token);
                result.put("userId",          profile.getId());
                result.put("name",            profile.getName());
                result.put("gender",          profile.getGender());
                result.put("message",         "Login successful");

            } else {
                // Number exists but profile incomplete
                result.put("isNewUser",       false);
                result.put("profileComplete", false);
                result.put("mobileNumber",    clean);
                result.put("message",         "Please complete your profile");
            }
        }
        return result;
    }

    // ── Step 3 — Complete profile ─────────────────────────────────────────────
    public Map<String, Object> completeProfile(CompleteProfileRequest req) {
        String clean = cleanNumber(req.getMobileNumber());

        UserProfile profile = userRepo.findByMobileNumber(clean)
            .orElseThrow(() -> new RuntimeException(
                "Mobile number not found. Please start again."
            ));

        String fullName = (req.getFirstName().trim() + " "
            + (req.getLastName() != null ? req.getLastName().trim() : "")).trim();

        profile.setName(fullName);
        profile.setAge(req.getAge());
        profile.setCity(req.getCity());
        profile.setGender(req.getGender());
        profile.setProfileCompletion(100);

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            profile.setEmail(req.getEmail().trim().toLowerCase());
        }
        if (req.getHeight() != null) profile.setHeight(req.getHeight());
        if (req.getWeight() != null) profile.setWeight(req.getWeight());

        // Set random password since field still exists in entity
        profile.setPasswordHash(
            passwordEncoder.encode(java.util.UUID.randomUUID().toString())
        );

        UserProfile saved = userRepo.save(profile);

        String identifier = saved.getEmail() != null
            ? saved.getEmail() : clean;
        String token = tokenProvider.generateToken(
            saved.getId(), identifier, saved.getRole()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("token",   token);
        result.put("userId",  saved.getId());
        result.put("name",    saved.getName());
        result.put("gender",  saved.getGender());
        result.put("message", "Profile created successfully");
        return result;
    }

 // ── 2Factor sender ────────────────────────────────────────────────────────
    private boolean sendVisFast2SMS(String mobileNumber, String otp) {
        try {
            String number = mobileNumber.startsWith("+91")
                ? mobileNumber.substring(3)
                : mobileNumber;

            // Option 1: SMS with automatic fallback to voice
            String url = "https://2factor.in/API/V1/"
                + fast2smsApiKey
                + "/SMS/" + number
                + "/" + otp
                + "/OTP1";  // Changed from AUTOGEN2 to OTP1 for better SMS delivery

            System.out.println("2Factor SMS URL: " + url);

            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "AAVIE-Backend/1.0");

            int statusCode = conn.getResponseCode();

            java.io.InputStream stream = statusCode == 200
                ? conn.getInputStream()
                : conn.getErrorStream();

            String body = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            stream.close();
            conn.disconnect();

            System.out.println("2Factor SMS status: " + statusCode);
            System.out.println("2Factor SMS response: " + body);

            // If SMS fails, try voice call as fallback
            if (statusCode != 200 || !body.contains("\"Status\":\"Success\"")) {
                System.out.println("SMS failed, attempting voice call...");
                return sendViaVoiceCall(number, otp);
            }

            return true;

        } catch (Exception e) {
            System.out.println("2Factor SMS error: " + e.getClass().getName() + " — " + e.getMessage());
            
            // Try voice call as fallback if SMS throws exception
            try {
                String number = mobileNumber.startsWith("+91")
                    ? mobileNumber.substring(3)
                    : mobileNumber;
                return sendViaVoiceCall(number, otp);
            } catch (Exception ex) {
                System.out.println("Voice call also failed: " + ex.getMessage());
                return false;
            }
        }
    }

    // Add this new method for voice call fallback (optional)
    private boolean sendViaVoiceCall(String number, String otp) {
        try {
            // 2Factor voice call API endpoint
            String voiceUrl = "https://2factor.in/API/V1/"
                + fast2smsApiKey
                + "/VOICE/" + number
                + "/" + otp;

            System.out.println("2Factor Voice URL: " + voiceUrl);

            java.net.URL apiUrl = new java.net.URL(voiceUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "AAVIE-Backend/1.0");

            int statusCode = conn.getResponseCode();

            java.io.InputStream stream = statusCode == 200
                ? conn.getInputStream()
                : conn.getErrorStream();

            String body = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            stream.close();
            conn.disconnect();

            System.out.println("2Factor Voice status: " + statusCode);
            System.out.println("2Factor Voice response: " + body);

            return statusCode == 200 && body.contains("\"Status\":\"Success\"");

        } catch (Exception e) {
            System.out.println("2Factor Voice error: " + e.getMessage());
            return false;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String cleanNumber(String number) {
        if (number == null) return "";
        String clean = number.replaceAll("[^+\\d]", "");
        if (!clean.startsWith("+")) clean = "+91" + clean;
        return clean;
    }

    // ── Request DTO ───────────────────────────────────────────────────────────
    public static class CompleteProfileRequest {
        private String  mobileNumber;
        private String  firstName;
        private String  lastName;
        private Integer age;
        private String  city;
        private String  gender;
        private String  email;
        private Integer height;
        private Integer weight;

        public String  getMobileNumber()          { return mobileNumber; }
        public void    setMobileNumber(String v)  { this.mobileNumber = v; }
        public String  getFirstName()             { return firstName; }
        public void    setFirstName(String v)     { this.firstName = v; }
        public String  getLastName()              { return lastName; }
        public void    setLastName(String v)      { this.lastName = v; }
        public Integer getAge()                   { return age; }
        public void    setAge(Integer v)          { this.age = v; }
        public String  getCity()                  { return city; }
        public void    setCity(String v)          { this.city = v; }
        public String  getGender()                { return gender; }
        public void    setGender(String v)        { this.gender = v; }
        public String  getEmail()                 { return email; }
        public void    setEmail(String v)         { this.email = v; }
        public Integer getHeight()                { return height; }
        public void    setHeight(Integer v)       { this.height = v; }
        public Integer getWeight()                { return weight; }
        public void    setWeight(Integer v)       { this.weight = v; }
    }
}