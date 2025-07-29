package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Component
public class ApiCompatibilityFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCompatibilityFeature.class);
    private final ApiCompatibilityChecker checker;

    public ApiCompatibilityFeature(ApiCompatibilityChecker checker) {
        this.checker = checker;
    }

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting API compatibility analysis for project: {}", projectDir);
        
        // Get compatibility issues from the checker
        List<Map<String, Object>> issues = checker.checkCompatibilityIssues(projectDir);
        
        // Prepare the data structure for the report
        Map<String, Object> compatibilityData = new HashMap<>();
        List<Map<String, Object>> sections = new ArrayList<>();
        
        Map<String, Object> section = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> compatibilityResult = new HashMap<>();
        compatibilityResult.put("compatibilityIssues", issues);
        results.add(compatibilityResult);
        section.put("result", results);
        sections.add(section);
        compatibilityData.put("sections", sections);
        
        logger.info("API compatibility analysis completed. Found {} issues", issues.size());
        return new FeatureReportSection(getFeatureName(), compatibilityData);
    }

    @Override
    public String getFeatureName() {
        return "API Compatibility";
    }
}