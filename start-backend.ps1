# start-backend.ps1
# Starts the Spring Boot backend with secrets loaded from .env
# Usage: powershell -ExecutionPolicy Bypass -File start-backend.ps1

$projectRoot = "c:\Users\saiad\OneDrive\Desktop\Gestalt\codeSage"
$envFile = "$projectRoot\.env"
$localPropsFile = "$projectRoot\backend\local.properties"

# Parse .env into hashtable
$envVars = @{}
Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $envVars[$parts[0].Trim()] = $parts[1].Trim()
}

# Write a Spring Boot properties file with all required secrets
$propsContent = @"
app.jwt.secret=$($envVars['JWT_SECRET'])
app.encryption.key=$($envVars['APP_ENCRYPTION_KEY'])
app.github.client-id=$($envVars['GITHUB_CLIENT_ID'])
app.github.client-secret=$($envVars['GITHUB_CLIENT_SECRET'])
app.github.webhook-secret=$($envVars['GITHUB_WEBHOOK_SECRET'])
spring.datasource.password=$($envVars['DB_PASSWORD'])
"@

$propsContent | Out-File -FilePath $localPropsFile -Encoding UTF8 -NoNewline
Write-Host "Written secrets to: $localPropsFile"
Write-Host "Starting Spring Boot backend (dev profile)..."

Set-Location "$projectRoot\backend"

# Use absolute path to local.properties to avoid relative path issues
$localPropsUri = "file:///$($localPropsFile.Replace('\', '/'))"
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.config.additional-location=optional:$localPropsUri"
