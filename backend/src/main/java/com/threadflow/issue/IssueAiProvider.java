package com.threadflow.issue;

public interface IssueAiProvider {
    boolean isConfigured();
    String classify(String originalNote);
    String providerId();
    String modelId();
}