package com.tourguide.common.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalAiConfig {

    @Bean
    @ConditionalOnProperty(name = "ai-service.provider", havingValue = "ollama", matchIfMissing = true)
    public ChatModel localChatModel(
            @Value("${local-ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${local-ai.ollama.model-name:qwen3:8b}") String modelName,
            @Value("${local-ai.ollama.temperature:0.4}") double temperature,
            @Value("${local-ai.ollama.think:false}") boolean think,
            @Value("${local-ai.ollama.timeout:PT120S}") Duration timeout) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .think(think)
                .returnThinking(false)
                .timeout(timeout)
                .build();
    }
}
