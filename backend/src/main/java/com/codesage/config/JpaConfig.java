package com.codesage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA-specific configuration.
 *
 * <p>Enables JPA repositories scanning under {@code com.codesage.domain}.
 * Architecture Spec mandates Flyway-only schema management;
 * Hibernate DDL auto is configured as {@code validate} in dev and {@code none} in prod
 * via {@code application-*.yml}.
 *
 * <p>Auditing is enabled via {@code @EnableJpaAuditing} on {@link com.codesage.CodeSageApplication}.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.codesage.domain")
public class JpaConfig {
    // All JPA settings (connection pool, ddl-auto, dialect) are in application-*.yml.
    // This class exists as the explicit boundary for JPA configuration and as
    // a place to add entity listeners, converters, and custom repository factories
    // in future sprints.
}
