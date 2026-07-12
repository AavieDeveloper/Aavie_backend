package com.example.AavieApp.repository;

import com.example.AavieApp.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    // Find latest unused valid OTP for a number
    Optional<OtpVerification> findTopByMobileNumberAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        String mobileNumber, LocalDateTime now
    );

    // Delete all old OTPs for a number before creating new one
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.mobileNumber = :mobileNumber")
    void deleteAllByMobileNumber(String mobileNumber);
    
    List<OtpVerification> findAllByOrderByCreatedAtDesc();
}