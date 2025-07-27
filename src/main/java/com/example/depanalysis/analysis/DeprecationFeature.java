package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.DeprecationIssue;
import com.example.depanalysis.dto.DeprecationResult;
import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class DeprecationFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(DeprecationFeature.class);
    
    // Common deprecated methods to look for
    private static final List<DeprecatedMethod> DEPRECATED_METHODS = Arrays.asList(
        new DeprecatedMethod("java.util.Date", "getYear", "JDK 1.1", "Use java.time.LocalDate.getYear() or Calendar.get(Calendar.YEAR)", "HIGH"),
        new DeprecatedMethod("java.util.Date", "getMonth", "JDK 1.1", "Use java.time.LocalDate.getMonthValue() or Calendar.get(Calendar.MONTH)", "HIGH"),
        new DeprecatedMethod("java.util.Date", "getDay", "JDK 1.1", "Use java.time.LocalDate.getDayOfWeek() or Calendar.get(Calendar.DAY_OF_WEEK)", "HIGH"),
        new DeprecatedMethod("java.net.URL", "encode", "JDK 1.3", "Use java.net.URLEncoder.encode(String, String)", "MEDIUM"),
        new DeprecatedMethod("java.lang.Thread", "stop", "JDK 1.2", "Use interrupt() and proper thread coordination", "CRITICAL"),
        new DeprecatedMethod("java.lang.Thread", "suspend", "JDK 1.2", "Use wait() and notify() or other synchronization", "CRITICAL"),
        new DeprecatedMethod("java.lang.Thread", "resume", "JDK 1.2", "Use wait() and notify() or other synchronization", "CRITICAL"),
        new DeprecatedMethod("javax.swing.JComponent", "enable", "JDK 1.1", "Use setEnabled(true) instead", "LOW"),
        new DeprecatedMethod("javax.swing.JComponent", "disable", "JDK 1.1", "Use setEnabled(false) instead", "LOW"),
        new DeprecatedMethod("java.lang.Integer", "valueOf", "JDK 9", "Still available but consider using Integer.parseInt() for primitives", "LOW")
    );

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting deprecation analysis for project: {}", projectDir);
        
        List<DeprecationIssue> issues = new ArrayList<>();
        
        try {
            // Find all Java files
            try (Stream<Path> paths = Files.walk(projectDir)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                     .forEach(javaFile -> analyzeJavaFile(javaFile, projectDir, issues));
            }
        } catch (IOException e) {
            logger.error("Error walking project directory: {}", e.getMessage());
        }
        
        if (issues.isEmpty()) {
            logger.info("No deprecation issues found in project");
            // Leave issues empty - this will be handled properly by the report generator
        }
        
        DeprecationResult result = new DeprecationResult();
        result.setDeprecationIssues(issues);
        
        logger.info("Deprecation analysis completed. Found {} issues", issues.size());
        return new FeatureReportSection(getFeatureName(), result);
    }
    
    private void analyzeJavaFile(Path javaFile, Path projectRoot, List<DeprecationIssue> issues) {
        try {
            String content = Files.readString(javaFile);
            String[] lines = content.split("\n");
            String relativePath = projectRoot.relativize(javaFile).toString();
            
            for (DeprecatedMethod deprecatedMethod : DEPRECATED_METHODS) {
                findDeprecatedUsage(deprecatedMethod, lines, relativePath, issues);
            }
            
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", javaFile, e.getMessage());
        }
    }
    
    private void findDeprecatedUsage(DeprecatedMethod deprecatedMethod, String[] lines, String filePath, List<DeprecationIssue> issues) {
        String methodPattern = "\\." + deprecatedMethod.methodName + "\\s*\\(";
        Pattern pattern = Pattern.compile(methodPattern);
        
        List<String> usageLocations = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
                usageLocations.add(filePath + ":" + (i + 1));
            }
        }
        
        if (!usageLocations.isEmpty()) {
            // Check if we already have an issue for this method
            DeprecationIssue existingIssue = issues.stream()
                .filter(issue -> issue.getClassName().equals(deprecatedMethod.className) && 
                               issue.getMethodName().equals(deprecatedMethod.methodName))
                .findFirst()
                .orElse(null);
                
            if (existingIssue != null) {
                // Add to existing issue
                List<String> locations = new ArrayList<>(existingIssue.getUsageLocations());
                locations.addAll(usageLocations);
                existingIssue.setUsageLocations(locations);
            } else {
                // Create new issue
                DeprecationIssue issue = new DeprecationIssue();
                issue.setClassName(deprecatedMethod.className);
                issue.setMethodName(deprecatedMethod.methodName);
                issue.setDeprecatedSince(deprecatedMethod.deprecatedSince);
                issue.setReason(deprecatedMethod.reason);
                issue.setSuggestedReplacement(deprecatedMethod.suggestedReplacement);
                issue.setRiskLevel(deprecatedMethod.riskLevel);
                issue.setUsageLocations(usageLocations);
                issues.add(issue);
            }
        }
    }

    @Override
    public String getFeatureName() {
        return "Deprecation Analysis";
    }
    
    private static class DeprecatedMethod {
        final String className;
        final String methodName;
        final String deprecatedSince;
        final String reason;
        final String suggestedReplacement;
        final String riskLevel;
        
        DeprecatedMethod(String className, String methodName, String deprecatedSince, String reason, String riskLevel) {
            this.className = className;
            this.methodName = methodName;
            this.deprecatedSince = deprecatedSince;
            this.reason = reason;
            this.suggestedReplacement = reason; // Using reason as suggested replacement for simplicity
            this.riskLevel = riskLevel;
        }
    }
}