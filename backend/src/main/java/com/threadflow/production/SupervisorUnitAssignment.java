package com.threadflow.production;

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
@Table(name = "supervisor_unit_assignments")
public class SupervisorUnitAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false) private User supervisor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_unit_id", nullable = false) private ProductionUnit productionUnit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false) private User assignedBy;
    @Column(name = "assigned_at", nullable = false, updatable = false) private Instant assignedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(nullable = false) private boolean active = true;

    protected SupervisorUnitAssignment() {}

    public SupervisorUnitAssignment(User supervisor, ProductionUnit productionUnit, User assignedBy) {
        this.supervisor = supervisor;
        this.productionUnit = productionUnit;
        this.assignedBy = assignedBy;
    }

    @PrePersist void onCreate() { assignedAt = Instant.now(); }
    public void revoke(Instant at) { active = false; revokedAt = at; }
    public Long getId() { return id; }
    public User getSupervisor() { return supervisor; }
    public ProductionUnit getProductionUnit() { return productionUnit; }
    public User getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isActive() { return active; }
}