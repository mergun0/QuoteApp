package com.merg.quoteapp.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.utils.FriendlyErrorMapper;
import com.merg.quoteapp.utils.QuoteVisibilityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteRepository {

    private static final String TAG = "FavoriteRepository";
    private static final String FAVORITES_COLLECTION = "favorites";
    private static final String QUOTES_COLLECTION = "quotes";
    private static final String USERS_COLLECTION = "users";
    private static volatile FavoriteRepository instance;

    public interface OperationCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface SavedStateCallback {
        void onSuccess(boolean saved);

        void onError(String message);
    }

    public interface SavedQuotesCallback {
        void onSuccess(List<Quote> quotes);

        void onError(String message);
    }

    public interface FavoriteCountCallback {
        void onSuccess(long count);

        void onError(String message);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Map<String, String> usernameCache = new HashMap<>();

    private FavoriteRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static FavoriteRepository getInstance() {
        if (instance == null) {
            synchronized (FavoriteRepository.class) {
                if (instance == null) {
                    instance = new FavoriteRepository();
                }
            }
        }
        return instance;
    }

    public void saveQuote(String quoteId, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Alıntı kaydetmek için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Kaydedilecek alıntı bulunamadı.");
            return;
        }

        String favoriteId = favoriteDocumentId(quoteId, user.getUid());
        Map<String, Object> data = new HashMap<>();
        data.put("favoriteId", favoriteId);
        data.put("quoteId", quoteId);
        data.put("userId", user.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference favoriteRef = firestore.collection(FAVORITES_COLLECTION)
                .document(favoriteId);
        DocumentReference quoteRef = firestore.collection(QUOTES_COLLECTION).document(quoteId);
        final String userId = user.getUid();
        final String[] transactionStep = {"prepare save transaction"};

        firestore.runTransaction(transaction -> {
                    transactionStep[0] = "read favorite document";
                    DocumentSnapshot favoriteSnapshot = transaction.get(favoriteRef);
                    transactionStep[0] = "read quote document";
                    DocumentSnapshot quoteSnapshot = transaction.get(quoteRef);
                    if (favoriteSnapshot.exists()) {
                        return false;
                    }
                    if (!quoteSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Quote document not found for favorite save.",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    if (QuoteVisibilityUtils.isHidden(quoteSnapshot)) {
                        throw new FirebaseFirestoreException(
                                "Quote document is hidden.",
                                FirebaseFirestoreException.Code.PERMISSION_DENIED);
                    }
                    transactionStep[0] = "create favorite document";
                    transaction.set(favoriteRef, data);
                    transactionStep[0] = "update quote favoriteCount";
                    transaction.update(quoteRef, "favoriteCount",
                            favoriteCountFrom(quoteSnapshot) + 1L);
                    return true;
                })
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "saveQuote success. quoteId=" + quoteId + ", userId=" + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    logFavoriteFailure("saveQuote", quoteId, userId, transactionStep[0], error);
                    callback.onError(readableError(error));
                });
    }

    public void unsaveQuote(String quoteId, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Kaydı kaldırmak için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Kaydı kaldırılacak alıntı bulunamadı.");
            return;
        }

        DocumentReference favoriteRef = firestore.collection(FAVORITES_COLLECTION)
                .document(favoriteDocumentId(quoteId, user.getUid()));
        DocumentReference quoteRef = firestore.collection(QUOTES_COLLECTION).document(quoteId);
        final String userId = user.getUid();
        final String[] transactionStep = {"prepare unsave transaction"};

        firestore.runTransaction(transaction -> {
                    transactionStep[0] = "read favorite document";
                    DocumentSnapshot favoriteSnapshot = transaction.get(favoriteRef);
                    transactionStep[0] = "read quote document";
                    DocumentSnapshot quoteSnapshot = transaction.get(quoteRef);
                    if (!favoriteSnapshot.exists()) {
                        return false;
                    }
                    if (!quoteSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Quote document not found for favorite unsave.",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    transactionStep[0] = "delete favorite document";
                    transaction.delete(favoriteRef);
                    transactionStep[0] = "update quote favoriteCount";
                    transaction.update(quoteRef, "favoriteCount",
                            Math.max(0L, favoriteCountFrom(quoteSnapshot) - 1L));
                    return true;
                })
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "unsaveQuote success. quoteId=" + quoteId + ", userId=" + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    logFavoriteFailure("unsaveQuote", quoteId, userId, transactionStep[0], error);
                    callback.onError(readableError(error));
                });
    }

    public void isSavedByCurrentUser(String quoteId, SavedStateCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Kayıt durumunu görmek için giriş yapmalısınız.");
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Alıntı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(FAVORITES_COLLECTION)
                .document(favoriteDocumentId(quoteId, user.getUid()))
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(document.exists()))
                .addOnFailureListener(error -> {
                    String favoriteId = favoriteDocumentId(quoteId, user.getUid());
                    logFavoriteFailure("isSavedByCurrentUser", quoteId, user.getUid(),
                            "get " + FAVORITES_COLLECTION + "/" + favoriteId, error);
                    callback.onError(readableError(error));
                });
    }

    public void getFavoriteCount(String quoteId, FavoriteCountCallback callback) {
        if (isBlank(quoteId)) {
            callback.onError("Alıntı bilgisi bulunamadı.");
            return;
        }

        firestore.collection(QUOTES_COLLECTION)
                .document(quoteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Alıntı artık mevcut değil.");
                        return;
                    }
                    if (QuoteVisibilityUtils.isHidden(document)) {
                        callback.onError(QuoteVisibilityUtils.HIDDEN_QUOTE_MESSAGE);
                        return;
                    }
                    callback.onSuccess(favoriteCountFrom(document));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void getSavedQuotesForCurrentUser(SavedQuotesCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Kaydedilen alıntıları görmek için giriş yapmalısınız.");
            return;
        }

        firestore.collection(FAVORITES_COLLECTION)
                .whereEqualTo("userId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> loadQuotesFromFavorites(
                        snapshot.getDocuments(), callback))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void loadQuotesFromFavorites(List<DocumentSnapshot> favoriteDocuments,
                                         SavedQuotesCallback callback) {
        if (favoriteDocuments == null || favoriteDocuments.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Quote> orderedQuotes = new ArrayList<>();
        final int[] remaining = {favoriteDocuments.size()};
        final boolean[] failed = {false};
        for (int index = 0; index < favoriteDocuments.size(); index++) {
            orderedQuotes.add(null);
            DocumentSnapshot favorite = favoriteDocuments.get(index);
            String quoteId = favorite.getString("quoteId");
            int quoteIndex = index;
            if (isBlank(quoteId)) {
                remaining[0]--;
                continue;
            }

            firestore.collection(QUOTES_COLLECTION)
                    .document(quoteId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (failed[0]) {
                            return;
                        }
                        Quote quote = document.toObject(Quote.class);
                        if (quote != null && QuoteVisibilityUtils.isVisible(document)) {
                            if (isBlank(quote.getQuoteId())) {
                                quote.setQuoteId(document.getId());
                            }
                            completeQuoteUsername(quote, completedQuote -> {
                                orderedQuotes.set(quoteIndex, completedQuote);
                                completeFavoriteQuoteLoad(orderedQuotes, remaining, callback);
                            });
                        } else {
                            completeFavoriteQuoteLoad(orderedQuotes, remaining, callback);
                        }
                    })
                    .addOnFailureListener(error -> {
                        if (failed[0]) {
                            return;
                        }
                        if (isHiddenOrMissingQuoteError(error)) {
                            completeFavoriteQuoteLoad(orderedQuotes, remaining, callback);
                            return;
                        }
                        failed[0] = true;
                        callback.onError(readableError(error));
                    });
        }
        if (remaining[0] == 0) {
            callback.onSuccess(compactQuotes(orderedQuotes));
        }
    }

    private void completeFavoriteQuoteLoad(List<Quote> orderedQuotes, int[] remaining,
                                           SavedQuotesCallback callback) {
        remaining[0]--;
        if (remaining[0] == 0) {
            callback.onSuccess(compactQuotes(orderedQuotes));
        }
    }

    private void completeQuoteUsername(Quote quote, QuoteUsernameCallback callback) {
        if (quote == null) {
            callback.onComplete(null);
            return;
        }
        if (!isBlank(quote.getUsername())) {
            callback.onComplete(quote);
            return;
        }
        if (isBlank(quote.getUserId())) {
            quote.setUsername("kullanıcı");
            callback.onComplete(quote);
            return;
        }
        String cachedUsername = usernameCache.get(quote.getUserId());
        if (!isBlank(cachedUsername)) {
            quote.setUsername(cachedUsername);
            callback.onComplete(quote);
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(quote.getUserId())
                .get()
                .addOnCompleteListener(task -> {
                    String username = task.isSuccessful() && task.getResult() != null
                            ? task.getResult().getString("username") : null;
                    if (isBlank(username)) {
                        username = "kullanıcı";
                    }
                    usernameCache.put(quote.getUserId(), username);
                    quote.setUsername(username);
                    callback.onComplete(quote);
                });
    }

    private List<Quote> compactQuotes(List<Quote> quotes) {
        List<Quote> result = new ArrayList<>();
        for (Quote quote : quotes) {
            if (quote != null && QuoteVisibilityUtils.isVisible(quote)) {
                result.add(quote);
            }
        }
        return result;
    }

    private String favoriteDocumentId(String quoteId, String userId) {
        return userId + "_" + quoteId;
    }

    private long favoriteCountFrom(DocumentSnapshot quoteSnapshot) {
        Object rawValue = quoteSnapshot.get("favoriteCount");
        if (rawValue instanceof Long) {
            return Math.max(0L, (Long) rawValue);
        }
        if (rawValue instanceof Integer) {
            return Math.max(0L, ((Integer) rawValue).longValue());
        }
        if (rawValue instanceof Double) {
            return Math.max(0L, ((Double) rawValue).longValue());
        }
        if (rawValue instanceof Number) {
            return Math.max(0L, ((Number) rawValue).longValue());
        }
        return 0L;
    }

    private boolean isHiddenOrMissingQuoteError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code =
                    ((FirebaseFirestoreException) error).getCode();
            return code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    || code == FirebaseFirestoreException.Code.NOT_FOUND;
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            switch (firestoreError.getCode()) {
                case PERMISSION_DENIED:
                    return QuoteVisibilityUtils.HIDDEN_QUOTE_MESSAGE;
                case UNAVAILABLE:
                    return "İnternet bağlantısı kurulamadı.";
                case ABORTED:
                    return "İşlem tamamlanamadı. Lütfen tekrar dene.";
                case NOT_FOUND:
                    return "Alıntı artık mevcut değil.";
                case FAILED_PRECONDITION:
                    return "Kaydetme işlemi şu anda tamamlanamadı.";
                default:
                    break;
            }
        }
        if (FriendlyErrorMapper.isNetworkError(error)) {
            return "İnternet bağlantısı kurulamadı.";
        }
        return "Kaydetme işlemi tamamlanamadı.";
    }

    private void logFavoriteFailure(String operation, String quoteId, String userId,
                                    String transactionStep, Exception exception) {
        StringBuilder details = new StringBuilder(operation)
                .append(" failed. quoteId=").append(quoteId)
                .append(", userId=").append(userId)
                .append(", step=").append(transactionStep)
                .append(", exceptionClass=").append(exception.getClass().getName())
                .append(", message=").append(exception.getMessage());
        if (exception instanceof FirebaseFirestoreException) {
            details.append(", firebaseCode=")
                    .append(((FirebaseFirestoreException) exception).getCode());
        }
        Log.e(TAG, details.toString(), exception);
    }

    private interface QuoteUsernameCallback {
        void onComplete(Quote quote);
    }
}
