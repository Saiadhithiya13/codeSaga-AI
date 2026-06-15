package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service to chunk files deterministically.
 *
 * <p><strong>Threading note:</strong> chunk generation (pure CPU work) is done sequentially
 * on the calling thread to avoid Hibernate session leakage across virtual threads.
 * The {@code saveAll} call is performed inside a single {@code @Transactional} boundary
 * so all chunks for the run are committed atomically.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CodeChunkingService {

    private final CodeChunkRepository codeChunkRepository;

    // Target ~400 tokens = ~1600 chars. We'll use max 2000 chars per chunk.
    private static final int MAX_CHARS_PER_CHUNK = 2000;
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

    /**
     * Chunks all files and persists all chunks in one transaction.
     *
     * @return the total number of chunks created
     */
    @Transactional
    public int chunkFiles(List<RepositoryFile> files, Path repoRoot) {
        log.info("Starting chunking for {} files", files.size());

        // Delete stale chunks for these files before re-chunking to prevent duplicates on re-index.
        // ON DELETE CASCADE on repository_files → code_chunks handles this at the DB level when
        // scanAndSaveFiles calls deleteByRepositoryId, but we guard here too for safety.
        List<UUID> fileIds = files.stream()
                .filter(f -> f.getId() != null)
                .map(RepositoryFile::getId)
                .toList();
        if (!fileIds.isEmpty()) {
            codeChunkRepository.deleteByRepositoryFileIdIn(fileIds);
        }

        List<CodeChunk> allChunks = new ArrayList<>();

        for (RepositoryFile file : files) {
            try {
                List<CodeChunk> chunks = chunkFile(file, repoRoot);
                allChunks.addAll(chunks);
            } catch (Exception e) {
                // Log but continue — one bad file should not abort the whole indexing run
                log.warn("Failed to chunk file {} — skipping. Cause: {}", file.getPath(), e.getMessage());
            }
        }

        log.info("Generated {} chunks across {} files. Saving to database...", allChunks.size(), files.size());
        codeChunkRepository.saveAll(allChunks);
        log.info("Saved {} chunks.", allChunks.size());

        return allChunks.size();
    }

    private List<CodeChunk> chunkFile(RepositoryFile file, Path repoRoot) throws IOException, NoSuchAlgorithmException {
        Path absolutePath = repoRoot.resolve(file.getPath());
        List<String> lines = Files.readAllLines(absolutePath, StandardCharsets.UTF_8);

        List<CodeChunk> chunks = new ArrayList<>();
        StringBuilder currentChunkContent = new StringBuilder();
        int startLine = 1;
        int chunkIndex = 0;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            currentChunkContent.append(line).append("\n");

            // If we hit the char limit, or it's the last line, flush the chunk
            if (currentChunkContent.length() >= MAX_CHARS_PER_CHUNK || i == lines.size() - 1) {
                String content = currentChunkContent.toString();
                byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));

                CodeChunk chunk = CodeChunk.builder()
                        .repositoryFile(file)
                        .chunkIndex(chunkIndex++)
                        .startLine(startLine)
                        .endLine(i + 1)
                        .content(content)
                        .contentHash(bytesToHex(hashBytes))
                        .tokenEstimate(content.length() / CHARS_PER_TOKEN_ESTIMATE)
                        .build();

                chunks.add(chunk);

                // Reset for next chunk
                currentChunkContent.setLength(0);
                startLine = i + 2;
            }
        }

        return chunks;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
