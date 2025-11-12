package com.gradproject.taskmanager.modules.git.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class IssueReferenceParser {

    
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("#(\\d+)");

    
    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9]{1,10})-(\\d+)\\b");

    
    private static final Pattern BRACKETED_PATTERN = Pattern.compile("\\[([A-Z][A-Z0-9]{1,10})-(\\d+)\\]");

    
    public List<String> extractReferences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        Set<String> references = new HashSet<>();

        
        Matcher numericMatcher = NUMERIC_PATTERN.matcher(text);
        while (numericMatcher.find()) {
            references.add("#" + numericMatcher.group(1));
        }

        
        Matcher projectKeyMatcher = PROJECT_KEY_PATTERN.matcher(text);
        while (projectKeyMatcher.find()) {
            String projectKey = projectKeyMatcher.group(1);
            String issueNumber = projectKeyMatcher.group(2);
            references.add(projectKey + "-" + issueNumber);
        }

        
        Matcher bracketedMatcher = BRACKETED_PATTERN.matcher(text);
        while (bracketedMatcher.find()) {
            String projectKey = bracketedMatcher.group(1);
            String issueNumber = bracketedMatcher.group(2);
            references.add(projectKey + "-" + issueNumber);
        }

        return new ArrayList<>(references);
    }

    
    public String extractFirstReference(String text) {
        List<String> references = extractReferences(text);
        return references.isEmpty() ? null : references.get(0);
    }

    
    public boolean containsReference(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return NUMERIC_PATTERN.matcher(text).find() ||
               PROJECT_KEY_PATTERN.matcher(text).find() ||
               BRACKETED_PATTERN.matcher(text).find();
    }

    
    public String normalizeReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }

        
        String normalized = reference.replace("[", "").replace("]", "").trim();

        
        if (normalized.matches("^([A-Z][A-Z0-9]{1,10}-\\d+|#\\d+)$")) {
            return normalized;
        }

        return null;
    }
}
