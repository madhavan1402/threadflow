package com.threadflow.production;

import com.threadflow.order.ProductionOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupervisorOrderAssignmentRepository extends JpaRepository<SupervisorOrderAssignment, SupervisorOrderAssignment.AssignmentId> {
    List<SupervisorOrderAssignment> findBySupervisorId(Long supervisorId);
    boolean existsBySupervisorIdAndOrderId(Long supervisorId, Long orderId);
}
