package com.example.depanalysis.dto;

public enum DeprecationSeverity {
    LOW("Recently deprecated, no immediate action required"),
    MEDIUM("Deprecated with timeline, plan migration"),
    HIGH("Marked for removal, migrate soon"), 
    CRITICAL("Very old deprecation or imminent removal");

    private final String description;

    DeprecationSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
