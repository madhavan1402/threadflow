package com.threadflow.order.warning;

import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "buyer_delay_warnings")
public class BuyerDelayWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BuyerDelayWarningStatus status = BuyerDelayWarningStatus.PENDING_CONFIRMATION;

    @Column(name = "risk_status", nullable = false, length = 32)
    private String riskStatus;

    @Column(name = "internal_reasons", columnDefinition = "TEXT")
    private String internalReasons;

    @Column(name = "suggested_message", nullable = false, columnDefinition = "TEXT")
    private String suggestedMessage;

    @Column(name = "confirmed_message", columnDefinition = "TEXT")
    private String confirmedMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BuyerDelayWarning() {}

    public BuyerDelayWarning(ProductionOrder order, String riskStatus, String internalReasons, String suggestedMessage) {
        this.order = order;
        this.riskStatus = riskStatus;
        this.internalReasons = internalReasons;
        this.suggestedMessage = suggestedMessage;
        this.status = BuyerDelayWarningStatus.PENDING_CONFIRMATION;
    }

    public void confirm(String message, User merchandiser) {
        this.confirmedMessage = message == null || message.isBlank() ? this.suggestedMessage : message.trim();
        this.confirmedBy = merchandiser;
        this.confirmedAt = Instant.now();
        this.status = BuyerDelayWarningStatus.CONFIRMED;
    }

    public void dismiss() {
        this.status = BuyerDelayWarningStatus.DISMISSED;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ProductionOrder getOrder() { return order; }
    public BuyerDelayWarningStatus getStatus() { return status; }
    public String getRiskStatus() { return riskStatus; }
    public String getInternalReasons() { return internalReasons; }
    public String getSuggestedMessage() { return suggestedMessage; }
    public String getConfirmedMessage() { return confirmedMessage; }
    public User getConfirmedBy() { return confirmedBy; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
