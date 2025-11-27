package com.gradproject.taskmanager.modules.task.domain;


public enum WorkLogSource {
    MANUAL("Logged manually via UI"),
    SMART_COMMIT("Logged via git smart commit"),
    API("Logged via API integration");

    private final String description;

    WorkLogSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
