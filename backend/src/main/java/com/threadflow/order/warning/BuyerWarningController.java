package com.threadflow.order.warning;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/buyer-warnings")
@PreAuthorize("hasRole('MERCHANDISER')")
public class BuyerWarningController {
    private final BuyerWarningService warningService;

    public BuyerWarningController(BuyerWarningService warningService) {
        this.warningService = warningService;
    }

    @GetMapping
    public List<BuyerWarningDtos.BuyerWarningResponse> list(
            @PathVariable Long orderId, Authentication authentication) {
        return warningService.getWarningsForOrder(orderId, authentication);
    }

    @PostMapping
    public BuyerWarningDtos.BuyerWarningResponse createDraft(
            @PathVariable Long orderId,
            @RequestBody(required = false) BuyerWarningDtos.CreateWarningDraftRequest request,
            Authentication authentication) {
        String customMessage = request != null ? request.customMessage() : null;
        return warningService.createWarningDraft(orderId, customMessage, authentication);
    }

    @RequestMapping(value = "/{warningId}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    public BuyerWarningDtos.BuyerWarningResponse confirm(
            @PathVariable Long orderId,
            @PathVariable Long warningId,
            @Valid @RequestBody BuyerWarningDtos.ConfirmWarningRequest request,
            Authentication authentication) {
        return warningService.confirmWarning(orderId, warningId, request.approvedMessage(), authentication);
    }

    @RequestMapping(value = "/{warningId}/dismiss", method = {RequestMethod.POST, RequestMethod.PUT})
    public BuyerWarningDtos.BuyerWarningResponse dismiss(
            @PathVariable Long orderId,
            @PathVariable Long warningId,
            Authentication authentication) {
        return warningService.dismissWarning(orderId, warningId, authentication);
    }
}
