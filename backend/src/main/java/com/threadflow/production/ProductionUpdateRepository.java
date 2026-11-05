package com.threadflow.production;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductionUpdateRepository extends JpaRepository<ProductionUpdate, Long> {
    Optional<ProductionUpdate> findByIdempotencyKey(String idempotencyKey);
    List<ProductionUpdate> findBySupervisorIdOrderByProductionDateDescCreatedAtDesc(Long supervisorId);
    List<ProductionUpdate> findByOrderIdOrderByProductionDateDescCreatedAtDesc(Long orderId);
    List<ProductionUpdate> findByMilestoneIdOrderByProductionDateDescCreatedAtDesc(Long milestoneId);
    @Query("select coalesce(sum(u.completedQuantity), 0) from ProductionUpdate u where u.order.id = :orderId")
    long sumCompletedQuantityForOrder(Long orderId);
}
