package com.codesage.domain.chat.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.chat.dto.ChatMessageDto;
import com.codesage.domain.chat.dto.ChatRequestDto;
import com.codesage.domain.chat.dto.ChatSessionCreateDto;
import com.codesage.domain.chat.dto.ChatSessionDto;
import com.codesage.domain.chat.service.RepositoryChatService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat", description = "Repository RAG Chat endpoints")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RepositoryChatService chatService;

    @Operation(summary = "Create Chat Session")
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionDto> createSession(
            @Valid @RequestBody ChatSessionCreateDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Session created", chatService.createSession(user.getId(), request));
    }

    @Operation(summary = "Get User Chat Sessions")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionDto>> getSessions(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Sessions retrieved", chatService.getUserSessions(user.getId()));
    }

    @Operation(summary = "Get Chat Session")
    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDto> getSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Session retrieved", chatService.getSession(id, user.getId()));
    }

    @Operation(summary = "Delete Chat Session")
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        chatService.deleteSession(id, user.getId());
        return ApiResponse.success("Session deleted");
    }

    @Operation(summary = "Get Chat Messages")
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageDto>> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success("Messages retrieved", chatService.getSessionMessages(id, user.getId()));
    }

    @Operation(summary = "Stream Chat Message (RAG)")
    @PostMapping("/sessions/{id}/messages/stream")
    public SseEmitter streamMessage(
            @PathVariable UUID id,
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        return chatService.streamMessage(id, user.getId(), request.message());
    }
}
