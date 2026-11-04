package com.threadflow.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    @Test
    void returnsConsistentValidationErrorResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidationTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/validation-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/validation-test"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void returnsConsistentMalformedJsonResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidationTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/validation-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @RestController
    static class ValidationTestController {
        @PostMapping("/validation-test")
        void validate(@Valid @RequestBody ValidationRequest request) {}
    }

    record ValidationRequest(@NotBlank(message = "Name is required") String name) {}
}
