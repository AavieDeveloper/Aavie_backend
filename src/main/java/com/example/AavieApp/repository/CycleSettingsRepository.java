package com.example.AavieApp.repository;
 
import com.example.AavieApp.model.CycleSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.Optional;
 
@Repository
public interface CycleSettingsRepository extends JpaRepository<CycleSettings, Long> {
    Optional<CycleSettings> findByUserId(Long userId);
}
 