package com.tourguide.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourguide.chat.dto.ChatResponse;
import com.tourguide.chat.dto.ChatSessionResponse;
import com.tourguide.chat.dto.CreateSessionRequest;
import com.tourguide.chat.dto.SendMessageRequest;
import com.tourguide.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.timeout:30000}")
    private int aiServiceTimeout;

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
        sessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content(request.getContent())
                .build();
        messageRepository.save(userMessage);

        // Call AI service
        String aiResponse = callAiService(sessionId, request.getContent());

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

    private String callAiService(UUID sessionId, String userMessage) {
        try {
            String url = aiServiceBaseUrl + "/api/v1/chat";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("session_id", sessionId.toString());
            requestBody.put("message", userMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("response").asText();
            }

            log.warn("AI service returned non-OK status: {}", response.getStatusCode());
            return "Üzgünüm, şu anda yanıt veremiyorum. Lütfen tekrar deneyin.";

        } catch (RestClientException e) {
            log.error("AI service call failed: {}", e.getMessage());
            return "AI servisine bağlanılamadı. Lütfen daha sonra tekrar deneyin.";
        } catch (Exception e) {
            log.error("Unexpected error calling AI service: {}", e.getMessage());
            return "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.";
        }
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
