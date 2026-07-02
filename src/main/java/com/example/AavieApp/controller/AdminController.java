package com.example.AavieApp.controller;

import com.example.AavieApp.model.UserAssessment;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.ArticleRepository;
import com.example.AavieApp.repository.ManufacturingRunRepository;
import com.example.AavieApp.model.ManufacturingRun;
import com.example.AavieApp.repository.UserProfileRepository;



import com.example.AavieApp.repository.UserAssessmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.AavieApp.model.TongueReading;
import com.example.AavieApp.repository.TongueReadingRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.example.AavieApp.model.UserProfile;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserProfileRepository    userRepo;
    private final UserAssessmentRepository assessRepo;
    private final ArticleRepository        articleRepo;
    private final TongueReadingRepository tongueRepo;
    private final ManufacturingRunRepository mfgRepo;
    
    public AdminController(
    	    UserProfileRepository userRepo,
    	    UserAssessmentRepository assessRepo,
    	    ArticleRepository articleRepo,
    	    TongueReadingRepository tongueRepo,
    	    ManufacturingRunRepository mfgRepo
    	) {
    	    this.userRepo    = userRepo;
    	    this.assessRepo  = assessRepo;
    	    this.articleRepo = articleRepo;
    	    this.tongueRepo  = tongueRepo;
    	    this.mfgRepo     = mfgRepo;
    	}

    // ── Dashboard Stats ───────────────────────────────────────────────────────
    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats() {
        long totalUsers   = userRepo.count();
        long prakritiDone = assessRepo.countByAssessmentType("PRAKRITI");
        long pcosDone     = assessRepo.countByAssessmentType("PCOS");
        long vikritiDone  = assessRepo.countByAssessmentType("VIKRITI");
        long articlesLive = articleRepo.countByStatus("live");

        // ── Prakriti breakdown ────────────────────────────────────────────────────
        List<UserAssessment> prakritiList = assessRepo.findAllByAssessmentType("PRAKRITI");
        Map<String, Long> prakritiBreakdown = prakritiList.stream()
            .filter(a -> a.getResultType() != null)
            .collect(Collectors.groupingBy(UserAssessment::getResultType, Collectors.counting()));

        // ── Age group breakdown from UserProfile ──────────────────────────────────
        List<UserProfile> allUsers = userRepo.findAll();
        Map<String, Long> ageGroups = new java.util.LinkedHashMap<>();
        ageGroups.put("18-24", allUsers.stream().filter(u -> u.getAge() != null && u.getAge() >= 18 && u.getAge() <= 24).count());
        ageGroups.put("25-30", allUsers.stream().filter(u -> u.getAge() != null && u.getAge() >= 25 && u.getAge() <= 30).count());
        ageGroups.put("31-35", allUsers.stream().filter(u -> u.getAge() != null && u.getAge() >= 31 && u.getAge() <= 35).count());
        ageGroups.put("36-45", allUsers.stream().filter(u -> u.getAge() != null && u.getAge() >= 36 && u.getAge() <= 45).count());

        // ── Recent 5 users ────────────────────────────────────────────────────────
        List<Map<String, Object>> recentUsers = allUsers.stream()
            .sorted((a, b) -> b.getCreatedAt() != null && a.getCreatedAt() != null
                ? b.getCreatedAt().compareTo(a.getCreatedAt()) : 0)
            .limit(5)
            .map(u -> {
                Optional<UserAssessment> prakritiOpt = assessRepo
                    .findByUserIdAndAssessmentType(u.getId(), "PRAKRITI");
                Map<String, Object> user = new HashMap<>();
                user.put("id",             u.getId());
                user.put("name",           u.getName());
                user.put("age",            u.getAge());
                user.put("city",           u.getCity());
                user.put("prakritiResult", prakritiOpt.map(UserAssessment::getResultType).orElse(null));
                return user;
            })
            .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",        totalUsers);
        stats.put("assessmentsDone",   prakritiDone);
        stats.put("prakritiDone",      prakritiDone);
        stats.put("pcosDone",          pcosDone);
        stats.put("vikritiDone",       vikritiDone);
        stats.put("articlesLive",      articlesLive);
        stats.put("activeThisMonth",   totalUsers);
        stats.put("weeklyGrowth",      0);
        stats.put("monthlyDelta",      0);
        stats.put("prakritiBreakdown", prakritiBreakdown);
        stats.put("ageGroups",         ageGroups);
        stats.put("recentUsers",       recentUsers);
        return stats;
    }
    
    
    // TO:
    @GetMapping("/users/stats")
    public Map<String, Object> getUserStats() {
        long totalUsers   = userRepo.count();
        long prakritiDone = assessRepo.countByAssessmentType("PRAKRITI");
        long pcosDone     = assessRepo.countByAssessmentType("PCOS");
        long vikritiDone  = assessRepo.countByAssessmentType("VIKRITI");
        return Map.of(
            "totalUsers",   totalUsers,
            "prakritiDone", prakritiDone,
            "pcosDone",     pcosDone,
            "vikritiDone",  vikritiDone
        );
    }

    // ── All Users — OPTIMISED (2 queries instead of 3 per user) ──────────────
    @GetMapping("/users")
    public Map<String, Object> getUsers(
        @RequestParam(defaultValue = "0")   int page,
        @RequestParam(defaultValue = "200") int size,
        @RequestParam(defaultValue = "")    String search,
        @RequestParam(defaultValue = "all") String status
    ) {
        List<UserProfile> all = userRepo.findAll();

        // Search filter
        if (!search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream().filter(u ->
                (u.getName()  != null && u.getName().toLowerCase().contains(q)) ||
                (u.getCity()  != null && u.getCity().toLowerCase().contains(q)) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        // ── ONE bulk query for all assessments instead of 3 queries per user ──────
        // Previously: 100 users = 300 database queries
        // Now:        100 users = 2 database queries total
        List<Long> userIds = all.stream()
            .map(UserProfile::getId)
            .collect(Collectors.toList());

        List<UserAssessment> allAssessments = userIds.isEmpty()
            ? List.of()
            : assessRepo.findByUserIdIn(userIds);

        // Build O(1) lookup map: "userId_ASSESSMENTTYPE" -> resultType
        Map<String, String> assessmentMap = new HashMap<>();
        for (UserAssessment a : allAssessments) {
            String key = a.getUserId() + "_" + a.getAssessmentType();
            assessmentMap.put(key, a.getResultType());
        }

        // Map to response using the lookup map — no DB call per user
        List<Map<String, Object>> users = all.stream().map(u -> {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",             u.getId());
            user.put("name",           u.getName());
            user.put("age",            u.getAge());
            user.put("city",           u.getCity());
            user.put("email",          u.getEmail() != null ? u.getEmail() : "");
            user.put("gender",         u.getGender());
            user.put("height",         u.getHeight());            // ← ADD THIS
            user.put("weight",         u.getWeight());  
            user.put("mobileNumber",   u.getMobileNumber());  // ← ADD THIS LINE
            user.put("prakritiResult", assessmentMap.get(u.getId() + "_PRAKRITI"));
            user.put("pcosResult",     assessmentMap.get(u.getId() + "_PCOS"));
            user.put("vikritiResult",  assessmentMap.get(u.getId() + "_VIKRITI"));
            user.put("joinedAt",       u.getCreatedAt() != null ?
                u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "");
            return user;
        }).collect(Collectors.toList());

        // Status filter
        if ("complete".equals(status)) {
            users = users.stream()
                .filter(u -> u.get("prakritiResult") != null && u.get("pcosResult") != null)
                .collect(Collectors.toList());
        } else if ("pending".equals(status)) {
            users = users.stream()
                .filter(u -> u.get("prakritiResult") == null || u.get("pcosResult") == null)
                .collect(Collectors.toList());
        }

        // Pagination
        int total = users.size();
        int from  = page * size;
        int to    = Math.min(from + size, total);
        List<Map<String, Object>> paged = from < total ? users.subList(from, to) : List.of();

        return Map.of("users", paged, "total", total, "page", page, "size", size);
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        return userRepo.findById(userId).map(u -> {
            // Delete user's assessments first
            assessRepo.deleteByUserId(userId);
            // Delete user profile
            userRepo.delete(u);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Single User ───────────────────────────────────────────────────────────
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        return userRepo.findById(userId).map(u -> {
            Optional<UserAssessment> prakritiOpt = assessRepo.findByUserIdAndAssessmentType(userId, "PRAKRITI");
            Optional<UserAssessment> pcosOpt     = assessRepo.findByUserIdAndAssessmentType(userId, "PCOS");
            Optional<UserAssessment> vikritiOpt  = assessRepo.findByUserIdAndAssessmentType(userId, "VIKRITI");
            Optional<TongueReading>  tongueOpt   = tongueRepo.findTopByUserIdOrderByCreatedAtDesc(userId);
            Optional<ManufacturingRun> mfgOpt    = mfgRepo.findByUserIdAndAssessmentType(userId, "PCOS");  // ← ADD

            Map<String, Object> user = new HashMap<>();
            user.put("id",             u.getId());
            user.put("name",           u.getName());
            user.put("age",            u.getAge());
            user.put("city",           u.getCity());
            user.put("email",          u.getEmail() != null ? u.getEmail() : "");
            user.put("gender",         u.getGender());
            user.put("height",         u.getHeight());
            user.put("weight",         u.getWeight());
            user.put("mobileNumber",   u.getMobileNumber());  // ← ADD THIS LINE
            
            user.put("prakritiResult", prakritiOpt.map(UserAssessment::getResultType).orElse(null));
            user.put("pcosResult",     pcosOpt.map(UserAssessment::getResultType).orElse(null));
            user.put("pcosSeverity",   pcosOpt.map(UserAssessment::getSeverity).orElse(null));
            user.put("vikritiResult",  vikritiOpt.map(UserAssessment::getResultType).orElse(null));
            
            // ── Tongue analysis ───────────────────────────────────────────────────
            tongueOpt.ifPresent(t -> {
                user.put("tongueAnalysis", buildTongueAnalysisText(t));
            });
            
            // ── Manufacturing/Formula data ────────────────────────────────────────
            mfgOpt.ifPresent(mfg -> {
                Map<String, Object> formula = new HashMap<>();
                formula.put("formulaJson", mfg.getFormulaJson());
                formula.put("doshaPct",    mfg.getDoshaPct());
                formula.put("severity",    mfg.getSeverity());
                formula.put("prakriti",    mfg.getPrakriti());
                formula.put("revealed",    mfg.isRevealed());
                formula.put("createdAt",   mfg.getCreatedAt() != null ?
                    mfg.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")) : "");
                user.put("herbFormula", formula);
            });
            
            user.put("joinedAt", u.getCreatedAt() != null ?
                u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "");
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }
    // ── Assessment Overview ───────────────────────────────────────────────────
    @GetMapping("/assessments/overview")
    public Map<String, Object> getAssessmentOverview() {
        return Map.of(
            "prakritiDone", assessRepo.countByAssessmentType("PRAKRITI"),
            "pcosDone",     assessRepo.countByAssessmentType("PCOS"),
            "vikritiDone",  assessRepo.countByAssessmentType("VIKRITI")
        );
    }

    // ── Assessment Stats with Breakdown ──────────────────────────────────────
    @GetMapping("/assessments/stats")
    public Map<String, Object> getAssessmentStats() {
        long totalUsers   = userRepo.count();
        long prakritiDone = assessRepo.countByAssessmentType("PRAKRITI");
        long pcosDone     = assessRepo.countByAssessmentType("PCOS");
        long vikritiDone  = assessRepo.countByAssessmentType("VIKRITI");

        List<UserAssessment> pcosAssessments = assessRepo.findAllByAssessmentType("PCOS");
        Map<String, Long> pcosBreakdown = pcosAssessments.stream()
            .filter(a -> a.getResultType() != null)
            .collect(Collectors.groupingBy(UserAssessment::getResultType, Collectors.counting()));

        List<UserAssessment> prakritiAssessments = assessRepo.findAllByAssessmentType("PRAKRITI");
        Map<String, Long> prakritiBreakdown = prakritiAssessments.stream()
            .filter(a -> a.getResultType() != null)
            .collect(Collectors.groupingBy(UserAssessment::getResultType, Collectors.counting()));

        // ── ADD vikriti breakdown ─────────────────────────────────────────────────
        List<UserAssessment> vikritiAssessments = assessRepo.findAllByAssessmentType("VIKRITI");
        Map<String, Long> vikritiBreakdown = vikritiAssessments.stream()
            .filter(a -> a.getResultType() != null)
            .collect(Collectors.groupingBy(UserAssessment::getResultType, Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",        totalUsers);
        stats.put("prakritiDone",      prakritiDone);
        stats.put("pcosDone",          pcosDone);
        stats.put("vikritiDone",       vikritiDone);
        stats.put("prakritiBreakdown", prakritiBreakdown);
        stats.put("pcosBreakdown",     pcosBreakdown);
        stats.put("vikritiBreakdown",  vikritiBreakdown);
        return stats;
    }

    // ── Recent Activity ───────────────────────────────────────────────────────
    @GetMapping("/activity")
    public List<Map<String, Object>> getRecentActivity() {
        List<UserAssessment> recent = assessRepo.findTop10ByOrderByUpdatedAtDesc();
        return recent.stream().map(a -> {
            Map<String, Object> act = new HashMap<>();
            act.put("type",       a.getAssessmentType());
            act.put("resultType", a.getResultType());
            act.put("userId",     a.getUserId());
            act.put("updatedAt",  a.getUpdatedAt() != null ?
                a.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) : "");
            return act;
        }).collect(Collectors.toList());
    }
    
    private String buildTongueAnalysisText(TongueReading t) {
        StringBuilder sb = new StringBuilder();
        
        if (t.getOneLineInsight() != null && !t.getOneLineInsight().isBlank()) {
            sb.append(t.getOneLineInsight()).append("\n\n");
        }
        
        sb.append("Dominant Dosha Imbalance: ")
          .append(t.getDominantDoshaImbalance() != null ? t.getDominantDoshaImbalance() : "—")
          .append("\n");
        
        sb.append("Ama Level: ")
          .append(t.getAmaLevel() != null ? t.getAmaLevel() : "—")
          .append("\n");
        
        sb.append("Agni State: ")
          .append(t.getAgniState() != null ? t.getAgniState() : "—")
          .append("\n");
        
        if (t.getCoatingLocation() != null && !t.getCoatingLocation().equals("none")) {
            sb.append("Coating Location: ").append(t.getCoatingLocation()).append("\n");
        }
        
        return sb.toString().trim();
    }
}