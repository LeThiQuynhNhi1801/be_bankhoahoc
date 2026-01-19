@echo off
echo ========================================
echo Starting Backend Ban Khoa Hoc Application
echo ========================================
echo.
echo Make sure MySQL is running and database 'bankhoahoc' exists
echo.
echo Swagger UI will be available at: http://localhost:8080/swagger-ui.html
echo API Base URL: http://localhost:8080/api
echo.
echo Press Ctrl+C to stop the application
echo.
echo ========================================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Using Maven...
    mvn spring-boot:run
) else (
    echo Maven not found in PATH.
    echo.
    echo Please install Maven or use an IDE like IntelliJ IDEA or Eclipse.
    echo.
    echo Alternatively, you can:
    echo 1. Install Maven from https://maven.apache.org/download.cgi
    echo 2. Add Maven to your PATH environment variable
    echo 3. Or use your IDE to run the BanKhoaHocApplication.java file
    echo.
    pause
)
