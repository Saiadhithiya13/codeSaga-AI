package com.codesage.domain.debt.service;

import com.codesage.config.TechnicalDebtConfigProperties;
import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.DebtSeverity;
import com.codesage.domain.debt.model.TechnicalDebtFinding;
import com.codesage.domain.debt.model.TechnicalDebtReport;
import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.repository.RepositoryFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class StaticCodeAnalysisService {

    private final RepositoryFileRepository fileRepository;
    private final CodeChunkRepository chunkRepository;
    private final TechnicalDebtConfigProperties config;

    public List<TechnicalDebtFinding> analyze(UUID repositoryId, TechnicalDebtReport report) {
        log.info("Starting static analysis for repository {}", repositoryId);
        List<TechnicalDebtFinding> findings = new ArrayList<>();
        
        List<RepositoryFile> files = fileRepository.findByRepositoryId(repositoryId);
        List<CodeChunk> chunks = chunkRepository.findByRepositoryId(repositoryId);

        for (RepositoryFile file : files) {
            List<CodeChunk> fileChunks = chunks.stream()
                    .filter(c -> c.getRepositoryFile().getId().equals(file.getId()))
                    .toList();

            // 1. Large File Check (Approximated by total bytes or chunk count)
            // A file > threshold bytes is considered large. We map bytes roughly to lines (e.g., 30 bytes/line).
            int approxLines = (int) (file.getSizeBytes() / 30);
            if (approxLines > config.getLargeFileThreshold()) {
                findings.add(TechnicalDebtFinding.builder()
                        .report(report)
                        .filePath(file.getPath())
                        .category(DebtCategory.LARGE_FILE)
                        .severity(approxLines > config.getLargeFileThreshold() * 2 ? DebtSeverity.HIGH : DebtSeverity.MEDIUM)
                        .description("File is exceptionally large (approx " + approxLines + " lines).")
                        .recommendation("Consider breaking this file down into smaller, more cohesive modules or classes.")
                        .build());
            }

            // 2. Deep Nesting & Complexity (Regex based heuristic on chunks)
            for (CodeChunk chunk : fileChunks) {
                if (hasDeepNesting(chunk.getContent())) {
                    findings.add(TechnicalDebtFinding.builder()
                            .report(report)
                            .filePath(file.getPath())
                            .category(DebtCategory.DEEP_NESTING)
                            .severity(DebtSeverity.MEDIUM)
                            .description("Deep indentation/nesting detected in chunk " + chunk.getChunkIndex() + ".")
                            .recommendation("Extract nested logic into separate private methods to improve readability.")
                            .build());
                }
            }
        }
        
        log.info("Completed static analysis. Found {} issues.", findings.size());
        return findings;
    }

    private boolean hasDeepNesting(String content) {
        // Simple heuristic: look for 4+ levels of indentation (spaces or tabs)
        String[] lines = content.split("\n");
        int threshold = config.getDeepNestingThreshold();
        // Assume 4 spaces per indent level
        String deepIndentPatternSpaces = " {" + (threshold * 4) + ",}\\S";
        String deepIndentPatternTabs = "\t{" + threshold + ",}\\S";

        for (String line : lines) {
            if (line.matches(".*" + deepIndentPatternSpaces + ".*") || line.matches(".*" + deepIndentPatternTabs + ".*")) {
                return true;
            }
        }
        return false;
    }
}
