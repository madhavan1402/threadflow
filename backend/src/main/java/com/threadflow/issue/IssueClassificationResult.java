package com.threadflow.issue;

import java.math.BigDecimal;

public record IssueClassificationResult(
        String detectedLanguage,
        String englishTranslation,
        IssueCategory category,
        BigDecimal confidence
) {}
