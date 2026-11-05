package com.threadflow.order;

import com.threadflow.buyer.Buyer;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {
    public enum Status { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false) private Buyer buyer;
    @Column(name = "po_number", nullable = false, unique = true) private String poNumber;
    @Column(name = "style_name", nullable = false) private String styleName;
    @Column(name = "product_description") private String productDescription;
    @Column(name = "total_quantity", nullable = false) private Integer totalQuantity;
    @Column(name = "ex_factory_date", nullable = false) private LocalDate exFactoryDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.PLANNED;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderVariant> variants = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<OrderMilestone> milestones = new ArrayList<>();

    protected ProductionOrder() {}
    public ProductionOrder(Buyer buyer, String poNumber, String styleName, String productDescription,
                           Integer totalQuantity, LocalDate exFactoryDate, Long createdBy) {
        this.buyer = buyer; this.poNumber = poNumber; this.styleName = styleName;
        this.productDescription = productDescription; this.totalQuantity = totalQuantity;
        this.exFactoryDate = exFactoryDate; this.createdBy = createdBy;
    }
    public void update(Buyer buyer, String styleName, String productDescription, Integer totalQuantity,
                       LocalDate exFactoryDate, Status status) {
        this.buyer = buyer; this.styleName = styleName; this.productDescription = productDescription;
        this.totalQuantity = totalQuantity; this.exFactoryDate = exFactoryDate; this.status = status;
    }
    public void addVariant(OrderVariant variant) { variants.add(variant); }
    public void addMilestone(OrderMilestone milestone) { milestones.add(milestone); }
    public void replaceVariants(List<OrderVariant> replacements) { variants.clear(); replacements.forEach(this::addVariant); }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Buyer getBuyer() { return buyer; }
    public String getPoNumber() { return poNumber; }
    public String getStyleName() { return styleName; }
    public String getProductDescription() { return productDescription; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public LocalDate getExFactoryDate() { return exFactoryDate; }
    public Status getStatus() { return status; }
    public List<OrderVariant> getVariants() { return variants; }
    public List<OrderMilestone> getMilestones() { return milestones; }
}
