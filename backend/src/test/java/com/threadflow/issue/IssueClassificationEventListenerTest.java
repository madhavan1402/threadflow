package com.threadflow.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class IssueClassificationEventListenerTest {

    @Test
    void classifyAfterCommitInvokesServiceClassify() {
        IssueClassificationService service = mock(IssueClassificationService.class);
        IssueClassificationEventListener listener = new IssueClassificationEventListener(service);

        listener.classifyAfterCommit(new IssueClassificationRequested(42L));

        verify(service).classify(42L);
    }

    @Test
    void verifiesTransactionalEventListenerAnnotations() throws NoSuchMethodException {
        Method method = IssueClassificationEventListener.class.getMethod("classifyAfterCommit", IssueClassificationRequested.class);

        TransactionalEventListener txListener = method.getAnnotation(TransactionalEventListener.class);
        assertNotNull(txListener, "Must have @TransactionalEventListener");
        assertEquals(TransactionPhase.AFTER_COMMIT, txListener.phase(), "Must execute AFTER_COMMIT");

        Async async = method.getAnnotation(Async.class);
        assertNotNull(async, "Must have @Async");
        assertEquals("issueClassificationExecutor", async.value(), "Must use issueClassificationExecutor");
    }
}
