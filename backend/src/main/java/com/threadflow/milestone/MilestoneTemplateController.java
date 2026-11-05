package com.threadflow.milestone;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/milestone-templates")
public class MilestoneTemplateController {
    private final MilestoneTemplateService service;
    public MilestoneTemplateController(MilestoneTemplateService service) { this.service = service; }
    @GetMapping public List<MilestoneDtos.TemplateResponse> list() { return service.list(); }
}
