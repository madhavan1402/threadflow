package com.threadflow.buyer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadflow.buyer.portal.BuyerAccessToken;
import com.threadflow.buyer.portal.BuyerAccessTokenRepository;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.order.warning.BuyerDelayWarning;
import com.threadflow.order.warning.BuyerDelayWarningRepository;
import com.threadflow.production.ProductionPhoto;
import com.threadflow.production.ProductionPhotoRepository;
import com.threadflow.production.ProductionPhotoStorage;
import com.threadflow.production.ProductionUpdate;
import com.threadflow.production.ProductionUpdateRepository;
import com.threadflow.user.Role;
import com.threadflow.user.RoleRepository;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerPortalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private BuyerAccessTokenRepository tokenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BuyerDelayWarningRepository warningRepository;

    @Autowired
    private ProductionUpdateRepository updateRepository;

    @Autowired
    private ProductionPhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductionPhotoStorage photoStorage;

    private Buyer buyerA;
    private Buyer buyerB;
    private BuyerAccessToken tokenA;
    private ProductionOrder orderA;
    private ProductionOrder orderB;
    private User merchandiser;

    @BeforeEach
    void setUp() {
        Role merchRole = roleRepository.findByName("MERCHANDISER").orElseThrow();
        User user = new User("merch-" + UUID.randomUUID() + "@threadflow.local", "secret", true);
        user.addRole(merchRole);
        merchandiser = userRepository.save(user);

        buyerA = buyerRepository.save(new Buyer("Buyer Alpha Corp", "UK", "alpha@buyer.com"));
        buyerB = buyerRepository.save(new Buyer("Buyer Beta Ltd", "Germany", "beta@buyer.com"));

        tokenA = tokenRepository.save(new BuyerAccessToken(
                buyerA,
                "tf_buyer_alpha_" + UUID.randomUUID(),
                "Alpha Test Token",
                Instant.now().plus(30, ChronoUnit.DAYS),
                merchandiser
        ));

        orderA = new ProductionOrder(buyerA, "PO-ALPHA-" + UUID.randomUUID().toString().substring(0, 8),
                "Summer Polo", "Men polo navy", 5000, LocalDate.now().plusDays(20), merchandiser.getId());
        orderA.addMilestone(new OrderMilestone(orderA, "Cutting", 1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(2)));
        orderA.addMilestone(new OrderMilestone(orderA, "Stitching", 2, LocalDate.now().plusDays(3), LocalDate.now().plusDays(10)));
        orderA = orderRepository.save(orderA);

        orderB = new ProductionOrder(buyerB, "PO-BETA-" + UUID.randomUUID().toString().substring(0, 8),
                "Winter Hoodie", "Fleece hoodie black", 3000, LocalDate.now().plusDays(30), merchandiser.getId());
        orderB.addMilestone(new OrderMilestone(orderB, "Knitting", 1, LocalDate.now().minusDays(10), LocalDate.now().minusDays(2)));
        orderB = orderRepository.save(orderB);
    }

    @Test
    void unauthenticatedRequestToBuyerEndpointsReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/buyer/portal"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buyerTokenAuthenticatesAndLimitsOrdersToThatBuyer() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/orders")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].poNumber", is(orderA.getPoNumber())))
                .andExpect(jsonPath("$[0].styleName", is(orderA.getStyleName())));
    }

    @Test
    void buyerCannotAccessAnotherBuyersOrder() throws Exception {
        // Buyer A attempts to access Buyer B's order
        mockMvc.perform(get("/api/v1/buyer/orders/" + orderB.getId())
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unconfirmedDelayWarningsAreInvisibleToBuyer() throws Exception {
        // Create an unconfirmed warning for order A
        BuyerDelayWarning warning = new BuyerDelayWarning(
                orderA,
                "HIGH",
                "Internal Tamil note: சாயப்பட்டறை தாமதம் (Dyeing delay)",
                "Potential 4-day delivery delay suggested"
        );
        warningRepository.save(warning);

        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId())
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedWarnings", hasSize(0)));
    }

    @Test
    void confirmedDelayWarningsAreVisibleToBuyerWithoutInternalNotes() throws Exception {
        // Create and confirm a warning for order A
        BuyerDelayWarning warning = new BuyerDelayWarning(
                orderA,
                "HIGH",
                "Internal Tamil note: சாயப்பட்டறை தாமதம் (Dyeing delay)",
                "Potential 4-day delivery delay suggested"
        );
        warning.confirm("Production is rescheduled by 4 days due to lab dip approval re-check.", merchandiser);
        warningRepository.save(warning);

        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId())
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedWarnings", hasSize(1)))
                .andExpect(jsonPath("$.confirmedWarnings[0].message",
                        is("Production is rescheduled by 4 days due to lab dip approval re-check.")))
                .andExpect(jsonPath("$.confirmedWarnings[0].riskStatus", is("HIGH")))
                // Ensure internal note is not leaked
                .andExpect(jsonPath("$.confirmedWarnings[0].internalReasons").doesNotExist())
                .andExpect(jsonPath("$.confirmedWarnings[0].suggestedMessage").doesNotExist());
    }

    @Test
    void unapprovedPhotosAreInvisibleToBuyer() throws Exception {
        OrderMilestone milestone = orderA.getMilestones().getFirst();
        ProductionUpdate update = updateRepository.save(new ProductionUpdate(
                orderA, milestone, merchandiser, LocalDate.now(), 500, "Daily output note", "idemp-" + UUID.randomUUID()));

        Path tempFile = Files.createTempFile("test-photo", ".jpg");
        Files.write(tempFile, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        ProductionPhoto photo = new ProductionPhoto(update, "cutting.jpg", "stored-cutting.jpg",
                "image/jpeg", 12L, tempFile.toString());
        // explicitly not approved
        photoRepository.save(photo);

        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId() + "/photos")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Directly requesting unapproved photo content returns 400 or 403
        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId() + "/photos/" + photo.getId() + "/content")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().is4xxClientError());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void approvedPhotosAreVisibleAndDownloadableByBuyer() throws Exception {
        OrderMilestone milestone = orderA.getMilestones().getFirst();
        ProductionUpdate update = updateRepository.save(new ProductionUpdate(
                orderA, milestone, merchandiser, LocalDate.now(), 500, "Daily output note", "idemp-" + UUID.randomUUID()));

        Path tempFile = Files.createTempFile("test-photo", ".jpg");
        byte[] dummyJpeg = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        Files.write(tempFile, dummyJpeg);

        ProductionPhoto photo = new ProductionPhoto(update, "approved-fabric.jpg", "stored-fabric.jpg",
                "image/jpeg", 12L, tempFile.toString());
        photo.setBuyerApproval(true, merchandiser);
        photoRepository.save(photo);

        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId() + "/photos")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].originalFilename", is("approved-fabric.jpg")));

        mockMvc.perform(get("/api/v1/buyer/orders/" + orderA.getId() + "/photos/" + photo.getId() + "/content")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isOk());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void buyerCannotAccessInternalMerchandiserOrSupervisorEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/workspaces/merchandiser/orders")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/workspaces/supervisor/updates")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokedOrExpiredTokenReturns401() throws Exception {
        tokenA.revoke();
        tokenRepository.save(tokenA);

        mockMvc.perform(get("/api/v1/buyer/orders")
                        .header("X-Buyer-Token", tokenA.getTokenValue()))
                .andExpect(status().isUnauthorized());
    }
}
