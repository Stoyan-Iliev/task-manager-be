package com.gradproject.taskmanager.modules.git.domain.enums;


public enum BranchStatus {
    ACTIVE("Active - being worked on"),
    MERGED("Merged into base branch"),
    DELETED("Deleted from repository");

    private final String description;

    BranchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
