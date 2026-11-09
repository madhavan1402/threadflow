package com.threadflow.production;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupervisorOrderAssignmentRepository extends JpaRepository<SupervisorOrderAssignment, Long> {
    List<SupervisorOrderAssignment> findBySupervisorId(Long supervisorId);
    boolean existsBySupervisorIdAndOrderId(Long supervisorId, Long orderId);
    boolean existsBySupervisorIdAndOrderIdAndActiveTrue(Long supervisorId, Long orderId);
    List<SupervisorOrderAssignment> findByOrderIdOrderByAssignedAtDesc(Long orderId);
    List<SupervisorOrderAssignment> findByOrderIdAndActiveTrue(Long orderId);
    List<SupervisorOrderAssignment> findBySupervisorIdAndActiveTrue(Long supervisorId);
}
