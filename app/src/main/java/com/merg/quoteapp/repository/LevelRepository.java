package com.merg.quoteapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Level;

import java.util.ArrayList;
import java.util.List;

public class LevelRepository {

    public interface LevelsCallback {
        void onSuccess(List<Level> levels);

        void onError(String message);
    }

    public interface LevelCallback {
        void onSuccess(Level level);

        void onError(String message);
    }

    private static final String LEVELS_COLLECTION = "levels";
    private static final Object CACHE_LOCK = new Object();
    private static List<Level> cachedLevels;
    private static final List<LevelsCallback> PENDING_LEVEL_LOADS = new ArrayList<>();
    private static volatile LevelRepository instance;

    private final FirebaseFirestore firestore;

    private LevelRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the shared LevelRepository instance.
     *
     * @return singleton repository instance
     */
    public static LevelRepository getInstance() {
        if (instance == null) {
            synchronized (LevelRepository.class) {
                if (instance == null) {
                    instance = new LevelRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Loads all configured levels ordered by level number.
     *
     * @param callback levels callback
     */
    public void getAllLevels(LevelsCallback callback) {
        getAllLevels(false, callback);
    }

    /**
     * Loads all configured levels ordered by level number.
     *
     * @param forceRefresh true to bypass the process memory cache
     * @param callback levels callback
     */
    public void getAllLevels(boolean forceRefresh, LevelsCallback callback) {
        List<Level> cached = levelsFromCache();
        if (!forceRefresh && cached != null) {
            callback.onSuccess(cached);
            return;
        }

        synchronized (CACHE_LOCK) {
            if (!PENDING_LEVEL_LOADS.isEmpty()) {
                PENDING_LEVEL_LOADS.add(callback);
                return;
            }
            PENDING_LEVEL_LOADS.add(callback);
        }

        firestore.collection(LEVELS_COLLECTION)
                .orderBy("level", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Level> levels = snapshot.toObjects(Level.class);
                    if (levels == null || levels.isEmpty()) {
                        levels = defaultLevels();
                    }
                    synchronized (CACHE_LOCK) {
                        cachedLevels = new ArrayList<>(levels);
                    }
                    completePendingLevelLoads(levels, null);
                })
                .addOnFailureListener(error -> {
                    List<Level> fallback = cached != null ? cached : defaultLevels();
                    completePendingLevelLoads(fallback, null);
                });
    }

    /**
     * Loads the current level for a total XP value.
     *
     * @param totalXp user's total XP
     * @param callback level callback
     */
    public void getCurrentLevel(long totalXp, LevelCallback callback) {
        getAllLevels(new LevelsCallback() {
            @Override
            public void onSuccess(List<Level> levels) {
                callback.onSuccess(findCurrentLevel(levels, totalXp));
            }

            @Override
            public void onError(String message) {
                callback.onSuccess(defaultLevel());
            }
        });
    }

    /**
     * Loads the next level after a total XP value.
     *
     * @param totalXp user's total XP
     * @param callback level callback
     */
    public void getNextLevel(long totalXp, LevelCallback callback) {
        getAllLevels(new LevelsCallback() {
            @Override
            public void onSuccess(List<Level> levels) {
                callback.onSuccess(findNextLevel(levels, totalXp));
            }

            @Override
            public void onError(String message) {
                callback.onSuccess(null);
            }
        });
    }

    private List<Level> levelsFromCache() {
        synchronized (CACHE_LOCK) {
            return cachedLevels == null ? null : new ArrayList<>(cachedLevels);
        }
    }

    private void completePendingLevelLoads(List<Level> levels, String errorMessage) {
        List<LevelsCallback> callbacks;
        synchronized (CACHE_LOCK) {
            callbacks = new ArrayList<>(PENDING_LEVEL_LOADS);
            PENDING_LEVEL_LOADS.clear();
        }
        for (LevelsCallback callback : callbacks) {
            if (levels != null) {
                callback.onSuccess(new ArrayList<>(levels));
            } else {
                callback.onError(errorMessage);
            }
        }
    }

    private Level findCurrentLevel(List<Level> levels, long totalXp) {
        Level current = defaultLevel();
        long safeXp = Math.max(0, totalXp);
        if (levels == null) {
            return current;
        }
        for (Level level : levels) {
            if (level != null && level.getRequiredTotalXp() <= safeXp
                    && level.getRequiredTotalXp() >= current.getRequiredTotalXp()) {
                current = level;
            }
        }
        return current;
    }

    private Level findNextLevel(List<Level> levels, long totalXp) {
        if (levels == null) {
            return null;
        }
        long safeXp = Math.max(0, totalXp);
        Level next = null;
        for (Level level : levels) {
            if (level != null && level.getRequiredTotalXp() > safeXp
                    && (next == null
                    || level.getRequiredTotalXp() < next.getRequiredTotalXp())) {
                next = level;
            }
        }
        return next;
    }

    private List<Level> defaultLevels() {
        List<Level> levels = new ArrayList<>();
        levels.add(defaultLevel());
        return levels;
    }

    private Level defaultLevel() {
        Level level = new Level();
        level.setLevel(1);
        level.setRequiredTotalXp(0);
        level.setTitle("Seviye 1");
        level.setBadgeName("Başlangıç");
        return level;
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Seviye sistemi için Firestore kurallarını kontrol edin.";
        }
        return "Seviye bilgileri yüklenemedi. Lütfen tekrar deneyin.";
    }
}
