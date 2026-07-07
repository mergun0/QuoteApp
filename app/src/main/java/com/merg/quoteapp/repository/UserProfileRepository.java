package com.merg.quoteapp.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.UserProfileData;
import com.merg.quoteapp.model.UserProfilePage;

import java.util.ArrayList;
import java.util.List;

public class UserProfileRepository {

    public interface UserProfileCallback {
        void onSuccess(UserProfilePage page);

        void onError(String message);
    }

    private static final int PAGE_SIZE = 20;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private DocumentSnapshot lastDocument;
    private boolean endReached;
    private boolean pageLoading;
    private String cachedUserId;
    private String cachedUsername;
    private Timestamp cachedJoinedAt;
    private int totalCount;
    private int movieCount;
    private int seriesCount;
    private int bookCount;

    public void resetPagination(String userId) {
        lastDocument = null;
        endReached = false;
        pageLoading = false;
        cachedUserId = userId;
        cachedUsername = null;
        cachedJoinedAt = null;
    }

    public void getNextPage(String userId, UserProfileCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("Kullanıcı bilgisi bulunamadı.");
            return;
        }
        if (!userId.equals(cachedUserId)) {
            resetPagination(userId);
        }
        if (pageLoading || endReached) {
            callback.onSuccess(createPage(new ArrayList<>(), !endReached));
            return;
        }
        pageLoading = true;

        if (cachedUsername == null) {
            loadProfileHeader(userId, callback);
        } else {
            loadQuotePage(userId, callback);
        }
    }

    private void loadProfileHeader(String userId, UserProfileCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (!userDocument.exists()) {
                        pageLoading = false;
                        callback.onError("Bu kullanıcı artık mevcut değil.");
                        return;
                    }
                    String username = userDocument.getString("username");
                    cachedUsername = username == null || username.trim().isEmpty()
                            ? "Kullanıcı" : username;
                    cachedJoinedAt = userDocument.getTimestamp("createdAt");
                    loadAggregateCounts(userId, callback);
                })
                .addOnFailureListener(error -> {
                    pageLoading = false;
                    callback.onError(readableError(error));
                });
    }

    private void loadAggregateCounts(String userId, UserProfileCallback callback) {
        Query baseQuery = firestore.collection("quotes").whereEqualTo("userId", userId);
        List<Task<AggregateQuerySnapshot>> tasks = new ArrayList<>();
        tasks.add(baseQuery.count().get(AggregateSource.SERVER));
        tasks.add(baseQuery.whereEqualTo("type", "Film").count().get(AggregateSource.SERVER));
        tasks.add(baseQuery.whereEqualTo("type", "Dizi").count().get(AggregateSource.SERVER));
        tasks.add(baseQuery.whereEqualTo("type", "Kitap").count().get(AggregateSource.SERVER));

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    totalCount = (int) ((AggregateQuerySnapshot) results.get(0)).getCount();
                    movieCount = (int) ((AggregateQuerySnapshot) results.get(1)).getCount();
                    seriesCount = (int) ((AggregateQuerySnapshot) results.get(2)).getCount();
                    bookCount = (int) ((AggregateQuerySnapshot) results.get(3)).getCount();
                    loadQuotePage(userId, callback);
                })
                .addOnFailureListener(error -> {
                    pageLoading = false;
                    callback.onError(readableError(error));
                });
    }

    private void loadQuotePage(String userId, UserProfileCallback callback) {
        Query query = firestore.collection("quotes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);
        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Quote> quotes = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Quote quote = document.toObject(Quote.class);
                        if (quote == null) {
                            continue;
                        }
                        if (quote.getQuoteId() == null || quote.getQuoteId().isEmpty()) {
                            quote.setQuoteId(document.getId());
                        }
                        if (quote.getUsername() == null || quote.getUsername().isEmpty()) {
                            quote.setUsername(cachedUsername);
                        }
                        quotes.add(quote);
                    }
                    if (!snapshot.isEmpty()) {
                        lastDocument = snapshot.getDocuments().get(snapshot.size() - 1);
                    }
                    endReached = snapshot.size() < PAGE_SIZE;
                    pageLoading = false;
                    callback.onSuccess(createPage(quotes, !endReached));
                })
                .addOnFailureListener(error -> {
                    pageLoading = false;
                    callback.onError(readableError(error));
                });
    }

    private UserProfilePage createPage(List<Quote> quotes, boolean hasMore) {
        UserProfileData profile = new UserProfileData(
                cachedUserId,
                cachedUsername == null ? "Kullanıcı" : cachedUsername,
                cachedJoinedAt,
                totalCount,
                movieCount,
                seriesCount,
                bookCount,
                quotes);
        return new UserProfilePage(profile, hasMore);
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Kullanıcı profilini görüntülemek için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Profil alıntıları için gerekli Firestore indeksi eksik.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Profil yüklenemedi. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "Kullanıcı profili yüklenemedi. Lütfen tekrar deneyin.";
    }
}
