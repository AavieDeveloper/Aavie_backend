package com.example.AavieApp.repository;

import com.example.AavieApp.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByAgeGroupOrAgeGroupOrderByCreatedAtDesc(
        String ageGroup, String all
    );

    List<Article> findByStatusOrderByCreatedAtDesc(String status);

    List<Article> findByAgeGroupAndStatusOrderByCreatedAtDesc(
        String ageGroup, String status
    );

 // ADD this method:
    List<Article> findAll();
    // Actually it's inherited from JpaRepository — just add this one:
    long countByStatus(String status); // already there ✓

    // ADD this missing method:
    List<Article> findByAgeGroup(String ageGroup);
    
    
    
}