package com.threadflow.order.warning;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class BuyerWarningDtos {
    private BuyerWarningDtos() {}

    public record CreateWarningDraftRequest(
            String customMessage
    ) {}

    public record ConfirmWarningRequest(
            @NotBlank String approvedMessage
    ) {}

    public record BuyerWarningResponse(
            Long id,
            Long orderId,
            String poNumber,
            String status,
            String riskStatus,
            String internalReasons,
            String suggestedMessage,
            String confirmedMessage,
            Long confirmedById,
            String confirmedByEmail,
            Instant confirmedAt,
            Instant createdAt
    ) {}
}
