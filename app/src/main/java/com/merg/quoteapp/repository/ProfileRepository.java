package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.utils.QuoteVisibilityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileRepository {

    public interface ProfileCallback {
        void onSuccess(ProfileStats stats);

        void onError(String message);
    }

    public interface AccountInfoCallback {
        void onSuccess(String username, String email);

        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final LikeRepository likeRepository = LikeRepository.getInstance();

    public void getProfile(ProfileCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Profil bilgileri için giriş yapmalısınız.");
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    String username = userDocument.getString("username");
                    String profileEmail = userDocument.getString("email");
                    if (username == null || username.trim().isEmpty()) {
                        username = "Kullanıcı";
                    }
                    if (profileEmail == null || profileEmail.trim().isEmpty()) {
                        profileEmail = currentUser.getEmail() == null ? "" : currentUser.getEmail();
                    }
                    loadQuoteStats(currentUser.getUid(), username, profileEmail, callback);
                })
                .addOnFailureListener(error ->
                        callback.onError("Profil bilgileri yüklenemedi."));
    }

    public void getAccountInfo(AccountInfoCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onSuccess("Kullanıcı", "");
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    String username = userDocument.getString("username");
                    String email = userDocument.getString("email");
                    if (username == null || username.trim().isEmpty()) {
                        username = "Kullanıcı";
                    }
                    if (email == null || email.trim().isEmpty()) {
                        email = currentUser.getEmail() == null ? "" : currentUser.getEmail();
                    }
                    callback.onSuccess(username, email);
                })
                .addOnFailureListener(error -> callback.onSuccess(
                        "Kullanıcı",
                        currentUser.getEmail() == null ? "" : currentUser.getEmail()));
    }

    private void loadQuoteStats(String uid, String username, String email,
                                ProfileCallback callback) {
        firestore.collection("quotes")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isHidden", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int movieCount = 0;
                    int seriesCount = 0;
                    int bookCount = 0;
                    List<String> quoteIds = new ArrayList<>();

                    for (int index = 0; index < snapshot.size(); index++) {
                        if (QuoteVisibilityUtils.isHidden(snapshot.getDocuments().get(index))) {
                            continue;
                        }
                        String quoteId = snapshot.getDocuments().get(index).getString("quoteId");
                        if (quoteId == null || quoteId.trim().isEmpty()) {
                            quoteId = snapshot.getDocuments().get(index).getId();
                        }
                        quoteIds.add(quoteId);
                        String type = snapshot.getDocuments().get(index).getString("type");
                        if ("Film".equals(type)) {
                            movieCount++;
                        } else if ("Dizi".equals(type)) {
                            seriesCount++;
                        } else if ("Kitap".equals(type)) {
                            bookCount++;
                        }
                    }

                    int finalMovieCount = movieCount;
                    int finalSeriesCount = seriesCount;
                    int finalBookCount = bookCount;
                    likeRepository.getLikeCounts(quoteIds, new LikeRepository.LikeCountsCallback() {
                        @Override
                        public void onSuccess(Map<String, Long> counts) {
                            int totalLikes = 0;
                            for (Long count : counts.values()) {
                                totalLikes += count == null ? 0 : count.intValue();
                            }
                            callback.onSuccess(new ProfileStats(
                                    username,
                                    email,
                                    quoteIds.size(),
                                    finalMovieCount,
                                    finalSeriesCount,
                                    finalBookCount,
                                    totalLikes));
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
                })
                .addOnFailureListener(error ->
                        callback.onError("Alıntı istatistikleri yüklenemedi."));
    }
}
