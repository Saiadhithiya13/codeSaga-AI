# debug-env.ps1 - debug what env vars get set
$envFile = Join-Path $PSScriptRoot ".env"
Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    Write-Host "SET: $key = $value"
}
Write-Host ""
Write-Host "DB_PASSWORD env = $env:DB_PASSWORD"
Write-Host "JWT_SECRET env  = $env:JWT_SECRET"
