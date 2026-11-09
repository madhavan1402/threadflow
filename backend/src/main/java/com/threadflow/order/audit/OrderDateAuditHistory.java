package com.threadflow.order.audit;

import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "order_date_audit_history")
public class OrderDateAuditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Column(name = "previous_ex_factory_date", nullable = false)
    private LocalDate previousExFactoryDate;

    @Column(name = "new_ex_factory_date", nullable = false)
    private LocalDate newExFactoryDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected OrderDateAuditHistory() {}

    public OrderDateAuditHistory(ProductionOrder order, LocalDate previousExFactoryDate,
                                 LocalDate newExFactoryDate, String reason, User changedBy) {
        this.order = order;
        this.previousExFactoryDate = previousExFactoryDate;
        this.newExFactoryDate = newExFactoryDate;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ProductionOrder getOrder() { return order; }
    public LocalDate getPreviousExFactoryDate() { return previousExFactoryDate; }
    public LocalDate getNewExFactoryDate() { return newExFactoryDate; }
    public String getReason() { return reason; }
    public User getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
