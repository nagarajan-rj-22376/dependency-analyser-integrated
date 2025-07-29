package com.example.depanalysis.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Interface for checking API compatibility issues in a project
 */
public interface ApiCompatibilityChecker {
    
    /**
     * Check for API compatibility issues in the given project directory
     * 
     * @param projectDir The root directory of the project to analyze
     * @return List of compatibility issues found
     */
    List<Map<String, Object>> checkCompatibilityIssues(Path projectDir);
}
