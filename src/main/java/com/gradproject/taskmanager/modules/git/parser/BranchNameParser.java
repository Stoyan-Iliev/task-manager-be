package com.gradproject.taskmanager.modules.git.parser;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class BranchNameParser {

    
    
    private static final Pattern BRANCH_PATTERN = Pattern.compile(
        "(?:feature|bugfix|hotfix|release)?/?([A-Z][A-Z0-9]{1,10})-(\\d+)(?:-|/|$)"
    );

    
    private static final Pattern NUMERIC_BRANCH_PATTERN = Pattern.compile(
        "(?:feature|bugfix|hotfix|release)?/?(\\d+)(?:-|/|$)"
    );

    
    public String extractTaskReference(String branchName) {
        if (branchName == null || branchName.trim().isEmpty()) {
            return null;
        }

        
        Matcher matcher = BRANCH_PATTERN.matcher(branchName);
        if (matcher.find()) {
            String projectKey = matcher.group(1);
            String issueNumber = matcher.group(2);
            return projectKey + "-" + issueNumber;
        }

        
        Matcher numericMatcher = NUMERIC_BRANCH_PATTERN.matcher(branchName);
        if (numericMatcher.find()) {
            return "#" + numericMatcher.group(1);
        }

        return null;
    }

    
    public boolean containsTaskReference(String branchName) {
        return extractTaskReference(branchName) != null;
    }

    
    public String generateBranchName(String taskKey, String description, String branchType) {
        if (taskKey == null || taskKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Task key cannot be empty");
        }

        String type = (branchType != null && !branchType.isEmpty()) ? branchType : "feature";
        String sanitizedDescription = sanitizeDescription(description);

        if (sanitizedDescription != null && !sanitizedDescription.isEmpty()) {
            return String.format("%s/%s-%s", type, taskKey, sanitizedDescription);
        } else {
            return String.format("%s/%s", type, taskKey);
        }
    }

    
    private String sanitizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        String sanitized = description.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")  
            .trim()
            .replaceAll("\\s+", "-")           
            .replaceAll("-+", "-");            

        
        if (sanitized.length() > 50) {
            return sanitized.substring(0, 50);
        }

        return sanitized.isEmpty() ? null : sanitized;
    }

    
    public boolean isValidBranchName(String branchName) {
        if (branchName == null || branchName.trim().isEmpty()) {
            return false;
        }

        
        return branchName.matches("^(feature|bugfix|hotfix|release|main|master|develop)/.*$") ||
               containsTaskReference(branchName);
    }
}
