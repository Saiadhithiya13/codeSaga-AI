#!/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# Ollama Docker entrypoint
#
# Starts the Ollama server, waits for it to be ready, then pulls the required
# models. The embedding model (nomic-embed-text, ~274MB) is pulled synchronously
# before the container reports healthy so the backend doesn't embed against a
# missing model. The chat model (qwen2.5:7b, ~4.7GB) is pulled in background.
#
# NOTE: qwen3:8b is NOT a valid Ollama model tag and will fail to pull.
# qwen2.5:7b is the validated default chat model for this project.
# ─────────────────────────────────────────────────────────────────────────────

# Start Ollama server in background
ollama serve &
OLLAMA_PID=$!

echo "[Ollama] Server starting (PID $OLLAMA_PID)..."

# Wait until the server's HTTP API responds
until curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; do
    echo "[Ollama] Waiting for server to be ready..."
    sleep 2
done

echo "[Ollama] Server ready. Pulling required models..."

# Pull embedding model synchronously — container is not healthy until this is done
ollama pull nomic-embed-text
echo "[Ollama] nomic-embed-text pulled successfully."

# Pull chat model in background (large: ~4.7GB)
ollama pull qwen2.5:7b &
echo "[Ollama] qwen2.5:7b pull started in background (this may take several minutes)."

# Keep the server in the foreground (container exits when server exits)
wait $OLLAMA_PID
