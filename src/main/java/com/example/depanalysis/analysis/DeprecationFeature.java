package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DeprecationFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(DeprecationFeature.class);
    private final DeprecationAnalyzer analyzer;

    public DeprecationFeature(DeprecationAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting deprecated API usage feature analysis");
        return analyzer.analyze(projectDir);
    }

    @Override
    public String getFeatureName() {
        return "Deprecated API Usage Analysis";
    }
}
