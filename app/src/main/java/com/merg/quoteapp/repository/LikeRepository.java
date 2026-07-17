package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.merg.quoteapp.utils.FriendlyErrorMapper;
import com.merg.quoteapp.utils.QuoteVisibilityUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LikeRepository {

    private static final String TAG = "LikeRepository";

    public interface OperationCallback {

        /**
         * Called when the like operation completes successfully.
         */
        void onSuccess();

        /**
         * Called when the like operation fails.
         *
         * @param message readable error message
         */
        void onError(String message);
    }

    public interface LikedStateCallback {

        /**
         * Called with the current user's like state for a quote.
         *
         * @param liked true if the current user liked the quote
         */
        void onSuccess(boolean liked);

        /**
         * Called when the liked state cannot be loaded.
         *
         * @param message readable error message
         */
        void onError(String message);
    }

    public interface LikeCountCallback {

        /**
         * Called with the total like count for a quote.
         *
         * @param count total like count
         */
        void onSuccess(long count);

        /**
         * Called when the like count cannot be loaded.
         *
         * @param message readable error message
         */
        void onError(String message);
    }

    public interface LikeCountsCallback {

        /**
         * Called with like counts keyed by quote id.
         *
         * @param counts like counts by quote id
         */
        void onSuccess(Map<String, Long> counts);

        /**
         * Called when like counts cannot be loaded.
         *
         * @param message readable error message
         */
        void onError(String message);
    }

    private static final String LIKES_COLLECTION = "likes";
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, Boolean> LIKED_STATE_CACHE = new HashMap<>();
    private static volatile LikeRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final AchievementEngineRepository achievementEngineRepository;

    private LikeRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        achievementEngineRepository = AchievementEngineRepository.getInstance();
    }

    /**
     * Returns the singleton LikeRepository instance.
     *
     * @return shared LikeRepository instance
     */
    public static LikeRepository getInstance() {
        if (instance == null) {
            synchronized (LikeRepository.class) {
                if (instance == null) {
                    instance = new LikeRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a like document for the current user and quote.
     *
     * @param quoteId id of the quote to like
     * @param callback operation result callback
     */
    public void likeQuote(String quoteId, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Beğenmek için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Beğenilecek alıntı bulunamadı.");
            return;
        }

        String likeId = likeDocumentId(quoteId, user.getUid());
        Map<String, Object> data = new HashMap<>();
        data.put("likeId", likeId);
        data.put("quoteId", quoteId);
        data.put("userId", user.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference document = firestore.collection(LIKES_COLLECTION).document(likeId);
        DocumentReference quoteRef = firestore.collection("quotes").document(quoteId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot quoteSnapshot = transaction.get(quoteRef);
                    if (!quoteSnapshot.exists() || QuoteVisibilityUtils.isHidden(quoteSnapshot)) {
                        throw new FirebaseFirestoreException(
                                "Quote document is hidden or unavailable.",
                                FirebaseFirestoreException.Code.PERMISSION_DENIED);
                    }
                    if (transaction.get(document).exists()) {
                        return false;
                    }
                    transaction.set(document, data);
                    return true;
                })
                .addOnSuccessListener(created -> {
                    putLikedState(user.getUid(), quoteId, true);
                    if (Boolean.TRUE.equals(created)) {
                        achievementEngineRepository.onQuoteLiked(quoteId, user.getUid());
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Like transaction failed", error);
                    callback.onError(readableError(error));
                });
    }

    /**
     * Deletes the current user's like document for a quote.
     *
     * @param quoteId id of the quote to unlike
     * @param callback operation result callback
     */
    public void unlikeQuote(String quoteId, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Beğeniyi kaldırmak için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Beğenisi kaldırılacak alıntı bulunamadı.");
            return;
        }

        firestore.collection(LIKES_COLLECTION)
                .document(likeDocumentId(quoteId, user.getUid()))
                .delete()
                .addOnSuccessListener(unused -> {
                    putLikedState(user.getUid(), quoteId, false);
                    achievementEngineRepository.onQuoteUnliked(quoteId);
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Unlike failed", error);
                    callback.onError(readableError(error));
                });
    }

    /**
     * Checks whether the current user has liked a quote.
     *
     * @param quoteId id of the quote to check
     * @param callback liked state callback
     */
    public void isLikedByCurrentUser(String quoteId, LikedStateCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Beğeni durumunu görmek için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Alıntı bilgisi bulunamadı.");
            return;
        }

        Boolean cached = cachedLikedState(user.getUid(), quoteId);
        if (cached != null) {
            callback.onSuccess(cached);
        }

        firestore.collection(LIKES_COLLECTION)
                .document(likeDocumentId(quoteId, user.getUid()))
                .get()
                .addOnSuccessListener(document -> {
                    boolean liked = document.exists();
                    putLikedState(user.getUid(), quoteId, liked);
                    callback.onSuccess(liked);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads the total like count for a quote.
     *
     * @param quoteId id of the quote to count likes for
     * @param callback like count callback
     */
    public void getLikeCount(String quoteId, LikeCountCallback callback) {
        if (isBlank(quoteId)) {
            callback.onError("Alıntı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(LIKES_COLLECTION)
                .whereEqualTo("quoteId", quoteId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.getCount()))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads like counts for multiple quotes.
     *
     * @param quoteIds quote ids to count likes for
     * @param callback like counts callback
     */
    public void getLikeCounts(List<String> quoteIds, LikeCountsCallback callback) {
        if (quoteIds == null || quoteIds.isEmpty()) {
            callback.onSuccess(new HashMap<>());
            return;
        }

        Map<String, Long> counts = new HashMap<>();
        List<String> uniqueQuoteIds = new ArrayList<>();
        for (String quoteId : quoteIds) {
            if (!isBlank(quoteId) && !uniqueQuoteIds.contains(quoteId)) {
                uniqueQuoteIds.add(quoteId);
            }
        }
        if (uniqueQuoteIds.isEmpty()) {
            callback.onSuccess(counts);
            return;
        }

        final int[] remaining = {uniqueQuoteIds.size()};
        final boolean[] failed = {false};
        for (String quoteId : uniqueQuoteIds) {
            getLikeCount(quoteId, new LikeCountCallback() {
                @Override
                public void onSuccess(long count) {
                    if (failed[0]) {
                        return;
                    }
                    counts.put(quoteId, count);
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        callback.onSuccess(counts);
                    }
                }

                @Override
                public void onError(String message) {
                    if (failed[0]) {
                        return;
                    }
                    failed[0] = true;
                    callback.onError(message);
                }
            });
        }
    }

    private String likeDocumentId(String quoteId, String userId) {
        return userId + "_" + quoteId;
    }

    /**
     * Clears the in-memory liked-state cache for the active process.
     */
    public static void clearMemoryCache() {
        synchronized (CACHE_LOCK) {
            LIKED_STATE_CACHE.clear();
        }
    }

    private Boolean cachedLikedState(String userId, String quoteId) {
        synchronized (CACHE_LOCK) {
            return LIKED_STATE_CACHE.get(cacheKey(userId, quoteId));
        }
    }

    private static void putLikedState(String userId, String quoteId, boolean liked) {
        synchronized (CACHE_LOCK) {
            LIKED_STATE_CACHE.put(cacheKey(userId, quoteId), liked);
        }
    }

    private static String cacheKey(String userId, String quoteId) {
        return userId + ":" + quoteId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return QuoteVisibilityUtils.HIDDEN_QUOTE_MESSAGE;
            }
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Beğeni işlemi için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.NOT_FOUND
                    || (details != null && details.toLowerCase(Locale.ROOT)
                    .contains("database (default) does not exist"))) {
                return "Firestore veritabanı henüz oluşturulmamış.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Firestore hizmetine ulaşılamıyor. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "Beğeni işlemi tamamlanamadı. Lütfen tekrar deneyin.";
    }
}
