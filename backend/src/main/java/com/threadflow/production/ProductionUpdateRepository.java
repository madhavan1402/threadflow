package com.threadflow.production;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ProductionUpdateRepository extends JpaRepository<ProductionUpdate, Long> {
    Optional<ProductionUpdate> findByIdempotencyKey(String idempotencyKey);
    List<ProductionUpdate> findBySupervisorIdOrderByProductionDateDescCreatedAtDesc(Long supervisorId);
    List<ProductionUpdate> findByOrderIdOrderByProductionDateDescCreatedAtDesc(Long orderId);
    List<ProductionUpdate> findByOrderIdIn(Collection<Long> orderIds);
    List<ProductionUpdate> findByMilestoneIdOrderByProductionDateDescCreatedAtDesc(Long milestoneId);
    @Query("select coalesce(sum(u.completedQuantity), 0) from ProductionUpdate u where u.order.id = :orderId")
    long sumCompletedQuantityForOrder(Long orderId);
    @Query("select coalesce(sum(u.completedQuantity), 0) from ProductionUpdate u where u.order.id = :orderId and u.milestone.id = :milestoneId")
    long sumCompletedQuantityForOrderAndMilestone(@Param("orderId") Long orderId,
                                                   @Param("milestoneId") Long milestoneId);
}
