# Enhanced Dependency Analysis Reports

This update integrates real file upload analysis with enhanced report generation, providing comprehensive dependency analysis reports with improved grouping, filtering, and formatting.

## 🆕 New Features

### 1. Upload-Based Analysis
- Upload ZIP files containing your project
- Automatic extraction and analysis
- Real-time report generation
- Supports all existing report formats (HTML, JSON, PDF)

### 2. Enhanced Report Structure
- **Vulnerability Analysis**: Groups vulnerabilities by dependency, sorted by count (descending)
- **Deprecation Analysis**: Groups by deprecated method, shows usage locations and frequency
- **API Compatibility**: Groups by method call, shows call count and source locations
- **ASM Signature Formatting**: Converts bytecode signatures to readable Java format
- **Smart Filtering**: Excludes dependencies with zero vulnerabilities for cleaner reports

## 🔗 API Endpoints

### Upload-Based Reports

#### Generate Report in Specific Format
```
POST /api/reports/upload/{format}
```
- **Parameters**: 
  - `format`: html, json, or pdf
  - `file`: ZIP file (multipart/form-data)
- **Response**: Success message with file location
- **Example**: `POST /api/reports/upload/html`

#### Generate All Format Reports
```
POST /api/reports/upload/all
```
- **Parameters**: 
  - `file`: ZIP file (multipart/form-data)
- **Response**: Success message for all generated reports
- **Generates**: HTML, JSON, and PDF reports simultaneously

### Sample Reports (for testing)

#### Generate Sample Report
```
GET /api/reports/sample/{format}
```
- **Parameters**: `format` (html, json, pdf)
- **Response**: Success message with sample report location

#### Generate All Sample Reports
```
GET /api/reports/sample/all
```
- **Response**: Success message for all sample report formats

#### Get Supported Formats
```
GET /api/reports/formats
```
- **Response**: List of supported report formats

## 📊 Report Structure Examples

### Vulnerability Analysis
```
Dependencies sorted by vulnerability count (highest first):
├── log4j-core-2.14.1.jar (3 vulnerabilities)
│   ├── CVE-2021-44228 (Critical - CVSS 10.0)
│   ├── CVE-2021-45046 (High - CVSS 9.0)
│   └── CVE-2021-45105 (Medium - CVSS 5.9)
├── jackson-databind-2.9.8.jar (2 vulnerabilities)
│   ├── CVE-2020-25649 (High - CVSS 7.5)
│   └── CVE-2020-24616 (High - CVSS 8.1)
└── commons-io-2.6.jar (1 vulnerability)
    └── CVE-2021-29425 (Medium - CVSS 4.8)
```

### Deprecation Analysis
```
Deprecated methods sorted by usage frequency (highest first):
├── java.util.Date.getYear() (12 usages)
│   ├── Risk Level: HIGH
│   ├── Deprecated Since: JDK 1.1
│   ├── Reason: Replaced by Calendar.get(Calendar.YEAR) - 1900
│   └── Usage Locations:
│       ├── com.example.service.DateService.formatDate:45
│       ├── com.example.util.TimeUtils.calculateAge:23
│       └── ... (9 more locations)
├── java.net.URL.encode() (7 usages)
│   ├── Risk Level: MEDIUM
│   └── Usage Locations: ...
└── java.lang.Thread.stop() (3 usages)
    ├── Risk Level: CRITICAL
    └── Usage Locations: ...
```

### API Compatibility
```
API compatibility issues sorted by call frequency (highest first):
├── org.apache.commons.logging.Log.trace() (16 calls)
│   ├── Issue: METHOD_NOT_FOUND
│   ├── Expected: void trace(Object)
│   ├── Actual: null
│   ├── Risk: HIGH
│   └── Call Locations:
│       ├── com.example.service.LoggingService.logTrace:50
│       ├── com.example.service.LoggingService.logTrace:51
│       └── ... (14 more locations)
├── org.apache.http.client.HttpClient.execute() (8 calls)
│   ├── Issue: METHOD_SIGNATURE_CHANGED
│   ├── Expected: HttpResponse execute(HttpUriRequest)
│   ├── Actual: HttpResponse execute(HttpUriRequest, HttpContext)
│   └── Call Locations: ...
└── javax.xml.bind.DatatypeConverter.parseBase64Binary() (1 call)
    ├── Issue: METHOD_NOT_FOUND
    ├── Expected: byte[] parseBase64Binary(String)
    └── Call Location: com.example.util.Base64Utils.decode:45
```

## 🎯 ASM Signature Formatting

The system now converts ASM bytecode signatures to readable Java format:

| ASM Format | Readable Format |
|------------|-----------------|
| `(Ljava/lang/String;)V` | `void method(String)` |
| `(ILjava/lang/Object;)[B` | `byte[] method(int, Object)` |
| `()Ljava/util/List;` | `List method()` |
| `([Ljava/lang/String;)I` | `int method(String[])` |

## 🚀 Usage Examples

### Test with Demo Page
1. Start the application: `mvn spring-boot:run`
2. Open `upload-demo.html` in your browser
3. Use the interactive interface to test endpoints

### cURL Examples

#### Upload and Generate HTML Report
```bash
curl -X POST -F "file=@your-project.zip" \
  http://localhost:8080/api/reports/upload/html
```

#### Upload and Generate All Reports
```bash
curl -X POST -F "file=@your-project.zip" \
  http://localhost:8080/api/reports/upload/all
```

#### Generate Sample Reports
```bash
curl http://localhost:8080/api/reports/sample/html
curl http://localhost:8080/api/reports/sample/json
curl http://localhost:8080/api/reports/sample/pdf
```

## 📁 Output Location

All generated reports are saved in the `reports/` directory with timestamped filenames:
- `reports/dependency_analysis_report_20250127_175830.html`
- `reports/dependency_analysis_report_20250127_175830.json`
- `reports/dependency_analysis_report_20250127_175830.pdf`

## 🔍 Key Improvements

1. **Real Analysis**: Process actual uploaded projects instead of sample data
2. **Smart Grouping**: Organize results by dependency/method for better readability
3. **Intelligent Sorting**: Sort by impact (vulnerability count, usage frequency, call count)
4. **Readable Signatures**: Convert ASM bytecode to Java syntax
5. **Clean Filtering**: Remove noise by excluding zero-vulnerability dependencies
6. **Comprehensive Coverage**: Support vulnerability, deprecation, and compatibility analysis
7. **Multi-Format Support**: Generate HTML, JSON, and PDF reports
8. **Temporary Cleanup**: Automatic cleanup of extracted files

## 🛠️ Technical Architecture

- **ReportController**: New upload endpoints for file-based analysis
- **AnalysisOrchestrator**: Existing service for project analysis
- **ZipExtractor**: Utility for safe ZIP file extraction
- **ReportGenerator**: Enhanced with grouping and formatting capabilities
- **SignatureFormatter**: New utility for ASM-to-Java signature conversion
- **ReportDataProcessor**: Enhanced with filtering and grouping logic

This integration provides a complete end-to-end solution for dependency analysis, from file upload to comprehensive report generation.
