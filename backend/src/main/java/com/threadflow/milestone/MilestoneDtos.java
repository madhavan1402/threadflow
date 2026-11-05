package com.threadflow.milestone;

import java.util.List;

public final class MilestoneDtos {
    private MilestoneDtos() {}
    public record TemplateResponse(Long id, String templateName, String description, boolean active,
                                   List<TemplateMilestoneResponse> milestones) {}
    public record TemplateMilestoneResponse(Long id, String milestoneName, Integer sequenceNumber,
                                            Integer defaultDurationDays) {}
}
