package com.example.depanalysis.dto;

public class FeatureReportSection {
    private String featureName;
    private Object result;

    public FeatureReportSection(String featureName, Object result) {
        this.featureName = featureName;
        this.result = result;
    }

    // Getters and Setters
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
}