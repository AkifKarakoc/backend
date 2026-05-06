package com.tourguide.chat;

import com.tourguide.chat.dto.ChatResponse;
import com.tourguide.chat.dto.ChatSessionResponse;
import com.tourguide.chat.dto.CreateSessionRequest;
import com.tourguide.chat.dto.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody(required = false) CreateSessionRequest request) {
        if (request == null) {
            request = new CreateSessionRequest();
        }
        ChatSession session = chatService.createSession(userId, request);
        ChatSessionResponse response = ChatSessionResponse.builder()
                .sessionId(session.getId())
                .language(session.getLanguage())
                .startedAt(session.getStartedAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getSessions(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(chatService.getSessions(userId));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ChatResponse> sendMessage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(userId, id, request));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<ChatResponse>> getMessages(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(chatService.getMessages(userId, id));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> endSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        chatService.endSession(userId, id);
        return ResponseEntity.noContent().build();
    }
}
