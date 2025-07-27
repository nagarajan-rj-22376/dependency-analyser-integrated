package com.example.depanalysis.dto;

import java.util.List;

public class ApiCompatibilityResult {
    private List<CompatibilityIssue> compatibilityIssues;

    public ApiCompatibilityResult() {}

    public ApiCompatibilityResult(List<CompatibilityIssue> compatibilityIssues) {
        this.compatibilityIssues = compatibilityIssues;
    }

    public List<CompatibilityIssue> getCompatibilityIssues() {
        return compatibilityIssues;
    }

    public void setCompatibilityIssues(List<CompatibilityIssue> compatibilityIssues) {
        this.compatibilityIssues = compatibilityIssues;
    }
}
