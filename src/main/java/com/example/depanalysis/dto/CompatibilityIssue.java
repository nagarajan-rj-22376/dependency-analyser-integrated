package com.example.depanalysis.dto;

public class CompatibilityIssue {
    private String issueType;
    private String className;
    private String methodName;
    private String expectedSignature;
    private String actualSignature;
    private String usageLocation;
    private String suggestedFix;
    private String riskLevel;
    private String impact;

    // Constructors
    public CompatibilityIssue() {}

    public CompatibilityIssue(String issueType, String className, String methodName, 
                             String usageLocation, String riskLevel, String impact) {
        this.issueType = issueType;
        this.className = className;
        this.methodName = methodName;
        this.usageLocation = usageLocation;
        this.riskLevel = riskLevel;
        this.impact = impact;
    }

    // Getters and Setters
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getExpectedSignature() { return expectedSignature; }
    public void setExpectedSignature(String expectedSignature) { this.expectedSignature = expectedSignature; }

    public String getActualSignature() { return actualSignature; }
    public void setActualSignature(String actualSignature) { this.actualSignature = actualSignature; }

    public String getUsageLocation() { return usageLocation; }
    public void setUsageLocation(String usageLocation) { this.usageLocation = usageLocation; }

    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
}
