package com.example.depanalysis.report;

import java.util.ArrayList;
import java.util.List;

public class AnalysisReport {
    private List<FeatureReportSection> sections = new ArrayList<>();

    public AnalysisReport() {}

    public List<FeatureReportSection> getSections() { return sections; }
    public void setSections(List<FeatureReportSection> sections) { this.sections = sections; }

    public void addSection(FeatureReportSection section) {
        this.sections.add(section);
    }
}