package com.codesage.domain.chat.service;

import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    public String buildContextualPrompt(String userMessage, List<SemanticSearchResultDto> searchResults) {
        StringBuilder contextBuilder = new StringBuilder();
        
        contextBuilder.append("You are CodeSage, an expert AI software engineer analyzing a codebase.\n\n");
        contextBuilder.append("Use ONLY the following retrieved code snippets to answer the user's question.\n");
        contextBuilder.append("If the answer cannot be determined from the provided context, explicitly state: 'I cannot answer this based on the current repository context.'\n");
        contextBuilder.append("DO NOT hallucinate or provide information outside of this context.\n\n");
        
        contextBuilder.append("When referring to code, you MUST cite the source file path using markdown format (e.g., `src/main/java/MyClass.java`).\n\n");

        contextBuilder.append("--- RETRIEVED CONTEXT ---\n\n");

        if (searchResults.isEmpty()) {
            contextBuilder.append("No relevant code snippets were found.\n\n");
        } else {
            for (SemanticSearchResultDto result : searchResults) {
                contextBuilder.append(String.format("File: %s\n", result.filePath()));
                contextBuilder.append(String.format("Language: %s\n", result.language()));
                contextBuilder.append("Code:\n```").append(result.language()).append("\n");
                contextBuilder.append(result.content()).append("\n```\n\n");
            }
        }

        contextBuilder.append("--- END OF CONTEXT ---\n\n");
        contextBuilder.append("User Question: ").append(userMessage).append("\n");

        return contextBuilder.toString();
    }
}
