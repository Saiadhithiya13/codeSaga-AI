package com.codesage.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Configures all AI model beans backed by a local Ollama instance.
 *
 * <p>All three beans are annotated @Primary so that Spring selects them
 * over any auto-configured alternatives (e.g. Google Gemini) even if a
 * Gemini JAR somehow ends up on the classpath.
 *
 * <p>Configuration properties (with defaults for local dev):
 * <ul>
 *   <li>{@code ai.ollama.base-url}      — Ollama server URL (default: http://localhost:11434)</li>
 *   <li>{@code ai.ollama.embedding-model} — embedding model name (default: nomic-embed-text)</li>
 *   <li>{@code ai.ollama.chat-model}    — chat model name (default: qwen3:8b)</li>
 * </ul>
 */
@Configuration
public class OllamaConfig {

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ai.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${ai.ollama.chat-model:qwen3:8b}")
    private String chatModelName;

    /**
     * Primary EmbeddingModel bean — always wins over any Gemini auto-config.
     * Increased timeout to 120s and 3 retries for large code chunks.
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(3)
                .build();
    }

    /**
     * Primary ChatLanguageModel bean (non-streaming, used for batch/tool calls).
     */
    @Bean
    @Primary
    public ChatModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(180))
                .build();
    }

    /**
     * Primary StreamingChatLanguageModel bean (used for SSE streaming in RAG chat).
     */
    @Bean
    @Primary
    public StreamingChatModel streamingChatLanguageModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(180))
                .build();
    }
}
