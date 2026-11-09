package com.threadflow.issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IssueControllerTest {
    private IssueClassificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(IssueClassificationService.class);
        IssueController controller = new IssueController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(roles = "MERCHANDISER")
    void pendingReviewReturnsList() throws Exception {
        IssueDtos.ClassificationResponse item = new IssueDtos.ClassificationResponse(
                1L, 10L, 20L, "PO-100", "Polo Shirt", 30L, "ஊசி உடைந்துவிட்டது",
                "ta", "The needle broke.", IssueCategory.MACHINE_BREAKDOWN,
                new BigDecimal("0.92"), "PENDING_REVIEW", "test-provider", "test-model",
                null, null, null, null, null, List.of()
        );
        when(service.pendingReview(any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/issues/pending-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].issueId").value(1))
                .andExpect(jsonPath("$[0].poNumber").value("PO-100"))
                .andExpect(jsonPath("$[0].originalNote").value("ஊசி உடைந்துவிட்டது"))
                .andExpect(jsonPath("$[0].englishTranslation").value("The needle broke."))
                .andExpect(jsonPath("$[0].predictedCategory").value("MACHINE_BREAKDOWN"))
                .andExpect(jsonPath("$[0].confidence").value(0.92));
    }

    @Test
    void getClassificationReturnsDetail() throws Exception {
        IssueDtos.ClassificationResponse item = new IssueDtos.ClassificationResponse(
                5L, 15L, 25L, "PO-200", "Hoodie", 35L, "நூல் தட்டுப்பாடு",
                "ta", "Thread shortage.", IssueCategory.MATERIAL_SHORTAGE,
                new BigDecimal("0.88"), "PENDING_REVIEW", "test-provider", "test-model",
                null, null, null, null, null, List.of()
        );
        when(service.classification(eq(5L), any())).thenReturn(item);

        mockMvc.perform(get("/api/v1/issues/5/classification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(5))
                .andExpect(jsonPath("$.predictedCategory").value("MATERIAL_SHORTAGE"));
    }

    @Test
    void reviewAcceptsAi() throws Exception {
        IssueDtos.ClassificationResponse updated = new IssueDtos.ClassificationResponse(
                5L, 15L, 25L, "PO-200", "Hoodie", 35L, "நூல் தட்டுப்பாடு",
                "ta", "Thread shortage.", IssueCategory.MATERIAL_SHORTAGE,
                new BigDecimal("0.88"), "REVIEWED", "test-provider", "test-model",
                IssueCategory.MATERIAL_SHORTAGE, "Thread shortage.", 7L, "merch@threadflow.test",
                Instant.now(), List.of(new IssueDtos.ReviewHistoryResponse(1L, "ACCEPTED_AI",
                IssueCategory.MATERIAL_SHORTAGE, "Thread shortage.", 7L, "merch@threadflow.test", Instant.now()))
        );
        when(service.review(eq(5L), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/issues/5/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptAiClassification\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"))
                .andExpect(jsonPath("$.reviewedCategory").value("MATERIAL_SHORTAGE"))
                .andExpect(jsonPath("$.reviewHistory[0].decision").value("ACCEPTED_AI"));
    }

    @Test
    void reviewRejectsNullAcceptFlag() throws Exception {
        mockMvc.perform(put("/api/v1/issues/5/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reclassifyCallsService() throws Exception {
        IssueDtos.ClassificationResponse response = new IssueDtos.ClassificationResponse(
                5L, 15L, 25L, "PO-200", "Hoodie", 35L, "நூல் தட்டுப்பாடு",
                null, null, null, null, "PENDING", null, null,
                null, null, null, null, null, List.of()
        );
        when(service.reclassify(eq(5L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/issues/5/reclassify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
