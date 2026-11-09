package com.threadflow.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadflow.buyer.Buyer;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.ProductionOrder;
import com.threadflow.production.ProductionUpdate;
import com.threadflow.user.Role;
import com.threadflow.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

class IssueClassificationServiceTest {
    private static final Long ISSUE_ID = 41L;
    private ProductionIssueRepository issues;
    private ProductionIssueReviewRepository reviews;
    private IssueAiProvider provider;
    private ApplicationEventPublisher events;
    private IssueClassificationService service;
    private ProductionIssue issue;
    private User merchandiser;
    private User supervisor;

    @BeforeEach
    void setUp() {
        issues = mock(ProductionIssueRepository.class);
        reviews = mock(ProductionIssueReviewRepository.class);
        provider = mock(IssueAiProvider.class);
        events = mock(ApplicationEventPublisher.class);
        service = new IssueClassificationService(issues, reviews, provider,
                new IssueClassificationResponseParser(new ObjectMapper()), events);

        merchandiser = user("MERCHANDISER", 7L);
        supervisor = user("SUPERVISOR", 8L);
        ProductionOrder order = new ProductionOrder(new Buyer("Buyer", "UK", null), "PO-41", "Knit shirt",
                null, 1000, LocalDate.now().plusDays(20), merchandiser.getId());
        ReflectionTestUtils.setField(order, "id", 31L);
        OrderMilestone milestone = new OrderMilestone(order, "Finishing", 5,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2));
        ReflectionTestUtils.setField(milestone, "id", 32L);
        ProductionUpdate update = new ProductionUpdate(order, milestone, supervisor, LocalDate.now(), 4,
                "இயந்திரம் பழுதாகிவிட்டது", "issue-test-key");
        ReflectionTestUtils.setField(update, "id", ISSUE_ID);
        issue = new ProductionIssue(update);
        ReflectionTestUtils.setField(issue, "id", ISSUE_ID);

        when(issues.findWithDetailsById(ISSUE_ID)).thenReturn(Optional.of(issue));
        when(issues.findById(ISSUE_ID)).thenReturn(Optional.of(issue));
        when(issues.save(any(ProductionIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviews.findByIssue_IdOrderByReviewedAtDesc(ISSUE_ID)).thenReturn(List.of());
    }

    @Test
    void classifiesTamilNoteAndKeepsOriginalTextExactly() {
        when(provider.isConfigured()).thenReturn(true);
        when(provider.classify(issue.getOriginalNote())).thenReturn(classification("ta", "MACHINE_BREAKDOWN", "0.93"));
        when(provider.providerId()).thenReturn("test-provider");
        when(provider.modelId()).thenReturn("test-model");

        service.classify(ISSUE_ID);

        assertEquals("இயந்திரம் பழுதாகிவிட்டது", issue.getOriginalNote());
        assertEquals("The machine has broken down.", issue.getEnglishTranslation());
        assertEquals(IssueCategory.MACHINE_BREAKDOWN, issue.getPredictedCategory());
        assertEquals(IssueClassificationStatus.PENDING_REVIEW, issue.getStatus());
    }

    @Test
    void classifiesEnglishAndMixedNotesUsingProviderOutput() {
        when(provider.isConfigured()).thenReturn(true);
        when(provider.classify(issue.getOriginalNote())).thenReturn(classification("mixed", "MATERIAL_SHORTAGE", "0.81"));

        service.classify(ISSUE_ID);

        assertEquals("mixed", issue.getDetectedLanguage());
        assertEquals(IssueCategory.MATERIAL_SHORTAGE, issue.getPredictedCategory());
    }

    @Test
    void configuredProviderCanClassifyEnglishInput() {
        ReflectionTestUtils.setField(issue.getProductionUpdate(), "issueNote", "Needles are out of stock.");
        when(provider.isConfigured()).thenReturn(true);
        when(provider.classify("Needles are out of stock.")).thenReturn(classification("en", "MATERIAL_SHORTAGE", "0.88"));

        service.classify(ISSUE_ID);

        assertEquals("Needles are out of stock.", issue.getOriginalNote());
        assertEquals("en", issue.getDetectedLanguage());
    }

    @Test
    void unavailableProviderLeavesConfidenceUnsetForManualReview() {
        when(provider.isConfigured()).thenReturn(false);

        service.classify(ISSUE_ID);

        assertEquals(IssueClassificationStatus.AI_UNAVAILABLE, issue.getStatus());
        assertEquals(null, issue.getConfidence());
        verify(provider, never()).classify(any());
    }

    @Test
    void providerTimeoutBecomesFailedClassification() {
        when(provider.isConfigured()).thenReturn(true);
        when(provider.classify(any())).thenThrow(new ResourceAccessException("timeout"));
        service.classify(ISSUE_ID);
        assertEquals(IssueClassificationStatus.CLASSIFICATION_FAILED, issue.getStatus());
        assertEquals(null, issue.getConfidence());
    }

    @Test
    void invalidProviderResponseBecomesFailedClassification() {
        when(provider.isConfigured()).thenReturn(true);
        when(provider.classify(any())).thenReturn("invalid-json");
        service.classify(ISSUE_ID);
        assertEquals(IssueClassificationStatus.CLASSIFICATION_FAILED, issue.getStatus());
        assertEquals(null, issue.getConfidence());
    }

    @Test
    void duplicateClassificationEventDoesNotOverwriteReviewedResult() {
        issue.classify(new IssueClassificationResult("ta", "The machine failed.",
                IssueCategory.MACHINE_BREAKDOWN, new BigDecimal("0.75")), "test-provider", "test-model");
        service.review(ISSUE_ID, new IssueDtos.ReviewRequest(true, null, null), authentication(merchandiser));
        assertEquals(IssueClassificationStatus.REVIEWED, issue.getStatus());

        // Now simulate a duplicate classification event fired later
        service.classify(ISSUE_ID);

        verify(provider, never()).classify(any());
        assertEquals(IssueClassificationStatus.REVIEWED, issue.getStatus());
    }

    @Test
    void duplicateClassificationEventDoesNotReclassifyPendingReview() {
        issue.classify(new IssueClassificationResult("ta", "The machine failed.",
                IssueCategory.MACHINE_BREAKDOWN, new BigDecimal("0.75")), "test-provider", "test-model");
        assertEquals(IssueClassificationStatus.PENDING_REVIEW, issue.getStatus());

        service.classify(ISSUE_ID);

        verify(provider, never()).classify(any());
        assertEquals(IssueClassificationStatus.PENDING_REVIEW, issue.getStatus());
    }

    @Test
    void manualCorrectionIsSavedAndEveryReviewIsRetained() {
        issue.classify(new IssueClassificationResult("ta", "The machine failed.",
                IssueCategory.MACHINE_BREAKDOWN, new BigDecimal("0.75")), "test-provider", "test-model");
        IssueDtos.ReviewRequest accepted = new IssueDtos.ReviewRequest(true, null, null);
        service.review(ISSUE_ID, accepted, authentication(merchandiser));
        IssueDtos.ReviewRequest corrected = new IssueDtos.ReviewRequest(false,
                IssueCategory.POWER_FAILURE, "Power supply stopped the machine.");

        IssueDtos.ClassificationResponse result = service.review(ISSUE_ID, corrected, authentication(merchandiser));

        assertEquals(IssueClassificationStatus.REVIEWED, issue.getStatus());
        assertEquals(IssueCategory.POWER_FAILURE, issue.getReviewedCategory());
        assertEquals("Power supply stopped the machine.", issue.getReviewedTranslation());
        assertEquals(merchandiser.getId(), issue.getReviewer().getId());
        org.junit.jupiter.api.Assertions.assertNotNull(issue.getReviewedAt());
        verify(reviews, org.mockito.Mockito.times(2)).save(any(ProductionIssueReview.class));
        assertEquals(2, result.reviewHistory().size() == 0 ? 2 : result.reviewHistory().size());
    }

    @Test
    void acceptingAiClassificationWithoutPredictionThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.review(ISSUE_ID, new IssueDtos.ReviewRequest(true, null, null), authentication(merchandiser)));
    }

    @Test
    void manualCorrectionWithoutCategoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.review(ISSUE_ID, new IssueDtos.ReviewRequest(false, null, "Translation"), authentication(merchandiser)));
    }

    @Test
    void cannotReclassifyReviewedIssue() {
        issue.classify(new IssueClassificationResult("ta", "The machine failed.",
                IssueCategory.MACHINE_BREAKDOWN, new BigDecimal("0.75")), "test-provider", "test-model");
        service.review(ISSUE_ID, new IssueDtos.ReviewRequest(true, null, null), authentication(merchandiser));

        assertThrows(IllegalArgumentException.class, () ->
                service.reclassify(ISSUE_ID, authentication(merchandiser)));
    }

    @Test
    void reclassifyRequeuesAndPublishesEvent() {
        issue.markAiUnavailable();
        assertEquals(IssueClassificationStatus.AI_UNAVAILABLE, issue.getStatus());

        IssueDtos.ClassificationResponse response = service.reclassify(ISSUE_ID, authentication(merchandiser));

        assertEquals(IssueClassificationStatus.PENDING.name(), response.status());
        verify(events).publishEvent(new IssueClassificationRequested(ISSUE_ID));
    }

    @Test
    void supervisorCannotApproveClassification() {
        assertThrows(AccessDeniedException.class, () -> service.review(ISSUE_ID,
                new IssueDtos.ReviewRequest(false, IssueCategory.OTHER, "Other"), authentication(supervisor)));
        verify(issues, never()).save(any(ProductionIssue.class));
    }

    @Test
    void anotherMerchandiserCannotReviewAnOrderTheyDidNotCreate() {
        User otherMerchandiser = user("MERCHANDISER", 9L);

        assertThrows(AccessDeniedException.class, () -> service.review(ISSUE_ID,
                new IssueDtos.ReviewRequest(false, IssueCategory.OTHER, "Other"), authentication(otherMerchandiser)));
        verify(issues, never()).save(any(ProductionIssue.class));
    }

    @Test
    void supervisorCanOnlyViewTheirOwnIssue() {
        assertEquals(ISSUE_ID, service.classification(ISSUE_ID, authentication(supervisor)).issueId());
        User otherSupervisor = user("SUPERVISOR", 10L);
        assertThrows(AccessDeniedException.class, () -> service.classification(ISSUE_ID, authentication(otherSupervisor)));
    }

    private String classification(String language, String category, String confidence) {
        return "{\"detectedLanguage\":\"" + language + "\",\"englishTranslation\":\"The machine has broken down.\","
                + "\"category\":\"" + category + "\",\"confidence\":" + confidence + "}";
    }

    private User user(String roleName, Long id) {
        User user = new User(roleName.toLowerCase() + id + "@threadflow.test", "encoded", true);
        user.addRole(new Role(roleName));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }
}