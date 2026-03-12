package com.tourguide.admin.dashboard;

import com.tourguide.admin.dashboard.dto.*;
import com.tourguide.badge.Badge;
import com.tourguide.badge.BadgeRepository;
import com.tourguide.badge.UserBadge;
import com.tourguide.badge.UserBadgeRepository;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.quest.Quest;
import com.tourguide.quest.QuestRepository;
import com.tourguide.quest.UserQuest;
import com.tourguide.quest.UserQuestRepository;
import com.tourguide.review.Review;
import com.tourguide.review.ReviewRepository;
import com.tourguide.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final ReviewRepository reviewRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    private static final List<MainDashboardResponse.DistrictFilter> DISTRICTS = List.of(
            MainDashboardResponse.DistrictFilter.builder()
                    .id("izmir").label("Izmir Center").center(new double[]{27.1428, 38.4237}).zoom(11.2).build(),
            MainDashboardResponse.DistrictFilter.builder()
                    .id("konak").label("Konak").center(new double[]{27.1286, 38.4189}).zoom(12.2).build(),
            MainDashboardResponse.DistrictFilter.builder()
                    .id("alsancak").label("Alsancak").center(new double[]{27.1438, 38.4381}).zoom(13.1).build(),
            MainDashboardResponse.DistrictFilter.builder()
                    .id("karsiyaka").label("Karsiyaka").center(new double[]{27.1156, 38.4556}).zoom(12.6).build()
    );

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    // ═══════════════════════════════════════════
    //  GET /admin/dashboard/main
    // ═══════════════════════════════════════════

    public MainDashboardResponse getMainDashboard() {
        return MainDashboardResponse.builder()
                .metrics(buildMainMetrics())
                .districts(DISTRICTS)
                .mapPoints(buildMapPoints())
                .questTrend(buildQuestTrend())
                .recentActivity(buildRecentActivity())
                .build();
    }

    private List<MainDashboardResponse.MetricCard> buildMainMetrics() {
        long totalUsers = userRepository.countByIsActiveTrue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime twoWeeksAgo = now.minusWeeks(2);

        long newThisWeek = userRepository.countActiveCreatedSince(oneWeekAgo);
        long newLastWeek = userRepository.countActiveCreatedSince(twoWeeksAgo) - newThisWeek;
        String userTrendDetail = buildTrendDetail(newThisWeek, newLastWeek, "from last week");
        String userTrend = newThisWeek >= newLastWeek ? "positive" : "warning";

        long activeQuests = questRepository.countByIsActiveTrue();
        long listedPlaces = placeRepository.countByIsActiveTrue();
        long totalXp = userRepository.sumExpPoints();

        return List.of(
                MainDashboardResponse.MetricCard.builder()
                        .id("total-users").label("Total Users")
                        .value(NUMBER_FORMAT.format(totalUsers))
                        .detail(userTrendDetail).trend(userTrend).icon("users").build(),
                MainDashboardResponse.MetricCard.builder()
                        .id("active-quests").label("Active Quests")
                        .value(String.valueOf(activeQuests))
                        .detail("Running live right now").trend("neutral").icon("quests").build(),
                MainDashboardResponse.MetricCard.builder()
                        .id("listed-places").label("Places Listed")
                        .value(NUMBER_FORMAT.format(listedPlaces))
                        .detail("Across Izmir province").trend("neutral").icon("places").build(),
                MainDashboardResponse.MetricCard.builder()
                        .id("earned-xp").label("Total XP Earned")
                        .value(formatLargeNumber(totalXp))
                        .detail("High engagement today").trend("neutral").icon("xp").build()
        );
    }

    private List<MainDashboardResponse.MapPointDto> buildMapPoints() {
        return placeRepository.findByIsActiveTrue().stream()
                .map(place -> MainDashboardResponse.MapPointDto.builder()
                        .id(place.getId().toString())
                        .label(place.getName())
                        .category(deriveMapCategory(place))
                        .coordinates(new double[]{place.getLongitude(), place.getLatitude()})
                        .build())
                .collect(Collectors.toList());
    }

    private List<MainDashboardResponse.TrendDatum> buildQuestTrend() {
        List<MainDashboardResponse.TrendDatum> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            long completed = userQuestRepository.countCompletedBetween(dayStart, dayEnd);
            long active = userQuestRepository.countDistinctActiveUsersBetween(dayStart, dayEnd);

            trend.add(MainDashboardResponse.TrendDatum.builder()
                    .label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US))
                    .completedQuests(completed)
                    .activeUsers(active)
                    .build());
        }
        return trend;
    }

    private List<MainDashboardResponse.ActivityItem> buildRecentActivity() {
        record ActivityCandidate(String id, String actor, String action, String target,
                                 LocalDateTime timestamp, String tone) {}

        List<ActivityCandidate> candidates = new ArrayList<>();

        // Recent quest completions
        List<UserQuest> recentQuests = userQuestRepository.findRecentCompleted(PageRequest.of(0, 4));
        for (UserQuest uq : recentQuests) {
            String actor = resolveUserName(uq.getUserId());
            String questTitle = resolveQuestTitle(uq.getQuestId());
            candidates.add(new ActivityCandidate(
                    uq.getId().toString(), actor, "completed", questTitle,
                    uq.getCompletedAt(), "blue"));
        }

        // Recent reviews
        List<Review> recentReviews = reviewRepository.findRecentReviews(PageRequest.of(0, 4));
        for (Review r : recentReviews) {
            String actor = resolveUserName(r.getUserId());
            String placeName = resolvePlaceName(r.getPlaceId());
            candidates.add(new ActivityCandidate(
                    r.getId().toString(), actor, "reviewed", placeName,
                    r.getCreatedAt(), "green"));
        }

        // Recent badge earnings
        List<UserBadge> recentBadges = userBadgeRepository.findRecentEarned(PageRequest.of(0, 4));
        for (UserBadge ub : recentBadges) {
            String actor = resolveUserName(ub.getUserId());
            String badgeName = resolveBadgeName(ub.getBadgeId());
            candidates.add(new ActivityCandidate(
                    ub.getId().toString(), actor, "earned badge", badgeName,
                    ub.getEarnedAt(), "amber"));
        }

        // Sort by timestamp desc and take top 4
        return candidates.stream()
                .sorted(Comparator.comparing(ActivityCandidate::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(c -> MainDashboardResponse.ActivityItem.builder()
                        .id(c.id()).actor(c.actor()).action(c.action()).target(c.target())
                        .time(formatRelativeTime(c.timestamp())).tone(c.tone())
                        .build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════
    //  GET /admin/dashboard/places
    // ═══════════════════════════════════════════

    public PlacesOverviewResponse getPlacesOverview() {
        long totalPlaces = placeRepository.count();
        long activePlaces = placeRepository.countByIsActiveTrue();
        double avgRating = placeRepository.avgRatingOfActive();
        long reviewVolume = placeRepository.sumReviewCountOfActive();
        long drafts = totalPlaces - activePlaces;

        List<PlacesOverviewResponse.MetricCard> metrics = List.of(
                PlacesOverviewResponse.MetricCard.builder()
                        .id("places-total").label("Listed Places")
                        .value(NUMBER_FORMAT.format(totalPlaces))
                        .detail("Across active districts").trend("neutral").icon("places").build(),
                PlacesOverviewResponse.MetricCard.builder()
                        .id("places-active").label("Active Listings")
                        .value(NUMBER_FORMAT.format(activePlaces))
                        .detail(drafts + " drafts in queue").trend("positive").icon("places").build(),
                PlacesOverviewResponse.MetricCard.builder()
                        .id("places-rating").label("Avg. Rating")
                        .value(String.format("%.1f", avgRating))
                        .detail("Visitor sentiment snapshot").trend("positive").icon("xp").build(),
                PlacesOverviewResponse.MetricCard.builder()
                        .id("places-reviews").label("Review Volume")
                        .value(NUMBER_FORMAT.format(reviewVolume))
                        .detail("Total reviews indexed").trend("neutral").icon("users").build()
        );

        List<PlacesOverviewResponse.TopPlace> topPlaces = placeRepository.findByIsActiveTrue().stream()
                .sorted(Comparator.comparingInt(Place::getReviewCount).reversed())
                .limit(4)
                .map(p -> PlacesOverviewResponse.TopPlace.builder()
                        .id(p.getId().toString())
                        .name(p.getName())
                        .district(p.getAddress() != null ? p.getAddress() : "Izmir")
                        .reviewCount(p.getReviewCount())
                        .rating(p.getAvgRating())
                        .build())
                .collect(Collectors.toList());

        return PlacesOverviewResponse.builder()
                .metrics(metrics)
                .topPlaces(topPlaces)
                .build();
    }

    // ═══════════════════════════════════════════
    //  GET /admin/dashboard/quests
    // ═══════════════════════════════════════════

    public QuestOverviewResponse getQuestOverview(int page, int pageSize) {
        return QuestOverviewResponse.builder()
                .metrics(buildQuestMetrics())
                .xpAccrualChart(buildXpAccrualChart())
                .levelDistribution(buildLevelDistribution())
                .questPerformance(buildQuestPerformance(page, pageSize))
                .build();
    }

    private List<QuestOverviewResponse.MetricCard> buildQuestMetrics() {
        long totalQuests = questRepository.count();
        long totalCompletions = userQuestRepository.countAllCompleted();
        long rewardRedemptions = userBadgeRepository.count();

        return List.of(
                QuestOverviewResponse.MetricCard.builder()
                        .id("total-quests").label("Total Quests")
                        .value(String.valueOf(totalQuests))
                        .detail("All time").tone("blue").build(),
                QuestOverviewResponse.MetricCard.builder()
                        .id("active-users").label("Active Users")
                        .value(NUMBER_FORMAT.format(totalCompletions))
                        .detail("Sum of completions").tone("green").build(),
                QuestOverviewResponse.MetricCard.builder()
                        .id("reward-redemptions").label("Reward Redemptions")
                        .value(NUMBER_FORMAT.format(rewardRedemptions))
                        .detail("Badges earned").tone("blue").build()
        );
    }

    private QuestOverviewResponse.XpAccrualChart buildXpAccrualChart() {
        LocalDate now = LocalDate.now();
        LocalDate thisMonthStart = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate nextMonthStart = thisMonthStart.plusMonths(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);

        List<Object[]> thisMonthRaw = userQuestRepository.sumXpByWeek(
                thisMonthStart.atStartOfDay(), nextMonthStart.atStartOfDay());
        List<Object[]> lastMonthRaw = userQuestRepository.sumXpByWeek(
                lastMonthStart.atStartOfDay(), thisMonthStart.atStartOfDay());

        DateTimeFormatter weekFormat = DateTimeFormatter.ofPattern("MMM dd", Locale.US);

        // Build weekly labels from this month's data or fall back to week starts
        List<String> labels = new ArrayList<>();
        List<Long> thisMonthValues = new ArrayList<>();
        List<Long> lastMonthValues = new ArrayList<>();

        // Generate week start dates for current month
        List<LocalDate> thisMonthWeeks = getWeekStarts(thisMonthStart, nextMonthStart);
        for (LocalDate ws : thisMonthWeeks) {
            labels.add(ws.format(weekFormat));
        }

        // Map raw data to week positions
        Map<LocalDate, Long> thisMonthMap = rawToWeekMap(thisMonthRaw);
        Map<LocalDate, Long> lastMonthMap = rawToWeekMap(lastMonthRaw);

        List<LocalDate> lastMonthWeeks = getWeekStarts(lastMonthStart, thisMonthStart);

        for (LocalDate ws : thisMonthWeeks) {
            thisMonthValues.add(thisMonthMap.getOrDefault(ws, 0L));
        }
        for (int i = 0; i < thisMonthWeeks.size(); i++) {
            if (i < lastMonthWeeks.size()) {
                lastMonthValues.add(lastMonthMap.getOrDefault(lastMonthWeeks.get(i), 0L));
            } else {
                lastMonthValues.add(0L);
            }
        }

        return QuestOverviewResponse.XpAccrualChart.builder()
                .labels(labels)
                .thisMonth(thisMonthValues)
                .lastMonth(lastMonthValues)
                .build();
    }

    private List<QuestOverviewResponse.LevelSegment> buildLevelDistribution() {
        List<Object[]> levelCounts = userRepository.countGroupedByLevel();

        long novices = 0, explorers = 0, legends = 0;
        for (Object[] row : levelCounts) {
            int level = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (level <= 5) novices += count;
            else if (level <= 14) explorers += count;
            else legends += count;
        }

        return List.of(
                QuestOverviewResponse.LevelSegment.builder().name("Legends").value(legends).build(),
                QuestOverviewResponse.LevelSegment.builder().name("Explorers").value(explorers).build(),
                QuestOverviewResponse.LevelSegment.builder().name("Novices").value(novices).build()
        );
    }

    private QuestOverviewResponse.QuestPerformancePage buildQuestPerformance(int page, int pageSize) {
        Page<Quest> questPage = questRepository.findAll(PageRequest.of(page, pageSize));

        Map<UUID, Long> completionsMap = toUuidLongMap(userQuestRepository.countCompletionsGroupedByQuest());
        Map<UUID, Long> abandonedMap = toUuidLongMap(userQuestRepository.countAbandonedGroupedByQuest());
        Map<UUID, Long> startedMap = toUuidLongMap(userQuestRepository.countStartedGroupedByQuest());

        List<QuestOverviewResponse.QuestPerformanceRow> rows = questPage.getContent().stream()
                .map(quest -> {
                    UUID qid = quest.getId();
                    long completions = completionsMap.getOrDefault(qid, 0L);
                    long abandoned = abandonedMap.getOrDefault(qid, 0L);
                    long started = startedMap.getOrDefault(qid, 0L);

                    double dropOff = started > 0 ? (double) abandoned / started * 100 : 0;

                    return QuestOverviewResponse.QuestPerformanceRow.builder()
                            .id(qid.toString())
                            .name(quest.getTitle())
                            .location(quest.getRegion() != null ? quest.getRegion() : "Izmir")
                            .tier(deriveTier(quest.getExpReward()))
                            .completions(completions)
                            .rating(0.0) // placeholder — no per-quest rating in schema
                            .dropOffRate(String.format("%.1f%%", dropOff))
                            .dropOffTone(dropOff > 20 ? "danger" : "good")
                            .build();
                })
                .collect(Collectors.toList());

        return QuestOverviewResponse.QuestPerformancePage.builder()
                .rows(rows)
                .page(page)
                .pageSize(pageSize)
                .totalElements(questPage.getTotalElements())
                .totalPages(questPage.getTotalPages())
                .build();
    }

    // ═══════════════════════════════════════════
    //  GET /admin/dashboard/users
    // ═══════════════════════════════════════════

    public UsersOverviewResponse getUsersOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        long newToday = userRepository.countActiveCreatedSince(today.atStartOfDay());
        long newThisWeek = userRepository.countActiveCreatedSince(now.minusWeeks(1));
        long newLastWeek = userRepository.countActiveCreatedSince(now.minusWeeks(2)) - newThisWeek;
        double weeklyGrowth = newLastWeek > 0 ? (double) (newThisWeek - newLastWeek) / newLastWeek * 100 : 0;
        double avgXp = userRepository.avgExpPoints();
        long globalRankers = userRepository.countByMinLevel(20);

        List<UsersOverviewResponse.MetricCard> metrics = List.of(
                UsersOverviewResponse.MetricCard.builder()
                        .id("new-today").label("New Today")
                        .value("+" + NUMBER_FORMAT.format(newToday))
                        .detail(newToday > 0 ? "Active registrations" : "No new users yet")
                        .trend("positive").icon("users").build(),
                UsersOverviewResponse.MetricCard.builder()
                        .id("weekly-growth").label("Weekly Growth")
                        .value(String.format("%.1f%%", weeklyGrowth))
                        .detail(buildTrendDetail(newThisWeek, newLastWeek, ""))
                        .trend(weeklyGrowth >= 0 ? "positive" : "warning").icon("users").build(),
                UsersOverviewResponse.MetricCard.builder()
                        .id("avg-xp").label("Avg. XP / User")
                        .value(NUMBER_FORMAT.format((long) avgXp))
                        .detail("Stable").trend("neutral").icon("xp").build(),
                UsersOverviewResponse.MetricCard.builder()
                        .id("global-rankers").label("Global Rankers")
                        .value(NUMBER_FORMAT.format(globalRankers))
                        .detail("Lvl 20+").trend("neutral").icon("users").build()
        );

        List<UsersOverviewResponse.TopUser> topUsers = userRepository
                .findByIsActiveTrueOrderByExpPointsDesc(PageRequest.of(0, 4))
                .stream()
                .map(u -> UsersOverviewResponse.TopUser.builder()
                        .id(u.getId().toString())
                        .name(u.getFirstName() + " " + u.getLastName())
                        .totalXp(u.getExpPoints())
                        .level(u.getLevel())
                        .role(deriveUserRole(u.getLevel()))
                        .joinedDate(u.getCreatedAt().format(DATE_FORMAT))
                        .build())
                .collect(Collectors.toList());

        return UsersOverviewResponse.builder()
                .metrics(metrics)
                .topUsers(topUsers)
                .build();
    }

    // ═══════════════════════════════════════════
    //  Helper methods
    // ═══════════════════════════════════════════

    private String formatLargeNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        }
        return NUMBER_FORMAT.format(num);
    }

    private String buildTrendDetail(long current, long previous, String suffix) {
        if (previous == 0) return "New " + suffix;
        long pct = Math.round((double) (current - previous) / previous * 100);
        String sign = pct >= 0 ? "+" : "";
        return sign + pct + "% " + suffix;
    }

    private String deriveMapCategory(Place place) {
        if (place.getReviewCount() != null && place.getReviewCount() > 100) {
            return "high-demand";
        }
        String cat = place.getCategory() != null ? place.getCategory().toLowerCase() : "";
        if (cat.contains("restaurant") || cat.contains("cafe") || cat.contains("hotel")) {
            return "hub";
        }
        return "landmark";
    }

    private String deriveTier(int expReward) {
        if (expReward >= 200) return "Legend";
        if (expReward >= 100) return "Expert";
        return "Beginner";
    }

    private String deriveUserRole(int level) {
        if (level >= 15) return "Ambassador";
        if (level >= 5) return "Local Guide";
        return "Explorer";
    }

    private String formatRelativeTime(LocalDateTime timestamp) {
        if (timestamp == null) return "Unknown";
        Duration d = Duration.between(timestamp, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        long hours = d.toHours();
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = d.toDays();
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    private String resolveUserName(UUID userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName().charAt(0) + ".")
                .orElse("Unknown User");
    }

    private String resolveQuestTitle(UUID questId) {
        return questRepository.findByIdAndIsActiveTrue(questId)
                .map(Quest::getTitle)
                .orElse("Unknown Quest");
    }

    private String resolvePlaceName(UUID placeId) {
        return placeRepository.findByIdAndIsActiveTrue(placeId)
                .map(Place::getName)
                .orElse("Unknown Place");
    }

    private String resolveBadgeName(UUID badgeId) {
        return badgeRepository.findByIdAndIsActiveTrue(badgeId)
                .map(Badge::getName)
                .orElse("Unknown Badge");
    }

    private List<LocalDate> getWeekStarts(LocalDate from, LocalDate to) {
        List<LocalDate> weeks = new ArrayList<>();
        LocalDate ws = from;
        while (ws.isBefore(to)) {
            weeks.add(ws);
            ws = ws.plusWeeks(1);
        }
        return weeks;
    }

    private Map<LocalDate, Long> rawToWeekMap(List<Object[]> raw) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (Object[] row : raw) {
            LocalDate weekStart;
            if (row[0] instanceof Timestamp ts) {
                weekStart = ts.toLocalDateTime().toLocalDate();
            } else if (row[0] instanceof LocalDateTime ldt) {
                weekStart = ldt.toLocalDate();
            } else {
                continue;
            }
            long xp = ((Number) row[1]).longValue();
            map.put(weekStart, xp);
        }
        return map;
    }

    private Map<UUID, Long> toUuidLongMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            UUID key = (UUID) row[0];
            long value = ((Number) row[1]).longValue();
            map.put(key, value);
        }
        return map;
    }
}
