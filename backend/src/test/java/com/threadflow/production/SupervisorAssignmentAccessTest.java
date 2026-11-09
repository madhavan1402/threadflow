package com.threadflow.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.Role;
import com.threadflow.user.RoleRepository;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupervisorAssignmentAccessTest {
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private BuyerRepository buyers;
    @Autowired private OrderRepository orders;
    @Autowired private ProductionUnitRepository units;
    @Autowired private SupervisorOrderAssignmentRepository orderAssignments;
    @Autowired private SupervisorUnitAssignmentRepository unitAssignments;
    @Autowired private SupervisorOrderAccessService access;
    @Autowired private ProductionUnitService assignmentService;
    @Autowired private ProductionService productionService;
    @Autowired private MockMvc mockMvc;

    private User supervisor;
    private User merchandiser;
    private ProductionUnit unit;
    private ProductionOrder order;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        Role supervisorRole = roles.findByName("SUPERVISOR").orElseThrow();
        Role merchandiserRole = roles.findByName("MERCHANDISER").orElseThrow();
        supervisor = new User("supervisor-" + suffix + "@threadflow.test", "encoded", true);
        supervisor.addRole(supervisorRole);
        supervisor = users.saveAndFlush(supervisor);
        merchandiser = new User("merchandiser-" + suffix + "@threadflow.test", "encoded", true);
        merchandiser.addRole(merchandiserRole);
        merchandiser = users.saveAndFlush(merchandiser);

        Buyer buyer = buyers.saveAndFlush(new Buyer("Buyer " + suffix, "UK", null));
        unit = units.saveAndFlush(new ProductionUnit("Stitching " + suffix, "ST-" + suffix.substring(0, 8), null, true));
        order = new ProductionOrder(buyer, "PO-" + suffix, "Knit shirt", null, 500,
                LocalDate.now().plusDays(30), merchandiser.getId());
        order.assignProductionUnit(unit);
        OrderMilestone milestone = new OrderMilestone(order, "Stitching", 1, LocalDate.now(), LocalDate.now().plusDays(10));
        order.addMilestone(milestone);
        order = orders.saveAndFlush(order);
    }

    @Test
    void supervisorCanAccessOrderThroughActiveUnitMembership() {
        unitAssignments.saveAndFlush(new SupervisorUnitAssignment(supervisor, unit, merchandiser));

        assertTrue(access.hasAccess(supervisor.getId(), order.getId()));
    }

    @Test
    void supervisorCanAccessIndividuallyAssignedOrder() {
        orderAssignments.saveAndFlush(new SupervisorOrderAssignment(supervisor, order, merchandiser));

        assertTrue(access.hasAccess(supervisor.getId(), order.getId()));
    }

    @Test
    void bothAssignmentRoutesReturnOneAccessibleOrder() {
        orderAssignments.saveAndFlush(new SupervisorOrderAssignment(supervisor, order, merchandiser));
        unitAssignments.saveAndFlush(new SupervisorUnitAssignment(supervisor, unit, merchandiser));

        assertEquals(List.of(order.getId()), orders.findAccessibleOrderIds(supervisor.getId()));
    }

    @Test
    void availableOrdersEndpointReturnsUnitAssignedOrderOnce() throws Exception {
        unitAssignments.saveAndFlush(new SupervisorUnitAssignment(supervisor, unit, merchandiser));

        mockMvc.perform(get("/api/v1/production/available-orders").with(user(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poNumber").value(order.getPoNumber()))
                .andExpect(jsonPath("$[0].productionUnit.name").value(unit.getName()))
                .andExpect(jsonPath("$[0].assignmentSource").value("UNIT"));
    }

    @Test
    void revokingUnitMembershipRemovesAccessWithoutDirectAssignment() {
        SupervisorUnitAssignment membership = unitAssignments.saveAndFlush(
                new SupervisorUnitAssignment(supervisor, unit, merchandiser));

        membership.revoke(Instant.now());
        unitAssignments.saveAndFlush(membership);

        assertFalse(access.hasAccess(supervisor.getId(), order.getId()));
    }

    @Test
    void revokingDirectAssignmentRemovesAccessWithoutUnitMembership() {
        SupervisorOrderAssignment assignment = orderAssignments.saveAndFlush(
                new SupervisorOrderAssignment(supervisor, order, merchandiser));

        assignment.revoke(Instant.now());
        orderAssignments.saveAndFlush(assignment);

        assertFalse(access.hasAccess(supervisor.getId(), order.getId()));
        assertEquals(1, orderAssignments.findByOrderIdOrderByAssignedAtDesc(order.getId()).size());
    }

    @Test
    void supervisorCannotAccessUnrelatedOrder() {
        ProductionOrder unrelated = createUnassignedOrder("unrelated");

        assertFalse(access.hasAccess(supervisor.getId(), unrelated.getId()));
    }

    @Test
    void inactiveSupervisorCannotBeAssigned() {
        User inactive = new User("inactive-" + UUID.randomUUID() + "@threadflow.test", "encoded", false);
        inactive.addRole(roles.findByName("SUPERVISOR").orElseThrow());
        inactive = users.saveAndFlush(inactive);

        User finalInactive = inactive;
        assertThrows(IllegalArgumentException.class, () -> assignmentService.setOrderSupervisors(
                order.getId(), new AssignmentDtos.SupervisorIdsRequest(List.of(finalInactive.getId())),
                authentication(merchandiser)));
        assertTrue(orderAssignments.findByOrderIdAndActiveTrue(order.getId()).isEmpty());
    }

    @Test
    void merchandiserCanManageDirectAndUnitAssignments() {
        assignmentService.setOrderSupervisors(order.getId(),
                new AssignmentDtos.SupervisorIdsRequest(List.of(supervisor.getId())), authentication(merchandiser));
        assignmentService.setOrderSupervisors(order.getId(),
            new AssignmentDtos.SupervisorIdsRequest(List.of(supervisor.getId())), authentication(merchandiser));
        assignmentService.setUnitSupervisors(unit.getId(),
                new AssignmentDtos.SupervisorIdsRequest(List.of(supervisor.getId())), authentication(merchandiser));
        assignmentService.setUnitSupervisors(unit.getId(),
            new AssignmentDtos.SupervisorIdsRequest(List.of(supervisor.getId())), authentication(merchandiser));

        assertTrue(access.hasAccess(supervisor.getId(), order.getId()));
        assertEquals(1, orderAssignments.findByOrderIdAndActiveTrue(order.getId()).size());
        assertEquals(1, unitAssignments.findByProductionUnitIdAndActiveTrue(unit.getId()).size());
    }

    @Test
    void productionUpdateRejectsUnauthorizedOrderId() {
        ProductionOrder unrelated = createUnassignedOrder("update-denied");
        Authentication authentication = authentication(supervisor);
        ProductionDtos.CreateUpdateRequest request = new ProductionDtos.CreateUpdateRequest(
                unrelated.getId(), 999L, LocalDate.now(), 10, null, "unauthorized-key");

        assertThrows(AccessDeniedException.class, () -> productionService.submit(request, null, authentication));
    }

    private ProductionOrder createUnassignedOrder(String label) {
        Buyer buyer = buyers.saveAndFlush(new Buyer("Buyer " + label + " " + UUID.randomUUID(), "US", null));
        return orders.saveAndFlush(new ProductionOrder(buyer, "PO-" + label + "-" + UUID.randomUUID(),
                "Knit shirt", null, 100, LocalDate.now().plusDays(20), merchandiser.getId()));
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }
}