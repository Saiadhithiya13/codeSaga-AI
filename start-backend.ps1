# start-backend.ps1
# Starts the Spring Boot backend with secrets loaded from .env
# Also verifies the local native Ollama instance has the required chat model.
# Usage: powershell -ExecutionPolicy Bypass -File start-backend.ps1

$projectRoot = "c:\Users\saiad\OneDrive\Desktop\Gestalt\codeSage"
$envFile = "$projectRoot\.env"
$localPropsFile = "$projectRoot\backend\local.properties"

# --- Local AI Configuration ---
# qwen3:8b does NOT exist as a pullable Ollama tag. qwen2.5:7b is the
# validated default chat model for local dev. Change here if you'd rather
# use e.g. llama3:8b (make sure to pull it first or let this script pull it).
$ollamaBaseUrl   = "http://localhost:11434"
$chatModel       = "qwen2.5:7b"
$embeddingModel  = "nomic-embed-text"
$chromaUrl       = "http://localhost:8000"

# Parse .env into hashtable
$envVars = @{}
Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $envVars[$parts[0].Trim()] = $parts[1].Trim()
}

# --- Step 1: Verify Ollama is reachable ---
Write-Host "========================================="
Write-Host " Checking local Ollama instance"
Write-Host "========================================="
Write-Host "Checking Ollama API at $ollamaBaseUrl/api/tags ..."

$ollamaAvailable = $false
$tagsResponse = $null
try {
    $tagsResponse = Invoke-RestMethod -Uri "$ollamaBaseUrl/api/tags" -Method Get
    $ollamaAvailable = $true
    Write-Host "Ollama is reachable." -ForegroundColor Green
} catch {
    Write-Host "WARNING: Ollama does not appear to be running at $ollamaBaseUrl." -ForegroundColor Yellow
    Write-Host "Start it first (e.g. run 'ollama serve' in another terminal) and re-run this script." -ForegroundColor Yellow
}

# --- Step 2: Ensure the chat model is pulled ---
if ($ollamaAvailable) {
    $modelPresent = $false

    if ($tagsResponse -and $tagsResponse.models) {
        foreach ($m in $tagsResponse.models) {
            if ($m.name -eq $chatModel -or $m.name -like "$chatModel*") {
                $modelPresent = $true
            }
        }
    }

    if ($modelPresent) {
        Write-Host "Chat model '$chatModel' is already available." -ForegroundColor Green
    } else {
        Write-Host "Chat model '$chatModel' not found locally. Pulling now..." -ForegroundColor Cyan

        $pulled = $false

        # Preferred: REST API pull (non-streaming, blocks until complete)
        try {
            $pullBody = @{ name = $chatModel; stream = $false } | ConvertTo-Json
            Invoke-RestMethod -Uri "$ollamaBaseUrl/api/pull" -Method Post -Body $pullBody -ContentType "application/json" | Out-Null
            $pulled = $true
        } catch {
            Write-Host "API pull failed or timed out, falling back to 'ollama pull' CLI..." -ForegroundColor Yellow
        }

        # Fallback: CLI pull (requires 'ollama' on PATH)
        if (-not $pulled) {
            try {
                & ollama pull $chatModel
                $pulled = $true
            } catch {
                Write-Host "ERROR: Could not pull '$chatModel' automatically." -ForegroundColor Red
                Write-Host "Please run manually:  ollama pull $chatModel" -ForegroundColor Red
            }
        }

        if ($pulled) {
            Write-Host "Successfully pulled '$chatModel'." -ForegroundColor Green
        }
    }

    # Sanity-check the embedding model too (should already be present per setup notes)
    $embeddingPresent = $false
    if ($tagsResponse -and $tagsResponse.models) {
        foreach ($m in $tagsResponse.models) {
            if ($m.name -eq $embeddingModel -or $m.name -like "$embeddingModel*") {
                $embeddingPresent = $true
            }
        }
    }
    if (-not $embeddingPresent) {
        Write-Host "WARNING: Embedding model '$embeddingModel' was not found. Run: ollama pull $embeddingModel" -ForegroundColor Yellow
    } else {
        Write-Host "Embedding model '$embeddingModel' is available." -ForegroundColor Green
    }
}

Write-Host ""

# --- Step 3: Write local.properties for Spring Boot ---
$propsContent = @"
app.jwt.secret=$($envVars['JWT_SECRET'])
app.encryption.key=$($envVars['APP_ENCRYPTION_KEY'])
app.github.client-id=$($envVars['GITHUB_CLIENT_ID'])
app.github.client-secret=$($envVars['GITHUB_CLIENT_SECRET'])
app.github.webhook-secret=$($envVars['GITHUB_WEBHOOK_SECRET'])
spring.datasource.password=$($envVars['DB_PASSWORD'])
spring.datasource.url=jdbc:postgresql://localhost:5432/codesage_db
ai.chroma.url=$chromaUrl
ai.ollama.base-url=$ollamaBaseUrl
ai.ollama.chat-model=$chatModel
ai.ollama.embedding-model=$embeddingModel
"@

$propsContent | Out-File -FilePath $localPropsFile -Encoding UTF8 -NoNewline
Write-Host "Written secrets + AI config to: $localPropsFile"
Write-Host "  ai.ollama.chat-model=$chatModel"
Write-Host "  ai.ollama.embedding-model=$embeddingModel"
Write-Host "  ai.chroma.url=$chromaUrl"
Write-Host ""
Write-Host "Starting Spring Boot backend (dev profile)..."

Set-Location "$projectRoot\backend"

# Use absolute path to local.properties to avoid relative path issues
$localPropsUri = "file:///$($localPropsFile.Replace('\', '/'))"
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.config.additional-location=optional:$localPropsUri"
