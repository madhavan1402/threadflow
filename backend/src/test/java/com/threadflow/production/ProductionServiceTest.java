package com.threadflow.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.threadflow.buyer.Buyer;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.Role;
import com.threadflow.user.User;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class ProductionServiceTest {
    private ProductionUpdateRepository updates;
    private ProductionPhotoRepository photos;
    private SupervisorOrderAssignmentRepository assignments;
    private SupervisorUnitAssignmentRepository unitAssignments;
    private SupervisorOrderAccessService supervisorAccess;
    private OrderRepository orders;
    private ProductionPhotoStorage storage;
    private ProductionService service;
    private User supervisor;
    private ProductionOrder order;
    private OrderMilestone milestone;

    @BeforeEach
    void setUp() {
        updates = mock(ProductionUpdateRepository.class);
        photos = mock(ProductionPhotoRepository.class);
        assignments = mock(SupervisorOrderAssignmentRepository.class);
        orders = mock(OrderRepository.class);
        unitAssignments = mock(SupervisorUnitAssignmentRepository.class);
        supervisorAccess = new SupervisorOrderAccessService(orders, assignments, unitAssignments);
        storage = mock(ProductionPhotoStorage.class);
        service = new ProductionService(updates, photos, supervisorAccess, orders, storage);

        Role supervisorRole = new Role("SUPERVISOR");
        supervisor = new User("supervisor@local.threadflow", "encoded", true);
        supervisor.addRole(supervisorRole);
        ReflectionTestUtils.setField(supervisor, "id", 7L);

        order = new ProductionOrder(new Buyer("Company B", "UK", null), "B-PO-1",
                "Crew tee", "Sample", 1000, LocalDate.now().plusDays(30), 1L);
        ReflectionTestUtils.setField(order, "id", 12L);
        milestone = new OrderMilestone(order, "Stitching", 1, LocalDate.now(), LocalDate.now().plusDays(5));
        ReflectionTestUtils.setField(milestone, "id", 21L);
        order.addMilestone(milestone);
    }

    @Test
    void supervisorCanSubmitValidUpdateWithTamilNote() {
        when(assignments.existsBySupervisorIdAndOrderIdAndActiveTrue(7L, 12L)).thenReturn(true);
        when(orders.findWithDetailsById(12L)).thenReturn(Optional.of(order));
        when(updates.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(updates.sumCompletedQuantityForOrderAndMilestone(12L, 21L)).thenReturn(0L);
        when(updates.save(any(ProductionUpdate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductionDtos.UpdateResponse response = service.submit(
                new ProductionDtos.CreateUpdateRequest(12L, 21L, LocalDate.now(), 100,
                        "இயந்திரம் பழுதாகிவிட்டது", "key-1"),
                null, supervisorAuthentication());

        assertEquals("இயந்திரம் பழுதாகிவிட்டது", response.issueNote());
        assertEquals(100, response.completedQuantity());
    }

    @Test
    void unassignedSupervisorIsRejected() {
        when(assignments.existsBySupervisorIdAndOrderIdAndActiveTrue(7L, 12L)).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.submit(request(100), null, supervisorAuthentication()));
    }

    @Test
    void invalidQuantityIsRejected() {
        when(assignments.existsBySupervisorIdAndOrderIdAndActiveTrue(7L, 12L)).thenReturn(true);
        when(orders.findWithDetailsById(12L)).thenReturn(Optional.of(order));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(request(0), null, supervisorAuthentication()));
    }

    @Test
    void milestoneOutsideOrderIsRejected() {
        when(assignments.existsBySupervisorIdAndOrderIdAndActiveTrue(7L, 12L)).thenReturn(true);
        when(orders.findWithDetailsById(12L)).thenReturn(Optional.of(order));
        ProductionDtos.CreateUpdateRequest request = new ProductionDtos.CreateUpdateRequest(
                12L, 999L, LocalDate.now(), 100, null, "key-2");
        assertThrows(IllegalArgumentException.class, () -> service.submit(request, null, supervisorAuthentication()));
    }

    private ProductionDtos.CreateUpdateRequest request(int quantity) {
        return new ProductionDtos.CreateUpdateRequest(12L, 21L, LocalDate.now(), quantity, null, "key");
    }

    private Authentication supervisorAuthentication() {
        return new UsernamePasswordAuthenticationToken(supervisor, "", supervisor.getAuthorities());
    }
}
