package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.merg.quoteapp.model.ProfileStats;

public class ProfileRepository {

    public interface ProfileCallback {
        void onSuccess(ProfileStats stats);

        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

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

    private void loadQuoteStats(String uid, String username, String email,
                                ProfileCallback callback) {
        firestore.collection("quotes")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int movieCount = 0;
                    int seriesCount = 0;
                    int bookCount = 0;

                    for (int index = 0; index < snapshot.size(); index++) {
                        String type = snapshot.getDocuments().get(index).getString("type");
                        if ("Film".equals(type)) {
                            movieCount++;
                        } else if ("Dizi".equals(type)) {
                            seriesCount++;
                        } else if ("Kitap".equals(type)) {
                            bookCount++;
                        }
                    }

                    callback.onSuccess(new ProfileStats(
                            username,
                            email,
                            snapshot.size(),
                            movieCount,
                            seriesCount,
                            bookCount));
                })
                .addOnFailureListener(error ->
                        callback.onError("Alıntı istatistikleri yüklenemedi."));
    }
}
