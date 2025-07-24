package com.example.depanalysis.dto;

import java.util.List;

public class RiskAssessment {
    private String overallRisk;
    private int affectedClasses;
    private int criticalIssues;
    private int highIssues;
    private int mediumIssues;
    private int lowIssues;
    private List<String> recommendations;

    // Constructors
    public RiskAssessment() {}

    // Getters and Setters
    public String getOverallRisk() { return overallRisk; }
    public void setOverallRisk(String overallRisk) { this.overallRisk = overallRisk; }

    public int getAffectedClasses() { return affectedClasses; }
    public void setAffectedClasses(int affectedClasses) { this.affectedClasses = affectedClasses; }

    public int getCriticalIssues() { return criticalIssues; }
    public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }

    public int getHighIssues() { return highIssues; }
    public void setHighIssues(int highIssues) { this.highIssues = highIssues; }

    public int getMediumIssues() { return mediumIssues; }
    public void setMediumIssues(int mediumIssues) { this.mediumIssues = mediumIssues; }

    public int getLowIssues() { return lowIssues; }
    public void setLowIssues(int lowIssues) { this.lowIssues = lowIssues; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
