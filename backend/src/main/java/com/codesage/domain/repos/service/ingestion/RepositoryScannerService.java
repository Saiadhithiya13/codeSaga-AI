package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.RepositoryFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Scans a local repository directory, filters supported files, and saves metadata.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryScannerService {

    private final RepositoryFileRepository repositoryFileRepository;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "kt", "js", "ts", "jsx", "tsx", "py", "go",
            "yaml", "yml", "xml", "md", "properties", "sql"
    );

    private static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", "target", "build", ".git", "dist", "coverage"
    );

    /**
     * Scans the repository and saves RepositoryFile records using Virtual Threads.
     *
     * @param repository The database repository entity
     * @param repoPath   The local path to the cloned repository
     * @return List of saved RepositoryFiles
     */
    @Transactional
    public List<RepositoryFile> scanAndSaveFiles(Repository repository, Path repoPath) {
        log.info("Scanning repository files at {}", repoPath);

        // Delete old files for a clean re-index and flush to prevent constraint violations
        repositoryFileRepository.deleteByRepositoryId(repository.getId());
        repositoryFileRepository.flush();

        List<Path> targetPaths;
        try (Stream<Path> pathStream = Files.walk(repoPath)) {
            targetPaths = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> isSupported(repoPath, path))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan repository directory: " + repoPath, e);
        }

        log.info("Found {} supported files in {}", targetPaths.size(), repository.getFullName());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RepositoryFile>> futures = targetPaths.stream()
                    .map(path -> executor.submit(() -> processFile(repository, repoPath, path)))
                    .toList();

            List<RepositoryFile> files = futures.stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException("Failed to process file concurrently", e);
                        }
                    })
                    .toList();

            return repositoryFileRepository.saveAll(files);
        }
    }

    private RepositoryFile processFile(Repository repository, Path repoRoot, Path file) throws IOException, NoSuchAlgorithmException {
        String relativePath = repoRoot.relativize(file).toString().replace('\\', '/');
        String extension = getExtension(file.getFileName().toString());
        byte[] content = Files.readAllBytes(file);
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);
        String shaHash = bytesToHex(hash);

        return RepositoryFile.builder()
                .repository(repository)
                .path(relativePath)
                .extension(extension)
                .sizeBytes((long) content.length)
                .shaHash(shaHash)
                .lastIndexedAt(Instant.now())
                .build();
    }

    private boolean isSupported(Path repoRoot, Path path) {
        Path relativePath = repoRoot.relativize(path);
        // Check ignored directories
        for (int i = 0; i < relativePath.getNameCount(); i++) {
            if (IGNORED_DIRS.contains(relativePath.getName(i).toString())) {
                return false;
            }
        }
        // Check extension
        String fileName = path.getFileName().toString();
        String ext = getExtension(fileName);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
