package com.threadflow.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void merchandiserWorkspaceRejectsSupervisor() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/merchandiser/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MERCHANDISER")
    void supervisorWorkspaceRejectsMerchandiser() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/supervisor/updates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotCreateOrder() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotUpdateOrder() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/orders/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MERCHANDISER")
    void merchandiserCannotSubmitProductionUpdate() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/production/updates")
                        .param("orderId", "1")
                        .param("milestoneId", "1")
                        .param("productionDate", "2026-11-05")
                        .param("completedQuantity", "100")
                        .param("idempotencyKey", "test-key")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotAccessMerchandiserProductionHistory() throws Exception {
        mockMvc.perform(get("/api/v1/production/updates"))
                .andExpect(status().isForbidden());
    }
}
