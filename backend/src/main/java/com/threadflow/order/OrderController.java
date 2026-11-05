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
    public OrderController(OrderService service) { this.service = service; }

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
    public OrderDtos.OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderDtos.UpdateOrderRequest request) {
        return service.update(id, request);
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
