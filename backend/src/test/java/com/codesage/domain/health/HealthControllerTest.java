package com.codesage.domain.health;

import com.codesage.domain.health.controller.HealthController;
import com.codesage.domain.health.dto.HealthResponseDto;
import com.codesage.domain.health.service.HealthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static com.codesage.domain.health.dto.HealthResponseDto.ComponentStatus;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link HealthController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation
 * with the Spring MVC infrastructure (routing, serialization, etc.)
 * but without starting the full application context.
 */
@WebMvcTest(HealthController.class)
@ActiveProfiles("dev")
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HealthService healthService;

    @Test
    @DisplayName("GET /api/v1/health returns 200 with UP status when all components are healthy")
    void health_allComponentsUp_returns200WithUpStatus() throws Exception {
        // Arrange
        HealthResponseDto mockResponse = new HealthResponseDto(
                "UP",
                "1.0.0-SNAPSHOT",
                "dev",
                Instant.now(),
                Map.of(
                        "database", ComponentStatus.up(),
                        "redis", ComponentStatus.up()
                )
        );
        when(healthService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.version").value("1.0.0-SNAPSHOT"))
                .andExpect(jsonPath("$.data.environment").value("dev"))
                .andExpect(jsonPath("$.data.components.database.status").value("UP"))
                .andExpect(jsonPath("$.data.components.redis.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health returns 200 with DEGRADED status when Redis is down")
    void health_redisDegraded_returns200WithDegradedStatus() throws Exception {
        // Arrange
        HealthResponseDto mockResponse = new HealthResponseDto(
                "DEGRADED",
                "1.0.0-SNAPSHOT",
                "dev",
                Instant.now(),
                Map.of(
                        "database", ComponentStatus.up(),
                        "redis", ComponentStatus.down("Connection refused: localhost:6379")
                )
        );
        when(healthService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())    // Always 200 — consumer checks 'status' field
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DEGRADED"))
                .andExpect(jsonPath("$.data.components.database.status").value("UP"))
                .andExpect(jsonPath("$.data.components.redis.status").value("DOWN"))
                .andExpect(jsonPath("$.data.components.redis.details").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/health response conforms to ApiResponse envelope")
    void health_responseConformsToApiResponseEnvelope() throws Exception {
        // Arrange
        HealthResponseDto mockResponse = new HealthResponseDto(
                "UP", "1.0.0-SNAPSHOT", "dev", Instant.now(), Map.of()
        );
        when(healthService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert — verify the envelope shape
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.data").isMap());
    }
}
