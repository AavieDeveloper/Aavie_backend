package com.example.AavieApp.controller;

import com.example.AavieApp.model.SupplementOrder;
import com.example.AavieApp.service.ProtocolService;

import com.example.AavieApp.service.ProtocolService.OrderRequest;
import com.example.AavieApp.service.ProtocolService.OrderResponse;
import com.example.AavieApp.service.ProtocolService.PlanResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
@RestController
@RequestMapping("/api/protocol")
@CrossOrigin(origins = "*")
public class ProtocolController {

    private final ProtocolService protocolService;
    private final com.example.AavieApp.repository.SupplementOrderRepository orderRepo;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public ProtocolController(
        ProtocolService protocolService,
        com.example.AavieApp.repository.SupplementOrderRepository orderRepo
    ) {
        this.protocolService = protocolService;
        this.orderRepo = orderRepo;
    }

    // ── POST /api/protocol/order ──────────────────────────────────────────────
    // Called from OrderScreen AFTER Razorpay payment succeeds.
    // Creates one supplement_plans row (if none exists) + one supplement_orders row.
    // Body: { userId, paymentId, amount, fullName, phone,
    //         addressLine1, addressLine2, city, state, pincode }
    
 // ── GET /api/protocol/config ─────────────────────────────────────────────
 // Returns only KEY_ID to frontend — secret never leaves backend
 @GetMapping("/config")
 public ResponseEntity<?> getConfig() {
     return ResponseEntity.ok(Map.of(
         "razorpayKeyId", razorpayKeyId
     ));
 }
 
//── POST /api/protocol/razorpay/create-order ─────────────────────────────
//Creates Razorpay order — amount is FIXED server-side, never trusted from frontend
@PostMapping("/razorpay/create-order")
public ResponseEntity<?> createRazorpayOrder(@RequestBody Map<String, Object> body) {
  try {
      if (body.get("userId") == null) {
          return ResponseEntity.badRequest()
              .body(Map.of("message", "userId is required"));
      }
      Long userId = Long.valueOf(body.get("userId").toString());

      // FIXED server-side price — never trust amount from frontend
      final int FIXED_PRICE_PAISE = 149900; // ₹1499 in paise

      String credentials = Base64.getEncoder()
          .encodeToString((razorpayKeyId + ":" + razorpayKeySecret)
              .getBytes(StandardCharsets.UTF_8));

      String requestBody = String.format(
          "{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"aavie_user_%d\"}",
          FIXED_PRICE_PAISE, userId
      );

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://api.razorpay.com/v1/orders"))
          .header("Authorization", "Basic " + credentials)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = client.send(
          request, HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() == 200) {
          return ResponseEntity.ok(response.body());
      } else {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Razorpay order creation failed"));
      }
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Failed to create order: " + e.getMessage()));
  }
}

//── POST /api/protocol/razorpay/verify ───────────────────────────────────
//Verifies Razorpay signature + checks for duplicate payment
@PostMapping("/razorpay/verify")
public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
 try {
     String orderId   = body.get("razorpay_order_id");
     String paymentId = body.get("razorpay_payment_id");
     String signature = body.get("razorpay_signature");

     if (orderId == null || paymentId == null || signature == null) {
         return ResponseEntity.badRequest()
             .body(Map.of("verified", false, "message", "Missing fields"));
     }

     // Check duplicate payment
     if (orderRepo.existsByPaymentId(paymentId)) {
         return ResponseEntity.status(HttpStatus.CONFLICT)
             .body(Map.of("verified", false, "message", "Payment already processed"));
     }

     // Verify HMAC signature
     String payload = orderId + "|" + paymentId;
     Mac mac = Mac.getInstance("HmacSHA256");
     SecretKeySpec secretKey = new SecretKeySpec(
         razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
     );
     mac.init(secretKey);
     byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

     StringBuilder hexString = new StringBuilder();
     for (byte b : hash) {
         String hex = Integer.toHexString(0xff & b);
         if (hex.length() == 1) hexString.append('0');
         hexString.append(hex);
     }

     boolean valid = hexString.toString().equals(signature);

     if (valid) {
         return ResponseEntity.ok(Map.of("verified", true));
     } else {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
             .body(Map.of("verified", false, "message", "Signature mismatch"));
     }
 } catch (Exception e) {
     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
         .body(Map.of("message", "Verification failed: " + e.getMessage()));
 }
}

    
    @PostMapping("/order")
    public ResponseEntity<?> order(@RequestBody OrderRequest req) {
        try {
            if (req.getUserId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "userId is required"));
            }
            if (req.getPaymentId() == null || req.getPaymentId().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "paymentId is required"));
            }
            OrderResponse response = protocolService.createOrder(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to create order: " + e.getMessage()));
        }
    }

    // ── GET /api/protocol/current/{userId} ────────────────────────────────────
    // Called on MyPlanScreen mount — returns most recent plan or 204.
    @GetMapping("/current/{userId}")
    public ResponseEntity<?> getCurrent(@PathVariable Long userId) {
        try {
            PlanResponse plan = protocolService.getCurrentPlan(userId);
            if (plan == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch plan: " + e.getMessage()));
        }
    }
    
    
    
 // ── GET /api/protocol/orders/{userId}/can-reorder ─────────────────────────
 // Returns whether user can place a new order + days remaining if not
 @GetMapping("/orders/{userId}/can-reorder")
 public ResponseEntity<?> canReorder(@PathVariable Long userId) {
     try {
         Optional<SupplementOrder> latestOpt =
             orderRepo.findTopByUserIdOrderByOrderedAtDesc(userId);

         if (latestOpt.isEmpty()) {
             return ResponseEntity.ok(Map.of(
                 "canOrder", true,
                 "daysRemaining", 0
             ));
         }

         SupplementOrder latest = latestOpt.get();
         if (latest.getOrderedAt() == null) {
             return ResponseEntity.ok(Map.of(
                 "canOrder", true,
                 "daysRemaining", 0
             ));
         }

         long daysSince = java.time.temporal.ChronoUnit.DAYS.between(
             latest.getOrderedAt(),
             java.time.LocalDateTime.now()
         );
         long daysRemaining = 30 - daysSince;

         if (daysRemaining <= 0) {
             return ResponseEntity.ok(Map.of(
                 "canOrder", true,
                 "daysRemaining", 0
             ));
         }

         return ResponseEntity.ok(Map.of(
             "canOrder", false,
             "daysRemaining", (int) daysRemaining
         ));
     } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(Map.of("message", "Failed to check reorder: " + e.getMessage()));
     }
 }
    
    

    // ── GET /api/protocol/orders/{userId} ─────────────────────────────────────
    // Called on MyOrdersScreen — returns all orders newest first.
    // Each item includes plan details (pcosType, severity) + order details
    // (paymentId, address, deliveryStatus, trackingId).
    @GetMapping("/orders/{userId}")
    public ResponseEntity<?> getOrders(@PathVariable Long userId) {
        try {
            List<OrderResponse> orders =
                protocolService.getOrdersForUser(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch orders: " + e.getMessage()));
        }
    }
    
 // ── PATCH /api/protocol/orders/{orderId}/status ───────────────────────────
    // Called by the order-tracking admin panel to change delivery status.
    // Body: { "status": "dispatched" }
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
        @PathVariable Long orderId,
        @RequestBody Map<String, String> body
    ) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "status is required"));
            }
            OrderResponse response = protocolService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to update order status: " + e.getMessage()));
        }
    }
}