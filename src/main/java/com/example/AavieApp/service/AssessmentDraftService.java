package com.example.AavieApp.service;

import com.example.AavieApp.controller.UserAssessmentController.DraftRequest;
import com.example.AavieApp.model.AssessmentDraft;
import com.example.AavieApp.repository.AssessmentDraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AssessmentDraftService {

    private final AssessmentDraftRepository repo;

    public AssessmentDraftService(AssessmentDraftRepository repo) {
        this.repo = repo;
    }

    public void saveDraft(DraftRequest req) {
        AssessmentDraft draft = repo
            .findByUserIdAndAssessmentType(req.getUserId(), req.getAssessmentType())
            .orElse(new AssessmentDraft());

        draft.setUserId(req.getUserId());
        draft.setAssessmentType(req.getAssessmentType());
        draft.setAnswersJson(req.getAnswersJson());
        draft.setCurrentQuestion(req.getCurrentQuestion());
        draft.setCompleted(req.getCompleted() != null ? req.getCompleted() : false);

        repo.save(draft);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDraft(Long userId, String type) {
        Optional<AssessmentDraft> opt = repo.findByUserIdAndAssessmentType(userId, type);

        if (opt.isEmpty()) {
            return Map.of("found", false);
        }

        AssessmentDraft draft = opt.get();
        return Map.of(
            "found",           true,
            "answersJson",     draft.getAnswersJson() != null ? draft.getAnswersJson() : "",
            "currentQuestion", draft.getCurrentQuestion() != null ? draft.getCurrentQuestion() : 0,
            "completed",       draft.getCompleted() != null ? draft.getCompleted() : false,
            "savedAt",         draft.getSavedAt() != null ? draft.getSavedAt().toString() : ""
        );
    }

    public void deleteDraft(Long userId, String type) {
        repo.deleteByUserIdAndAssessmentType(userId, type);
    }
}