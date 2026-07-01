@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo  Attendance Management System
echo ========================================
echo.
echo NOTE: In PowerShell, use:  .\run.ps1
echo       Or in CMD, use:       run.bat
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH.
    echo Install JDK 17+ from https://adoptium.net/
    pause
    exit /b 1
)

set "MAVEN_CMD="
if exist "mvnw.cmd" (
    set "MAVEN_CMD=mvnw.cmd"
) else if exist "%TEMP%\apache-maven\bin\mvn.cmd" (
    set "MAVEN_CMD=%TEMP%\apache-maven\bin\mvn.cmd"
) else (
    echo Downloading Maven...
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0download-maven.ps1"
    set "MAVEN_CMD=%TEMP%\apache-maven\bin\mvn.cmd"
)

set "PROFILE="
echo Using Supabase PostgreSQL database.
echo.

echo Starting server at http://localhost:8081
echo Demo login: admin / admin123
echo Press Ctrl+C to stop.
echo.

call "%MAVEN_CMD%" spring-boot:run %PROFILE% -Djava.net.preferIPv4Stack=true
pause
