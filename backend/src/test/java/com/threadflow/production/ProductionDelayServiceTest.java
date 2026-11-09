package com.threadflow.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.threadflow.buyer.Buyer;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.Role;
import com.threadflow.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class ProductionDelayServiceTest {
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
    private static final Long ORDER_ID = 101L;
    private static final Long FINAL_MILESTONE_ID = 202L;

    private OrderRepository orders;
    private ProductionUpdateRepository updates;
    private SupervisorOrderAccessService supervisorAccess;
    private ProductionDelayService service;
    private ProductionOrder order;
    private OrderMilestone earlierMilestone;
    private OrderMilestone finalMilestone;

    @BeforeEach
    void setUp() {
        orders = org.mockito.Mockito.mock(OrderRepository.class);
        updates = org.mockito.Mockito.mock(ProductionUpdateRepository.class);
        supervisorAccess = org.mockito.Mockito.mock(SupervisorOrderAccessService.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(FACTORY_ZONE).toInstant(), FACTORY_ZONE);
        service = new ProductionDelayService(orders, updates, supervisorAccess, clock);

        order = new ProductionOrder(new Buyer("Tiruppur buyer", "UK", null), "PO-101", "Knit shirt",
                null, 1000, TODAY.plusDays(10), 1L);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        earlierMilestone = milestone(201L, "Stitching", 5);
        finalMilestone = milestone(FINAL_MILESTONE_ID, "Finishing and packing", 6);
        order.addMilestone(earlierMilestone);
        order.addMilestone(finalMilestone);
    }

    @Test
    void classifiesOrderOnTrackWhenFinalStagePaceMeetsDeadline() {
        var risk = service.calculate(order, List.of(update(finalMilestone, 500, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.ON_TRACK, risk.riskStatus());
        assertEquals(500, risk.remainingQuantity());
        assertEquals(TODAY.plusDays(10), risk.estimatedCompletionDate());
    }

    @Test
    void classifiesOrderAtRiskWhenActualPaceIsInsufficient() {
        var risk = service.calculate(order, List.of(update(finalMilestone, 100, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.AT_RISK, risk.riskStatus());
        assertEquals(100, risk.completedQuantity());
        assertEquals(900, risk.remainingQuantity());
        assertTrue(risk.reasons().size() >= 2);
    }

    @Test
    void classifiesOrderDelayedAfterExFactoryDate() {
        order.update(order.getBuyer(), order.getStyleName(), null, 1000,
                TODAY.minusDays(1), ProductionOrder.Status.IN_PROGRESS);

        var risk = service.calculate(order, List.of(update(finalMilestone, 100, TODAY.minusDays(1))), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.DELAYED, risk.riskStatus());
        assertEquals(0, risk.remainingProductionDays());
        assertNull(risk.requiredDailyRate());
    }

    @Test
    void classifiesOrderCompletedWhenReportedQuantityMeetsOrderTotal() {
        var risk = service.calculate(order, List.of(update(finalMilestone, 1000, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.COMPLETED, risk.riskStatus());
        assertEquals(0, risk.remainingQuantity());
        assertEquals(new java.math.BigDecimal("100.00"), risk.completionPercent());
    }

    @Test
    void zeroProductionHasZeroActualRateAndNoEstimatedDate() {
        var risk = service.calculate(order, List.of(), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.AT_RISK, risk.riskStatus());
        assertEquals(0, risk.actualDailyRate().compareTo(java.math.BigDecimal.ZERO));
        assertNull(risk.estimatedCompletionDate());
        assertEquals(1000, risk.remainingQuantity());
    }

    @Test
    void zeroRemainingDaysDoesNotDivideByZero() {
        order.update(order.getBuyer(), order.getStyleName(), null, 1000,
            TODAY.minusDays(1), ProductionOrder.Status.IN_PROGRESS);

        var risk = service.calculate(order, List.of(update(finalMilestone, 100, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.DELAYED, risk.riskStatus());
        assertEquals(0, risk.remainingProductionDays());
        assertNull(risk.requiredDailyRate());
    }

    @Test
    void classificationUsesExactRatesWhenDisplayedRatesRoundEqual() {
        order.update(order.getBuyer(), order.getStyleName(), null, 1000,
                TODAY.plusDays(5), ProductionOrder.Status.IN_PROGRESS);
        OrderMilestone finalDayMilestone = milestone(203L, "Shipment", 7);
        OrderMilestone longPlan = new OrderMilestone(order, "Planning start", 1,
                TODAY.minusDays(5999), TODAY.minusDays(5998));
        ReflectionTestUtils.setField(longPlan, "id", 204L);
        order.addMilestone(longPlan);
        order.addMilestone(finalDayMilestone);

        var risk = service.calculate(order, List.of(update(finalDayMilestone, 999, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.AT_RISK, risk.riskStatus());
        assertEquals(risk.requiredDailyRate(), risk.actualDailyRate());
    }

    @Test
    void overProductionIsCappedWithoutMakingRemainingQuantityNegative() {
        var risk = service.calculate(order, List.of(update(finalMilestone, 1200, TODAY)), TODAY);

        assertEquals(ProductionDelayDtos.RiskStatus.COMPLETED, risk.riskStatus());
        assertEquals(1200, risk.reportedCompletedQuantity());
        assertEquals(1000, risk.completedQuantity());
        assertEquals(0, risk.remainingQuantity());
        assertEquals(2, risk.reasons().size());
    }

    @Test
    void ignoresEarlierMilestoneAndFutureOrNonPositiveReports() {
        List<ProductionUpdate> orderUpdates = List.of(
                update(earlierMilestone, 700, TODAY),
                update(finalMilestone, 200, TODAY),
                update(finalMilestone, 900, TODAY.plusDays(1)),
                update(finalMilestone, 0, TODAY));

        var risk = service.calculate(order, orderUpdates, TODAY);

        assertEquals(200, risk.reportedCompletedQuantity());
        assertEquals("Finishing and packing", risk.finalMilestoneName());
    }

    @Test
    void excludesCancelledOrdersFromDelayAnalysis() {
        order.update(order.getBuyer(), order.getStyleName(), null, 1000,
                order.getExFactoryDate(), ProductionOrder.Status.CANCELLED);

        assertThrows(IllegalArgumentException.class, () -> service.calculate(order, List.of(), TODAY));
    }

    @Test
    void supervisorCannotReadRiskForUnassignedOrder() {
        User supervisor = user("SUPERVISOR");
        Authentication authentication = authentication(supervisor);
        doThrow(new AccessDeniedException("not assigned"))
                .when(supervisorAccess).requireAccess(supervisor, ORDER_ID);

        assertThrows(AccessDeniedException.class, () -> service.orderRisk(ORDER_ID, authentication));
        verify(orders, never()).findWithDetailsById(ORDER_ID);
    }

    @Test
    void merchandiserCanReadOrderRiskWithoutSupervisorAssignment() {
        User merchandiser = user("MERCHANDISER");
        when(orders.findWithDetailsById(ORDER_ID)).thenReturn(Optional.of(order));
        when(updates.findByOrderIdOrderByProductionDateDescCreatedAtDesc(ORDER_ID)).thenReturn(List.of());

        var risk = service.orderRisk(ORDER_ID, authentication(merchandiser));

        assertEquals(ProductionDelayDtos.RiskStatus.AT_RISK, risk.riskStatus());
        verify(supervisorAccess, never()).requireAccess(merchandiser, ORDER_ID);
    }

    @Test
    void supervisorRiskListIsLoadedOnlyFromCentralAccessibleOrderIds() {
        User supervisor = user("SUPERVISOR");
        when(supervisorAccess.accessibleOrderIds(7L)).thenReturn(List.of(ORDER_ID));
        when(orders.findDistinctByIdIn(List.of(ORDER_ID))).thenReturn(List.of(order));
        when(updates.findByOrderIdIn(List.of(ORDER_ID))).thenReturn(List.of(update(finalMilestone, 200, TODAY)));

        var risks = service.allOrderRisks(authentication(supervisor));

        assertEquals(1, risks.size());
        assertEquals(ORDER_ID, risks.getFirst().orderId());
        verify(orders, never()).findByStatusNot(ProductionOrder.Status.CANCELLED);
    }

    private OrderMilestone milestone(Long id, String name, int sequence) {
        OrderMilestone milestone = new OrderMilestone(order, name, sequence,
                TODAY.minusDays(9), TODAY.plusDays(1));
        ReflectionTestUtils.setField(milestone, "id", id);
        return milestone;
    }

    private ProductionUpdate update(OrderMilestone milestone, int quantity, LocalDate date) {
        return new ProductionUpdate(order, milestone, user("SUPERVISOR"), date, quantity, null,
                "delay-test-" + milestone.getId() + "-" + quantity + "-" + date);
    }

    private User user(String roleName) {
        User user = new User(roleName.toLowerCase() + "@threadflow.test", "encoded", true);
        user.addRole(new Role(roleName));
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }
}