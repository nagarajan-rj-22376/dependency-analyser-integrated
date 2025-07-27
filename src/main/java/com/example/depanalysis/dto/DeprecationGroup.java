package com.example.depanalysis.dto;

import java.util.List;

public class DeprecationGroup {
    private String methodSignature;
    private String className;
    private String methodName;
    private String deprecatedSince;
    private String reason;
    private String suggestedReplacement;
    private int totalUsageCount;
    private List<String> usageLocations;
    private String riskLevel;

    public DeprecationGroup() {}

    public DeprecationGroup(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = className + "." + methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public int getTotalUsageCount() {
        return totalUsageCount;
    }

    public void setTotalUsageCount(int totalUsageCount) {
        this.totalUsageCount = totalUsageCount;
    }

    public List<String> getUsageLocations() {
        return usageLocations;
    }

    public void setUsageLocations(List<String> usageLocations) {
        this.usageLocations = usageLocations;
        this.totalUsageCount = usageLocations != null ? usageLocations.size() : 0;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
