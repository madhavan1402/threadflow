package com.threadflow.production;

import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "supervisor_order_assignments")
public class SupervisorOrderAssignment {
    @EmbeddedId private AssignmentId id = new AssignmentId();
    @MapsId("supervisorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id") private User supervisor;
    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id") private ProductionOrder order;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected SupervisorOrderAssignment() {}
    public SupervisorOrderAssignment(User supervisor, ProductionOrder order) {
        this.supervisor = supervisor; this.order = order;
        this.id = new AssignmentId(supervisor.getId(), order.getId());
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public boolean isPresent() { return true; }
    public ProductionOrder getOrder() { return order; }

    @Embeddable
    public static class AssignmentId implements Serializable {
        @Column(name = "supervisor_id") private Long supervisorId;
        @Column(name = "order_id") private Long orderId;
        public AssignmentId() {}
        public AssignmentId(Long supervisorId, Long orderId) { this.supervisorId = supervisorId; this.orderId = orderId; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AssignmentId that)) return false;
            return java.util.Objects.equals(supervisorId, that.supervisorId)
                    && java.util.Objects.equals(orderId, that.orderId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(supervisorId, orderId); }
    }
}
