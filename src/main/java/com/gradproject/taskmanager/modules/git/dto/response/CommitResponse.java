package com.gradproject.taskmanager.modules.git.dto.response;

import java.time.LocalDateTime;
import java.util.List;


public record CommitResponse(
    Long id,
    Long gitIntegrationId,
    String commitSha,
    String shortSha,  
    String parentSha,
    String branchName,

    
    String authorName,
    String authorEmail,
    LocalDateTime authorDate,
    String committerName,
    String committerEmail,
    LocalDateTime committerDate,

    
    String message,
    String messageBody,

    
    Integer linesAdded,
    Integer linesDeleted,
    Integer filesChanged,

    
    String commitUrl,

    
    List<String> linkedTaskKeys,  

    
    List<String> smartCommands,

    LocalDateTime createdAt
) {
    
    public CommitResponse {
        if (shortSha == null && commitSha != null && commitSha.length() >= 7) {
            shortSha = commitSha.substring(0, 7);
        }
    }
}
