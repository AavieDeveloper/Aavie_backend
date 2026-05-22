package com.example.AavieApp.controller;

import com.example.AavieApp.model.Article;
import com.example.AavieApp.repository.ArticleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ArticleController {

    private final ArticleRepository repo;

    public ArticleController(ArticleRepository repo) {
        this.repo = repo;
    }

    // ── PUBLIC — no auth, used by Expo app ────────────────────────────────────

    @GetMapping("/api/public/articles")
    public List<Article> getPublicArticles(
        @RequestParam(defaultValue = "all") String ageGroup
    ) {
        if ("all".equals(ageGroup)) {
            return repo.findByStatusOrderByCreatedAtDesc("live");
        }
        return repo.findByAgeGroupAndStatusOrderByCreatedAtDesc(ageGroup, "live");
    }

    // ── ADMIN — JWT + ADMIN role required ─────────────────────────────────────

    @GetMapping("/api/admin/articles")
    public List<Article> getAllArticles(
        @RequestParam(defaultValue = "") String ageGroup,
        @RequestParam(defaultValue = "") String status
    ) {
        if (!ageGroup.isEmpty() && !status.isEmpty()) {
            return repo.findByAgeGroupAndStatusOrderByCreatedAtDesc(ageGroup, status);
        }
        if (!status.isEmpty()) {
            return repo.findByStatusOrderByCreatedAtDesc(status);
        }
        return repo.findAll();
    }

    @GetMapping("/api/admin/articles/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        return repo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/articles")
    public Article createArticle(@RequestBody Article article) {
        return repo.save(article);
    }

    @PutMapping("/api/admin/articles/{id}")
    public ResponseEntity<Article> updateArticle(
        @PathVariable Long id,
        @RequestBody Article updated
    ) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setCategory(updated.getCategory());
            existing.setAgeGroup(updated.getAgeGroup());
            existing.setBody(updated.getBody());
            existing.setImageUrl(updated.getImageUrl());
            existing.setReadTime(updated.getReadTime());
            existing.setStatus(updated.getStatus());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/articles/stats")
    public Map<String, Object> getArticleStats() {
        long total = repo.count();
        long live  = repo.countByStatus("live");
        long draft = repo.countByStatus("draft");
        return Map.of("total", total, "live", live, "draft", draft);
    }
}