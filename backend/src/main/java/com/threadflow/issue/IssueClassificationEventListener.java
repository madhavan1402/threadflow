package com.threadflow.issue;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IssueClassificationEventListener {
    private final IssueClassificationService service;

    public IssueClassificationEventListener(IssueClassificationService service) { this.service = service; }

    @Async("issueClassificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void classifyAfterCommit(IssueClassificationRequested event) {
        service.classify(event.issueId());
    }
}