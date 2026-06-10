package com.codesage.domain.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Response payload for the {@code GET /api/v1/health} endpoint.
 *
 * <p>Reports application status and connectivity to all critical
 * infrastructure components. A {@code status} of {@code "DEGRADED"}
 * means the app is running but one or more dependencies are unavailable.
 *
 * @param status      overall application status: {@code UP}, {@code DEGRADED}, or {@code DOWN}
 * @param version     application version from {@code app.version} property
 * @param environment active Spring profile (dev / prod)
 * @param timestamp   server UTC time at response generation
 * @param components  per-component status map (e.g., database, redis)
 */
@Schema(description = "Application and infrastructure health status")
public record HealthResponseDto(

        @Schema(description = "Overall system status", example = "UP",
                allowableValues = {"UP", "DEGRADED", "DOWN"})
        String status,

        @Schema(description = "Application version", example = "1.0.0-SNAPSHOT")
        String version,

        @Schema(description = "Active Spring profile", example = "dev")
        String environment,

        @Schema(description = "UTC timestamp of health check")
        Instant timestamp,

        @Schema(description = "Status of individual infrastructure components")
        Map<String, ComponentStatus> components
) {

    /**
     * Represents the health status of a single infrastructure component.
     *
     * @param status  {@code UP} or {@code DOWN}
     * @param details optional detail message (e.g., error cause)
     */
    @Schema(description = "Individual component health status")
    public record ComponentStatus(
            @Schema(description = "Component status", example = "UP")
            String status,

            @Schema(description = "Optional detail or error message")
            String details
    ) {
        public static ComponentStatus up() {
            return new ComponentStatus("UP", null);
        }

        public static ComponentStatus down(String details) {
            return new ComponentStatus("DOWN", details);
        }
    }
}
