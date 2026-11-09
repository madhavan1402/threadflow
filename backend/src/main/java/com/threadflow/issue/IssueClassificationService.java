package com.threadflow.issue;

import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueClassificationService {
    private static final List<IssueClassificationStatus> REVIEWABLE_STATUSES = List.of(
            IssueClassificationStatus.PENDING,
            IssueClassificationStatus.PENDING_REVIEW,
            IssueClassificationStatus.AI_UNAVAILABLE,
            IssueClassificationStatus.CLASSIFICATION_FAILED);

    private final ProductionIssueRepository issues;
    private final ProductionIssueReviewRepository reviews;
    private final IssueAiProvider provider;
    private final IssueClassificationResponseParser parser;
    private final ApplicationEventPublisher events;

    public IssueClassificationService(ProductionIssueRepository issues,
                                      ProductionIssueReviewRepository reviews,
                                      IssueAiProvider provider,
                                      IssueClassificationResponseParser parser,
                                      ApplicationEventPublisher events) {
        this.issues = issues;
        this.reviews = reviews;
        this.provider = provider;
        this.parser = parser;
        this.events = events;
    }

    public void classify(Long issueId) {
        ProductionIssue issue = issues.findWithDetailsById(issueId).orElse(null);
        if (issue == null || issue.getStatus() != IssueClassificationStatus.PENDING) return;
        if (!provider.isConfigured()) {
            issue.markAiUnavailable();
            issues.save(issue);
            return;
        }

        IssueClassificationResult result;
        try {
            result = parser.parse(provider.classify(issue.getOriginalNote()));
        } catch (RuntimeException exception) {
            issue.markClassificationFailed();
            issues.save(issue);
            return;
        }

        ProductionIssue current = issues.findById(issueId).orElse(null);
        if (current == null || current.getStatus() != IssueClassificationStatus.PENDING) return;
        current.classify(result, provider.providerId(), provider.modelId());
        issues.save(current);
    }

    @Transactional(readOnly = true)
    public List<IssueDtos.ClassificationResponse> pendingReview(Authentication authentication) {
        User merchandiser = requireMerchandiser(authentication);
        return issues.findByStatusInOrderByCreatedAtAsc(REVIEWABLE_STATUSES).stream()
                .filter(issue -> canManage(merchandiser, issue))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IssueDtos.ClassificationResponse classification(Long issueId, Authentication authentication) {
        ProductionIssue issue = requireIssue(issueId);
        if (hasRole(authentication, "MERCHANDISER")) {
            User merchandiser = requireMerchandiser(authentication);
            requireManageAccess(merchandiser, issue);
        } else if (hasRole(authentication, "SUPERVISOR")
                && authentication.getPrincipal() instanceof User supervisor) {
            if (!issue.getProductionUpdate().getSupervisor().getId().equals(supervisor.getId())) {
                throw new AccessDeniedException("Supervisors may only view their own submitted issues");
            }
        } else {
            throw new AccessDeniedException("Issue access is restricted to merchandisers and submitting supervisors");
        }
        return toResponse(issue);
    }

    @Transactional
    public IssueDtos.ClassificationResponse review(Long issueId, IssueDtos.ReviewRequest request,
                                                    Authentication authentication) {
        User merchandiser = requireMerchandiser(authentication);
        ProductionIssue issue = requireIssue(issueId);
        requireManageAccess(merchandiser, issue);

        IssueCategory category;
        String translation;
        IssueReviewDecision decision;
        if (request.acceptAiClassification()) {
            if (issue.getPredictedCategory() == null) {
                throw new IllegalArgumentException("There is no AI classification to accept");
            }
            category = issue.getPredictedCategory();
            translation = issue.getEnglishTranslation();
            decision = IssueReviewDecision.ACCEPTED_AI;
        } else {
            if (request.category() == null) throw new IllegalArgumentException("A corrected issue category is required");
            category = request.category();
            translation = request.englishTranslation() == null
                    ? issue.getEnglishTranslation() : request.englishTranslation();
            decision = IssueReviewDecision.CORRECTED;
        }

        Instant reviewedAt = Instant.now();
        issue.review(category, translation, merchandiser, reviewedAt);
        issues.save(issue);
        reviews.save(new ProductionIssueReview(issue, merchandiser, decision, category, translation, reviewedAt));
        return toResponse(issue);
    }

    @Transactional
    public IssueDtos.ClassificationResponse reclassify(Long issueId, Authentication authentication) {
        User merchandiser = requireMerchandiser(authentication);
        ProductionIssue issue = requireIssue(issueId);
        requireManageAccess(merchandiser, issue);
        if (issue.getStatus() == IssueClassificationStatus.REVIEWED) {
            throw new IllegalArgumentException("Reviewed classifications cannot be reclassified");
        }
        issue.requeue();
        issues.save(issue);
        events.publishEvent(new IssueClassificationRequested(issueId));
        return toResponse(issue);
    }

    private ProductionIssue requireIssue(Long issueId) {
        return issues.findWithDetailsById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User requireMerchandiser(Authentication authentication) {
        if (!hasRole(authentication, "MERCHANDISER")
                || !(authentication.getPrincipal() instanceof User merchandiser)) {
            throw new AccessDeniedException("Authenticated merchandiser account required");
        }
        return merchandiser;
    }

    private void requireManageAccess(User merchandiser, ProductionIssue issue) {
        if (!canManage(merchandiser, issue)) {
            throw new AccessDeniedException("This issue belongs to another merchandiser's order");
        }
    }

    private boolean canManage(User merchandiser, ProductionIssue issue) {
        Long createdBy = issue.getProductionUpdate().getOrder().getCreatedBy();
        return createdBy == null || createdBy.equals(merchandiser.getId());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private IssueDtos.ClassificationResponse toResponse(ProductionIssue issue) {
        var update = issue.getProductionUpdate();
        ProductionOrder order = update.getOrder();
        List<IssueDtos.ReviewHistoryResponse> history = reviews
                .findByIssue_IdOrderByReviewedAtDesc(issue.getId()).stream()
                .map(review -> new IssueDtos.ReviewHistoryResponse(review.getId(), review.getDecision().name(),
                        review.getCategory(), review.getEnglishTranslation(), review.getReviewer().getId(),
                        review.getReviewer().getEmail(), review.getReviewedAt())).toList();
        return new IssueDtos.ClassificationResponse(issue.getId(), update.getId(), order.getId(),
                order.getPoNumber(), order.getStyleName(), update.getSupervisor().getId(), issue.getOriginalNote(),
                issue.getDetectedLanguage(), issue.getEnglishTranslation(), issue.getPredictedCategory(),
                issue.getConfidence(), issue.getStatus().name(), issue.getProviderId(), issue.getModelId(),
                issue.getReviewedCategory(), issue.getReviewedTranslation(),
                issue.getReviewer() == null ? null : issue.getReviewer().getId(),
                issue.getReviewer() == null ? null : issue.getReviewer().getEmail(), issue.getReviewedAt(), history);
    }
}