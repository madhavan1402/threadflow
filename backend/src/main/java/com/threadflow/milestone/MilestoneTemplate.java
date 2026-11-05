package com.threadflow.milestone;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "milestone_templates")
public class MilestoneTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "template_name", nullable = false) private String templateName;
    private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<TemplateMilestone> milestones = new ArrayList<>();

    protected MilestoneTemplate() {}
    public MilestoneTemplate(String templateName, String description, boolean active) {
        this.templateName = templateName;
        this.description = description;
        this.active = active;
    }
    public void addMilestone(TemplateMilestone milestone) {
        milestones.add(milestone);
        milestone.assignTemplate(this);
    }
    public Long getId() { return id; }
    public String getTemplateName() { return templateName; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public List<TemplateMilestone> getMilestones() { return milestones; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
