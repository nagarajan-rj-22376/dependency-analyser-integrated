package com.example.depanalysis.dto;

import java.time.LocalDate;

public class DeprecationInfo {
    private String elementName;           // Full method/class name
    private String elementType;           // "METHOD", "CLASS", "FIELD", "PACKAGE"
    private String since;                 // @Deprecated(since = "1.5")
    private boolean forRemoval;           // @Deprecated(forRemoval = true)
    private String reason;                // Javadoc reason or annotation message
    private String replacement;           // Suggested replacement
    private String javadocDeprecated;     // @deprecated tag content
    private LocalDate deprecatedDate;     // When it was deprecated (estimated)
    private String sourceLocation;        // Which JAR/dependency contains this

    public DeprecationInfo() {}

    public DeprecationInfo(String elementName, String elementType) {
        this.elementName = elementName;
        this.elementType = elementType;
    }

    // Getters and Setters
    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }

    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }

    public String getSince() { return since; }
    public void setSince(String since) { this.since = since; }

    public boolean isForRemoval() { return forRemoval; }
    public void setForRemoval(boolean forRemoval) { this.forRemoval = forRemoval; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReplacement() { return replacement; }
    public void setReplacement(String replacement) { this.replacement = replacement; }

    public String getJavadocDeprecated() { return javadocDeprecated; }
    public void setJavadocDeprecated(String javadocDeprecated) { this.javadocDeprecated = javadocDeprecated; }

    public LocalDate getDeprecatedDate() { return deprecatedDate; }
    public void setDeprecatedDate(LocalDate deprecatedDate) { this.deprecatedDate = deprecatedDate; }

    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    @Override
    public String toString() {
        return String.format("DeprecationInfo{element='%s', type='%s', since='%s', forRemoval=%s}", 
                           elementName, elementType, since, forRemoval);
    }
}
