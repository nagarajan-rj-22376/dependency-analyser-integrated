package com.example.depanalysis.report.handlers;

import com.example.depanalysis.dto.*;
import com.example.depanalysis.report.ReportFormatHandler;
import com.example.depanalysis.util.ReportDataProcessor;
import com.example.depanalysis.util.ReportDataProcessor.VulnerabilityGroup;
import com.example.depanalysis.util.ReportDataProcessor.DeprecationGroup;
import com.example.depanalysis.util.ReportDataProcessor.CompatibilityGroup;
import com.example.depanalysis.util.SignatureFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonReportHandler implements ReportFormatHandler {

    private final ObjectMapper objectMapper;

    public JsonReportHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void generateReport(FinalReport report, String outputPath) throws IOException {
        EnhancedJsonReport enhancedReport = createEnhancedReport(report);
        
        Path reportsDir = Paths.get("reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        Path jsonFile = reportsDir.resolve("dependency-analysis-report.json");
        objectMapper.writeValue(jsonFile.toFile(), enhancedReport);
    }

    private EnhancedJsonReport createEnhancedReport(FinalReport report) {
        EnhancedJsonReport enhancedReport = new EnhancedJsonReport();
        enhancedReport.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        enhancedReport.setReportVersion("1.0");
        
        Map<String, Object> processedSections = new HashMap<>();
        
        if (report.getSections() != null) {
            for (FeatureReportSection section : report.getSections()) {
                String featureName = section.getFeatureName();
                Object processedData = processSection(featureName, section.getResult());
                processedSections.put(featureName, processedData);
            }
        }
        
        enhancedReport.setSections(processedSections);
        enhancedReport.setSummary(generateSummary(processedSections));
        
        return enhancedReport;
    }

    private Object processSection(String featureName, Object result) {
        switch (featureName.toLowerCase()) {
            case "vulnerability scan":
                return processVulnerabilitySection(result);
            case "deprecation analysis":
                return processDeprecationSection(result);
            case "api compatibility":
                return processCompatibilitySection(result);
            default:
                return result;
        }
    }

    private Object processVulnerabilitySection(Object result) {
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<VulnerabilityResult> vulnerabilityResults = (List<VulnerabilityResult>) result;
            
            List<VulnerabilityGroup> groups = ReportDataProcessor.groupVulnerabilitiesByDependency(vulnerabilityResults);
            
            Map<String, Object> processedData = new HashMap<>();
            processedData.put("totalDependencies", groups.size());
            processedData.put("totalVulnerabilities", groups.stream().mapToInt(VulnerabilityGroup::getVulnerabilityCount).sum());
            processedData.put("dependenciesGroupedByVulnerabilities", groups);
            processedData.put("rawData", result);
            
            return processedData;
        }
        return result;
    }

    private Object processDeprecationSection(Object result) {
        if (result instanceof DeprecationResult) {
            DeprecationResult deprecationResult = (DeprecationResult) result;
            List<DeprecationGroup> groups = ReportDataProcessor.groupDeprecationsByMethod(deprecationResult.getDeprecationIssues());
            
            Map<String, Object> processedData = new HashMap<>();
            processedData.put("totalDeprecatedMethods", groups.size());
            processedData.put("totalUsages", groups.stream().mapToInt(DeprecationGroup::getTotalUsageCount).sum());
            processedData.put("methodsGroupedByUsage", groups);
            processedData.put("rawData", result);
            
            return processedData;
        }
        return result;
    }

    private Object processCompatibilitySection(Object result) {
        List<CompatibilityIssue> issues = ReportDataProcessor.parseCompatibilityIssues(result);
        List<CompatibilityGroup> groups = ReportDataProcessor.groupCompatibilityByMethod(issues);
        
        Map<String, Object> processedData = new HashMap<>();
        processedData.put("totalAffectedMethods", groups.size());
        processedData.put("totalCalls", groups.stream().mapToInt(CompatibilityGroup::getTotalCallCount).sum());
        processedData.put("methodsGroupedByCalls", groups);
        processedData.put("rawData", result);
        
        return processedData;
    }

    private Map<String, Object> generateSummary(Map<String, Object> sections) {
        Map<String, Object> summary = new HashMap<>();
        
        // Vulnerability summary
        Object vulnSection = sections.get("Vulnerability Scan");
        if (vulnSection instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vulnData = (Map<String, Object>) vulnSection;
            summary.put("vulnerabilities", Map.of(
                "dependenciesWithVulnerabilities", vulnData.getOrDefault("totalDependencies", 0),
                "totalVulnerabilities", vulnData.getOrDefault("totalVulnerabilities", 0)
            ));
        }
        
        // Deprecation summary
        Object deprecationSection = sections.get("Deprecation Analysis");
        if (deprecationSection instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> deprecationData = (Map<String, Object>) deprecationSection;
            summary.put("deprecations", Map.of(
                "deprecatedMethods", deprecationData.getOrDefault("totalDeprecatedMethods", 0),
                "totalUsages", deprecationData.getOrDefault("totalUsages", 0)
            ));
        }
        
        // Compatibility summary
        Object compatibilitySection = sections.get("API Compatibility");
        if (compatibilitySection instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> compatibilityData = (Map<String, Object>) compatibilitySection;
            summary.put("compatibility", Map.of(
                "affectedMethods", compatibilityData.getOrDefault("totalAffectedMethods", 0),
                "totalCalls", compatibilityData.getOrDefault("totalCalls", 0)
            ));
        }
        
        return summary;
    }

    public static class EnhancedJsonReport {
        private String generatedAt;
        private String reportVersion;
        private Map<String, Object> summary;
        private Map<String, Object> sections;

        public String getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        public String getReportVersion() {
            return reportVersion;
        }

        public void setReportVersion(String reportVersion) {
            this.reportVersion = reportVersion;
        }

        public Map<String, Object> getSummary() {
            return summary;
        }

        public void setSummary(Map<String, Object> summary) {
            this.summary = summary;
        }

        public Map<String, Object> getSections() {
            return sections;
        }

        public void setSections(Map<String, Object> sections) {
            this.sections = sections;
        }
    }
}
