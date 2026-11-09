package com.threadflow.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class IssueClassificationResponseParser {
    private final ObjectMapper objectMapper;

    public IssueClassificationResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IssueClassificationResult parse(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("AI response was empty");
        try {
            JsonNode root = objectMapper.readTree(json);
            String language = normalizeLanguage(requiredText(root, "detectedLanguage"));
            String translation = requiredText(root, "englishTranslation").trim();
            if (translation.length() > 5000) throw new IllegalArgumentException("English translation is too long");
            String categoryValue = requiredText(root, "category").trim().toUpperCase(Locale.ROOT);
            IssueCategory category;
            try {
                category = IssueCategory.valueOf(categoryValue);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("AI returned an unsupported issue category");
            }
            JsonNode confidenceNode = root.get("confidence");
            if (confidenceNode == null || !confidenceNode.isNumber()) {
                throw new IllegalArgumentException("AI confidence must be a JSON number");
            }
            BigDecimal confidence = confidenceNode.decimalValue();
            if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("AI confidence must be between 0 and 1");
            }
            return new IssueClassificationResult(language, translation, category, confidence);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AI response was not valid JSON");
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI response is missing " + field);
        }
        return value.asText();
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "ta", "tamil" -> "ta";
            case "en", "english" -> "en";
            case "mixed", "ta-en", "ta+en", "tamil-english", "tamil and english" -> "mixed";
            case "unknown" -> "unknown";
            default -> throw new IllegalArgumentException("AI returned an unsupported detected language");
        };
    }
}