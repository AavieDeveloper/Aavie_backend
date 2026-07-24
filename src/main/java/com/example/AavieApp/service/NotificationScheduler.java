package com.example.AavieApp.service;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.UserAssessmentRepository;
import com.example.AavieApp.repository.CycleRepository;
import com.example.AavieApp.repository.UserProfileRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificationScheduler {

    private final UserProfileRepository userRepo;
    private final UserAssessmentRepository assessRepo;
    private final NotificationService notificationService;

    public NotificationScheduler(
        UserProfileRepository userRepo,
        UserAssessmentRepository assessRepo,
        NotificationService notificationService
    ) {
        this.userRepo = userRepo;
        this.assessRepo = assessRepo;
        this.notificationService = notificationService;
    }

    // ── Runs every day at 5:00 PM IST (11:30 UTC) ────────────────
    @Scheduled(cron = "0 50 17 * * *", zone = "UTC")
    public void sendDailyAssessmentReminders() {
        System.out.println("🔔 Running daily assessment reminder job: "
            + LocalDateTime.now());

        List<UserProfile> allUsers = userRepo.findAll();

        for (UserProfile user : allUsers) {
            if (user.getExpoPushToken() == null
                    || user.getExpoPushToken().isBlank()) continue;

            Long userId = user.getId();
            String name = user.getName() != null
                ? user.getName().split(" ")[0] : "there";

            boolean prakritiDone = assessRepo
                .existsByUserIdAndAssessmentType(userId, "PRAKRITI");
            boolean pcosDone = assessRepo
                .existsByUserIdAndAssessmentType(userId, "PCOS");
            boolean vikritiDone = assessRepo
                .existsByUserIdAndAssessmentType(userId, "VIKRITI");

            if (prakritiDone && pcosDone && vikritiDone) {
                notificationService.sendToUser(
                    userId,
                    "Your AAVIE daily check-in 🌿",
                    "Log today's symptoms and track your wellness journey."
                );
                continue;
            }

            String title;
            String body;

            if (!prakritiDone) {
                title = "Hey " + name + ", discover your body type 🌿";
                body = "Your Prakriti assessment takes 3 minutes. "
                    + "Find out how your body is wired — uniquely yours.";
            } else if (!pcosDone) {
                title = "One step closer, " + name + " 🌸";
                body = "You've completed Prakriti! "
                    + "Your Cycle Intelligence assessment is next — "
                    + "5 minutes to understand your cycle.";
            } else {
                title = "Last step, " + name + " ✨";
                body = "Complete your Vikriti assessment to unlock your "
                    + "personalised herb protocol. Takes just 3 minutes.";
            }

            notificationService.sendToUser(userId, title, body);
        }

        System.out.println("✅ Daily assessment reminders sent");
    }

    // ── Runs every day at 8:00 AM IST (02:30 UTC) ────────────────
    @Scheduled(cron = "0 30 2 * * *", zone = "UTC")
    public void sendPeriodMarkingReminder() {
        System.out.println("🔔 Running period marking reminder job");

        List<UserProfile> allUsers = userRepo.findAll();

        for (UserProfile user : allUsers) {
            if (user.getExpoPushToken() == null
                    || user.getExpoPushToken().isBlank()) continue;

            Long userId = user.getId();
            String name = user.getName() != null
                ? user.getName().split(" ")[0] : "there";

            boolean prakritiDone = assessRepo
                .existsByUserIdAndAssessmentType(userId, "PRAKRITI");
            if (!prakritiDone) continue;

            notificationService.sendToUser(
                userId,
                "Track your cycle today, " + name + " 🗓️",
                "Mark when your period started to unlock daily cycle insights, "
                + "phase guidance, and your personalised protocol."
            );
        }

        System.out.println("✅ Period marking reminders sent");
    }

    // ── Runs every Sunday at 9:00 AM IST (03:30 UTC) ─────────────
    @Scheduled(cron = "0 30 3 * * SUN", zone = "UTC")
    public void sendWeeklyReEngagement() {
        System.out.println("🔔 Running weekly re-engagement job");

        List<UserProfile> allUsers = userRepo.findAll();

        for (UserProfile user : allUsers) {
            if (user.getExpoPushToken() == null
                    || user.getExpoPushToken().isBlank()) continue;

            Long userId = user.getId();
            String name = user.getName() != null
                ? user.getName().split(" ")[0] : "there";

            boolean allDone = assessRepo
                .existsByUserIdAndAssessmentType(userId, "PRAKRITI")
                && assessRepo.existsByUserIdAndAssessmentType(userId, "PCOS")
                && assessRepo.existsByUserIdAndAssessmentType(userId, "VIKRITI");

            if (allDone) {
                notificationService.sendToUser(
                    userId,
                    "Your weekly check-in is ready 🌿",
                    "Log today's symptoms and see how your body is responding "
                    + "to your protocol this week."
                );
            } else {
                notificationService.sendToUser(
                    userId,
                    name + ", your body is waiting to be understood 🌸",
                    "Thousands of women have found clarity through AAVIE. "
                    + "Your personalised assessment takes just 5 minutes."
                );
            }
        }

        System.out.println("✅ Weekly re-engagement sent");
    }
}