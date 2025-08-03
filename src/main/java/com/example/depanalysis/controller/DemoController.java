package com.example.depanalysis.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DemoController {

    private static final String REPORTS_DIR = "reports";

    @GetMapping("/download/reports/{filename}")
    public ResponseEntity<FileSystemResource> downloadReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(REPORTS_DIR, filename);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getContentType(filename))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<FileSystemResource> viewReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(REPORTS_DIR, filename);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(getContentType(filename))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType getContentType(String filename) {
        if (filename.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else if (filename.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
