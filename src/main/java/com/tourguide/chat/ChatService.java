package com.tourguide.chat;

import com.tourguide.chat.dto.ChatResponse;
import com.tourguide.chat.dto.CreateSessionRequest;
import com.tourguide.chat.dto.SendMessageRequest;
import com.tourguide.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "chat:session:";

    @Transactional
    public ChatSession createSession(UUID userId, CreateSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .language(request.getLanguage() != null ? request.getLanguage() : "tr")
                .build();

        session = sessionRepository.save(session);
        log.info("Chat session created: sessionId={} userId={} language={}", session.getId(), userId, session.getLanguage());

        try {
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.getId(), userId.toString());
        } catch (Exception e) {
            log.warn("Failed to cache chat session: sessionId={} userId={} reason={}", session.getId(), userId, e.getMessage());
        }

        return session;
    }

    @Transactional
    public ChatResponse sendMessage(UUID userId, UUID sessionId, SendMessageRequest request) {
        sessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content(request.getContent())
                .build();
        messageRepository.save(userMessage);

        // Stub AI response
        String aiResponse = "Bu bir stub yanittir. Gercek AI servisi baglandiginda bu mesaj degisecektir. Sizin mesajiniz: " + request.getContent();

        ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(aiResponse)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        log.info("Chat message processed: sessionId={} userId={} userMessageLength={} assistantMessageId={}",
                sessionId, userId, request.getContent().length(), assistantMessage.getId());

        return ChatResponse.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(aiResponse)
                .sentAt(assistantMessage.getSentAt())
                .build();
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
            log.warn("Failed to evict chat session cache: sessionId={} userId={} reason={}", sessionId, userId, e.getMessage());
        }

        log.info("Chat session ended: sessionId={} userId={}", sessionId, userId);
    }
}
