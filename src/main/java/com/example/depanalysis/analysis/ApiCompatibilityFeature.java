package com.example.depanalysis.analysis;

import com.example.depanalysis.dto.FeatureReportSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class ApiCompatibilityFeature implements AnalysisFeature {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCompatibilityFeature.class);
    
    // Common API compatibility issues to look for
    private static final List<CompatibilityIssue> KNOWN_ISSUES = Arrays.asList(
        // Legacy collection classes
        new CompatibilityIssue("java.util.Vector", "new Vector", "LEGACY_CLASS_USAGE", 
                               "Vector", null, "Use ArrayList instead for better performance and thread safety control", "MEDIUM"),
        new CompatibilityIssue("java.util.Hashtable", "new Hashtable", "LEGACY_CLASS_USAGE",
                               "Hashtable", null, "Use HashMap for non-synchronized access or ConcurrentHashMap for thread-safe access", "MEDIUM"),
        new CompatibilityIssue("java.util.Stack", "new Stack", "LEGACY_CLASS_USAGE",
                               "Stack", null, "Use ArrayDeque or LinkedList with Deque interface for stack operations", "MEDIUM"),
        
        // Deprecated AWT methods
        new CompatibilityIssue("java.awt.Frame", "resize", "DEPRECATED_METHOD",
                               "resize(int,int)", "setSize(int,int)", "Use setSize() method instead", "LOW"),
        new CompatibilityIssue("java.awt.Frame", "move", "DEPRECATED_METHOD",
                               "move(int,int)", "setLocation(int,int)", "Use setLocation() method instead", "LOW"),
        new CompatibilityIssue("java.awt.Frame", "show", "DEPRECATED_METHOD",
                               "show()", "setVisible(true)", "Use setVisible(true) method instead", "LOW"),
        new CompatibilityIssue("java.awt.Frame", "hide", "DEPRECATED_METHOD",
                               "hide()", "setVisible(false)", "Use setVisible(false) method instead", "LOW"),
        new CompatibilityIssue("java.awt.Component", "size", "DEPRECATED_METHOD",
                               "size()", "getSize()", "Use getSize() method instead", "LOW"),
        new CompatibilityIssue("java.awt.Component", "location", "DEPRECATED_METHOD",
                               "location()", "getLocation()", "Use getLocation() method instead", "LOW"),
        new CompatibilityIssue("java.awt.Component", "bounds", "DEPRECATED_METHOD",
                               "bounds()", "getBounds()", "Use getBounds() method instead", "LOW"),
        new CompatibilityIssue("java.awt.Component", "inside", "DEPRECATED_METHOD",
                               "inside(int,int)", "contains(int,int)", "Use contains() method instead", "LOW"),
        
        // Apache Commons and external library issues
        new CompatibilityIssue("org.apache.commons.logging.Log", "trace", "METHOD_NOT_FOUND", 
                               "(Ljava/lang/Object;)V", null, "Use debug() method instead or upgrade to newer version", "HIGH"),
        new CompatibilityIssue("org.apache.http.client.HttpClient", "execute", "METHOD_SIGNATURE_CHANGED",
                               "(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;",
                               "(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                               "Add HttpContext parameter or use different overload", "MEDIUM"),
        new CompatibilityIssue("org.apache.commons.lang.StringUtils", "isEmpty", "CLASS_NOT_FOUND",
                               "(Ljava/lang/String;)Z", null, "Use org.apache.commons.lang3.StringUtils instead", "HIGH"),
        new CompatibilityIssue("javax.xml.bind.DatatypeConverter", "parseBase64Binary", "METHOD_NOT_FOUND",
                               "(Ljava/lang/String;)[B", null, "Use java.util.Base64.getDecoder().decode() instead", "MEDIUM")
    );

    @Override
    public FeatureReportSection analyze(Path projectDir) {
        logger.info("Starting API compatibility analysis for project: {}", projectDir);
        
        Map<String, Object> compatibilityData = new HashMap<>();
        List<Map<String, Object>> sections = new ArrayList<>();
        
        Map<String, Object> section = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> compatibilityResult = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        
        try {
            // Find all Java files and analyze them
            try (Stream<Path> paths = Files.walk(projectDir)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                     .forEach(javaFile -> analyzeJavaFile(javaFile, projectDir, issues));
            }
        } catch (IOException e) {
            logger.error("Error walking project directory: {}", e.getMessage());
        }
        
        if (issues.isEmpty()) {
            logger.info("No API compatibility issues found in project");
            // Leave issues empty - this will be handled properly by the report generator
        }
        
        compatibilityResult.put("compatibilityIssues", issues);
        results.add(compatibilityResult);
        section.put("result", results);
        sections.add(section);
        compatibilityData.put("sections", sections);
        
        logger.info("API compatibility analysis completed. Found {} issues", issues.size());
        return new FeatureReportSection(getFeatureName(), compatibilityData);
    }
    
    private void analyzeJavaFile(Path javaFile, Path projectRoot, List<Map<String, Object>> issues) {
        try {
            String content = Files.readString(javaFile);
            String[] lines = content.split("\n");
            String relativePath = projectRoot.relativize(javaFile).toString();
            
            for (CompatibilityIssue knownIssue : KNOWN_ISSUES) {
                findCompatibilityIssues(knownIssue, lines, relativePath, issues);
            }
            
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", javaFile, e.getMessage());
        }
    }
    
    private void findCompatibilityIssues(CompatibilityIssue knownIssue, String[] lines, String filePath, List<Map<String, Object>> issues) {
        String searchPattern;
        Pattern pattern;
        
        // Different patterns for different issue types
        if (knownIssue.issueType.equals("LEGACY_CLASS_USAGE")) {
            // Look for "new Vector", "new Hashtable", "new Stack"
            searchPattern = "new\\s+" + knownIssue.methodName.replace("new ", "") + "\\s*[<(]";
            pattern = Pattern.compile(searchPattern);
        } else {
            // Look for method calls
            searchPattern = "\\." + knownIssue.methodName + "\\s*\\(";
            pattern = Pattern.compile(searchPattern);
        }
        
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
                // For legacy class usage, check if it's the right class type
                if (knownIssue.issueType.equals("LEGACY_CLASS_USAGE")) {
                    String line = lines[i];
                    String simpleClassName = knownIssue.className.substring(knownIssue.className.lastIndexOf('.') + 1);
                    if (!line.contains(simpleClassName)) {
                        continue; // Skip if class doesn't match
                    }
                } else {
                    // For method calls, check for specific class usage if needed
                    String line = lines[i];
                    if (knownIssue.className.contains(".") && !line.contains(knownIssue.className.substring(knownIssue.className.lastIndexOf('.') + 1))) {
                        continue; // Skip if class doesn't match
                    }
                }
                
                Map<String, Object> issue = new HashMap<>();
                issue.put("issueType", knownIssue.issueType);
                issue.put("className", knownIssue.className);
                issue.put("methodName", knownIssue.methodName);
                issue.put("expectedSignature", knownIssue.expectedSignature);
                issue.put("actualSignature", knownIssue.actualSignature);
                issue.put("usageLocation", filePath + ":" + (i + 1));
                issue.put("suggestedFix", knownIssue.suggestedFix);
                issue.put("riskLevel", knownIssue.riskLevel);
                issue.put("impact", getImpactMessage(knownIssue.issueType));
                
                issues.add(issue);
            }
        }
    }
    
    private String getImpactMessage(String issueType) {
        switch (issueType) {
            case "METHOD_NOT_FOUND":
                return "NoSuchMethodError at runtime";
            case "CLASS_NOT_FOUND":
                return "ClassNotFoundException at runtime";
            case "METHOD_SIGNATURE_CHANGED":
                return "Compilation error or runtime exception";
            case "LEGACY_CLASS_USAGE":
                return "Performance and maintainability issues";
            case "DEPRECATED_METHOD":
                return "May be removed in future versions";
            default:
                return "Potential runtime issues";
        }
    }

    @Override
    public String getFeatureName() {
        return "API Compatibility";
    }
    
    private static class CompatibilityIssue {
        final String className;
        final String methodName;
        final String issueType;
        final String expectedSignature;
        final String actualSignature;
        final String suggestedFix;
        final String riskLevel;
        
        CompatibilityIssue(String className, String methodName, String issueType, 
                          String expectedSignature, String actualSignature, 
                          String suggestedFix, String riskLevel) {
            this.className = className;
            this.methodName = methodName;
            this.issueType = issueType;
            this.expectedSignature = expectedSignature;
            this.actualSignature = actualSignature;
            this.suggestedFix = suggestedFix;
            this.riskLevel = riskLevel;
        }
    }
}