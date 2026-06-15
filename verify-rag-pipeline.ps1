# CodeSage RAG Pipeline — Verification Playbook
# Run each block in order after starting the backend with start-backend.ps1

# ─── Prerequisites ────────────────────────────────────────────────────────────
# 1. Docker services running:    docker compose up -d
# 2. Ollama running locally:     ollama serve   (in a separate terminal)
# 3. Backend running:            .\start-backend.ps1  (from project root)
# 4. You have a JWT from a login (replace TOKEN below)

$BASE     = "http://localhost:8080"
$TOKEN    = "REPLACE_WITH_JWT"
$REPO_ID  = "REPLACE_WITH_REPOSITORY_UUID"   # from POST /api/v1/repositories

# ─── A. Trigger indexing ──────────────────────────────────────────────────────
Invoke-RestMethod -Method POST `
    -Uri "$BASE/api/v1/repositories/$REPO_ID/index" `
    -Headers @{ Authorization = "Bearer $TOKEN" }

# Wait ~30-60s for the async pipeline to complete.
# Poll status until it shows INDEXED:
do {
    Start-Sleep -Seconds 5
    $status = (Invoke-RestMethod -Uri "$BASE/api/v1/repositories/$REPO_ID/index-status" `
        -Headers @{ Authorization = "Bearer $TOKEN" }).data
    Write-Host "Status: $status"
} while ($status -notin @("INDEXED", "EMBEDDING_FAILED", "FAILED"))

# ─── B. DB: repository_files count ───────────────────────────────────────────
# Run in psql (Docker):
Write-Host @"
psql command to run:
  docker exec -it codesage_postgres psql -U codesage_user -d codesage_db -c "SELECT COUNT(*) FROM repository_files;"
Expected: > 1
"@

# ─── C. DB: code_chunks count ─────────────────────────────────────────────────
Write-Host @"
  docker exec -it codesage_postgres psql -U codesage_user -d codesage_db -c "SELECT COUNT(*) FROM code_chunks;"
Expected: > repository_files count (multiple chunks per file)
"@

# ─── D. DB: embedding_status breakdown ────────────────────────────────────────
Write-Host @"
  docker exec -it codesage_postgres psql -U codesage_user -d codesage_db -c \
    "SELECT embedding_status, COUNT(*) FROM code_chunks GROUP BY embedding_status;"
Expected: COMPLETED > 0, FAILED = 0
"@

# ─── E. API: vector stats ─────────────────────────────────────────────────────
$vstats = Invoke-RestMethod -Uri "$BASE/api/v1/repositories/$REPO_ID/vector-stats" `
    -Headers @{ Authorization = "Bearer $TOKEN" }
Write-Host "Vector stats: $($vstats.data | ConvertTo-Json)"
# Expected: total > 0, embedded = total, failed = 0

# ─── F. ChromaDB: collection list + count ─────────────────────────────────────
Write-Host @"
ChromaDB API:
  curl http://localhost:8000/api/v1/collections
  # Find collection "repo_<repo_id_no_dashes>"
  # Then:
  curl http://localhost:8000/api/v1/collections/<collection_id>/count
Expected: count > 0
"@

$chromaCollections = Invoke-RestMethod -Uri "http://localhost:8000/api/v1/collections"
$repoIdNoDash = $REPO_ID -replace "-", ""
$collection = $chromaCollections | Where-Object { $_.name -eq "repo_$repoIdNoDash" }
Write-Host "Chroma collection: $($collection | ConvertTo-Json)"

$count = Invoke-RestMethod -Uri "http://localhost:8000/api/v1/collections/$($collection.id)/count"
Write-Host "Chroma vector count: $count"

# ─── G. Semantic search ───────────────────────────────────────────────────────
$searchResult = Invoke-RestMethod -Method POST `
    -Uri "$BASE/api/v1/repositories/$REPO_ID/search" `
    -Headers @{ Authorization = "Bearer $TOKEN"; "Content-Type" = "application/json" } `
    -Body '{ "query": "What tables exist in this project?", "maxResults": 5 }'

Write-Host "Semantic search results:"
$searchResult.data | ForEach-Object {
    Write-Host "  File: $($_.filePath)  Score: $($_.score)"
    Write-Host "  Snippet: $($_.content.Substring(0, [Math]::Min(200, $_.content.Length)))..."
    Write-Host ""
}
