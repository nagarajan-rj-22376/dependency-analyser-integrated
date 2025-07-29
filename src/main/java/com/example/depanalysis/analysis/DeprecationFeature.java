package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.DeprecationResult;
import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DeprecationFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(DeprecationFeature.class);
    private final DeprecationChecker checker;

    public DeprecationFeature(DeprecationChecker checker) {
        this.checker = checker;
    }

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting deprecation analysis for project: {}", projectDir);
        
        // Get deprecation issues from the checker
        var issues = checker.checkDeprecationIssues(projectDir);
        
        DeprecationResult result = new DeprecationResult();
        result.setDeprecationIssues(issues);
        
        logger.info("Deprecation analysis completed. Found {} issues", issues.size());
        return new FeatureReportSection(getFeatureName(), result);
    }

    @Override
    public String getFeatureName() {
        return "Deprecation Analysis";
    }
}