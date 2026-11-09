package com.threadflow.production;

import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uses calendar days in Asia/Kolkata because no factory workday calendar exists yet.
 * Completed pieces are the positive, non-future quantity reports on the order's
 * highest-sequence milestone only. Reports at earlier milestones describe the
 * same garments in progress and are never added to finished output.
 */
@Service
public class ProductionDelayService {
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final OrderRepository orders;
    private final ProductionUpdateRepository updates;
    private final SupervisorOrderAccessService supervisorAccess;
    private final Clock clock;

    @Autowired
    public ProductionDelayService(OrderRepository orders, ProductionUpdateRepository updates,
                                  SupervisorOrderAccessService supervisorAccess) {
        this(orders, updates, supervisorAccess, Clock.system(FACTORY_ZONE));
    }

    ProductionDelayService(OrderRepository orders, ProductionUpdateRepository updates,
                           SupervisorOrderAccessService supervisorAccess, Clock clock) {
        this.orders = orders;
        this.updates = updates;
        this.supervisorAccess = supervisorAccess;
        this.clock = clock.withZone(FACTORY_ZONE);
    }

    @Transactional(readOnly = true)
    public ProductionDelayDtos.OrderDelayRiskResponse orderRisk(Long orderId, Authentication authentication) {
        User supervisor = requireAnalyticsAccess(authentication);
        if (supervisor != null) supervisorAccess.requireAccess(supervisor, orderId);
        ProductionOrder order = orders.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        ensureAnalysable(order);
        return calculate(order, updates.findByOrderIdOrderByProductionDateDescCreatedAtDesc(orderId), today());
    }

    @Transactional(readOnly = true)
    public List<ProductionDelayDtos.OrderDelayRiskResponse> allOrderRisks(Authentication authentication) {
        User supervisor = requireAnalyticsAccess(authentication);
        List<ProductionOrder> accessibleOrders;
        if (supervisor == null) {
            accessibleOrders = orders.findByStatusNot(ProductionOrder.Status.CANCELLED);
        } else {
            List<Long> accessibleIds = supervisorAccess.accessibleOrderIds(supervisor.getId());
            accessibleOrders = accessibleIds.isEmpty() ? List.of() : orders.findDistinctByIdIn(accessibleIds);
        }
        accessibleOrders = accessibleOrders.stream()
                .filter(order -> order.getStatus() != ProductionOrder.Status.CANCELLED)
                .sorted(Comparator.comparing(ProductionOrder::getPoNumber))
                .toList();
        if (accessibleOrders.isEmpty()) return List.of();

        List<Long> orderIds = accessibleOrders.stream().map(ProductionOrder::getId).toList();
        Map<Long, List<ProductionUpdate>> updatesByOrder = new HashMap<>();
        updates.findByOrderIdIn(orderIds).forEach(update ->
                updatesByOrder.computeIfAbsent(update.getOrder().getId(), ignored -> new ArrayList<>()).add(update));
        LocalDate today = today();
        return accessibleOrders.stream()
                .map(order -> calculate(order, updatesByOrder.getOrDefault(order.getId(), List.of()), today))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductionDelayDtos.ProductionSummaryResponse summary(Authentication authentication) {
        List<ProductionDelayDtos.OrderDelayRiskResponse> risks = allOrderRisks(authentication);
        int onTrack = count(risks, ProductionDelayDtos.RiskStatus.ON_TRACK);
        int atRisk = count(risks, ProductionDelayDtos.RiskStatus.AT_RISK);
        int delayed = count(risks, ProductionDelayDtos.RiskStatus.DELAYED);
        int completed = count(risks, ProductionDelayDtos.RiskStatus.COMPLETED);
        return new ProductionDelayDtos.ProductionSummaryResponse(today(), risks.size(), onTrack, atRisk, delayed, completed);
    }

    ProductionDelayDtos.OrderDelayRiskResponse calculate(ProductionOrder order,
            List<ProductionUpdate> orderUpdates, LocalDate today) {
        ensureAnalysable(order);
        OrderMilestone finalMilestone = order.getMilestones().stream()
                .max(Comparator.comparing(OrderMilestone::getSequenceNumber)).orElse(null);
        long reportedCompleted = finalMilestone == null ? 0 : orderUpdates.stream()
                .filter(update -> update.getMilestone().getId().equals(finalMilestone.getId()))
                .filter(update -> update.getCompletedQuantity() != null && update.getCompletedQuantity() > 0)
                .filter(update -> !update.getProductionDate().isAfter(today))
                .mapToLong(ProductionUpdate::getCompletedQuantity).sum();
        int total = order.getTotalQuantity();
        int completed = (int) Math.min(reportedCompleted, total);
        int remaining = Math.max(0, total - completed);

        LocalDate productionStart = order.getMilestones().stream()
                .map(OrderMilestone::getPlannedStartDate).min(LocalDate::compareTo)
                .orElseGet(() -> order.getCreatedAt() == null ? today
                        : order.getCreatedAt().atZone(FACTORY_ZONE).toLocalDate());
        long plannedDays = Math.max(1, ChronoUnit.DAYS.between(productionStart, order.getExFactoryDate()) + 1);
        long elapsedDays = today.isBefore(productionStart) ? 0
                : ChronoUnit.DAYS.between(productionStart, today) + 1;
        long remainingDays = today.isAfter(order.getExFactoryDate()) ? 0
                : ChronoUnit.DAYS.between(today, order.getExFactoryDate()) + 1;

        BigDecimal actualRate = elapsedDays == 0 ? ZERO
                : divide(BigDecimal.valueOf(completed), elapsedDays);
        BigDecimal requiredRate = remainingDays == 0 ? null
                : divide(BigDecimal.valueOf(remaining), remainingDays);
        BigDecimal estimatedDays = remaining == 0 ? ZERO
            : completed == 0 || elapsedDays == 0 ? null
            : BigDecimal.valueOf(remaining).multiply(BigDecimal.valueOf(elapsedDays))
                .divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
        LocalDate estimatedDate = estimatedDays == null ? null
                : today.plusDays((long) Math.ceil(estimatedDays.doubleValue()));

        long elapsedForPlan = Math.min(elapsedDays, plannedDays);
        BigDecimal expectedByToday = BigDecimal.valueOf(total)
                .multiply(BigDecimal.valueOf(elapsedForPlan))
                .divide(BigDecimal.valueOf(plannedDays), 2, RoundingMode.HALF_UP);
        BigDecimal shortfall = expectedByToday.subtract(BigDecimal.valueOf(completed)).max(ZERO);
        BigDecimal completionPercent = BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        boolean paceInsufficient = isPaceInsufficient(completed, remaining, elapsedDays, remainingDays);
        ProductionDelayDtos.RiskStatus status = classify(completed, total, today, order.getExFactoryDate(),
            paceInsufficient, estimatedDate);
        List<String> reasons = explain(status, completed, remaining, total, requiredRate,
            estimatedDate, order.getExFactoryDate(), reportedCompleted, paceInsufficient,
            elapsedDays, remainingDays);

        return new ProductionDelayDtos.OrderDelayRiskResponse(order.getId(), order.getPoNumber(),
                order.getBuyer().getBuyerName(), order.getStyleName(), order.getExFactoryDate(), today,
                finalMilestone == null ? null : finalMilestone.getMilestoneName(), total, reportedCompleted,
                completed, remaining, elapsedDays, remainingDays, requiredRate, actualRate, estimatedDays,
                estimatedDate, shortfall, completionPercent, status, reasons);
    }

    private ProductionDelayDtos.RiskStatus classify(int completed, int total, LocalDate today,
            LocalDate exFactoryDate, boolean paceInsufficient, LocalDate estimatedDate) {
        if (completed >= total) return ProductionDelayDtos.RiskStatus.COMPLETED;
        if (exFactoryDate.isBefore(today)) return ProductionDelayDtos.RiskStatus.DELAYED;
        if (paceInsufficient || estimatedDate == null || estimatedDate.isAfter(exFactoryDate)) {
            return ProductionDelayDtos.RiskStatus.AT_RISK;
        }
        return ProductionDelayDtos.RiskStatus.ON_TRACK;
    }

    private List<String> explain(ProductionDelayDtos.RiskStatus status, int completed, int remaining,
            int total, BigDecimal requiredRate, LocalDate estimatedDate,
            LocalDate exFactoryDate, long reportedCompleted, boolean paceInsufficient,
            long elapsedDays, long remainingDays) {
        List<String> reasons = new ArrayList<>();
        if (status == ProductionDelayDtos.RiskStatus.COMPLETED) {
            reasons.add("Final-stage production reports meet the order quantity of " + total + " pieces.");
        } else if (status == ProductionDelayDtos.RiskStatus.DELAYED) {
            reasons.add("The ex-factory date has passed with " + remaining + " pieces remaining.");
        } else if (status == ProductionDelayDtos.RiskStatus.AT_RISK) {
            if (reportedCompleted == 0) reasons.add("No valid quantity has been reported at the final production milestone.");
            if (requiredRate == null) reasons.add("No production days remain before the ex-factory date.");
            else if (paceInsufficient) {
                reasons.add("Final-stage output averages " + completed + " pieces over " + elapsedDays
                        + " calendar days; " + remaining + " pieces remain for " + remainingDays
                        + " calendar days, so the current pace is insufficient.");
            }
            if (estimatedDate != null && estimatedDate.isAfter(exFactoryDate)) {
                reasons.add("At the current pace, estimated completion is " + estimatedDate + ".");
            }
        } else {
            reasons.add("At the current pace, estimated completion is " + estimatedDate
                    + ", on or before the ex-factory date.");
        }
        if (reportedCompleted > total) {
            reasons.add("Final-stage reports exceed the order by " + (reportedCompleted - total)
                    + " pieces; completed progress is capped at the order quantity.");
        }
        return List.copyOf(reasons);
    }

    private boolean isPaceInsufficient(int completed, int remaining, long elapsedDays, long remainingDays) {
        if (remaining <= 0) return false;
        if (completed <= 0 || elapsedDays <= 0 || remainingDays <= 0) return true;
        BigDecimal achievedPace = BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(remainingDays));
        BigDecimal requiredPace = BigDecimal.valueOf(remaining).multiply(BigDecimal.valueOf(elapsedDays));
        return achievedPace.compareTo(requiredPace) < 0;
    }

    private User requireAnalyticsAccess(Authentication authentication) {
        if (hasRole(authentication, "MERCHANDISER")) return null;
        if (hasRole(authentication, "SUPERVISOR") && authentication.getPrincipal() instanceof User supervisor) {
            return supervisor;
        }
        throw new AccessDeniedException("Analytics access is restricted to merchandisers and assigned supervisors");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private void ensureAnalysable(ProductionOrder order) {
        if (order.getStatus() == ProductionOrder.Status.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders are excluded from delay analytics");
        }
        if (order.getTotalQuantity() == null || order.getTotalQuantity() <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive for delay analytics");
        }
    }

    private BigDecimal divide(BigDecimal quantity, long days) {
        return quantity.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private int count(List<ProductionDelayDtos.OrderDelayRiskResponse> risks, ProductionDelayDtos.RiskStatus status) {
        return (int) risks.stream().filter(risk -> risk.riskStatus() == status).count();
    }

    private LocalDate today() { return LocalDate.now(clock); }
}