package com.gradproject.taskmanager.modules.git.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchNameParserTest {

    private BranchNameParser parser;

    @BeforeEach
    void setUp() {
        parser = new BranchNameParser();
    }

    

    @Test
    void extractTaskReference_shouldExtractFromFeatureBranch() {
        
        String branchName = "feature/PROJ-123-add-authentication";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123");
    }

    @Test
    void extractTaskReference_shouldExtractFromBugfixBranch() {
        
        String branchName = "bugfix/TASK-456-fix-login-error";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("TASK-456");
    }

    @Test
    void extractTaskReference_shouldExtractFromHotfixBranch() {
        
        String branchName = "hotfix/BUG-789-critical-security-fix";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("BUG-789");
    }

    @Test
    void extractTaskReference_shouldExtractFromReleaseBranch() {
        
        String branchName = "release/PROJ-100-version-2.0";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-100");
    }

    @Test
    void extractTaskReference_shouldExtractWithoutPrefix() {
        
        String branchName = "PROJ-123-simple-branch";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123");
    }

    @Test
    void extractTaskReference_shouldExtractWithSlashInDescription() {
        
        String branchName = "feature/PROJ-123-update/refactor";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123");
    }

    @Test
    void extractTaskReference_shouldExtractNumericReference() {
        
        String branchName = "feature/123-quick-fix";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("#123");
    }

    @Test
    void extractTaskReference_shouldReturnNullForInvalidBranch() {
        
        String branchName = "main";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isNull();
    }

    @Test
    void extractTaskReference_shouldReturnNullForNull() {
        
        String reference = parser.extractTaskReference(null);

        
        assertThat(reference).isNull();
    }

    @Test
    void extractTaskReference_shouldReturnNullForEmpty() {
        
        String reference = parser.extractTaskReference("");

        
        assertThat(reference).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "feature/PROJ-123-description, PROJ-123",
        "bugfix/TASK-456, TASK-456",
        "hotfix/BUG-789-fix, BUG-789",
        "PROJ-100-no-prefix, PROJ-100",
        "feature/123, #123",
        "bugfix/999-description, #999"
    })
    void extractTaskReference_shouldExtractCorrectly(String branchName, String expected) {
        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "main",
        "master",
        "develop",
        "staging",
        "no-task-reference",
        "feature/some-branch-without-task",
        "feature/proj-abc-lowercase"  
    })
    void extractTaskReference_shouldReturnNullForInvalidBranches(String branchName) {
        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isNull();
    }

    

    @Test
    void containsTaskReference_shouldReturnTrueWhenReferenceExists() {
        
        String branchName = "feature/PROJ-123-description";

        
        boolean contains = parser.containsTaskReference(branchName);

        
        assertThat(contains).isTrue();
    }

    @Test
    void containsTaskReference_shouldReturnFalseWhenNoReference() {
        
        String branchName = "main";

        
        boolean contains = parser.containsTaskReference(branchName);

        
        assertThat(contains).isFalse();
    }

    @Test
    void containsTaskReference_shouldReturnFalseForNull() {
        
        boolean contains = parser.containsTaskReference(null);

        
        assertThat(contains).isFalse();
    }

    

    @Test
    void generateBranchName_shouldGenerateFeatureBranch() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add user authentication", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-user-authentication");
    }

    @Test
    void generateBranchName_shouldGenerateBugfixBranch() {
        
        String branchName = parser.generateBranchName("TASK-456", "Fix login error", "bugfix");

        
        assertThat(branchName).isEqualTo("bugfix/TASK-456-fix-login-error");
    }

    @Test
    void generateBranchName_shouldDefaultToFeature() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Some description", null);

        
        assertThat(branchName).startsWith("feature/PROJ-123");
    }

    @Test
    void generateBranchName_shouldHandleEmptyDescription() {
        
        String branchName = parser.generateBranchName("PROJ-123", "", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123");
    }

    @Test
    void generateBranchName_shouldHandleNullDescription() {
        
        String branchName = parser.generateBranchName("PROJ-123", null, "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123");
    }

    @Test
    void generateBranchName_shouldSanitizeDescription() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add User! Authentication?", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-user-authentication");
    }

    @Test
    void generateBranchName_shouldConvertToLowercase() {
        
        String branchName = parser.generateBranchName("PROJ-123", "ADD USER AUTH", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-user-auth");
    }

    @Test
    void generateBranchName_shouldReplaceSpacesWithHyphens() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add    multiple   spaces", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-multiple-spaces");
    }

    @Test
    void generateBranchName_shouldRemoveMultipleConsecutiveHyphens() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add--user---auth", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-user-auth");
    }

    @Test
    void generateBranchName_shouldRemoveSpecialCharacters() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add @#$% special & chars!", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-special-chars");
    }

    @Test
    void generateBranchName_shouldLimitDescriptionLength() {
        
        String longDescription = "This is a very long description that should be truncated because it exceeds the maximum length allowed for branch names which is fifty characters";

        
        String branchName = parser.generateBranchName("PROJ-123", longDescription, "feature");

        
        
        
        assertThat(branchName).startsWith("feature/PROJ-123-");
        String description = branchName.substring("feature/PROJ-123-".length());
        assertThat(description.length()).isLessThanOrEqualTo(50);
    }

    @Test
    void generateBranchName_shouldThrowExceptionForEmptyTaskKey() {
        
        assertThatThrownBy(() -> parser.generateBranchName("", "description", "feature"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Task key cannot be empty");
    }

    @Test
    void generateBranchName_shouldThrowExceptionForNullTaskKey() {
        
        assertThatThrownBy(() -> parser.generateBranchName(null, "description", "feature"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Task key cannot be empty");
    }

    @ParameterizedTest
    @CsvSource({
        "feature, Add feature, feature/PROJ-123-add-feature",
        "bugfix, Fix bug, bugfix/PROJ-123-fix-bug",
        "hotfix, Quick fix, hotfix/PROJ-123-quick-fix",
        "release, Version 2.0, release/PROJ-123-version-20"
    })
    void generateBranchName_shouldGenerateCorrectBranchType(String type, String description, String expected) {
        
        String branchName = parser.generateBranchName("PROJ-123", description, type);

        
        assertThat(branchName).isEqualTo(expected);
    }

    

    @Test
    void isValidBranchName_shouldReturnTrueForFeatureBranch() {
        
        boolean valid = parser.isValidBranchName("feature/PROJ-123-description");

        
        assertThat(valid).isTrue();
    }

    @Test
    void isValidBranchName_shouldReturnTrueForBugfixBranch() {
        
        boolean valid = parser.isValidBranchName("bugfix/TASK-456-fix");

        
        assertThat(valid).isTrue();
    }

    @Test
    void isValidBranchName_shouldReturnTrueForMainBranch() {
        
        boolean valid = parser.isValidBranchName("main");

        
        assertThat(valid).isFalse();  
    }

    @Test
    void isValidBranchName_shouldReturnTrueForBranchWithTaskReference() {
        
        boolean valid = parser.isValidBranchName("PROJ-123-description");

        
        assertThat(valid).isTrue();
    }

    @Test
    void isValidBranchName_shouldReturnFalseForInvalidBranch() {
        
        boolean valid = parser.isValidBranchName("invalid-branch-name");

        
        assertThat(valid).isFalse();
    }

    @Test
    void isValidBranchName_shouldReturnFalseForNull() {
        
        boolean valid = parser.isValidBranchName(null);

        
        assertThat(valid).isFalse();
    }

    @Test
    void isValidBranchName_shouldReturnFalseForEmpty() {
        
        boolean valid = parser.isValidBranchName("");

        
        assertThat(valid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "feature/PROJ-123-description",
        "bugfix/TASK-456",
        "hotfix/BUG-789-fix",
        "release/PROJ-100",
        "feature/123-numeric",
        "PROJ-123-no-prefix"
    })
    void isValidBranchName_shouldReturnTrueForValidBranches(String branchName) {
        
        boolean valid = parser.isValidBranchName(branchName);

        
        assertThat(valid).isTrue();
    }

    

    @Test
    void extractTaskReference_shouldHandleBranchWithMultipleHyphens() {
        
        String branchName = "feature/PROJ-123-this-has-many-hyphens";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123");
    }

    @Test
    void extractTaskReference_shouldNotMatchBranchWithUnderscores() {
        
        String branchName = "feature/PROJ-123_description_with_underscores";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isNull(); 
    }

    @Test
    void extractTaskReference_shouldHandleShortProjectKey() {
        
        String branchName = "feature/AB-123-description";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("AB-123");
    }

    @Test
    void extractTaskReference_shouldHandleLongProjectKey() {
        
        String branchName = "feature/ABCDEFGHIJ-123-description";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("ABCDEFGHIJ-123");
    }

    @Test
    void extractTaskReference_shouldHandleProjectKeyWithNumbers() {
        
        String branchName = "feature/ABC123-456-description";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("ABC123-456");
    }

    @Test
    void generateBranchName_shouldHandleDescriptionWithNumbers() {
        
        String branchName = parser.generateBranchName("PROJ-123", "Add feature v2.0", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123-add-feature-v20");
    }

    @Test
    void generateBranchName_shouldHandleDescriptionWithOnlySpaces() {
        
        String branchName = parser.generateBranchName("PROJ-123", "     ", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123");
    }

    @Test
    void generateBranchName_shouldHandleDescriptionWithOnlySpecialChars() {
        
        String branchName = parser.generateBranchName("PROJ-123", "@#$%^&*()", "feature");

        
        assertThat(branchName).isEqualTo("feature/PROJ-123");
    }

    @Test
    void extractTaskReference_shouldHandleBranchEndingWithSlash() {
        
        String branchName = "feature/PROJ-123/";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123");
    }

    @Test
    void extractTaskReference_shouldExtractTaskKeyAtEnd() {
        
        String branchName = "feature/add-auth-PROJ-123";

        
        String reference = parser.extractTaskReference(branchName);

        
        assertThat(reference).isEqualTo("PROJ-123"); 
    }
}
