package com.threadflow.order;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.order.warning.BuyerDelayWarning;
import com.threadflow.order.warning.BuyerDelayWarningRepository;
import com.threadflow.order.warning.BuyerDelayWarningStatus;
import com.threadflow.order.warning.BuyerWarningDtos;
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
class BuyerDelayWarningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BuyerDelayWarningRepository warningRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private ProductionOrder testOrder;
    private User testMerchandiser;

    @BeforeEach
    void setUp() {
        Role merchRole = roleRepository.findByName("MERCHANDISER").orElseThrow();
        testMerchandiser = userRepository.findByEmailIgnoreCase("merchandiser-warning@threadflow.local")
                .orElseGet(() -> {
                    User user = new User("merchandiser-warning@threadflow.local", "secret", true);
                    user.addRole(merchRole);
                    return userRepository.save(user);
                });

        Buyer buyer = buyerRepository.save(new Buyer("Warning Test Buyer", "France", null));
        testOrder = new ProductionOrder(buyer, "PO-WARN-" + UUID.randomUUID().toString().substring(0, 6),
                "Warning Jersey", "Description", 2000, LocalDate.now().plusDays(10), testMerchandiser.getId());
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    @WithMockUser(username = "merchandiser-warning@threadflow.local", roles = "MERCHANDISER")
    void merchandiserCanCreateConfirmAndDismissDelayWarnings() throws Exception {
        BuyerWarningDtos.CreateWarningDraftRequest draftRequest = new BuyerWarningDtos.CreateWarningDraftRequest(
                "Estimated 5-day delivery delay due to equipment servicing"
        );

        // 1. Create Draft
        String createResponse = mockMvc.perform(post("/api/v1/orders/" + testOrder.getId() + "/buyer-warnings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_CONFIRMATION")))
                .andExpect(jsonPath("$.riskStatus", is("AT_RISK")))
                .andReturn().getResponse().getContentAsString();

        BuyerWarningDtos.BuyerWarningResponse created = objectMapper.readValue(
                createResponse, BuyerWarningDtos.BuyerWarningResponse.class);

        // 2. Confirm Draft
        BuyerWarningDtos.ConfirmWarningRequest confirmRequest = new BuyerWarningDtos.ConfirmWarningRequest(
                "Approved buyer message: Delivery will be postponed by 5 calendar days due to planned maintenance."
        );

        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/buyer-warnings/" + created.id() + "/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.confirmedMessage", is("Approved buyer message: Delivery will be postponed by 5 calendar days due to planned maintenance.")));

        // 3. Dismiss
        mockMvc.perform(put("/api/v1/orders/" + testOrder.getId() + "/buyer-warnings/" + created.id() + "/dismiss")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISMISSED")));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotManageBuyerDelayWarnings() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + testOrder.getId() + "/buyer-warnings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
