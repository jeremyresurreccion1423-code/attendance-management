$zip = "$env:TEMP\apache-maven.zip"
$dir = "$env:TEMP\apache-maven"
Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile $zip
if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
Expand-Archive -Path $zip -DestinationPath $env:TEMP -Force
Rename-Item "$env:TEMP\apache-maven-3.9.6" $dir -Force
