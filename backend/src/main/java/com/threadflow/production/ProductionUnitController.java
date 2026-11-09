package com.threadflow.production;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('MERCHANDISER')")
public class ProductionUnitController {
    private final ProductionUnitService service;

    public ProductionUnitController(ProductionUnitService service) { this.service = service; }

    @GetMapping("/production-units")
    public List<AssignmentDtos.ProductionUnitResponse> units(Authentication authentication) {
        return service.listUnits(authentication);
    }

    @PostMapping("/production-units")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDtos.ProductionUnitResponse createUnit(
            @Valid @RequestBody AssignmentDtos.ProductionUnitRequest request, Authentication authentication) {
        return service.createUnit(request, authentication);
    }

    @PutMapping("/production-units/{unitId}")
    public AssignmentDtos.ProductionUnitResponse updateUnit(@PathVariable Long unitId,
            @Valid @RequestBody AssignmentDtos.ProductionUnitRequest request, Authentication authentication) {
        return service.updateUnit(unitId, request, authentication);
    }

    @GetMapping("/production-units/{unitId}/supervisors")
    public AssignmentDtos.UnitSupervisorsResponse unitSupervisors(@PathVariable Long unitId, Authentication authentication) {
        return service.unitSupervisors(unitId, authentication);
    }

    @PutMapping("/production-units/{unitId}/supervisors")
    public AssignmentDtos.UnitSupervisorsResponse setUnitSupervisors(@PathVariable Long unitId,
            @Valid @RequestBody AssignmentDtos.SupervisorIdsRequest request, Authentication authentication) {
        return service.setUnitSupervisors(unitId, request, authentication);
    }

    @GetMapping("/supervisors")
    public List<AssignmentDtos.SupervisorResponse> supervisors(Authentication authentication) {
        return service.availableSupervisors(authentication);
    }

    @GetMapping("/orders/{orderId}/assignments")
    public AssignmentDtos.OrderAssignmentsResponse orderAssignments(@PathVariable Long orderId, Authentication authentication) {
        return service.orderAssignments(orderId, authentication);
    }

    @PutMapping("/orders/{orderId}/production-unit")
    public AssignmentDtos.OrderAssignmentsResponse setProductionUnit(@PathVariable Long orderId,
            @RequestBody AssignmentDtos.ProductionUnitIdRequest request, Authentication authentication) {
        return service.setOrderProductionUnit(orderId, request.productionUnitId(), authentication);
    }

    @PutMapping("/orders/{orderId}/supervisors")
    public AssignmentDtos.OrderAssignmentsResponse setOrderSupervisors(@PathVariable Long orderId,
            @Valid @RequestBody AssignmentDtos.SupervisorIdsRequest request, Authentication authentication) {
        return service.setOrderSupervisors(orderId, request, authentication);
    }
}