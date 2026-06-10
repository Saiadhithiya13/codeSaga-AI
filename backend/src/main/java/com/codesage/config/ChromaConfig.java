package com.codesage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * ChromaDB connection configuration.
 *
 * <p>Consolidates Chroma connection to a single URL property ({@code ai.chroma.url})
 * so that both local dev and Docker deployments use the same config path.
 *
 * <p>Docker compose sets {@code CHROMA_URL=http://chromadb:8000} via environment;
 * local dev falls back to {@code http://localhost:8000}.
 */
@Configuration
public class ChromaConfig {

    @Value("${ai.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    public String getChromaUrl() {
        return chromaUrl;
    }
}
