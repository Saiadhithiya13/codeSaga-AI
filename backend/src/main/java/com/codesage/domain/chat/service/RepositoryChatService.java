package com.codesage.domain.chat.service;

import com.codesage.exception.ResourceNotFoundException;
import com.codesage.domain.chat.dto.ChatMessageDto;
import com.codesage.domain.chat.dto.ChatSessionCreateDto;
import com.codesage.domain.chat.dto.ChatSessionDto;
import com.codesage.domain.chat.model.ChatMessage;
import com.codesage.domain.chat.model.ChatSession;
import com.codesage.domain.chat.model.MessageRole;
import com.codesage.domain.chat.repository.ChatMessageRepository;
import com.codesage.domain.chat.repository.ChatSessionRepository;
import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.service.RepositoryService;
import com.codesage.domain.repos.service.embedding.SemanticSearchService;
import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.repository.UserRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    private final SemanticSearchService semanticSearchService;
    private final PromptBuilderService promptBuilderService;
    
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    // Use virtual threads for async chat tasks, wrapped to inherit SecurityContext
    private final ExecutorService executor = new org.springframework.security.concurrent.DelegatingSecurityContextExecutorService(Executors.newVirtualThreadPerTaskExecutor());

    @Transactional
    public ChatSessionDto createSession(UUID userId, ChatSessionCreateDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Repository repo = repositoryService.getRepositoryEntity(request.repositoryId(), userId);

        ChatSession session = ChatSession.builder()
                .user(user)
                .repository(repo)
                .title(request.title())
                .build();
        
        session = sessionRepository.save(session);
        return toDto(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionDto> getUserSessions(UUID userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionDto getSession(UUID sessionId, UUID userId) {
        return toDto(getSessionEntity(sessionId, userId));
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = getSessionEntity(sessionId, userId);
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getSessionMessages(UUID sessionId, UUID userId) {
        getSessionEntity(sessionId, userId); // check ownership
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    /**
     * Executes RAG flow and returns an SSE emitter for streaming.
     */
    @Transactional
    public SseEmitter streamMessage(UUID sessionId, UUID userId, String userMessage) {
        ChatSession session = getSessionEntity(sessionId, userId);
        
        // 1. Save user message immediately
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(userMessage)
                .tokenCount(0) // Token count not needed for user msg currently
                .build();
        messageRepository.save(userMsg);
        session.setUpdatedAt(userMsg.getCreatedAt()); // Update session time
        sessionRepository.save(session);

        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout
        
        // Run generation asynchronously
        executor.submit(() -> executeRagFlow(session, userMessage, emitter));
        
        return emitter;
    }

    private void executeRagFlow(ChatSession session, String userMessage, SseEmitter emitter) {
        try {
            // 2. Perform Semantic Search
            UUID repoId = session.getRepository().getId();
            List<SemanticSearchResultDto> searchResults = semanticSearchService.search(repoId, userMessage, 5);
            
            // 3. Build context prompt
            String contextPrompt = promptBuilderService.buildContextualPrompt(userMessage, searchResults);
            
            // 4. Retrieve conversation history (limit to last 10 messages)
            List<ChatMessage> historyEntities = messageRepository.findLatestBySessionId(session.getId(), PageRequest.of(0, 10));
            // Reverse so they are chronological
            List<ChatMessage> chronologicalHistory = new ArrayList<>(historyEntities);
            Collections.reverse(chronologicalHistory);
            
            // Construct LangChain4j messages
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            // Only use the contextual prompt for the current question
            messages.add(new SystemMessage("You are a code assistant answering questions about a codebase."));
            
            for (ChatMessage msg : chronologicalHistory) {
                if (msg.getRole() == MessageRole.USER) {
                    // For the latest message, use the contextual prompt instead of just the user text
                    if (msg.getContent().equals(userMessage)) {
                         messages.add(new UserMessage(contextPrompt));
                    } else {
                         messages.add(new UserMessage(msg.getContent()));
                    }
                } else if (msg.getRole() == MessageRole.ASSISTANT) {
                    messages.add(new AiMessage(msg.getContent()));
                }
            }

            // Fallback if the user message wasn't in history logic (should be)
            if (chronologicalHistory.isEmpty() || !chronologicalHistory.get(chronologicalHistory.size() - 1).getContent().equals(userMessage)) {
                messages.add(new UserMessage(contextPrompt));
            }

            // Send citations metadata to frontend first
            sendCitationsAsEvent(emitter, searchResults);

            // 5. Invoke Gemini with streaming
            StringBuilder fullResponse = new StringBuilder();
            
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    try {
                        fullResponse.append(partialResponse);
                        emitter.send(SseEmitter.event().name("token").data(partialResponse));
                    } catch (IOException e) {
                        log.error("Error sending SSE token", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        // Persist AI response
                        int tokens = 0;
                        if (completeResponse.tokenUsage() != null && completeResponse.tokenUsage().totalTokenCount() != null) {
                            tokens = completeResponse.tokenUsage().totalTokenCount();
                        }
                        saveAssistantMessage(session, fullResponse.toString(), tokens);
                        
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Error completing SSE stream", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Error from Gemini streaming model", error);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("Failed to generate response."));
                        emitter.completeWithError(error);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });

        } catch (Exception e) {
            log.error("Exception during RAG flow", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("Internal server error during RAG flow."));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                // ignore
            }
        }
    }

    private void sendCitationsAsEvent(SseEmitter emitter, List<SemanticSearchResultDto> searchResults) throws IOException {
        // Send a custom event with citations metadata
        emitter.send(SseEmitter.event().name("citations").data(searchResults));
    }

    @Transactional
    protected void saveAssistantMessage(ChatSession session, String content, int tokens) {
        ChatMessage msg = ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .tokenCount(tokens)
                .build();
        messageRepository.save(msg);
        session.setUpdatedAt(msg.getCreatedAt());
        sessionRepository.save(session);
    }

    private ChatSession getSessionEntity(UUID sessionId, UUID userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
    }

    private ChatSessionDto toDto(ChatSession session) {
        return new ChatSessionDto(
                session.getId(),
                session.getRepository().getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ChatMessageDto toMessageDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
