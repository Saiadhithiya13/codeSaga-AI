package com.codesage.domain.debt.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.debt.dto.TechnicalDebtFindingDto;
import com.codesage.domain.debt.dto.TechnicalDebtReportDto;
import com.codesage.domain.debt.service.TechnicalDebtOrchestratorService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Technical Debt", description = "Endpoints for analyzing and retrieving technical debt")
@RestController
@RequestMapping("/api/v1/repositories/{id}")
@RequiredArgsConstructor
public class TechnicalDebtController {

    private final TechnicalDebtOrchestratorService debtService;

    @Operation(summary = "Start Technical Debt Analysis")
    @PostMapping("/technical-debt/analyze")
    public ApiResponse<Void> triggerAnalysis(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        debtService.triggerAnalysis(id, user.getId());
        return ApiResponse.success("Analysis triggered successfully. This runs in the background.");
    }

    @Operation(summary = "Get Latest Technical Debt Report")
    @GetMapping("/technical-debt")
    public ApiResponse<TechnicalDebtReportDto> getLatestReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Report retrieved", debtService.getLatestReport(id, user.getId()));
    }

    @Operation(summary = "Get Technical Debt Findings")
    @GetMapping("/technical-debt/findings")
    public ApiResponse<List<TechnicalDebtFindingDto>> getFindings(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Findings retrieved", debtService.getFindings(id, user.getId()));
    }

    @Operation(summary = "Get Repository Health Score")
    @GetMapping("/health-score")
    public ApiResponse<Integer> getHealthScore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        TechnicalDebtReportDto report = debtService.getLatestReport(id, user.getId());
        return ApiResponse.success("Health score retrieved", report.overallScore());
    }
}
