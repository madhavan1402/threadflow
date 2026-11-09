package com.threadflow.issue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class IssueDtos {
    private IssueDtos() {}

    public record ReviewRequest(
            @NotNull Boolean acceptAiClassification,
            IssueCategory category,
            @Size(max = 5000) String englishTranslation
    ) {}

    public record ReviewHistoryResponse(Long id, String decision, IssueCategory category,
            String englishTranslation, Long reviewerId, String reviewerEmail, Instant reviewedAt) {}

    public record ClassificationResponse(
            Long issueId,
            Long productionUpdateId,
            Long orderId,
            String poNumber,
            String styleName,
            Long supervisorId,
            String originalNote,
            String detectedLanguage,
            String englishTranslation,
            IssueCategory predictedCategory,
            BigDecimal confidence,
            String status,
            String providerId,
            String modelId,
            IssueCategory reviewedCategory,
            String reviewedTranslation,
            Long reviewerId,
            String reviewerEmail,
            Instant reviewedAt,
            List<ReviewHistoryResponse> reviewHistory
    ) {}
}