Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Backend Ban Khoa Hoc Application" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Make sure MySQL is running and database 'bankhoahoc' exists" -ForegroundColor Yellow
Write-Host ""
Write-Host "Swagger UI will be available at: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "API Base URL: http://localhost:8080/api" -ForegroundColor Green
Write-Host ""
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Maven is available
$mvnPath = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnPath) {
    Write-Host "Using Maven..." -ForegroundColor Green
    mvn spring-boot:run
} else {
    Write-Host "Maven not found in PATH." -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Maven or use an IDE like IntelliJ IDEA or Eclipse." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Alternatively, you can:" -ForegroundColor Yellow
    Write-Host "1. Install Maven from https://maven.apache.org/download.cgi" -ForegroundColor White
    Write-Host "2. Add Maven to your PATH environment variable" -ForegroundColor White
    Write-Host "3. Or use your IDE to run the BanKhoaHocApplication.java file" -ForegroundColor White
    Write-Host ""
    Read-Host "Press Enter to exit"
}
