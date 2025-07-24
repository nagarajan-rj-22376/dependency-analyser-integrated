package com.example.depanalysis.dto;

import java.util.List;
import java.util.ArrayList;

public class DeprecationResult {
    private String callerDependency;           // "com.test:client-app:1.0.0"
    private String calleeDependency;           // "com.test:library:2.0.0"
    private List<DeprecationIssue> issues;     // Found deprecated usages
    private DeprecationRiskAssessment riskAssessment; // Overall risk level
    private DeprecationStatistics statistics;  // Summary stats

    public DeprecationResult() {
        this.issues = new ArrayList<>();
    }

    public DeprecationResult(String callerDependency, String calleeDependency) {
        this.callerDependency = callerDependency;
        this.calleeDependency = calleeDependency;
        this.issues = new ArrayList<>();
    }

    // Getters and Setters
    public String getCallerDependency() { return callerDependency; }
    public void setCallerDependency(String callerDependency) { this.callerDependency = callerDependency; }

    public String getCalleeDependency() { return calleeDependency; }
    public void setCalleeDependency(String calleeDependency) { this.calleeDependency = calleeDependency; }

    public List<DeprecationIssue> getIssues() { return issues; }
    public void setIssues(List<DeprecationIssue> issues) { this.issues = issues; }

    public DeprecationRiskAssessment getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(DeprecationRiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }

    public DeprecationStatistics getStatistics() { return statistics; }
    public void setStatistics(DeprecationStatistics statistics) { this.statistics = statistics; }

    // Utility methods
    public void addIssue(DeprecationIssue issue) {
        this.issues.add(issue);
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public int getIssueCount() {
        return issues.size();
    }

    @Override
    public String toString() {
        return String.format("DeprecationResult{caller='%s', callee='%s', issues=%d}", 
                           callerDependency, calleeDependency, issues.size());
    }
}
