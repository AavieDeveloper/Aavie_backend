package com.example.AavieApp.service;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.UserProfileRepository;

import java.util.Optional;
 
/**
 * Aavie — UserProfile Service
 *
 * Business logic layer between the controller and the repository.
 * All profile creation, retrieval and update operations live here.
 */
@Service
@Transactional
public class UserProfileService {
 
    private final UserProfileRepository repo;
 
    public UserProfileService(UserProfileRepository repo) {
        this.repo = repo;
    }
 
    // ── DTOs ──────────────────────────────────────────────────────────────────
 
    /** Inbound DTO — data sent from the React Native Create Profile form */
    public static class CreateProfileRequest {
        private String  name;
        private Integer age;
        private String  city;
        private String  gender;   // "Male" | "Female"
 
        public String  getName()   { return name; }
        public void    setName(String name)   { this.name = name; }
        public Integer getAge()    { return age; }
        public void    setAge(Integer age)    { this.age = age; }
        public String  getCity()   { return city; }
        public void    setCity(String city)   { this.city = city; }
        public String  getGender() { return gender; }
        public void    setGender(String gender) { this.gender = gender; }
        private Integer height;
        private Integer weight;
        public Integer getHeight() { return height; }
        public void    setHeight(Integer height) { this.height = height; }
        public Integer getWeight() { return weight; }
        public void    setWeight(Integer weight) { this.weight = weight; }
    }
 
    /** Outbound DTO — data returned to the React Native app */
    public static class ProfileResponse {
        private Long    id;
        private String  name;
        private Integer age;
        private String  city;
        private String  gender;
        private Integer profileCompletion;
        private String  prakruti;
        private String  vikriti;
        private String  createdAt;  // ISO-8601 string
 
        // Builder-style setters for convenience
        public ProfileResponse id(Long id)                            { this.id = id;                         return this; }
        public ProfileResponse name(String name)                      { this.name = name;                     return this; }
        public ProfileResponse age(Integer age)                       { this.age = age;                       return this; }
        public ProfileResponse city(String city)                      { this.city = city;                     return this; }
        public ProfileResponse gender(String gender)                  { this.gender = gender;                 return this; }
        public ProfileResponse profileCompletion(Integer p)           { this.profileCompletion = p;           return this; }
        public ProfileResponse prakruti(String prakruti)              { this.prakruti = prakruti;             return this; }
        public ProfileResponse vikriti(String vikriti)                { this.vikriti = vikriti;               return this; }
        public ProfileResponse createdAt(String createdAt)            { this.createdAt = createdAt;           return this; }
 
        private Integer height;
        private Integer weight;
        public ProfileResponse height(Integer height) { this.height = height; return this; }
        public ProfileResponse weight(Integer weight) { this.weight = weight; return this; }
        public Integer getHeight() { return height; }
        public Integer getWeight() { return weight; }
        public Long    getId()                { return id; }
        public String  getName()              { return name; }
        public Integer getAge()               { return age; }
        public String  getCity()              { return city; }
        public String  getGender()            { return gender; }
        public Integer getProfileCompletion() { return profileCompletion; }
        public String  getPrakruti()          { return prakruti; }
        public String  getVikriti()           { return vikriti; }
        public String  getCreatedAt()         { return createdAt; }
    
    }
 
    // ── Service Methods ───────────────────────────────────────────────────────
 
    /**
     * Create a new user profile.
     * Called by POST /api/user/profile
     */
    public ProfileResponse createProfile(CreateProfileRequest req) {
        // Normalize inputs
        String name   = req.getName().trim();
        String city   = req.getCity().trim();
        String gender = capitalize(req.getGender().trim());
 
        // Validate gender value
        if (!gender.equals("Male") && !gender.equals("Female")) {
            throw new IllegalArgumentException("Gender must be 'Male' or 'Female'");
        }
 
        UserProfile profile = new UserProfile(name, req.getAge(), city, gender);
        profile.setProfileCompletion(50);
        if (req.getHeight() != null) profile.setHeight(req.getHeight());
        if (req.getWeight() != null) profile.setWeight(req.getWeight());
 
        UserProfile saved = repo.save(profile);
        return toResponse(saved);
    }
 
    /**
     * Retrieve a profile by ID.
     * Called by GET /api/user/profile/{id}
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long id) {
        UserProfile profile = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
        return toResponse(profile);
    }
 
    /**
     * Update an existing profile.
     * Called by PUT /api/user/profile/{id}
     */
  
 
    /**
     * Delete a profile.
     * Called by DELETE /api/user/profile/{id}
     */
  
    // ── Private Helpers ───────────────────────────────────────────────────────
 
    private ProfileResponse toResponse(UserProfile p) {
    	return new ProfileResponse()
    		    .id(p.getId())
    		    .name(p.getName())
    		    .age(p.getAge())
    		    .city(p.getCity())
    		    .gender(p.getGender())
    		    .profileCompletion(p.getProfileCompletion())
    		    .prakruti(p.getPrakruti())
    		    .vikriti(p.getVikriti())
    		    .height(p.getHeight())
    		    .weight(p.getWeight())
    		    .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
    }
 
    private int calculateCompletion(UserProfile p) {
        int score = 50; // base after creation
        if (p.getPrakruti() != null && !p.getPrakruti().isBlank()) score += 25; // quiz done
        if (p.getVikriti()  != null && !p.getVikriti().isBlank())  score += 25; // vikriti resolved
        return Math.min(score, 100);
    }
 
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
 
