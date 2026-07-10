package com.merg.quoteapp.repository;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.UserStats;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class UserStatsRepository {

    public interface UserStatsCallback {
        void onSuccess(UserStats stats);

        void onError(String message);
    }

    public interface OperationCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface UserStatsListenerCallback {
        void onStatsChanged(UserStats stats);

        void onError(String message);
    }

    private static final String USER_STATS_COLLECTION = "userStats";
    private static final String QUOTES_COLLECTION = "quotes";
    private static final String LIKES_COLLECTION = "likes";
    private static final long STATS_SYNC_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L;
    private static volatile UserStatsRepository instance;

    private final FirebaseFirestore firestore;

    private UserStatsRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the shared UserStatsRepository instance.
     *
     * @return singleton repository instance
     */
    public static UserStatsRepository getInstance() {
        if (instance == null) {
            synchronized (UserStatsRepository.class) {
                if (instance == null) {
                    instance = new UserStatsRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Loads a user's stats document.
     *
     * @param userId user id whose stats will be loaded
     * @param callback stats callback
     */
    public void getUserStats(String userId, UserStatsCallback callback) {
        if (isBlank(userId)) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess(defaultStats(userId));
                        return;
                    }
                    UserStats stats = document.toObject(UserStats.class);
                    callback.onSuccess(stats == null ? defaultStats(userId) : stats);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Observes a user's stats document in real time.
     *
     * @param userId user id whose stats will be observed
     * @param callback realtime stats callback
     * @return listener registration that should be removed when no longer needed
     */
    public ListenerRegistration observeUserStats(String userId,
                                                 UserStatsListenerCallback callback) {
        if (isBlank(userId)) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return null;
        }

        return firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        callback.onError(readableError(error));
                        return;
                    }
                    if (document == null || !document.exists()) {
                        callback.onStatsChanged(defaultStats(userId));
                        return;
                    }
                    UserStats stats = document.toObject(UserStats.class);
                    callback.onStatsChanged(stats == null ? defaultStats(userId) : stats);
                });
    }

    /**
     * Creates a default stats document if it does not already exist.
     *
     * @param userId user id for the stats document
     * @param callback operation callback
     */
    public void createDefaultUserStatsIfMissing(String userId, OperationCallback callback) {
        if (isBlank(userId)) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess();
                    } else {
                        firestore.collection(USER_STATS_COLLECTION)
                                .document(userId)
                                .set(defaultStatsMap(userId), SetOptions.merge())
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(error -> callback.onError(readableError(error)));
                    }
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Updates a user's stats document with the provided values.
     *
     * @param userId user id for the stats document
     * @param stats stats values to persist
     * @param callback operation callback
     */
    public void updateUserStats(String userId, UserStats stats, OperationCallback callback) {
        if (isBlank(userId) || stats == null) {
            callback.onError("Güncellenecek istatistik bulunamadı.");
            return;
        }

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .set(statsMap(userId, stats), SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Adds XP to a user's stats document.
     *
     * @param userId user id that will receive XP
     * @param amount XP amount to add
     * @param callback operation callback
     */
    public void addXp(String userId, long amount, OperationCallback callback) {
        if (isBlank(userId)) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return;
        }
        if (amount <= 0) {
            callback.onError("Eklenecek XP miktarı geçersiz.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("userId", userId);
        updates.put("totalXp", FieldValue.increment(amount));
        updates.put("lastUpdatedAt", FieldValue.serverTimestamp());

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Safely recalculates public user stats from existing quotes and likes.
     * Existing XP and moderation counters are preserved and never decreased.
     *
     * @param userId user id whose stats will be synchronized
     * @param callback synchronized stats callback
     */
    public void syncUserStatsFromExistingData(String userId, UserStatsCallback callback) {
        if (isBlank(userId)) {
            callback.onError("KullanÄ±cÄ± bilgisi bulunamadÄ±.");
            return;
        }

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    UserStats existingStats = statsFromDocument(userId, document);
                    if (isSyncFresh(existingStats)) {
                        callback.onSuccess(existingStats);
                        return;
                    }
                    recalculateFromQuotes(userId, existingStats, callback);
                })
                .addOnFailureListener(error -> recalculateFromQuotes(
                        userId, defaultStats(userId), callback));
    }

    private void recalculateFromQuotes(String userId, UserStats existingStats,
                                       UserStatsCallback callback) {
        firestore.collection(QUOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> documents = snapshot.getDocuments();
                    RecalculatedStats totals = new RecalculatedStats();
                    totals.totalQuotes = documents.size();
                    for (DocumentSnapshot document : documents) {
                        Quote quote = document.toObject(Quote.class);
                        if (quote == null) {
                            continue;
                        }
                        countType(quote.getType(), totals);
                    }
                    if (documents.isEmpty()) {
                        persistSyncedStats(userId, existingStats, totals, callback);
                        return;
                    }
                    countLikesForQuotes(userId, existingStats, documents, totals,
                            0, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void countLikesForQuotes(String userId, UserStats existingStats,
                                     List<DocumentSnapshot> quoteDocuments,
                                     RecalculatedStats totals, int index,
                                     UserStatsCallback callback) {
        if (index >= quoteDocuments.size()) {
            persistSyncedStats(userId, existingStats, totals, callback);
            return;
        }

        DocumentSnapshot quoteDocument = quoteDocuments.get(index);
        String quoteId = quoteDocument.getId();
        Quote quote = quoteDocument.toObject(Quote.class);
        if (quote != null && !isBlank(quote.getQuoteId())) {
            quoteId = quote.getQuoteId();
        }
        if (isBlank(quoteId)) {
            countLikesForQuotes(userId, existingStats, quoteDocuments, totals,
                    index + 1, callback);
            return;
        }

        firestore.collection(LIKES_COLLECTION)
                .whereEqualTo("quoteId", quoteId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    long likeCount = Math.max(0, snapshot.getCount());
                    totals.totalLikesReceived += likeCount;
                    totals.maxSingleQuoteLikes = Math.max(totals.maxSingleQuoteLikes, likeCount);
                    countLikesForQuotes(userId, existingStats, quoteDocuments, totals,
                            index + 1, callback);
                })
                .addOnFailureListener(error -> countLikesForQuotes(userId, existingStats,
                        quoteDocuments, totals, index + 1, callback));
    }

    private void persistSyncedStats(String userId, UserStats existingStats,
                                    RecalculatedStats totals,
                                    UserStatsCallback callback) {
        UserStats syncedStats = existingStats == null ? defaultStats(userId) : existingStats;
        syncedStats.setUserId(userId);
        syncedStats.setLevel(syncedStats.getLevel() <= 0 ? 1 : syncedStats.getLevel());
        syncedStats.setTotalQuotes(Math.max(0, totals.totalQuotes));
        syncedStats.setTotalMovieQuotes(Math.max(0, totals.totalMovieQuotes));
        syncedStats.setTotalSeriesQuotes(Math.max(0, totals.totalSeriesQuotes));
        syncedStats.setTotalBookQuotes(Math.max(0, totals.totalBookQuotes));
        syncedStats.setTotalLikesReceived(Math.max(0, totals.totalLikesReceived));
        syncedStats.setMaxSingleQuoteLikes(Math.max(0, totals.maxSingleQuoteLikes));
        syncedStats.setTotalXp(Math.max(0, syncedStats.getTotalXp()));

        Map<String, Object> data = statsMap(userId, syncedStats);
        data.put("statsSyncedAt", FieldValue.serverTimestamp());

        firestore.collection(USER_STATS_COLLECTION)
                .document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(syncedStats))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private UserStats defaultStats(String userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLevel(1);
        return stats;
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

    private boolean isSyncFresh(UserStats stats) {
        if (stats == null || stats.getStatsSyncedAt() == null) {
            return false;
        }
        long syncedAt = stats.getStatsSyncedAt().toDate().getTime();
        return System.currentTimeMillis() - syncedAt < STATS_SYNC_INTERVAL_MILLIS;
    }

    private void countType(String type, RecalculatedStats totals) {
        if (isBlank(type)) {
            return;
        }
        String normalized = type.trim().toLowerCase();
        if ("film".equals(normalized) || "movie".equals(normalized)) {
            totals.totalMovieQuotes++;
        } else if ("dizi".equals(normalized) || "series".equals(normalized)
                || "tv".equals(normalized)) {
            totals.totalSeriesQuotes++;
        } else if ("kitap".equals(normalized) || "book".equals(normalized)) {
            totals.totalBookQuotes++;
        }
    }

    private Map<String, Object> defaultStatsMap(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("totalXp", 0);
        data.put("level", 1);
        data.put("totalQuotes", 0);
        data.put("totalLikesReceived", 0);
        data.put("maxSingleQuoteLikes", 0);
        data.put("totalMovieQuotes", 0);
        data.put("totalSeriesQuotes", 0);
        data.put("totalBookQuotes", 0);
        data.put("validReports", 0);
        data.put("invalidReports", 0);
        data.put("unlockedAchievementCount", 0);
        data.put("lastUpdatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private Map<String, Object> statsMap(String userId, UserStats stats) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("totalXp", stats.getTotalXp());
        data.put("level", stats.getLevel());
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

    private static class RecalculatedStats {
        private long totalQuotes;
        private long totalLikesReceived;
        private long maxSingleQuoteLikes;
        private long totalMovieQuotes;
        private long totalSeriesQuotes;
        private long totalBookQuotes;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Kullanıcı istatistikleri için Firestore kurallarını kontrol edin.";
        }
        return "Kullanıcı istatistikleri güncellenemedi. Lütfen tekrar deneyin.";
    }
}
