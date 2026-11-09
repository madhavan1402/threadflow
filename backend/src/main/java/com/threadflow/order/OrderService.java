package com.threadflow.order;

import com.threadflow.buyer.Buyer;
import com.threadflow.buyer.BuyerRepository;
import com.threadflow.milestone.MilestoneTemplate;
import com.threadflow.milestone.MilestoneTemplateRepository;
import com.threadflow.milestone.TemplateMilestone;
import com.threadflow.production.SupervisorOrderAccessService;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final BuyerRepository buyers;
    private final MilestoneTemplateRepository templates;
    private final SupervisorOrderAccessService supervisorAccess;
    private final com.threadflow.order.audit.OrderAuditService auditService;
    private final UserRepository users;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderService(OrderRepository orders, BuyerRepository buyers, MilestoneTemplateRepository templates,
                        SupervisorOrderAccessService supervisorAccess,
                        com.threadflow.order.audit.OrderAuditService auditService,
                        UserRepository users) {
        this.orders = orders; this.buyers = buyers; this.templates = templates;
        this.supervisorAccess = supervisorAccess;
        this.auditService = auditService;
        this.users = users;
    }

    public OrderService(OrderRepository orders, BuyerRepository buyers, MilestoneTemplateRepository templates,
                        SupervisorOrderAccessService supervisorAccess,
                        com.threadflow.order.audit.OrderAuditService auditService) {
        this(orders, buyers, templates, supervisorAccess, auditService, null);
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderResponse> list(String search, Long buyerId, String status,
                                               LocalDate from, LocalDate to, Pageable pageable,
                                               Authentication authentication) {
        requireMerchandiser(authentication);
        Specification<ProductionOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String value = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("poNumber")), value),
                        cb.like(cb.lower(root.get("styleName")), value)));
            }
            if (buyerId != null) predicates.add(cb.equal(root.get("buyer").get("id"), buyerId));
            if (status != null && !status.isBlank()) {
                try { predicates.add(cb.equal(root.get("status"), ProductionOrder.Status.valueOf(status))); }
                catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid order status: " + status); }
            }
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("exFactoryDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("exFactoryDate"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return orders.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse get(Long id, Authentication authentication) {
        requireOrderReadAccess(id, authentication);
        return toResponse(orders.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id)));
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public OrderDtos.OrderResponse create(OrderDtos.CreateOrderRequest request, Authentication authentication) {
        validateVariants(request.totalQuantity(), request.variants());
        if (request.exFactoryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ex-factory date must be today or in the future");
        }
        if (orders.existsByPoNumberIgnoreCase(request.poNumber().trim())) {
            throw new IllegalArgumentException("PO number already exists: " + request.poNumber());
        }
        Buyer buyer = buyers.findById(request.buyerId())
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found: " + request.buyerId()));
        MilestoneTemplate template = templates.findById(request.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Milestone template not found: " + request.templateId()));
        if (!template.isActive()) throw new IllegalArgumentException("Selected milestone template is not active");
        if (template.getMilestones().isEmpty()) throw new IllegalArgumentException("Selected template has no milestones");

        Long userId = authentication != null && authentication.getPrincipal() instanceof User user ? user.getId() : null;
        ProductionOrder order = new ProductionOrder(buyer, request.poNumber().trim(), request.styleName().trim(),
                request.productDescription(), request.totalQuantity(), request.exFactoryDate(), userId);
        request.variants().forEach(v -> order.addVariant(new OrderVariant(order, v.color().trim(), v.size().trim(), v.quantity())));
        generateMilestones(order, template);
        return toResponse(orders.save(order));
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public OrderDtos.OrderResponse update(Long id, OrderDtos.UpdateOrderRequest request) {
        return update(id, request, null);
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public OrderDtos.OrderResponse update(Long id, OrderDtos.UpdateOrderRequest request, Authentication authentication) {
        ProductionOrder order = orders.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        validateVariants(request.totalQuantity(), request.variants().stream()
                .map(v -> new OrderDtos.VariantRequest(v.color(), v.size(), v.quantity())).toList());
        Buyer buyer = buyers.findById(request.buyerId())
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found: " + request.buyerId()));
        ProductionOrder.Status status;
        try { status = ProductionOrder.Status.valueOf(request.status()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid order status: " + request.status()); }

        LocalDate previousDate = order.getExFactoryDate();
        LocalDate newDate = request.exFactoryDate();

        order.update(buyer, request.styleName().trim(), request.productDescription(),
                request.totalQuantity(), newDate, status);
        order.replaceVariants(request.variants().stream()
                .map(v -> new OrderVariant(order, v.color().trim(), v.size().trim(), v.quantity())).toList());
        ProductionOrder saved = orders.save(order);

        if (!previousDate.equals(newDate)) {
            User user = authentication != null && authentication.getPrincipal() instanceof User u ? u : null;
            if (user != null) {
                auditService.recordDateChange(saved, previousDate, newDate, "Order updated by merchandiser", user);
            }
        }
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public OrderDtos.OrderResponse changeExFactoryDate(Long id, com.threadflow.order.audit.OrderAuditDtos.DateChangeRequest request,
                                                       Authentication authentication) {
        User merchandiser = requireMerchandiserUser(authentication);
        ProductionOrder order = orders.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        LocalDate previousDate = order.getExFactoryDate();
        LocalDate newDate = request.newExFactoryDate();

        if (previousDate.equals(newDate)) {
            throw new IllegalArgumentException("New ex-factory date must be different from current date");
        }

        order.updateExFactoryDate(newDate);
        ProductionOrder saved = orders.save(order);
        auditService.recordDateChange(saved, previousDate, newDate, request.reason().trim(), merchandiser);
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public OrderDtos.MilestoneResponse updateMilestone(Long orderId, Long milestoneId,
                                                        OrderDtos.MilestoneUpdateRequest request) {
        ProductionOrder order = orders.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderMilestone milestone = order.getMilestones().stream().filter(m -> m.getId().equals(milestoneId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));
        OrderMilestone.Status status;
        try { status = OrderMilestone.Status.valueOf(request.status()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid milestone status: " + request.status()); }
        if (request.actualStartDate() != null && request.actualCompletionDate() != null
                && request.actualCompletionDate().isBefore(request.actualStartDate())) {
            throw new IllegalArgumentException("Actual completion date must not be before actual start date");
        }
        milestone.update(status, request.actualStartDate(), request.actualCompletionDate(), request.notes());
        return toMilestoneResponse(milestone);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.MilestoneResponse> milestones(Long orderId, Authentication authentication) {
        requireOrderReadAccess(orderId, authentication);
        return orders.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId))
                .getMilestones().stream().map(this::toMilestoneResponse).toList();
    }

    private void requireMerchandiser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_MERCHANDISER"))) {
            throw new org.springframework.security.access.AccessDeniedException("Order access is restricted to merchandisers");
        }
    }

    private User requireMerchandiserUser(Authentication authentication) {
        requireMerchandiser(authentication);
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication != null && authentication.getName() != null && users != null) {
            return users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        }
        return null;
    }

    private boolean hasAuthority(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private void requireOrderReadAccess(Long orderId, Authentication authentication) {
        if (hasAuthority(authentication, "MERCHANDISER")) return;
        if (hasAuthority(authentication, "SUPERVISOR") && authentication.getPrincipal() instanceof User supervisor) {
            supervisorAccess.requireAccess(supervisor, orderId);
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("Order access is restricted to assigned supervisors and merchandisers");
    }

    private void validateVariants(Integer total, List<OrderDtos.VariantRequest> variants) {
        if (variants == null || variants.isEmpty()) throw new IllegalArgumentException("At least one variant is required");
        int sum = variants.stream().mapToInt(v -> {
            if (v.quantity() == null || v.quantity() <= 0) throw new IllegalArgumentException("Variant quantities must be positive");
            if (v.color() == null || v.color().isBlank() || v.size() == null || v.size().isBlank())
                throw new IllegalArgumentException("Variant color and size are required");
            return v.quantity();
        }).sum();
        if (sum != total) throw new IllegalArgumentException("Variant quantities must add up to total quantity");
    }

    private void generateMilestones(ProductionOrder order, MilestoneTemplate template) {
        int duration = template.getMilestones().stream().mapToInt(TemplateMilestone::getDefaultDurationDays).sum();
        LocalDate start = order.getExFactoryDate().minusDays(duration);
        for (TemplateMilestone item : template.getMilestones()) {
            LocalDate end = start.plusDays(item.getDefaultDurationDays());
            order.addMilestone(new OrderMilestone(order, item.getMilestoneName(), item.getSequenceNumber(), start, end));
            start = end;
        }
    }

    private OrderDtos.OrderResponse toResponse(ProductionOrder order) {
        return new OrderDtos.OrderResponse(order.getId(), order.getPoNumber(),
                new OrderDtos.BuyerSummary(order.getBuyer().getId(), order.getBuyer().getBuyerName(), order.getBuyer().getCountry()),
                order.getStyleName(), order.getProductDescription(), order.getTotalQuantity(),
                order.getExFactoryDate(), order.getStatus().name(),
                order.getVariants().stream().map(v -> new OrderDtos.VariantResponse(v.getId(), v.getColor(), v.getSize(), v.getQuantity())).toList(),
                order.getMilestones().stream().map(this::toMilestoneResponse).toList());
    }
    private OrderDtos.MilestoneResponse toMilestoneResponse(OrderMilestone m) {
        return new OrderDtos.MilestoneResponse(m.getId(), m.getMilestoneName(), m.getSequenceNumber(),
                m.getPlannedStartDate(), m.getPlannedEndDate(), m.getActualStartDate(),
                m.getActualCompletionDate(), m.getStatus().name(), m.getNotes());
    }
}
