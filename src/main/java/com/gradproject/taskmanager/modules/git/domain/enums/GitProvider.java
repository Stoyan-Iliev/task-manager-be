package com.gradproject.taskmanager.modules.git.domain.enums;


public enum GitProvider {
    GITHUB("GitHub"),
    GITLAB("GitLab"),
    BITBUCKET("Bitbucket");

    private final String displayName;

    GitProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
