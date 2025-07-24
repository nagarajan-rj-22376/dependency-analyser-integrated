package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ApiCompatibilityFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCompatibilityFeature.class);
    private final ApiCompatibilityAnalyzer analyzer;

    public ApiCompatibilityFeature(ApiCompatibilityAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting API compatibility feature analysis");
        return analyzer.analyze(projectDir);
    }

    @Override
    public String getFeatureName() {
        return "API Compatibility Analysis";
    }
}
