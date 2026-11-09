package com.threadflow.order;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<ProductionOrder, Long>, JpaSpecificationExecutor<ProductionOrder> {
    boolean existsByPoNumberIgnoreCase(String poNumber);
    Optional<ProductionOrder> findByPoNumberIgnoreCase(String poNumber);
    @EntityGraph(attributePaths = {"buyer", "milestones", "productionUnit"})
    Optional<ProductionOrder> findWithDetailsById(Long id);
    @EntityGraph(attributePaths = {"buyer", "milestones", "productionUnit"})
    List<ProductionOrder> findDistinctByIdIn(Collection<Long> ids);
    List<ProductionOrder> findByStatusNot(ProductionOrder.Status status);
    @Query("""
            select distinct o.id from ProductionOrder o
            where exists (select a.id from SupervisorOrderAssignment a
                          where a.order = o and a.supervisor.id = :supervisorId and a.active = true)
               or exists (select a.id from SupervisorUnitAssignment a
                          where a.productionUnit = o.productionUnit and a.supervisor.id = :supervisorId
                            and a.active = true and a.productionUnit.active = true)
            """)
    List<Long> findAccessibleOrderIds(@Param("supervisorId") Long supervisorId);
}
