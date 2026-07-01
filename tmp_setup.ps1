$ErrorActionPreference = 'Stop'
$repo = 'C:\Users\Johnzyril\Desktop\ATTENDANCE MANAGEMENT SYSTEM'
Set-Location $repo

if (-not (Test-Path "$env:TEMP\apache-maven\bin\mvn.cmd")) {
    & "$repo\download-maven.ps1"
}

$maven = "$env:TEMP\apache-maven\bin\mvn.cmd"
Write-Host "MAVEN=$maven"
& $maven -version
