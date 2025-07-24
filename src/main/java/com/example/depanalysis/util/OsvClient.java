package com.example.depanalysis.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OsvClient {
    private static final Logger logger = LoggerFactory.getLogger(OsvClient.class);
    private static final String OSV_API = "https://api.osv.dev/v1/query";

    public static JsonNode queryMaven(String groupId, String artifactId, String version) throws Exception {
        logger.info("Querying OSV API for Maven dependency: {}:{}:{}", groupId, artifactId, version);
        
        ObjectMapper mapper = new ObjectMapper();
        String body = String.format(
            "{\"package\":{\"ecosystem\":\"Maven\",\"name\":\"%s:%s\"},\"version\":\"%s\"}",
            groupId, artifactId, version
        );
        
        logger.debug("OSV API request body: {}", body);
        logger.debug("OSV API endpoint: {}", OSV_API);
        
        URL url = new URL(OSV_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        logger.debug("Sending POST request to OSV API");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
            logger.debug("Request body written to output stream");
        }

        int responseCode = conn.getResponseCode();
        logger.debug("OSV API response code: {}", responseCode);
        
        if (responseCode != 200) {
            logger.error("OSV API request failed for {}:{}:{} with response code: {}", 
                        groupId, artifactId, version, responseCode);
            throw new RuntimeException("OSV API error: " + responseCode);
        }
        
        logger.debug("Successfully received response from OSV API");
        JsonNode response = mapper.readTree(conn.getInputStream());
        
        if (response.has("vulns") && response.get("vulns").size() > 0) {
            logger.info("OSV API found {} vulnerabilities for {}:{}:{}", 
                       response.get("vulns").size(), groupId, artifactId, version);
        } else {
            logger.debug("OSV API found no vulnerabilities for {}:{}:{}", groupId, artifactId, version);
        }
        
        return response;
    }
}