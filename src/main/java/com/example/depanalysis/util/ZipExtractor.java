package com.example.depanalysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipExtractor.class);
    
    public static Path extractZip(MultipartFile file) throws IOException {
        logger.info("Starting ZIP extraction for file: {}", file.getOriginalFilename());
        Path tempDir = Files.createTempDirectory("uploaded_project_");
        Path tempZipFile = null;
        
        try {
            // First save the MultipartFile to a temporary file
            tempZipFile = Files.createTempFile("upload_", ".zip");
            file.transferTo(tempZipFile.toFile());
            
            logger.debug("Created temporary ZIP file at: {}", tempZipFile);
            
            // Use ZipFile for better compatibility with various ZIP formats
            try (ZipFile zipFile = new ZipFile(tempZipFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path newPath = tempDir.resolve(entry.getName()).normalize();
                    
                    // Security check: prevent zip slip vulnerability
                    if (!newPath.startsWith(tempDir)) {
                        logger.warn("Skipping entry outside target directory: {}", entry.getName());
                        continue;
                    }
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(newPath);
                        logger.debug("Created directory: {}", newPath);
                    } else {
                        Files.createDirectories(newPath.getParent());
                        
                        try (InputStream is = zipFile.getInputStream(entry);
                             OutputStream os = Files.newOutputStream(newPath)) {
                            
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                            
                            logger.debug("Extracted file: {} (size: {} bytes)", newPath, entry.getSize());
                        }
                    }
                }
            }
            
            logger.info("Successfully extracted ZIP to: {}", tempDir);
            return tempDir;
            
        } catch (IOException e) {
            logger.error("Error extracting ZIP file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            
            // Clean up temp directory if extraction failed
            if (Files.exists(tempDir)) {
                try {
                    deleteDirectoryRecursively(tempDir);
                } catch (IOException cleanupException) {
                    logger.error("Failed to cleanup temp directory after extraction failure: {}", cleanupException.getMessage());
                }
            }
            
            throw new IOException("Failed to extract ZIP file: " + e.getMessage(), e);
            
        } finally {
            // Clean up temporary ZIP file
            if (tempZipFile != null && Files.exists(tempZipFile)) {
                try {
                    Files.delete(tempZipFile);
                    logger.debug("Cleaned up temporary ZIP file: {}", tempZipFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary ZIP file: {}", e.getMessage());
                }
            }
        }
    }
    
    private static void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Log but don't throw - best effort cleanup
                    }
                });
    }
}