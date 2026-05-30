package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusMinutes(10);
        }
    }

    public Long          getId()                        { return id; }
    public String        getMobileNumber()              { return mobileNumber; }
    public void          setMobileNumber(String v)      { this.mobileNumber = v; }
    public String        getOtpCode()                   { return otpCode; }
    public void          setOtpCode(String v)           { this.otpCode = v; }
    public Boolean       getIsUsed()                    { return isUsed; }
    public void          setIsUsed(Boolean v)           { this.isUsed = v; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public LocalDateTime getExpiresAt()                 { return expiresAt; }
    public void          setExpiresAt(LocalDateTime v)  { this.expiresAt = v; }
}