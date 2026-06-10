package com.codesage.domain.debt.service;

import com.codesage.config.TechnicalDebtConfigProperties;
import com.codesage.domain.debt.dto.TechnicalDebtFindingDto;
import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.DebtSeverity;
import com.codesage.domain.debt.model.TechnicalDebtFinding;
import com.codesage.domain.debt.model.TechnicalDebtReport;
import com.codesage.domain.repos.model.CodeChunk;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AiDebtAnalysisService {

    private final DebtExtractor extractor;
    private final TechnicalDebtConfigProperties config;

    interface DebtExtractor {
        @SystemMessage({
            "You are an expert software architect analyzing code for technical debt.",
            "Analyze the provided code chunks and identify code smells, bad practices, and maintainability issues.",
            "Return a list of findings."
        })
        @UserMessage("Analyze the following code for technical debt:\n\n{{it}}")
        List<TechnicalDebtFindingDto> extractFindings(String codeContext);
    }

    public AiDebtAnalysisService(ChatModel chatLanguageModel, TechnicalDebtConfigProperties config) {
        this.extractor = AiServices.create(DebtExtractor.class, chatLanguageModel);
        this.config = config;
    }

    public List<TechnicalDebtFinding> analyze(List<CodeChunk> chunks, List<TechnicalDebtFinding> staticFindings, TechnicalDebtReport report) {
        log.info("Starting AI debt analysis on {} chunks", chunks.size());
        
        // 1. Filter chunks (hybrid approach)
        // Keep chunks that were flagged by static analysis
        List<String> flaggedPaths = staticFindings.stream()
                .map(TechnicalDebtFinding::getFilePath)
                .distinct()
                .toList();

        List<CodeChunk> candidates = new ArrayList<>();
        List<CodeChunk> unflagged = new ArrayList<>();

        for (CodeChunk chunk : chunks) {
            if (flaggedPaths.contains(chunk.getRepositoryFile().getPath())) {
                candidates.add(chunk);
            } else {
                unflagged.add(chunk);
            }
        }

        // 2. Add random sample of unflagged chunks
        Collections.shuffle(unflagged);
        int sampleSize = (int) Math.ceil(unflagged.size() * (config.getAiSamplePercentage() / 100.0));
        candidates.addAll(unflagged.subList(0, sampleSize));

        log.info("AI will analyze {} filtered chunks out of {} total", candidates.size(), chunks.size());

        List<TechnicalDebtFinding> aiFindings = new ArrayList<>();

        // 3. Process in batches to avoid context length limits
        int batchSize = 10;
        for (int i = 0; i < candidates.size(); i += batchSize) {
            List<CodeChunk> batch = candidates.subList(i, Math.min(i + batchSize, candidates.size()));
            String context = buildContext(batch);
            
            try {
                List<TechnicalDebtFindingDto> dtos = extractor.extractFindings(context);
                for (TechnicalDebtFindingDto dto : dtos) {
                    aiFindings.add(TechnicalDebtFinding.builder()
                            .report(report)
                            .filePath(dto.filePath() != null ? dto.filePath() : "Unknown")
                            .category(dto.category() != null ? dto.category() : DebtCategory.OTHER)
                            .severity(dto.severity() != null ? dto.severity() : DebtSeverity.MEDIUM)
                            .description(dto.description() != null ? dto.description() : "AI identified issue")
                            .recommendation(dto.recommendation() != null ? dto.recommendation() : "Refactor code")
                            .build());
                }
            } catch (Exception e) {
                log.error("AI extraction failed for batch", e);
            }
        }

        log.info("Completed AI debt analysis. Found {} issues.", aiFindings.size());
        return aiFindings;
    }

    private String buildContext(List<CodeChunk> batch) {
        StringBuilder sb = new StringBuilder();
        for (CodeChunk chunk : batch) {
            sb.append("File: ").append(chunk.getRepositoryFile().getPath()).append("\n");
            sb.append("Code:\n").append(chunk.getContent()).append("\n\n");
        }
        return sb.toString();
    }
}
