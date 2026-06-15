package com.codesage;

import com.codesage.domain.chat.service.RepositoryChatService;
import com.codesage.domain.chat.model.ChatSession;
import com.codesage.domain.chat.model.ChatMessage;
import com.codesage.domain.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.List;

@SpringBootTest
public class ChatTriggerTest {

    @Autowired
    private RepositoryChatService chatService;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    public void testRagFlow() throws Exception {
        UUID repoId = UUID.fromString("be230c8b-ddd1-4240-a7a5-8f078df801be");
        UUID userId = UUID.fromString("57db9884-ffe3-4515-8edb-9d2a07ed1598");

        System.out.println("=========================================");
        System.out.println("CREATING CHAT SESSION FOR: " + repoId);
        com.codesage.domain.chat.dto.ChatSessionCreateDto dto = new com.codesage.domain.chat.dto.ChatSessionCreateDto(repoId, "Test Session");
        com.codesage.domain.chat.dto.ChatSessionDto session = chatService.createSession(userId, dto);
        System.out.println("SESSION CREATED: " + session.id());

        String question = "What does the frontend api.js file do?";
        System.out.println("ASKING QUESTION: " + question);

        SseEmitter emitter = chatService.streamMessage(session.id(), userId, question);

        System.out.println("WAITING FOR RAG FLOW TO COMPLETE (180 SECONDS MAX)...");
        Thread.sleep(180000);

        System.out.println("FETCHING MESSAGES FROM DATABASE...");
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.id());
        for (ChatMessage msg : messages) {
            System.out.println("ROLE: " + msg.getRole());
            System.out.println("CONTENT: " + msg.getContent());
            System.out.println("----------");
        }
        System.out.println("=========================================");
    }
}
