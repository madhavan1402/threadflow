package com.threadflow.order;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;
    private final com.threadflow.order.audit.OrderAuditService auditService;

    public OrderController(OrderService service, com.threadflow.order.audit.OrderAuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping
    public Page<OrderDtos.OrderResponse> list(@RequestParam(required=false) String search,
            @RequestParam(required=false) Long buyer, @RequestParam(required=false) String status,
            @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
            @PageableDefault(size=20, sort="id") Pageable pageable, Authentication authentication) {
        return service.list(search, buyer, status, from, to, pageable, authentication);
    }
    @GetMapping("/{id}")
    public OrderDtos.OrderResponse get(@PathVariable Long id, Authentication authentication) { return service.get(id, authentication); }
    @PostMapping
    @PreAuthorize("hasRole('MERCHANDISER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.OrderResponse create(@Valid @RequestBody OrderDtos.CreateOrderRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public OrderDtos.OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderDtos.UpdateOrderRequest request,
                                          Authentication authentication) {
        return service.update(id, request, authentication);
    }
    @PutMapping("/{id}/ex-factory-date")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public OrderDtos.OrderResponse changeExFactoryDate(@PathVariable Long id,
            @Valid @RequestBody com.threadflow.order.audit.OrderAuditDtos.DateChangeRequest request,
            Authentication authentication) {
        return service.changeExFactoryDate(id, request, authentication);
    }
    @GetMapping("/{id}/audit-history")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public List<com.threadflow.order.audit.OrderAuditDtos.DateAuditResponse> auditHistory(
            @PathVariable Long id, Authentication authentication) {
        return auditService.getAuditHistory(id, authentication);
    }
    @GetMapping("/{id}/milestones")
    public List<OrderDtos.MilestoneResponse> milestones(@PathVariable Long id, Authentication authentication) {
        return service.milestones(id, authentication);
    }
    @PutMapping("/{id}/milestones/{milestoneId}")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public OrderDtos.MilestoneResponse updateMilestone(@PathVariable Long id, @PathVariable Long milestoneId,
                                                       @Valid @RequestBody OrderDtos.MilestoneUpdateRequest request) {
        return service.updateMilestone(id, milestoneId, request);
    }
}
