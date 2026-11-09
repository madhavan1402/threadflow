package com.threadflow.config;

import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.milestone.MilestoneTemplate;
import com.threadflow.milestone.MilestoneTemplateRepository;
import com.threadflow.milestone.TemplateMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.OrderVariant;
import com.threadflow.order.ProductionOrder;
import com.threadflow.order.OrderMilestone;
import com.threadflow.production.SupervisorOrderAssignment;
import com.threadflow.production.SupervisorOrderAssignmentRepository;
import com.threadflow.production.ProductionUnit;
import com.threadflow.production.ProductionUnitRepository;
import com.threadflow.production.SupervisorUnitAssignment;
import com.threadflow.production.SupervisorUnitAssignmentRepository;
import com.threadflow.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@Order(2)
public class LocalOrderSampleInitializer implements ApplicationRunner {
    private final BuyerRepository buyers;
    private final MilestoneTemplateRepository templates;
    private final OrderRepository orders;
    private final UserRepository users;
    private final SupervisorOrderAssignmentRepository assignments;
        private final ProductionUnitRepository units;
        private final SupervisorUnitAssignmentRepository unitAssignments;

    public LocalOrderSampleInitializer(BuyerRepository buyers, MilestoneTemplateRepository templates,
                                       OrderRepository orders, UserRepository users,
                                       SupervisorOrderAssignmentRepository assignments,
                                       ProductionUnitRepository units,
                                       SupervisorUnitAssignmentRepository unitAssignments) {
        this.buyers = buyers; this.templates = templates; this.orders = orders;
        this.users = users; this.assignments = assignments; this.units = units;
        this.unitAssignments = unitAssignments;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        MilestoneTemplate template = templates.findByTemplateNameIgnoreCase("Standard knitwear TNA")
                .orElseGet(() -> {
                    MilestoneTemplate created = new MilestoneTemplate("Standard knitwear TNA",
                            "Local sample TNA covering yarn through shipment", true);
                    created.addMilestone(new TemplateMilestone("Yarn allocation", 1, 2));
                    created.addMilestone(new TemplateMilestone("Knitting and dyeing", 2, 8));
                    created.addMilestone(new TemplateMilestone("Cutting", 3, 4));
                    created.addMilestone(new TemplateMilestone("Stitching", 4, 10));
                    created.addMilestone(new TemplateMilestone("Finishing and packing", 5, 5));
                    created.addMilestone(new TemplateMilestone("Shipment", 6, 2));
                    return templates.save(created);
                });

        Buyer buyerB = buyers.findByBuyerNameIgnoreCaseAndCountryIgnoreCase("Company B", "UK")
                .orElseGet(() -> buyers.save(new Buyer("Company B", "UK", null)));
        Buyer buyerC = buyers.findByBuyerNameIgnoreCaseAndCountryIgnoreCase("Company C", "Germany")
                .orElseGet(() -> buyers.save(new Buyer("Company C", "Germany", null)));

        createOrder("B-PO-88213", buyerB, "Men's crew neck tee navy",
                "Men's crew neck tee in navy", 12000, LocalDate.of(2026, 11, 20), template);
        createOrder("B-PO-88240", buyerB, "Men's polo white",
                "Men's polo shirt in white", 8000, LocalDate.of(2026, 11, 12), template);
        createOrder("C-PO-0488", buyerC, "Kids printed tee yellow",
                "Kids printed tee in yellow", 10000, LocalDate.of(2026, 11, 28), template);

        ProductionUnit stitchingOne = ensureUnit("Stitching Unit 1", "STITCH-1", "Main garment stitching floor");
        ProductionUnit stitchingTwo = ensureUnit("Stitching Unit 2", "STITCH-2", "Secondary stitching floor");
        ProductionUnit printing = ensureUnit("Printing Unit", "PRINT", "Screen and digital print facility");
        assignUnit("B-PO-88213", stitchingOne);
        assignUnit("B-PO-88240", stitchingTwo);
        assignUnit("C-PO-0488", printing);

        users.findByEmailIgnoreCase("supervisor@local.threadflow").ifPresent(supervisor -> {
            if (!supervisor.isEnabled() || !supervisor.hasRole("SUPERVISOR")) return;
            ensureMembership(supervisor, stitchingOne);
            ensureMembership(supervisor, stitchingTwo);
            orders.findByPoNumberIgnoreCase("C-PO-0488").ifPresent(order -> {
                if (!assignments.existsBySupervisorIdAndOrderId(supervisor.getId(), order.getId())) {
                    assignments.save(new SupervisorOrderAssignment(supervisor, order, supervisor));
                }
            });
            assignments.findBySupervisorIdAndActiveTrue(supervisor.getId()).stream()
                    .filter(assignment -> !List.of("B-PO-88213", "B-PO-88240", "C-PO-0488")
                            .contains(assignment.getOrder().getPoNumber()))
                    .forEach(assignment -> assignment.revoke(java.time.Instant.now()));
        });
    }

    private void createOrder(String po, Buyer buyer, String style, String description, int quantity,
                             LocalDate exFactory, MilestoneTemplate template) {
        if (orders.existsByPoNumberIgnoreCase(po)) return;
        ProductionOrder order = new ProductionOrder(buyer, po, style, description, quantity, exFactory, null);
        order.addVariant(new OrderVariant(order, "Sample color", "Mixed", quantity));

        int totalDuration = template.getMilestones().stream().mapToInt(TemplateMilestone::getDefaultDurationDays).sum();
        LocalDate start = exFactory.minusDays(totalDuration);
        for (TemplateMilestone item : template.getMilestones()) {
            LocalDate end = start.plusDays(item.getDefaultDurationDays());
            order.addMilestone(new OrderMilestone(order, item.getMilestoneName(), item.getSequenceNumber(), start, end));
            start = end;
        }
        orders.save(order);
    }

    private ProductionUnit ensureUnit(String name, String code, String description) {
        return units.findByCodeIgnoreCase(code).orElseGet(() -> units.save(
                new ProductionUnit(name, code, description, true)));
    }

    private void assignUnit(String poNumber, ProductionUnit unit) {
        orders.findByPoNumberIgnoreCase(poNumber).ifPresent(order -> {
            if (order.getProductionUnit() == null) {
                order.assignProductionUnit(unit);
                orders.save(order);
            }
        });
    }

    private void ensureMembership(com.threadflow.user.User supervisor, ProductionUnit unit) {
        if (!unitAssignments.existsBySupervisorIdAndProductionUnitId(supervisor.getId(), unit.getId())) {
            unitAssignments.save(new SupervisorUnitAssignment(supervisor, unit, supervisor));
        }
    }
}
