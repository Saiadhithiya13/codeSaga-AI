package com.codesage.domain.prreview.service;

import com.codesage.domain.prreview.dto.PullRequestFindingDto;
import com.codesage.domain.prreview.model.PrReviewCategory;
import com.codesage.domain.prreview.model.PrReviewSeverity;
import com.codesage.domain.prreview.model.PullRequestFinding;
import com.codesage.domain.prreview.model.PullRequestReview;
import com.codesage.domain.repos.dto.SearchRequestDto;
import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import com.codesage.domain.repos.service.embedding.SemanticSearchService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class AiPullRequestReviewService {

    private final SemanticSearchService semanticSearchService;
    private final PrReviewExtractor extractor;

    interface PrReviewExtractor {
        @SystemMessage({
                "You are an expert Senior Software Engineer reviewing a Pull Request.",
                "Review the provided code diffs, considering the original repository context.",
                "Identify bugs, security issues, performance problems, and maintainability concerns.",
                "Provide structured feedback."
        })
        @UserMessage("Review the following PR context:\n\n{{it}}")
        List<PullRequestFindingDto> extractFindings(String context);
    }

    public AiPullRequestReviewService(SemanticSearchService semanticSearchService, ChatModel chatLanguageModel) {
        this.semanticSearchService = semanticSearchService;
        this.extractor = AiServices.create(PrReviewExtractor.class, chatLanguageModel);
    }

    public List<PullRequestFinding> analyze(PullRequestReview review, List<PullRequestDiffService.DiffFile> diffFiles) {
        List<PullRequestFinding> allFindings = new ArrayList<>();
        
        for (PullRequestDiffService.DiffFile diffFile : diffFiles) {
            if (diffFile.getFilePath() == null || diffFile.getDiffContent() == null) {
                continue;
            }
            
            // Context retrieval: Find related code for this file's context
            // We search using the filename and first few lines of diff to find surrounding architecture context
            String query = "Architecture context for " + diffFile.getFilePath();
            List<SemanticSearchResultDto> relatedContext = semanticSearchService.search(
                    review.getRepository().getId(), query, 3
            );

            String contextPrompt = buildPromptContext(diffFile, relatedContext);

            try {
                List<PullRequestFindingDto> dtos = extractor.extractFindings(contextPrompt);
                for (PullRequestFindingDto dto : dtos) {
                    allFindings.add(PullRequestFinding.builder()
                            .review(review)
                            .filePath(diffFile.getFilePath())
                            .category(dto.category() != null ? dto.category() : PrReviewCategory.OTHER)
                            .severity(dto.severity() != null ? dto.severity() : PrReviewSeverity.MEDIUM)
                            .confidenceScore(dto.confidenceScore() != null ? dto.confidenceScore() : 80)
                            .description(dto.description() != null ? dto.description() : "AI identified issue")
                            .recommendation(dto.recommendation() != null ? dto.recommendation() : "Refactor code")
                            .build());
                }
            } catch (Exception e) {
                log.error("Failed to extract PR findings for file {}", diffFile.getFilePath(), e);
            }
        }
        
        return allFindings;
    }

    private String buildPromptContext(PullRequestDiffService.DiffFile diffFile, List<SemanticSearchResultDto> relatedContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(diffFile.getFilePath()).append("\n");
        sb.append("Additions: ").append(diffFile.getAdditions()).append("\n");
        sb.append("Deletions: ").append(diffFile.getDeletions()).append("\n");
        sb.append("Git Diff:\n").append(diffFile.getDiffContent()).append("\n\n");
        
        if (!relatedContext.isEmpty()) {
            sb.append("Related Repository Context (Do not review this code, use it to understand dependencies):\n");
            for (SemanticSearchResultDto res : relatedContext) {
                sb.append("--- Context File: ").append(res.filePath()).append(" ---\n");
                sb.append(res.content()).append("\n\n");
            }
        }
        
        return sb.toString();
    }
}
