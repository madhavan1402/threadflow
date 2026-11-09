package com.threadflow.issue;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionIssueReviewRepository extends JpaRepository<ProductionIssueReview, Long> {
    @EntityGraph(attributePaths = "reviewer")
    List<ProductionIssueReview> findByIssue_IdOrderByReviewedAtDesc(Long issueId);
}