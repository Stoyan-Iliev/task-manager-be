package com.gradproject.taskmanager.modules.git.domain.enums;


public enum LinkMethod {
    BRANCH_NAME("Detected from branch name"),
    COMMIT_MESSAGE("Detected from commit message"),
    PR_TITLE("Detected from PR title"),
    PR_DESCRIPTION("Detected from PR description"),
    MANUAL("Manually linked by user");

    private final String description;

    LinkMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
