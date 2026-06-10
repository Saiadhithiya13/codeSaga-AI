package com.codesage.domain.chat.service;

import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private final PromptBuilderService promptBuilderService = new PromptBuilderService();

    @Test
    void buildContextualPrompt_withResults_formatsCorrectly() {
        // Arrange
        String userMessage = "Where is authentication?";
        List<SemanticSearchResultDto> searchResults = List.of(
                new SemanticSearchResultDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "src/Auth.java",
                        0,
                        "java",
                        "public class Auth {}",
                        0.95
                )
        );

        // Act
        String prompt = promptBuilderService.buildContextualPrompt(userMessage, searchResults);

        // Assert
        assertTrue(prompt.contains("User Question: Where is authentication?"));
        assertTrue(prompt.contains("File: src/Auth.java"));
        assertTrue(prompt.contains("Language: java"));
        assertTrue(prompt.contains("public class Auth {}"));
    }

    @Test
    void buildContextualPrompt_emptyResults_indicatesNoContext() {
        // Arrange
        String userMessage = "What is the meaning of life?";

        // Act
        String prompt = promptBuilderService.buildContextualPrompt(userMessage, List.of());

        // Assert
        assertTrue(prompt.contains("No relevant code snippets were found."));
        assertTrue(prompt.contains("User Question: What is the meaning of life?"));
    }
}
