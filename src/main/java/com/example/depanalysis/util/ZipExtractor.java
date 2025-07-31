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
        String originalFilename = file.getOriginalFilename();
        logger.info("Starting archive extraction for file: {}", originalFilename);
        
        boolean isWarFile = originalFilename != null && originalFilename.toLowerCase().endsWith(".war");
        Path tempDir = Files.createTempDirectory("uploaded_project_");
        Path tempArchiveFile = null;
        
        try {
            // First save the MultipartFile to a temporary file
            String extension = isWarFile ? ".war" : ".zip";
            tempArchiveFile = Files.createTempFile("upload_", extension);
            file.transferTo(tempArchiveFile.toFile());
            
            logger.debug("Created temporary archive file at: {}", tempArchiveFile);
            
            // Use ZipFile for better compatibility with various ZIP/WAR formats
            try (ZipFile zipFile = new ZipFile(tempArchiveFile.toFile())) {
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
            
            // Post-process WAR files to extract nested JARs
            if (isWarFile) {
                extractNestedJarsFromWar(tempDir);
            }
            
            logger.info("Successfully extracted {} to: {}", isWarFile ? "WAR" : "ZIP", tempDir);
            return tempDir;
            
        } catch (IOException e) {
            logger.error("Error extracting archive file {}: {}", originalFilename, e.getMessage(), e);
            
            // Clean up temp directory if extraction failed
            if (Files.exists(tempDir)) {
                try {
                    deleteDirectoryRecursively(tempDir);
                } catch (IOException cleanupException) {
                    logger.error("Failed to cleanup temp directory after extraction failure: {}", cleanupException.getMessage());
                }
            }
            
            throw new IOException("Failed to extract archive file: " + e.getMessage(), e);
            
        } finally {
            // Clean up temporary archive file
            if (tempArchiveFile != null && Files.exists(tempArchiveFile)) {
                try {
                    Files.delete(tempArchiveFile);
                    logger.debug("Cleaned up temporary archive file: {}", tempArchiveFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary archive file: {}", e.getMessage());
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
    
    /**
     * Extracts nested JAR files from WAR structure for deeper analysis
     * WAR files typically contain JARs in WEB-INF/lib/ directory
     */
    private static void extractNestedJarsFromWar(Path warExtractDir) throws IOException {
        logger.info("Extracting nested JARs from WAR file structure");
        
        // Look for WEB-INF/lib directory (standard WAR structure)
        Path webInfLib = warExtractDir.resolve("WEB-INF").resolve("lib");
        if (Files.exists(webInfLib) && Files.isDirectory(webInfLib)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(webInfLib, "*.jar")) {
                for (Path jarFile : stream) {
                    extractNestedJar(jarFile, webInfLib);
                }
            }
        }
        
        // Also look for any JAR files in the root or other directories
        Files.walk(warExtractDir)
                .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                .filter(path -> !path.getParent().toString().contains("extracted_")) // Avoid re-extracting
                .forEach(jarPath -> {
                    try {
                        extractNestedJar(jarPath, jarPath.getParent());
                    } catch (IOException e) {
                        logger.warn("Failed to extract nested JAR {}: {}", jarPath, e.getMessage());
                    }
                });
        
        logger.info("Completed extraction of nested JARs from WAR structure");
    }
    
    /**
     * Extracts a single JAR file to enable analysis of its contents
     */
    private static void extractNestedJar(Path jarFile, Path parentDir) throws IOException {
        String jarName = jarFile.getFileName().toString();
        String extractDirName = jarName.substring(0, jarName.lastIndexOf('.')) + "_extracted";
        Path extractDir = parentDir.resolve(extractDirName);
        
        logger.debug("Extracting nested JAR: {} to {}", jarFile, extractDir);
        
        Files.createDirectories(extractDir);
        
        try (ZipFile zipFile = new ZipFile(jarFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path newPath = extractDir.resolve(entry.getName()).normalize();
                
                // Security check: prevent zip slip vulnerability
                if (!newPath.startsWith(extractDir)) {
                    logger.warn("Skipping JAR entry outside target directory: {}", entry.getName());
                    continue;
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    
                    try (InputStream is = zipFile.getInputStream(entry);
                         OutputStream os = Files.newOutputStream(newPath)) {
                        
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
        
        logger.debug("Successfully extracted nested JAR: {} ({} bytes)", jarName, Files.size(jarFile));
    }
}