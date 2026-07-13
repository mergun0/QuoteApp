package com.merg.quoteapp.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.merg.quoteapp.model.Quote;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DiscoverRepository {

    public interface DiscoverCallback {
        void onQuotesChanged(List<Quote> quotes);

        void onError(String message);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public ListenerRegistration getAllQuotes(DiscoverCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser == null ? null : currentUser.getUid();
        return firestore.collection("quotes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(readableError(error));
                        return;
                    }

                    List<Quote> quotes = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            Quote quote = document.toObject(Quote.class);
                            if (quote != null) {
                                if (quote.getQuoteId() == null || quote.getQuoteId().isEmpty()) {
                                    quote.setQuoteId(document.getId());
                                }
                                if (currentUserId == null || !currentUserId.equals(quote.getUserId())) {
                                    quotes.add(quote);
                                }
                            }
                        }
                    }
                    completeMissingUsernames(quotes, callback);
                });
    }

    private void completeMissingUsernames(List<Quote> quotes, DiscoverCallback callback) {
        Set<String> missingUserIds = new LinkedHashSet<>();
        for (Quote quote : quotes) {
            if ((quote.getUsername() == null || quote.getUsername().trim().isEmpty())
                    && quote.getUserId() != null && !quote.getUserId().isEmpty()) {
                quote.setUsername("Kullanıcı");
                missingUserIds.add(quote.getUserId());
            }
        }

        if (missingUserIds.isEmpty()) {
            callback.onQuotesChanged(quotes);
            return;
        }

        List<String> userIds = new ArrayList<>(missingUserIds);
        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
        for (String userId : userIds) {
            userTasks.add(firestore.collection("users").document(userId).get());
        }

        Tasks.whenAllComplete(userTasks).addOnCompleteListener(ignored -> {
            for (int index = 0; index < userTasks.size(); index++) {
                Task<DocumentSnapshot> task = userTasks.get(index);
                if (!task.isSuccessful() || task.getResult() == null) {
                    continue;
                }
                String username = task.getResult().getString("username");
                if (username == null || username.trim().isEmpty()) {
                    continue;
                }
                String userId = userIds.get(index);
                for (Quote quote : quotes) {
                    if (userId.equals(quote.getUserId())) {
                        quote.setUsername(username);
                    }
                }
            }
            callback.onQuotesChanged(quotes);
        });
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Keşfet akışını okumak için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.NOT_FOUND
                    || (details != null && details.toLowerCase(Locale.ROOT)
                    .contains("database (default) does not exist"))) {
                return "Firestore veritabanı henüz oluşturulmamış.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Keşfet akışına ulaşılamıyor. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "Keşfet akışı yüklenemedi. Lütfen tekrar deneyin.";
    }
}
