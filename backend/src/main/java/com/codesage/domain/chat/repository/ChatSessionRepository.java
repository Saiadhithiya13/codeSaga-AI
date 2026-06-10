package com.codesage.domain.chat.repository;

import com.codesage.domain.chat.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    
    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId ORDER BY s.updatedAt DESC")
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT s FROM ChatSession s WHERE s.id = :id AND s.user.id = :userId")
    Optional<ChatSession> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
