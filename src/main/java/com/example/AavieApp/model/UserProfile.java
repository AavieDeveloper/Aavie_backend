package com.example.AavieApp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private Integer age;

    @Column
    private String city;

    @Column
    private String gender;

    /** Email — unique, used for login */
    @Column(unique = true) 
    private String email;

    /** BCrypt hashed password */
    @Column
    private String passwordHash;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer profileCompletion = 50; 

    @Column
    private String prakruti;
    
    @Column
    private Integer height;

    @Column
    private Integer weight;
    
    @Column(unique = true)
    private String mobileNumber;
    
    @Column(name = "expo_push_token", length = 255)
    private String expoPushToken;

    public String getExpoPushToken()             { return expoPushToken; }
    public void   setExpoPushToken(String token) { this.expoPushToken = token; }

    @Column
    private String vikriti;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Column(nullable = false)
    private String role = "USER";  // USER or ADMIN

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    @Column(name = "last_notification_sent_at")
    private LocalDateTime lastNotificationSentAt;

    public LocalDateTime getLastNotificationSentAt() { 
        return lastNotificationSentAt; 
    }
    public void setLastNotificationSentAt(LocalDateTime v) { 
        this.lastNotificationSentAt = v; 
    }
    
 

    public UserProfile() {}

    public UserProfile(String name, Integer age, String city, String gender) {
        this.name   = name;
        this.age    = age;
        this.city   = city;
        this.gender = gender;
    }
    

    // ── Getters & Setters ─────────────────────────────────────────────────
    public Long    getId()                         { return id; }
    public void    setId(Long id)                  { this.id = id; }
    public String  getName()                       { return name; }
    public void    setName(String name)            { this.name = name; }
    public Integer getAge()                        { return age; }
    public void    setAge(Integer age)             { this.age = age; }
    public String  getCity()                       { return city; }
    public void    setCity(String city)            { this.city = city; }
    public String  getGender()                     { return gender; }
    public void    setGender(String gender)        { this.gender = gender; }
    public String  getEmail()                      { return email; }
    public void    setEmail(String email)          { this.email = email; }
    public String  getPasswordHash()               { return passwordHash; }
    public void    setPasswordHash(String hash)    { this.passwordHash = hash; }
    public Integer getProfileCompletion()          { return profileCompletion; }
    public void    setProfileCompletion(Integer p) { this.profileCompletion = p; }
    public String  getPrakruti()                   { return prakruti; }
    public void    setPrakruti(String prakruti)    { this.prakruti = prakruti; }
    public Integer getHeight()                     { return height; }
    public void    setHeight(Integer height)       { this.height = height; }
    public Integer getWeight()                     { return weight; }
    public void    setWeight(Integer weight)       { this.weight = weight; }
    public String  getVikriti()                    { return vikriti; }
    public void    setVikriti(String vikriti)      { this.vikriti = vikriti; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    
    public String getMobileNumber()              { return mobileNumber; }
    public void   setMobileNumber(String mobile) { this.mobileNumber = mobile; }
}