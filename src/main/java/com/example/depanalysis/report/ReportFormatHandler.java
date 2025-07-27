package com.example.depanalysis.report;

import com.example.depanalysis.dto.FinalReport;
import java.io.IOException;

public interface ReportFormatHandler {
    void generateReport(FinalReport report, String outputPath) throws IOException;
}
