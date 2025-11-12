package com.gradproject.taskmanager.modules.git.domain.enums;


public enum SmartCommitCommandType {
    TRANSITION("Change task status"),
    COMMENT("Add comment to task"),
    TIME("Log work time"),
    ASSIGN("Assign task to user"),
    LABEL("Add label to task"),
    CUSTOM("Custom command");

    private final String description;

    SmartCommitCommandType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
