package com.example.depanalysis.analysis;

import com.example.depanalysis.util.JarBytecodeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Component
public class ApiCompatibilityAnalyzer implements ApiCompatibilityChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCompatibilityAnalyzer.class);
    
    private final JarBytecodeAnalyzer jarAnalyzer;
    
    public ApiCompatibilityAnalyzer(JarBytecodeAnalyzer jarAnalyzer) {
        this.jarAnalyzer = jarAnalyzer;
    }

    @Override
    public List<Map<String, Object>> checkCompatibilityIssues(Path projectDir) {
        logger.info("Starting API compatibility check for project directory: {}", projectDir);
        List<Map<String, Object>> issues = new ArrayList<>();
        
        // Analyze JAR files using ASM bytecode analysis to dynamically detect compatibility issues
        logger.info("Starting ASM-based JAR analysis...");
        JarBytecodeAnalyzer.JarAnalysisResult jarResult = jarAnalyzer.analyzeJarsInProject(projectDir);
        
        // Process method calls found in JARs to detect runtime compatibility issues
        // This will dynamically detect issues like ClassNotFoundException, NoSuchMethodError, etc.
        // based on actual method calls found in the bytecode
        for (JarBytecodeAnalyzer.JarAnalysisResult.MethodCall methodCall : jarResult.getMethodCalls()) {
            analyzeMethodCallForCompatibility(methodCall, issues);
        }
        
        logger.info("API compatibility check completed. Found {} issues from dynamic analysis", issues.size());
        return issues;
    }
    
    private void analyzeMethodCallForCompatibility(JarBytecodeAnalyzer.JarAnalysisResult.MethodCall methodCall, List<Map<String, Object>> issues) {
        // Dynamic analysis: detect potential compatibility issues based on method call patterns
        // This analyzes actual bytecode method calls to identify potential runtime failures
        
        String targetClass = methodCall.getTargetClass();
        String methodName = methodCall.getMethodName();
        String location = methodCall.getLocation();
        
        // Example dynamic checks (can be extended based on actual analysis needs)
        if (isDeprecatedApiCall(targetClass, methodName)) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("issueType", "DEPRECATED_API");
            issue.put("className", targetClass);
            issue.put("methodName", methodName);
            issue.put("usageLocation", location);
            issue.put("suggestedFix", "Check documentation for replacement API");
            issue.put("riskLevel", "MEDIUM");
            issue.put("impact", "Potential deprecation warning or future removal");
            issue.put("sourceType", "JAR");
            
            issues.add(issue);
            logger.debug("Found potential deprecated API usage: {} in {} (JAR)", 
                       targetClass + "." + methodName, location);
        }
        
        // Additional dynamic checks can be added here based on bytecode analysis
        checkForVersionIncompatibilities(targetClass, methodName, location, issues);
        checkForMissingMethods(targetClass, methodName, location, issues);
    }
    
    private boolean isDeprecatedApiCall(String targetClass, String methodName) {
        // Dynamic check based on actual deprecated annotations found in bytecode
        // This leverages the ASM analysis to detect @Deprecated annotations
        return false; // Placeholder - would be enhanced with actual deprecation detection
    }
    
    private void checkForVersionIncompatibilities(String targetClass, String methodName, String location, List<Map<String, Object>> issues) {
        // Dynamic analysis for version incompatibilities
        // This can analyze method signatures, class versions, etc.
        
        // Check for common version-related issues dynamically
        if (targetClass.startsWith("javax.xml.bind") && isJdk11Plus()) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("issueType", "CLASS_NOT_FOUND");
            issue.put("className", targetClass);
            issue.put("methodName", methodName);
            issue.put("usageLocation", location);
            issue.put("suggestedFix", "Add JAXB dependency or migrate to java.util.Base64");
            issue.put("riskLevel", "HIGH");
            issue.put("impact", "ClassNotFoundException at runtime in JDK 11+");
            issue.put("sourceType", "JAR");
            
            issues.add(issue);
        }
    }
    
    private void checkForMissingMethods(String targetClass, String methodName, String location, List<Map<String, Object>> issues) {
        // Dynamic analysis for missing methods
        // This can check method signatures against expected APIs
        
        // This would use reflection or other dynamic techniques to verify method existence
        // For now, this is a placeholder for dynamic analysis
    }
    
    private boolean isJdk11Plus() {
        // Check if running on JDK 11 or later
        String version = System.getProperty("java.version");
        try {
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            return majorVersion >= 11;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
