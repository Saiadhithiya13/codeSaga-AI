package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeChunkingServiceTest {

    @Mock
    private CodeChunkRepository codeChunkRepository;

    @InjectMocks
    private CodeChunkingService chunkingService;

    @Captor
    private ArgumentCaptor<List<CodeChunk>> chunksCaptor;

    @TempDir
    Path tempDir;

    @Test
    void chunkFiles_splitsLargeFileDeterministically() throws Exception {
        // Arrange
        Path testFile = tempDir.resolve("Test.java");
        StringBuilder content = new StringBuilder();
        // Generate 300 lines of ~20 chars = ~6000 chars total.
        // With 2000 chars max per chunk, should be roughly 3 chunks.
        for (int i = 0; i < 300; i++) {
            content.append(String.format("public void test%03d() {}\n", i));
        }
        Files.writeString(testFile, content.toString());

        RepositoryFile repoFile = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .path("Test.java")
                .repository(new Repository())
                .build();

        // Act
        chunkingService.chunkFiles(List.of(repoFile), tempDir);

        // Assert
        verify(codeChunkRepository).saveAll(chunksCaptor.capture());
        List<CodeChunk> savedChunks = chunksCaptor.getValue();
        
        assertFalse(savedChunks.isEmpty());
        assertTrue(savedChunks.size() >= 3);
        
        // Verify deterministic continuity
        int nextExpectedStartLine = 1;
        for (int i = 0; i < savedChunks.size(); i++) {
            CodeChunk chunk = savedChunks.get(i);
            assertEquals(i, chunk.getChunkIndex());
            assertEquals(nextExpectedStartLine, chunk.getStartLine());
            assertNotNull(chunk.getContentHash());
            assertTrue(chunk.getTokenEstimate() > 0);
            nextExpectedStartLine = chunk.getEndLine() + 1;
        }
        
        assertEquals(301, nextExpectedStartLine); // Total lines + 1
    }
}
