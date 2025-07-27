package com.example.depanalysis.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DemoController {

    @GetMapping(value = "/demo", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDemoPage() throws IOException {
        Path demoPath = Paths.get("upload-demo.html");
        if (Files.exists(demoPath)) {
            String content = Files.readString(demoPath);
            return ResponseEntity.ok(content);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/reports/{filename}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("reports", filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);
            
            // Determine content type based on file extension
            String contentType = getContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String getContentType(String filename) {
        if (filename.endsWith(".html")) {
            return "text/html";
        } else if (filename.endsWith(".json")) {
            return "application/json";
        } else if (filename.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }
}
