package com.threadflow.order.audit;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDateAuditHistoryRepository extends JpaRepository<OrderDateAuditHistory, Long> {
    @EntityGraph(attributePaths = {"order", "changedBy"})
    List<OrderDateAuditHistory> findByOrder_IdOrderByChangedAtDesc(Long orderId);
}
