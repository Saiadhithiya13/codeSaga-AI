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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service to chunk files deterministically.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CodeChunkingService {

    private final CodeChunkRepository codeChunkRepository;

    // Target ~400 tokens = ~1600 chars. We'll use max 2000 chars per chunk.
    private static final int MAX_CHARS_PER_CHUNK = 2000;
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

    @Transactional
    public void chunkFiles(List<RepositoryFile> files, Path repoRoot) {
        log.info("Starting chunking for {} files", files.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<CodeChunk>>> futures = files.stream()
                    .map(file -> executor.submit(() -> chunkFile(file, repoRoot)))
                    .toList();

            List<CodeChunk> allChunks = new ArrayList<>();
            for (Future<List<CodeChunk>> future : futures) {
                try {
                    allChunks.addAll(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to chunk file concurrently", e);
                }
            }

            log.info("Generated {} chunks. Saving to database...", allChunks.size());
            codeChunkRepository.saveAll(allChunks);
        }
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
