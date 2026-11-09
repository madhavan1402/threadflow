package com.threadflow.order.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public final class OrderAuditDtos {
    private OrderAuditDtos() {}

    public record DateChangeRequest(
            @NotNull LocalDate newExFactoryDate,
            @NotBlank String reason
    ) {}

    public record DateAuditResponse(
            Long id,
            Long orderId,
            LocalDate previousExFactoryDate,
            LocalDate newExFactoryDate,
            String reason,
            Long changedById,
            String changedByEmail,
            Instant changedAt
    ) {}
}
