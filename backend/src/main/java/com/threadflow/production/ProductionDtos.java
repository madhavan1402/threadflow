package com.threadflow.production;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ProductionDtos {
    public record CreateUpdateRequest(
            @NotNull Long orderId,
            @NotNull Long milestoneId,
            @NotNull LocalDate productionDate,
            @NotNull @Min(1) Integer completedQuantity,
            @Size(max=5000) String issueNote,
            @NotBlank @Size(max=64) String idempotencyKey
    ) {}
    public record PhotoResponse(Long id, String originalFilename, String contentType, Long fileSize) {}
    public record UpdateResponse(
            Long id, Long orderId, String poNumber, Long milestoneId, String milestoneName,
            Long supervisorId, String supervisorEmail, LocalDate productionDate,
            Integer completedQuantity, String issueNote, Instant createdAt, Instant updatedAt,
            List<PhotoResponse> photos
    ) {}
    public record AvailableOrderResponse(
            Long id, String poNumber, String styleName, String buyerName,
            Integer totalQuantity, LocalDate exFactoryDate, AssignmentDtos.ProductionUnitResponse productionUnit,
            String assignmentSource, List<MilestoneResponse> milestones
    ) {}
    public record MilestoneResponse(Long id, String milestoneName, Integer sequenceNumber, String status) {}
}
