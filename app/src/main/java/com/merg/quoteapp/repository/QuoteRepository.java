package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.merg.quoteapp.model.Quote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuoteRepository {

    public interface OperationCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface QuotesCallback {
        void onQuotesChanged(List<Quote> quotes);

        void onError(String message);
    }

    private static final String QUOTES_COLLECTION = "quotes";
    private static volatile QuoteRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private QuoteRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static QuoteRepository getInstance() {
        if (instance == null) {
            synchronized (QuoteRepository.class) {
                if (instance == null) {
                    instance = new QuoteRepository();
                }
            }
        }
        return instance;
    }

    public void addQuote(Quote quote, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Alıntı paylaşmak için giriş yapmalısınız.");
            return;
        }

        DocumentReference document = firestore.collection(QUOTES_COLLECTION).document();
        quote.setQuoteId(document.getId());
        quote.setUserId(user.getUid());

        Map<String, Object> data = quoteData(quote);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        document.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public ListenerRegistration getCurrentUserQuotes(QuotesCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Alıntıları görmek için giriş yapmalısınız.");
            return null;
        }

        return firestore.collection(QUOTES_COLLECTION)
                .whereEqualTo("userId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(readableError(error));
                        return;
                    }

                    List<Quote> quotes = new ArrayList<>();
                    if (snapshot != null) {
                        snapshot.getDocuments().forEach(document -> {
                            Quote quote = document.toObject(Quote.class);
                            if (quote != null) {
                                if (quote.getQuoteId() == null || quote.getQuoteId().isEmpty()) {
                                    quote.setQuoteId(document.getId());
                                }
                                quotes.add(quote);
                            }
                        });
                    }
                    callback.onQuotesChanged(quotes);
                });
    }

    public void updateQuote(Quote quote, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Alıntıyı düzenlemek için giriş yapmalısınız.");
            return;
        }
        if (quote.getQuoteId() == null || quote.getQuoteId().trim().isEmpty()) {
            callback.onError("Alıntı bilgisi bulunamadı.");
            return;
        }

        quote.setUserId(user.getUid());
        Map<String, Object> data = quoteData(quote);
        data.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(QUOTES_COLLECTION)
                .document(quote.getQuoteId())
                .update(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void deleteQuote(String quoteId, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Alıntıyı silmek için giriş yapmalısınız.");
            return;
        }
        if (quoteId == null || quoteId.trim().isEmpty()) {
            callback.onError("Silinecek alıntı bulunamadı.");
            return;
        }

        firestore.collection(QUOTES_COLLECTION)
                .document(quoteId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private Map<String, Object> quoteData(Quote quote) {
        Map<String, Object> data = new HashMap<>();
        data.put("quoteId", quote.getQuoteId());
        data.put("userId", quote.getUserId());
        data.put("type", quote.getType());
        data.put("text", quote.getText());
        data.put("title", quote.getTitle());
        data.put("author", quote.getAuthor());
        data.put("characterName", quote.getCharacterName());
        data.put("season", quote.getSeason());
        data.put("episode", quote.getEpisode());
        data.put("tags", quote.getTags());
        data.put("spoiler", quote.isSpoiler());
        return data;
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Bu işlem için Firestore izniniz yok. Güvenlik kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.NOT_FOUND
                    || (details != null && details.toLowerCase(Locale.ROOT)
                    .contains("database (default) does not exist"))) {
                return "Firestore veritabanı henüz oluşturulmamış.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    && details != null && details.toLowerCase(Locale.ROOT).contains("index")) {
                return "Alıntı listesini sıralamak için gerekli Firestore indeksi eksik.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Firestore hizmetine ulaşılamıyor. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
    }
}
