package com.merg.quoteapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;

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
        firestore.collection(ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.toObjects(Achievement.class)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
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

        firestore.collection(ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.toObjects(Achievement.class)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
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
