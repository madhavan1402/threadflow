package com.threadflow.issue;

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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "production_issue_reviews")
public class ProductionIssueReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_update_id", nullable = false) private ProductionIssue issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false) private User reviewer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private IssueReviewDecision decision;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private IssueCategory category;
    @Column(name = "english_translation", columnDefinition = "TEXT") private String englishTranslation;
    @Column(name = "reviewed_at", nullable = false, updatable = false) private Instant reviewedAt;

    protected ProductionIssueReview() {}

    public ProductionIssueReview(ProductionIssue issue, User reviewer, IssueReviewDecision decision,
                                 IssueCategory category, String englishTranslation, Instant reviewedAt) {
        this.issue = issue;
        this.reviewer = reviewer;
        this.decision = decision;
        this.category = category;
        this.englishTranslation = englishTranslation;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() { return id; }
    public ProductionIssue getIssue() { return issue; }
    public User getReviewer() { return reviewer; }
    public IssueReviewDecision getDecision() { return decision; }
    public IssueCategory getCategory() { return category; }
    public String getEnglishTranslation() { return englishTranslation; }
    public Instant getReviewedAt() { return reviewedAt; }
}