package com.example.depanalysis.dto;

import java.util.List;

public class CompatibilityGroup {
    private String methodSignature;
    private String className;
    private String methodName;
    private String issueType;
    private String expectedSignature;
    private String actualSignature;
    private String suggestedFix;
    private String riskLevel;
    private String impact;
    private int totalCallCount;
    private List<String> callerLocations;

    public CompatibilityGroup() {}

    public CompatibilityGroup(String className, String methodName) {
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

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getExpectedSignature() {
        return expectedSignature;
    }

    public void setExpectedSignature(String expectedSignature) {
        this.expectedSignature = expectedSignature;
    }

    public String getActualSignature() {
        return actualSignature;
    }

    public void setActualSignature(String actualSignature) {
        this.actualSignature = actualSignature;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public int getTotalCallCount() {
        return totalCallCount;
    }

    public void setTotalCallCount(int totalCallCount) {
        this.totalCallCount = totalCallCount;
    }

    public List<String> getCallerLocations() {
        return callerLocations;
    }

    public void setCallerLocations(List<String> callerLocations) {
        this.callerLocations = callerLocations;
        this.totalCallCount = callerLocations != null ? callerLocations.size() : 0;
    }
}
