package com.example.AavieApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplement_orders")
public class SupplementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address_line1", length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "delivery_status", length = 30)
    private String deliveryStatus = "vaidya_review";

    @Column(name = "tracking_id", length = 100)
    private String trackingId;

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        orderedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId()                           { return id; }
    public void setId(Long v)                     { this.id = v; }
    public Long getUserId()                       { return userId; }
    public void setUserId(Long v)                 { this.userId = v; }
    public Long getPlanId()                       { return planId; }
    public void setPlanId(Long v)                 { this.planId = v; }
    public String getPaymentId()                  { return paymentId; }
    public void setPaymentId(String v)            { this.paymentId = v; }
    public Integer getAmount()                    { return amount; }
    public void setAmount(Integer v)              { this.amount = v; }
    public String getFullName()                   { return fullName; }
    public void setFullName(String v)             { this.fullName = v; }
    public String getPhone()                      { return phone; }
    public void setPhone(String v)                { this.phone = v; }
    public String getAddressLine1()               { return addressLine1; }
    public void setAddressLine1(String v)         { this.addressLine1 = v; }
    public String getAddressLine2()               { return addressLine2; }
    public void setAddressLine2(String v)         { this.addressLine2 = v; }
    public String getCity()                       { return city; }
    public void setCity(String v)                 { this.city = v; }
    public String getState()                      { return state; }
    public void setState(String v)                { this.state = v; }
    public String getPincode()                    { return pincode; }
    public void setPincode(String v)              { this.pincode = v; }
    public String getDeliveryStatus()             { return deliveryStatus; }
    public void setDeliveryStatus(String v)       { this.deliveryStatus = v; }
    public String getTrackingId()                 { return trackingId; }
    public void setTrackingId(String v)           { this.trackingId = v; }
    public LocalDateTime getOrderedAt()           { return orderedAt; }
    public void setOrderedAt(LocalDateTime v)     { this.orderedAt = v; }
    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)     { this.updatedAt = v; }
}