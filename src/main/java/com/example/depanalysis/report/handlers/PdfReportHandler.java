package com.example.depanalysis.report.handlers;

import com.example.depanalysis.dto.*;
import com.example.depanalysis.report.ReportFormatHandler;
import com.example.depanalysis.util.ReportDataProcessor;
import com.example.depanalysis.util.ReportDataProcessor.VulnerabilityGroup;
import com.example.depanalysis.util.ReportDataProcessor.DeprecationGroup;
import com.example.depanalysis.util.ReportDataProcessor.CompatibilityGroup;
import com.example.depanalysis.util.SignatureFormatter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfReportHandler implements ReportFormatHandler {

    @Override
    public void generateReport(FinalReport report, String outputPath) throws IOException {
        String textContent = generateTextContent(report);
        
        Path reportsDir = Paths.get("reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        // For now, generate as text file (can be enhanced with PDF library later)
        Path pdfFile = reportsDir.resolve("dependency-analysis-report.pdf");
        Files.write(pdfFile, textContent.getBytes());
    }

    private String generateTextContent(FinalReport report) {
        StringBuilder content = new StringBuilder();
        
        // Header
        content.append("=====================================================\n");
        content.append("         DEPENDENCY ANALYSIS REPORT\n");
        content.append("=====================================================\n");
        content.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("=====================================================\n\n");

        if (report.getSections() != null) {
            for (FeatureReportSection section : report.getSections()) {
                content.append(generateSectionContent(section));
                content.append("\n");
            }
        }

        return content.toString();
    }

    private String generateSectionContent(FeatureReportSection section) {
        StringBuilder content = new StringBuilder();
        String featureName = section.getFeatureName();
        
        content.append("─────────────────────────────────────────────────────\n");
        content.append(String.format("  %s\n", featureName.toUpperCase()));
        content.append("─────────────────────────────────────────────────────\n\n");

        if ("Vulnerability Scan".equalsIgnoreCase(featureName)) {
            content.append(generateVulnerabilityContent(section.getResult()));
        } else if ("Deprecation Analysis".equalsIgnoreCase(featureName)) {
            content.append(generateDeprecationContent(section.getResult()));
        } else if ("API Compatibility".equalsIgnoreCase(featureName)) {
            content.append(generateCompatibilityContent(section.getResult()));
        } else {
            content.append(section.getResult().toString()).append("\n");
        }

        return content.toString();
    }

    private String generateVulnerabilityContent(Object result) {
        StringBuilder content = new StringBuilder();
        
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<VulnerabilityResult> vulnerabilityResults = (List<VulnerabilityResult>) result;
            
            List<VulnerabilityGroup> groups = ReportDataProcessor.groupVulnerabilitiesByDependency(vulnerabilityResults);
            
            if (groups.isEmpty()) {
                content.append("✓ No vulnerabilities found in dependencies\n\n");
            } else {
                content.append(String.format("SUMMARY:\n"));
                content.append(String.format("  • Dependencies with vulnerabilities: %d\n", groups.size()));
                content.append(String.format("  • Total vulnerabilities found: %d\n\n", 
                    groups.stream().mapToInt(VulnerabilityGroup::getVulnerabilityCount).sum()));

                content.append("DEPENDENCIES (sorted by vulnerability count):\n\n");
                
                for (int i = 0; i < groups.size(); i++) {
                    VulnerabilityGroup group = groups.get(i);
                    content.append(String.format("%d. %s\n", i + 1, group.getDependencyKey()));
                    content.append(String.format("   Group ID: %s\n", group.getGroupId()));
                    content.append(String.format("   Artifact: %s\n", group.getArtifactId()));
                    content.append(String.format("   Version: %s\n", group.getVersion()));
                    content.append(String.format("   Vulnerabilities: %d\n", group.getVulnerabilityCount()));
                    
                    if (group.getVulnerabilities() != null && !group.getVulnerabilities().isEmpty()) {
                        content.append("   \n   Vulnerability Details:\n");
                        for (VulnerabilityInfo vuln : group.getVulnerabilities()) {
                            content.append(String.format("   ▸ ID: %s\n", vuln.getId()));
                            content.append(String.format("     Summary: %s\n", vuln.getSummary()));
                            if (vuln.getSeverity() != null && !vuln.getSeverity().isEmpty()) {
                                content.append(String.format("     Severity: %s\n", vuln.getSeverity().get(0)));
                            }
                            content.append("\n");
                        }
                    }
                    content.append("\n");
                }
            }
        }
        
        return content.toString();
    }

    private String generateDeprecationContent(Object result) {
        StringBuilder content = new StringBuilder();
        
        if (result instanceof DeprecationResult) {
            DeprecationResult deprecationResult = (DeprecationResult) result;
            List<DeprecationGroup> groups = ReportDataProcessor.groupDeprecationsByMethod(deprecationResult.getDeprecationIssues());
            
            if (groups.isEmpty()) {
                content.append("✓ No deprecated methods found\n\n");
            } else {
                content.append(String.format("SUMMARY:\n"));
                content.append(String.format("  • Deprecated methods: %d\n", groups.size()));
                content.append(String.format("  • Total usages: %d\n\n", 
                    groups.stream().mapToInt(DeprecationGroup::getTotalUsageCount).sum()));

                content.append("DEPRECATED METHODS (sorted by usage count):\n\n");
                
                for (int i = 0; i < groups.size(); i++) {
                    DeprecationGroup group = groups.get(i);
                    content.append(String.format("%d. %s\n", i + 1, group.getMethodSignature()));
                    content.append(String.format("   Class: %s\n", group.getClassName()));
                    content.append(String.format("   Method: %s\n", group.getMethodName()));
                    content.append(String.format("   Usage Count: %d\n", group.getTotalUsageCount()));
                    
                    if (group.getDeprecatedSince() != null) {
                        content.append(String.format("   Deprecated Since: %s\n", group.getDeprecatedSince()));
                    }
                    if (group.getReason() != null) {
                        content.append(String.format("   Reason: %s\n", group.getReason()));
                    }
                    if (group.getSuggestedReplacement() != null) {
                        content.append(String.format("   Suggested Replacement: %s\n", group.getSuggestedReplacement()));
                    }
                    if (group.getRiskLevel() != null) {
                        content.append(String.format("   Risk Level: %s\n", group.getRiskLevel()));
                    }
                    
                    if (group.getUsageLocations() != null && !group.getUsageLocations().isEmpty()) {
                        content.append("   \n   Usage Locations:\n");
                        for (String location : group.getUsageLocations()) {
                            content.append(String.format("   ▸ %s\n", location));
                        }
                    }
                    content.append("\n");
                }
            }
        }
        
        return content.toString();
    }

    private String generateCompatibilityContent(Object result) {
        StringBuilder content = new StringBuilder();
        
        List<CompatibilityIssue> issues = ReportDataProcessor.parseCompatibilityIssues(result);
        List<CompatibilityGroup> groups = ReportDataProcessor.groupCompatibilityByMethod(issues);
        
        if (groups.isEmpty()) {
            content.append("✓ No API compatibility issues found\n\n");
        } else {
            content.append(String.format("SUMMARY:\n"));
            content.append(String.format("  • Affected methods: %d\n", groups.size()));
            content.append(String.format("  • Total calls: %d\n\n", 
                groups.stream().mapToInt(CompatibilityGroup::getTotalCallCount).sum()));

            content.append("COMPATIBILITY ISSUES (sorted by call count):\n\n");
            
            for (int i = 0; i < groups.size(); i++) {
                CompatibilityGroup group = groups.get(i);
                content.append(String.format("%d. %s\n", i + 1, group.getMethodSignature()));
                content.append(String.format("   Class: %s\n", group.getClassName()));
                content.append(String.format("   Method: %s\n", group.getMethodName()));
                content.append(String.format("   Call Count: %d\n", group.getTotalCallCount()));
                content.append(String.format("   Issue Type: %s\n", group.getIssueType()));
                content.append(String.format("   Risk Level: %s\n", group.getRiskLevel()));
                
                if (group.getExpectedSignature() != null) {
                    String formattedExpected = group.getMethodName() != null ? 
                        SignatureFormatter.formatMethodSignature(group.getMethodName(), group.getExpectedSignature()) :
                        group.getExpectedSignature();
                    content.append(String.format("   Expected Signature: %s\n", formattedExpected));
                }
                if (group.getActualSignature() != null) {
                    String formattedActual = group.getMethodName() != null ?
                        SignatureFormatter.formatMethodSignature(group.getMethodName(), group.getActualSignature()) :
                        group.getActualSignature();
                    content.append(String.format("   Actual Signature: %s\n", formattedActual));
                } else {
                    content.append("   Actual Signature: [METHOD NOT FOUND]\n");
                }
                if (group.getImpact() != null) {
                    content.append(String.format("   Impact: %s\n", group.getImpact()));
                }
                if (group.getSuggestedFix() != null) {
                    content.append(String.format("   Suggested Fix: %s\n", group.getSuggestedFix()));
                }
                
                if (group.getCallerLocations() != null && !group.getCallerLocations().isEmpty()) {
                    content.append("   \n   Caller Locations:\n");
                    for (String location : group.getCallerLocations()) {
                        content.append(String.format("   ▸ %s\n", location));
                    }
                }
                content.append("\n");
            }
        }
        
        return content.toString();
    }
}
