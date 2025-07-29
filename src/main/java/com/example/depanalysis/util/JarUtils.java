package com.example.depanalysis.util;

import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarUtils {
    private static final Logger logger = LoggerFactory.getLogger(JarUtils.class);
    
    public static List<Path> findAllJars(Path rootDir) {
        List<Path> jars = new ArrayList<>();
        try {
            Files.walk(rootDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.getFileName().toString().startsWith("._")) // Filter out macOS metadata files
                .filter(p -> !p.getFileName().toString().startsWith("_"))  // Filter out JARs starting with underscore
                .filter(p -> !p.getFileName().toString().startsWith(".")) // Filter out other hidden files
                .forEach(jar -> {
                    jars.add(jar);
                    logger.debug("Found valid JAR file: {}", jar.getFileName());
                });
            
            // Log any filtered out files for debugging
            Files.walk(rootDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> p.getFileName().toString().startsWith("._") || p.getFileName().toString().startsWith("_"))
                .forEach(jar -> logger.debug("Filtered out metadata/underscore JAR file: {}", jar.getFileName()));
                
        } catch (Exception e) {
            logger.error("Error while scanning for JAR files in {}: {}", rootDir, e.getMessage(), e);
        }
        
        logger.info("Found {} valid JAR files in {}", jars.size(), rootDir);
        return jars;
    }
}