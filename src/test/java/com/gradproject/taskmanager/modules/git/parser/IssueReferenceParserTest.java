package com.gradproject.taskmanager.modules.git.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssueReferenceParserTest {

    private IssueReferenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new IssueReferenceParser();
    }

    

    @Test
    void extractReferences_shouldExtractNumericReference() {
        
        String text = "Fixed bug #123 in authentication module";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("#123");
    }

    @Test
    void extractReferences_shouldExtractProjectKeyReference() {
        
        String text = "Implemented PROJ-456 feature for dashboard";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("PROJ-456");
    }

    @Test
    void extractReferences_shouldExtractBracketedReference() {
        
        String text = "[TASK-789] Updated configuration files";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("TASK-789");
    }

    @Test
    void extractReferences_shouldExtractMultipleReferences() {
        
        String text = "Fixed PROJ-123 and PROJ-456, also closes #789";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("PROJ-123", "PROJ-456", "#789");
    }

    @Test
    void extractReferences_shouldHandleMixedFormats() {
        
        String text = "Resolves #123, TASK-456, and [BUG-789]";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("#123", "TASK-456", "BUG-789");
    }

    @Test
    void extractReferences_shouldIgnoreDuplicates() {
        
        String text = "PROJ-123 fixes PROJ-123 mentioned in PROJ-123";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).hasSize(1).containsExactly("PROJ-123");
    }

    @Test
    void extractReferences_shouldReturnEmptyListForNullText() {
        
        List<String> references = parser.extractReferences(null);

        
        assertThat(references).isEmpty();
    }

    @Test
    void extractReferences_shouldReturnEmptyListForEmptyText() {
        
        List<String> references = parser.extractReferences("");

        
        assertThat(references).isEmpty();
    }

    @Test
    void extractReferences_shouldReturnEmptyListWhenNoReferences() {
        
        String text = "This commit has no task references at all";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Fixed #1",
        "Closed #999999",
        "TASK-1 implementation",
        "ABC-42 feature",
        "PROJECTKEY-12345 done",
        "[FIX-1] bugfix",
        "[LONGKEY123-999] update"
    })
    void extractReferences_shouldExtractValidReferences(String text) {
        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "No references here",
        "Just some text",
        "Version 1.2.3 release",
        "PR-without-number",
        "lowercase-123",  
        "A-123"  
    })
    void extractReferences_shouldNotExtractInvalidReferences(String text) {
        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).isEmpty();
    }

    @Test
    void extractReferences_shouldHandleMultilineText() {
        
        String text = """
            PROJ-123 Initial implementation

            Related to TASK-456
            Closes #789
            """;

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("PROJ-123", "TASK-456", "#789");
    }

    @Test
    void extractReferences_shouldHandleReferencesWithSpecialCharacters() {
        
        String text = "Fixed PROJ-123, TASK-456. Closes #789!";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("PROJ-123", "TASK-456", "#789");
    }

    

    @Test
    void extractFirstReference_shouldReturnFirstReference() {
        
        String text = "Fixed PROJ-123 and TASK-456";

        
        String reference = parser.extractFirstReference(text);

        
        assertThat(reference).isIn("PROJ-123", "TASK-456");  
    }

    @Test
    void extractFirstReference_shouldReturnNullWhenNoReferences() {
        
        String text = "No references here";

        
        String reference = parser.extractFirstReference(text);

        
        assertThat(reference).isNull();
    }

    @Test
    void extractFirstReference_shouldReturnNullForNullText() {
        
        String reference = parser.extractFirstReference(null);

        
        assertThat(reference).isNull();
    }

    

    @Test
    void containsReference_shouldReturnTrueWhenNumericReferenceExists() {
        
        String text = "Fixed bug #123";

        
        boolean contains = parser.containsReference(text);

        
        assertThat(contains).isTrue();
    }

    @Test
    void containsReference_shouldReturnTrueWhenProjectKeyExists() {
        
        String text = "Implemented PROJ-456";

        
        boolean contains = parser.containsReference(text);

        
        assertThat(contains).isTrue();
    }

    @Test
    void containsReference_shouldReturnTrueWhenBracketedReferenceExists() {
        
        String text = "[TASK-789] Update";

        
        boolean contains = parser.containsReference(text);

        
        assertThat(contains).isTrue();
    }

    @Test
    void containsReference_shouldReturnFalseWhenNoReferences() {
        
        String text = "No task references here";

        
        boolean contains = parser.containsReference(text);

        
        assertThat(contains).isFalse();
    }

    @Test
    void containsReference_shouldReturnFalseForNullText() {
        
        boolean contains = parser.containsReference(null);

        
        assertThat(contains).isFalse();
    }

    @Test
    void containsReference_shouldReturnFalseForEmptyText() {
        
        boolean contains = parser.containsReference("");

        
        assertThat(contains).isFalse();
    }

    

    @Test
    void normalizeReference_shouldRemoveBrackets() {
        
        String normalized = parser.normalizeReference("[PROJ-123]");

        
        assertThat(normalized).isEqualTo("PROJ-123");
    }

    @Test
    void normalizeReference_shouldReturnProjectKeyAsIs() {
        
        String normalized = parser.normalizeReference("PROJ-123");

        
        assertThat(normalized).isEqualTo("PROJ-123");
    }

    @Test
    void normalizeReference_shouldReturnNumericReferenceAsIs() {
        
        String normalized = parser.normalizeReference("#123");

        
        assertThat(normalized).isEqualTo("#123");
    }

    @Test
    void normalizeReference_shouldReturnNullForInvalidFormat() {
        
        String normalized = parser.normalizeReference("invalid-format");

        
        assertThat(normalized).isNull();
    }

    @Test
    void normalizeReference_shouldReturnNullForNull() {
        
        String normalized = parser.normalizeReference(null);

        
        assertThat(normalized).isNull();
    }

    @Test
    void normalizeReference_shouldReturnNullForEmpty() {
        
        String normalized = parser.normalizeReference("");

        
        assertThat(normalized).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "PROJ-123, PROJ-123",
        "[PROJ-123], PROJ-123",
        "#123, #123",
        "TASK-1, TASK-1",
        "[LONGKEY123-999], LONGKEY123-999"
    })
    void normalizeReference_shouldNormalizeCorrectly(String input, String expected) {
        
        String normalized = parser.normalizeReference(input);

        
        assertThat(normalized).isEqualTo(expected);
    }

    

    @Test
    void extractReferences_shouldHandleVeryLongText() {
        
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("Some text ");
        }
        longText.append("PROJ-123");

        
        List<String> references = parser.extractReferences(longText.toString());

        
        assertThat(references).containsExactly("PROJ-123");
    }

    @Test
    void extractReferences_shouldHandleProjectKeyWithNumbers() {
        
        String text = "Fixed ABC123-456 and DEF789-999";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("ABC123-456", "DEF789-999");
    }

    @Test
    void extractReferences_shouldNotExtractLowercaseProjectKeys() {
        
        String text = "This proj-123 should not match";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).isEmpty();
    }

    @Test
    void extractReferences_shouldHandleReferencesAtTextBoundaries() {
        
        String text = "PROJ-123";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("PROJ-123");
    }

    @Test
    void extractReferences_shouldHandleConsecutiveReferences() {
        
        String text = "PROJ-123 TASK-456 BUG-789";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("PROJ-123", "TASK-456", "BUG-789");
    }

    @Test
    void extractReferences_shouldHandleReferencesInUrls() {
        
        String text = "See https://jira.example.com/browse/PROJ-123 for details";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("PROJ-123");
    }

    @Test
    void extractReferences_shouldHandleMaxProjectKeyLength() {
        
        String text = "Fixed ABCDEFGHIJ-123 issue";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactly("ABCDEFGHIJ-123");
    }

    @Test
    void extractReferences_shouldNotExtractTooLongProjectKeys() {
        
        
        String text = "This ABCDEFGHIJKL-123 is too long";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).isEmpty();
    }

    @Test
    void extractReferences_shouldHandleZeroPaddedNumbers() {
        
        String text = "Task #0001 and #0042 completed";

        
        List<String> references = parser.extractReferences(text);

        
        assertThat(references).containsExactlyInAnyOrder("#0001", "#0042");
    }
}
