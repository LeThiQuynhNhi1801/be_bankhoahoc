Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Backend Ban Khoa Hoc Application" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Java
$javaPath = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaPath) {
    Write-Host "ERROR: Java not found!" -ForegroundColor Red
    Write-Host "Please install JDK 17+ and add to PATH" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

Write-Host "Java found: $($javaPath.Version)" -ForegroundColor Green
Write-Host ""

# Kiểm tra Maven
$mvnPath = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnPath) {
    Write-Host "Maven found. Building and running..." -ForegroundColor Green
    Write-Host ""
    Write-Host "Swagger UI will be available at: http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
    Write-Host "API Base URL: http://localhost:8080/api" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Cyan
    Write-Host ""
    
    mvn clean spring-boot:run
} else {
    Write-Host "Maven not found in PATH." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Cyan
    Write-Host "1. Install Maven and add to PATH" -ForegroundColor White
    Write-Host "2. Use your IDE (IntelliJ IDEA, Eclipse, VS Code)" -ForegroundColor White
    Write-Host "3. Build JAR manually and run" -ForegroundColor White
    Write-Host ""
    Write-Host "For IDE:" -ForegroundColor Yellow
    Write-Host "- IntelliJ IDEA: Right-click BanKhoaHocApplication.java -> Run" -ForegroundColor White
    Write-Host "- Eclipse: Right-click project -> Run As -> Spring Boot App" -ForegroundColor White
    Write-Host "- VS Code: Click Run button on BanKhoaHocApplication.java" -ForegroundColor White
    Write-Host ""
    
    # Kiểm tra JAR file
    $jarFile = Get-ChildItem -Path target -Filter "*.jar" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "*SNAPSHOT.jar" -or $_.Name -like "be-bankhoahoc*.jar" } | Select-Object -First 1
    
    if ($jarFile) {
        Write-Host "Found JAR file: $($jarFile.FullName)" -ForegroundColor Green
        Write-Host "Running JAR file..." -ForegroundColor Green
        Write-Host ""
        Write-Host "Swagger UI will be available at: http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
        Write-Host "API Base URL: http://localhost:8080/api" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Cyan
        Write-Host ""
        
        java -jar $jarFile.FullName
    } else {
        Write-Host "No JAR file found. Please build the project first." -ForegroundColor Red
        Write-Host ""
        Write-Host "To build:" -ForegroundColor Yellow
        Write-Host "1. Install Maven: https://maven.apache.org/download.cgi" -ForegroundColor White
        Write-Host "2. Run: mvn clean package" -ForegroundColor White
        Write-Host "3. Then run this script again" -ForegroundColor White
        Write-Host ""
        Read-Host "Press Enter to exit"
    }
}
