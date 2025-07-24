package com.example.depanalysis.util;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarMetadataExtractor {
    public static Optional<Map<String, String>> extractMavenCoordinates(Path jarPath) {
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("pom.properties")) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Properties props = new Properties();
                        props.load(is);
                        String groupId = props.getProperty("groupId");
                        String artifactId = props.getProperty("artifactId");
                        String version = props.getProperty("version");
                        if (groupId != null && artifactId != null && version != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("groupId", groupId);
                            map.put("artifactId", artifactId);
                            map.put("version", version);
                            return Optional.of(map);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Optionally log the error
        }
        return Optional.empty();
    }
}