package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.RepositoryFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryScannerServiceTest {

    @Mock
    private RepositoryFileRepository repositoryFileRepository;

    @InjectMocks
    private RepositoryScannerService scannerService;

    @TempDir
    Path tempDir;

    @Test
    void scanAndSaveFiles_filtersIgnoredDirsAndExtensions() throws Exception {
        // Arrange
        Repository repo = new Repository();
        repo.setId(UUID.randomUUID());

        // Create valid file
        Path srcDir = tempDir.resolve("src");
        Files.createDirectory(srcDir);
        Files.writeString(srcDir.resolve("Main.java"), "public class Main {}");

        // Create ignored directory
        Path nodeModules = tempDir.resolve("node_modules");
        Files.createDirectory(nodeModules);
        Files.writeString(nodeModules.resolve("index.js"), "console.log('ignored');");

        // Create unsupported extension
        Files.writeString(srcDir.resolve("image.png"), "binarydata");

        when(repositoryFileRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        List<RepositoryFile> results = scannerService.scanAndSaveFiles(repo, tempDir);

        // Assert
        verify(repositoryFileRepository).deleteByRepositoryId(repo.getId());
        verify(repositoryFileRepository).saveAll(any());
        
        assertEquals(1, results.size(), "Only Main.java should be indexed");
        RepositoryFile indexedFile = results.get(0);
        assertEquals("java", indexedFile.getExtension());
        assertTrue(indexedFile.getPath().endsWith("Main.java"));
    }
}
