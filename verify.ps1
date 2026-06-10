$ErrorActionPreference = "Stop"

Write-Host "========================================="
Write-Host " CodeSage Verification Script "
Write-Host "========================================="

Write-Host "`n[1] Verifying Ollama Models..."
try {
    $ollamaTags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags"
    Write-Host "Ollama is running. Available models:"
    $ollamaTags.models | ForEach-Object { Write-Host " - $($_.name)" }
} catch {
    Write-Host "Ollama is not accessible at localhost:11434. Is the container running?" -ForegroundColor Red
}

Write-Host "`n[2] Verifying Postgres Database (Indexing & Chunks)..."
try {
    $repoFilesQuery = "SELECT COUNT(*) FROM repository_files;"
    $chunksQuery = "SELECT COUNT(*) FROM code_chunks;"
    $embeddingQuery = "SELECT embedding_status, COUNT(*) FROM code_chunks GROUP BY embedding_status;"
    
    Write-Host "Running DB Queries inside codesage_postgres container..."
    Write-Host "Repository Files Count:"
    docker exec codesage_postgres psql -U codesage_user -d codesage_db -c $repoFilesQuery
    
    Write-Host "Code Chunks Count:"
    docker exec codesage_postgres psql -U codesage_user -d codesage_db -c $chunksQuery
    
    Write-Host "Embedding Status Counts:"
    docker exec codesage_postgres psql -U codesage_user -d codesage_db -c $embeddingQuery
} catch {
    Write-Host "Failed to query Postgres. Is the codesage_postgres container running?" -ForegroundColor Red
}

Write-Host "`n[3] Verifying ChromaDB..."
try {
    Write-Host "Getting collection counts from ChromaDB..."
    $chromaRes = Invoke-RestMethod -Uri "http://localhost:8000/api/v1/collections"
    if ($chromaRes.count -gt 0) {
        Write-Host "Collections found:"
        $chromaRes | ForEach-Object { 
            Write-Host " - Name: $($_.name)"
            # Note: Fetching document counts in Chroma requires a specific collection query
        }
    } else {
        Write-Host "No collections found in ChromaDB yet. Have you triggered embedding generation?"
    }
} catch {
    Write-Host "ChromaDB is not accessible at localhost:8000." -ForegroundColor Red
}

Write-Host "`n========================================="
Write-Host " Verification Complete "
Write-Host "========================================="
