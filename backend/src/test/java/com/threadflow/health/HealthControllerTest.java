package com.threadflow.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadflow.common.dto.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DatabaseHealthService databaseHealthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(databaseHealthService)).build();
    }

    @Test
    void returnsStructuredUpResponseWhenDatabaseIsAvailable() throws Exception {
        when(databaseHealthService.isUp()).thenReturn(true);
        when(databaseHealthService.vendor()).thenReturn("MySQL");

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("threadflow-api"))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.database.vendor").value("MySQL"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseIsDown() throws Exception {
        when(databaseHealthService.isUp()).thenReturn(false);
        when(databaseHealthService.vendor()).thenReturn("Unknown");

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database.status").value("DOWN"));
    }
}
