package com.codesage.domain.debt.service;

import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.DebtSeverity;
import com.codesage.domain.debt.model.TechnicalDebtFinding;
import com.codesage.domain.debt.model.TechnicalDebtReport;
import com.codesage.domain.debt.repository.TechnicalDebtFindingRepository;
import com.codesage.domain.debt.repository.TechnicalDebtReportRepository;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.service.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TechnicalDebtOrchestratorServiceTest {

    private TechnicalDebtOrchestratorService orchestrator;
    private TechnicalDebtReportRepository reportRepository;
    private StaticCodeAnalysisService staticAnalysisService;
    private AiDebtAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        RepositoryService repositoryService = mock(RepositoryService.class);
        CodeChunkRepository chunkRepository = mock(CodeChunkRepository.class);
        staticAnalysisService = mock(StaticCodeAnalysisService.class);
        aiAnalysisService = mock(AiDebtAnalysisService.class);
        reportRepository = mock(TechnicalDebtReportRepository.class);
        TechnicalDebtFindingRepository findingRepository = mock(TechnicalDebtFindingRepository.class);

        orchestrator = new TechnicalDebtOrchestratorService(
                repositoryService, chunkRepository, staticAnalysisService, aiAnalysisService, reportRepository, findingRepository
        );
    }

    @Test
    void testScoringLogic() {
        Repository repo = new Repository();
        repo.setId(UUID.randomUUID());
        repo.setFullName("test/repo");

        when(reportRepository.save(any(TechnicalDebtReport.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock static findings: 1 Critical Duplicate Logic, 1 High Deep Nesting
        List<TechnicalDebtFinding> staticFindings = List.of(
                TechnicalDebtFinding.builder().category(DebtCategory.DUPLICATE_LOGIC).severity(DebtSeverity.CRITICAL).build(),
                TechnicalDebtFinding.builder().category(DebtCategory.DEEP_NESTING).severity(DebtSeverity.HIGH).build()
        );
        when(staticAnalysisService.analyze(any(), any())).thenReturn(staticFindings);

        // Mock AI findings: 1 Medium Low Cohesion
        List<TechnicalDebtFinding> aiFindings = List.of(
                TechnicalDebtFinding.builder().category(DebtCategory.LOW_COHESION).severity(DebtSeverity.MEDIUM).build()
        );
        when(aiAnalysisService.analyze(anyList(), anyList(), any())).thenReturn(aiFindings);

        // Act
        orchestrator.runAnalysisPipeline(repo);

        // Assert
        ArgumentCaptor<TechnicalDebtReport> captor = ArgumentCaptor.forClass(TechnicalDebtReport.class);
        verify(reportRepository, times(2)).save(captor.capture());

        TechnicalDebtReport finalReport = captor.getAllValues().get(1);

        // Duplication: 1 CRITICAL = 10 penalty -> 90
        assertEquals(90, finalReport.getDuplicationScore());

        // Complexity: 1 HIGH = 5 penalty -> 95
        assertEquals(95, finalReport.getComplexityScore());

        // Maintainability: 1 MEDIUM = 2 penalty -> 98
        assertEquals(98, finalReport.getMaintainabilityScore());

        // Overall: 98*0.4(39.2) + 95*0.3(28.5) + 90*0.2(18) + 100*0.1(10) = 39.2 + 28.5 + 18 + 10 = 95.7 -> 95
        assertEquals(95, finalReport.getOverallScore());
    }
}
