package com.tourguide.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_CONTEXT_MESSAGES = 12;
    private static final String FALLBACK_RESPONSE = "AI modeline bağlanılamadı. Lütfen daha sonra tekrar deneyin.";

    private final AiModelClient aiModelClient;
    private final ChatKnowledgeService chatKnowledgeService;

    public String generateReply(List<ChatMessage> conversation, String language) {
        try {
            String knowledgeContext = chatKnowledgeService.buildKnowledgeContext(conversation);
            return aiModelClient.generate(buildPrompt(conversation, language, knowledgeContext));
        } catch (Exception e) {
            log.error("AI model call failed: {}", e.getMessage());
            return FALLBACK_RESPONSE;
        }
    }

    private String buildPrompt(List<ChatMessage> conversation, String language, String knowledgeContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                Sen Smart Tourism Guide uygulamasının yerel turist rehberi asistanısın.
                Kullanıcıya kısa, net ve pratik cevap ver.
                Bilmediğin veya emin olmadığın bilgiyi uydurma; gerekirse bunu açıkça söyle.
                Turizm, rota, mekan, gezi planı ve uygulama kullanımı konularında yardımcı ol.
                Eğer "Uygulama veritabanından bulunan ilgili kayıtlar" bölümü varsa, önerilerini öncelikle bu kayıtlardan yap.
                Mekan, rota veya görev adı verirken yalnızca veritabanı bağlamında listelenen adları kullan.
                Veritabanı bağlamında yeterli kayıt yoksa bunu açıkça söyle; dış bilgi gerekiyorsa bunu "genel bilgi" diye ayır.
                Cevap dili: """).append(normalizeLanguage(language)).append("\n\n");

        if (knowledgeContext != null && !knowledgeContext.isBlank()) {
            prompt.append("Uygulama veritabanı bağlamı:\n")
                    .append(knowledgeContext)
                    .append("\n");
        }

        prompt.append("Konuşma geçmişi:\n");
        conversation.stream()
                .skip(Math.max(0, conversation.size() - MAX_CONTEXT_MESSAGES))
                .forEach(message -> prompt
                        .append(message.getRole())
                        .append(": ")
                        .append(message.getContent())
                        .append("\n"));
        prompt.append("\nassistant:");
        return prompt.toString();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank() || language.equalsIgnoreCase("tr")) {
            return "Türkçe";
        }
        if (language.equalsIgnoreCase("en")) {
            return "English";
        }
        return language;
    }
}
