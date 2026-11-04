package com.threadflow.common.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        Instant timestamp,
        DatabaseHealth database
) {
    public record DatabaseHealth(String status, String vendor) {}
}
