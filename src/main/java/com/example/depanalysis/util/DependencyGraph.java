package com.example.depanalysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DependencyGraph {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);
    
    private Map<String, DependencyNode> nodes = new HashMap<>();
    private Map<String, String> resolvedVersions = new HashMap<>(); // groupId:artifactId -> resolved version

    public static class DependencyNode {
        private String groupId;
        private String artifactId;
        private String version;
        private String source; // "pom.xml", "build.gradle", "jar", "transitive"
        private Set<String> dependencies = new HashSet<>(); // Set of "groupId:artifactId" dependencies
        private boolean isTransitive = false;

        public DependencyNode(String groupId, String artifactId, String version, String source) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.source = source;
        }

        // Getters and Setters
        public String getGroupId() { return groupId; }
        public String getArtifactId() { return artifactId; }
        public String getVersion() { return version; }
        public String getSource() { return source; }
        public Set<String> getDependencies() { return dependencies; }
        public boolean isTransitive() { return isTransitive; }
        public void setTransitive(boolean transitive) { isTransitive = transitive; }
        
        public String getKey() { return groupId + ":" + artifactId; }
        public String getFullKey() { return groupId + ":" + artifactId + ":" + version; }
    }

    public void addDependency(String groupId, String artifactId, String version, String source) {
        String key = groupId + ":" + artifactId;
        DependencyNode node = new DependencyNode(groupId, artifactId, version, source);
        
        if (nodes.containsKey(key)) {
            // Handle version conflict - simulate Maven resolution (highest version wins)
            DependencyNode existing = nodes.get(key);
            if (compareVersions(version, existing.getVersion()) > 0) {
                logger.info("Version conflict resolved: {} {} -> {} (source: {})", 
                           key, existing.getVersion(), version, source);
                nodes.put(key, node);
                resolvedVersions.put(key, version);
            } else {
                logger.debug("Keeping existing version: {} {} over {} (source: {})", 
                            key, existing.getVersion(), version, source);
            }
        } else {
            nodes.put(key, node);
            resolvedVersions.put(key, version);
            logger.debug("Added dependency: {} (source: {})", node.getFullKey(), source);
        }
    }

    public DependencyNode getDependency(String groupId, String artifactId) {
        return nodes.get(groupId + ":" + artifactId);
    }

    public String getResolvedVersion(String groupId, String artifactId) {
        return resolvedVersions.get(groupId + ":" + artifactId);
    }

    public Collection<DependencyNode> getAllDependencies() {
        return nodes.values();
    }

    public List<DependencyNode> getVersionConflicts() {
        // This would be enhanced to track all versions seen for each dependency
        // For now, return empty list as conflicts are resolved during addition
        return new ArrayList<>();
    }

    private int compareVersions(String version1, String version2) {
        // Simple version comparison - can be enhanced with proper semantic versioning
        try {
            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");
            
            int maxLength = Math.max(v1Parts.length, v2Parts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
                
                if (v1Part != v2Part) {
                    return Integer.compare(v1Part, v2Part);
                }
            }
            return 0;
        } catch (Exception e) {
            logger.warn("Error comparing versions {} and {}: {}", version1, version2, e.getMessage());
            return version1.compareTo(version2);
        }
    }

    private int parseVersionPart(String part) {
        // Extract numeric part from version component (e.g., "2" from "2-SNAPSHOT")
        StringBuilder numeric = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                break;
            }
        }
        return numeric.length() > 0 ? Integer.parseInt(numeric.toString()) : 0;
    }
}
