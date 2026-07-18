package com.merg.quoteapp.repository;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountDeletionRepository {

    public interface OperationCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface PendingStatusCallback {
        void onResult(boolean pending);
    }

    public enum DeletionState {
        CLEAR,
        PENDING,
        UNKNOWN
    }

    public interface DeletionStateCallback {
        void onResult(DeletionState state);
    }

    public static final String CONFIRMATION_TEXT = "HESABIMI S\u0130L";
    private static final String REQUESTS_COLLECTION = "accountDeletionRequests";
    private static final String USERS_COLLECTION = "users";
    private static final long DELETION_VERSION = 1L;
    private static volatile AccountDeletionRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private AccountDeletionRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static AccountDeletionRepository getInstance() {
        if (instance == null) {
            synchronized (AccountDeletionRepository.class) {
                if (instance == null) {
                    instance = new AccountDeletionRepository();
                }
            }
        }
        return instance;
    }

    public void checkCurrentUserPending(PendingStatusCallback callback) {
        checkCurrentUserDeletionState(state -> callback.onResult(state == DeletionState.PENDING));
    }

    public void checkCurrentUserDeletionState(DeletionStateCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(DeletionState.CLEAR);
            return;
        }
        firestore.collection(REQUESTS_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onResult(DeletionState.CLEAR);
                        return;
                    }
                    String status = snapshot.getString("status");
                    callback.onResult(isPendingStatus(status)
                            ? DeletionState.PENDING : DeletionState.CLEAR);
                })
                .addOnFailureListener(error -> callback.onResult(DeletionState.UNKNOWN));
    }

    public boolean currentUserUsesPasswordProvider() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return false;
        }
        for (UserInfo info : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public void requestAccountDeletion(String password, String confirmation,
                                       String reason, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Hesap silme talebi için tekrar giriş yapmanız gerekiyor.");
            return;
        }
        if (!CONFIRMATION_TEXT.equals(confirmation == null ? "" : confirmation.trim())) {
            callback.onError("Devam etmek için onay metnini tam olarak yazın: " + CONFIRMATION_TEXT);
            return;
        }
        if (currentUserUsesPasswordProvider()) {
            String email = user.getEmail();
            if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
                callback.onError("Hesabınızı doğrulamak için mevcut şifrenizi girin.");
                return;
            }
            user.reauthenticate(EmailAuthProvider.getCredential(email, password))
                    .addOnSuccessListener(result -> createDeletionRequest(user, reason, callback))
                    .addOnFailureListener(error -> callback.onError(readableError(error)));
            return;
        }
        user.reload()
                .addOnSuccessListener(unused -> createDeletionRequest(user, reason, callback))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void createDeletionRequest(FirebaseUser user, String reason, OperationCallback callback) {
        String uid = user.getUid();
        DocumentReference requestRef = firestore.collection(REQUESTS_COLLECTION).document(uid);
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(uid);

        requestRef.get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        callback.onSuccess();
                        return;
                    }
                    userRef.get()
                            .addOnSuccessListener(userSnapshot -> {
                                String username = userSnapshot.getString("username");
                                String normalizedUsername = userSnapshot.getString("usernameLowercase");
                                if (isBlank(username)) {
                                    username = "Kullanıcı";
                                }
                                if (isBlank(normalizedUsername)) {
                                    normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
                                }

                                Map<String, Object> requestData = new HashMap<>();
                                requestData.put("userId", uid);
                                requestData.put("username", username);
                                requestData.put("normalizedUsername", normalizedUsername);
                                requestData.put("status", "PENDING");
                                requestData.put("requestedAt", FieldValue.serverTimestamp());
                                requestData.put("requestedBy", uid);
                                requestData.put("reason", trimToLimit(reason, 500));
                                requestData.put("profileHidden", true);
                                requestData.put("deletionVersion", DELETION_VERSION);
                                requestData.put("completedAt", null);
                                requestData.put("completedBy", null);
                                requestData.put("failureCode", null);
                                requestData.put("failureMessage", null);
                                requestData.put("currentPhase", "REQUESTED");
                                requestData.put("completedPhases", new ArrayList<String>());

                                Map<String, Object> userUpdate = new HashMap<>();
                                userUpdate.put("deletionPending", true);
                                userUpdate.put("profileHidden", true);
                                userUpdate.put("deletionRequestedAt", FieldValue.serverTimestamp());

                                firestore.batch()
                                        .set(requestRef, requestData)
                                        .update(userRef, userUpdate)
                                        .commit()
                                        .addOnSuccessListener(unused -> callback.onSuccess())
                                        .addOnFailureListener(error -> callback.onError(readableError(error)));
                            })
                            .addOnFailureListener(error -> callback.onError(readableError(error)));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private boolean isPendingStatus(String status) {
        return "PENDING".equals(status) || "PROCESSING".equals(status) || "FAILED".equals(status);
    }

    private String trimToLimit(String value, int limit) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > limit ? trimmed.substring(0, limit) : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_WRONG_PASSWORD".equals(code) || "ERROR_INVALID_CREDENTIAL".equals(code)) {
                return "Şifreniz doğrulanamadı.";
            }
            if ("ERROR_REQUIRES_RECENT_LOGIN".equals(code)) {
                return "Güvenlik nedeniyle tekrar giriş yapmanız gerekiyor.";
            }
        }
        if (error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Hesap silme talebi oluşturulamadı. Lütfen tekrar giriş yapıp deneyin.";
        }
        return "Hesap silme talebi oluşturulamadı.";
    }
}
