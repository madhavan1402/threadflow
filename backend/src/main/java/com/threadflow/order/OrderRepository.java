package com.threadflow.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<ProductionOrder, Long>, JpaSpecificationExecutor<ProductionOrder> {
    boolean existsByPoNumberIgnoreCase(String poNumber);
    @EntityGraph(attributePaths = {"buyer", "milestones"})
    Optional<ProductionOrder> findWithDetailsById(Long id);
}
