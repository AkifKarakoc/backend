package com.tourguide.chat;

import com.tourguide.chat.dto.ChatResponse;
import com.tourguide.chat.dto.ChatSessionResponse;
import com.tourguide.chat.dto.CreateSessionRequest;
import com.tourguide.chat.dto.SendMessageRequest;
import com.tourguide.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiChatService aiChatService;

    private static final String SESSION_KEY_PREFIX = "chat:session:";

    @Transactional
    public ChatSession createSession(UUID userId, CreateSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .language(request.getLanguage() != null ? request.getLanguage() : "tr")
                .build();

        session = sessionRepository.save(session);

        try {
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.getId(), userId.toString());
        } catch (Exception e) {
            log.warn("Failed to store chat session in Redis: {}", e.getMessage());
        }

        return session;
    }

    @Transactional
    public ChatResponse sendMessage(UUID userId, UUID sessionId, SendMessageRequest request) {
        ChatSession session = sessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content(request.getContent())
                .build();
        messageRepository.save(userMessage);

        List<ChatMessage> conversation = messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
        String aiResponse = aiChatService.generateReply(conversation, session.getLanguage());

        ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(aiResponse)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        return ChatResponse.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(aiResponse)
                .sentAt(assistantMessage.getSentAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessions(UUID userId) {
        return sessionRepository.findByUserIdAndIsActiveTrueOrderByStartedAtDesc(userId).stream()
                .map(session -> ChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .language(session.getLanguage())
                        .startedAt(session.getStartedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getMessages(UUID userId, UUID sessionId) {
        sessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", sessionId));

        return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId).stream()
                .map(msg -> ChatResponse.builder()
                        .sessionId(sessionId)
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void endSession(UUID userId, UUID sessionId) {
        ChatSession session = sessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", sessionId));

        session.setIsActive(false);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        try {
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("Failed to remove chat session from Redis: {}", e.getMessage());
        }
    }
}
