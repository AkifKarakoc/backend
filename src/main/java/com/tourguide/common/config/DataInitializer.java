package com.tourguide.common.config;

import com.tourguide.badge.Badge;
import com.tourguide.badge.BadgeRepository;
import com.tourguide.badge.UserBadge;
import com.tourguide.badge.UserBadgeRepository;
import com.tourguide.common.enums.Role;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.quest.*;
import com.tourguide.review.Review;
import com.tourguide.review.ReviewRepository;
import com.tourguide.review.ReviewStatus;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final IUserService userService;
    private final PlaceRepository placeRepository;
    private final BadgeRepository badgeRepository;
    private final QuestRepository questRepository;
    private final QuestStepRepository questStepRepository;
    private final UserQuestRepository userQuestRepository;
    private final ReviewRepository reviewRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    private static final String SUPERADMIN_EMAIL = "superadmin@tourguide.com";
    private static final String SUPERADMIN_PASSWORD = "Admin@1234";
    private static final String MOCK_PASSWORD = "User@1234";

    @Override
    public void run(ApplicationArguments args) {
        seedSuperAdmin();

        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            seedMockData();
        }
    }

    private void seedSuperAdmin() {
        if (!userService.existsByEmail(SUPERADMIN_EMAIL)) {
            User admin = User.builder()
                    .email(SUPERADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(SUPERADMIN_PASSWORD))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.SUPERADMIN)
                    .preferredLanguage("tr")
                    .build();
            userService.createUser(admin);
            log.info("SuperAdmin user created: {}", SUPERADMIN_EMAIL);
        } else {
            log.debug("SuperAdmin user already exists, skipping seed.");
        }
    }

    // ═══════════════════════════════════════════
    //  Mock Data Seeding (dev profile only)
    // ═══════════════════════════════════════════

    private void seedMockData() {
        if (placeRepository.count() > 0) {
            log.debug("Mock data already exists, skipping seed.");
            return;
        }

        log.info("Seeding mock data for dev environment...");

        String encodedPassword = passwordEncoder.encode(MOCK_PASSWORD);
        LocalDateTime now = LocalDateTime.now();

        List<User> users = seedUsers(encodedPassword, now);
        List<Place> places = seedPlaces();
        List<Badge> badges = seedBadges();
        List<Quest> quests = seedQuests(badges, places);
        seedUserQuests(users, quests, now);
        seedReviews(users, places, now);
        seedUserBadges(users, badges, now);

        log.info("Mock data seeding completed: {} users, {} places, {} badges, {} quests",
                users.size(), places.size(), badges.size(), quests.size());
    }

    private List<User> seedUsers(String encodedPassword, LocalDateTime now) {
        record UserSeed(String email, String firstName, String lastName, Role role,
                        int level, int expPoints, int daysAgo) {}

        List<UserSeed> seeds = List.of(
                new UserSeed("enes.b@example.com", "Enes", "Bilgic", Role.TOURIST, 4, 4821, 28),
                new UserSeed("aras.yilmaz@izmirguide.com", "Aras", "Yilmaz", Role.TOURIST, 12, 48250, 25),
                new UserSeed("selen.d@provider.net", "Selen", "Deniz", Role.TOURIST, 5, 12400, 20),
                new UserSeed("polat_can@gmail.com", "Can", "Polat", Role.TOURIST, 2, 1200, 15),
                new UserSeed("derya@aksoy.co", "Derya", "Aksoy", Role.TOURIST, 25, 125400, 30),
                new UserSeed("mkaya@business.com", "Murat", "Kaya", Role.TOURIST, 8, 18700, 22),
                new UserSeed("zeynep.l@mail.com", "Zeynep", "Ler", Role.TOURIST, 20, 82000, 18),
                new UserSeed("arda.k@outlook.com", "Arda", "Koc", Role.TOURIST, 3, 2500, 10),
                new UserSeed("elif.s@university.edu", "Elif", "Sahin", Role.TOURIST, 6, 15000, 7),
                new UserSeed("burak.d@tech.io", "Burak", "Demir", Role.TOURIST, 1, 200, 3),
                new UserSeed("ayse.y@mail.com", "Ayse", "Yurt", Role.TOURIST, 15, 55000, 12),
                new UserSeed("ali.v@corp.com", "Ali", "Veli", Role.MODERATOR, 10, 30000, 26),
                new UserSeed("fatma.o@editor.com", "Fatma", "Oz", Role.CONTENT_EDITOR, 7, 20000, 24),
                new UserSeed("hasan.t@example.com", "Hasan", "Tuncer", Role.TOURIST, 1, 50, 0)
        );

        List<User> users = new java.util.ArrayList<>();

        for (UserSeed s : seeds) {
            if (userService.existsByEmail(s.email())) continue;

            User user = User.builder()
                    .email(s.email())
                    .passwordHash(encodedPassword)
                    .firstName(s.firstName())
                    .lastName(s.lastName())
                    .role(s.role())
                    .level(s.level())
                    .expPoints(s.expPoints())
                    .preferredLanguage("tr")
                    .build();

            User saved = userService.createUser(user);
            users.add(saved);

            // Override created_at via JDBC (JPA @CreatedDate prevents setter override)
            LocalDateTime createdAt = now.minusDays(s.daysAgo());
            jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(createdAt), saved.getId());
        }

        log.info("  Seeded {} users", users.size());
        return users;
    }

    private List<Place> seedPlaces() {
        record PlaceSeed(String name, String category, double lat, double lng,
                         double avgRating, int reviewCount, String address) {}

        List<PlaceSeed> seeds = List.of(
                new PlaceSeed("Kemeralti Bazaar", "Historical Site", 38.4186, 27.1282, 4.8, 120, "Konak, Izmir"),
                new PlaceSeed("Kordon Promenade", "Park", 38.4381, 27.1437, 4.7, 250, "Alsancak, Konak"),
                new PlaceSeed("Deniz Restaurant", "Restaurant", 38.4350, 27.1400, 4.6, 84, "1. Kordon, Alsancak"),
                new PlaceSeed("Historical Elevator", "Historical Site", 38.4210, 27.1260, 4.9, 310, "Karatas, Izmir"),
                new PlaceSeed("Bostanli Coast", "Park", 38.4556, 27.1156, 4.7, 172, "Karsiyaka"),
                new PlaceSeed("Agora Open Air Museum", "Historical Site", 38.4195, 27.1310, 4.5, 95, "Namazgah, Konak"),
                new PlaceSeed("Clock Tower", "Historical Site", 38.4189, 27.1287, 4.4, 200, "Konak Square"),
                new PlaceSeed("Alsancak Cafe", "Restaurant", 38.4340, 27.1420, 4.3, 60, "Kibris Sehitleri, Alsancak")
        );

        List<Place> places = new java.util.ArrayList<>();
        for (PlaceSeed s : seeds) {
            Place place = Place.builder()
                    .name(s.name())
                    .nameTr(s.name())
                    .category(s.category())
                    .latitude(s.lat())
                    .longitude(s.lng())
                    .avgRating(s.avgRating())
                    .reviewCount(s.reviewCount())
                    .address(s.address())
                    .build();
            places.add(placeRepository.save(place));
        }

        log.info("  Seeded {} places", places.size());
        return places;
    }

    private List<Badge> seedBadges() {
        List<Badge> badges = List.of(
                Badge.builder().name("History Master").description("Complete all heritage quests").build(),
                Badge.builder().name("Local Foodie").description("Finish food-focused quests").build(),
                Badge.builder().name("Agora Explorer").description("Traverse the Agora route").build(),
                Badge.builder().name("Coastal Voyager").description("Complete waterfront trail series").build()
        );

        List<Badge> saved = badgeRepository.saveAll(badges);
        log.info("  Seeded {} badges", saved.size());
        return saved;
    }

    private List<Quest> seedQuests(List<Badge> badges, List<Place> places) {
        Quest q1 = questRepository.save(Quest.builder()
                .title("Find the Hidden Han").expReward(150).region("Konak District").badgeId(badges.get(0).getId()).build());
        Quest q2 = questRepository.save(Quest.builder()
                .title("Ancient Gate Explorer").expReward(200).region("Old Town").badgeId(badges.get(2).getId()).build());
        Quest q3 = questRepository.save(Quest.builder()
                .title("Clock Tower Challenge").expReward(100).region("Konak Square").build());
        Quest q4 = questRepository.save(Quest.builder()
                .title("Seaside Flavors Trail").expReward(80).region("Kordon").badgeId(badges.get(1).getId()).build());
        Quest q5 = questRepository.save(Quest.builder()
                .title("Coastal Sunset Route").expReward(250).region("Waterfront").badgeId(badges.get(3).getId()).build());

        List<Quest> quests = List.of(q1, q2, q3, q4, q5);

        // 2 steps per quest, linking to places
        addSteps(q1, places.get(0).getId(), places.get(5).getId());
        addSteps(q2, places.get(5).getId(), places.get(6).getId());
        addSteps(q3, places.get(6).getId(), places.get(3).getId());
        addSteps(q4, places.get(2).getId(), places.get(7).getId());
        addSteps(q5, places.get(1).getId(), places.get(4).getId());

        log.info("  Seeded {} quests with steps", quests.size());
        return quests;
    }

    private void addSteps(Quest quest, UUID placeId1, UUID placeId2) {
        questStepRepository.save(QuestStep.builder()
                .quest(quest).placeId(placeId1).stepOrder(1).hint("Find and photograph this location").build());
        questStepRepository.save(QuestStep.builder()
                .quest(quest).placeId(placeId2).stepOrder(2).hint("Continue to the next landmark").build());
    }

    private void seedUserQuests(List<User> users, List<Quest> quests, LocalDateTime now) {
        if (users.isEmpty() || quests.isEmpty()) return;

        int count = 0;

        // Distribute completions across last 14 days
        for (int day = 13; day >= 0; day--) {
            int completionsPerDay = 2 + (day % 2);
            for (int c = 0; c < completionsPerDay && count < users.size() * quests.size(); c++) {
                int userIdx = count % users.size();
                int questIdx = (count / users.size()) % quests.size();

                UUID userId = users.get(userIdx).getId();
                UUID questId = quests.get(questIdx).getId();
                if (userQuestRepository.existsByUserIdAndQuestId(userId, questId)) {
                    count++;
                    continue;
                }

                LocalDateTime startedAt = now.minusDays(day + 2);
                LocalDateTime completedAt = now.minusDays(day).withHour(10 + c).withMinute(30);

                UserQuest uq = UserQuest.builder()
                        .userId(userId)
                        .questId(questId)
                        .status(QuestStatus.COMPLETED)
                        .startedAt(startedAt)
                        .completedAt(completedAt)
                        .build();
                userQuestRepository.save(uq);
                count++;
            }
        }

        // Add IN_PROGRESS and ABANDONED entries
        for (int i = 0; i < 4 && i < users.size(); i++) {
            UUID userId = users.get(users.size() - 1 - i).getId();
            UUID questId = quests.get(i % quests.size()).getId();
            if (userQuestRepository.existsByUserIdAndQuestId(userId, questId)) continue;

            QuestStatus status = (i % 2 == 0) ? QuestStatus.IN_PROGRESS : QuestStatus.ABANDONED;
            UserQuest uq = UserQuest.builder()
                    .userId(userId)
                    .questId(questId)
                    .status(status)
                    .startedAt(now.minusDays(i + 1))
                    .build();
            userQuestRepository.save(uq);
            count++;
        }

        log.info("  Seeded {} user-quest records", count);
    }

    private void seedReviews(List<User> users, List<Place> places, LocalDateTime now) {
        if (users.isEmpty() || places.isEmpty()) return;

        record ReviewSeed(int userIdx, int placeIdx, int rating, ReviewStatus status, int daysAgo) {}

        List<ReviewSeed> seeds = List.of(
                new ReviewSeed(0, 0, 5, ReviewStatus.APPROVED, 6),
                new ReviewSeed(0, 1, 4, ReviewStatus.APPROVED, 5),
                new ReviewSeed(1, 0, 5, ReviewStatus.APPROVED, 4),
                new ReviewSeed(1, 3, 5, ReviewStatus.APPROVED, 3),
                new ReviewSeed(2, 1, 4, ReviewStatus.APPROVED, 3),
                new ReviewSeed(2, 4, 5, ReviewStatus.PENDING, 2),
                new ReviewSeed(3, 6, 3, ReviewStatus.APPROVED, 1),
                new ReviewSeed(4, 3, 5, ReviewStatus.APPROVED, 1),
                new ReviewSeed(5, 2, 4, ReviewStatus.PENDING, 0),
                new ReviewSeed(6, 5, 4, ReviewStatus.APPROVED, 0),
                new ReviewSeed(7, 6, 3, ReviewStatus.APPROVED, 2),
                new ReviewSeed(8, 7, 4, ReviewStatus.PENDING, 1)
        );

        int count = 0;
        for (ReviewSeed s : seeds) {
            if (s.userIdx() >= users.size() || s.placeIdx() >= places.size()) continue;

            UUID userId = users.get(s.userIdx()).getId();
            UUID placeId = places.get(s.placeIdx()).getId();
            if (reviewRepository.existsByUserIdAndPlaceId(userId, placeId)) continue;

            Review review = Review.builder()
                    .userId(userId)
                    .placeId(placeId)
                    .rating(s.rating())
                    .comment("Great place to visit!")
                    .status(s.status())
                    .build();

            Review saved = reviewRepository.save(review);
            count++;

            jdbcTemplate.update("UPDATE reviews SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(now.minusDays(s.daysAgo())), saved.getId());
        }

        log.info("  Seeded {} reviews", count);
    }

    private void seedUserBadges(List<User> users, List<Badge> badges, LocalDateTime now) {
        if (users.isEmpty() || badges.isEmpty()) return;

        record BadgeSeed(int userIdx, int badgeIdx, int daysAgo) {}

        List<BadgeSeed> seeds = List.of(
                new BadgeSeed(0, 0, 5),
                new BadgeSeed(1, 0, 10),
                new BadgeSeed(1, 1, 8),
                new BadgeSeed(4, 2, 3),
                new BadgeSeed(4, 3, 1),
                new BadgeSeed(6, 0, 2),
                new BadgeSeed(2, 1, 4),
                new BadgeSeed(5, 2, 6)
        );

        int count = 0;
        for (BadgeSeed s : seeds) {
            if (s.userIdx() >= users.size() || s.badgeIdx() >= badges.size()) continue;

            UUID userId = users.get(s.userIdx()).getId();
            UUID badgeId = badges.get(s.badgeIdx()).getId();
            if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) continue;

            UserBadge ub = UserBadge.builder()
                    .userId(userId)
                    .badgeId(badgeId)
                    .earnedAt(now.minusDays(s.daysAgo()))
                    .build();
            userBadgeRepository.save(ub);
            count++;
        }

        log.info("  Seeded {} user-badge records", count);
    }
}
