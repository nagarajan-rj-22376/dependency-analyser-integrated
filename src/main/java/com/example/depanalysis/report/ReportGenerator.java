package com.example.depanalysis.report;

import com.example.depanalysis.dto.FinalReport;
import com.example.depanalysis.report.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final Map<ReportFormat, ReportFormatHandler> handlers;

    @Autowired
    public ReportGenerator(HtmlReportHandler htmlHandler, 
                          JsonReportHandler jsonHandler,
                          PdfReportHandler pdfHandler) {
        handlers = new HashMap<>();
        handlers.put(ReportFormat.HTML, htmlHandler);
        handlers.put(ReportFormat.JSON, jsonHandler);
        handlers.put(ReportFormat.PDF, pdfHandler);
        
        logger.info("ReportGenerator initialized with {} handlers", handlers.size());
    }

    public void generateReport(FinalReport report, ReportFormat format, String outputPath) throws IOException {
        ReportFormatHandler handler = handlers.get(format);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported report format: " + format);
        }
        
        logger.info("Generating {} report to {}", format, outputPath);
        handler.generateReport(report, outputPath);
        logger.info("Successfully generated {} report", format);
    }

    public void generateReports(FinalReport report, List<ReportFormat> formats, String outputPath) throws IOException {
        for (ReportFormat format : formats) {
            generateReport(report, format, outputPath);
        }
    }

    public List<String> getSupportedFormats() {
        return handlers.keySet().stream()
                .map(Enum::name)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
}
