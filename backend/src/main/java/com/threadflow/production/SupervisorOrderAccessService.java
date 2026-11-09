package com.threadflow.production;

import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisorOrderAccessService {
    private final OrderRepository orders;
    private final SupervisorOrderAssignmentRepository orderAssignments;
    private final SupervisorUnitAssignmentRepository unitAssignments;

    public SupervisorOrderAccessService(OrderRepository orders,
                                        SupervisorOrderAssignmentRepository orderAssignments,
                                        SupervisorUnitAssignmentRepository unitAssignments) {
        this.orders = orders;
        this.orderAssignments = orderAssignments;
        this.unitAssignments = unitAssignments;
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long supervisorId, Long orderId) {
        if (orderAssignments.existsBySupervisorIdAndOrderIdAndActiveTrue(supervisorId, orderId)) return true;
        return orders.findById(orderId)
                .map(order -> order.getProductionUnit() != null && order.getProductionUnit().isActive()
                        && unitAssignments.existsBySupervisorIdAndProductionUnitIdAndActiveTrue(
                                supervisorId, order.getProductionUnit().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void requireAccess(User supervisor, Long orderId) {
        if (!hasAccess(supervisor.getId(), orderId)) {
            throw new AccessDeniedException("You are not assigned to this order");
        }
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleOrderIds(Long supervisorId) {
        return orders.findAccessibleOrderIds(supervisorId);
    }

    @Transactional(readOnly = true)
    public String assignmentSource(Long supervisorId, ProductionOrder order) {
        boolean direct = orderAssignments.existsBySupervisorIdAndOrderIdAndActiveTrue(supervisorId, order.getId());
        boolean unit = order.getProductionUnit() != null && order.getProductionUnit().isActive()
                && unitAssignments.existsBySupervisorIdAndProductionUnitIdAndActiveTrue(
                        supervisorId, order.getProductionUnit().getId());
        if (direct && unit) return "BOTH";
        return direct ? "DIRECT" : "UNIT";
    }
}