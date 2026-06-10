package com.codesage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * General application bean configuration.
 *
 * <p>Configures shared infrastructure beans that are not
 * tied to a specific concern (Web, Redis, JPA, etc.).
 */
@Configuration
public class AppConfig {

    /**
     * Configures the primary {@link ObjectMapper} with:
     * <ul>
     *   <li>Java 8+ time support via {@link JavaTimeModule}</li>
     *   <li>Instants serialized as ISO-8601 strings, not Unix timestamps</li>
     * </ul>
     */
    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Serialize java.time.Instant as ISO string, not number
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Resolves X-Forwarded-* headers when behind a proxy (like Nginx/Docker).
     * Necessary for correct redirect URIs and secure cookie flags.
     */
    @Bean
    public org.springframework.web.filter.ForwardedHeaderFilter forwardedHeaderFilter() {
        return new org.springframework.web.filter.ForwardedHeaderFilter();
    }

    /**
     * Standard RestTemplate with reasonable timeouts for external API calls.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
