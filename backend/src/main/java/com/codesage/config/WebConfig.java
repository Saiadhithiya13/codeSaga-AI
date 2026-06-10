package com.codesage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC and CORS configuration.
 *
 * <p>CORS is configured to allow requests from the frontend dev server
 * and production domain. Allowed origins are injected from environment
 * configuration, never hardcoded.
 *
 * <p>Architecture Spec §Security: All tokens travel via HttpOnly cookies.
 * {@code allowCredentials(true)} is required for cookie-based auth to work
 * across the frontend/backend origin boundary.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)         // Required for HttpOnly cookie auth (Sprint 2)
                .maxAge(3600);                  // Pre-flight cache: 1 hour
    }
}
