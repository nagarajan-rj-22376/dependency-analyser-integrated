package com.example.depanalysis.util;

import com.example.depanalysis.dto.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReportDataProcessor {

    /**
     * Groups vulnerability results by dependency and sorts by vulnerability count (descending)
     */
    public static List<VulnerabilityGroup> groupVulnerabilitiesByDependency(List<VulnerabilityResult> vulnerabilityResults) {
        if (vulnerabilityResults == null || vulnerabilityResults.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, VulnerabilityGroup> groupMap = new HashMap<>();

        for (VulnerabilityResult result : vulnerabilityResults) {
            String key = result.getGroupId() + ":" + result.getArtifactId() + ":" + result.getVersion();
            
            VulnerabilityGroup group = groupMap.computeIfAbsent(key, k -> {
                VulnerabilityGroup g = new VulnerabilityGroup(
                    key, result.getGroupId(), result.getArtifactId(), result.getVersion()
                );
                g.setVulnerabilities(new ArrayList<>());
                return g;
            });

            if (result.getVulnerabilities() != null) {
                group.getVulnerabilities().addAll(result.getVulnerabilities());
            }
        }

        // Update vulnerability counts, filter out dependencies with 0 vulnerabilities, and sort by count (descending)
        return groupMap.values().stream()
                .peek(group -> group.setVulnerabilityCount(group.getVulnerabilities().size()))
                .filter(group -> group.getVulnerabilityCount() > 0) // Exclude dependencies with 0 vulnerabilities
                .sorted((a, b) -> Integer.compare(b.getVulnerabilityCount(), a.getVulnerabilityCount()))
                .collect(Collectors.toList());
    }

    /**
     * Groups deprecation issues by method and sorts by usage count (descending)
     */
    public static List<DeprecationGroup> groupDeprecationsByMethod(List<DeprecationIssue> deprecationIssues) {
        if (deprecationIssues == null || deprecationIssues.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, DeprecationGroup> groupMap = new HashMap<>();

        for (DeprecationIssue issue : deprecationIssues) {
            String key = issue.getClassName() + "." + issue.getMethodName();
            
            DeprecationGroup group = groupMap.computeIfAbsent(key, k -> {
                DeprecationGroup g = new DeprecationGroup(issue.getClassName(), issue.getMethodName());
                g.setUsageLocations(new ArrayList<>());
                g.setDeprecatedSince(issue.getDeprecatedSince());
                g.setReason(issue.getReason());
                g.setSuggestedReplacement(issue.getSuggestedReplacement());
                g.setRiskLevel(issue.getRiskLevel());
                return g;
            });

            // Aggregate usage locations
            if (issue.getUsageLocations() != null) {
                group.getUsageLocations().addAll(issue.getUsageLocations());
            }
        }

        // Update total usage counts and sort by count (descending)
        return groupMap.values().stream()
                .peek(group -> group.setTotalUsageCount(group.getUsageLocations().size()))
                .sorted((a, b) -> Integer.compare(b.getTotalUsageCount(), a.getTotalUsageCount()))
                .collect(Collectors.toList());
    }

    /**
     * Groups compatibility issues by method and sorts by call count (descending)
     */
    public static List<CompatibilityGroup> groupCompatibilityByMethod(List<CompatibilityIssue> compatibilityIssues) {
        if (compatibilityIssues == null || compatibilityIssues.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, CompatibilityGroup> groupMap = new HashMap<>();

        for (CompatibilityIssue issue : compatibilityIssues) {
            String key = issue.getClassName() + "." + issue.getMethodName();
            
            CompatibilityGroup group = groupMap.computeIfAbsent(key, k -> {
                CompatibilityGroup g = new CompatibilityGroup(issue.getClassName(), issue.getMethodName());
                g.setCallerLocations(new ArrayList<>());
                g.setIssueType(issue.getIssueType());
                g.setExpectedSignature(issue.getExpectedSignature());
                g.setActualSignature(issue.getActualSignature());
                g.setSuggestedFix(issue.getSuggestedFix());
                g.setRiskLevel(issue.getRiskLevel());
                g.setImpact(issue.getImpact());
                
                // Format method signature for better readability
                if (issue.getMethodName() != null && issue.getExpectedSignature() != null) {
                    String formattedSig = SignatureFormatter.formatMethodSignature(issue.getMethodName(), issue.getExpectedSignature());
                    if (issue.getClassName() != null) {
                        formattedSig = SignatureFormatter.formatClassName(issue.getClassName()) + "." + formattedSig;
                    }
                    g.setMethodSignature(formattedSig);
                } else {
                    // Fallback to simple class.method format
                    String fallbackSig = (issue.getClassName() != null ? issue.getClassName() + "." : "") + 
                                         (issue.getMethodName() != null ? issue.getMethodName() : "unknown");
                    g.setMethodSignature(fallbackSig);
                }
                
                return g;
            });

            // Aggregate caller locations
            if (issue.getCallerLocations() != null) {
                group.getCallerLocations().addAll(issue.getCallerLocations());
            } else if (issue.getUsageLocation() != null) {
                // Handle single usage location
                group.getCallerLocations().add(issue.getUsageLocation());
            }
        }

        // Update total call counts and sort by count (descending)
        return groupMap.values().stream()
                .peek(group -> group.setTotalCallCount(group.getCallerLocations().size()))
                .sorted((a, b) -> Integer.compare(b.getTotalCallCount(), a.getTotalCallCount()))
                .collect(Collectors.toList());
    }

    /**
     * Convert from JSON compatibility data to structured compatibility issues
     */
    public static List<CompatibilityIssue> parseCompatibilityIssues(Object apiCompatibilityResult) {
        List<CompatibilityIssue> issues = new ArrayList<>();
        
        if (apiCompatibilityResult instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) apiCompatibilityResult;
            
            Object sectionsObj = resultMap.get("sections");
            if (sectionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> sections = (List<Object>) sectionsObj;
                
                for (Object section : sections) {
                    if (section instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sectionMap = (Map<String, Object>) section;
                        
                        Object resultObj = sectionMap.get("result");
                        if (resultObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> compatibilityList = (List<Object>) resultObj;
                            
                            for (Object compatItem : compatibilityList) {
                                if (compatItem instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> compatMap = (Map<String, Object>) compatItem;
                                    
                                    Object compatibilityIssuesObj = compatMap.get("compatibilityIssues");
                                    if (compatibilityIssuesObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<Object> compatibilityIssues = (List<Object>) compatibilityIssuesObj;
                                        
                                        for (Object issueObj : compatibilityIssues) {
                                            if (issueObj instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> issueMap = (Map<String, Object>) issueObj;
                                                
                                                CompatibilityIssue issue = new CompatibilityIssue();
                                                issue.setIssueType(getString(issueMap, "issueType"));
                                                issue.setClassName(getString(issueMap, "className"));
                                                issue.setMethodName(getString(issueMap, "methodName"));
                                                issue.setExpectedSignature(getString(issueMap, "expectedSignature"));
                                                issue.setActualSignature(getString(issueMap, "actualSignature"));
                                                issue.setUsageLocation(getString(issueMap, "usageLocation"));
                                                issue.setSuggestedFix(getString(issueMap, "suggestedFix"));
                                                issue.setRiskLevel(getString(issueMap, "riskLevel"));
                                                issue.setImpact(getString(issueMap, "impact"));
                                                issue.setCallCount(1); // Each issue represents one call
                                                issue.setCallerLocations(Arrays.asList(getString(issueMap, "usageLocation")));
                                                
                                                issues.add(issue);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return issues;
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    // Inner classes for grouping
    public static class VulnerabilityGroup {
        private String dependencyKey;
        private String groupId;
        private String artifactId;
        private String version;
        private List<VulnerabilityInfo> vulnerabilities;
        private int vulnerabilityCount;

        public VulnerabilityGroup(String dependencyKey, String groupId, String artifactId, String version) {
            this.dependencyKey = dependencyKey;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.vulnerabilities = new ArrayList<>();
        }

        // Getters and setters
        public String getDependencyKey() { return dependencyKey; }
        public void setDependencyKey(String dependencyKey) { this.dependencyKey = dependencyKey; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getArtifactId() { return artifactId; }
        public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public List<VulnerabilityInfo> getVulnerabilities() { return vulnerabilities; }
        public void setVulnerabilities(List<VulnerabilityInfo> vulnerabilities) { this.vulnerabilities = vulnerabilities; }
        public int getVulnerabilityCount() { return vulnerabilityCount; }
        public void setVulnerabilityCount(int vulnerabilityCount) { this.vulnerabilityCount = vulnerabilityCount; }
    }

    public static class DeprecationGroup {
        private String className;
        private String methodName;
        private String methodSignature;
        private String deprecatedSince;
        private String reason;
        private String suggestedReplacement;
        private String riskLevel;
        private int totalUsageCount;
        private List<String> usageLocations;

        public DeprecationGroup(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.methodSignature = className + "." + methodName;
            this.usageLocations = new ArrayList<>();
        }

        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getMethodSignature() { return methodSignature; }
        public void setMethodSignature(String methodSignature) { this.methodSignature = methodSignature; }
        public String getDeprecatedSince() { return deprecatedSince; }
        public void setDeprecatedSince(String deprecatedSince) { this.deprecatedSince = deprecatedSince; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getSuggestedReplacement() { return suggestedReplacement; }
        public void setSuggestedReplacement(String suggestedReplacement) { this.suggestedReplacement = suggestedReplacement; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public int getTotalUsageCount() { return totalUsageCount; }
        public void setTotalUsageCount(int totalUsageCount) { this.totalUsageCount = totalUsageCount; }
        public List<String> getUsageLocations() { return usageLocations; }
        public void setUsageLocations(List<String> usageLocations) { this.usageLocations = usageLocations; }
    }

    public static class CompatibilityGroup {
        private String className;
        private String methodName;
        private String methodSignature;
        private String issueType;
        private String expectedSignature;
        private String actualSignature;
        private String suggestedFix;
        private String riskLevel;
        private String impact;
        private int totalCallCount;
        private List<String> callerLocations;

        public CompatibilityGroup(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.methodSignature = className + "." + methodName;
            this.callerLocations = new ArrayList<>();
        }

        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getMethodSignature() { return methodSignature; }
        public void setMethodSignature(String methodSignature) { this.methodSignature = methodSignature; }
        public String getIssueType() { return issueType; }
        public void setIssueType(String issueType) { this.issueType = issueType; }
        public String getExpectedSignature() { return expectedSignature; }
        public void setExpectedSignature(String expectedSignature) { this.expectedSignature = expectedSignature; }
        public String getActualSignature() { return actualSignature; }
        public void setActualSignature(String actualSignature) { this.actualSignature = actualSignature; }
        public String getSuggestedFix() { return suggestedFix; }
        public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }
        public int getTotalCallCount() { return totalCallCount; }
        public void setTotalCallCount(int totalCallCount) { this.totalCallCount = totalCallCount; }
        public List<String> getCallerLocations() { return callerLocations; }
        public void setCallerLocations(List<String> callerLocations) { this.callerLocations = callerLocations; }
    }
}
