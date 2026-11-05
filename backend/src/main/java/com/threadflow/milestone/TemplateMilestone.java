package com.threadflow.milestone;

import jakarta.persistence.*;

@Entity
@Table(name = "template_milestones")
public class TemplateMilestone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private MilestoneTemplate template;
    @Column(name = "milestone_name", nullable = false) private String milestoneName;
    @Column(name = "sequence_number", nullable = false) private Integer sequenceNumber;
    @Column(name = "default_duration_days", nullable = false) private Integer defaultDurationDays;

    protected TemplateMilestone() {}
    public TemplateMilestone(String milestoneName, Integer sequenceNumber, Integer defaultDurationDays) {
        this.milestoneName = milestoneName;
        this.sequenceNumber = sequenceNumber;
        this.defaultDurationDays = defaultDurationDays;
    }
    void assignTemplate(MilestoneTemplate template) { this.template = template; }
    public Long getId() { return id; }
    public String getMilestoneName() { return milestoneName; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public Integer getDefaultDurationDays() { return defaultDurationDays; }
}
