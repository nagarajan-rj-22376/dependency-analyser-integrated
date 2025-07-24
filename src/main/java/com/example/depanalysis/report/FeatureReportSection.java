package com.example.depanalysis.report;

public class FeatureReportSection {
    private String featureName;
    private Object result; // Could use generics, but Object is flexible for different features

    public FeatureReportSection() {}

    public FeatureReportSection(String featureName, Object result) {
        this.featureName = featureName;
        this.result = result;
    }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
}