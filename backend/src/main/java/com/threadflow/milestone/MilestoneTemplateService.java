package com.threadflow.milestone;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilestoneTemplateService {
    private final MilestoneTemplateRepository repository;
    public MilestoneTemplateService(MilestoneTemplateRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<MilestoneDtos.TemplateResponse> list() {
        return repository.findAllByActiveTrue().stream().map(template -> new MilestoneDtos.TemplateResponse(
                template.getId(), template.getTemplateName(), template.getDescription(), template.isActive(),
                template.getMilestones().stream().map(item -> new MilestoneDtos.TemplateMilestoneResponse(
                        item.getId(), item.getMilestoneName(), item.getSequenceNumber(), item.getDefaultDurationDays()
                )).toList()
        )).toList();
    }
}
