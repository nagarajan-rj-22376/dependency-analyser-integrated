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
public class HtmlReportHandler implements ReportFormatHandler {

    @Override
    public void generateReport(FinalReport report, String outputPath) throws IOException {
        String htmlContent = generateHtmlContent(report);
        
        Path reportsDir = Paths.get("reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        Path htmlFile = reportsDir.resolve("dependency-analysis-report.html");
        Files.write(htmlFile, htmlContent.getBytes());
    }

    private String generateHtmlContent(FinalReport report) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>Dependency Analysis Report</title>\n")
            .append("    <style>\n")
            .append(getCssStyles())
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"container\">\n")
            .append("        <header class=\"report-header\">\n")
            .append("            <h1>🔍 Dependency Analysis Report</h1>\n")
            .append("            <div class=\"metadata\">\n")
            .append("                <span class=\"timestamp\">Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</span>\n")
            .append("            </div>\n")
            .append("        </header>\n");

        if (report.getSections() != null && !report.getSections().isEmpty()) {
            // Generate tab navigation
            html.append("        <div class=\"tab-container\">\n")
                .append("            <div class=\"tab-navigation\">\n");
            
            for (int i = 0; i < report.getSections().size(); i++) {
                FeatureReportSection section = report.getSections().get(i);
                String featureName = section.getFeatureName();
                String activeClass = i == 0 ? "active" : "";
                String tabId = getTabId(featureName);
                
                html.append("                <button class=\"tab-button ").append(activeClass).append("\" ")
                    .append("data-tab=\"").append(tabId).append("\" ")
                    .append("onclick=\"switchTab('").append(tabId).append("')\">\n")
                    .append("                    ").append(getFeatureIcon(featureName)).append(" ").append(featureName).append("\n")
                    .append("                </button>\n");
            }
            
            html.append("            </div>\n")
                .append("            <div class=\"tab-content\">\n");
            
            // Generate tab content
            for (int i = 0; i < report.getSections().size(); i++) {
                FeatureReportSection section = report.getSections().get(i);
                String featureName = section.getFeatureName();
                String activeClass = i == 0 ? "active" : "";
                String tabId = getTabId(featureName);
                
                html.append("                <div class=\"tab-panel ").append(activeClass).append("\" id=\"").append(tabId).append("\">\n")
                    .append("                    <section class=\"analysis-section\">\n")
                    .append("                        <h2 class=\"section-title\">").append(getFeatureIcon(featureName)).append(" ").append(featureName).append("</h2>\n");
                
                if ("Vulnerability Scan".equalsIgnoreCase(featureName)) {
                    html.append(generateVulnerabilitySection(section.getResult()));
                } else if ("Deprecation Analysis".equalsIgnoreCase(featureName)) {
                    html.append(generateDeprecationSection(section.getResult()));
                } else if ("API Compatibility".equalsIgnoreCase(featureName)) {
                    html.append(generateCompatibilitySection(section.getResult()));
                } else {
                    html.append("                        <div class=\"content-card\">\n")
                        .append("                            <pre>").append(escapeHtml(section.getResult().toString())).append("</pre>\n")
                        .append("                        </div>\n");
                }
                
                html.append("                    </section>\n")
                    .append("                </div>\n");
            }
            
            html.append("            </div>\n")
                .append("        </div>\n");
        }

        html.append("    </div>\n")
            .append("    <script>\n")
            .append(getJavaScript())
            .append("    </script>\n")
            .append("</body>\n")
            .append("</html>");

        return html.toString();
    }

    private String getTabId(String featureName) {
        return featureName.toLowerCase()
                         .replace(" ", "-")
                         .replace("&", "and")
                         .replaceAll("[^a-z0-9-]", "");
    }

    private String generateSectionHtml(FeatureReportSection section) {
        StringBuilder html = new StringBuilder();
        String featureName = section.getFeatureName();
        
        html.append("        <section class=\"analysis-section\">\n")
            .append("            <h2 class=\"section-title\">").append(getFeatureIcon(featureName)).append(" ").append(featureName).append("</h2>\n");

        if ("Vulnerability Scan".equalsIgnoreCase(featureName)) {
            html.append(generateVulnerabilitySection(section.getResult()));
        } else if ("Deprecation Analysis".equalsIgnoreCase(featureName)) {
            html.append(generateDeprecationSection(section.getResult()));
        } else if ("API Compatibility".equalsIgnoreCase(featureName)) {
            html.append(generateCompatibilitySection(section.getResult()));
        } else {
            html.append("            <div class=\"content-card\">\n")
                .append("                <pre>").append(escapeHtml(section.getResult().toString())).append("</pre>\n")
                .append("            </div>\n");
        }

        html.append("        </section>\n");
        return html.toString();
    }

    private String generateVulnerabilitySection(Object result) {
        StringBuilder html = new StringBuilder();
        
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<VulnerabilityResult> vulnerabilityResults = (List<VulnerabilityResult>) result;
            
            // Group vulnerabilities by dependency and sort by count
            List<VulnerabilityGroup> groups = ReportDataProcessor.groupVulnerabilitiesByDependency(vulnerabilityResults);
            
            if (groups.isEmpty()) {
                html.append("            <div class=\"content-card success\">\n")
                    .append("                <p class=\"no-issues\">✅ No vulnerabilities found in dependencies</p>\n")
                    .append("            </div>\n");
            } else {
                html.append("            <div class=\"summary-stats\">\n")
                    .append("                <div class=\"stat-card\">\n")
                    .append("                    <div class=\"stat-number\">").append(groups.size()).append("</div>\n")
                    .append("                    <div class=\"stat-label\">Dependencies with Vulnerabilities</div>\n")
                    .append("                </div>\n")
                    .append("                <div class=\"stat-card\">\n")
                    .append("                    <div class=\"stat-number\">").append(groups.stream().mapToInt(VulnerabilityGroup::getVulnerabilityCount).sum()).append("</div>\n")
                    .append("                    <div class=\"stat-label\">Total Vulnerabilities</div>\n")
                    .append("                </div>\n")
                    .append("            </div>\n");

                for (VulnerabilityGroup group : groups) {
                    html.append(generateVulnerabilityGroupCard(group));
                }
            }
        }
        
        return html.toString();
    }

    private String generateVulnerabilityGroupCard(VulnerabilityGroup group) {
        StringBuilder html = new StringBuilder();
        String severityClass = group.getVulnerabilityCount() > 5 ? "critical" : 
                              group.getVulnerabilityCount() > 2 ? "high" : "medium";
        
        html.append("            <div class=\"vulnerability-group-card ").append(severityClass).append("\">\n")
            .append("                <div class=\"group-header\">\n")
            .append("                    <h3 class=\"dependency-name\">").append(escapeHtml(group.getDependencyKey())).append("</h3>\n")
            .append("                    <div class=\"vulnerability-count\">").append(group.getVulnerabilityCount()).append(" vulnerabilities</div>\n")
            .append("                </div>\n")
            .append("                <div class=\"dependency-details\">\n")
            .append("                    <span class=\"detail-item\">📦 ").append(escapeHtml(group.getGroupId())).append(":").append(escapeHtml(group.getArtifactId())).append("</span>\n")
            .append("                    <span class=\"detail-item\">🏷️ Version: ").append(escapeHtml(group.getVersion())).append("</span>\n")
            .append("                </div>\n")
            .append("                <div class=\"vulnerabilities-list\">\n");

        if (group.getVulnerabilities() != null) {
            for (VulnerabilityInfo vuln : group.getVulnerabilities()) {
                html.append("                    <div class=\"vulnerability-item\">\n")
                    .append("                        <div class=\"vuln-header\">\n")
                    .append("                            <span class=\"vuln-id\">").append(escapeHtml(vuln.getId())).append("</span>\n");
                
                if (vuln.getSeverity() != null && !vuln.getSeverity().isEmpty()) {
                    html.append("                            <span class=\"severity-badge\">").append(escapeHtml(vuln.getSeverity().get(0))).append("</span>\n");
                }
                
                html.append("                        </div>\n")
                    .append("                        <p class=\"vuln-summary\">").append(escapeHtml(vuln.getSummary())).append("</p>\n");
                
                if (vuln.getReferences() != null && !vuln.getReferences().isEmpty()) {
                    html.append("                        <div class=\"vuln-references\">\n")
                        .append("                            <span class=\"references-label\">References:</span>\n");
                    for (String ref : vuln.getReferences()) {
                        html.append("                            <a href=\"").append(escapeHtml(ref)).append("\" target=\"_blank\">").append(escapeHtml(ref)).append("</a>\n");
                    }
                    html.append("                        </div>\n");
                }
                
                html.append("                    </div>\n");
            }
        }
        
        html.append("                </div>\n")
            .append("            </div>\n");
        
        return html.toString();
    }

    private String generateDeprecationSection(Object result) {
        StringBuilder html = new StringBuilder();
        
        if (result instanceof DeprecationResult) {
            DeprecationResult deprecationResult = (DeprecationResult) result;
            List<DeprecationGroup> groups = ReportDataProcessor.groupDeprecationsByMethod(deprecationResult.getDeprecationIssues());
            
            if (groups.isEmpty()) {
                html.append("            <div class=\"content-card success\">\n")
                    .append("                <p class=\"no-issues\">✅ No deprecated methods found</p>\n")
                    .append("            </div>\n");
            } else {
                html.append("            <div class=\"summary-stats\">\n")
                    .append("                <div class=\"stat-card\">\n")
                    .append("                    <div class=\"stat-number\">").append(groups.size()).append("</div>\n")
                    .append("                    <div class=\"stat-label\">Deprecated Methods</div>\n")
                    .append("                </div>\n")
                    .append("                <div class=\"stat-card\">\n")
                    .append("                    <div class=\"stat-number\">").append(groups.stream().mapToInt(DeprecationGroup::getTotalUsageCount).sum()).append("</div>\n")
                    .append("                    <div class=\"stat-label\">Total Usages</div>\n")
                    .append("                </div>\n")
                    .append("            </div>\n");

                for (DeprecationGroup group : groups) {
                    html.append(generateDeprecationGroupCard(group));
                }
            }
        }
        
        return html.toString();
    }

    private String generateDeprecationGroupCard(DeprecationGroup group) {
        StringBuilder html = new StringBuilder();
        String severityClass = group.getTotalUsageCount() > 10 ? "critical" : 
                              group.getTotalUsageCount() > 5 ? "high" : "medium";
        
        html.append("            <div class=\"deprecation-group-card ").append(severityClass).append("\">\n")
            .append("                <div class=\"group-header\">\n")
            .append("                    <h3 class=\"method-signature\">").append(escapeHtml(group.getMethodSignature())).append("</h3>\n")
            .append("                    <div class=\"usage-count\">").append(group.getTotalUsageCount()).append(" usages</div>\n")
            .append("                </div>\n")
            .append("                <div class=\"deprecation-details\">\n");
        
        if (group.getDeprecatedSince() != null) {
            html.append("                    <span class=\"detail-item\">📅 Deprecated since: ").append(escapeHtml(group.getDeprecatedSince())).append("</span>\n");
        }
        if (group.getReason() != null) {
            html.append("                    <p class=\"deprecation-reason\">💭 Reason: ").append(escapeHtml(group.getReason())).append("</p>\n");
        }
        if (group.getSuggestedReplacement() != null) {
            html.append("                    <p class=\"suggested-replacement\">💡 Suggested replacement: ").append(escapeHtml(group.getSuggestedReplacement())).append("</p>\n");
        }
        
        html.append("                </div>\n")
            .append("                <div class=\"usage-locations\">\n")
            .append("                    <h4>Usage Locations:</h4>\n")
            .append("                    <div class=\"locations-list\">\n");
        
        if (group.getUsageLocations() != null) {
            for (String location : group.getUsageLocations()) {
                html.append("                        <div class=\"location-item\">📍 ").append(escapeHtml(location)).append("</div>\n");
            }
        }
        
        html.append("                    </div>\n")
            .append("                </div>\n")
            .append("            </div>\n");
        
        return html.toString();
    }

    private String generateCompatibilitySection(Object result) {
        StringBuilder html = new StringBuilder();
        
        // Parse compatibility issues from the nested JSON structure
        List<CompatibilityIssue> issues = ReportDataProcessor.parseCompatibilityIssues(result);
        List<CompatibilityGroup> groups = ReportDataProcessor.groupCompatibilityByMethod(issues);
        
        if (groups.isEmpty()) {
            html.append("            <div class=\"content-card success\">\n")
                .append("                <p class=\"no-issues\">✅ No API compatibility issues found</p>\n")
                .append("            </div>\n");
        } else {
            html.append("            <div class=\"summary-stats\">\n")
                .append("                <div class=\"stat-card\">\n")
                .append("                    <div class=\"stat-number\">").append(groups.size()).append("</div>\n")
                .append("                    <div class=\"stat-label\">Affected Methods</div>\n")
                .append("                </div>\n")
                .append("                <div class=\"stat-card\">\n")
                .append("                    <div class=\"stat-number\">").append(groups.stream().mapToInt(CompatibilityGroup::getTotalCallCount).sum()).append("</div>\n")
                .append("                    <div class=\"stat-label\">Total Calls</div>\n")
                .append("                </div>\n")
                .append("            </div>\n");

            for (CompatibilityGroup group : groups) {
                html.append(generateCompatibilityGroupCard(group));
            }
        }
        
        return html.toString();
    }

    private String generateCompatibilityGroupCard(CompatibilityGroup group) {
        StringBuilder html = new StringBuilder();
        String severityClass = "HIGH".equalsIgnoreCase(group.getRiskLevel()) ? "critical" : 
                              "MEDIUM".equalsIgnoreCase(group.getRiskLevel()) ? "high" : "medium";
        
        // Use the already formatted signature from the group
        String formattedSignature = group.getMethodSignature();
        
        html.append("            <div class=\"compatibility-group-card ").append(severityClass).append("\">\n")
            .append("                <div class=\"group-header\">\n")
            .append("                    <h3 class=\"method-signature\">").append(escapeHtml(formattedSignature)).append("</h3>\n")
            .append("                    <div class=\"call-count\">").append(group.getTotalCallCount()).append(" calls</div>\n")
            .append("                </div>\n")
            .append("                <div class=\"compatibility-details\">\n")
            .append("                    <div class=\"issue-type\">⚠️ Issue: ").append(escapeHtml(group.getIssueType())).append("</div>\n");
        
        if (group.getExpectedSignature() != null) {
            // Format the expected signature for display
            String formattedExpected = group.getMethodName() != null ? 
                SignatureFormatter.formatMethodSignature(group.getMethodName(), group.getExpectedSignature()) :
                group.getExpectedSignature();
            
            html.append("                    <div class=\"signature-details\">\n")
                .append("                        <p><strong>Expected:</strong> ").append(escapeHtml(formattedExpected)).append("</p>\n");
            
            if (group.getActualSignature() != null) {
                String formattedActual = group.getMethodName() != null ?
                    SignatureFormatter.formatMethodSignature(group.getMethodName(), group.getActualSignature()) :
                    group.getActualSignature();
                html.append("                        <p><strong>Actual:</strong> ").append(escapeHtml(formattedActual)).append("</p>\n");
            } else {
                html.append("                        <p><strong>Actual:</strong> <span class=\"missing\">Method not found</span></p>\n");
            }
            html.append("                    </div>\n");
        }
        
        if (group.getImpact() != null) {
            html.append("                    <p class=\"impact\">💥 Impact: ").append(escapeHtml(group.getImpact())).append("</p>\n");
        }
        if (group.getSuggestedFix() != null) {
            html.append("                    <p class=\"suggested-fix\">🔧 Suggested fix: ").append(escapeHtml(group.getSuggestedFix())).append("</p>\n");
        }
        
        html.append("                </div>\n")
            .append("                <div class=\"caller-locations\">\n")
            .append("                    <h4>Caller Locations:</h4>\n")
            .append("                    <div class=\"locations-list\">\n");
        
        if (group.getCallerLocations() != null) {
            for (String location : group.getCallerLocations()) {
                html.append("                        <div class=\"location-item\">📍 ").append(escapeHtml(location)).append("</div>\n");
            }
        }
        
        html.append("                    </div>\n")
            .append("                </div>\n")
            .append("            </div>\n");
        
        return html.toString();
    }

    private String getFeatureIcon(String featureName) {
        switch (featureName.toLowerCase()) {
            case "vulnerability scan": return "🛡️";
            case "deprecation analysis": return "⚠️";
            case "api compatibility": return "🔗";
            default: return "📊";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private String getCssStyles() {
        return "* {\n" +
               "    margin: 0;\n" +
               "    padding: 0;\n" +
               "    box-sizing: border-box;\n" +
               "}\n" +
               "\n" +
               "body {\n" +
               "    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
               "    line-height: 1.6;\n" +
               "    color: #333;\n" +
               "    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
               "    min-height: 100vh;\n" +
               "}\n" +
               "\n" +
               ".container {\n" +
               "    max-width: 1200px;\n" +
               "    margin: 0 auto;\n" +
               "    padding: 20px;\n" +
               "}\n" +
               "\n" +
               ".report-header {\n" +
               "    background: rgba(255, 255, 255, 0.95);\n" +
               "    border-radius: 15px;\n" +
               "    padding: 30px;\n" +
               "    margin-bottom: 30px;\n" +
               "    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);\n" +
               "    text-align: center;\n" +
               "}\n" +
               "\n" +
               ".report-header h1 {\n" +
               "    color: #2c3e50;\n" +
               "    font-size: 2.5em;\n" +
               "    margin-bottom: 10px;\n" +
               "    font-weight: 700;\n" +
               "}\n" +
               "\n" +
               ".metadata {\n" +
               "    color: #7f8c8d;\n" +
               "    font-size: 1.1em;\n" +
               "}\n" +
               "\n" +
               ".tab-container {\n" +
               "    background: rgba(255, 255, 255, 0.95);\n" +
               "    border-radius: 15px;\n" +
               "    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);\n" +
               "    overflow: hidden;\n" +
               "}\n" +
               "\n" +
               ".tab-navigation {\n" +
               "    display: flex;\n" +
               "    background: #f8f9fa;\n" +
               "    border-bottom: 1px solid #e9ecef;\n" +
               "    overflow-x: auto;\n" +
               "}\n" +
               "\n" +
               ".tab-button {\n" +
               "    background: none;\n" +
               "    border: none;\n" +
               "    padding: 20px 30px;\n" +
               "    font-size: 1.1em;\n" +
               "    font-weight: 600;\n" +
               "    color: #6c757d;\n" +
               "    cursor: pointer;\n" +
               "    transition: all 0.3s ease;\n" +
               "    border-bottom: 3px solid transparent;\n" +
               "    white-space: nowrap;\n" +
               "    position: relative;\n" +
               "}\n" +
               "\n" +
               ".tab-button:hover {\n" +
               "    color: #495057;\n" +
               "    background: rgba(0, 0, 0, 0.05);\n" +
               "}\n" +
               "\n" +
               ".tab-button.active {\n" +
               "    color: #2c3e50;\n" +
               "    background: white;\n" +
               "    border-bottom-color: #e74c3c;\n" +
               "    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);\n" +
               "}\n" +
               "\n" +
               ".tab-content {\n" +
               "    min-height: 400px;\n" +
               "}\n" +
               "\n" +
               ".tab-panel {\n" +
               "    display: none;\n" +
               "    padding: 30px;\n" +
               "    animation: fadeIn 0.3s ease-in;\n" +
               "}\n" +
               "\n" +
               ".tab-panel.active {\n" +
               "    display: block;\n" +
               "}\n" +
               "\n" +
               "@keyframes fadeIn {\n" +
               "    from {\n" +
               "        opacity: 0;\n" +
               "        transform: translateY(10px);\n" +
               "    }\n" +
               "    to {\n" +
               "        opacity: 1;\n" +
               "        transform: translateY(0);\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               ".analysis-section {\n" +
               "    background: rgba(255, 255, 255, 0.95);\n" +
               "    border-radius: 15px;\n" +
               "    padding: 30px;\n" +
               "    margin-bottom: 30px;\n" +
               "    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);\n" +
               "}\n" +
               "\n" +
               ".section-title {\n" +
               "    color: #2c3e50;\n" +
               "    font-size: 1.8em;\n" +
               "    margin-bottom: 20px;\n" +
               "    padding-bottom: 10px;\n" +
               "    border-bottom: 3px solid #e74c3c;\n" +
               "}\n" +
               "\n" +
               ".summary-stats {\n" +
               "    display: grid;\n" +
               "    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
               "    gap: 20px;\n" +
               "    margin-bottom: 30px;\n" +
               "}\n" +
               "\n" +
               ".stat-card {\n" +
               "    background: linear-gradient(135deg, #3498db, #2980b9);\n" +
               "    color: white;\n" +
               "    padding: 20px;\n" +
               "    border-radius: 10px;\n" +
               "    text-align: center;\n" +
               "    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);\n" +
               "}\n" +
               "\n" +
               ".stat-number {\n" +
               "    font-size: 2em;\n" +
               "    font-weight: bold;\n" +
               "    margin-bottom: 5px;\n" +
               "}\n" +
               "\n" +
               ".stat-label {\n" +
               "    font-size: 0.9em;\n" +
               "    opacity: 0.9;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-group-card,\n" +
               ".deprecation-group-card,\n" +
               ".compatibility-group-card {\n" +
               "    background: white;\n" +
               "    border-radius: 10px;\n" +
               "    padding: 20px;\n" +
               "    margin-bottom: 20px;\n" +
               "    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);\n" +
               "    border-left: 5px solid #3498db;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-group-card.critical,\n" +
               ".deprecation-group-card.critical,\n" +
               ".compatibility-group-card.critical {\n" +
               "    border-left-color: #e74c3c;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-group-card.high,\n" +
               ".deprecation-group-card.high,\n" +
               ".compatibility-group-card.high {\n" +
               "    border-left-color: #f39c12;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-group-card.medium,\n" +
               ".deprecation-group-card.medium,\n" +
               ".compatibility-group-card.medium {\n" +
               "    border-left-color: #f1c40f;\n" +
               "}\n" +
               "\n" +
               ".group-header {\n" +
               "    display: flex;\n" +
               "    justify-content: space-between;\n" +
               "    align-items: center;\n" +
               "    margin-bottom: 15px;\n" +
               "    padding-bottom: 15px;\n" +
               "    border-bottom: 1px solid #ecf0f1;\n" +
               "}\n" +
               "\n" +
               ".dependency-name,\n" +
               ".method-signature {\n" +
               "    color: #2c3e50;\n" +
               "    font-size: 1.3em;\n" +
               "    font-weight: 600;\n" +
               "    margin: 0;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-count,\n" +
               ".usage-count,\n" +
               ".call-count {\n" +
               "    background: #e74c3c;\n" +
               "    color: white;\n" +
               "    padding: 5px 15px;\n" +
               "    border-radius: 20px;\n" +
               "    font-weight: bold;\n" +
               "    font-size: 0.9em;\n" +
               "}\n" +
               "\n" +
               ".dependency-details,\n" +
               ".deprecation-details,\n" +
               ".compatibility-details {\n" +
               "    margin-bottom: 15px;\n" +
               "}\n" +
               "\n" +
               ".detail-item {\n" +
               "    display: inline-block;\n" +
               "    margin-right: 20px;\n" +
               "    margin-bottom: 10px;\n" +
               "    color: #7f8c8d;\n" +
               "    font-size: 0.9em;\n" +
               "}\n" +
               "\n" +
               ".vulnerabilities-list,\n" +
               ".usage-locations,\n" +
               ".caller-locations {\n" +
               "    margin-top: 15px;\n" +
               "}\n" +
               "\n" +
               ".vulnerability-item {\n" +
               "    background: #f8f9fa;\n" +
               "    border-radius: 8px;\n" +
               "    padding: 15px;\n" +
               "    margin-bottom: 15px;\n" +
               "    border-left: 3px solid #e74c3c;\n" +
               "}\n" +
               "\n" +
               ".vuln-header {\n" +
               "    display: flex;\n" +
               "    justify-content: space-between;\n" +
               "    align-items: center;\n" +
               "    margin-bottom: 10px;\n" +
               "}\n" +
               "\n" +
               ".vuln-id {\n" +
               "    font-weight: bold;\n" +
               "    color: #2c3e50;\n" +
               "    font-family: monospace;\n" +
               "}\n" +
               "\n" +
               ".severity-badge {\n" +
               "    background: #e74c3c;\n" +
               "    color: white;\n" +
               "    padding: 3px 10px;\n" +
               "    border-radius: 15px;\n" +
               "    font-size: 0.8em;\n" +
               "    font-weight: bold;\n" +
               "}\n" +
               "\n" +
               ".vuln-summary {\n" +
               "    color: #555;\n" +
               "    margin-bottom: 10px;\n" +
               "}\n" +
               "\n" +
               ".vuln-references {\n" +
               "    font-size: 0.9em;\n" +
               "}\n" +
               "\n" +
               ".vuln-references a {\n" +
               "    color: #3498db;\n" +
               "    text-decoration: none;\n" +
               "    margin-right: 10px;\n" +
               "}\n" +
               "\n" +
               ".vuln-references a:hover {\n" +
               "    text-decoration: underline;\n" +
               "}\n" +
               "\n" +
               ".locations-list {\n" +
               "    max-height: 200px;\n" +
               "    overflow-y: auto;\n" +
               "    background: #f8f9fa;\n" +
               "    border-radius: 8px;\n" +
               "    padding: 15px;\n" +
               "}\n" +
               "\n" +
               ".location-item {\n" +
               "    padding: 5px 0;\n" +
               "    border-bottom: 1px solid #ecf0f1;\n" +
               "    font-family: monospace;\n" +
               "    font-size: 0.9em;\n" +
               "    color: #555;\n" +
               "}\n" +
               "\n" +
               ".location-item:last-child {\n" +
               "    border-bottom: none;\n" +
               "}\n" +
               "\n" +
               ".deprecation-reason,\n" +
               ".suggested-replacement,\n" +
               ".impact,\n" +
               ".suggested-fix {\n" +
               "    background: #f8f9fa;\n" +
               "    padding: 10px;\n" +
               "    border-radius: 5px;\n" +
               "    margin: 10px 0;\n" +
               "    border-left: 3px solid #3498db;\n" +
               "}\n" +
               "\n" +
               ".signature-details {\n" +
               "    background: #f8f9fa;\n" +
               "    padding: 15px;\n" +
               "    border-radius: 8px;\n" +
               "    margin: 10px 0;\n" +
               "    font-family: monospace;\n" +
               "    font-size: 0.9em;\n" +
               "}\n" +
               "\n" +
               ".signature-details .missing {\n" +
               "    color: #e74c3c;\n" +
               "    font-weight: bold;\n" +
               "}\n" +
               "\n" +
               ".issue-type {\n" +
               "    background: #fff3cd;\n" +
               "    color: #856404;\n" +
               "    padding: 10px;\n" +
               "    border-radius: 5px;\n" +
               "    margin: 10px 0;\n" +
               "    border: 1px solid #ffeaa7;\n" +
               "}\n" +
               "\n" +
               ".content-card {\n" +
               "    background: white;\n" +
               "    border-radius: 10px;\n" +
               "    padding: 20px;\n" +
               "    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);\n" +
               "}\n" +
               "\n" +
               ".content-card.success {\n" +
               "    border-left: 5px solid #27ae60;\n" +
               "    background: #d5f4e6;\n" +
               "}\n" +
               "\n" +
               ".no-issues {\n" +
               "    color: #27ae60;\n" +
               "    font-weight: bold;\n" +
               "    font-size: 1.1em;\n" +
               "    text-align: center;\n" +
               "}\n" +
               "\n" +
               "pre {\n" +
               "    background: #f8f9fa;\n" +
               "    border: 1px solid #e9ecef;\n" +
               "    border-radius: 5px;\n" +
               "    padding: 15px;\n" +
               "    overflow-x: auto;\n" +
               "    white-space: pre-wrap;\n" +
               "    word-wrap: break-word;\n" +
               "}\n" +
               "\n" +
               "@media (max-width: 768px) {\n" +
               "    .container {\n" +
               "        padding: 10px;\n" +
               "    }\n" +
               "    \n" +
               "    .tab-navigation {\n" +
               "        flex-direction: column;\n" +
               "    }\n" +
               "    \n" +
               "    .tab-button {\n" +
               "        text-align: center;\n" +
               "        padding: 15px 20px;\n" +
               "        border-bottom: 1px solid #e9ecef;\n" +
               "        border-right: none;\n" +
               "    }\n" +
               "    \n" +
               "    .tab-button.active {\n" +
               "        border-bottom: 1px solid #e9ecef;\n" +
               "        border-left: 3px solid #e74c3c;\n" +
               "    }\n" +
               "    \n" +
               "    .tab-panel {\n" +
               "        padding: 20px 15px;\n" +
               "    }\n" +
               "    \n" +
               "    .group-header {\n" +
               "        flex-direction: column;\n" +
               "        align-items: flex-start;\n" +
               "    }\n" +
               "    \n" +
               "    .vulnerability-count,\n" +
               "    .usage-count,\n" +
               "    .call-count {\n" +
               "        margin-top: 10px;\n" +
               "    }\n" +
               "}";
    }

    private String getJavaScript() {
        return "// Tab switching functionality\n" +
               "function switchTab(tabId) {\n" +
               "    // Hide all tab panels\n" +
               "    const panels = document.querySelectorAll('.tab-panel');\n" +
               "    panels.forEach(panel => panel.classList.remove('active'));\n" +
               "    \n" +
               "    // Remove active class from all tab buttons\n" +
               "    const buttons = document.querySelectorAll('.tab-button');\n" +
               "    buttons.forEach(button => button.classList.remove('active'));\n" +
               "    \n" +
               "    // Show selected tab panel\n" +
               "    const targetPanel = document.getElementById(tabId);\n" +
               "    if (targetPanel) {\n" +
               "        targetPanel.classList.add('active');\n" +
               "    }\n" +
               "    \n" +
               "    // Add active class to clicked button\n" +
               "    const targetButton = document.querySelector(`[data-tab=\"${tabId}\"]`);\n" +
               "    if (targetButton) {\n" +
               "        targetButton.classList.add('active');\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "// Enhanced report functionality\n" +
               "document.addEventListener('DOMContentLoaded', function() {\n" +
               "    // Add click handlers for expandable sections\n" +
               "    const groupCards = document.querySelectorAll('.vulnerability-group-card, .deprecation-group-card, .compatibility-group-card');\n" +
               "    \n" +
               "    groupCards.forEach(card => {\n" +
               "        const locationsSection = card.querySelector('.vulnerabilities-list, .usage-locations, .caller-locations');\n" +
               "        if (locationsSection) {\n" +
               "            const header = card.querySelector('.group-header');\n" +
               "            header.style.cursor = 'pointer';\n" +
               "            header.addEventListener('click', () => {\n" +
               "                locationsSection.style.display = locationsSection.style.display === 'none' ? 'block' : 'none';\n" +
               "            });\n" +
               "        }\n" +
               "    });\n" +
               "    \n" +
               "    // Add smooth animations on load\n" +
               "    const sections = document.querySelectorAll('.analysis-section');\n" +
               "    sections.forEach((section, index) => {\n" +
               "        section.style.opacity = '0';\n" +
               "        section.style.transform = 'translateY(20px)';\n" +
               "        setTimeout(() => {\n" +
               "            section.style.transition = 'all 0.5s ease';\n" +
               "            section.style.opacity = '1';\n" +
               "            section.style.transform = 'translateY(0)';\n" +
               "        }, index * 200);\n" +
               "    });\n" +
               "    \n" +
               "    // Add keyboard navigation for tabs\n" +
               "    document.addEventListener('keydown', function(e) {\n" +
               "        if (e.ctrlKey && e.key >= '1' && e.key <= '9') {\n" +
               "            e.preventDefault();\n" +
               "            const tabIndex = parseInt(e.key) - 1;\n" +
               "            const buttons = document.querySelectorAll('.tab-button');\n" +
               "            if (buttons[tabIndex]) {\n" +
               "                buttons[tabIndex].click();\n" +
               "            }\n" +
               "        }\n" +
               "    });\n" +
               "});";
    }
}
