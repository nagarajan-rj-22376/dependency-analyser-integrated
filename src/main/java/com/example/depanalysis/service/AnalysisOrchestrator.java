package com.example.depanalysis.service;

import com.example.depanalysis.analysis.AnalysisFeature;
import com.example.depanalysis.dto.FinalReport;
import com.example.depanalysis.dto.FeatureReportSection;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AnalysisOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisOrchestrator.class);
    
    private List<AnalysisFeature> features;

    public AnalysisOrchestrator(List<AnalysisFeature> features) {
        logger.info("Beans injected: " + features);
        this.features = features;
        logger.info("AnalysisOrchestrator initialized with {} features", features.size());
        for (AnalysisFeature feature : features) {
            logger.debug("Registered feature: {}", feature.getClass().getSimpleName());
        }
    }

    public FinalReport analyzeAll(Path projectDir) {
        logger.info("Starting analysis for project directory: {}", projectDir);
        List<FeatureReportSection> sections = new ArrayList<>();
        
        for (AnalysisFeature feature : features) {
            logger.info("Executing analysis feature: {}", feature.getClass().getSimpleName());
            try {
                FeatureReportSection section = feature.analyze(projectDir);
                sections.add(section);
                logger.debug("Feature {} completed successfully", feature.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Error executing feature {}: {}", feature.getClass().getSimpleName(), e.getMessage(), e);
                throw e;
            }
        }
        
        logger.info("Analysis completed. Generated {} report sections", sections.size());
        return new FinalReport(sections);
    }
}