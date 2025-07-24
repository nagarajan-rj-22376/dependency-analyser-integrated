package com.example.depanalysis.dto;

import java.util.List;

public class ApiCompatibilityResult {
    private String callerDependency;
    private String calleeDependency;
    private VersionMismatch versionMismatch;
    private List<CompatibilityIssue> compatibilityIssues;
    private RiskAssessment riskAssessment;

    // Constructors
    public ApiCompatibilityResult() {}

    public ApiCompatibilityResult(String callerDependency, String calleeDependency) {
        this.callerDependency = callerDependency;
        this.calleeDependency = calleeDependency;
    }

    // Getters and Setters
    public String getCallerDependency() { return callerDependency; }
    public void setCallerDependency(String callerDependency) { this.callerDependency = callerDependency; }

    public String getCalleeDependency() { return calleeDependency; }
    public void setCalleeDependency(String calleeDependency) { this.calleeDependency = calleeDependency; }

    public VersionMismatch getVersionMismatch() { return versionMismatch; }
    public void setVersionMismatch(VersionMismatch versionMismatch) { this.versionMismatch = versionMismatch; }

    public List<CompatibilityIssue> getCompatibilityIssues() { return compatibilityIssues; }
    public void setCompatibilityIssues(List<CompatibilityIssue> compatibilityIssues) { this.compatibilityIssues = compatibilityIssues; }

    public RiskAssessment getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(RiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }
}
