package com.codesage.domain.prreview.service;

import com.codesage.exception.ResourceNotFoundException;
import com.codesage.domain.prreview.dto.GitHubPullRequestDto;
import com.codesage.domain.prreview.dto.PullRequestFindingDto;
import com.codesage.domain.prreview.dto.PullRequestReviewDto;
import com.codesage.domain.prreview.model.PrReviewSeverity;
import com.codesage.domain.prreview.model.PullRequestFinding;
import com.codesage.domain.prreview.model.PullRequestReview;
import com.codesage.domain.prreview.repository.PullRequestFindingRepository;
import com.codesage.domain.prreview.repository.PullRequestReviewRepository;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.service.GitHubRepositoryClient;
import com.codesage.domain.repos.service.RepositoryService;
import com.codesage.security.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Service
@RequiredArgsConstructor
public class PullRequestOrchestratorService {

    private final RepositoryService repositoryService;
    private final TokenEncryptionService tokenEncryptionService;
    private final GitHubRepositoryClient gitHubClient;
    private final PullRequestDiffService diffService;
    private final AiPullRequestReviewService aiReviewService;
    private final PullRequestReviewRepository reviewRepository;
    private final PullRequestFindingRepository findingRepository;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void triggerReview(UUID repositoryId, String pullNumber, UUID userId) {
        Repository repo = repositoryService.getRepositoryEntity(repositoryId, userId);
        String accessToken = tokenEncryptionService.decrypt(repo.getUser().getGithubAccessToken());
        
        executor.submit(() -> {
            try {
                runReviewPipeline(repo, pullNumber, accessToken);
            } catch (Exception e) {
                log.error("Failed to run PR review pipeline for repo {} PR {}", repositoryId, pullNumber, e);
            }
        });
    }

    @Transactional
    protected void runReviewPipeline(Repository repo, String pullNumber, String accessToken) {
        log.info("Starting PR Review Pipeline for repo: {}, PR: {}", repo.getFullName(), pullNumber);

        // 1. Fetch Metadata
        GitHubPullRequestDto prMeta = gitHubClient.fetchPullRequest(accessToken, repo.getFullName(), pullNumber);
        
        PullRequestReview review = PullRequestReview.builder()
                .repository(repo)
                .githubPrId(pullNumber)
                .title(prMeta.getTitle() != null ? prMeta.getTitle() : "PR #" + pullNumber)
                .reviewSummary("Review in progress...")
                .riskScore(0)
                .build();
        review = reviewRepository.save(review);

        // 2. Fetch Diff
        String rawDiff = gitHubClient.fetchPullRequestDiff(accessToken, repo.getFullName(), pullNumber);
        List<PullRequestDiffService.DiffFile> diffFiles = diffService.parseDiff(rawDiff);

        // 3. AI Analysis
        List<PullRequestFinding> findings = aiReviewService.analyze(review, diffFiles);

        for (PullRequestFinding finding : findings) {
            review.addFinding(finding);
        }

        // 4. Calculate Risk Score
        calculateRiskScore(review, findings, diffFiles);

        review.setReviewSummary("AI Review completed. Found " + findings.size() + " findings across " + diffFiles.size() + " files.");
        reviewRepository.save(review);
        log.info("PR Review Pipeline completed for repo: {}, PR: {}", repo.getFullName(), pullNumber);
    }

    private void calculateRiskScore(PullRequestReview review, List<PullRequestFinding> findings, List<PullRequestDiffService.DiffFile> diffFiles) {
        int score = 0;
        
        for (PullRequestFinding finding : findings) {
            int severityWeight = switch (finding.getSeverity()) {
                case CRITICAL -> 25;
                case HIGH -> 15;
                case MEDIUM -> 5;
                case LOW -> 1;
            };
            // Multiply by confidence (0.0 to 1.0)
            score += (int) (severityWeight * (finding.getConfidenceScore() / 100.0));
        }

        // Factor in file change volume (more changes = slightly higher base risk)
        int totalChanges = diffFiles.stream().mapToInt(f -> f.getAdditions() + f.getDeletions()).sum();
        score += Math.min(20, totalChanges / 100);

        review.setRiskScore(Math.min(100, score));
    }

    @Transactional(readOnly = true)
    public List<PullRequestReviewDto> getReviews(UUID repositoryId, UUID userId) {
        repositoryService.getRepositoryEntity(repositoryId, userId);
        return reviewRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId).stream()
                .map(this::toReviewDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PullRequestReviewDto getReview(UUID repositoryId, UUID reviewId, UUID userId) {
        repositoryService.getRepositoryEntity(repositoryId, userId);
        PullRequestReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("PullRequestReview", reviewId));
        return toReviewDto(review);
    }

    @Transactional(readOnly = true)
    public List<PullRequestFindingDto> getFindings(UUID repositoryId, UUID reviewId, UUID userId) {
        repositoryService.getRepositoryEntity(repositoryId, userId);
        return findingRepository.findByReviewIdOrderBySeverityDesc(reviewId).stream()
                .map(this::toFindingDto)
                .toList();
    }

    private PullRequestReviewDto toReviewDto(PullRequestReview review) {
        return new PullRequestReviewDto(
                review.getId(),
                review.getRepository().getId(),
                review.getGithubPrId(),
                review.getTitle(),
                review.getReviewSummary(),
                review.getRiskScore(),
                review.getCreatedAt()
        );
    }

    private PullRequestFindingDto toFindingDto(PullRequestFinding finding) {
        return new PullRequestFindingDto(
                finding.getId(),
                finding.getFilePath(),
                finding.getCategory(),
                finding.getSeverity(),
                finding.getConfidenceScore(),
                finding.getDescription(),
                finding.getRecommendation()
        );
    }
}
