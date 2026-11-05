package com.threadflow.milestone;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneTemplateRepository extends JpaRepository<MilestoneTemplate, Long> {
    List<MilestoneTemplate> findAllByActiveTrue();
    Optional<MilestoneTemplate> findByTemplateNameIgnoreCase(String templateName);
}
