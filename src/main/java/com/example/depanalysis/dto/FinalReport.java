package com.example.depanalysis.dto;

import java.util.List;

public class FinalReport {
    private List<FeatureReportSection> sections;

    public FinalReport() {}

    public FinalReport(List<FeatureReportSection> sections) {
        this.sections = sections;
    }

    public List<FeatureReportSection> getSections() {
        return sections;
    }

    public void setSections(List<FeatureReportSection> sections) {
        this.sections = sections;
    }
}