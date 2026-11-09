package com.threadflow.order;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.order.audit.OrderAuditDtos;
import com.threadflow.order.audit.OrderDateAuditHistoryRepository;
import com.threadflow.user.Role;
import com.threadflow.user.RoleRepository;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderDateAuditHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDateAuditHistoryRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private ProductionOrder testOrder;
    private User testMerchandiser;

    @BeforeEach
    void setUp() {
        Role merchRole = roleRepository.findByName("MERCHANDISER").orElseThrow();
        testMerchandiser = userRepository.findByEmailIgnoreCase("merchandiser-audit@threadflow.local")
                .orElseGet(() -> {
                    User user = new User("merchandiser-audit@threadflow.local", "secret", true);
                    user.addRole(merchRole);
                    return userRepository.save(user);
                });

        Buyer buyer = buyerRepository.save(new Buyer("Audit Test Buyer", "UK", null));
        testOrder = new ProductionOrder(buyer, "PO-AUDIT-" + UUID.randomUUID().toString().substring(0, 6),
                "Audit Tee", "Description", 1000, LocalDate.now().plusDays(15), testMerchandiser.getId());
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    @WithMockUser(username = "merchandiser-audit@threadflow.local", roles = "MERCHANDISER")
    void merchandiserCanChangeDateAndAuditRecordIsAppended() throws Exception {
        LocalDate originalDate = testOrder.getExFactoryDate();
        LocalDate newDate = originalDate.plusDays(7);
        OrderAuditDtos.DateChangeRequest request = new OrderAuditDtos.DateChangeRequest(
                newDate, "Fabric delivery delayed by mill by 7 days");

        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/ex-factory-date")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exFactoryDate", is(newDate.toString())));

        // Verify audit history endpoint returns the change
        mockMvc.perform(get("/api/v1/orders/" + testOrder.getId() + "/audit-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].previousExFactoryDate", is(originalDate.toString())))
                .andExpect(jsonPath("$[0].newExFactoryDate", is(newDate.toString())))
                .andExpect(jsonPath("$[0].reason", is("Fabric delivery delayed by mill by 7 days")));
    }

    @Test
    @WithMockUser(username = "merchandiser-audit@threadflow.local", roles = "MERCHANDISER")
    void dateChangeWithoutReasonIsRejected() throws Exception {
        LocalDate newDate = testOrder.getExFactoryDate().plusDays(7);
        OrderAuditDtos.DateChangeRequest request = new OrderAuditDtos.DateChangeRequest(
                newDate, "   ");

        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/ex-factory-date")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "merchandiser-audit@threadflow.local", roles = "MERCHANDISER")
    void dateChangeToSameDateIsRejected() throws Exception {
        OrderAuditDtos.DateChangeRequest request = new OrderAuditDtos.DateChangeRequest(
                testOrder.getExFactoryDate(), "No real change");

        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/ex-factory-date")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotChangeExFactoryDate() throws Exception {
        LocalDate newDate = testOrder.getExFactoryDate().plusDays(5);
        OrderAuditDtos.DateChangeRequest request = new OrderAuditDtos.DateChangeRequest(
                newDate, "Supervisor attempting date change");

        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/ex-factory-date")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
