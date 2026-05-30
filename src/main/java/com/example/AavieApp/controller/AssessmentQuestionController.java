package com.example.AavieApp.controller;

import com.example.AavieApp.model.AssessmentQuestion;
import com.example.AavieApp.repository.AssessmentQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class AssessmentQuestionController {

    private final AssessmentQuestionRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssessmentQuestionController(AssessmentQuestionRepository repo) {
        this.repo = repo;
    }

    // ── PUBLIC — React Native app fetches questions ───────────────────────────
    @GetMapping("/api/public/questions/{assessmentType}")
    public List<AssessmentQuestion> getPublicQuestions(
        @PathVariable String assessmentType
    ) {
        return repo.findByAssessmentTypeAndIsActiveTrueOrderByQuestionOrderAsc(
            assessmentType.toUpperCase()
        );
    }

    // ── ADMIN — all questions including inactive ──────────────────────────────
    @GetMapping("/api/admin/questions/{assessmentType}")
    public List<AssessmentQuestion> getAdminQuestions(
        @PathVariable String assessmentType
    ) {
        return repo.findByAssessmentTypeOrderByQuestionOrderAsc(
            assessmentType.toUpperCase()
        );
    }

    @GetMapping("/api/admin/questions/item/{id}")
    public ResponseEntity<AssessmentQuestion> getQuestion(@PathVariable Long id) {
        return repo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/questions")
    public ResponseEntity<?> createQuestion(@RequestBody AssessmentQuestion q) {
        try {
            // Validate optionsJson is valid JSON
            objectMapper.readTree(q.getOptionsJson());
            return ResponseEntity.ok(repo.save(q));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid optionsJson: " + e.getMessage()));
        }
    }

    @PutMapping("/api/admin/questions/{id}")
    public ResponseEntity<?> updateQuestion(
        @PathVariable Long id,
        @RequestBody AssessmentQuestion updated
    ) {
        return repo.findById(id).map(existing -> {
            existing.setQuestionText(updated.getQuestionText());
            existing.setSubText(updated.getSubText());
            existing.setSubStyle(updated.getSubStyle());
            existing.setSection(updated.getSection());
            existing.setSectionIcon(updated.getSectionIcon());
            existing.setQuestionType(updated.getQuestionType());
            existing.setQuestionOrder(updated.getQuestionOrder());
            existing.setIsActive(updated.getIsActive());
            existing.setIsBanner(updated.getIsBanner());
            existing.setBannerTitle(updated.getBannerTitle());
            existing.setBannerDesc(updated.getBannerDesc());
            existing.setOptionsJson(updated.getOptionsJson());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Toggle active/inactive instead of hard delete
    @PatchMapping("/api/admin/questions/{id}/toggle")
    public ResponseEntity<AssessmentQuestion> toggleQuestion(@PathVariable Long id) {
        return repo.findById(id).map(q -> {
            q.setIsActive(!q.getIsActive());
            return ResponseEntity.ok(repo.save(q));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Reorder questions
    @PutMapping("/api/admin/questions/reorder")
    public ResponseEntity<?> reorderQuestions(
        @RequestBody List<Map<String, Object>> orderList
    ) {
        orderList.forEach(item -> {
            Long qId = Long.valueOf(item.get("id").toString());
            Integer order = Integer.valueOf(item.get("order").toString());
            repo.findById(qId).ifPresent(q -> {
                q.setQuestionOrder(order);
                repo.save(q);
            });
        });
        return ResponseEntity.ok(Map.of("message", "Reordered successfully"));
    }
}