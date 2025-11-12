package com.gradproject.taskmanager.modules.git.parser;

import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class SmartCommitParser {

    
    private static final Pattern TRANSITION_PATTERN = Pattern.compile(
        "#(close|closed|done|complete|completed|in-progress|review|to-do|todo|blocked)(?:\\s+(.*))?",
        Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "#comment\\s+(.+?)(?=#|$)",
        Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "#time\\s+((?:\\d+w\\s*)?(?:\\d+d\\s*)?(?:\\d+h\\s*)?(?:\\d+m\\s*)?)(?:\\s+(.*))?",
        Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern ASSIGN_PATTERN = Pattern.compile(
        "#assign\\s+@?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern LABEL_PATTERN = Pattern.compile(
        "#label\\s+(\\w+)",
        Pattern.CASE_INSENSITIVE
    );

    
    public List<SmartCommitCommand> parseCommands(String commitMessage) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return List.of();
        }

        List<SmartCommitCommand> commands = new ArrayList<>();

        
        Matcher transitionMatcher = TRANSITION_PATTERN.matcher(commitMessage);
        while (transitionMatcher.find()) {
            String action = transitionMatcher.group(1).toLowerCase();
            String additionalText = transitionMatcher.group(2);

            String statusName = mapActionToStatus(action);
            commands.add(new SmartCommitCommand(
                SmartCommitCommandType.TRANSITION,
                statusName,
                additionalText,
                transitionMatcher.group(0)
            ));
        }

        
        Matcher commentMatcher = COMMENT_PATTERN.matcher(commitMessage);
        while (commentMatcher.find()) {
            String commentText = commentMatcher.group(1).trim();
            commands.add(new SmartCommitCommand(
                SmartCommitCommandType.COMMENT,
                commentText,
                null,
                commentMatcher.group(0)
            ));
        }

        
        Matcher timeMatcher = TIME_PATTERN.matcher(commitMessage);
        while (timeMatcher.find()) {
            String timeValue = timeMatcher.group(1).trim();
            String additionalText = timeMatcher.group(2);
            commands.add(new SmartCommitCommand(
                SmartCommitCommandType.TIME,
                timeValue,
                additionalText,
                timeMatcher.group(0)
            ));
        }

        
        Matcher assignMatcher = ASSIGN_PATTERN.matcher(commitMessage);
        while (assignMatcher.find()) {
            String username = assignMatcher.group(1);
            commands.add(new SmartCommitCommand(
                SmartCommitCommandType.ASSIGN,
                username,
                null,
                assignMatcher.group(0)
            ));
        }

        
        Matcher labelMatcher = LABEL_PATTERN.matcher(commitMessage);
        while (labelMatcher.find()) {
            String labelName = labelMatcher.group(1);
            commands.add(new SmartCommitCommand(
                SmartCommitCommandType.LABEL,
                labelName,
                null,
                labelMatcher.group(0)
            ));
        }

        return commands;
    }

    
    public boolean containsSmartCommands(String commitMessage) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return false;
        }

        return TRANSITION_PATTERN.matcher(commitMessage).find() ||
               COMMENT_PATTERN.matcher(commitMessage).find() ||
               TIME_PATTERN.matcher(commitMessage).find() ||
               ASSIGN_PATTERN.matcher(commitMessage).find() ||
               LABEL_PATTERN.matcher(commitMessage).find();
    }

    
    private String mapActionToStatus(String action) {
        return switch (action.toLowerCase()) {
            case "close", "closed", "done", "complete", "completed" -> "Done";
            case "in-progress" -> "In Progress";
            case "review" -> "Code Review";
            case "to-do", "todo" -> "To Do";
            case "blocked" -> "Blocked";
            default -> action; 
        };
    }

    
    public int parseTimeToMinutes(String timeValue) {
        if (timeValue == null || timeValue.trim().isEmpty()) {
            return 0;
        }

        int totalMinutes = 0;

        
        Pattern weekPattern = Pattern.compile("(\\d+)w");
        Matcher weekMatcher = weekPattern.matcher(timeValue);
        if (weekMatcher.find()) {
            totalMinutes += Integer.parseInt(weekMatcher.group(1)) * 40 * 60;
        }

        
        Pattern dayPattern = Pattern.compile("(\\d+)d");
        Matcher dayMatcher = dayPattern.matcher(timeValue);
        if (dayMatcher.find()) {
            totalMinutes += Integer.parseInt(dayMatcher.group(1)) * 8 * 60;
        }

        
        Pattern hourPattern = Pattern.compile("(\\d+)h");
        Matcher hourMatcher = hourPattern.matcher(timeValue);
        if (hourMatcher.find()) {
            totalMinutes += Integer.parseInt(hourMatcher.group(1)) * 60;
        }

        
        Pattern minutePattern = Pattern.compile("(\\d+)m");
        Matcher minuteMatcher = minutePattern.matcher(timeValue);
        if (minuteMatcher.find()) {
            totalMinutes += Integer.parseInt(minuteMatcher.group(1));
        }

        return totalMinutes;
    }

    
    @Getter
    public static class SmartCommitCommand {
        private final SmartCommitCommandType type;
        private final String value;
        private final String additionalText;
        private final String originalText;

        public SmartCommitCommand(SmartCommitCommandType type, String value, String additionalText, String originalText) {
            this.type = type;
            this.value = value;
            this.additionalText = additionalText;
            this.originalText = originalText;
        }

        @Override
        public String toString() {
            return String.format("SmartCommitCommand{type=%s, value='%s', original='%s'}",
                type, value, originalText);
        }
    }
}
