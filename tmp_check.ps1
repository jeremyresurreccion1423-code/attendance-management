$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/login' -WebSession $session
Write-Host ('login-status=' + $login.StatusCode)
$body = @{ username='student1'; password='student123' }
$post = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/login' -Method Post -WebSession $session -Body $body -ContentType 'application/x-www-form-urlencoded'
Write-Host ('post-status=' + $post.StatusCode)
Write-Host ('post-url=' + $post.BaseResponse.ResponseUri.AbsoluteUri)
$scan = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/student/scan' -WebSession $session
Write-Host ('scan-status=' + $scan.StatusCode)
Write-Host ('scan-url=' + $scan.BaseResponse.ResponseUri.AbsoluteUri)
$html = $scan.Content
Write-Host ('scan-has-StartCamera=' + $html.Contains('Start Camera'))
Write-Host ('scan-has-scanForm=' + $html.Contains('scanForm'))
Write-Host ('scan-has-profileTrigger=' + $html.Contains('profile-trigger'))
