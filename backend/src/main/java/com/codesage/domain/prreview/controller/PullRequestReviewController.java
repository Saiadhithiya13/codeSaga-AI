package com.codesage.domain.prreview.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.prreview.dto.PullRequestFindingDto;
import com.codesage.domain.prreview.dto.PullRequestReviewDto;
import com.codesage.domain.prreview.service.PullRequestOrchestratorService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Pull Request Reviews", description = "Endpoints for AI-powered PR reviews")
@RestController
@RequestMapping("/api/v1/repositories/{id}/pull-requests")
@RequiredArgsConstructor
public class PullRequestReviewController {

    private final PullRequestOrchestratorService prService;

    @Operation(summary = "Start PR Review")
    @PostMapping("/{prId}/review")
    public ApiResponse<Void> triggerReview(
            @PathVariable UUID id,
            @PathVariable String prId,
            @AuthenticationPrincipal UserPrincipal user) {
        prService.triggerReview(id, prId, user.getId());
        return ApiResponse.success("PR Review triggered successfully. This runs in the background.");
    }

    @Operation(summary = "Get PR Reviews for Repository")
    @GetMapping("/reviews")
    public ApiResponse<List<PullRequestReviewDto>> getReviews(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Reviews retrieved", prService.getReviews(id, user.getId()));
    }

    @Operation(summary = "Get PR Review Details")
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<PullRequestReviewDto> getReview(
            @PathVariable UUID id,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Review retrieved", prService.getReview(id, reviewId, user.getId()));
    }

    @Operation(summary = "Get PR Review Findings")
    @GetMapping("/reviews/{reviewId}/findings")
    public ApiResponse<List<PullRequestFindingDto>> getFindings(
            @PathVariable UUID id,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Findings retrieved", prService.getFindings(id, reviewId, user.getId()));
    }
}
