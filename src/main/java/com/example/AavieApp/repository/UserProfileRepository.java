package com.example.AavieApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.AavieApp.model.UserProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** Find all profiles for a given gender (case-insensitive) */
    List<UserProfile> findByGenderIgnoreCase(String gender);

    /** Check whether a profile already exists for a given name + city combo */
    Optional<UserProfile> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    /** Find by email — used for login */
    Optional<UserProfile> findByEmail(String email);

    /** Check if email is already registered — used during registration */
    boolean existsByEmail(String email);

    /** Count profiles by gender */
    @Query("SELECT COUNT(u) FROM UserProfile u WHERE LOWER(u.gender) = LOWER(:gender)")
    long countByGender(String gender);

    /** Fetch profiles with incomplete assessment */
    @Query("SELECT u FROM UserProfile u WHERE u.profileCompletion < 100 ORDER BY u.createdAt DESC")
    List<UserProfile> findIncompleteProfiles();
}