package com.threadflow.order.warning;

import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.production.ProductionDelayDtos;
import com.threadflow.production.ProductionDelayService;
import com.threadflow.user.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadflow.user.UserRepository;

@Service
public class BuyerWarningService {
    private final BuyerDelayWarningRepository warningRepository;
    private final OrderRepository orderRepository;
    private final ProductionDelayService delayService;
    private final UserRepository userRepository;

    public BuyerWarningService(BuyerDelayWarningRepository warningRepository,
                               OrderRepository orderRepository,
                               ProductionDelayService delayService,
                               UserRepository userRepository) {
        this.warningRepository = warningRepository;
        this.orderRepository = orderRepository;
        this.delayService = delayService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BuyerWarningDtos.BuyerWarningResponse> getWarningsForOrder(Long orderId, Authentication authentication) {
        requireMerchandiser(authentication);
        requireOrder(orderId);
        return warningRepository.findByOrder_IdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BuyerWarningDtos.BuyerWarningResponse createWarningDraft(Long orderId, String customMessage, Authentication authentication) {
        requireMerchandiser(authentication);
        ProductionOrder order = requireOrder(orderId);

        String riskStatus = "AT_RISK";
        String internalReasons = "Production progress behind planned schedule.";
        String suggested = customMessage != null && !customMessage.isBlank()
                ? customMessage.trim()
                : "Production schedule delay anticipated due to processing pace. Order completion date under review.";

        try {
            ProductionDelayDtos.OrderDelayRiskResponse risk = delayService.orderRisk(orderId, authentication);
            riskStatus = risk.riskStatus().name();
            internalReasons = String.join("; ", risk.reasons());
            if (customMessage == null || customMessage.isBlank()) {
                suggested = "Order " + order.getPoNumber() + " (" + order.getStyleName() + ") is currently "
                        + riskStatus.toLowerCase().replace('_', ' ') + ". "
                        + (risk.estimatedCompletionDate() != null
                        ? "Estimated factory completion is updated to " + risk.estimatedCompletionDate() + "."
                        : "Our merchandising team is monitoring progress closely.");
            }
        } catch (RuntimeException ignored) {
            // fallback if delay service is unavailable or order unanalysable
        }

        BuyerDelayWarning warning = new BuyerDelayWarning(order, riskStatus, internalReasons, suggested);
        return toResponse(warningRepository.save(warning));
    }

    @Transactional
    public BuyerWarningDtos.BuyerWarningResponse confirmWarning(Long orderId, Long warningId, String approvedMessage, Authentication authentication) {
        User merchandiser = requireMerchandiser(authentication);
        requireOrder(orderId);
        BuyerDelayWarning warning = warningRepository.findByIdAndOrder_Id(warningId, orderId)
                .orElseThrow(() -> new IllegalArgumentException("Warning not found: " + warningId));

        warning.confirm(approvedMessage, merchandiser);
        return toResponse(warningRepository.save(warning));
    }

    @Transactional
    public BuyerWarningDtos.BuyerWarningResponse dismissWarning(Long orderId, Long warningId, Authentication authentication) {
        requireMerchandiser(authentication);
        requireOrder(orderId);
        BuyerDelayWarning warning = warningRepository.findByIdAndOrder_Id(warningId, orderId)
                .orElseThrow(() -> new IllegalArgumentException("Warning not found: " + warningId));

        warning.dismiss();
        return toResponse(warningRepository.save(warning));
    }

    private ProductionOrder requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private User requireMerchandiser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_MERCHANDISER"))) {
            throw new AccessDeniedException("Buyer warning management is restricted to merchandisers");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        }
        return null;
    }

    private BuyerWarningDtos.BuyerWarningResponse toResponse(BuyerDelayWarning warning) {
        return new BuyerWarningDtos.BuyerWarningResponse(
                warning.getId(),
                warning.getOrder().getId(),
                warning.getOrder().getPoNumber(),
                warning.getStatus().name(),
                warning.getRiskStatus(),
                warning.getInternalReasons(),
                warning.getSuggestedMessage(),
                warning.getConfirmedMessage(),
                warning.getConfirmedBy() != null ? warning.getConfirmedBy().getId() : null,
                warning.getConfirmedBy() != null ? warning.getConfirmedBy().getEmail() : null,
                warning.getConfirmedAt(),
                warning.getCreatedAt()
        );
    }
}
