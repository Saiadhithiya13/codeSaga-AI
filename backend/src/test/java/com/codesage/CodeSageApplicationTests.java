package com.codesage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 *
 * <p>This test uses the {@code test} profile which will require PostgreSQL
 * and Redis Testcontainers to be configured in Sprint 2+.
 * For Sprint 1, this serves as a context-load sanity check.
 */
@SpringBootTest
@ActiveProfiles("dev")
class CodeSageApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the full Spring context assembled without errors.
        // This validates: all @Bean definitions, @Configuration classes,
        // auto-wiring, and property binding.
    }
}
