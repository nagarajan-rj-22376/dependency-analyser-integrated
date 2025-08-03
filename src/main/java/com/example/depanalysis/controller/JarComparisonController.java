package com.example.depanalysis.controller;

import com.example.depanalysis.service.JarComparisonService;
import com.example.depanalysis.dto.JarComparisonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jar-comparison")
@CrossOrigin(origins = "*")
public class JarComparisonController {

    private static final Logger logger = LoggerFactory.getLogger(JarComparisonController.class);
    private final JarComparisonService jarComparisonService;

    public JarComparisonController(JarComparisonService jarComparisonService) {
        this.jarComparisonService = jarComparisonService;
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compareJars(
            @RequestParam("oldJar") MultipartFile oldJar,
            @RequestParam("newJar") MultipartFile newJar) {
        
        logger.info("Received JAR comparison request - oldJar: {}, newJar: {}", 
                   oldJar.getOriginalFilename(), newJar.getOriginalFilename());

        try {
            // Validate files
            if (oldJar.isEmpty() || newJar.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Both JAR files are required"));
            }

            if (!isJarFile(oldJar) || !isJarFile(newJar)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Both files must be JAR files"));
            }

            // Save uploaded files temporarily
            Path tempDir = Files.createTempDirectory("jar-comparison");
            Path oldJarPath = tempDir.resolve("old-" + oldJar.getOriginalFilename());
            Path newJarPath = tempDir.resolve("new-" + newJar.getOriginalFilename());

            Files.copy(oldJar.getInputStream(), oldJarPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newJar.getInputStream(), newJarPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Saved JAR files to temporary location: {}", tempDir);

            // Perform comparison
            JarComparisonResult result = jarComparisonService.compareJars(
                oldJarPath, newJarPath, 
                oldJar.getOriginalFilename(), newJar.getOriginalFilename()
            );

            // Log result details for debugging
            logger.info("Comparison result summary: {}", result.getSummary());
            logger.info("Package level changes count: {}", result.getPackageLevel() != null ? result.getPackageLevel().size() : 0);
            logger.info("Class level changes count: {}", result.getClassLevel() != null ? result.getClassLevel().size() : 0);

            // Generate HTML report and save to reports directory
            String reportFilename = generateJarComparisonReport(result);
            
            // Clean up temporary files
            Files.deleteIfExists(oldJarPath);
            Files.deleteIfExists(newJarPath);
            Files.deleteIfExists(tempDir);

            logger.info("JAR comparison completed successfully");
            
            // Return response with downloadUrl like project analyzer
            return ResponseEntity.ok(java.util.Map.of(
                "downloadUrl", "/" + reportFilename,
                "message", "JAR comparison completed successfully",
                "summary", result.getSummary()
            ));

        } catch (IOException e) {
            logger.error("IO error during JAR comparison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error processing JAR files: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during JAR comparison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Unexpected error: " + e.getMessage()));
        }
    }

    @PostMapping("/compare-and-redirect")
    public ResponseEntity<String> compareJarsWithRedirect(
            @RequestParam("oldJar") MultipartFile oldJar,
            @RequestParam("newJar") MultipartFile newJar) {
        
        logger.info("Received JAR comparison request with redirect - oldJar: {}, newJar: {}", 
                   oldJar.getOriginalFilename(), newJar.getOriginalFilename());

        try {
            // Validate files
            if (oldJar.isEmpty() || newJar.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorHtml("Both JAR files are required"));
            }

            if (!isJarFile(oldJar) || !isJarFile(newJar)) {
                return ResponseEntity.badRequest()
                    .body(createErrorHtml("Both files must be JAR files"));
            }

            // Create temporary directory
            Path tempDir = Files.createTempDirectory("jar-comparison");
            logger.info("Saved JAR files to temporary location: {}", tempDir);

            try {
                // Save uploaded files
                Path oldJarPath = tempDir.resolve(oldJar.getOriginalFilename());
                Path newJarPath = tempDir.resolve(newJar.getOriginalFilename());

                Files.copy(oldJar.getInputStream(), oldJarPath, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(newJar.getInputStream(), newJarPath, StandardCopyOption.REPLACE_EXISTING);

                // Perform comparison
                JarComparisonResult result = jarComparisonService.compareJars(
                    oldJarPath, newJarPath, 
                    oldJar.getOriginalFilename(), newJar.getOriginalFilename());

                // Log results
                logger.info("Comparison result summary: {}", result.getSummary());
                logger.info("Package level changes count: {}", 
                           result.getPackageLevel() != null ? result.getPackageLevel().size() : 0);
                logger.info("Class level changes count: {}", 
                           result.getClassLevel() != null ? result.getClassLevel().size() : 0);

                logger.info("JAR comparison completed successfully");

                // Return HTML page with embedded data and auto-redirect
                return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(createRedirectHtml(result));

            } finally {
                // Clean up temporary files
                try {
                    Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete temporary file: {}", path, e);
                            }
                        });
                } catch (IOException e) {
                    logger.warn("Failed to clean up temporary directory: {}", tempDir, e);
                }
            }

        } catch (IOException e) {
            logger.error("Error processing JAR files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorHtml("Error processing JAR files: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during JAR comparison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorHtml("Unexpected error: " + e.getMessage()));
        }
    }

    private String createRedirectHtml(JarComparisonResult result) {
        try {
            // Convert result to JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonResult = mapper.writeValueAsString(result);
            
            // Properly escape JSON for JavaScript - use Base64 encoding to avoid escaping issues
            String base64Json = java.util.Base64.getEncoder().encodeToString(jsonResult.getBytes());
            
            return String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <title>JAR Comparison - Redirecting...</title>" +
                "</head>" +
                "<body>" +
                "    <div style=\"text-align: center; margin-top: 50px;\">" +
                "        <h2>JAR Comparison Complete</h2>" +
                "        <p>Redirecting to results page...</p>" +
                "        <p><a href=\"jar-comparison-results.html\">Click here if you are not redirected automatically</a></p>" +
                "    </div>" +
                "    <script>" +
                "        try {" +
                "            // Decode the base64 JSON data" +
                "            const base64Data = '%s';" +
                "            const jsonData = atob(base64Data);" +
                "            console.log('Storing comparison result:', JSON.parse(jsonData));" +
                "            localStorage.setItem('jarComparisonResult', jsonData);" +
                "            " +
                "            // Redirect to results page" +
                "            setTimeout(function() {" +
                "                window.location.href = 'jar-comparison-results.html';" +
                "            }, 1000);" +
                "        } catch (error) {" +
                "            console.error('Error processing comparison result:', error);" +
                "            alert('Error processing comparison result. Redirecting anyway...');" +
                "            setTimeout(function() {" +
                "                window.location.href = 'jar-comparison-results.html';" +
                "            }, 2000);" +
                "        }" +
                "    </script>" +
                "</body>" +
                "</html>", base64Json);
        } catch (Exception e) {
            logger.error("Error creating redirect HTML", e);
            return createErrorHtml("Error creating redirect page");
        }
    }

    private String createErrorHtml(String message) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <title>JAR Comparison Error</title>" +
            "</head>" +
            "<body>" +
            "    <div style=\"text-align: center; margin-top: 50px;\">" +
            "        <h2>Error</h2>" +
            "        <p>%s</p>" +
            "        <p><a href=\"jar-comparison.html\">Go back to JAR comparison</a></p>" +
            "    </div>" +
            "</body>" +
            "</html>", message);
    }

    private String generateJarComparisonReport(JarComparisonResult result) throws IOException {
        // Generate HTML report similar to existing jar-comparison-results.html
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "jar-comparison-report-" + timestamp + ".html";
        
        // Generate directly in the static directory (not in a subdirectory)
        Path staticDir = Paths.get("target/classes/static");
        Files.createDirectories(staticDir);
        
        // Read the template from jar-comparison-results.html in classpath
        String template;
        try {
            // Try to read from classpath first (compiled version)
            var classLoader = getClass().getClassLoader();
            var inputStream = classLoader.getResourceAsStream("static/jar-comparison-results.html");
            if (inputStream != null) {
                template = new String(inputStream.readAllBytes());
                logger.info("Read template from classpath");
            } else {
                // Fallback to source file
                Path templatePath = Paths.get("src/main/resources/static/jar-comparison-results.html");
                template = Files.readString(templatePath);
                logger.info("Read template from source file");
            }
        } catch (Exception e) {
            logger.error("Error reading template", e);
            throw new IOException("Could not read jar-comparison-results.html template", e);
        }
        
        // Convert result to JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonResult = mapper.writeValueAsString(result);
        
        // Inject the data into the template
        String reportHtml = template.replace(
            "loadComparisonResults();",
            String.format(
                "// Pre-loaded comparison data\n" +
                "                const preloadedResult = %s;\n" +
                "                console.log('Using pre-loaded comparison result:', preloadedResult);\n" +
                "                displayResults(preloadedResult);",
                jsonResult
            )
        );
        
        // Write the report to the static directory
        Path reportPath = staticDir.resolve(filename);
        Files.writeString(reportPath, reportHtml);
        
        logger.info("Generated JAR comparison report: {}", reportPath);
        
        return filename;
    }

    private boolean isJarFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".jar");
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
