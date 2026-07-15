package com.merg.quoteapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.utils.FriendlyErrorMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AchievementRepository {

    public interface AchievementsCallback {
        void onSuccess(List<Achievement> achievements);

        void onError(String message);
    }

    public interface UserAchievementsCallback {
        void onSuccess(List<UserAchievement> achievements);

        void onError(String message);
    }

    public interface AchievementStatusCallback {
        void onSuccess(List<Achievement> lockedAchievements,
                       List<UserAchievement> unlockedAchievements);

        void onError(String message);
    }

    private static final String ACHIEVEMENTS_COLLECTION = "achievements";
    private static final String USER_ACHIEVEMENTS_COLLECTION = "userAchievements";
    private static final Object CACHE_LOCK = new Object();
    private static List<Achievement> cachedActiveAchievements;
    private static final List<AchievementsCallback> PENDING_ACTIVE_LOADS = new ArrayList<>();
    private static volatile AchievementRepository instance;

    private final FirebaseFirestore firestore;

    private AchievementRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the shared AchievementRepository instance.
     *
     * @return singleton repository instance
     */
    public static AchievementRepository getInstance() {
        if (instance == null) {
            synchronized (AchievementRepository.class) {
                if (instance == null) {
                    instance = new AchievementRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Loads active achievement definitions ordered for display.
     *
     * @param callback active achievements callback
     */
    public void getActiveAchievements(AchievementsCallback callback) {
        getActiveAchievements(false, callback);
    }

    /**
     * Loads active achievement definitions ordered for display.
     *
     * @param forceRefresh true to bypass the process memory cache
     * @param callback active achievements callback
     */
    public void getActiveAchievements(boolean forceRefresh, AchievementsCallback callback) {
        List<Achievement> cached = activeAchievementsFromCache();
        if (!forceRefresh && cached != null) {
            callback.onSuccess(cached);
            return;
        }

        synchronized (CACHE_LOCK) {
            if (!PENDING_ACTIVE_LOADS.isEmpty()) {
                PENDING_ACTIVE_LOADS.add(callback);
                return;
            }
            PENDING_ACTIVE_LOADS.add(callback);
        }

        firestore.collection(ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Achievement> achievements = snapshot.toObjects(Achievement.class);
                    synchronized (CACHE_LOCK) {
                        cachedActiveAchievements = new ArrayList<>(achievements);
                    }
                    completePendingActiveLoads(achievements, null);
                })
                .addOnFailureListener(error -> {
                    List<Achievement> fallback = cached != null ? cached : new ArrayList<>();
                    if (!fallback.isEmpty()) {
                        completePendingActiveLoads(fallback, null);
                    } else {
                        completePendingActiveLoads(null, readableError(error));
                    }
                });
    }

    /**
     * Loads active achievement definitions for a category.
     *
     * @param category achievement category such as SOCIAL, QUOTE or MODERATION
     * @param callback achievements callback
     */
    public void getAchievementsByCategory(String category, AchievementsCallback callback) {
        if (isBlank(category)) {
            callback.onError("Başarı kategorisi bulunamadı.");
            return;
        }

        getActiveAchievements(new AchievementsCallback() {
            @Override
            public void onSuccess(List<Achievement> achievements) {
                List<Achievement> filtered = new ArrayList<>();
                for (Achievement achievement : achievements) {
                    if (achievement != null && category.equals(achievement.getCategory())) {
                        filtered.add(achievement);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Loads achievements unlocked by a user.
     *
     * @param userId user id whose achievements will be loaded
     * @param callback user achievements callback
     */
    public void getUserAchievements(String userId, UserAchievementsCallback callback) {
        if (isBlank(userId)) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(USER_ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.toObjects(UserAchievement.class)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads active achievements and splits them into locked definitions and unlocked user records.
     *
     * @param userId user id whose achievement status will be calculated
     * @param callback combined achievement status callback
     */
    public void getLockedAndUnlockedAchievements(String userId, AchievementStatusCallback callback) {
        getActiveAchievements(new AchievementsCallback() {
            @Override
            public void onSuccess(List<Achievement> achievements) {
                getUserAchievements(userId, new UserAchievementsCallback() {
                    @Override
                    public void onSuccess(List<UserAchievement> userAchievements) {
                        callback.onSuccess(filterLocked(achievements, userAchievements), userAchievements);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private List<Achievement> activeAchievementsFromCache() {
        synchronized (CACHE_LOCK) {
            return cachedActiveAchievements == null
                    ? null : new ArrayList<>(cachedActiveAchievements);
        }
    }

    private void completePendingActiveLoads(List<Achievement> achievements, String errorMessage) {
        List<AchievementsCallback> callbacks;
        synchronized (CACHE_LOCK) {
            callbacks = new ArrayList<>(PENDING_ACTIVE_LOADS);
            PENDING_ACTIVE_LOADS.clear();
        }
        for (AchievementsCallback callback : callbacks) {
            if (achievements != null) {
                callback.onSuccess(new ArrayList<>(achievements));
            } else {
                callback.onError(errorMessage);
            }
        }
    }

    private List<Achievement> filterLocked(List<Achievement> achievements,
                                           List<UserAchievement> userAchievements) {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement != null && !isUnlocked(achievement, userAchievements)) {
                locked.add(achievement);
            }
        }
        return locked;
    }

    private boolean isUnlocked(Achievement achievement, List<UserAchievement> userAchievements) {
        if (achievement == null || userAchievements == null) {
            return false;
        }
        for (UserAchievement userAchievement : userAchievements) {
            if (userAchievement != null
                    && achievement.getAchievementId() != null
                    && achievement.getAchievementId().equals(userAchievement.getAchievementId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (FriendlyErrorMapper.isNetworkError(error)) {
            return FriendlyErrorMapper.NETWORK_MESSAGE;
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Başarı sistemi için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    && details != null && details.toLowerCase(Locale.ROOT).contains("index")) {
                return "Başarı sistemi için gerekli Firestore indeksi eksik.";
            }
        }
        return "Başarı bilgileri yüklenemedi. Lütfen tekrar deneyin.";
    }
}
