package com.tourguide.chat;

import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.quest.Quest;
import com.tourguide.quest.QuestRepository;
import com.tourguide.route.Route;
import com.tourguide.route.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKnowledgeService {

    private static final int MAX_PLACES = 8;
    private static final int MAX_ROUTES = 4;
    private static final int MAX_QUESTS = 4;
    private static final int MAX_CONTEXT_CHARS = 5000;

    private static final Set<String> STOP_WORDS = Set.of(
            "bir", "bu", "su", "şu", "ve", "veya", "ile", "icin", "için", "de", "da", "mi", "mı", "mu", "mü",
            "ben", "bana", "bunu", "nasıl", "nasil", "ne", "nerede", "neresi", "oner", "öner", "oneri", "öneri",
            "plan", "gezi", "rota", "yap", "yapar", "misin", "mısın", "istiyorum", "var", "olan", "yakın", "yakin"
    );

    private final PlaceRepository placeRepository;
    private final RouteRepository routeRepository;
    private final QuestRepository questRepository;

    @Transactional(readOnly = true)
    public String buildKnowledgeContext(List<ChatMessage> conversation) {
        String userQuery = latestUserMessage(conversation);
        List<String> tokens = tokenize(userQuery);
        if (tokens.isEmpty()) {
            return "";
        }

        try {
            List<Place> places = rankPlaces(tokens);
            List<Route> routes = rankRoutes(tokens);
            List<Quest> quests = rankQuests(tokens);

            if (places.isEmpty() && routes.isEmpty() && quests.isEmpty()) {
                return """
                        Uygulama veritabanında bu isteğe doğrudan uyan kayıt bulunamadı.
                        Kullanıcıya bunu kısa söyle ve genel öneri verirsen bunun veritabanı kaydı olmadığını belirt.
                        """;
            }

            StringBuilder context = new StringBuilder();
            context.append("""
                    Uygulama veritabanından bulunan ilgili kayıtlar aşağıdadır.
                    Cevap verirken öncelikle bu kayıtları kullan. Kayıtlarda olmayan mekan, rota veya görevleri kesin varmış gibi söyleme.
                    """);

            appendPlaces(context, places);
            appendRoutes(context, routes);
            appendQuests(context, quests);

            if (context.length() > MAX_CONTEXT_CHARS) {
                return context.substring(0, MAX_CONTEXT_CHARS) + "\n[Bağlam uzun olduğu için kesildi]\n";
            }
            return context.toString();
        } catch (Exception e) {
            log.warn("Failed to build chat knowledge context: {}", e.getMessage());
            return "";
        }
    }

    private String latestUserMessage(List<ChatMessage> conversation) {
        if (conversation == null) {
            return "";
        }
        for (int i = conversation.size() - 1; i >= 0; i--) {
            ChatMessage message = conversation.get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                return Objects.toString(message.getContent(), "");
            }
        }
        return "";
    }

    private List<String> tokenize(String text) {
        String normalized = normalize(text);
        return Arrays.stream(normalized.split("[^a-z0-9çğıöşü]+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private List<Place> rankPlaces(List<String> tokens) {
        return placeRepository.findByIsActiveTrue().stream()
                .map(place -> new ScoredItem<>(place, scorePlace(place, tokens)))
                .filter(item -> item.score() > 0)
                .sorted()
                .limit(MAX_PLACES)
                .map(ScoredItem::item)
                .toList();
    }

    private List<Route> rankRoutes(List<String> tokens) {
        return routeRepository.findByIsActiveTrue().stream()
                .map(route -> new ScoredItem<>(route, scoreRoute(route, tokens)))
                .filter(item -> item.score() > 0)
                .sorted()
                .limit(MAX_ROUTES)
                .map(ScoredItem::item)
                .toList();
    }

    private List<Quest> rankQuests(List<String> tokens) {
        return questRepository.findByIsActiveTrue().stream()
                .map(quest -> new ScoredItem<>(quest, scoreQuest(quest, tokens)))
                .filter(item -> item.score() > 0)
                .sorted()
                .limit(MAX_QUESTS)
                .map(ScoredItem::item)
                .toList();
    }

    private int scorePlace(Place place, List<String> tokens) {
        String haystack = normalize(String.join(" ",
                Objects.toString(place.getName(), ""),
                Objects.toString(place.getNameTr(), ""),
                Objects.toString(place.getNameEn(), ""),
                Objects.toString(place.getCategory(), ""),
                Objects.toString(place.getDescription(), ""),
                Objects.toString(place.getAddress(), ""),
                String.join(" ", place.getKeywords() == null ? List.of() : place.getKeywords())
        ));
        int score = scoreText(haystack, tokens);
        score += Math.min(3, Math.max(0, Objects.requireNonNullElse(place.getPopularityScore(), 0) / 3));
        score += Math.min(2, Math.max(0, Objects.requireNonNullElse(place.getReviewCount(), 0) / 5));
        return score;
    }

    private int scoreRoute(Route route, List<String> tokens) {
        String haystack = normalize(String.join(" ",
                Objects.toString(route.getName(), ""),
                Objects.toString(route.getDescription(), "")
        ));
        return scoreText(haystack, tokens);
    }

    private int scoreQuest(Quest quest, List<String> tokens) {
        String haystack = normalize(String.join(" ",
                Objects.toString(quest.getTitle(), ""),
                Objects.toString(quest.getDescription(), ""),
                Objects.toString(quest.getRegion(), "")
        ));
        return scoreText(haystack, tokens);
    }

    private int scoreText(String haystack, List<String> tokens) {
        int score = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                score += 4;
            }
        }
        return score;
    }

    private void appendPlaces(StringBuilder context, List<Place> places) {
        if (places.isEmpty()) {
            return;
        }
        context.append("\nMekanlar:\n");
        for (Place place : places) {
            context.append("- ")
                    .append(firstNonBlank(place.getNameTr(), place.getName()))
                    .append(" | kategori: ").append(blankToDash(place.getCategory()))
                    .append(" | adres: ").append(blankToDash(place.getAddress()))
                    .append(" | puan: ").append(Objects.requireNonNullElse(place.getAvgRating(), 0.0))
                    .append(" | yorum: ").append(Objects.requireNonNullElse(place.getReviewCount(), 0))
                    .append(" | açıklama: ").append(shorten(place.getDescription(), 180))
                    .append("\n");
        }
    }

    private void appendRoutes(StringBuilder context, List<Route> routes) {
        if (routes.isEmpty()) {
            return;
        }
        context.append("\nRotalar:\n");
        for (Route route : routes) {
            context.append("- ")
                    .append(route.getName())
                    .append(" | süre: ").append(route.getEstimatedMinutes() == null ? "-" : route.getEstimatedMinutes() + " dk")
                    .append(" | XP: ").append(Objects.requireNonNullElse(route.getExpReward(), 0))
                    .append(" | açıklama: ").append(shorten(route.getDescription(), 180))
                    .append("\n");
        }
    }

    private void appendQuests(StringBuilder context, List<Quest> quests) {
        if (quests.isEmpty()) {
            return;
        }
        context.append("\nGörevler:\n");
        for (Quest quest : quests) {
            context.append("- ")
                    .append(quest.getTitle())
                    .append(" | bölge: ").append(blankToDash(quest.getRegion()))
                    .append(" | XP: ").append(Objects.requireNonNullElse(quest.getExpReward(), 0))
                    .append(" | açıklama: ").append(shorten(quest.getDescription(), 180))
                    .append("\n");
        }
    }

    private String normalize(String value) {
        String lower = Objects.toString(value, "").toLowerCase(Locale.forLanguageTag("tr"));
        String ascii = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii
                .replace('ı', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ö', 'o')
                .replace('ç', 'c');
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : Objects.toString(fallback, "-");
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength - 3) + "...";
    }

    private record ScoredItem<T>(T item, int score) implements Comparable<ScoredItem<T>> {
        @Override
        public int compareTo(ScoredItem<T> other) {
            return Comparator.<ScoredItem<T>>comparingInt(ScoredItem::score)
                    .reversed()
                    .compare(this, other);
        }
    }
}
