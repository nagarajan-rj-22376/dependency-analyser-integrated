package com.example.depanalysis.controller;

import com.example.depanalysis.dto.*;
import com.example.depanalysis.report.ReportGenerator;
import com.example.depanalysis.report.ReportFormat;
import com.example.depanalysis.service.AnalysisOrchestrator;
import com.example.depanalysis.util.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportGenerator reportGenerator;
    
    @Autowired
    private AnalysisOrchestrator analysisOrchestrator;

    @GetMapping("/sample/{format}")
    public ResponseEntity<String> generateSampleReport(@PathVariable String format) {
        try {
            FinalReport sampleReport = createSampleReport();
            
            ReportFormat reportFormat;
            try {
                reportFormat = ReportFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid format. Supported formats: " + Arrays.toString(ReportFormat.values()));
            }
            
            reportGenerator.generateReport(sampleReport, reportFormat, "reports/");
            return ResponseEntity.ok("Sample " + format.toUpperCase() + " report generated successfully in reports/ directory");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating report: " + e.getMessage());
        }
    }

    @GetMapping("/sample/all")
    public ResponseEntity<String> generateSampleReports() {
        try {
            FinalReport sampleReport = createSampleReport();
            List<ReportFormat> formats = Arrays.asList(ReportFormat.values());
            
            reportGenerator.generateReports(sampleReport, formats, "reports/");
            return ResponseEntity.ok("Sample reports generated in all formats successfully in reports/ directory");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating reports: " + e.getMessage());
        }
    }

    @GetMapping("/formats")
    public ResponseEntity<List<String>> getSupportedFormats() {
        return ResponseEntity.ok(reportGenerator.getSupportedFormats());
    }

    // New endpoints for uploading files and generating reports
    @PostMapping("/upload/{format}")
    public ResponseEntity<String> generateReportFromUpload(@PathVariable String format, @RequestParam("file") MultipartFile file) {
        try {
            // Validate format parameter (even though we generate both)
            try {
                ReportFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid format. Supported formats: " + Arrays.toString(ReportFormat.values()));
            }
            
            FinalReport finalReport = analyzeUploadedFile(file);
            
            // Always generate both HTML and JSON for the results page
            reportGenerator.generateReport(finalReport, ReportFormat.HTML, "reports/");
            reportGenerator.generateReport(finalReport, ReportFormat.JSON, "reports/");
            
            return ResponseEntity.ok("Report generated successfully from uploaded file in reports/ directory");
        } catch (Exception e) {
            logger.error("Error generating report from upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    @PostMapping("/upload/all")
    public ResponseEntity<String> generateAllReportsFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            FinalReport finalReport = analyzeUploadedFile(file);
            List<ReportFormat> formats = Arrays.asList(ReportFormat.values());
            
            reportGenerator.generateReports(finalReport, formats, "reports/");
            return ResponseEntity.ok("All format reports generated successfully from uploaded file in reports/ directory");
        } catch (Exception e) {
            logger.error("Error generating reports from upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    private FinalReport analyzeUploadedFile(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".zip") && !fileName.toLowerCase().endsWith(".war"))) {
            throw new IllegalArgumentException("Only ZIP and WAR files are supported");
        }

        // Extract ZIP file to temporary directory
        Path extractedDir = null;
        
        try {
            extractedDir = ZipExtractor.extractZip(file);
            
            // Perform analysis
            return analysisOrchestrator.analyzeAll(extractedDir);
        } finally {
            // Clean up temporary directory
            if (extractedDir != null) {
                try {
                    Files.walk(extractedDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    logger.warn("Failed to delete temporary file: " + path, e);
                                }
                            });
                } catch (IOException e) {
                    logger.warn("Failed to clean up temporary directory: " + extractedDir, e);
                }
            }
        }
    }

    private FinalReport createSampleReport() {
        FinalReport report = new FinalReport();
        List<FeatureReportSection> sections = new ArrayList<>();

        // Create Vulnerability Scan section
        sections.add(new FeatureReportSection("Vulnerability Scan", createSampleVulnerabilityData()));
        
        // Create Deprecation Analysis section
        sections.add(new FeatureReportSection("Deprecation Analysis", createSampleDeprecationData()));
        
        // Create API Compatibility section  
        sections.add(new FeatureReportSection("API Compatibility", createSampleCompatibilityData()));

        report.setSections(sections);
        return report;
    }

    private List<VulnerabilityResult> createSampleVulnerabilityData() {
        List<VulnerabilityResult> results = new ArrayList<>();

        // High vulnerability dependency
        VulnerabilityResult highVulnResult = new VulnerabilityResult();
        highVulnResult.setJarName("log4j-core-2.14.1.jar");
        highVulnResult.setGroupId("org.apache.logging.log4j");
        highVulnResult.setArtifactId("log4j-core");
        highVulnResult.setVersion("2.14.1");
        
        List<VulnerabilityInfo> highVulns = new ArrayList<>();
        
        VulnerabilityInfo cve1 = new VulnerabilityInfo();
        cve1.setId("CVE-2021-44228");
        cve1.setSummary("Apache Log4j2 JNDI features do not protect against attacker controlled LDAP and other JNDI related endpoints.");
        cve1.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H 10.0"));
        cve1.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2021-44228"));
        cve1.setAliases(Arrays.asList("GHSA-jfh8-c2jp-5v3q"));
        highVulns.add(cve1);
        
        VulnerabilityInfo cve2 = new VulnerabilityInfo();
        cve2.setId("CVE-2021-45046");
        cve2.setSummary("Incomplete fix for CVE-2021-44228 in certain non-default configurations.");
        cve2.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H 9.0"));
        cve2.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2021-45046"));
        highVulns.add(cve2);
        
        VulnerabilityInfo cve3 = new VulnerabilityInfo();
        cve3.setId("CVE-2021-45105");
        cve3.setSummary("Apache Log4j2 versions 2.0-alpha1 through 2.16.0 did not protect from uncontrolled recursion from self-referential lookups.");
        cve3.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H 5.9"));
        cve3.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2021-45105"));
        highVulns.add(cve3);
        
        highVulnResult.setVulnerabilities(highVulns);
        results.add(highVulnResult);

        // Medium vulnerability dependency
        VulnerabilityResult mediumVulnResult = new VulnerabilityResult();
        mediumVulnResult.setJarName("jackson-databind-2.9.8.jar");
        mediumVulnResult.setGroupId("com.fasterxml.jackson.core");
        mediumVulnResult.setArtifactId("jackson-databind");
        mediumVulnResult.setVersion("2.9.8");
        
        List<VulnerabilityInfo> mediumVulns = new ArrayList<>();
        
        VulnerabilityInfo cve4 = new VulnerabilityInfo();
        cve4.setId("CVE-2020-25649");
        cve4.setSummary("A flaw was found in jackson-databind that could allow an unauthenticated user to perform code execution.");
        cve4.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H 7.5"));
        cve4.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2020-25649"));
        mediumVulns.add(cve4);
        
        VulnerabilityInfo cve5 = new VulnerabilityInfo();
        cve5.setId("CVE-2020-24616");
        cve5.setSummary("FasterXML jackson-databind 2.x before 2.9.10.6 mishandles the interaction between serialization gadgets and typing.");
        cve5.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H 8.1"));
        cve5.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2020-24616"));
        mediumVulns.add(cve5);
        
        mediumVulnResult.setVulnerabilities(mediumVulns);
        results.add(mediumVulnResult);

        // Low vulnerability dependency
        VulnerabilityResult lowVulnResult = new VulnerabilityResult();
        lowVulnResult.setJarName("commons-io-2.6.jar");
        lowVulnResult.setGroupId("commons-io");
        lowVulnResult.setArtifactId("commons-io");
        lowVulnResult.setVersion("2.6");
        
        List<VulnerabilityInfo> lowVulns = new ArrayList<>();
        
        VulnerabilityInfo cve6 = new VulnerabilityInfo();
        cve6.setId("CVE-2021-29425");
        cve6.setSummary("Apache Commons IO before 2.7 is vulnerable to directory traversal.");
        cve6.setSeverity(Arrays.asList("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:H/A:N 4.8"));
        cve6.setReferences(Arrays.asList("https://nvd.nist.gov/vuln/detail/CVE-2021-29425"));
        lowVulns.add(cve6);
        
        lowVulnResult.setVulnerabilities(lowVulns);
        results.add(lowVulnResult);

        // Clean dependency (no vulnerabilities)
        VulnerabilityResult cleanResult = new VulnerabilityResult();
        cleanResult.setJarName("gson-2.8.9.jar");
        cleanResult.setGroupId("com.google.code.gson");
        cleanResult.setArtifactId("gson");
        cleanResult.setVersion("2.8.9");
        cleanResult.setVulnerabilities(new ArrayList<>());
        results.add(cleanResult);

        return results;
    }

    private DeprecationResult createSampleDeprecationData() {
        List<DeprecationIssue> issues = new ArrayList<>();

        // High usage deprecated method
        DeprecationIssue issue1 = new DeprecationIssue();
        issue1.setClassName("java.util.Date");
        issue1.setMethodName("getYear");
        issue1.setDeprecatedSince("JDK 1.1");
        issue1.setReason("Replaced by Calendar.get(Calendar.YEAR) - 1900");
        issue1.setSuggestedReplacement("Use java.time.LocalDate.getYear() or Calendar.get(Calendar.YEAR)");
        issue1.setRiskLevel("HIGH");
        issue1.setUsageLocations(Arrays.asList(
            "com.example.service.DateService.formatDate:45",
            "com.example.util.TimeUtils.calculateAge:23",
            "com.example.controller.ReportController.generateTimestamp:67",
            "com.example.model.UserProfile.getCreationYear:89",
            "com.example.service.AuditService.logActivity:156",
            "com.example.util.DateConverter.convertToString:34",
            "com.example.service.AnalyticsService.getYearlyReport:201",
            "com.example.controller.UserController.validateBirthYear:78",
            "com.example.service.NotificationService.scheduleAnnual:123",
            "com.example.util.CalendarHelper.extractYear:12",
            "com.example.service.BackupService.createYearlyBackup:89",
            "com.example.controller.AdminController.generateReport:45"
        ));
        issues.add(issue1);

        // Medium usage deprecated method
        DeprecationIssue issue2 = new DeprecationIssue();
        issue2.setClassName("java.net.URL");
        issue2.setMethodName("encode");
        issue2.setDeprecatedSince("JDK 1.3");
        issue2.setReason("The resulting string may vary depending on the platform's default encoding");
        issue2.setSuggestedReplacement("Use java.net.URLEncoder.encode(String, String)");
        issue2.setRiskLevel("MEDIUM");
        issue2.setUsageLocations(Arrays.asList(
            "com.example.service.ApiService.buildUrl:78",
            "com.example.util.UrlHelper.encodeParameters:45",
            "com.example.controller.WebhookController.processCallback:123",
            "com.example.service.IntegrationService.createRequest:234",
            "com.example.util.HttpUtils.formatUrl:89",
            "com.example.service.ExternalApiService.sendRequest:167",
            "com.example.controller.ProxyController.forwardRequest:56"
        ));
        issues.add(issue2);

        // Low usage deprecated method
        DeprecationIssue issue3 = new DeprecationIssue();
        issue3.setClassName("java.lang.Thread");
        issue3.setMethodName("stop");
        issue3.setDeprecatedSince("JDK 1.2");
        issue3.setReason("This method is inherently unsafe and can cause deadlocks");
        issue3.setSuggestedReplacement("Use interrupt() and proper thread coordination");
        issue3.setRiskLevel("CRITICAL");
        issue3.setUsageLocations(Arrays.asList(
            "com.example.service.TaskManager.terminateTask:234",
            "com.example.util.ThreadPool.shutdown:45",
            "com.example.service.BackgroundService.forceStop:78"
        ));
        issues.add(issue3);

        // Single usage deprecated method
        DeprecationIssue issue4 = new DeprecationIssue();
        issue4.setClassName("javax.swing.JComponent");
        issue4.setMethodName("enable");
        issue4.setDeprecatedSince("JDK 1.1");
        issue4.setReason("As of JDK version 1.1, replaced by setEnabled(boolean)");
        issue4.setSuggestedReplacement("Use setEnabled(true) instead");
        issue4.setRiskLevel("LOW");
        issue4.setUsageLocations(Arrays.asList(
            "com.example.ui.MainFrame.initializeComponents:123"
        ));
        issues.add(issue4);

        DeprecationResult result = new DeprecationResult();
        result.setDeprecationIssues(issues);
        return result;
    }

    private Object createSampleCompatibilityData() {
        // Simulate the nested JSON structure that would come from the API compatibility analysis
        Map<String, Object> compatibilityData = new HashMap<>();
        List<Map<String, Object>> sections = new ArrayList<>();
        
        Map<String, Object> section = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> compatibilityResult = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        
        // High call count compatibility issue
        Map<String, Object> issue1 = new HashMap<>();
        issue1.put("issueType", "METHOD_NOT_FOUND");
        issue1.put("className", "org.apache.commons.logging.Log");
        issue1.put("methodName", "trace");
        issue1.put("expectedSignature", "(Ljava/lang/Object;)V");
        issue1.put("actualSignature", null);
        issue1.put("usageLocation", "com.example.service.LoggingService.logTrace");
        issue1.put("suggestedFix", "Use debug() method instead or upgrade to newer version");
        issue1.put("riskLevel", "HIGH");
        issue1.put("impact", "NoSuchMethodError at runtime");
        issues.add(issue1);
        
        // Duplicate the issue to simulate multiple calls to same method
        for (int i = 0; i < 15; i++) {
            Map<String, Object> duplicateIssue = new HashMap<>(issue1);
            duplicateIssue.put("usageLocation", "com.example.service.LoggingService.logTrace:" + (i + 50));
            issues.add(duplicateIssue);
        }
        
        // Medium call count compatibility issue
        Map<String, Object> issue2 = new HashMap<>();
        issue2.put("issueType", "METHOD_SIGNATURE_CHANGED");
        issue2.put("className", "org.apache.http.client.HttpClient");
        issue2.put("methodName", "execute");
        issue2.put("expectedSignature", "(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;");
        issue2.put("actualSignature", "(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;");
        issue2.put("usageLocation", "com.example.service.HttpService.makeRequest");
        issue2.put("suggestedFix", "Add HttpContext parameter or use different overload");
        issue2.put("riskLevel", "MEDIUM");
        issue2.put("impact", "Compilation error or runtime exception");
        
        for (int i = 0; i < 8; i++) {
            Map<String, Object> duplicateIssue = new HashMap<>(issue2);
            duplicateIssue.put("usageLocation", "com.example.service.HttpService.makeRequest:" + (i + 100));
            issues.add(duplicateIssue);
        }
        
        // Low call count compatibility issue
        Map<String, Object> issue3 = new HashMap<>();
        issue3.put("issueType", "CLASS_NOT_FOUND");
        issue3.put("className", "org.apache.commons.lang.StringUtils");
        issue3.put("methodName", "isEmpty");
        issue3.put("expectedSignature", "(Ljava/lang/String;)Z");
        issue3.put("actualSignature", null);
        issue3.put("usageLocation", "com.example.util.ValidationUtils.checkEmpty");
        issue3.put("suggestedFix", "Use org.apache.commons.lang3.StringUtils instead");
        issue3.put("riskLevel", "HIGH");
        issue3.put("impact", "ClassNotFoundException at runtime");
        
        for (int i = 0; i < 3; i++) {
            Map<String, Object> duplicateIssue = new HashMap<>(issue3);
            duplicateIssue.put("usageLocation", "com.example.util.ValidationUtils.checkEmpty:" + (i + 200));
            issues.add(duplicateIssue);
        }
        
        // Single call compatibility issue
        Map<String, Object> issue4 = new HashMap<>();
        issue4.put("issueType", "METHOD_NOT_FOUND");
        issue4.put("className", "javax.xml.bind.DatatypeConverter");
        issue4.put("methodName", "parseBase64Binary");
        issue4.put("expectedSignature", "(Ljava/lang/String;)[B");
        issue4.put("actualSignature", null);
        issue4.put("usageLocation", "com.example.util.Base64Utils.decode:45");
        issue4.put("suggestedFix", "Use java.util.Base64.getDecoder().decode() instead");
        issue4.put("riskLevel", "MEDIUM");
        issue4.put("impact", "NoSuchMethodError at runtime");
        issues.add(issue4);
        
        compatibilityResult.put("compatibilityIssues", issues);
        results.add(compatibilityResult);
        section.put("result", results);
        sections.add(section);
        compatibilityData.put("sections", sections);
        
        return compatibilityData;
    }
}
