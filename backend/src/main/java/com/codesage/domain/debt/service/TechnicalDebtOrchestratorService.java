package com.codesage.domain.debt.service;

import com.codesage.exception.ResourceNotFoundException;
import com.codesage.domain.debt.dto.TechnicalDebtFindingDto;
import com.codesage.domain.debt.dto.TechnicalDebtReportDto;
import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.DebtSeverity;
import com.codesage.domain.debt.model.TechnicalDebtFinding;
import com.codesage.domain.debt.model.TechnicalDebtReport;
import com.codesage.domain.debt.repository.TechnicalDebtFindingRepository;
import com.codesage.domain.debt.repository.TechnicalDebtReportRepository;
import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Service
@RequiredArgsConstructor
public class TechnicalDebtOrchestratorService {

    private final RepositoryService repositoryService;
    private final CodeChunkRepository chunkRepository;
    private final StaticCodeAnalysisService staticAnalysisService;
    private final AiDebtAnalysisService aiAnalysisService;
    private final TechnicalDebtReportRepository reportRepository;
    private final TechnicalDebtFindingRepository findingRepository;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void triggerAnalysis(UUID repositoryId, UUID userId) {
        Repository repo = repositoryService.getRepositoryEntity(repositoryId, userId);
        
        executor.submit(() -> {
            try {
                runAnalysisPipeline(repo);
            } catch (Exception e) {
                log.error("Failed to run technical debt analysis for repository {}", repositoryId, e);
            }
        });
    }

    @Transactional
    protected void runAnalysisPipeline(Repository repo) {
        log.info("Starting Technical Debt Analysis Pipeline for repo: {}", repo.getFullName());

        TechnicalDebtReport report = TechnicalDebtReport.builder()
                .repository(repo)
                .overallScore(100)
                .maintainabilityScore(100)
                .complexityScore(100)
                .duplicationScore(100)
                .aiAssessment("Analysis in progress...")
                .build();
        report = reportRepository.save(report);

        List<TechnicalDebtFinding> allFindings = new ArrayList<>();

        // 1. Static Analysis
        List<TechnicalDebtFinding> staticFindings = staticAnalysisService.analyze(repo.getId(), report);
        allFindings.addAll(staticFindings);

        // 2. AI Analysis
        List<CodeChunk> allChunks = chunkRepository.findByRepositoryId(repo.getId());
        List<TechnicalDebtFinding> aiFindings = aiAnalysisService.analyze(allChunks, staticFindings, report);
        allFindings.addAll(aiFindings);

        // 3. Save findings
        for (TechnicalDebtFinding finding : allFindings) {
            report.addFinding(finding);
        }

        // 4. Calculate Scores
        calculateScores(report, allFindings);

        // 5. Generate AI Summary (Optional - simple generic for now, could be another Gemini call)
        report.setAiAssessment("Analysis completed. Found " + allFindings.size() + " technical debt issues. " +
                "Critical issues: " + allFindings.stream().filter(f -> f.getSeverity() == DebtSeverity.CRITICAL).count());

        reportRepository.save(report);
        log.info("Technical Debt Analysis Pipeline completed for repo: {}", repo.getFullName());
    }

    private void calculateScores(TechnicalDebtReport report, List<TechnicalDebtFinding> findings) {
        int maintainabilityPenalties = 0;
        int complexityPenalties = 0;
        int duplicationPenalties = 0;

        for (TechnicalDebtFinding finding : findings) {
            int penalty = switch (finding.getSeverity()) {
                case CRITICAL -> 10;
                case HIGH -> 5;
                case MEDIUM -> 2;
                case LOW -> 1;
            };

            switch (finding.getCategory()) {
                case LONG_METHOD, LARGE_CLASS, DEEP_NESTING -> complexityPenalties += penalty;
                case DUPLICATE_LOGIC -> duplicationPenalties += penalty;
                default -> maintainabilityPenalties += penalty;
            }
        }

        int maintainabilityScore = Math.max(0, 100 - maintainabilityPenalties);
        int complexityScore = Math.max(0, 100 - complexityPenalties);
        int duplicationScore = Math.max(0, 100 - duplicationPenalties);

        // Weighted: Maintainability 40%, Complexity 30%, Duplication 20%, Documentation 10% (Assuming docs 100 for now)
        int overallScore = (int) (maintainabilityScore * 0.4 + complexityScore * 0.3 + duplicationScore * 0.2 + 100 * 0.1);

        report.setMaintainabilityScore(maintainabilityScore);
        report.setComplexityScore(complexityScore);
        report.setDuplicationScore(duplicationScore);
        report.setOverallScore(overallScore);
    }

    @Transactional(readOnly = true)
    public TechnicalDebtReportDto getLatestReport(UUID repositoryId, UUID userId) {
        repositoryService.getRepositoryEntity(repositoryId, userId); // check ownership
        return reportRepository.findLatestByRepositoryId(repositoryId)
                .map(this::toReportDto)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicalDebtReport", repositoryId));
    }

    @Transactional(readOnly = true)
    public List<TechnicalDebtFindingDto> getFindings(UUID repositoryId, UUID userId) {
        TechnicalDebtReportDto report = getLatestReport(repositoryId, userId);
        return findingRepository.findByReportIdOrderBySeverityDesc(report.id()).stream()
                .map(this::toFindingDto)
                .toList();
    }

    private TechnicalDebtReportDto toReportDto(TechnicalDebtReport report) {
        return new TechnicalDebtReportDto(
                report.getId(),
                report.getRepository().getId(),
                report.getOverallScore(),
                report.getMaintainabilityScore(),
                report.getComplexityScore(),
                report.getDuplicationScore(),
                report.getAiAssessment(),
                report.getGeneratedAt()
        );
    }

    private TechnicalDebtFindingDto toFindingDto(TechnicalDebtFinding finding) {
        return new TechnicalDebtFindingDto(
                finding.getId(),
                finding.getFilePath(),
                finding.getCategory(),
                finding.getSeverity(),
                finding.getDescription(),
                finding.getRecommendation()
        );
    }
}
