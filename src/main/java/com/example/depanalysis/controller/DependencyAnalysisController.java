package com.example.depanalysis.controller;

import com.example.depanalysis.dto.FinalReport;
import com.example.depanalysis.service.AnalysisOrchestrator;
import com.example.depanalysis.util.ZipExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Objects;

@RestController
@RequestMapping("/api/dependency-analysis")
public class DependencyAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisController.class);

    private final AnalysisOrchestrator orchestrator;

    public DependencyAnalysisController(AnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> analyze(@RequestParam("file") MultipartFile file) {
        Path tempDir = null;
        try {
            // Validate the file
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
            if (file.isEmpty() || (!originalFilename.endsWith(".zip") && !originalFilename.endsWith(".war"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Uploaded file must be a non-empty ZIP or WAR archive.");
            }

            // Extract ZIP to temp dir
            tempDir = ZipExtractor.extractZip(file);

            logger.info("Extracted files:");
            Files.walk(tempDir).forEach(path -> logger.info(path.toString()));

            // Run orchestrator
            FinalReport report = orchestrator.analyzeAll(tempDir);
            
            return ResponseEntity.ok(report);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing uploaded ZIP: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectoryRecursively(tempDir);
                } catch (IOException e) {
                    // Optionally log cleanup failure, but don't throw further
                }
            }
        }
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ignored) { }
                });
    }
}