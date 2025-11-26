package com.gradproject.taskmanager.modules.git.parser;

import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import com.gradproject.taskmanager.modules.git.parser.SmartCommitParser.SmartCommitCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmartCommitParserTest {

    private SmartCommitParser parser;

    @BeforeEach
    void setUp() {
        parser = new SmartCommitParser();
    }

    

    @Test
    void parseCommands_shouldParseCloseCommand() {
        
        String commitMessage = "PROJ-123 #close Fixed the authentication bug";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Done");
    }

    @ParameterizedTest
    @ValueSource(strings = {"#close", "#closed", "#done", "#complete", "#completed"})
    void parseCommands_shouldParseAllClosingCommands(String command) {
        
        String commitMessage = "PROJ-123 " + command + " Fixed";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Done");
    }

    @Test
    void parseCommands_shouldParseInProgressCommand() {
        
        String commitMessage = "PROJ-123 #in-progress Starting work on this task";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("In Progress");
    }

    @Test
    void parseCommands_shouldParseReviewCommand() {
        
        String commitMessage = "PROJ-123 #review Ready for code review";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Code Review");
    }

    @Test
    void parseCommands_shouldParseTodoCommand() {
        
        String commitMessage = "PROJ-123 #to-do Moved back to backlog";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("To Do");
    }

    @Test
    void parseCommands_shouldParseBlockedCommand() {
        
        String commitMessage = "PROJ-123 #blocked Waiting for API access";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Blocked");
    }

    

    @Test
    void parseCommands_shouldParseCommentCommand() {
        
        String commitMessage = "PROJ-123 #comment Added validation logic for user input";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(0).getValue()).isEqualTo("Added validation logic for user input");
    }

    @Test
    void parseCommands_shouldParseMultipleWordsInComment() {
        
        String commitMessage = "PROJ-123 #comment This is a longer comment with multiple words and sentences.";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(0).getValue()).isEqualTo("This is a longer comment with multiple words and sentences.");
    }

    @Test
    void parseCommands_shouldStopCommentAtNextCommand() {
        
        String commitMessage = "PROJ-123 #comment First comment #close";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(2);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Done");
        assertThat(commands.get(1).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(1).getValue()).isEqualTo("First comment");
    }

    

    @Test
    void parseCommands_shouldParseTimeCommand() {
        
        String commitMessage = "PROJ-123 #time 2h 30m";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TIME);
        assertThat(commands.get(0).getValue()).isEqualTo("2h 30m");
    }

    @Test
    void parseCommands_shouldParseTimeWithWeeksAndDays() {
        
        String commitMessage = "PROJ-123 #time 1w 2d 4h 30m";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("1w 2d 4h 30m");
    }

    @Test
    void parseCommands_shouldParseTimeWithOnlyHours() {
        
        String commitMessage = "PROJ-123 #time 5h";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("5h");
    }

    

    @Test
    void parseCommands_shouldParseAssignCommand() {
        
        String commitMessage = "PROJ-123 #assign @johndoe";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.ASSIGN);
        assertThat(commands.get(0).getValue()).isEqualTo("johndoe");
    }

    @Test
    void parseCommands_shouldParseAssignWithoutAtSign() {
        
        String commitMessage = "PROJ-123 #assign johndoe";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("johndoe");
    }

    

    @Test
    void parseCommands_shouldParseLabelCommand() {
        
        String commitMessage = "PROJ-123 #label bug";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.LABEL);
        assertThat(commands.get(0).getValue()).isEqualTo("bug");
    }

    

    @Test
    void parseCommands_shouldParseMultipleCommands() {
        
        String commitMessage = "PROJ-123 #comment Fixed bug #time 2h #close";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(1).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(2).getType()).isEqualTo(SmartCommitCommandType.TIME);
    }

    @Test
    void parseCommands_shouldParseAllCommandTypes() {
        
        String commitMessage = "PROJ-123 #comment Fixed #time 1h #assign @jane #label bug #close";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(5);
        assertThat(commands).extracting(SmartCommitCommand::getType)
            .containsExactly(
                SmartCommitCommandType.TRANSITION,
                SmartCommitCommandType.COMMENT,
                SmartCommitCommandType.TIME,
                SmartCommitCommandType.ASSIGN,
                SmartCommitCommandType.LABEL
            );
    }

    

    @Test
    void parseCommands_shouldReturnEmptyListForNull() {
        
        List<SmartCommitCommand> commands = parser.parseCommands(null);

        
        assertThat(commands).isEmpty();
    }

    @Test
    void parseCommands_shouldReturnEmptyListForEmpty() {
        
        List<SmartCommitCommand> commands = parser.parseCommands("");

        
        assertThat(commands).isEmpty();
    }

    @Test
    void parseCommands_shouldReturnEmptyListWhenNoCommands() {
        
        String commitMessage = "PROJ-123 Regular commit message without smart commands";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).isEmpty();
    }

    @Test
    void parseCommands_shouldBeCaseInsensitive() {
        
        String commitMessage = "PROJ-123 #CLOSE #Comment Test #TIME 1h";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(3);
    }

    @Test
    void parseCommands_shouldHandleMultilineCommitMessage() {
        
        String commitMessage = """
            PROJ-123 Fixed authentication bug

            #comment Added proper validation #time 2h
            #close
            """;

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(1).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(2).getType()).isEqualTo(SmartCommitCommandType.TIME);
    }

    

    @Test
    void containsSmartCommands_shouldReturnTrueWhenCommandsExist() {
        
        String commitMessage = "PROJ-123 #close Fixed";

        
        boolean contains = parser.containsSmartCommands(commitMessage);

        
        assertThat(contains).isTrue();
    }

    @Test
    void containsSmartCommands_shouldReturnFalseWhenNoCommands() {
        
        String commitMessage = "PROJ-123 Regular commit";

        
        boolean contains = parser.containsSmartCommands(commitMessage);

        
        assertThat(contains).isFalse();
    }

    @Test
    void containsSmartCommands_shouldReturnFalseForNull() {
        
        boolean contains = parser.containsSmartCommands(null);

        
        assertThat(contains).isFalse();
    }

    @Test
    void containsSmartCommands_shouldReturnFalseForEmpty() {
        
        boolean contains = parser.containsSmartCommands("");

        
        assertThat(contains).isFalse();
    }

    

    @Test
    void parseTimeToMinutes_shouldParseHours() {
        
        int minutes = parser.parseTimeToMinutes("2h");

        
        assertThat(minutes).isEqualTo(120);
    }

    @Test
    void parseTimeToMinutes_shouldParseMinutes() {
        
        int minutes = parser.parseTimeToMinutes("30m");

        
        assertThat(minutes).isEqualTo(30);
    }

    @Test
    void parseTimeToMinutes_shouldParseHoursAndMinutes() {
        
        int minutes = parser.parseTimeToMinutes("2h 30m");

        
        assertThat(minutes).isEqualTo(150);
    }

    @Test
    void parseTimeToMinutes_shouldParseDays() {
        
        int minutes = parser.parseTimeToMinutes("1d");

        
        assertThat(minutes).isEqualTo(480);  
    }

    @Test
    void parseTimeToMinutes_shouldParseWeeks() {
        
        int minutes = parser.parseTimeToMinutes("1w");

        
        assertThat(minutes).isEqualTo(2400);  
    }

    @Test
    void parseTimeToMinutes_shouldParseComplexTime() {
        
        int minutes = parser.parseTimeToMinutes("1w 2d 3h 15m");

        
        assertThat(minutes).isEqualTo(2400 + 960 + 180 + 15);  
    }

    @Test
    void parseTimeToMinutes_shouldReturnZeroForNull() {
        
        int minutes = parser.parseTimeToMinutes(null);

        
        assertThat(minutes).isEqualTo(0);
    }

    @Test
    void parseTimeToMinutes_shouldReturnZeroForEmpty() {
        
        int minutes = parser.parseTimeToMinutes("");

        
        assertThat(minutes).isEqualTo(0);
    }

    @Test
    void parseTimeToMinutes_shouldReturnZeroForInvalidFormat() {
        
        int minutes = parser.parseTimeToMinutes("invalid");

        
        assertThat(minutes).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({
        "1h, 60",
        "2h, 120",
        "30m, 30",
        "1h 30m, 90",
        "2h 45m, 165",
        "1d, 480",
        "1d 2h, 600",
        "1w, 2400",
        "1w 1d, 2880"
    })
    void parseTimeToMinutes_shouldParseCorrectly(String timeValue, int expectedMinutes) {
        
        int minutes = parser.parseTimeToMinutes(timeValue);

        
        assertThat(minutes).isEqualTo(expectedMinutes);
    }

    

    @Test
    void smartCommitCommand_shouldStoreCorrectData() {
        
        SmartCommitCommand command = new SmartCommitCommand(
            SmartCommitCommandType.TRANSITION,
            "Done",
            "Fixed the bug",
            "#close Fixed the bug"
        );

        
        assertThat(command.getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(command.getValue()).isEqualTo("Done");
        assertThat(command.getAdditionalText()).isEqualTo("Fixed the bug");
        assertThat(command.getOriginalText()).isEqualTo("#close Fixed the bug");
    }

    @Test
    void smartCommitCommand_shouldHaveToString() {
        
        SmartCommitCommand command = new SmartCommitCommand(
            SmartCommitCommandType.COMMENT,
            "Test comment",
            null,
            "#comment Test comment"
        );

        
        String toString = command.toString();

        
        assertThat(toString).contains("COMMENT");
        assertThat(toString).contains("Test comment");
        assertThat(toString).contains("#comment Test comment");
    }

    

    @Test
    void parseCommands_shouldHandleCommandsWithExtraSpaces() {
        
        String commitMessage = "PROJ-123 #close    Fixed   with   spaces";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
    }

    @Test
    void parseCommands_shouldHandleCommandAtStartOfMessage() {
        
        String commitMessage = "#close PROJ-123";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
    }

    @Test
    void parseCommands_shouldHandleCommandAtEndOfMessage() {
        
        String commitMessage = "PROJ-123 Fixed bug #close";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).hasSize(1);
    }

    @Test
    void parseCommands_shouldNotParsePartialMatches() {
        
        String commitMessage = "PROJ-123 Closed the issue (no command)";

        
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        
        assertThat(commands).isEmpty();  
    }

    @Test
    void parseTimeToMinutes_shouldHandleTimeWithoutSpaces() {
        
        int minutes = parser.parseTimeToMinutes("2h30m");

        
        assertThat(minutes).isEqualTo(150);
    }

    @Test
    void parseTimeToMinutes_shouldHandleTimeWithMultipleSpaces() {

        int minutes = parser.parseTimeToMinutes("2h    30m");


        assertThat(minutes).isEqualTo(150);
    }

    // ==================== Custom Status Tests ====================

    @Test
    void parseCommands_shouldParseCustomSingleWordStatus() {
        // Given - a custom status "Testing" that exists in the project
        String commitMessage = "PROJ-123 #testing Ready for QA";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Testing");
    }

    @Test
    void parseCommands_shouldParseMultiWordKebabCaseStatus() {
        // Given - a multi-word status "Pull Request Waiting"
        String commitMessage = "PROJ-123 #pull-request-waiting Awaiting review";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Pull Request Waiting");
    }

    @Test
    void parseCommands_shouldParseCodeReviewAsKebabCase() {
        // Given - using kebab-case instead of the alias
        String commitMessage = "PROJ-123 #code-review";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo("Code Review");
    }

    @Test
    void parseCommands_shouldParseQaVerification() {
        // Given - a custom multi-word status
        String commitMessage = "PROJ-123 #qa-verification";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("Qa Verification");
    }

    @Test
    void parseCommands_shouldParseAwaitingDeployment() {
        // Given - a three-word kebab-case status
        String commitMessage = "PROJ-123 #awaiting-prod-deployment";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("Awaiting Prod Deployment");
    }

    @ParameterizedTest
    @CsvSource({
        "#testing, Testing",
        "#qa, Qa",
        "#uat, Uat",
        "#staging, Staging",
        "#deployed, Deployed",
        "#on-hold, On Hold",
        "#needs-info, Needs Info",
        "#ready-for-dev, Ready For Dev",
        "#in-qa, In Qa",
        "#pull-request-waiting, Pull Request Waiting"
    })
    void parseCommands_shouldConvertKebabCaseToTitleCase(String command, String expectedStatus) {
        // Given
        String commitMessage = "PROJ-123 " + command;

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TRANSITION);
        assertThat(commands.get(0).getValue()).isEqualTo(expectedStatus);
    }

    @Test
    void parseCommands_shouldNotConfuseCustomStatusWithCommentCommand() {
        // Given - ensure #comment is not treated as a status
        String commitMessage = "PROJ-123 #comment This is a comment";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.COMMENT);
        assertThat(commands.get(0).getValue()).isEqualTo("This is a comment");
    }

    @Test
    void parseCommands_shouldNotConfuseCustomStatusWithTimeCommand() {
        // Given - ensure #time is not treated as a status
        String commitMessage = "PROJ-123 #time 2h";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.TIME);
    }

    @Test
    void parseCommands_shouldNotConfuseCustomStatusWithAssignCommand() {
        // Given - ensure #assign is not treated as a status
        String commitMessage = "PROJ-123 #assign @john";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.ASSIGN);
    }

    @Test
    void parseCommands_shouldNotConfuseCustomStatusWithLabelCommand() {
        // Given - ensure #label is not treated as a status
        String commitMessage = "PROJ-123 #label bug";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getType()).isEqualTo(SmartCommitCommandType.LABEL);
    }

    @Test
    void parseCommands_shouldHandleCustomStatusWithOtherCommands() {
        // Given - mix of custom status and other commands
        String commitMessage = "PROJ-123 #testing #comment Added tests #assign @jane";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(3);
        assertThat(commands).extracting(SmartCommitCommand::getType)
            .containsExactlyInAnyOrder(
                SmartCommitCommandType.TRANSITION,
                SmartCommitCommandType.COMMENT,
                SmartCommitCommandType.ASSIGN
            );

        // Verify the custom status was parsed correctly
        SmartCommitCommand transitionCmd = commands.stream()
            .filter(c -> c.getType() == SmartCommitCommandType.TRANSITION)
            .findFirst()
            .orElseThrow();
        assertThat(transitionCmd.getValue()).isEqualTo("Testing");
    }

    @Test
    void parseCommands_shouldPreserveExistingAliasesWithCustomStatuses() {
        // Given - both alias and custom status in same message
        String commitMessage = "PROJ-123 #close #comment Done and tested";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(2);
        SmartCommitCommand transitionCmd = commands.stream()
            .filter(c -> c.getType() == SmartCommitCommandType.TRANSITION)
            .findFirst()
            .orElseThrow();
        assertThat(transitionCmd.getValue()).isEqualTo("Done"); // Alias still works
    }

    @Test
    void containsSmartCommands_shouldReturnTrueForCustomStatus() {
        // Given
        String commitMessage = "PROJ-123 #testing";

        // When
        boolean contains = parser.containsSmartCommands(commitMessage);

        // Then
        assertThat(contains).isTrue();
    }

    @Test
    void containsSmartCommands_shouldReturnTrueForMultiWordCustomStatus() {
        // Given
        String commitMessage = "PROJ-123 #pull-request-waiting";

        // When
        boolean contains = parser.containsSmartCommands(commitMessage);

        // Then
        assertThat(contains).isTrue();
    }

    @Test
    void parseCommands_shouldHandleCaseInsensitiveCustomStatus() {
        // Given - uppercase custom status
        String commitMessage = "PROJ-123 #TESTING Ready for QA";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("Testing");
    }

    @Test
    void parseCommands_shouldHandleMixedCaseKebabStatus() {
        // Given - mixed case kebab status
        String commitMessage = "PROJ-123 #Pull-Request-WAITING";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("Pull Request Waiting");
    }

    @Test
    void parseCommands_shouldHandleStatusWithNumbers() {
        // Given - status with numbers (e.g., "Phase 2" -> "phase-2")
        String commitMessage = "PROJ-123 #phase-2";

        // When
        List<SmartCommitCommand> commands = parser.parseCommands(commitMessage);

        // Then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getValue()).isEqualTo("Phase 2");
    }
}
