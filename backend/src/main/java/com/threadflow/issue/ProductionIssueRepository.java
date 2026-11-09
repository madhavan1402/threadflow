package com.threadflow.issue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionIssueRepository extends JpaRepository<ProductionIssue, Long> {
    @EntityGraph(attributePaths = {"productionUpdate", "productionUpdate.order", "productionUpdate.supervisor", "reviewer"})
    Optional<ProductionIssue> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"productionUpdate", "productionUpdate.order", "productionUpdate.supervisor", "reviewer"})
    List<ProductionIssue> findByStatusInOrderByCreatedAtAsc(Collection<IssueClassificationStatus> statuses);
}