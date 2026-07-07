package com.example.AavieApp.controller;

import com.example.AavieApp.model.HomeSection;
import com.example.AavieApp.repository.HomeSectionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class HomeSectionController {

    private final HomeSectionRepository repo;

    @Value("${upload.base-path:uploads}")
    private String uploadBasePath;

    public HomeSectionController(HomeSectionRepository repo) {
        this.repo = repo;
    }

    // ── Public — app fetches all active sections ──
    @GetMapping("/api/public/home-sections")
    public List<HomeSection> getPublicSections() {
        return repo.findByIsActiveTrueOrderBySectionKeyAsc();
    }

    // ── Public — fetch one section by key ──
    @GetMapping("/api/public/home-sections/{key}")
    public ResponseEntity<?> getSection(@PathVariable String key) {
        return repo.findBySectionKey(key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Admin — update section content ──
    @PutMapping("/api/admin/home-sections/{key}")
    public ResponseEntity<?> updateSection(
        @PathVariable String key,
        @RequestBody Map<String, Object> req
    ) {
        return repo.findBySectionKey(key).map(section -> {
            if (req.containsKey("contentJson"))
                section.setContentJson((String) req.get("contentJson"));
            if (req.containsKey("isActive"))
                section.setActive((Boolean) req.get("isActive"));
            section.setUpdatedAt(LocalDateTime.now());
            repo.save(section);
            return ResponseEntity.ok(Map.of("saved", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Admin — upload image for a section ──
    @PostMapping("/api/admin/home-sections/{key}/upload-image")
    public ResponseEntity<?> uploadImage(
        @PathVariable String key,
        @RequestParam("file") MultipartFile file
    ) {
        try {
            String filename = "home_" + key + "_" + System.currentTimeMillis()
                + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get(uploadBasePath, "home-sections");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath,
                StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = "/uploads/home-sections/" + filename;
            return ResponseEntity.ok(Map.of("imageUrl", publicUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    // ── Admin — get all sections ──
    @GetMapping("/api/admin/home-sections")
    public List<HomeSection> getAllSections() {
        return repo.findByIsActiveTrueOrderBySectionKeyAsc();
    }
}