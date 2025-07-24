package com.example.depanalysis.analysis;

import java.nio.file.Path;
import com.example.depanalysis.dto.FeatureReportSection;

public interface AnalysisFeature {
    FeatureReportSection analyze(Path projectDir);
    String getFeatureName();
}