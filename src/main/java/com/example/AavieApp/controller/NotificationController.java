package com.example.AavieApp.controller;

import com.example.AavieApp.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Send to one user
    @PostMapping("/send")
    public ResponseEntity<?> sendToUser(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        return ResponseEntity.ok(notificationService.sendToUser(userId, title, message));
    }

    // Send to all users
    @PostMapping("/send-all")
    public ResponseEntity<?> sendToAll(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        return ResponseEntity.ok(notificationService.sendToAll(title, message));
    }

    // Send to users with pending assessments
    @PostMapping("/send-pending")
    public ResponseEntity<?> sendToPending(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("userIds");
        List<Long> userIds = ids.stream().map(Long::valueOf).toList();
        return ResponseEntity.ok(
            notificationService.sendToUsersWithPendingAssessment(title, message, userIds)
        );
    }
}