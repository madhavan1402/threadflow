package com.threadflow.issue;

import com.threadflow.production.ProductionUpdate;
import com.threadflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "production_issue_classifications")
public class ProductionIssue {
    @Id
    @Column(name = "production_update_id") private Long id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_update_id", nullable = false) private ProductionUpdate productionUpdate;
    @Column(name = "detected_language", length = 16) private String detectedLanguage;
    @Column(name = "english_translation", columnDefinition = "TEXT") private String englishTranslation;
    @Enumerated(EnumType.STRING) @Column(name = "predicted_category", length = 32) private IssueCategory predictedCategory;
    @Column(precision = 4, scale = 3) private BigDecimal confidence;
    @Enumerated(EnumType.STRING) @Column(name = "classification_status", nullable = false, length = 32)
    private IssueClassificationStatus status = IssueClassificationStatus.PENDING;
    @Column(name = "provider_id", length = 100) private String providerId;
    @Column(name = "model_id", length = 255) private String modelId;
    @Enumerated(EnumType.STRING) @Column(name = "reviewed_category", length = 32) private IssueCategory reviewedCategory;
    @Column(name = "reviewed_translation", columnDefinition = "TEXT") private String reviewedTranslation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id") private User reviewer;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ProductionIssue() {}

    public ProductionIssue(ProductionUpdate productionUpdate) {
        this.productionUpdate = productionUpdate;
    }

    public void classify(IssueClassificationResult result, String providerId, String modelId) {
        this.detectedLanguage = result.detectedLanguage();
        this.englishTranslation = result.englishTranslation();
        this.predictedCategory = result.category();
        this.confidence = result.confidence();
        this.providerId = providerId;
        this.modelId = modelId;
        this.status = IssueClassificationStatus.PENDING_REVIEW;
    }

    public void markAiUnavailable() {
        clearPrediction();
        status = IssueClassificationStatus.AI_UNAVAILABLE;
    }

    public void markClassificationFailed() {
        clearPrediction();
        status = IssueClassificationStatus.CLASSIFICATION_FAILED;
    }

    public void requeue() {
        if (status == IssueClassificationStatus.REVIEWED) {
            throw new IllegalStateException("Reviewed issue classifications cannot be reclassified");
        }
        clearPrediction();
        status = IssueClassificationStatus.PENDING;
    }

    public void review(IssueCategory category, String translation, User reviewer, Instant reviewedAt) {
        reviewedCategory = category;
        reviewedTranslation = translation;
        this.reviewer = reviewer;
        this.reviewedAt = reviewedAt;
        status = IssueClassificationStatus.REVIEWED;
    }

    private void clearPrediction() {
        detectedLanguage = null;
        englishTranslation = null;
        predictedCategory = null;
        confidence = null;
        providerId = null;
        modelId = null;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public ProductionUpdate getProductionUpdate() { return productionUpdate; }
    public String getOriginalNote() { return productionUpdate.getIssueNote(); }
    public String getDetectedLanguage() { return detectedLanguage; }
    public String getEnglishTranslation() { return englishTranslation; }
    public IssueCategory getPredictedCategory() { return predictedCategory; }
    public BigDecimal getConfidence() { return confidence; }
    public IssueClassificationStatus getStatus() { return status; }
    public String getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public IssueCategory getReviewedCategory() { return reviewedCategory; }
    public String getReviewedTranslation() { return reviewedTranslation; }
    public User getReviewer() { return reviewer; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}