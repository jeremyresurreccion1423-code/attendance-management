# Attendance Management System - PowerShell startup script
Set-Location $PSScriptRoot

Write-Host "========================================"
Write-Host " Attendance Management System"
Write-Host "========================================"
Write-Host ""

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Java is not installed or not in PATH."
    Write-Host "Install JDK 17+ from https://adoptium.net/"
    Read-Host "Press Enter to exit"
    exit 1
}

$mvnCmd = $null
if (Test-Path ".\mvnw.cmd") {
    $mvnCmd = ".\mvnw.cmd"
} elseif (Test-Path "$env:TEMP\apache-maven\bin\mvn.cmd") {
    $mvnCmd = "$env:TEMP\apache-maven\bin\mvn.cmd"
} else {
    Write-Host "Downloading Maven..."
    & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\download-maven.ps1"
    $mvnCmd = "$env:TEMP\apache-maven\bin\mvn.cmd"
}

Write-Host "Using Supabase PostgreSQL database."
Write-Host ""
Write-Host "Starting server at http://localhost:8081"
Write-Host "Demo login: admin / admin123"
Write-Host "Press Ctrl+C to stop."
Write-Host ""

& $mvnCmd spring-boot:run "-Djava.net.preferIPv4Stack=true"
