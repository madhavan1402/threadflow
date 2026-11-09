package com.threadflow.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IssueClassificationResponseParserTest {
    private IssueClassificationResponseParser parser;

    @BeforeEach
    void setUp() { parser = new IssueClassificationResponseParser(new ObjectMapper()); }

    @Test
    void parsesTamilIssueResponse() {
        IssueClassificationResult result = parser.parse("""
                {"detectedLanguage":"ta","englishTranslation":"The knitting machine stopped.",
                 "category":"MACHINE_BREAKDOWN","confidence":0.94}
                """);
        assertEquals("ta", result.detectedLanguage());
        assertEquals(IssueCategory.MACHINE_BREAKDOWN, result.category());
        assertEquals(new BigDecimal("0.94"), result.confidence());
    }

    @Test
    void parsesEnglishAndMixedLanguageLabels() {
        assertEquals("en", parser.parse(response("English", "MATERIAL_SHORTAGE", "0.7")).detectedLanguage());
        assertEquals("mixed", parser.parse(response("Tamil-English", "QUALITY_ISSUE", "0.7")).detectedLanguage());
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not-json"));
    }

    @Test
    void rejectsUnsupportedCategories() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(response("ta", "BUYER_DELAY_WARNING", "0.8")));
    }

    @Test
    void rejectsConfidenceOutsideZeroToOneAndNonNumericConfidence() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(response("ta", "OTHER", "1.01")));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(response("ta", "OTHER", "-0.01")));
        assertThrows(IllegalArgumentException.class,
            () -> parser.parse("{\"detectedLanguage\":\"ta\",\"englishTranslation\":\"x\","
                + "\"category\":\"OTHER\",\"confidence\":\"high\"}"));
    }

    @Test
    void rejectsEmptyOrBlankJson() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }

    @Test
    void rejectsMissingFields() {
        // Missing detectedLanguage
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"englishTranslation\":\"Shortage\",\"category\":\"OTHER\",\"confidence\":0.8}"));
        // Missing englishTranslation
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"detectedLanguage\":\"ta\",\"category\":\"OTHER\",\"confidence\":0.8}"));
        // Missing category
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"detectedLanguage\":\"ta\",\"englishTranslation\":\"Shortage\",\"confidence\":0.8}"));
        // Missing confidence
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"detectedLanguage\":\"ta\",\"englishTranslation\":\"Shortage\",\"category\":\"OTHER\"}"));
    }

    @Test
    void rejectsExcessivelyLongEnglishTranslation() {
        String longText = "a".repeat(5001);
        String json = "{\"detectedLanguage\":\"ta\",\"englishTranslation\":\"" + longText + "\",\"category\":\"OTHER\",\"confidence\":0.5}";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(json));
    }

    private String response(String language, String category, String confidence) {
        return "{\"detectedLanguage\":\"" + language + "\",\"englishTranslation\":\"Shortage noted\","
                + "\"category\":\"" + category + "\",\"confidence\":" + confidence + "}";
    }
}