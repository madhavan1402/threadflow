package com.threadflow.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AssignmentDtos {
    private AssignmentDtos() {}

    public record ProductionUnitRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 50) String code,
            @Size(max = 500) String description,
            @NotNull Boolean active
    ) {}

    public record ProductionUnitIdRequest(Long productionUnitId) {}
    public record SupervisorIdsRequest(@NotNull List<@NotNull @Positive Long> supervisorIds) {}
    public record ProductionUnitResponse(Long id, String name, String code, String description, boolean active) {}
    public record SupervisorResponse(Long id, String email, boolean enabled) {}
    public record AssignmentHistoryResponse(Long id, Long supervisorId, String supervisorEmail,
            Long assignedById, String assignedByEmail, Instant assignedAt, Instant revokedAt, boolean active) {}
    public record UnitSupervisorsResponse(Long productionUnitId, List<AssignmentHistoryResponse> assignments) {}
    public record OrderAssignmentsResponse(Long orderId, ProductionUnitResponse productionUnit,
            List<AssignmentHistoryResponse> directAssignments) {}
}