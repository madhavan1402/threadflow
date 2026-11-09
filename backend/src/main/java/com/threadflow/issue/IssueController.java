package com.threadflow.issue;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/issues")
@PreAuthorize("isAuthenticated()")
public class IssueController {
    private final IssueClassificationService service;

    public IssueController(IssueClassificationService service) { this.service = service; }

    @GetMapping("/pending-review")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public List<IssueDtos.ClassificationResponse> pendingReview(Authentication authentication) {
        return service.pendingReview(authentication);
    }

    @GetMapping("/{issueId}/classification")
    public IssueDtos.ClassificationResponse classification(@PathVariable Long issueId, Authentication authentication) {
        return service.classification(issueId, authentication);
    }

    @PutMapping("/{issueId}/review")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public IssueDtos.ClassificationResponse review(@PathVariable Long issueId,
            @Valid @RequestBody IssueDtos.ReviewRequest request, Authentication authentication) {
        return service.review(issueId, request, authentication);
    }

    @PostMapping("/{issueId}/reclassify")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public IssueDtos.ClassificationResponse reclassify(@PathVariable Long issueId, Authentication authentication) {
        return service.reclassify(issueId, authentication);
    }
}