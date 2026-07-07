package com.example.AavieApp.repository;

import com.example.AavieApp.model.HomeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HomeSectionRepository extends JpaRepository<HomeSection, Long> {
    List<HomeSection> findByIsActiveTrueOrderBySectionKeyAsc();
    Optional<HomeSection> findBySectionKey(String sectionKey);
}