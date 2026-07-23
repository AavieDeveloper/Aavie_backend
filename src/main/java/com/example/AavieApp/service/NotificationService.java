package com.example.AavieApp.service;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final UserProfileRepository userRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NotificationService(UserProfileRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ── Send to one user ──────────────────────────────────────────
    public Map<String, Object> sendToUser(Long userId, String title, String body) {
        if (userRepo.findById(userId).isEmpty()) {
            return Map.of("success", false, "reason", "User not found");
        }
        UserProfile user = userRepo.findById(userId).get();
        if (user.getExpoPushToken() == null || user.getExpoPushToken().isBlank()) {
            return Map.of("success", false, "reason", "No push token for this user");
        }
        return sendPushNotification(List.of(user.getExpoPushToken()), title, body);
    }
    // ── Send to all users ─────────────────────────────────────────
    public Map<String, Object> sendToAll(String title, String body) {
        List<String> tokens = userRepo.findAll().stream()
            .map(UserProfile::getExpoPushToken)
            .filter(t -> t != null && !t.isBlank())
            .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return Map.of("success", false, "reason", "No users have push tokens");
        }
        return sendPushNotification(tokens, title, body);
    }

    // ── Send to users missing specific assessment ─────────────────
    public Map<String, Object> sendToUsersWithPendingAssessment(
            String title, String body, List<Long> userIds) {
        List<String> tokens = userRepo.findAllById(userIds).stream()
            .map(UserProfile::getExpoPushToken)
            .filter(t -> t != null && !t.isBlank())
            .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return Map.of("success", false, "reason", "No push tokens found");
        }
        return sendPushNotification(tokens, title, body);
    }

    // ── Core Expo push sender ─────────────────────────────────────
    private Map<String, Object> sendPushNotification(
            List<String> tokens, String title, String body) {
        try {
            // Build messages array — Expo accepts up to 100 per request
            List<Map<String, Object>> messages = tokens.stream().map(token -> {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("to", token);
                msg.put("title", title);
                msg.put("body", body);
                msg.put("sound", "default");
                msg.put("priority", "high");
                return msg;
            }).collect(Collectors.toList());

            String json = mapper.writeValueAsString(messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://exp.host/--/api/v2/push/send"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Expo push response: " + response.body());

            return Map.of(
                "success", true,
                "sent", tokens.size(),
                "response", response.body()
            );
        } catch (Exception e) {
            System.out.println("Push notification error: " + e.getMessage());
            return Map.of("success", false, "reason", e.getMessage());
        }
    }
}