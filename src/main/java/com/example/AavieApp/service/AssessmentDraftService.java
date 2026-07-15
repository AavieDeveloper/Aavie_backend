package com.example.AavieApp.service;

import com.example.AavieApp.controller.UserAssessmentController.DraftRequest;

import com.example.AavieApp.model.AssessmentDraft;
import com.example.AavieApp.repository.AssessmentDraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.AavieApp.model.AssessmentQuestion;
import com.example.AavieApp.repository.AssessmentQuestionRepository;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AssessmentDraftService {
    private final AssessmentDraftRepository repo;
    private final AssessmentQuestionRepository questionRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();

    public AssessmentDraftService(
        AssessmentDraftRepository repo,
        AssessmentQuestionRepository questionRepo
    ) {
        this.repo = repo;
        this.questionRepo = questionRepo;
    }

    public void saveDraft(DraftRequest req) {
        AssessmentDraft draft = repo
            .findByUserIdAndAssessmentType(req.getUserId(), req.getAssessmentType())
            .orElse(new AssessmentDraft());
        draft.setUserId(req.getUserId());
        draft.setAssessmentType(req.getAssessmentType());
        draft.setAnswersJson(buildReadableAnswers(req.getAnswersJson(), req.getAssessmentType()));
        draft.setCurrentQuestion(req.getCurrentQuestion());
        draft.setCompleted(req.getCompleted() != null ? req.getCompleted() : false);
        repo.save(draft);
    }
    
    
    private String buildReadableAnswers(String answersJson, String assessmentType) {
        if (answersJson == null || answersJson.isBlank()) return answersJson;
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(answersJson);
            com.fasterxml.jackson.databind.JsonNode answersNode = root.has("answers")
                ? root.get("answers") : root;
            com.fasterxml.jackson.databind.JsonNode multiNode = root.has("multi")
                ? root.get("multi") : mapper.createObjectNode();

            List<AssessmentQuestion> questions = questionRepo
                .findByAssessmentTypeAndIsActiveTrueOrderByQuestionOrderAsc(
                    assessmentType.toUpperCase()
                );

            java.util.Map<String, AssessmentQuestion> questionMap = new java.util.HashMap<>();
            for (AssessmentQuestion q : questions) {
                questionMap.put(q.getQuestionId(), q);
            }

            List<java.util.Map<String, Object>> readableAnswers = new ArrayList<>();
            int order = 0;

            java.util.Iterator<java.util.Map.Entry<String,
                com.fasterxml.jackson.databind.JsonNode>> fields = answersNode.fields();

            while (fields.hasNext()) {
                java.util.Map.Entry<String,
                    com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                String questionId = entry.getKey();
                com.fasterxml.jackson.databind.JsonNode selectedIndexNode = entry.getValue();

                AssessmentQuestion question = questionMap.get(questionId);
                if (question == null) {
                    System.out.println("⚠️ No question found for ID: " + questionId
                        + " type=" + assessmentType);
                    continue;
                }

                com.fasterxml.jackson.databind.JsonNode optionsArray;
                try {
                    optionsArray = mapper.readTree(question.getOptionsJson());
                } catch (Exception parseEx) {
                    System.out.println("⚠️ Failed to parse optionsJson for "
                        + questionId + ": " + parseEx.getMessage());
                    continue;
                }

                String selectedOption = "";
                String dosha = "";

                // Check multi node first — use it for multi-select questions
                com.fasterxml.jackson.databind.JsonNode multiForQuestion =
                    multiNode.has(questionId) ? multiNode.get(questionId) : null;

                if (multiForQuestion != null && multiForQuestion.isArray()
                        && multiForQuestion.size() > 0) {
                    // Multi-select — use indices from multi node
                    List<String> labels = new ArrayList<>();
                    List<String> doshas = new ArrayList<>();
                    for (com.fasterxml.jackson.databind.JsonNode idxNode : multiForQuestion) {
                        int idx = idxNode.asInt();
                        if (optionsArray.isArray() && idx < optionsArray.size()) {
                            com.fasterxml.jackson.databind.JsonNode opt = optionsArray.get(idx);
                            labels.add(opt.has("label") ? opt.get("label").asText()
                            	    : opt.has("t") ? opt.get("t").asText()
                            	    : opt.asText());
                            if (opt.has("dosha")) doshas.add(opt.get("dosha").asText());
                        }
                    }
                    selectedOption = String.join(", ", labels);
                    dosha = String.join(", ", doshas);

                } else if (selectedIndexNode.isInt()) {
                    // Single select
                    int idx = selectedIndexNode.asInt();
                    if (optionsArray.isArray() && idx < optionsArray.size()) {
                        com.fasterxml.jackson.databind.JsonNode opt = optionsArray.get(idx);
                        selectedOption = opt.has("label") ? opt.get("label").asText()
                        	    : opt.has("t") ? opt.get("t").asText()
                        	    : opt.asText();
                        dosha = opt.has("dosha") ? opt.get("dosha").asText() : "";
                    }

                } else if (selectedIndexNode.isArray()) {
                    // Fallback — array directly in answers node
                    List<String> labels = new ArrayList<>();
                    List<String> doshas = new ArrayList<>();
                    for (com.fasterxml.jackson.databind.JsonNode idxNode : selectedIndexNode) {
                        int idx = idxNode.asInt();
                        if (optionsArray.isArray() && idx < optionsArray.size()) {
                            com.fasterxml.jackson.databind.JsonNode opt = optionsArray.get(idx);
                            labels.add(opt.has("label") ? opt.get("label").asText()
                            	    : opt.has("t") ? opt.get("t").asText()
                            	    : opt.asText());
                            if (opt.has("dosha")) doshas.add(opt.get("dosha").asText());
                        }
                    }
                    selectedOption = String.join(", ", labels);
                    dosha = String.join(", ", doshas);
                }

                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("questionId",     order);
                row.put("questionKey",    questionId);
                row.put("question",       question.getQuestionText());
                row.put("selectedOption", selectedOption);
                if (!dosha.isEmpty()) row.put("dosha", dosha);
                readableAnswers.add(row);
                order++;
            }

            java.util.Map<String, Object> output = new java.util.LinkedHashMap<>();
            output.put("readableAnswers", readableAnswers);

            // Preserve other top-level fields (flags, dosha, ama, medFlags etc)
            if (root.isObject()) {
                root.fields().forEachRemaining(e -> {
                    if (!e.getKey().equals("answers") && !e.getKey().equals("multi")) {
                        try {
                            output.put(e.getKey(),
                                mapper.treeToValue(e.getValue(), Object.class));
                        } catch (Exception ignored) {}
                    }
                });
            }

            return mapper.writeValueAsString(output);

        } catch (Exception e) {
            System.out.println("⚠️ buildReadableAnswers failed: " + e.getMessage());
            return answersJson;
        }
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