package com.threadflow.order.audit;

import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAuditService {
    private final OrderDateAuditHistoryRepository auditRepository;
    private final OrderRepository orderRepository;

    public OrderAuditService(OrderDateAuditHistoryRepository auditRepository, OrderRepository orderRepository) {
        this.auditRepository = auditRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderDateAuditHistory recordDateChange(ProductionOrder order, LocalDate previousDate,
                                                  LocalDate newDate, String reason, User changedBy) {
        if (previousDate.equals(newDate)) return null;
        OrderDateAuditHistory history = new OrderDateAuditHistory(order, previousDate, newDate, reason, changedBy);
        return auditRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<OrderAuditDtos.DateAuditResponse> getAuditHistory(Long orderId, Authentication authentication) {
        requireMerchandiser(authentication);
        if (!orderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return auditRepository.findByOrder_IdOrderByChangedAtDesc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireMerchandiser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_MERCHANDISER"))) {
            throw new AccessDeniedException("Order audit history is restricted to merchandisers");
        }
    }

    private OrderAuditDtos.DateAuditResponse toResponse(OrderDateAuditHistory history) {
        return new OrderAuditDtos.DateAuditResponse(
                history.getId(),
                history.getOrder().getId(),
                history.getPreviousExFactoryDate(),
                history.getNewExFactoryDate(),
                history.getReason(),
                history.getChangedBy().getId(),
                history.getChangedBy().getEmail(),
                history.getChangedAt()
        );
    }
}
