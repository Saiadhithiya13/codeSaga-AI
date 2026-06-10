package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.dto.GitHubTreeItemDto;
import com.codesage.domain.repos.dto.GitHubTreeResponseDto;
import com.codesage.domain.repos.service.GitHubRepositoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
@Service
@RequiredArgsConstructor
public class GitHubTreeService {

    private final GitHubRepositoryClient gitHubClient;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "kt", "js", "ts", "jsx", "tsx", "py", "go",
            "yaml", "yml", "xml", "md", "properties", "sql"
    );

    private static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", "target", "build", ".git", "dist", "coverage"
    );

    /**
     * Downloads the repository to a temporary directory using GitHub's Tree API.
     * Only supported source files are downloaded.
     */
    public Path downloadRepository(String fullName, String accessToken) throws IOException {
        // Assume default branch is 'main', but we should ideally fetch the default branch from repo metadata.
        // For now, try 'main', then fallback to 'master' if it fails.
        GitHubTreeResponseDto treeResponse;
        try {
            treeResponse = gitHubClient.fetchRepositoryTree(accessToken, fullName, "main");
        } catch (Exception e) {
            treeResponse = gitHubClient.fetchRepositoryTree(accessToken, fullName, "master");
        }

        Path tempDir = Files.createTempDirectory("codesage-" + fullName.replace("/", "-"));
        log.info("Created temp directory {} for {}", tempDir, fullName);

        List<GitHubTreeItemDto> items = treeResponse.getTree().stream()
                .filter(item -> "blob".equals(item.getType()))
                .filter(this::isSupported)
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = items.stream()
                    .map(item -> executor.submit(() -> {
                        String content = gitHubClient.fetchFileContent(accessToken, fullName, item.getSha());
                        Path filePath = tempDir.resolve(item.getPath());
                        Files.createDirectories(filePath.getParent());
                        Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        return (Void) null;
                    }))
                    .toList();

            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Failed to download file blob", e);
                }
            }
        }

        return tempDir;
    }

    private boolean isSupported(GitHubTreeItemDto item) {
        String path = item.getPath();
        String[] segments = path.split("/");
        
        for (int i = 0; i < segments.length - 1; i++) {
            if (IGNORED_DIRS.contains(segments[i])) {
                return false;
            }
        }
        
        String fileName = segments[segments.length - 1];
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
}
