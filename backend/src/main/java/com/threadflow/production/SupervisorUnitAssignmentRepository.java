package com.threadflow.production;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupervisorUnitAssignmentRepository extends JpaRepository<SupervisorUnitAssignment, Long> {
    boolean existsBySupervisorIdAndProductionUnitIdAndActiveTrue(Long supervisorId, Long productionUnitId);
    boolean existsBySupervisorIdAndProductionUnitId(Long supervisorId, Long productionUnitId);
    List<SupervisorUnitAssignment> findByProductionUnitIdOrderByAssignedAtDesc(Long productionUnitId);
    List<SupervisorUnitAssignment> findByProductionUnitIdAndActiveTrue(Long productionUnitId);
    List<SupervisorUnitAssignment> findBySupervisorIdAndActiveTrue(Long supervisorId);
}