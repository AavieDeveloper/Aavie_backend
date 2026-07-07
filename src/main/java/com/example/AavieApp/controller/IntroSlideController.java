package com.example.AavieApp.controller;

import com.example.AavieApp.model.IntroSlide;

import com.example.AavieApp.repository.IntroSlideRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@CrossOrigin(origins = "*")
public class IntroSlideController {

    private final IntroSlideRepository repo;

    public IntroSlideController(IntroSlideRepository repo) {
        this.repo = repo;
    }
    
    @Value("${upload.base-path:uploads}")
    private String uploadBasePath;

    // ── Public endpoint — React Native app fetches this ──
    @GetMapping("/api/public/intro-slides")
    public List<IntroSlide> getPublicSlides() {
        return repo.findByIsActiveTrueOrderBySlideIndexAsc();
    }

    // ── Admin endpoints ──
    @GetMapping("/api/admin/intro-slides")
    public List<IntroSlide> getAllSlides() {
        return repo.findAllByOrderBySlideIndexAsc();
    }
    
    @PostMapping("/api/admin/intro-slides/{id}/upload-image")
    public ResponseEntity<?> uploadSlideImage(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        try {
            String filename = "slide_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get(uploadBasePath, "intro-slides");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = "/uploads/intro-slides/" + filename;
            return repo.findById(id).map(slide -> {
                slide.setImageUrl(publicUrl);
                repo.save(slide);
                return ResponseEntity.ok(Map.of("imageUrl", publicUrl));
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @PutMapping("/api/admin/intro-slides/{id}")
    public ResponseEntity<?> updateSlide(@PathVariable Long id,
                                          @RequestBody Map<String, Object> req) {
        return repo.findById(id).map(slide -> {
            if (req.containsKey("label"))      slide.setLabel((String) req.get("label"));
            if (req.containsKey("titleJson"))  slide.setTitleJson((String) req.get("titleJson"));
            if (req.containsKey("body"))       slide.setBody((String) req.get("body"));
            if (req.containsKey("cta"))        slide.setCta((String) req.get("cta"));
            if (req.containsKey("tag"))           slide.setTag((String) req.get("tag"));
            if (req.containsKey("eyebrowColor"))  slide.setEyebrowColor((String) req.get("eyebrowColor"));
            if (req.containsKey("imageUrl"))      slide.setImageUrl((String) req.get("imageUrl"));
            if (req.containsKey("isActive"))      slide.setActive((Boolean) req.get("isActive"));
            if (req.containsKey("active"))        slide.setActive((Boolean) req.get("active"));
            repo.save(slide);
            return ResponseEntity.ok(Map.of("saved", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/admin/intro-slides/reorder")
    public ResponseEntity<?> reorder(@RequestBody List<Map<String, Object>> items) {
        items.forEach(item -> {
            Long id = Long.valueOf(item.get("id").toString());
            int idx = Integer.parseInt(item.get("slideIndex").toString());
            repo.findById(id).ifPresent(s -> {
                s.setSlideIndex(idx);
                repo.save(s);
            });
        });
        return ResponseEntity.ok(Map.of("saved", true));
    }
}