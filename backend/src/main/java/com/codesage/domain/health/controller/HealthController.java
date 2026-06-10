package com.codesage.domain.health.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.health.dto.HealthResponseDto;
import com.codesage.domain.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system health and liveness checks.
 *
 * <p>Base path: {@code /api/v1/health}
 *
 * <p>This endpoint is always public (no auth required) and is used by:
 * <ul>
 *   <li>Docker health checks</li>
 *   <li>Load balancer / reverse proxy liveness probes</li>
 *   <li>Frontend status page</li>
 *   <li>Monitoring systems</li>
 * </ul>
 */
@Tag(name = "Health", description = "System liveness and infrastructure health endpoints")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(
            summary = "Application health check",
            description = """
                    Returns the overall application status and per-component connectivity
                    for PostgreSQL and Redis. Status can be: UP, DEGRADED, or DOWN.
                    
                    This endpoint is always public and never requires authentication.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Health check completed (check 'status' field for actual health)",
                    content = @Content(schema = @Schema(implementation = HealthResponseDto.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponseDto>> health() {
        HealthResponseDto healthResponse = healthService.checkHealth();
        return ResponseEntity.ok(ApiResponse.success(healthResponse));
    }
}
