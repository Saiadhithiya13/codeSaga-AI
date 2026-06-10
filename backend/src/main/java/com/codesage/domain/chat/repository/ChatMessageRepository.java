package com.codesage.domain.chat.repository;

import com.codesage.domain.chat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") UUID sessionId);

    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestBySessionId(@Param("sessionId") UUID sessionId, Pageable pageable);
}
