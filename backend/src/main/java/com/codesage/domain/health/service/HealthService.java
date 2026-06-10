package com.codesage.domain.health.service;

import com.codesage.domain.health.dto.HealthResponseDto;
import com.codesage.domain.health.dto.DiagnosticsDto;

/**
 * Contract for the health check service.
 *
 * <p>Separating interface from implementation allows:
 * <ul>
 *   <li>Easy mocking in unit tests</li>
 *   <li>Swapping implementations without changing the controller</li>
 * </ul>
 */
public interface HealthService {

    /**
     * Checks the health of the application and all infrastructure components.
     *
     * @return a {@link HealthResponseDto} reflecting current system status
     */
    HealthResponseDto checkHealth();

    /**
     * Returns global diagnostic statistics for the admin dashboard.
     *
     * @return a {@link DiagnosticsDto} with counts for repos, files, chunks, and embeddings
     */
    DiagnosticsDto getDiagnostics();
}
