package com.example.depanalysis.dto;

import java.util.Map;
import java.util.HashMap;

public class DeprecationStatistics {
    private int totalDeprecatedElements;
    private int methodsDeprecated;
    private int classesDeprecated;
    private int fieldsDeprecated;
    private int elementsMarkedForRemoval;
    private Map<String, Integer> deprecationsByLibrary; // Library -> count
    private Map<DeprecationSeverity, Integer> issuesBySeverity;
    private double averageDeprecationAge; // In days
    
    public DeprecationStatistics() {
        this.deprecationsByLibrary = new HashMap<>();
        this.issuesBySeverity = new HashMap<>();
        
        // Initialize severity counts
        for (DeprecationSeverity severity : DeprecationSeverity.values()) {
            this.issuesBySeverity.put(severity, 0);
        }
    }

    // Getters and Setters
    public int getTotalDeprecatedElements() { return totalDeprecatedElements; }
    public void setTotalDeprecatedElements(int totalDeprecatedElements) { this.totalDeprecatedElements = totalDeprecatedElements; }

    public int getMethodsDeprecated() { return methodsDeprecated; }
    public void setMethodsDeprecated(int methodsDeprecated) { this.methodsDeprecated = methodsDeprecated; }

    public int getClassesDeprecated() { return classesDeprecated; }
    public void setClassesDeprecated(int classesDeprecated) { this.classesDeprecated = classesDeprecated; }

    public int getFieldsDeprecated() { return fieldsDeprecated; }
    public void setFieldsDeprecated(int fieldsDeprecated) { this.fieldsDeprecated = fieldsDeprecated; }

    public int getElementsMarkedForRemoval() { return elementsMarkedForRemoval; }
    public void setElementsMarkedForRemoval(int elementsMarkedForRemoval) { this.elementsMarkedForRemoval = elementsMarkedForRemoval; }

    public Map<String, Integer> getDeprecationsByLibrary() { return deprecationsByLibrary; }
    public void setDeprecationsByLibrary(Map<String, Integer> deprecationsByLibrary) { this.deprecationsByLibrary = deprecationsByLibrary; }

    public Map<DeprecationSeverity, Integer> getIssuesBySeverity() { return issuesBySeverity; }
    public void setIssuesBySeverity(Map<DeprecationSeverity, Integer> issuesBySeverity) { this.issuesBySeverity = issuesBySeverity; }

    public double getAverageDeprecationAge() { return averageDeprecationAge; }
    public void setAverageDeprecationAge(double averageDeprecationAge) { this.averageDeprecationAge = averageDeprecationAge; }

    // Utility methods
    public void incrementSeverityCount(DeprecationSeverity severity) {
        this.issuesBySeverity.merge(severity, 1, Integer::sum);
    }

    public void incrementLibraryCount(String library) {
        this.deprecationsByLibrary.merge(library, 1, Integer::sum);
    }

    public void incrementDeprecationType(String type) {
        switch (type.toUpperCase()) {
            case "METHOD":
                this.methodsDeprecated++;
                break;
            case "CLASS":
                this.classesDeprecated++;
                break;
            case "FIELD":
                this.fieldsDeprecated++;
                break;
        }
        this.totalDeprecatedElements++;
    }

    @Override
    public String toString() {
        return String.format("DeprecationStatistics{total=%d, methods=%d, classes=%d, forRemoval=%d}", 
                           totalDeprecatedElements, methodsDeprecated, classesDeprecated, elementsMarkedForRemoval);
    }
}
