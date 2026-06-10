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

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ai.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${ai.ollama.chat-model:llama3.1:8b}") // Fallback default if not specified
    private String chatModelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public ChatModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatLanguageModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(120))
                .build();
    }
}
