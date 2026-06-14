# CodeSage RAG Pipeline — Forensic Root-Cause Analysis

**Classification:** Staff-Engineer Incident Post-Mortem  
**Status:** CONFIRMED — 7 Distinct Bugs Identified  
**Database State Explained:** `repository_files=1`, `code_chunks=1`, `embedding_status=FAILED`, `indexing_status=INDEXED`

---

## Executive Summary

The RAG pipeline is broken in three independent, compounding layers. None of them are configuration errors — they are code defects baked into the implementation.

**Layer 1 — File Discovery (Why only README.md):** `GitHubTreeService` launches every blob download as an unbounded concurrent virtual thread burst. GitHub's secondary rate limit (max ~90–100 concurrent API requests per token) kicks in almost immediately, causing all-but-the-first download to fail silently. Only `README.md` survives because it is the first file processed — root-level files are returned first by the Tree API. All other failures are swallowed by a per-future `catch (Exception e) { log.error(...) }` and execution continues.

**Layer 2 — Premature INDEXED Status (Why the repo says INDEXED):** `RepositoryIndexingService.startIndexingAsync` sets `indexing_status = INDEXED` the moment `chunkingService.chunkFiles()` returns — regardless of how many files were actually discovered, and regardless of whether any embedding has ever occurred. Embedding is never called from the indexing pipeline. `INDEXED` is a misnomer for "chunking completed."

**Layer 3 — Disconnected Embedding Pipeline (Why embedding is FAILED):** `RepositoryEmbeddingService.startRepositoryEmbeddingAsync` is never invoked by `RepositoryIndexingService`. It must be triggered by a separate manual `POST /{id}/embed` call. When it was triggered (either manually or by the retry scheduler), the single README.md chunk's embedding failed — most likely because Ollama was under load from the concurrent `qwen3:8b` background pull, or because the `PROCESSING` state lock-out prevented recovery after a transient failure.

---

## Part 1 — Complete Pipeline Trace

### 1.1 Entry Point

```
POST /api/v1/repositories/{id}/index
  → RepositoryController.startIndexing()
      → repositoryService.getRepository(id, userId)  // ownership check
      → indexingService.startIndexingAsync(id, userId)  // fires @Async, returns immediately
```

`startIndexingAsync` is annotated `@Async`. Spring Boot 3.5's `TaskExecutionAutoConfiguration` auto-configures a `SimpleAsyncTaskExecutor` (backed by virtual threads when `spring.threads.virtual.enabled=true`) as the default async executor. This works without `@EnableAsync` in Boot 3.x. The call returns `202`-equivalent immediately; the pipeline runs on a background thread.

### 1.2 Async Pipeline Sequence

```
RepositoryIndexingService.startIndexingAsync(repositoryId, userId)
  1. getRepositoryForUpdate()        → @Transactional protected — see Bug #3
  2. updateStatus(INDEXING)          → @Transactional protected — see Bug #3
  3. treeService.downloadRepository(fullName, accessToken)  ← BUG #1 LIVES HERE
  4. scannerService.scanAndSaveFiles(repository, repoPath)
  5. chunkingService.chunkFiles(files, repoPath)
  6. updateStatus(INDEXED)           ← BUG #2 LIVES HERE
  // embeddingService is NEVER CALLED  ← BUG #4
```

### 1.3 File Download Deep-Dive (`GitHubTreeService.downloadRepository`)

```java
// Step A: Fetch tree (recursive=1) — one API call, returns ALL paths with SHAs
GitHubTreeResponseDto treeResponse = gitHubClient.fetchRepositoryTree(accessToken, fullName, "main");

// Step B: Filter to supported files
List<GitHubTreeItemDto> items = treeResponse.getTree().stream()
    .filter(item -> "blob".equals(item.getType()))
    .filter(this::isSupported)  // extension + ignored-dir check
    .toList();

// Step C: Spawn one virtual thread PER FILE — all fire simultaneously
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Void>> futures = items.stream()
        .map(item -> executor.submit(() -> {
            String content = gitHubClient.fetchFileContent(accessToken, fullName, item.getSha());
            Path filePath = tempDir.resolve(item.getPath());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, ...);
            return null;
        }))
        .toList();

    for (Future<Void> future : futures) {
        try {
            future.get();
        } catch (Exception e) {
            log.error("Failed to download file blob", e);  // ← SWALLOWS ALL FAILURES
        }
    }
}
return tempDir;  // ← RETURNED REGARDLESS OF HOW MANY FILES ARE ACTUALLY IN IT
```

For `Bhargava2007/BuildLedger` (a Spring Boot / Maven project), the tree API returns approximately 80–200 files matching the supported extensions (`java`, `xml`, `md`, `properties`, `yml`, `sql`). All 80–200 virtual threads fire HTTP requests to `https://api.github.com/repos/.../git/blobs/{sha}` within milliseconds of each other.

**GitHub's secondary rate limit** applies: "Create too many requests in too short a time" results in HTTP `403` responses with body `{ "message": "You have exceeded a secondary rate limit..." }`. The `GitHubRepositoryClient.fetchFileContent` catches this as `RestClientException` and throws `ExternalServiceException`. That exception propagates through `future.get()`, is caught by the `catch (Exception e)` in `downloadRepository`, logged as `ERROR`, and discarded. Execution continues.

**Why README.md specifically?** The GitHub Tree API returns tree entries in a consistent order — root-level files typically appear before deeply nested ones. `README.md` is a root-level file. Its virtual thread fires first and completes before the rate limit threshold is crossed. Subsequent threads (hitting `src/main/java/...` paths which are deeper in the tree) encounter the rate limit and fail. The `Files.createDirectories(filePath.getParent())` line is not the issue — `tempDir` itself already exists for `README.md` (whose parent is `tempDir`), and the method would create nested dirs correctly if the downloads succeeded.

### 1.4 Scanner (`RepositoryScannerService.scanAndSaveFiles`)

```java
try (Stream<Path> pathStream = Files.walk(repoPath)) {
    targetPaths = pathStream
        .filter(Files::isRegularFile)
        .filter(path -> isSupported(repoPath, path))
        .toList();
}
```

`Files.walk(repoPath)` only sees what is actually on disk in `tempDir`. Since only `README.md` was written, only `README.md` is returned. The scanner correctly deletes old `repository_files` records and saves 1 new one. This is working as designed — the bug is upstream in `GitHubTreeService`.

### 1.5 Chunking (`CodeChunkingService.chunkFiles`)

`README.md` is chunked into 1 `CodeChunk` with `embedding_status = PENDING` (correct after V11 migration fixed the `NOT_EMBEDDED` default). The `@Transactional` boundary is correct here: all virtual thread tasks are pure CPU work (file reads), and `codeChunkRepository.saveAll()` runs on the original transaction thread after `future.get()` completes for all tasks.

### 1.6 Indexing Status Set to INDEXED

```java
// RepositoryIndexingService.java, line 58
updateStatus(repository, IndexingStatus.INDEXED);
```

This executes after `chunkingService.chunkFiles()` returns successfully. At this point: 1 file has been indexed, 1 chunk has been saved, embedding has never been attempted. The status `INDEXED` is set unconditionally — there is no minimum-file check, no embedding-completion check, and no connection to the embedding pipeline.

---

## Part 2 — Complete Bug Inventory

### Bug #1 — `GitHubTreeService`: Unbounded Concurrent Blob Fetch Causes Silent Mass Failure

**File:** `src/main/java/com/codesage/domain/repos/service/ingestion/GitHubTreeService.java`  
**Lines:** 56–78  
**Severity:** CRITICAL — Direct cause of `repository_files = 1`

The `Executors.newVirtualThreadPerTaskExecutor()` creates one thread per file with no concurrency cap. For a repository with N supported files, N simultaneous HTTP requests are fired to `api.github.com`. GitHub's secondary rate limit fires within milliseconds. Failures are silently swallowed per-future and `tempDir` is returned regardless of actual content.

There is also a secondary issue: `treeResponse.getTruncated()` is never checked. GitHub's recursive tree API truncates results at 100,000 tree entries, returning `truncated: true`. If the tree is truncated, a subset of files is silently omitted with no error or warning.

### Bug #2 — `RepositoryIndexingService`: `INDEXED` Set Before (and Without) Embedding

**File:** `src/main/java/com/codesage/domain/repos/service/ingestion/RepositoryIndexingService.java`  
**Line:** 58  
**Severity:** HIGH — Causes misleading status and prevents detection of the real failure

```java
chunkingService.chunkFiles(files, repoPath);   // line 55
updateStatus(repository, IndexingStatus.INDEXED);  // line 58 — set HERE
// embeddingService is never referenced in this class
```

`INDEXED` means "I found files, chunked them, and I'm done." It says nothing about embedding. A repository with 0 files successfully indexed can be marked `INDEXED`. There is no guard checking `files.size() > 0` or any embedding completion signal.

### Bug #3 — `RepositoryIndexingService`: `@Transactional` on `protected` Methods Called via `this` (Self-Invocation)

**File:** `src/main/java/com/codesage/domain/repos/service/ingestion/RepositoryIndexingService.java`  
**Lines:** 76–86  
**Severity:** MEDIUM — Transactions on `updateStatus` and `getRepositoryForUpdate` are non-functional

```java
@Async
public void startIndexingAsync(UUID repositoryId, UUID userId) {
    Repository repository = getRepositoryForUpdate(repositoryId, userId);  // direct call
    updateStatus(repository, IndexingStatus.INDEXING);                     // direct call
    ...
}

@Transactional
protected Repository getRepositoryForUpdate(...) { ... }  // proxy bypassed

@Transactional
protected void updateStatus(...) { ... }  // proxy bypassed
```

Spring's `@Transactional` works via a proxy. When `startIndexingAsync` calls `getRepositoryForUpdate()` and `updateStatus()` directly on `this`, the Spring proxy is bypassed entirely and no transaction is opened. The code works despite this because `SimpleJpaRepository.save()` opens its own transaction internally — but the intent is violated, and any future logic added to `updateStatus()` that relies on a transaction boundary will silently run without one.

Additionally, `protected` methods are not intercepted by Spring AOP's JDK proxy unless `proxyTargetClass=true` (CGLIB). Even with CGLIB, the self-invocation bypass still applies.

### Bug #4 — `RepositoryIndexingService`: Embedding Pipeline Is Completely Disconnected

**File:** `src/main/java/com/codesage/domain/repos/service/ingestion/RepositoryIndexingService.java`  
**Severity:** CRITICAL — Means RAG never becomes operational without a manual second API call

`RepositoryIndexingService` has zero imports of `RepositoryEmbeddingService`. After chunking completes, the embedding pipeline is never triggered. The user must separately call `POST /api/v1/repositories/{id}/embed` to start embedding. This is not documented in any error message, log, or API response. The only way to know embedding is required is to read the source code or notice the status never progresses beyond `INDEXED` with `PENDING` chunks.

The retry scheduler in `RepositoryEmbeddingService.retryFailedEmbeddings()` fires every 60 seconds and will pick up `PENDING` chunks, but:
- It only runs if the backend has been up long enough (first run is 60 seconds after startup)
- It will also continuously retry `FAILED` chunks, potentially masking the underlying error

### Bug #5 — `RepositoryEmbeddingService.processChunks`: `PROCESSING` State Lock-Out on Outer Exception

**File:** `src/main/java/com/codesage/domain/repos/service/embedding/RepositoryEmbeddingService.java`  
**Lines:** 73–98  
**Severity:** HIGH — Can cause chunks to be permanently stuck and never retried

```java
private void processChunks(UUID repositoryId, List<CodeChunk> unembeddedChunks) {
    // Mark ALL chunks as PROCESSING first
    unembeddedChunks.forEach(c -> c.setEmbeddingStatus(EmbeddingStatus.PROCESSING));
    codeChunkRepository.saveAll(unembeddedChunks);  // all set to PROCESSING in DB

    EmbeddingStore<TextSegment> store = chromaDbClient.getStoreForRepository(repositoryId);

    try (ExecutorService executor = ...) {
        ...
        for (Future<Void> future : futures) {
            future.get();  // if this throws an unchecked exception NOT caught inside processBatch...
        }
    } catch (Exception e) {
        log.error("Error during parallel embedding generation...", e);
        // ← chunks remain PROCESSING forever. Retry scheduler queries PENDING and FAILED only.
    }
}
```

`processBatch()` has its own `try-catch` that sets chunks to `FAILED` on exception — this is correct. However, if an exception escapes `processBatch()` before reaching the inner catch (e.g., during `toTextSegment()` if lazy-loading a `RepositoryFile.getRepository()` outside a transaction), the outer `catch (Exception e)` in `processChunks` logs it and returns. The chunks are left in `PROCESSING` state. The retry scheduler only queries for `PENDING` or `FAILED`, so `PROCESSING` chunks are never retried. They are permanently stuck.

### Bug #6 — `ChromaConfig`: Unused Stray `chroma.*` Properties in Dev/Prod YML

**Files:** `application-dev.yml` (lines 74–78), `application-prod.yml`  
**Severity:** LOW — Functional dead code, but indicates architectural drift

```yaml
# application-dev.yml (bottom of file)
chroma:
  host: ${CHROMA_HOST:localhost}
  port: ${CHROMA_PORT:8000}
  collection-name: codesage_vectors
```

`ChromaConfig` binds from `ai.chroma.url` (correct). The stray `chroma.*` properties are not bound by any `@ConfigurationProperties` class and have no effect. They are leftover from a previous design and will confuse anyone maintaining the configuration.

### Bug #7 — ChromaDB Healthcheck Is a No-Op

**File:** `docker-compose.yml`  
**Lines:** ChromaDB service healthcheck block  
**Severity:** MEDIUM — The backend may start before ChromaDB is actually ready

```yaml
chromadb:
  healthcheck:
    test: ["CMD", "true"]  # always exits 0, regardless of ChromaDB state
```

The backend service has `depends_on: chromadb: condition: service_healthy`. Since the ChromaDB healthcheck is `["CMD", "true"]`, it passes immediately at container start even before ChromaDB's HTTP server is listening. The backend can attempt to connect to ChromaDB before it is ready, causing the first embedding operation to fail. This is the most likely explanation for the `FAILED` embedding status on the single README.md chunk.

---

## Part 3 — Verification of Ollama / Embedding Bean Wiring

### 3.1 `OllamaConfig` — Bean Wiring

```java
@Configuration
public class OllamaConfig {
    @Bean @Primary
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)                    // http://ollama:11434 in Docker
                .modelName(embeddingModelName)       // nomic-embed-text
                .timeout(Duration.ofSeconds(120))
                .maxRetries(3)
                .build();
    }

    @Bean @Primary
    public ChatModel chatLanguageModel() { ... }     // OllamaChatModel

    @Bean @Primary
    public StreamingChatModel streamingChatLanguageModel() { ... }  // OllamaStreamingChatModel
}
```

**Assessment: CORRECT.** The `@Primary` annotations ensure Ollama beans take precedence over any auto-configured alternatives. The `autoconfigure.exclude` in `application.yml` provides defence-in-depth against Gemini JARs. `AI_OLLAMA_BASE_URL=http://ollama:11434` is correctly set in `docker-compose.yml`. Bean wiring is sound.

### 3.2 `EmbeddingGenerationService` — API Usage

```java
return embeddingModel.embedAll(segments).content();
```

**Assessment: CORRECT for LangChain4j 1.0.0.** The compiled bytecode confirms `dev/langchain4j/model/output/Response` return type. `embedAll(List<TextSegment>)` returning `Response<List<Embedding>>` is the correct API in lc4j 1.0.0. `.content()` correctly unwraps it.

### 3.3 `ChromaDbClient` — Store Construction

```java
return ChromaEmbeddingStore.builder()
        .baseUrl(chromaConfig.getChromaUrl())   // from ai.chroma.url
        .collectionName("repo_" + repositoryId.toString().replace("-", ""))
        .timeout(Duration.ofSeconds(60))
        .build();
```

**Assessment: CORRECT API usage.** Collection name (`repo_` + 32 hex chars = 37 chars) is valid. The store is cached in `ConcurrentHashMap<UUID, EmbeddingStore>` — this is fine for a long-running service.

**However:** `ChromaConfig` uses `@Value("${ai.chroma.url:http://localhost:8000}")` — this is the correct property path. It is wired correctly.

### 3.4 `RepositoryChatService` — Streaming Chat

The comment in `executeRagFlow()` says `"// 5. Invoke Gemini with streaming"` but the actual call uses `streamingChatModel` — the `@Primary` `OllamaStreamingChatModel` bean. This is a stale comment from the migration off Gemini, but the code is functionally correct.

### 3.5 `RepositoryEmbeddingService` — Never Wired Into Indexing

**Assessment: DISCONNECTED.** The service exists and is correct internally, but is never called by the indexing pipeline. `RepositoryIndexingService` has no reference to it.

---

## Part 4 — Retry Scheduler Analysis

### Can `PENDING → COMPLETED` ever happen?

**Yes, with caveats.**

```java
@Scheduled(fixedDelayString = "${ai.embedding.retry-interval-ms:60000}")
public void retryFailedEmbeddings() {
    List<EmbeddingStatus> targetStatuses = List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED);
    List<CodeChunk> unembeddedChunks = codeChunkRepository.findByEmbeddingStatusIn(targetStatuses);
    ...
    processChunks(entry.getKey(), entry.getValue());
}
```

Spring Boot 3.x's `TaskSchedulingAutoConfiguration` registers `ScheduledAnnotationBeanPostProcessor` which processes `@Scheduled` annotations without requiring explicit `@EnableScheduling`. So the scheduler **does fire**. It queries `PENDING` and `FAILED` chunks every 60 seconds and calls `processChunks()`. If embedding succeeds, status becomes `COMPLETED`.

**The transition can happen but is currently blocked by:**

1. **Bug #5 (PROCESSING lock-out):** If a previous run set chunks to `PROCESSING` and then the outer `catch` was hit, those chunks are permanently excluded from retries.
2. **Bug #7 (ChromaDB false healthy):** If the first embedding attempt failed because ChromaDB wasn't ready, the chunk is now `FAILED`. The scheduler will retry it. If ChromaDB is now ready (user confirms it is healthy), the next retry should succeed — **unless** Bug #5 has left the chunk stuck at `PROCESSING`.
3. **Current DB state:** The chunk is `FAILED` (not `PROCESSING`), so the retry scheduler **will** pick it up. However, since only 1 chunk exists (README.md) and infrastructure is healthy, if the retry succeeds it will only embed README.md — which is insufficient for meaningful RAG.

---

## Part 5 — The `INDEXED` False Positive — Exact Code Path

This is the precise sequence that produced `indexing_status = INDEXED` despite the pipeline having effectively failed:

```
startIndexingAsync called
  → updateStatus(INDEXING)          // repository.indexing_status = INDEXING

  → treeService.downloadRepository()
      // 100+ concurrent blob fetches fired
      // 99+ fail with GitHub secondary rate limit
      // catch per-future swallows all failures
      // tempDir contains ONLY README.md

  → scannerService.scanAndSaveFiles(repository, tempDir)
      // Files.walk finds 1 file: README.md
      // Saves 1 RepositoryFile
      // Returns [readmeRepositoryFile]

  → chunkingService.chunkFiles([readmeRepositoryFile], tempDir)
      // Chunks README.md → 1 CodeChunk (PENDING)
      // Saves to DB
      // Returns normally (no exception thrown)

  → updateStatus(INDEXED)           // ← SET HERE, line 58
      // repository.indexing_status = INDEXED
      // NO EXCEPTION was thrown by any of the above
      // Pipeline considers this SUCCESS

  // embeddingService.startRepositoryEmbeddingAsync() NEVER CALLED
```

The pipeline has no concept of "did I find enough files?" or "did I start embedding?" It treats "chunkFiles returned without throwing" as success. This is structurally incorrect.

---

## Part 6 — Exact Code Changes Required

### Fix 1 — `GitHubTreeService`: Add Concurrency Throttling and Truncation Check

**File:** `backend/src/main/java/com/codesage/domain/repos/service/ingestion/GitHubTreeService.java`

```diff
+import java.util.concurrent.Semaphore;

 public Path downloadRepository(String fullName, String accessToken) throws IOException {
+    // Check if tree was truncated by GitHub API (> 100,000 entries)
+    if (Boolean.TRUE.equals(treeResponse.getTruncated())) {
+        log.warn("Repository tree for {} was truncated by GitHub API. Some files may be missing.", fullName);
+    }
+
     List<GitHubTreeItemDto> items = treeResponse.getTree().stream()
             .filter(item -> "blob".equals(item.getType()))
             .filter(this::isSupported)
             .toList();
+
+    // Throttle concurrent downloads to stay under GitHub's secondary rate limit.
+    // GitHub allows ~90 concurrent requests per token before returning 403.
+    final Semaphore semaphore = new Semaphore(20);

     try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
         List<Future<Void>> futures = items.stream()
                 .map(item -> executor.submit(() -> {
+                    semaphore.acquire();
+                    try {
                         String content = gitHubClient.fetchFileContent(accessToken, fullName, item.getSha());
                         Path filePath = tempDir.resolve(item.getPath());
                         Files.createDirectories(filePath.getParent());
                         Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
+                    } finally {
+                        semaphore.release();
+                    }
                     return (Void) null;
                 }))
                 .toList();
```

### Fix 2 — `RepositoryIndexingService`: Wire Embedding into Indexing Pipeline

**File:** `backend/src/main/java/com/codesage/domain/repos/service/ingestion/RepositoryIndexingService.java`

```diff
+import com.codesage.domain.repos.service.embedding.RepositoryEmbeddingService;

 @RequiredArgsConstructor
 public class RepositoryIndexingService {

     private final RepositoryRepository repositoryRepository;
     private final GitHubTreeService treeService;
     private final RepositoryScannerService scannerService;
     private final CodeChunkingService chunkingService;
     private final UserService userService;
     private final TokenEncryptionService tokenEncryptionService;
+    private final RepositoryEmbeddingService embeddingService;

     @Async
     public void startIndexingAsync(UUID repositoryId, UUID userId) {
         ...
         try {
             ...
             // 3. Chunk Files and Save CodeChunks
             chunkingService.chunkFiles(files, repoPath);

+            // 4. Guard: require at least 1 file to declare success
+            if (files.isEmpty()) {
+                log.warn("No supported files found for {}. Marking FAILED.", repository.getFullName());
+                updateStatus(repository, IndexingStatus.FAILED);
+                return;
+            }

-            // 4. Mark success
+            // 5. Mark chunking complete (files found and chunked)
             updateStatus(repository, IndexingStatus.INDEXED);
             log.info("Successfully completed indexing pipeline for {}", repository.getFullName());

+            // 6. Trigger embedding pipeline (async — non-blocking)
+            embeddingService.startRepositoryEmbeddingAsync(repositoryId);
+            log.info("Embedding pipeline triggered for {}", repository.getFullName());

         } catch (Exception e) {
```

### Fix 3 — `RepositoryIndexingService`: Fix `@Transactional` Self-Invocation

**File:** `backend/src/main/java/com/codesage/domain/repos/service/ingestion/RepositoryIndexingService.java`

Extract `updateStatus` and `getRepositoryForUpdate` to a separate Spring-managed bean so the proxy intercepts them:

```diff
+// New file: RepositoryStatusUpdater.java
+@Service
+@RequiredArgsConstructor
+public class RepositoryStatusUpdater {
+    private final RepositoryRepository repositoryRepository;
+
+    @Transactional
+    public Repository loadForUpdate(UUID repositoryId, UUID userId) {
+        return repositoryRepository.findByIdAndUserId(repositoryId, userId)
+                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));
+    }
+
+    @Transactional
+    public void updateStatus(Repository repository, IndexingStatus status) {
+        repository.setIndexingStatus(status);
+        repositoryRepository.save(repository);
+    }
+}
```

Then in `RepositoryIndexingService`:
```diff
-    @Transactional
-    protected Repository getRepositoryForUpdate(UUID repositoryId, UUID userId) { ... }
-
-    @Transactional
-    protected void updateStatus(Repository repository, IndexingStatus status) { ... }

+    private final RepositoryStatusUpdater statusUpdater;

     @Async
     public void startIndexingAsync(UUID repositoryId, UUID userId) {
-        Repository repository = getRepositoryForUpdate(repositoryId, userId);
-        updateStatus(repository, IndexingStatus.INDEXING);
+        Repository repository = statusUpdater.loadForUpdate(repositoryId, userId);
+        statusUpdater.updateStatus(repository, IndexingStatus.INDEXING);
         ...
-        updateStatus(repository, IndexingStatus.INDEXED);
+        statusUpdater.updateStatus(repository, IndexingStatus.INDEXED);
         ...
-        updateStatus(repository, IndexingStatus.FAILED);
+        statusUpdater.updateStatus(repository, IndexingStatus.FAILED);
     }
```

### Fix 4 — `RepositoryEmbeddingService`: Fix `PROCESSING` State Lock-Out

**File:** `backend/src/main/java/com/codesage/domain/repos/service/embedding/RepositoryEmbeddingService.java`

```diff
     } catch (Exception e) {
         log.error("Error during parallel embedding generation for repository {}", repositoryId, e);
+        // Reset any chunks still stuck in PROCESSING back to FAILED so the retry scheduler picks them up
+        unembeddedChunks.stream()
+                .filter(c -> c.getEmbeddingStatus() == EmbeddingStatus.PROCESSING)
+                .forEach(c -> {
+                    c.setEmbeddingStatus(EmbeddingStatus.FAILED);
+                    c.setLastError("Outer processing failure: " + e.getMessage());
+                });
+        codeChunkRepository.saveAll(unembeddedChunks);
     }
```

### Fix 5 — `docker-compose.yml`: Fix ChromaDB Healthcheck

**File:** `docker-compose.yml`

```diff
 chromadb:
   healthcheck:
-    test: ["CMD", "true"]
+    test: ["CMD-SHELL", "curl -sf http://localhost:8000/api/v1/heartbeat || exit 1"]
     interval: 15s
     timeout: 10s
     retries: 5
     start_period: 30s
```

ChromaDB 0.5.x exposes `GET /api/v1/heartbeat` which returns `{"nanosecond heartbeat": N}`. This ensures the backend only starts after ChromaDB's HTTP server is actually accepting connections.

### Fix 6 — `application-dev.yml`: Remove Stray `chroma.*` Block

**File:** `backend/src/main/resources/application-dev.yml`

```diff
-# ─── ChromaDB (ready for Sprint 5) ──────────────────────────────────────────
-chroma:
-  host: ${CHROMA_HOST:localhost}
-  port: ${CHROMA_PORT:8000}
-  collection-name: codesage_vectors
```

These properties are not bound to any `@ConfigurationProperties` class. The actual binding is in `ChromaConfig` via `${ai.chroma.url}`, which is correctly set in `application.yml` and overridden by `CHROMA_URL` in `docker-compose.yml`.

### Fix 7 — `RepositoryIndexingService`: Add `@EnableAsync` Explicitly

**File:** `backend/src/main/java/com/codesage/CodeSageApplication.java` (or a config class)

While Spring Boot 3.x auto-configures async task execution, the `@Async` semantics are only fully guaranteed when `@EnableAsync` is explicit. Without it, the exact executor used depends on auto-configuration order and may change across Boot versions:

```diff
+import org.springframework.scheduling.annotation.EnableAsync;
+import org.springframework.scheduling.annotation.EnableScheduling;

 @SpringBootApplication
 @EnableJpaAuditing
 @ConfigurationPropertiesScan("com.codesage")
+@EnableAsync
+@EnableScheduling
 public class CodeSageApplication { ... }
```

---

## Part 7 — Complete File Inventory

| File | Role in Pipeline | Bug Present |
|---|---|---|
| `RepositoryController.java` | API entry points; triggers indexing and embedding | No (but exposes separate /embed endpoint that creates UX confusion) |
| `RepositoryIndexingService.java` | Async orchestrator: download → scan → chunk → status | Bugs #2, #3, #4 |
| `GitHubTreeService.java` | Downloads repo files via GitHub Tree + Blob APIs | Bug #1 |
| `GitHubRepositoryClient.java` | Raw HTTP client for GitHub API | No |
| `RepositoryScannerService.java` | Walks tempDir, saves RepositoryFile records | No |
| `CodeChunkingService.java` | Splits files into CodeChunk records | No |
| `RepositoryEmbeddingService.java` | Embeds chunks via Ollama, stores in Chroma | Bug #5 |
| `EmbeddingGenerationService.java` | Wraps `EmbeddingModel.embedAll()` with retry | No |
| `ChromaDbClient.java` | Creates/caches `ChromaEmbeddingStore` per repository | No |
| `SemanticSearchService.java` | Query-time embedding + Chroma vector search | No |
| `RepositoryChatService.java` | RAG chat: semantic search + Ollama streaming | No (stale comment only) |
| `OllamaConfig.java` | Defines `EmbeddingModel`, `ChatModel`, `StreamingChatModel` beans | No |
| `ChromaConfig.java` | Reads `ai.chroma.url` property | No |
| `CodeSageApplication.java` | App entry point; missing `@EnableAsync`, `@EnableScheduling` | Bug #7 (minor) |
| `application-dev.yml` | Dev profile config; has stray `chroma.*` block | Bug #6 |
| `docker-compose.yml` | Service orchestration; ChromaDB healthcheck is no-op | Bug #7 |
| `V5__embeddings.sql` | Added `embedding_status` with wrong default `NOT_EMBEDDED` | Fixed by V11 |
| `V11__fix_embedding_status_default.sql` | Migrates `NOT_EMBEDDED → PENDING`; fixes column default | Applied; correct |
| `CodeChunkRepository.java` | Queries for PENDING/FAILED chunks; used by retry scheduler | No |
| `RepositoryFileRepository.java` | `@Modifying deleteByRepositoryId` for clean re-index | No |

---

## Part 8 — Verification Plan

### Step 1: Confirm Root Cause (GitHub Rate Limit)

Enable DEBUG logging and re-trigger indexing. Look for repeated entries of the pattern below in the Spring logs:

```
ERROR - Failed to download file blob
com.codesage.exception.ExternalServiceException: GitHub API — Failed to fetch file content
Caused by: org.springframework.web.client.HttpClientErrorException$Forbidden: 403 Forbidden
```

If present, Bug #1 is confirmed. Count the number of `ERROR` log lines versus the number of `blob` entries in the tree to see how many downloads failed.

### Step 2: Confirm PROCESSING Lock-Out

Query the database before applying fixes:

```sql
SELECT embedding_status, retry_count, last_error
FROM code_chunks
WHERE repository_file_id IN (
    SELECT id FROM repository_files WHERE repository_id = '<your-repo-uuid>'
);
```

If status is `PROCESSING` and `last_error` is null, Bug #5 is active and the chunk is permanently stuck. Reset it manually:

```sql
UPDATE code_chunks SET embedding_status = 'FAILED', last_error = 'manual reset — stuck PROCESSING'
WHERE embedding_status = 'PROCESSING';
```

### Step 3: Verify ChromaDB API Accessibility

From inside the backend container:

```bash
curl -sf http://chromadb:8000/api/v1/heartbeat
# Expected: {"nanosecond heartbeat": 1234567890}
# Failure: connection refused or timeout
```

If this fails, ChromaDB was not ready when the backend attempted the first embedding. Fix 5 resolves this.

### Step 4: Apply All Fixes and Re-Index

After applying all code changes:

1. `docker compose down && docker compose up -d`
2. Wait for all services including the new ChromaDB healthcheck to pass
3. Trigger re-index: `POST /api/v1/repositories/{id}/index`
4. Monitor logs — you should see blob downloads for all files, not just README.md
5. Confirm `repository_files` count matches the expected number of source files
6. Confirm `code_chunks` count is generated proportionally
7. Embedding should auto-start (triggered by indexing pipeline after Fix 2)

### Step 5: Confirm Embeddings Reach ChromaDB

```bash
# Query ChromaDB directly
curl http://localhost:8000/api/v1/collections | python3 -m json.tool
# Should show: repo_{uuid-without-dashes} collection

curl http://localhost:8000/api/v1/collections/repo_{uuid}/count
# Should return a number > 0
```

### Step 6: Validate RAG End-to-End

```bash
curl -X POST http://localhost:8080/api/v1/repositories/{id}/search \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"query": "main application entry point", "maxResults": 5}'
```

Expect 5 results with non-zero `similarityScore`. If `similarityScore = 0.0` or results are empty, Chroma has no vectors for this repository.

---

## Part 9 — Summary Table

| # | Bug | File | Symptom | Fix |
|---|---|---|---|---|
| 1 | Unbounded concurrent blob fetch hits GitHub rate limit | `GitHubTreeService.java:56–78` | `repository_files = 1` (only README.md) | Semaphore(20) throttle |
| 2 | `INDEXED` set after chunking, not embedding | `RepositoryIndexingService.java:58` | `indexing_status = INDEXED` despite nothing embedded | Add file count guard; trigger embedding after chunking |
| 3 | `@Transactional` self-invocation on protected methods | `RepositoryIndexingService.java:76–86` | Transactions silently bypass Spring proxy | Extract to `RepositoryStatusUpdater` bean |
| 4 | Embedding pipeline never called from indexing | `RepositoryIndexingService.java` (no import) | Requires separate `POST /embed` call | Inject `RepositoryEmbeddingService`; call after chunkFiles |
| 5 | `PROCESSING` state orphan on outer exception | `RepositoryEmbeddingService.java:94–97` | Chunks stuck in PROCESSING; never retried | Reset PROCESSING → FAILED in outer catch |
| 6 | ChromaDB healthcheck is `CMD true` (no-op) | `docker-compose.yml` | Backend starts before ChromaDB ready; first embed fails | Use `/api/v1/heartbeat` curl check |
| 7 | Stray `chroma.*` properties in dev/prod YML | `application-dev.yml:74–78` | Configuration confusion; no functional impact | Remove stale block |
