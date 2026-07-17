package com.merg.quoteapp.repository;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import android.util.Log;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.AchievementFeedbackEvent;
import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.model.XpRewards;
import com.merg.quoteapp.utils.AchievementFeedbackCenter;
import com.merg.quoteapp.utils.QuoteVisibilityUtils;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementEngineRepository {

    private static final String TAG = "AchievementEngineRepo";

    public interface EngineCallback {
        void onComplete();

        void onError(String message);
    }

    private static final String USERS_STATS_COLLECTION = "userStats";
    private static final String ACHIEVEMENTS_COLLECTION = "achievements";
    private static final String USER_ACHIEVEMENTS_COLLECTION = "userAchievements";
    private static final String LEVELS_COLLECTION = "levels";
    private static final String QUOTES_COLLECTION = "quotes";
    private static final String LIKES_COLLECTION = "likes";
    private static final String APP_CONFIG_COLLECTION = "appConfig";
    private static final String XP_REWARDS_DOCUMENT = "xpRewards";
    private static volatile AchievementEngineRepository instance;

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final AchievementFeedbackCenter feedbackCenter;

    private AchievementEngineRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        feedbackCenter = AchievementFeedbackCenter.getInstance();
    }

    /**
     * Returns the shared achievement engine repository.
     *
     * @return singleton repository instance
     */
    public static AchievementEngineRepository getInstance() {
        if (instance == null) {
            synchronized (AchievementEngineRepository.class) {
                if (instance == null) {
                    instance = new AchievementEngineRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Handles XP, stats, achievement and level updates after a quote is created.
     *
     * @param quote created quote
     */
    public void onQuoteCreated(Quote quote) {
        if (quote == null || isBlank(quote.getUserId())) {
            return;
        }
        loadXpRewards(rewards -> applyQuoteCreated(quote, rewards.valueFor(
                XpRewards.EVENT_CREATE_QUOTE)));
    }

    /**
     * Handles XP, stats, achievement and level updates after a quote receives a like.
     *
     * @param quoteId quote id that received the like
     * @param likerUserId user id that liked the quote
     */
    public void onQuoteLiked(String quoteId, String likerUserId) {
        if (isBlank(quoteId)) {
            return;
        }
        firestore.collection(QUOTES_COLLECTION)
                .document(quoteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && QuoteVisibilityUtils.isVisible(document)) {
                        handleLikedQuoteDocument(document, likerUserId);
                        return;
                    }
                    firestore.collection(QUOTES_COLLECTION)
                            .whereEqualTo("quoteId", quoteId)
                            .whereEqualTo("isHidden", false)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (!snapshot.isEmpty()
                                        && QuoteVisibilityUtils.isVisible(snapshot.getDocuments().get(0))) {
                                    handleLikedQuoteDocument(snapshot.getDocuments().get(0), likerUserId);
                                }
                            })
                            .addOnFailureListener(error ->
                                    Log.e(TAG, "Quote lookup by quoteId failed", error));
                })
                .addOnFailureListener(error -> Log.e(TAG, "Quote lookup for like failed", error));
    }

    public void onQuoteUnliked(String quoteId) {
        if (isBlank(quoteId)) {
            return;
        }
        firestore.collection(QUOTES_COLLECTION)
                .document(quoteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && QuoteVisibilityUtils.isVisible(document)) {
                        Quote quote = document.toObject(Quote.class);
                        if (quote != null && !isBlank(quote.getUserId())) {
                            recalculateLikeStatsAndAchievements(quote.getUserId(), 0);
                        }
                    }
                })
                .addOnFailureListener(error -> Log.e(TAG, "Quote lookup for unlike failed", error));
    }

    private void handleLikedQuoteDocument(DocumentSnapshot document, String likerUserId) {
        Quote quote = document.toObject(Quote.class);
        if (quote == null || isBlank(quote.getUserId())
                || quote.getUserId().equals(likerUserId)) {
            return;
        }
        if (isBlank(quote.getQuoteId())) {
            quote.setQuoteId(document.getId());
        }
        loadXpRewards(rewards -> applyLikeReceived(
                quote, rewards.valueFor(XpRewards.EVENT_RECEIVE_LIKE)));
    }

    /**
     * Future-ready entry point for valid moderation reports.
     *
     * @param userId user id that submitted a valid report
     */
    public void onValidReport(String userId) {
        if (isBlank(userId)) {
            return;
        }
        loadXpRewards(rewards -> updateStatsForEvent(userId,
                rewards.valueFor(XpRewards.EVENT_VALID_REPORT),
                stats -> stats.setValidReports(stats.getValidReports() + 1)));
    }

    /**
     * Re-evaluates active achievements using the currently persisted user stats.
     * Existing unlocked achievement documents are respected, so XP is granted only
     * for newly unlocked achievements.
     *
     * @param userId user id whose achievements will be reconciled
     * @param callback operation callback
     */
    public void evaluateAchievementsForUser(String userId, EngineCallback callback) {
        if (isBlank(userId)) {
            if (callback != null) {
                callback.onError("KullanÄ±cÄ± bilgisi bulunamadÄ±.");
            }
            return;
        }
        loadStats(userId, stats -> evaluateAchievements(stats, callback));
    }

    private void applyQuoteCreated(Quote quote, long xpAmount) {
        updateStatsForEvent(quote.getUserId(), xpAmount, stats -> {
            stats.setTotalQuotes(stats.getTotalQuotes() + 1);
            if ("Film".equals(quote.getType())) {
                stats.setTotalMovieQuotes(stats.getTotalMovieQuotes() + 1);
            } else if ("Dizi".equals(quote.getType())) {
                stats.setTotalSeriesQuotes(stats.getTotalSeriesQuotes() + 1);
            } else if ("Kitap".equals(quote.getType())) {
                stats.setTotalBookQuotes(stats.getTotalBookQuotes() + 1);
            }
        });
    }

    private void applyLikeReceived(Quote quote, long xpAmount) {
        recalculateLikeStatsAndAchievements(quote.getUserId(), xpAmount);
    }

    private void recalculateLikeStatsAndAchievements(String userId, long xpAmount) {
        firestore.collection(QUOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isHidden", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> quoteDocuments = snapshot.getDocuments();
                    if (quoteDocuments.isEmpty()) {
                        updateStatsForEvent(userId, xpAmount, stats -> {
                            stats.setTotalLikesReceived(0);
                            stats.setMaxSingleQuoteLikes(0);
                        });
                        return;
                    }
                    LikeTotals totals = new LikeTotals();
                    countOwnerQuoteLikes(userId, xpAmount, quoteDocuments, 0, totals);
                })
                .addOnFailureListener(error -> Log.e(TAG, "Owner quote lookup for like stats failed", error));
    }

    private void countOwnerQuoteLikes(String userId, long xpAmount,
                                      List<DocumentSnapshot> quoteDocuments,
                                      int index, LikeTotals totals) {
        if (index >= quoteDocuments.size()) {
            updateStatsForEvent(userId, xpAmount, stats -> {
                stats.setTotalLikesReceived(Math.max(0, totals.totalLikes));
                stats.setMaxSingleQuoteLikes(Math.max(0, totals.maxSingleQuoteLikes));
            });
            return;
        }
        DocumentSnapshot quoteDocument = quoteDocuments.get(index);
        if (QuoteVisibilityUtils.isHidden(quoteDocument)) {
            countOwnerQuoteLikes(userId, xpAmount, quoteDocuments, index + 1, totals);
            return;
        }
        String quoteId = quoteDocument.getString("quoteId");
        if (isBlank(quoteId)) {
            quoteId = quoteDocument.getId();
        }
        firestore.collection(LIKES_COLLECTION)
                .whereEqualTo("quoteId", quoteId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(count -> {
                    long quoteLikes = Math.max(0, count.getCount());
                    totals.totalLikes += quoteLikes;
                    totals.maxSingleQuoteLikes = Math.max(totals.maxSingleQuoteLikes, quoteLikes);
                    countOwnerQuoteLikes(userId, xpAmount, quoteDocuments, index + 1, totals);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Like count for owner quote failed", error);
                    countOwnerQuoteLikes(userId, xpAmount, quoteDocuments, index + 1, totals);
                });
    }

    private void updateStatsForEvent(String userId, long xpAmount, StatsMutator mutator) {
        loadStats(userId, stats -> {
            int previousLevel = safeLevel(stats.getLevel());
            long safeXpAmount = Math.max(0, xpAmount);
            mutator.mutate(stats);
            stats.setTotalXp(Math.max(0, stats.getTotalXp()) + safeXpAmount);
            updateLevelAndSave(stats, level -> {
                if (shouldNotify(userId) && safeXpAmount > 0) {
                    feedbackCenter.publish(AchievementFeedbackEvent.xpGained(safeXpAmount));
                }
                if (shouldNotify(userId) && level != null
                        && safeLevel(level.getLevel()) > previousLevel) {
                    feedbackCenter.publish(AchievementFeedbackEvent.levelUp(level));
                }
                evaluateAchievements(stats);
            });
        });
    }

    private void evaluateAchievements(UserStats stats) {
        evaluateAchievements(stats, null);
    }

    private void evaluateAchievements(UserStats stats, EngineCallback callback) {
        firestore.collection(ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snapshot -> unlockMetAchievements(
                        stats, snapshot.toObjects(Achievement.class), callback))
                .addOnFailureListener(error -> {
                    if (callback != null) {
                        callback.onError("Başarımlar değerlendirilemedi. Lütfen tekrar deneyin.");
                    }
                });
    }

    private void unlockMetAchievements(UserStats stats, List<Achievement> achievements,
                                       EngineCallback callback) {
        if (achievements == null || achievements.isEmpty()) {
            recalculateAndPersistLevel(stats.getUserId(), callback);
            return;
        }

        final int[] remaining = {achievements.size()};
        List<Achievement> unlockedAchievements = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (!isRequirementMet(achievement, stats)) {
                completeUnlockCheck(stats.getUserId(), remaining, unlockedAchievements,
                        safeLevel(stats.getLevel()), callback);
                continue;
            }
            unlockAchievementIfNeeded(stats, achievement, unlocked -> {
                if (unlocked) {
                    unlockedAchievements.add(achievement);
                    if (shouldNotify(stats.getUserId())) {
                        feedbackCenter.publish(
                                AchievementFeedbackEvent.achievementUnlocked(achievement));
                    }
                }
                completeUnlockCheck(stats.getUserId(), remaining, unlockedAchievements,
                        safeLevel(stats.getLevel()), callback);
            });
        }
    }

    private void completeUnlockCheck(String userId, int[] remaining,
                                     List<Achievement> unlockedAchievements,
                                     int levelBeforeAchievementXp,
                                     EngineCallback callback) {
        remaining[0]--;
        if (remaining[0] == 0) {
            recalculateAndPersistLevel(userId, levelBeforeAchievementXp, callback);
        }
    }

    private void unlockAchievementIfNeeded(UserStats stats, Achievement achievement,
                                           UnlockCallback callback) {
        String achievementId = achievement.getAchievementId();
        if (isBlank(achievementId)) {
            callback.onComplete(false);
            return;
        }
        String userAchievementId = stats.getUserId() + "_" + achievementId;
        firestore.collection(USER_ACHIEVEMENTS_COLLECTION)
                .document(userAchievementId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onComplete(false);
                    } else {
                        writeUserAchievement(stats, achievement, userAchievementId, callback);
                    }
                })
                .addOnFailureListener(error -> callback.onComplete(false));
    }

    private void writeUserAchievement(UserStats stats, Achievement achievement,
                                      String userAchievementId, UnlockCallback callback) {
        DocumentReference userAchievementRef = firestore.collection(USER_ACHIEVEMENTS_COLLECTION)
                .document(userAchievementId);
        DocumentReference statsRef = firestore.collection(USERS_STATS_COLLECTION)
                .document(stats.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("userAchievementId", userAchievementId);
        data.put("userId", stats.getUserId());
        data.put("achievementId", achievement.getAchievementId());
        data.put("achievementGroup", achievement.getAchievementGroup());
        data.put("tier", achievement.getTier());
        data.put("unlockedAt", FieldValue.serverTimestamp());
        data.put("progressAtUnlock", metricValue(achievement.getMetric(), stats));
        data.put("xpRewardGranted", true);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot userAchievementSnapshot = transaction.get(userAchievementRef);
                    DocumentSnapshot statsSnapshot = transaction.get(statsRef);
                    if (userAchievementSnapshot.exists()) {
                        return false;
                    }

                    UserStats latestStats = statsFromDocument(stats.getUserId(), statsSnapshot);
                    latestStats.setTotalXp(Math.max(0, latestStats.getTotalXp())
                            + Math.max(0, achievement.getXpReward()));
                    latestStats.setUnlockedAchievementCount(
                            Math.max(0, latestStats.getUnlockedAchievementCount()) + 1);

                    transaction.set(userAchievementRef, data);
                    transaction.set(statsRef, statsMap(latestStats), SetOptions.merge());
                    return true;
                })
                .addOnSuccessListener(unlocked -> {
                    UserStatsRepository.invalidateUserStatsCache(stats.getUserId());
                    callback.onComplete(Boolean.TRUE.equals(unlocked));
                })
                .addOnFailureListener(error -> {
                    logAchievementWriteFailure(
                            "create",
                            userAchievementRef.getPath(),
                            achievement.getAchievementId(),
                            stats.getUserId(),
                            data,
                            error);
                    callback.onComplete(false);
                });
    }

    private void recalculateAndPersistLevel(String userId) {
        recalculateAndPersistLevel(userId, null);
    }

    private void recalculateAndPersistLevel(String userId, EngineCallback callback) {
        loadStats(userId, stats -> updateLevelAndSave(stats, level -> {
            if (callback != null) {
                callback.onComplete();
            }
        }));
    }

    private void recalculateAndPersistLevel(String userId, int previousLevel) {
        recalculateAndPersistLevel(userId, previousLevel, null);
    }

    private void recalculateAndPersistLevel(String userId, int previousLevel,
                                            EngineCallback callback) {
        loadStats(userId, stats -> updateLevelAndSave(stats, level -> {
            if (shouldNotify(userId) && level != null
                    && safeLevel(level.getLevel()) > safeLevel(previousLevel)) {
                feedbackCenter.publish(AchievementFeedbackEvent.levelUp(level));
            }
            if (callback != null) {
                callback.onComplete();
            }
        }));
    }

    private void updateLevelAndSave(UserStats stats, LevelSaveCallback afterSave) {
        calculateLevel(stats.getTotalXp(), level -> {
            stats.setLevel(level == null ? 1 : safeLevel(level.getLevel()));
            Map<String, Object> data = statsMap(stats);
            firestore.collection(USERS_STATS_COLLECTION)
                    .document(stats.getUserId())
                    .set(data, SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            logAchievementWriteFailure(
                                    "set",
                                    USERS_STATS_COLLECTION + "/" + stats.getUserId(),
                                    null,
                                    stats.getUserId(),
                                    data,
                                    task.getException());
                        }
                        UserStatsRepository.invalidateUserStatsCache(stats.getUserId());
                        if (afterSave != null) {
                            afterSave.onSaved(level);
                        }
                    });
        });
    }

    private void calculateLevel(long totalXp, LevelResultCallback callback) {
        firestore.collection(LEVELS_COLLECTION)
                .whereLessThanOrEqualTo("requiredTotalXp", Math.max(0, totalXp))
                .orderBy("requiredTotalXp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onResult(defaultLevel());
                    } else {
                        Level level = snapshot.getDocuments().get(0).toObject(Level.class);
                        callback.onResult(level == null ? defaultLevel() : level);
                    }
                })
                .addOnFailureListener(error -> callback.onResult(defaultLevel()));
    }

    private void loadStats(String userId, StatsCallback callback) {
        if (isBlank(userId)) {
            return;
        }
        firestore.collection(USERS_STATS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> callback.onStats(statsFromDocument(userId, document)))
                .addOnFailureListener(error -> callback.onStats(defaultStats(userId)));
    }

    private UserStats statsFromDocument(String userId, DocumentSnapshot document) {
        if (document != null && document.exists()) {
            UserStats stats = document.toObject(UserStats.class);
            if (stats != null) {
                if (isBlank(stats.getUserId())) {
                    stats.setUserId(userId);
                }
                if (stats.getLevel() <= 0) {
                    stats.setLevel(1);
                }
                return stats;
            }
        }
        return defaultStats(userId);
    }

    private UserStats defaultStats(String userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLevel(1);
        return stats;
    }

    private Map<String, Object> statsMap(UserStats stats) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", stats.getUserId());
        data.put("totalXp", Math.max(0, stats.getTotalXp()));
        data.put("level", stats.getLevel() <= 0 ? 1 : stats.getLevel());
        data.put("totalQuotes", stats.getTotalQuotes());
        data.put("totalLikesReceived", stats.getTotalLikesReceived());
        data.put("maxSingleQuoteLikes", stats.getMaxSingleQuoteLikes());
        data.put("totalMovieQuotes", stats.getTotalMovieQuotes());
        data.put("totalSeriesQuotes", stats.getTotalSeriesQuotes());
        data.put("totalBookQuotes", stats.getTotalBookQuotes());
        data.put("validReports", stats.getValidReports());
        data.put("invalidReports", stats.getInvalidReports());
        data.put("unlockedAchievementCount", stats.getUnlockedAchievementCount());
        data.put("lastUpdatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private void logAchievementWriteFailure(String operation, String path, String achievementId,
                                            String userId, Map<String, Object> payload,
                                            Exception error) {
        String code = error instanceof FirebaseFirestoreException
                ? ((FirebaseFirestoreException) error).getCode().name()
                : "n/a";
        Log.e(TAG,
                "Achievement write failed. operation=" + operation
                        + ", path=" + path
                        + ", achievementId=" + achievementId
                        + ", userId=" + userId
                        + ", payloadKeys=" + (payload == null ? "[]" : payload.keySet())
                        + ", firebaseCode=" + code,
                error);
    }

    private void loadXpRewards(XpRewardsCallback callback) {
        firestore.collection(APP_CONFIG_COLLECTION)
                .document(XP_REWARDS_DOCUMENT)
                .get()
                .addOnSuccessListener(document -> {
                    XpRewards rewards = document.toObject(XpRewards.class);
                    callback.onRewards(withSafeDefaults(rewards));
                })
                .addOnFailureListener(error -> callback.onRewards(XpRewards.safeDefaults()));
    }

    private XpRewards withSafeDefaults(XpRewards rewards) {
        XpRewards defaults = XpRewards.safeDefaults();
        if (rewards == null) {
            return defaults;
        }
        if (rewards.getCreateQuote() <= 0) {
            rewards.setCreateQuote(defaults.getCreateQuote());
        }
        if (rewards.getReceiveLike() <= 0) {
            rewards.setReceiveLike(defaults.getReceiveLike());
        }
        if (rewards.getValidReport() <= 0) {
            rewards.setValidReport(defaults.getValidReport());
        }
        return rewards;
    }

    private boolean isRequirementMet(Achievement achievement, UserStats stats) {
        if (achievement == null || stats == null || isBlank(achievement.getMetric())) {
            return false;
        }
        long value = metricValue(achievement.getMetric(), stats);
        long target = achievement.getTargetValue();
        String operator = achievement.getOperator();
        if ("GREATER_THAN".equals(operator)) {
            return value > target;
        }
        if ("EQUAL".equals(operator)) {
            return value == target;
        }
        if ("LESS_OR_EQUAL".equals(operator)) {
            return value <= target;
        }
        if ("LESS_THAN".equals(operator)) {
            return value < target;
        }
        return value >= target;
    }

    private long metricValue(String metric, UserStats stats) {
        if ("totalXp".equals(metric)) {
            return stats.getTotalXp();
        }
        if ("totalQuotes".equals(metric)) {
            return stats.getTotalQuotes();
        }
        if ("totalLikesReceived".equals(metric)) {
            return stats.getTotalLikesReceived();
        }
        if ("maxSingleQuoteLikes".equals(metric) || "singleQuoteLikes".equals(metric)) {
            return stats.getMaxSingleQuoteLikes();
        }
        if ("totalMovieQuotes".equals(metric)) {
            return stats.getTotalMovieQuotes();
        }
        if ("totalSeriesQuotes".equals(metric)) {
            return stats.getTotalSeriesQuotes();
        }
        if ("totalBookQuotes".equals(metric)) {
            return stats.getTotalBookQuotes();
        }
        if ("validReports".equals(metric)) {
            return stats.getValidReports();
        }
        if ("invalidReports".equals(metric)) {
            return stats.getInvalidReports();
        }
        if ("unlockedAchievementCount".equals(metric)) {
            return stats.getUnlockedAchievementCount();
        }
        return 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean shouldNotify(String userId) {
        return auth.getCurrentUser() != null
                && !isBlank(userId)
                && userId.equals(auth.getCurrentUser().getUid());
    }

    private int safeLevel(int level) {
        return level <= 0 ? 1 : level;
    }

    private Level defaultLevel() {
        Level level = new Level();
        level.setLevel(1);
        level.setRequiredTotalXp(0);
        return level;
    }

    private interface XpRewardsCallback {
        void onRewards(XpRewards rewards);
    }

    private interface StatsCallback {
        void onStats(UserStats stats);
    }

    private interface StatsMutator {
        void mutate(UserStats stats);
    }

    private interface UnlockCallback {
        void onComplete(boolean unlocked);
    }

    private interface LevelResultCallback {
        void onResult(Level level);
    }

    private interface LevelSaveCallback {
        void onSaved(Level level);
    }

    private static class LikeTotals {
        private long totalLikes;
        private long maxSingleQuoteLikes;
    }
}
