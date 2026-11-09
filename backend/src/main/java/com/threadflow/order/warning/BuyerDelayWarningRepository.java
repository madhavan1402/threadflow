package com.threadflow.order.warning;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerDelayWarningRepository extends JpaRepository<BuyerDelayWarning, Long> {
    @EntityGraph(attributePaths = {"order", "confirmedBy"})
    List<BuyerDelayWarning> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "confirmedBy"})
    List<BuyerDelayWarning> findByOrder_IdAndStatusOrderByConfirmedAtDesc(Long orderId, BuyerDelayWarningStatus status);

    @EntityGraph(attributePaths = {"order", "confirmedBy"})
    List<BuyerDelayWarning> findByOrder_Buyer_IdAndStatusOrderByConfirmedAtDesc(Long buyerId, BuyerDelayWarningStatus status);

    Optional<BuyerDelayWarning> findByIdAndOrder_Id(Long id, Long orderId);
}
