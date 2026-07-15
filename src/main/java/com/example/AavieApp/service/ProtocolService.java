package com.example.AavieApp.service;

import com.example.AavieApp.model.SupplementOrder;
import com.example.AavieApp.model.SupplementPlan;
import com.example.AavieApp.model.UserAssessment;
import com.example.AavieApp.repository.SupplementOrderRepository;
import com.example.AavieApp.repository.SupplementPlanRepository;
import com.example.AavieApp.repository.UserAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProtocolService {

    private final SupplementPlanRepository  planRepo;
    private final SupplementOrderRepository orderRepo;
    private final UserAssessmentRepository  assessRepo;

    public ProtocolService(
        SupplementPlanRepository  planRepo,
        SupplementOrderRepository orderRepo,
        UserAssessmentRepository  assessRepo
    ) {
        this.planRepo   = planRepo;
        this.orderRepo  = orderRepo;
        this.assessRepo = assessRepo;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DTOs
    // ════════════════════════════════════════════════════════════════════════

    /** Request body sent from OrderScreen after Razorpay success */
    public static class OrderRequest {
        private Long    userId;
        private String  paymentId;
        private Integer amount;
        private String  fullName;
        private String  phone;
        private String  addressLine1;
        private String  addressLine2;
        private String  city;
        private String  state;
        private String  pincode;

        public Long    getUserId()              { return userId; }
        public void    setUserId(Long v)        { this.userId = v; }
        public String  getPaymentId()           { return paymentId; }
        public void    setPaymentId(String v)   { this.paymentId = v; }
        public Integer getAmount()              { return amount; }
        public void    setAmount(Integer v)     { this.amount = v; }
        public String  getFullName()            { return fullName; }
        public void    setFullName(String v)    { this.fullName = v; }
        public String  getPhone()               { return phone; }
        public void    setPhone(String v)       { this.phone = v; }
        public String  getAddressLine1()        { return addressLine1; }
        public void    setAddressLine1(String v){ this.addressLine1 = v; }
        public String  getAddressLine2()        { return addressLine2; }
        public void    setAddressLine2(String v){ this.addressLine2 = v; }
        public String  getCity()                { return city; }
        public void    setCity(String v)        { this.city = v; }
        public String  getState()               { return state; }
        public void    setState(String v)       { this.state = v; }
        public String  getPincode()             { return pincode; }
        public void    setPincode(String v)     { this.pincode = v; }
    }

    /** Combined plan + order response returned to frontend */
    public static class OrderResponse {
        private Long    orderId;
        private Long    planId;
        private String  paymentId;
        private String  pcosType;
        private String  severity;
        private String  formulaVersion;
        private String  reviewStatus;
        private Boolean approvedForUser;
        private String  fullName;
        private String  phone;
        private String  addressLine1;
        private String  addressLine2;
        private String  city;
        private String  state;
        private String  pincode;
        private Integer amount;
        private String  deliveryStatus;
        private String  trackingId;
        private String  orderedAt;

        public Long    getOrderId()                  { return orderId; }
        public void    setOrderId(Long v)            { this.orderId = v; }
        public Long    getPlanId()                   { return planId; }
        public void    setPlanId(Long v)             { this.planId = v; }
        public String  getPaymentId()                { return paymentId; }
        public void    setPaymentId(String v)        { this.paymentId = v; }
        public String  getPcosType()                 { return pcosType; }
        public void    setPcosType(String v)         { this.pcosType = v; }
        public String  getSeverity()                 { return severity; }
        public void    setSeverity(String v)         { this.severity = v; }
        public String  getFormulaVersion()           { return formulaVersion; }
        public void    setFormulaVersion(String v)   { this.formulaVersion = v; }
        public String  getReviewStatus()             { return reviewStatus; }
        public void    setReviewStatus(String v)     { this.reviewStatus = v; }
        public Boolean getApprovedForUser()          { return approvedForUser; }
        public void    setApprovedForUser(Boolean v) { this.approvedForUser = v; }
        public String  getFullName()                 { return fullName; }
        public void    setFullName(String v)         { this.fullName = v; }
        public String  getPhone()                    { return phone; }
        public void    setPhone(String v)            { this.phone = v; }
        public String  getAddressLine1()             { return addressLine1; }
        public void    setAddressLine1(String v)     { this.addressLine1 = v; }
        public String  getAddressLine2()             { return addressLine2; }
        public void    setAddressLine2(String v)     { this.addressLine2 = v; }
        public String  getCity()                     { return city; }
        public void    setCity(String v)             { this.city = v; }
        public String  getState()                    { return state; }
        public void    setState(String v)            { this.state = v; }
        public String  getPincode()                  { return pincode; }
        public void    setPincode(String v)          { this.pincode = v; }
        public Integer getAmount()                   { return amount; }
        public void    setAmount(Integer v)          { this.amount = v; }
        public String  getDeliveryStatus()           { return deliveryStatus; }
        public void    setDeliveryStatus(String v)   { this.deliveryStatus = v; }
        public String  getTrackingId()               { return trackingId; }
        public void    setTrackingId(String v)       { this.trackingId = v; }
        public String  getOrderedAt()                { return orderedAt; }
        public void    setOrderedAt(String v)        { this.orderedAt = v; }
    }

    /** Minimal plan response for MyPlanScreen mount check */
    public static class PlanResponse {
        private Long    planId;
        private String  pcosType;
        private String  severity;
        private String  reviewStatus;
        private Boolean approvedForUser;
        private String  formulaVersion;
        private String  createdAt;

        public Long    getPlanId()                   { return planId; }
        public void    setPlanId(Long v)             { this.planId = v; }
        public String  getPcosType()                 { return pcosType; }
        public void    setPcosType(String v)         { this.pcosType = v; }
        public String  getSeverity()                 { return severity; }
        public void    setSeverity(String v)         { this.severity = v; }
        public String  getReviewStatus()             { return reviewStatus; }
        public void    setReviewStatus(String v)     { this.reviewStatus = v; }
        public Boolean getApprovedForUser()          { return approvedForUser; }
        public void    setApprovedForUser(Boolean v) { this.approvedForUser = v; }
        public String  getFormulaVersion()           { return formulaVersion; }
        public void    setFormulaVersion(String v)   { this.formulaVersion = v; }
        public String  getCreatedAt()                { return createdAt; }
        public void    setCreatedAt(String v)        { this.createdAt = v; }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CREATE ORDER
    //  Called after Razorpay payment succeeds in OrderScreen.
    //  1. Creates or reuses a supplement_plans row for this user
    //  2. Always creates a new supplement_orders row (one per payment)
    // ════════════════════════════════════════════════════════════════════════

    public OrderResponse createOrder(OrderRequest req) {

        // ── 1. Get or create the plan for this user ───────────────────────
        SupplementPlan plan = planRepo
            .findTopByUserIdOrderByCreatedAtDesc(req.getUserId())
            .orElseGet(() -> createNewPlan(req.getUserId()));

        // ── 2. Create the order row ───────────────────────────────────────
        SupplementOrder order = new SupplementOrder();
        order.setUserId(req.getUserId());
        order.setPlanId(plan.getId());
        order.setPaymentId(req.getPaymentId());
        order.setAmount(req.getAmount() != null ? req.getAmount() : 1499);
        order.setFullName(req.getFullName());
        order.setPhone(req.getPhone());
        order.setAddressLine1(req.getAddressLine1());
        order.setAddressLine2(req.getAddressLine2());
        order.setCity(req.getCity());
        order.setState(req.getState());
        order.setPincode(req.getPincode());
        order.setDeliveryStatus("vaidya_review");

        SupplementOrder savedOrder = orderRepo.save(order);
        return toOrderResponse(savedOrder, plan);
    }

    // ── Private: create a new plan from assessment data ───────────────────
    private SupplementPlan createNewPlan(Long userId) {
        String pcosType = null;
        String severity = null;

        Optional<UserAssessment> pcosOpt =
            assessRepo.findByUserIdAndAssessmentType(userId, "PCOS");
        if (pcosOpt.isPresent()) {
            pcosType = pcosOpt.get().getResultType();
            severity = pcosOpt.get().getSeverity();
        }

        if (severity == null) {
            Optional<UserAssessment> vikOpt =
                assessRepo.findByUserIdAndAssessmentType(userId, "VIKRITI");
            if (vikOpt.isPresent()) {
                severity = vikOpt.get().getSeverity();
            }
        }

        SupplementPlan plan = new SupplementPlan();
        plan.setUserId(userId);
        plan.setPcosType(pcosType);
        plan.setSeverity(severity);
        plan.setFormulaVersion("AAVIE_DOCTRINE_v4");
        plan.setReviewStatus("pending");
        plan.setApprovedForUser(false);

        return planRepo.save(plan);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET CURRENT PLAN
    //  Called on MyPlanScreen mount — just checks plan exists.
    // ════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PlanResponse getCurrentPlan(Long userId) {
        return planRepo
            .findTopByUserIdOrderByCreatedAtDesc(userId)
            .map(this::toPlanResponse)
            .orElse(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET ALL ORDERS
    //  Called by MyOrdersScreen — returns all orders with plan details.
    // ════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        List<SupplementOrder> orders =
            orderRepo.findByUserIdOrderByOrderedAtDesc(userId);

        return orders.stream().map(o -> {
            SupplementPlan plan = planRepo.findById(o.getPlanId())
                .orElse(null);
            return toOrderResponse(o, plan);
        }).collect(Collectors.toList());
    }
    
 // ════════════════════════════════════════════════════════════════════════
    //  GET ALL ORDERS FOR A GIVEN DATE (across all users)
    //  Called by the order-tracking admin dashboard's "today's orders" view.
    // ════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForDate(java.time.LocalDate date) {
        java.time.LocalDateTime startOfDay = date.atStartOfDay();
        java.time.LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);

        List<SupplementOrder> orders =
            orderRepo.findByOrderedAtBetweenOrderByOrderedAtDesc(startOfDay, endOfDay);

        return orders.stream().map(o -> {
            SupplementPlan plan = planRepo.findById(o.getPlanId()).orElse(null);
            return toOrderResponse(o, plan);
        }).collect(Collectors.toList());
    }
    
 // ════════════════════════════════════════════════════════════════════════
    //  UPDATE ORDER STATUS
    //  Called by the order-tracking admin panel. Changes deliveryStatus on
    //  an existing order. This is the write that the React Native app reads
    //  back via GET /orders/{userId} (o.deliveryStatus).
    // ════════════════════════════════════════════════════════════════════════

    private static final List<String> VALID_STATUSES = List.of(
        "vaidya_review", "blending", "dispatched", "delivered", "cancelled"
    );

    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException(
                "Invalid status: " + newStatus + ". Must be one of " + VALID_STATUSES
            );
        }

        SupplementOrder order = orderRepo.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setDeliveryStatus(newStatus);
        SupplementOrder saved = orderRepo.save(order);

        SupplementPlan plan = planRepo.findById(saved.getPlanId()).orElse(null);
        return toOrderResponse(saved, plan);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MAPPERS
    // ════════════════════════════════════════════════════════════════════════

    private OrderResponse toOrderResponse(SupplementOrder o, SupplementPlan p) {
        OrderResponse r = new OrderResponse();
        r.setOrderId(o.getId());
        r.setPlanId(o.getPlanId());
        r.setPaymentId(o.getPaymentId());
        r.setAmount(o.getAmount());
        r.setFullName(o.getFullName());
        r.setPhone(o.getPhone());
        r.setAddressLine1(o.getAddressLine1());
        r.setAddressLine2(o.getAddressLine2());
        r.setCity(o.getCity());
        r.setState(o.getState());
        r.setPincode(o.getPincode());
        r.setDeliveryStatus(o.getDeliveryStatus());
        r.setTrackingId(o.getTrackingId());
        r.setOrderedAt(o.getOrderedAt() != null
            ? o.getOrderedAt().toString() : null);

        if (p != null) {
            r.setPcosType(p.getPcosType());
            r.setSeverity(p.getSeverity());
            r.setFormulaVersion(p.getFormulaVersion());
            r.setReviewStatus(p.getReviewStatus());
            r.setApprovedForUser(p.getApprovedForUser());
        }

        return r;
    }

    private PlanResponse toPlanResponse(SupplementPlan p) {
        PlanResponse r = new PlanResponse();
        r.setPlanId(p.getId());
        r.setPcosType(p.getPcosType());
        r.setSeverity(p.getSeverity());
        r.setReviewStatus(p.getReviewStatus());
        r.setApprovedForUser(p.getApprovedForUser());
        r.setFormulaVersion(p.getFormulaVersion());
        r.setCreatedAt(p.getCreatedAt() != null
            ? p.getCreatedAt().toString() : null);
        return r;
    }
}