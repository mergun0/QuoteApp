package com.merg.quoteapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Level;

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
        firestore.collection(LEVELS_COLLECTION)
                .orderBy("level", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.toObjects(Level.class)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads the current level for a total XP value.
     *
     * @param totalXp user's total XP
     * @param callback level callback
     */
    public void getCurrentLevel(long totalXp, LevelCallback callback) {
        firestore.collection(LEVELS_COLLECTION)
                .whereLessThanOrEqualTo("requiredTotalXp", totalXp)
                .orderBy("requiredTotalXp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onSuccess(snapshot.getDocuments().get(0).toObject(Level.class));
                    }
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads the next level after a total XP value.
     *
     * @param totalXp user's total XP
     * @param callback level callback
     */
    public void getNextLevel(long totalXp, LevelCallback callback) {
        firestore.collection(LEVELS_COLLECTION)
                .whereGreaterThan("requiredTotalXp", totalXp)
                .orderBy("requiredTotalXp", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onSuccess(snapshot.getDocuments().get(0).toObject(Level.class));
                    }
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
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
