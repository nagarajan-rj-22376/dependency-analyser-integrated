package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.DeprecationIssue;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for checking deprecation issues in a project.
 * Implementations should scan the project for usage of deprecated APIs and methods.
 */
public interface DeprecationChecker {
    
    /**
     * Checks for deprecation issues in the given project directory.
     * 
     * @param projectDir the root directory of the project to analyze
     * @return a list of deprecation issues found in the project
     */
    List<DeprecationIssue> checkDeprecationIssues(Path projectDir);
}
