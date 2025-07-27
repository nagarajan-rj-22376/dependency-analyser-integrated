package com.example.depanalysis.dto;

import java.util.List;

public class DeprecationResult {
    private List<DeprecationIssue> deprecationIssues;

    public DeprecationResult() {}

    public DeprecationResult(List<DeprecationIssue> deprecationIssues) {
        this.deprecationIssues = deprecationIssues;
    }

    public List<DeprecationIssue> getDeprecationIssues() {
        return deprecationIssues;
    }

    public void setDeprecationIssues(List<DeprecationIssue> deprecationIssues) {
        this.deprecationIssues = deprecationIssues;
    }
}
