package com.threadflow.production;

import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionUnitService {
    private final ProductionUnitRepository units;
    private final SupervisorUnitAssignmentRepository unitAssignments;
    private final SupervisorOrderAssignmentRepository orderAssignments;
    private final OrderRepository orders;
    private final UserRepository users;

    public ProductionUnitService(ProductionUnitRepository units,
                                 SupervisorUnitAssignmentRepository unitAssignments,
                                 SupervisorOrderAssignmentRepository orderAssignments,
                                 OrderRepository orders, UserRepository users) {
        this.units = units;
        this.unitAssignments = unitAssignments;
        this.orderAssignments = orderAssignments;
        this.orders = orders;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AssignmentDtos.ProductionUnitResponse> listUnits(Authentication authentication) {
        requireMerchandiser(authentication);
        return units.findAllByOrderByNameAsc().stream().map(this::toUnit).toList();
    }

    @Transactional
    public AssignmentDtos.ProductionUnitResponse createUnit(AssignmentDtos.ProductionUnitRequest request,
                                                             Authentication authentication) {
        requireMerchandiser(authentication);
        validateUnitNameAndCode(request.name(), request.code(), null);
        ProductionUnit unit = new ProductionUnit(request.name().trim(), request.code().trim().toUpperCase(),
                request.description(), request.active());
        return toUnit(units.save(unit));
    }

    @Transactional
    public AssignmentDtos.ProductionUnitResponse updateUnit(Long unitId, AssignmentDtos.ProductionUnitRequest request,
                                                             Authentication authentication) {
        requireMerchandiser(authentication);
        ProductionUnit unit = units.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Production unit not found: " + unitId));
        validateUnitNameAndCode(request.name(), request.code(), unitId);
        unit.update(request.name().trim(), request.code().trim().toUpperCase(), request.description(), request.active());
        return toUnit(units.save(unit));
    }

    @Transactional(readOnly = true)
    public List<AssignmentDtos.SupervisorResponse> availableSupervisors(Authentication authentication) {
        requireMerchandiser(authentication);
        return users.findAll().stream().filter(User::isEnabled).filter(user -> user.hasRole("SUPERVISOR"))
                .sorted((first, second) -> first.getEmail().compareToIgnoreCase(second.getEmail()))
                .map(user -> new AssignmentDtos.SupervisorResponse(user.getId(), user.getEmail(), user.isEnabled())).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDtos.UnitSupervisorsResponse unitSupervisors(Long unitId, Authentication authentication) {
        requireMerchandiser(authentication);
        requireUnit(unitId);
        return new AssignmentDtos.UnitSupervisorsResponse(unitId, unitAssignments
                .findByProductionUnitIdOrderByAssignedAtDesc(unitId).stream().map(this::toHistory).toList());
    }

    @Transactional
    public AssignmentDtos.UnitSupervisorsResponse setUnitSupervisors(Long unitId,
            AssignmentDtos.SupervisorIdsRequest request, Authentication authentication) {
        User actor = requireMerchandiserUser(authentication);
        ProductionUnit unit = requireUnit(unitId);
        if (!unit.isActive() && !request.supervisorIds().isEmpty()) {
            throw new IllegalArgumentException("Supervisors cannot be assigned to an inactive production unit");
        }
        List<SupervisorUnitAssignment> current = unitAssignments.findByProductionUnitIdAndActiveTrue(unitId);
        Set<Long> currentIds = new HashSet<>();
        current.forEach(assignment -> currentIds.add(assignment.getSupervisor().getId()));
        List<User> requested = validatedSupervisors(request.supervisorIds(), currentIds);
        Set<Long> desiredIds = new HashSet<>(request.supervisorIds());
        current.stream().filter(assignment -> !desiredIds.contains(assignment.getSupervisor().getId()))
                .forEach(assignment -> assignment.revoke(Instant.now()));
        requested.stream().filter(user -> !currentIds.contains(user.getId()))
                .forEach(user -> unitAssignments.save(new SupervisorUnitAssignment(user, unit, actor)));
        unitAssignments.saveAll(current);
        return unitSupervisors(unitId, authentication);
    }

    @Transactional(readOnly = true)
    public AssignmentDtos.OrderAssignmentsResponse orderAssignments(Long orderId, Authentication authentication) {
        requireMerchandiser(authentication);
        ProductionOrder order = requireOrder(orderId);
        return orderAssignmentResponse(order);
    }

    @Transactional
    public AssignmentDtos.OrderAssignmentsResponse setOrderProductionUnit(Long orderId, Long unitId,
                                                                           Authentication authentication) {
        requireMerchandiser(authentication);
        ProductionOrder order = requireOrder(orderId);
        ProductionUnit unit = unitId == null ? null : requireUnit(unitId);
        if (unit != null && !unit.isActive()) throw new IllegalArgumentException("Production unit is inactive");
        order.assignProductionUnit(unit);
        orders.save(order);
        return orderAssignmentResponse(order);
    }

    @Transactional
    public AssignmentDtos.OrderAssignmentsResponse setOrderSupervisors(Long orderId,
            AssignmentDtos.SupervisorIdsRequest request, Authentication authentication) {
        User actor = requireMerchandiserUser(authentication);
        ProductionOrder order = requireOrder(orderId);
        List<SupervisorOrderAssignment> current = orderAssignments.findByOrderIdAndActiveTrue(orderId);
        Set<Long> currentIds = new HashSet<>();
        current.forEach(assignment -> currentIds.add(assignment.getSupervisor().getId()));
        List<User> requested = validatedSupervisors(request.supervisorIds(), currentIds);
        Set<Long> desiredIds = new HashSet<>(request.supervisorIds());
        current.stream().filter(assignment -> !desiredIds.contains(assignment.getSupervisor().getId()))
                .forEach(assignment -> assignment.revoke(Instant.now()));
        requested.stream().filter(user -> !currentIds.contains(user.getId()))
                .forEach(user -> orderAssignments.save(new SupervisorOrderAssignment(user, order, actor)));
        orderAssignments.saveAll(current);
        return orderAssignmentResponse(order);
    }

    private List<User> validatedSupervisors(List<Long> supervisorIds, Set<Long> currentIds) {
        if (new HashSet<>(supervisorIds).size() != supervisorIds.size()) {
            throw new IllegalArgumentException("Duplicate supervisor IDs are not allowed");
        }
        List<User> requested = users.findAllById(supervisorIds);
        if (requested.size() != supervisorIds.size() || requested.stream()
            .anyMatch(user -> !user.hasRole("SUPERVISOR") || (!user.isEnabled() && !currentIds.contains(user.getId())))) {
            throw new IllegalArgumentException("Only active supervisor accounts can be assigned");
        }
        return requested;
    }

    private void validateUnitNameAndCode(String name, String code, Long currentId) {
        String normalizedCode = code.trim().toUpperCase();
        units.findAllByOrderByNameAsc().stream().filter(unit -> !unit.getId().equals(currentId))
                .filter(unit -> unit.getName().equalsIgnoreCase(name.trim())
                        || unit.getCode().equalsIgnoreCase(normalizedCode))
                .findFirst().ifPresent(unit -> {
                    throw new IllegalArgumentException("Production unit name and code must be unique");
                });
    }

    private ProductionUnit requireUnit(Long unitId) {
        return units.findById(unitId).orElseThrow(() -> new IllegalArgumentException("Production unit not found: " + unitId));
    }

    private ProductionOrder requireOrder(Long orderId) {
        return orders.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private AssignmentDtos.OrderAssignmentsResponse orderAssignmentResponse(ProductionOrder order) {
        return new AssignmentDtos.OrderAssignmentsResponse(order.getId(),
                order.getProductionUnit() == null ? null : toUnit(order.getProductionUnit()),
                orderAssignments.findByOrderIdOrderByAssignedAtDesc(order.getId()).stream()
                        .map(this::toHistory).toList());
    }

    private AssignmentDtos.AssignmentHistoryResponse toHistory(SupervisorOrderAssignment assignment) {
        return new AssignmentDtos.AssignmentHistoryResponse(assignment.getId(), assignment.getSupervisor().getId(),
                assignment.getSupervisor().getEmail(), assignment.getAssignedBy() == null ? null : assignment.getAssignedBy().getId(),
                assignment.getAssignedBy() == null ? null : assignment.getAssignedBy().getEmail(),
                assignment.getAssignedAt(), assignment.getRevokedAt(), assignment.isActive());
    }

    private AssignmentDtos.AssignmentHistoryResponse toHistory(SupervisorUnitAssignment assignment) {
        return new AssignmentDtos.AssignmentHistoryResponse(assignment.getId(), assignment.getSupervisor().getId(),
                assignment.getSupervisor().getEmail(), assignment.getAssignedBy().getId(), assignment.getAssignedBy().getEmail(),
                assignment.getAssignedAt(), assignment.getRevokedAt(), assignment.isActive());
    }

    private AssignmentDtos.ProductionUnitResponse toUnit(ProductionUnit unit) {
        return new AssignmentDtos.ProductionUnitResponse(unit.getId(), unit.getName(), unit.getCode(),
                unit.getDescription(), unit.isActive());
    }

    private User requireMerchandiserUser(Authentication authentication) {
        requireMerchandiser(authentication);
        if (authentication.getPrincipal() instanceof User user) return user;
        throw new AccessDeniedException("Authenticated merchandiser account required");
    }

    private void requireMerchandiser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_MERCHANDISER"))) {
            throw new AccessDeniedException("Merchandiser role required");
        }
    }
}