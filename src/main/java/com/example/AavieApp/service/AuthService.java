package com.example.AavieApp.service;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.UserProfileRepository;
import com.example.AavieApp.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserProfileRepository repo;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthService(UserProfileRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    // DTOs remain the same...
    public static class RegisterRequest {
        private String  name;
        private Integer age;
        private String  city;
        private String  gender;
        private String  email;
        private String  password;

        // Getters and setters remain the same...
        public String  getName()     { return name; }
        public void    setName(String v)     { this.name = v; }
        public Integer getAge()      { return age; }
        public void    setAge(Integer v)     { this.age = v; }
        public String  getCity()     { return city; }
        public void    setCity(String v)     { this.city = v; }
        public String  getGender()   { return gender; }
        public void    setGender(String v)   { this.gender = v; }
        public String  getEmail()    { return email; }
        public void    setEmail(String v)    { this.email = v; }
        public String  getPassword() { return password; }
        public void    setPassword(String v) { this.password = v; }
        private Integer height;
        private Integer weight;
        public Integer getHeight() { return height; }
        public void    setHeight(Integer v) { this.height = v; }
        public Integer getWeight() { return weight; }
        public void    setWeight(Integer v) { this.weight = v; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail()    { return email; }
        public void   setEmail(String v)    { this.email = v; }
        public String getPassword() { return password; }
        public void   setPassword(String v) { this.password = v; }
    }

    public static class AuthResponse {
        private Long   userId;
        private String gender;
        private String name;
        private String message;
        private String token;        // NEW: JWT token
        private String tokenType = "Bearer";  // NEW

        public AuthResponse(Long userId, String gender, String name, String message, String token) {
            this.userId  = userId;
            this.gender  = gender;
            this.name    = name;
            this.message = message;
            this.token   = token;
        }

        public Long   getUserId() { return userId; }
        public String getGender() { return gender; }
        public String getName()   { return name; }
        public String getMessage(){ return message; }
        public String getToken()  { return token; }
        public String getTokenType() { return tokenType; }
    }

    // Register method
    public AuthResponse register(RegisterRequest req) {
        // Normalise
        String email  = req.getEmail().trim().toLowerCase();
        String name   = req.getName().trim();
        String city   = req.getCity().trim();
        String gender = capitalize(req.getGender().trim());

        // Validate gender
        if (!gender.equals("Male") && !gender.equals("Female")) {
            throw new IllegalArgumentException("Gender must be 'Male' or 'Female'");
        }

        // Check email uniqueness
        if (repo.existsByEmail(email)) {
            throw new IllegalStateException("An account with this email already exists.");
        }

        // Validate password length
        if (req.getPassword() == null || req.getPassword().trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        // Build and save profile
        UserProfile profile = new UserProfile(name, req.getAge(), city, gender);
        profile.setEmail(email);
        profile.setPasswordHash(passwordEncoder.encode(req.getPassword().trim()));
        profile.setProfileCompletion(50);

        if (req.getHeight() != null) profile.setHeight(req.getHeight());
        if (req.getWeight() != null) profile.setWeight(req.getWeight());
        UserProfile saved = repo.save(profile);
        
        // Generate JWT token
        String token = tokenProvider.generateToken(saved.getId(), saved.getEmail());
        
        return new AuthResponse(saved.getId(), saved.getGender(), saved.getName(), 
                               "Profile created successfully", token);
    }

    // Login method
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        
        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, req.getPassword().trim())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserProfile profile = repo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("No account found with this email."));
        
        // Generate JWT token
        String token = tokenProvider.generateToken(profile.getId(), profile.getEmail());
        
        return new AuthResponse(profile.getId(), profile.getGender(), profile.getName(), 
                               "Login successful", token);
    }

    // Helper
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}


