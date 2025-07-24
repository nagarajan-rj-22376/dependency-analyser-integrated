package com.example.depanalysis.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DeprecationIssue {
    private String deprecatedElement;          // "com.lib.OldClass.oldMethod"
    private String deprecationType;            // "METHOD", "CLASS", "FIELD"
    private String usageLocation;              // "com.app.Service.doWork"
    private String callerDependency;           // "com.test:client-app:1.0.0"
    private String targetDependency;           // "com.test:library:2.0.0"
    
    // Deprecation details
    private String since;                      // "1.5"
    private boolean forRemoval;                // true
    private String reason;                     // Why deprecated
    private String replacement;                // "Use newMethod() instead"
    private String javadocInfo;                // @deprecated tag content
    
    // Severity assessment
    private DeprecationSeverity severity;      // HIGH, MEDIUM, LOW, CRITICAL
    private String riskDescription;            // Human-readable risk
    private String recommendation;             // "Migrate before v3.0"
    private LocalDate estimatedRemovalDate;    // When it might be removed
    private long daysSinceDeprecation;         // Timeline analysis

    public DeprecationIssue() {}

    public DeprecationIssue(String deprecatedElement, String deprecationType, String usageLocation) {
        this.deprecatedElement = deprecatedElement;
        this.deprecationType = deprecationType;
        this.usageLocation = usageLocation;
    }

    // Getters and Setters
    public String getDeprecatedElement() { return deprecatedElement; }
    public void setDeprecatedElement(String deprecatedElement) { this.deprecatedElement = deprecatedElement; }

    public String getDeprecationType() { return deprecationType; }
    public void setDeprecationType(String deprecationType) { this.deprecationType = deprecationType; }

    public String getUsageLocation() { return usageLocation; }
    public void setUsageLocation(String usageLocation) { this.usageLocation = usageLocation; }

    public String getCallerDependency() { return callerDependency; }
    public void setCallerDependency(String callerDependency) { this.callerDependency = callerDependency; }

    public String getTargetDependency() { return targetDependency; }
    public void setTargetDependency(String targetDependency) { this.targetDependency = targetDependency; }

    public String getSince() { return since; }
    public void setSince(String since) { this.since = since; }

    public boolean isForRemoval() { return forRemoval; }
    public void setForRemoval(boolean forRemoval) { this.forRemoval = forRemoval; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReplacement() { return replacement; }
    public void setReplacement(String replacement) { this.replacement = replacement; }

    public String getJavadocInfo() { return javadocInfo; }
    public void setJavadocInfo(String javadocInfo) { this.javadocInfo = javadocInfo; }

    public DeprecationSeverity getSeverity() { return severity; }
    public void setSeverity(DeprecationSeverity severity) { this.severity = severity; }

    public String getRiskDescription() { return riskDescription; }
    public void setRiskDescription(String riskDescription) { this.riskDescription = riskDescription; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public LocalDate getEstimatedRemovalDate() { return estimatedRemovalDate; }
    public void setEstimatedRemovalDate(LocalDate estimatedRemovalDate) { this.estimatedRemovalDate = estimatedRemovalDate; }

    public long getDaysSinceDeprecation() { return daysSinceDeprecation; }
    public void setDaysSinceDeprecation(long daysSinceDeprecation) { this.daysSinceDeprecation = daysSinceDeprecation; }

    // Utility method to calculate days since deprecation
    public void calculateTimeline(LocalDate deprecatedDate) {
        if (deprecatedDate != null) {
            this.daysSinceDeprecation = ChronoUnit.DAYS.between(deprecatedDate, LocalDate.now());
        }
    }

    @Override
    public String toString() {
        return String.format("DeprecationIssue{element='%s', severity=%s, since='%s', forRemoval=%s}", 
                           deprecatedElement, severity, since, forRemoval);
    }
}
