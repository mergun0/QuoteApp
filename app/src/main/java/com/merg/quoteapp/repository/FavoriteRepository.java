package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Quote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoriteRepository {

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

    private static final String FAVORITES_COLLECTION = "favorites";
    private static final String QUOTES_COLLECTION = "quotes";
    private static final String USERS_COLLECTION = "users";
    private static volatile FavoriteRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Map<String, String> usernameCache = new HashMap<>();

    private FavoriteRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the shared FavoriteRepository instance.
     *
     * @return singleton repository instance
     */
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

    /**
     * Saves a quote for the current user.
     *
     * @param quoteId quote id to save
     * @param callback operation callback
     */
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

        firestore.collection(FAVORITES_COLLECTION)
                .document(favoriteId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Removes a saved quote for the current user.
     *
     * @param quoteId quote id to remove from saved collection
     * @param callback operation callback
     */
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

        firestore.collection(FAVORITES_COLLECTION)
                .document(favoriteDocumentId(quoteId, user.getUid()))
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Checks whether the current user saved a quote.
     *
     * @param quoteId quote id to check
     * @param callback saved state callback
     */
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
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads saved quotes for the current user.
     *
     * @param callback saved quotes callback
     */
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
                        if (quote != null) {
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
            if (quote != null) {
                result.add(quote);
            }
        }
        return result;
    }

    private String favoriteDocumentId(String quoteId, String userId) {
        return quoteId + "_" + userId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Kaydetme işlemi için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    && details != null && details.toLowerCase(Locale.ROOT).contains("index")) {
                return "Kaydedilen alıntılar için gerekli Firestore indeksi eksik.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Kaydedilen alıntılar yüklenemedi. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "Kaydetme işlemi tamamlanamadı. Lütfen tekrar deneyin.";
    }

    private interface QuoteUsernameCallback {
        void onComplete(Quote quote);
    }
}
