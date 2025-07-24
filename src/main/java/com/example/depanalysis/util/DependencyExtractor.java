package com.example.depanalysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DependencyExtractor.class);

    public static List<Path> findAllPomXmls(Path projectDir) {
        List<Path> poms = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(projectDir)) {
            poms = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error walking project directory for pom.xml files: {}", e.getMessage(), e);
        }
        logger.info("Found {} pom.xml files in project: {}", poms.size(), projectDir);
        return poms;
    }

    public static List<Path> findAllBuildGradleFiles(Path projectDir) {
        List<Path> gradles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(projectDir)) {
            gradles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("build.gradle"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error walking project directory for build.gradle files: {}", e.getMessage(), e);
        }
        logger.info("Found {} build.gradle files in project: {}", gradles.size(), projectDir);
        return gradles;
    }

    public static List<Map<String, String>> parsePomXml(Path pomPath) {
        logger.info("Parsing pom.xml at path: {}", pomPath);
        List<Map<String, String>> deps = new ArrayList<>();
        if (!Files.exists(pomPath)) {
            logger.warn("pom.xml file does not exist at path: {}", pomPath);
            return deps;
        }
        try (InputStream is = Files.newInputStream(pomPath)) {
            logger.debug("Successfully opened pom.xml file for reading");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(is);
            NodeList depNodes = doc.getElementsByTagName("dependency");
            logger.info("Found {} dependency nodes in pom.xml", depNodes.getLength());
            for (int i = 0; i < depNodes.getLength(); i++) {
                Element depElem = (Element) depNodes.item(i);
                String groupId = getTagValue(depElem, "groupId");
                String artifactId = getTagValue(depElem, "artifactId");
                String version = getTagValue(depElem, "version");
                if (groupId != null && artifactId != null && version != null) {
                    logger.debug("Found valid dependency: {}:{}:{}", groupId, artifactId, version);
                    Map<String, String> dep = new HashMap<>();
                    dep.put("groupId", groupId);
                    dep.put("artifactId", artifactId);
                    dep.put("version", version);
                    deps.add(dep);
                } else {
                    logger.warn("Skipping incomplete dependency at index {}: groupId={}, artifactId={}, version={}",
                            i, groupId, artifactId, version);
                }
            }
            logger.info("Successfully parsed {} valid dependencies from pom.xml", deps.size());
        } catch (Exception e) {
            logger.error("Error parsing pom.xml at {}: {}", pomPath, e.getMessage(), e);
        }
        return deps;
    }

    private static String getTagValue(Element elem, String tag) {
        NodeList nl = elem.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }

    public static List<Map<String, String>> parseBuildGradle(Path gradlePath) {
        logger.info("Parsing build.gradle at path: {}", gradlePath);
        List<Map<String, String>> deps = new ArrayList<>();
        if (!Files.exists(gradlePath)) {
            logger.warn("build.gradle file does not exist at path: {}", gradlePath);
            return deps;
        }

        Pattern pattern = Pattern.compile("[\"']([\\w\\-.]+):([\\w\\-.]+):([\\w\\-.]+)[\"']");
        logger.debug("Using regex pattern for dependency matching: {}", pattern.pattern());

        try {
            List<String> lines = Files.readAllLines(gradlePath);
            logger.info("Successfully read {} lines from build.gradle", lines.size());

            int lineNumber = 0;
            int dependenciesFound = 0;

            for (String line : lines) {
                lineNumber++;
                if (line.contains("implementation") || line.contains("api") || line.contains("compile")) {
                    logger.debug("Found dependency declaration at line {}: {}", lineNumber, line.trim());
                    Matcher m = pattern.matcher(line);
                    while (m.find()) {
                        String groupId = m.group(1);
                        String artifactId = m.group(2);
                        String version = m.group(3);

                        logger.debug("Extracted dependency from line {}: {}:{}:{}", lineNumber, groupId, artifactId, version);

                        Map<String, String> dep = new HashMap<>();
                        dep.put("groupId", groupId);
                        dep.put("artifactId", artifactId);
                        dep.put("version", version);
                        deps.add(dep);
                        dependenciesFound++;
                    }
                }
            }

            logger.info("Successfully parsed {} dependencies from build.gradle", dependenciesFound);

        } catch (IOException e) {
            logger.error("Error reading build.gradle at {}: {}", gradlePath, e.getMessage(), e);
        }
        return deps;
    }
}