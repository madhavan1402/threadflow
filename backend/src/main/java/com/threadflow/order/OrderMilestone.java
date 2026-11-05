package com.threadflow.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "order_milestones")
public class OrderMilestone {
    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;
    @Column(name = "milestone_name", nullable = false) private String milestoneName;
    @Column(name = "sequence_number", nullable = false) private Integer sequenceNumber;
    @Column(name = "planned_start_date", nullable = false) private LocalDate plannedStartDate;
    @Column(name = "planned_end_date", nullable = false) private LocalDate plannedEndDate;
    @Column(name = "actual_start_date") private LocalDate actualStartDate;
    @Column(name = "actual_completion_date") private LocalDate actualCompletionDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.NOT_STARTED;
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected OrderMilestone() {}
    public OrderMilestone(ProductionOrder order, String milestoneName, Integer sequenceNumber,
                          LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.order = order; this.milestoneName = milestoneName; this.sequenceNumber = sequenceNumber;
        this.plannedStartDate = plannedStartDate; this.plannedEndDate = plannedEndDate;
    }
    public void update(Status status, LocalDate actualStart, LocalDate actualCompletion, String notes) {
        this.status = status; this.actualStartDate = actualStart; this.actualCompletionDate = actualCompletion; this.notes = notes;
    }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getMilestoneName() { return milestoneName; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
}
