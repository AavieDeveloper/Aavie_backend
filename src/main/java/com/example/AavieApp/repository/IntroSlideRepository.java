package com.example.AavieApp.repository;

import com.example.AavieApp.model.IntroSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntroSlideRepository extends JpaRepository<IntroSlide, Long> {
    List<IntroSlide> findAllByOrderBySlideIndexAsc();
    List<IntroSlide> findByIsActiveTrueOrderBySlideIndexAsc();
}