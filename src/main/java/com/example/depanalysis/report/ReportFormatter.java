package com.example.depanalysis.report;

import com.example.depanalysis.dto.VulnerabilityResult;
import com.example.depanalysis.dto.VulnerabilityInfo;

import java.util.List;

public class ReportFormatter {

    public static String generateReport(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        for (FeatureReportSection section : report.getSections()) {
            sb.append("=== ").append(section.getFeatureName()).append(" ===\n\n");
            if ("Vulnerability Scan".equalsIgnoreCase(section.getFeatureName())) {
                @SuppressWarnings("unchecked")
                List<VulnerabilityResult> vulnResults = (List<VulnerabilityResult>) section.getResult();
                sb.append(formatVulnerabilitySection(vulnResults));
            } else {
                sb.append(section.getResult().toString()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String formatVulnerabilitySection(List<VulnerabilityResult> results) {
        StringBuilder sb = new StringBuilder();
        if (results == null || results.isEmpty()) {
            sb.append("No dependencies or JARs analyzed.\n");
            return sb.toString();
        }
        for (VulnerabilityResult vr : results) {
            sb.append("Dependency: ")
              .append(vr.getGroupId()).append(":")
              .append(vr.getArtifactId()).append(":")
              .append(vr.getVersion()).append("\n");
            sb.append("Source: ").append(vr.getJarName()).append("\n");

            List<VulnerabilityInfo> vulnerabilities = vr.getVulnerabilities();
            if (vulnerabilities == null || vulnerabilities.isEmpty()) {
                sb.append("  No known vulnerabilities found.\n\n");
            } else {
                sb.append("  Vulnerabilities:\n");
                for (VulnerabilityInfo vuln : vulnerabilities) {
                    sb.append("    - ID: ").append(vuln.getId()).append("\n");
                    if (vuln.getSummary() != null)
                        sb.append("      Summary: ").append(vuln.getSummary()).append("\n");
                    if (vuln.getDetails() != null)
                        sb.append("      Details: ").append(vuln.getDetails()).append("\n");
                    if (vuln.getAliases() != null && !vuln.getAliases().isEmpty())
                        sb.append("      Aliases: ").append(String.join(", ", vuln.getAliases())).append("\n");
                    if (vuln.getReferences() != null && !vuln.getReferences().isEmpty()) {
                        sb.append("      References:\n");
                        for (String ref : vuln.getReferences())
                            sb.append("        * ").append(ref).append("\n");
                    }
                    if (vuln.getSeverity() != null && !vuln.getSeverity().isEmpty())
                        sb.append("      Severity: ").append(String.join(", ", vuln.getSeverity())).append("\n");
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}