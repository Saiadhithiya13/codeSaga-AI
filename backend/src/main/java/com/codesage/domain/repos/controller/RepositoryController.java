package com.codesage.domain.repos.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.repos.dto.GitHubRepoDto;
import com.codesage.domain.repos.dto.RepositoryContributorDto;
import com.codesage.domain.repos.dto.RepositoryDto;
import com.codesage.domain.repos.dto.RepositoryMetricDto;
import com.codesage.domain.repos.dto.RepositoryFileDto;
import com.codesage.domain.repos.dto.CodeChunkDto;
import com.codesage.domain.repos.dto.SearchRequestDto;
import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import com.codesage.domain.repos.dto.VectorStatsDto;
import com.codesage.domain.repos.model.RepositoryContributor;
import com.codesage.domain.repos.model.RepositoryMetric;
import com.codesage.domain.repos.model.IndexingStatus;
import com.codesage.domain.repos.model.EmbeddingStatus;
import com.codesage.domain.repos.repository.RepositoryContributorRepository;
import com.codesage.domain.repos.repository.RepositoryMetricRepository;
import com.codesage.domain.repos.repository.RepositoryFileRepository;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.service.GitHubRepositorySyncService;
import com.codesage.domain.repos.service.RepositoryService;
import com.codesage.domain.repos.service.ingestion.RepositoryIndexingService;
import com.codesage.domain.repos.service.embedding.RepositoryEmbeddingService;
import com.codesage.domain.repos.service.embedding.SemanticSearchService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Repositories", description = "GitHub repository management")
@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final GitHubRepositorySyncService syncService;
    private final RepositoryContributorRepository contributorRepository;
    private final RepositoryMetricRepository metricRepository;
    
    // Sprint 4
    private final RepositoryIndexingService indexingService;
    private final RepositoryFileRepository fileRepository;
    private final CodeChunkRepository chunkRepository;
    
    // Sprint 5
    private final RepositoryEmbeddingService embeddingService;
    private final SemanticSearchService semanticSearchService;

    @Operation(summary = "List Connected Repositories", description = "Get all repositories connected by the authenticated user")
    @GetMapping
    public ApiResponse<List<RepositoryDto>> getRepositories(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Repositories retrieved", repositoryService.getUserRepositories(user.getId()));
    }

    @Operation(summary = "Get Repository Details", description = "Get a specific connected repository")
    @GetMapping("/{id}")
    public ApiResponse<RepositoryDto> getRepository(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Repository retrieved", repositoryService.getRepository(id, user.getId()));
    }

    @Operation(summary = "List Available Remote Repositories", description = "Fetch repositories from GitHub to connect")
    @GetMapping("/remote")
    public ApiResponse<List<GitHubRepoDto>> getRemoteRepositories(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Remote repositories fetched", repositoryService.getAvailableRemoteRepositories(user.getId()));
    }

    @Operation(summary = "Connect Repository", description = "Connect a GitHub repository to CodeSage")
    @PostMapping
    public ApiResponse<RepositoryDto> connectRepository(@RequestParam String fullName, @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Repository connected", repositoryService.connectRepository(user.getId(), fullName));
    }

    @Operation(summary = "Disconnect Repository", description = "Remove a connected repository")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> disconnectRepository(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        repositoryService.disconnectRepository(id, user.getId());
        return ApiResponse.success("Repository disconnected");
    }

    @Operation(summary = "Manual Sync", description = "Force a synchronization of repository metadata and contributors")
    @PostMapping("/{id}/sync")
    public ApiResponse<Void> syncRepository(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        syncService.syncRepository(id, user.getId());
        return ApiResponse.success("Repository sync completed");
    }

    @Operation(summary = "Get Repository Contributors", description = "List contributors for a connected repository")
    @GetMapping("/{id}/contributors")
    public ApiResponse<List<RepositoryContributorDto>> getContributors(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check via service first
        repositoryService.getRepository(id, user.getId());
        
        List<RepositoryContributorDto> contributors = contributorRepository.findByRepositoryIdOrderByContributionsDesc(id).stream()
                .map(c -> new RepositoryContributorDto(c.getId(), c.getUsername(), c.getAvatarUrl(), c.getContributions()))
                .toList();
                
        return ApiResponse.success("Contributors retrieved", contributors);
    }

    @Operation(summary = "Get Repository Metrics", description = "List historical metrics for a connected repository")
    @GetMapping("/{id}/metrics")
    public ApiResponse<List<RepositoryMetricDto>> getMetrics(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check via service first
        repositoryService.getRepository(id, user.getId());
        
        List<RepositoryMetricDto> metrics = metricRepository.findByRepositoryIdOrderByRecordedAtAsc(id).stream()
                .map(m -> new RepositoryMetricDto(m.getId(), m.getContributorCount(), m.getOpenPrCount(), m.getOpenIssueCount(), m.getStarsCount(), m.getForksCount(), m.getRecordedAt()))
                .toList();
                
        return ApiResponse.success("Metrics retrieved", metrics);
    }

    // --- Sprint 4: Repository Ingestion & Indexing ---

    @Operation(summary = "Start Indexing", description = "Asynchronously clone, scan, and chunk the repository source code")
    @PostMapping("/{id}/index")
    public ApiResponse<Void> startIndexing(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check via service first
        repositoryService.getRepository(id, user.getId());
        indexingService.startIndexingAsync(id, user.getId());
        return ApiResponse.success("Indexing pipeline started asynchronously");
    }

    @Operation(summary = "Start Indexing (No Auth)", description = "Test endpoint")
    @GetMapping("/trigger-index/{id}")
    public ApiResponse<Void> triggerIndex(@PathVariable UUID id) {
        UUID userId = UUID.fromString("57db9884-ffe3-4515-8edb-9d2a07ed1598");
        indexingService.startIndexingAsync(id, userId);
        return ApiResponse.success("Indexing pipeline started asynchronously");
    }

    @Operation(summary = "Get Indexing Status", description = "Get the current indexing status of a connected repository")
    @GetMapping("/{id}/index-status")
    public ApiResponse<IndexingStatus> getIndexingStatus(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        RepositoryDto repo = repositoryService.getRepository(id, user.getId());
        // For simplicity, we can fetch the entity again or map status into RepositoryDto. 
        // We'll fetch the entity to get the latest status directly.
        IndexingStatus status = repositoryService.getRepositoryEntity(id, user.getId()).getIndexingStatus();
        return ApiResponse.success("Status retrieved", status);
    }

    @Operation(summary = "Get Repository Files", description = "List all indexed files for a repository")
    @GetMapping("/{id}/files")
    public ApiResponse<List<RepositoryFileDto>> getFiles(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check
        repositoryService.getRepository(id, user.getId());
        List<RepositoryFileDto> files = fileRepository.findByRepositoryId(id).stream()
                .map(f -> new RepositoryFileDto(f.getId(), f.getPath(), f.getExtension(), f.getSizeBytes(), f.getShaHash(), f.getLastIndexedAt()))
                .toList();
        return ApiResponse.success("Files retrieved", files);
    }

    @Operation(summary = "Get Repository Code Chunks", description = "List all generated code chunks for a repository")
    @GetMapping("/{id}/chunks")
    public ApiResponse<List<CodeChunkDto>> getChunks(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check
        repositoryService.getRepository(id, user.getId());
        List<CodeChunkDto> chunks = chunkRepository.findByRepositoryId(id).stream()
                .map(c -> new CodeChunkDto(c.getId(), c.getChunkIndex(), c.getStartLine(), c.getEndLine(), c.getContent(), c.getContentHash(), c.getTokenEstimate()))
                .toList();
        return ApiResponse.success("Chunks retrieved", chunks);
    }

    // --- Sprint 5: AI Embeddings & Vector Search ---

    @Operation(summary = "Start Embedding Generation", description = "Generate and store embeddings for all unembedded code chunks")
    @PostMapping("/{id}/embed")
    public ApiResponse<Void> startEmbedding(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check
        repositoryService.getRepository(id, user.getId());
        embeddingService.startRepositoryEmbeddingAsync(id);
        return ApiResponse.success("Embedding generation started asynchronously");
    }

    @Operation(summary = "Semantic Search", description = "Query the repository vector database for semantically similar code chunks")
    @PostMapping("/{id}/search")
    public ApiResponse<List<SemanticSearchResultDto>> search(
            @PathVariable UUID id, 
            @Valid @RequestBody SearchRequestDto request, 
            @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check
        repositoryService.getRepository(id, user.getId());
        int max = request.maxResults() != null ? request.maxResults() : 5;
        List<SemanticSearchResultDto> results = semanticSearchService.search(id, request.query(), max);
        return ApiResponse.success("Search results retrieved", results);
    }

    @Operation(summary = "Get Vector Statistics", description = "Returns the status of embeddings for the repository")
    @GetMapping("/{id}/vector-stats")
    public ApiResponse<VectorStatsDto> getVectorStats(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        // Enforce ownership check
        repositoryService.getRepository(id, user.getId());
        
        List<com.codesage.domain.repos.model.CodeChunk> allChunks = chunkRepository.findByRepositoryId(id);
        long total = allChunks.size();
        long embedded = allChunks.stream().filter(c -> c.getEmbeddingStatus() == EmbeddingStatus.COMPLETED).count();
        long pending = allChunks.stream().filter(c -> c.getEmbeddingStatus() == EmbeddingStatus.PENDING || c.getEmbeddingStatus() == EmbeddingStatus.PROCESSING).count();
        long failed = allChunks.stream().filter(c -> c.getEmbeddingStatus() == EmbeddingStatus.FAILED || c.getEmbeddingStatus() == EmbeddingStatus.DEAD_LETTER).count();

        VectorStatsDto stats = new VectorStatsDto(total, embedded, pending, failed);
        return ApiResponse.success("Vector stats retrieved", stats);
    }
}
