package com.codesage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Retry configuration.
 * 
 * <p>Enables the use of {@code @Retryable} and {@code @Recover} annotations
 * across the application, backed by Spring AOP.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
