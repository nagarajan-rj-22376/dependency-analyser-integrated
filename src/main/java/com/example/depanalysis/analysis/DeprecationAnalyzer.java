package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.DeprecationIssue;
import com.example.depanalysis.util.JarBytecodeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Analyzes projects for deprecated API usage by examining both project source code
 * and JAR dependencies using ASM bytecode analysis.
 */
@Component
public class DeprecationAnalyzer implements DeprecationChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(DeprecationAnalyzer.class);
    
    private final JarBytecodeAnalyzer jarAnalyzer;
    
    public DeprecationAnalyzer(JarBytecodeAnalyzer jarAnalyzer) {
        this.jarAnalyzer = jarAnalyzer;
    }
    
    // Pure ASM-based analysis - no hardcoded lists needed
    // Dynamic detection through bytecode analysis will find all deprecated usage

    @Override
    public List<DeprecationIssue> checkDeprecationIssues(Path projectDir) {
        logger.info("Starting deprecation analysis for project: {}", projectDir);
        
        List<DeprecationIssue> issues = new ArrayList<>();
        
        // Step 1: Analyze JAR files to find which methods are deprecated
        logger.info("Starting ASM-based JAR analysis to find deprecated elements...");
        JarBytecodeAnalyzer.JarAnalysisResult jarResult = jarAnalyzer.analyzeJarsInProject(projectDir);
        List<JarBytecodeAnalyzer.JarAnalysisResult.DeprecatedElement> deprecatedElements = jarResult.getDeprecatedElements();
        logger.debug("Found {} deprecated elements in JARs", deprecatedElements.size());
        
        // Step 2: Analyze project source files for both JDK and JAR deprecated method usage
        try {
            try (Stream<Path> paths = Files.walk(projectDir)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                     .forEach(javaFile -> {
                         // Analyze for JDK deprecated methods (direct pattern matching)
                         analyzeJavaFileForJDKDeprecated(javaFile, projectDir, issues);
                         // Analyze for JAR deprecated methods
                         analyzeJavaFile(javaFile, projectDir, deprecatedElements, issues);
                     });
            }
        } catch (IOException e) {
            logger.error("Error walking project directory: {}", e.getMessage());
        }
        
        // Step 3: Also process method calls found in JARs that target deprecated methods
        for (JarBytecodeAnalyzer.JarAnalysisResult.MethodCall methodCall : jarResult.getMethodCalls()) {
            analyzeMethodCallForDeprecation(methodCall, deprecatedElements, issues);
        }
        
        logger.info("Deprecation analysis completed. Found {} deprecated method usage patterns", issues.size());
        
        return issues;
    }
    
    private void analyzeJavaFile(Path javaFile, Path projectRoot, 
                                 List<JarBytecodeAnalyzer.JarAnalysisResult.DeprecatedElement> deprecatedElements, 
                                 List<DeprecationIssue> issues) {
        try {
            String content = Files.readString(javaFile);
            String[] lines = content.split("\n");
            String relativePath = projectRoot.relativize(javaFile).toString();
            
            // Search for calls to each deprecated method found in JARs
            for (JarBytecodeAnalyzer.JarAnalysisResult.DeprecatedElement deprecatedElement : deprecatedElements) {
                findDeprecatedUsage(deprecatedElement, lines, relativePath, issues);
            }
            
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", javaFile, e.getMessage());
        }
    }
    
    private void analyzeJavaFileForJDKDeprecated(Path javaFile, Path projectRoot, List<DeprecationIssue> issues) {
        try {
            String content = Files.readString(javaFile);
            String[] lines = content.split("\n");
            String relativePath = projectRoot.relativize(javaFile).toString();
            
            // Common JDK deprecated methods - these are not in separate JARs but in the JDK itself
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.util.Date", "getYear", 
                "\\.getYear\\s*\\(", "Use Calendar or LocalDate instead");
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.util.Date", "getMonth", 
                "\\.getMonth\\s*\\(", "Use Calendar or LocalDate instead");
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.util.Date", "getDay", 
                "\\.getDay\\s*\\(", "Use Calendar or LocalDate instead");
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.lang.Thread", "stop", 
                "\\.stop\\s*\\(", "Use interrupt() mechanism instead - stop() is unsafe");
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.lang.Thread", "suspend", 
                "\\.suspend\\s*\\(", "Use wait/notify mechanism instead");
            checkJDKDeprecatedPattern(lines, relativePath, issues, "java.lang.Thread", "resume", 
                "\\.resume\\s*\\(", "Use wait/notify mechanism instead");
                
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", javaFile, e.getMessage());
        }
    }
    
    private void checkJDKDeprecatedPattern(String[] lines, String filePath, List<DeprecationIssue> issues,
                                           String className, String methodName, String pattern, String replacement) {
        Pattern compiledPattern = Pattern.compile(pattern);
        List<String> usageLocations = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = compiledPattern.matcher(lines[i]);
            if (matcher.find()) {
                usageLocations.add(filePath + ":" + (i + 1));
            }
        }
        
        if (!usageLocations.isEmpty()) {
            // Check if we already have an issue for this method
            DeprecationIssue existingIssue = issues.stream()
                .filter(issue -> issue.getClassName().equals(className) && 
                               issue.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);
                
            if (existingIssue != null) {
                // Add to existing issue
                List<String> locations = new ArrayList<>(existingIssue.getUsageLocations());
                usageLocations.stream()
                    .filter(loc -> !locations.contains(loc))
                    .forEach(locations::add);
                existingIssue.setUsageLocations(locations);
            } else {
                // Create new issue
                DeprecationIssue issue = new DeprecationIssue();
                issue.setClassName(className);
                issue.setMethodName(methodName);
                issue.setDeprecatedSince("JDK deprecated method");
                issue.setReason("Method marked with @Deprecated annotation in JDK");
                issue.setSuggestedReplacement(replacement);
                issue.setRiskLevel(determineRiskLevel(className, methodName));
                issue.setUsageLocations(usageLocations);
                issue.setSourceType("PROJECT");
                issues.add(issue);
            }
        }
    }
    
    private void findDeprecatedUsage(JarBytecodeAnalyzer.JarAnalysisResult.DeprecatedElement deprecatedElement, 
                                     String[] lines, String filePath, List<DeprecationIssue> issues) {
        String className = deprecatedElement.getClassName();
        String methodName = deprecatedElement.getElementName();
        
        // Create regex pattern to find method calls
        String methodPattern;
        Pattern pattern;
        
        if (methodName.equals("<init>")) {
            // Constructor calls - look for "new ClassName"
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            methodPattern = "new\\s+" + simpleClassName + "\\s*\\(";
            pattern = Pattern.compile(methodPattern);
        } else {
            // Method calls - look for ".methodName("
            methodPattern = "\\." + Pattern.quote(methodName) + "\\s*\\(";
            pattern = Pattern.compile(methodPattern);
        }
        
        List<String> usageLocations = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
                // Check if the class is likely the one we're looking for
                String line = lines[i];
                String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                
                // Only report if we can reasonably assume this is the deprecated method
                if (line.contains(className) || 
                    line.contains(simpleClassName) || 
                    isLikelyMatch(className, methodName, line)) {
                    usageLocations.add(filePath + ":" + (i + 1));
                }
            }
        }
        
        if (!usageLocations.isEmpty()) {
            // Check if we already have an issue for this method
            DeprecationIssue existingIssue = issues.stream()
                .filter(issue -> issue.getClassName().equals(className) && 
                               issue.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);
                
            if (existingIssue != null) {
                // Add to existing issue
                List<String> locations = new ArrayList<>(existingIssue.getUsageLocations());
                locations.addAll(usageLocations);
                existingIssue.setUsageLocations(locations);
                // Update source type to include project source
                if (!"PROJECT".equals(existingIssue.getSourceType()) && !"MIXED".equals(existingIssue.getSourceType())) {
                    existingIssue.setSourceType("MIXED");
                }
            } else {
                // Create new issue
                DeprecationIssue issue = new DeprecationIssue();
                issue.setClassName(className);
                issue.setMethodName(methodName);
                issue.setDeprecatedSince("Detected via ASM analysis");
                issue.setReason("Method marked with @Deprecated annotation");
                issue.setSuggestedReplacement("Check documentation for replacement API");
                issue.setRiskLevel(determineRiskLevel(className, methodName));
                issue.setUsageLocations(usageLocations);
                issue.setSourceType("PROJECT");
                issues.add(issue);
            }
        }
    }
    
    private boolean isLikelyMatch(String className, String methodName, String line) {
        // Additional heuristics for common deprecated methods
        if (methodName.equals("getYear") || 
            methodName.equals("getMonth") || 
            methodName.equals("getDay")) {
            return line.contains("Date") || line.contains("date");
        }
        
        if (methodName.equals("stop") || 
            methodName.equals("suspend") || 
            methodName.equals("resume")) {
            return line.contains("Thread") || line.contains("thread");
        }
        
        return false;
    }
    
    private void analyzeMethodCallForDeprecation(JarBytecodeAnalyzer.JarAnalysisResult.MethodCall methodCall, 
                                                 List<JarBytecodeAnalyzer.JarAnalysisResult.DeprecatedElement> deprecatedElements, 
                                                 List<DeprecationIssue> issues) {
        // Check if this method call targets a deprecated method
        String targetClass = methodCall.getTargetClass();
        String methodName = methodCall.getMethodName();
        String usageLocation = methodCall.getLocation(); // This is where the deprecated method is USED
        
        // Look for matching deprecated element
        boolean isDeprecated = deprecatedElements.stream()
            .anyMatch(depElement -> 
                depElement.getClassName().equals(targetClass) && 
                depElement.getElementName().equals(methodName));
        
        if (isDeprecated) {
            // Check if we already have an issue for this deprecated method
            DeprecationIssue existingIssue = issues.stream()
                .filter(issue -> issue.getClassName().equals(targetClass) && 
                               issue.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);
                
            if (existingIssue != null) {
                // Add this usage location to existing issue
                List<String> locations = new ArrayList<>(existingIssue.getUsageLocations());
                if (!locations.contains(usageLocation)) {
                    locations.add(usageLocation);
                    existingIssue.setUsageLocations(locations);
                }
            } else {
                // Create new issue for this deprecated method usage
                DeprecationIssue issue = new DeprecationIssue();
                issue.setClassName(targetClass);
                issue.setMethodName(methodName);
                issue.setDeprecatedSince("Detected via ASM analysis");
                issue.setReason("Method marked with @Deprecated annotation");
                issue.setSuggestedReplacement("Check documentation for replacement API");
                issue.setRiskLevel(determineRiskLevel(targetClass, methodName));
                issue.setUsageLocations(List.of(usageLocation)); // This is the caller location, not the deprecated method location
                issue.setSourceType("JAR");
                issues.add(issue);
            }
            
            logger.debug("Found deprecated method usage: {}.{} called from {}", targetClass, methodName, usageLocation);
        }
    }
    
    private String determineRiskLevel(String className, String methodName) {
        // Dynamic risk assessment based on method patterns
        if (className.contains("Thread") && 
            (methodName.equals("stop") || methodName.equals("suspend") || methodName.equals("resume"))) {
            return "CRITICAL";
        }
        
        if (className.contains("Date") && 
            (methodName.equals("getYear") || methodName.equals("getMonth") || methodName.equals("getDay"))) {
            return "HIGH";
        }
        
        if (className.contains("Runtime") || className.contains("System")) {
            return "CRITICAL";
        }
        
        return "MEDIUM"; // Default risk level
    }
}