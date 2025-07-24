package com.example.depanalysis.dto;

public class VersionMismatch {
    private String expectedVersion;
    private String resolvedVersion;
    private String versionSource; // "pom.xml", "build.gradle", "transitive"

    // Constructors
    public VersionMismatch() {}

    public VersionMismatch(String expectedVersion, String resolvedVersion, String versionSource) {
        this.expectedVersion = expectedVersion;
        this.resolvedVersion = resolvedVersion;
        this.versionSource = versionSource;
    }

    // Getters and Setters
    public String getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(String expectedVersion) { this.expectedVersion = expectedVersion; }

    public String getResolvedVersion() { return resolvedVersion; }
    public void setResolvedVersion(String resolvedVersion) { this.resolvedVersion = resolvedVersion; }

    public String getVersionSource() { return versionSource; }
    public void setVersionSource(String versionSource) { this.versionSource = versionSource; }
}
