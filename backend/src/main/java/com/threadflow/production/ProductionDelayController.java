package com.threadflow.production;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("isAuthenticated()")
public class ProductionDelayController {
    private final ProductionDelayService service;

    public ProductionDelayController(ProductionDelayService service) { this.service = service; }

    @GetMapping("/orders/{orderId}/delay-risk")
    public ProductionDelayDtos.OrderDelayRiskResponse orderRisk(@PathVariable Long orderId,
                                                                 Authentication authentication) {
        return service.orderRisk(orderId, authentication);
    }

    @GetMapping("/delay-risk")
    public List<ProductionDelayDtos.OrderDelayRiskResponse> allOrderRisks(Authentication authentication) {
        return service.allOrderRisks(authentication);
    }

    @GetMapping("/production-summary")
    public ProductionDelayDtos.ProductionSummaryResponse summary(Authentication authentication) {
        return service.summary(authentication);
    }
}