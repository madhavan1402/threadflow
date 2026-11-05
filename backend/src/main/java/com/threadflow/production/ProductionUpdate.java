package com.threadflow.production;

import com.threadflow.order.OrderMilestone;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_updates")
public class ProductionUpdate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private ProductionOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "milestone_id", nullable = false) private OrderMilestone milestone;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supervisor_id", nullable = false) private User supervisor;
    @Column(name = "production_date", nullable = false) private LocalDate productionDate;
    @Column(name = "completed_quantity", nullable = false) private Integer completedQuantity;
    @Column(name = "issue_note", columnDefinition = "TEXT") private String issueNote;
    @Column(name = "idempotency_key", nullable = false, length = 64) private String idempotencyKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "productionUpdate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionPhoto> photos = new ArrayList<>();

    protected ProductionUpdate() {}
    public ProductionUpdate(ProductionOrder order, OrderMilestone milestone, User supervisor, LocalDate productionDate,
                            Integer completedQuantity, String issueNote, String idempotencyKey) {
        this.order = order; this.milestone = milestone; this.supervisor = supervisor;
        this.productionDate = productionDate; this.completedQuantity = completedQuantity;
        this.issueNote = issueNote; this.idempotencyKey = idempotencyKey;
    }
    public void addPhoto(ProductionPhoto photo) { photos.add(photo); }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public ProductionOrder getOrder() { return order; }
    public OrderMilestone getMilestone() { return milestone; }
    public User getSupervisor() { return supervisor; }
    public LocalDate getProductionDate() { return productionDate; }
    public Integer getCompletedQuantity() { return completedQuantity; }
    public String getIssueNote() { return issueNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ProductionPhoto> getPhotos() { return photos; }
}
