package com.codesage.domain.health.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.health.dto.DiagnosticsDto;
import com.codesage.domain.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Diagnostics", description = "Global system diagnostics")
@RestController
@RequestMapping("/api/v1/health/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final HealthService healthService;

    @Operation(summary = "Get Global Diagnostics", description = "Get global counts for admin dashboard")
    @GetMapping
    public ResponseEntity<ApiResponse<DiagnosticsDto>> getDiagnostics() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getDiagnostics()));
    }
}
