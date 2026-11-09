package com.threadflow.production;

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

@Entity
@Table(name = "supervisor_order_assignments")
public class SupervisorOrderAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false) private User supervisor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) private ProductionOrder order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by") private User assignedBy;
    @Column(name = "assigned_at", nullable = false, updatable = false) private Instant assignedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(nullable = false) private boolean active = true;

    protected SupervisorOrderAssignment() {}
    public SupervisorOrderAssignment(User supervisor, ProductionOrder order) {
        this(supervisor, order, null);
    }
    public SupervisorOrderAssignment(User supervisor, ProductionOrder order, User assignedBy) {
        this.supervisor = supervisor; this.order = order; this.assignedBy = assignedBy;
    }
    @PrePersist void onCreate() { assignedAt = Instant.now(); }
    public void revoke(Instant at) { active = false; revokedAt = at; }
    public Long getId() { return id; }
    public User getSupervisor() { return supervisor; }
    public ProductionOrder getOrder() { return order; }
    public User getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isActive() { return active; }
}
