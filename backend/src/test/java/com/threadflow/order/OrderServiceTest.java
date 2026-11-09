package com.threadflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.milestone.MilestoneTemplate;
import com.threadflow.milestone.MilestoneTemplateRepository;
import com.threadflow.production.SupervisorOrderAccessService;
import com.threadflow.milestone.TemplateMilestone;
import com.threadflow.order.audit.OrderAuditService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class OrderServiceTest {
    private OrderRepository orders;
    private BuyerRepository buyers;
    private MilestoneTemplateRepository templates;
    private OrderAuditService auditService;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        buyers = mock(BuyerRepository.class);
        templates = mock(MilestoneTemplateRepository.class);
        auditService = mock(OrderAuditService.class);
        service = new OrderService(orders, buyers, templates, mock(SupervisorOrderAccessService.class), auditService);
    }

    @Test
    void duplicatePoIsRejected() {
        when(orders.existsByPoNumberIgnoreCase("B-PO-1")).thenReturn(true);
        var request = request("B-PO-1", 100, List.of(new OrderDtos.VariantRequest("Navy", "M", 100)));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);
        assertEquals("PO number already exists: B-PO-1",
                assertThrows(IllegalArgumentException.class, () -> service.create(request, authentication)).getMessage());
    }

    @Test
    void variantMismatchIsRejected() {
        var request = request("B-PO-2", 100, List.of(new OrderDtos.VariantRequest("Navy", "M", 90)));
        assertThrows(IllegalArgumentException.class, () -> service.create(request, mock(Authentication.class)));
    }

    @Test
    void milestoneGenerationCreatesTemplateSequence() {
        Buyer buyer = new Buyer("Company B", "UK", null);
        MilestoneTemplate template = new MilestoneTemplate("Standard", "Test", true);
        template.addMilestone(new TemplateMilestone("Cutting", 1, 4));
        template.addMilestone(new TemplateMilestone("Stitching", 2, 6));
        when(buyers.findById(1L)).thenReturn(Optional.of(buyer));
        when(templates.findById(2L)).thenReturn(Optional.of(template));
        when(orders.save(any(ProductionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = request("B-PO-3", 100, List.of(new OrderDtos.VariantRequest("Navy", "M", 100)));
        request = new OrderDtos.CreateOrderRequest(1L, request.poNumber(), request.styleName(),
                request.productDescription(), 100, LocalDate.now().plusDays(30), 2L, request.variants());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        OrderDtos.OrderResponse response = service.create(request, authentication);
        assertEquals(2, response.milestones().size());
        assertEquals("Cutting", response.milestones().get(0).milestoneName());
        assertEquals("Stitching", response.milestones().get(1).milestoneName());
    }

    private OrderDtos.CreateOrderRequest request(String po, int quantity, List<OrderDtos.VariantRequest> variants) {
        return new OrderDtos.CreateOrderRequest(1L, po, "Crew tee", "Sample", quantity,
                LocalDate.now().plusDays(30), 2L, variants);
    }
}
