package com.example.depanalysis.dto;

import java.util.List;

public class DeprecationIssue {
    private String methodName;
    private String className;
    private String deprecatedSince;
    private String reason;
    private String suggestedReplacement;
    private int usageCount;
    private List<String> usageLocations;
    private String riskLevel;
    private String sourceType; // "PROJECT" or "JAR"

    public DeprecationIssue() {}

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDeprecatedSince() {
        return deprecatedSince;
    }

    public void setDeprecatedSince(String deprecatedSince) {
        this.deprecatedSince = deprecatedSince;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestedReplacement() {
        return suggestedReplacement;
    }

    public void setSuggestedReplacement(String suggestedReplacement) {
        this.suggestedReplacement = suggestedReplacement;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public List<String> getUsageLocations() {
        return usageLocations;
    }

    public void setUsageLocations(List<String> usageLocations) {
        this.usageLocations = usageLocations;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
