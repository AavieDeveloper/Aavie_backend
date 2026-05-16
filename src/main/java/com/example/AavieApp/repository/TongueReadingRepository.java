package com.example.AavieApp.repository;

import com.example.AavieApp.model.TongueReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TongueReadingRepository extends JpaRepository<TongueReading, Long> {
    Optional<TongueReading> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}