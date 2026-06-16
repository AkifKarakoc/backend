package com.tourguide.chat;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-service.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaAiModelClient implements AiModelClient {

    private final ChatModel chatModel;

    @Override
    public String generate(String prompt) {
        return chatModel.chat(prompt);
    }
}
