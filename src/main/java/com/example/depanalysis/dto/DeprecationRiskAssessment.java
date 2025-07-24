package com.example.depanalysis.dto;

import java.util.List;
import java.util.ArrayList;

public class DeprecationRiskAssessment {
    private DeprecationSeverity overallRisk;
    private int criticalIssues;
    private int highIssues;
    private int mediumIssues;
    private int lowIssues;
    private List<String> recommendations;
    private String summary;
    private boolean hasImminentRemovals;      // Any forRemoval=true found
    private long averageDaysSinceDeprecation; // Timeline analysis

    public DeprecationRiskAssessment() {
        this.recommendations = new ArrayList<>();
    }

    // Getters and Setters
    public DeprecationSeverity getOverallRisk() { return overallRisk; }
    public void setOverallRisk(DeprecationSeverity overallRisk) { this.overallRisk = overallRisk; }

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

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isHasImminentRemovals() { return hasImminentRemovals; }
    public void setHasImminentRemovals(boolean hasImminentRemovals) { this.hasImminentRemovals = hasImminentRemovals; }

    public long getAverageDaysSinceDeprecation() { return averageDaysSinceDeprecation; }
    public void setAverageDaysSinceDeprecation(long averageDaysSinceDeprecation) { this.averageDaysSinceDeprecation = averageDaysSinceDeprecation; }

    // Utility methods
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    public int getTotalIssues() {
        return criticalIssues + highIssues + mediumIssues + lowIssues;
    }

    @Override
    public String toString() {
        return String.format("DeprecationRiskAssessment{overallRisk=%s, totalIssues=%d, imminentRemovals=%s}", 
                           overallRisk, getTotalIssues(), hasImminentRemovals);
    }
}
