package com.example.depanalysis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
// ... imports

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final long MAX_SIZE = 100 * 1024 * 1024; // 100MB

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No file uploaded.");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Only ZIP files are accepted.");
        }
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("File too large. Max size is 100MB.");
        }

        // Unpack ZIP to temp dir
        try {
            Path tempDir = Files.createTempDirectory("uploaded_zip_");
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = tempDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (OutputStream os = Files.newOutputStream(entryPath)) {
                            zis.transferTo(os);
                        }
                    }
                    zis.closeEntry();
                }
            }

            // Log extracted files for demo
            Files.walk(tempDir)
                .forEach(p -> System.out.println("Extracted: " + p));

            return ResponseEntity.ok("File uploaded and extracted successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to extract ZIP: " + e.getMessage());
        }
    }
}