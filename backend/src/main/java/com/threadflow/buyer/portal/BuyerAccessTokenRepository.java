package com.threadflow.buyer.portal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerAccessTokenRepository extends JpaRepository<BuyerAccessToken, Long> {
    @EntityGraph(attributePaths = {"buyer", "createdBy"})
    Optional<BuyerAccessToken> findByTokenValue(String tokenValue);

    @EntityGraph(attributePaths = {"buyer", "createdBy"})
    List<BuyerAccessToken> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId);
}
