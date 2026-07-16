package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.merg.quoteapp.utils.FriendlyErrorMapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    private static final String USERS_COLLECTION = "users";
    private static final String USERNAMES_COLLECTION = "usernames";
    private static final String USERNAME_LOGINS_COLLECTION = "usernameLogins";
    private static volatile AuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository();
                }
            }
        }
        return instance;
    }

    public void register(String username, String email, String password, AuthCallback callback) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            callback.onError("Geçerli bir kullanıcı adı girin.");
            return;
        }
        createAuthUser(username.trim(), normalizedUsername, email.trim(), password, callback);
    }

    private void createAuthUser(String username, String normalizedUsername, String email,
                                String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        callback.onError("Kullanıcı oluşturulamadı. Lütfen tekrar deneyin.");
                        return;
                    }
                    reserveUsernameAndCreateProfile(
                            result.getUser().getUid(),
                            username,
                            normalizedUsername,
                            email,
                            result.getUser(),
                            callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void reserveUsernameAndCreateProfile(String uid, String username,
                                                 String normalizedUsername, String email,
                                                 FirebaseUser authUser,
                                                 AuthCallback callback) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(uid);
        DocumentReference usernameRef = firestore.collection(USERNAMES_COLLECTION)
                .document(normalizedUsername);
        DocumentReference usernameLoginRef = firestore.collection(USERNAME_LOGINS_COLLECTION)
                .document(normalizedUsername);

        firestore.runTransaction(transaction -> {
                    if (transaction.get(usernameRef).exists()) {
                        throw new FirebaseFirestoreException(
                                "Username already reserved.",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS);
                    }

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("username", username);
                    userData.put("usernameLowercase", normalizedUsername);
                    userData.put("role", "user");
                    userData.put("validReports", 0);
                    userData.put("invalidReports", 0);
                    userData.put("reportRestrictionUntil", null);
                    userData.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> usernameData = new HashMap<>();
                    usernameData.put("uid", uid);
                    usernameData.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> loginData = new HashMap<>();
                    loginData.put("uid", uid);
                    loginData.put("email", email);
                    loginData.put("createdAt", FieldValue.serverTimestamp());

                    transaction.set(usernameRef, usernameData);
                    transaction.set(usernameLoginRef, loginData);
                    transaction.set(userRef, userData);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> authUser.delete()
                        .addOnCompleteListener(task -> {
                            if (error instanceof FirebaseFirestoreException
                                    && ((FirebaseFirestoreException) error).getCode()
                                    == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                                callback.onError("Bu kullanıcı adı zaten kullanılıyor.");
                                return;
                            }
                            callback.onError("Profil kaydedilemedi. Lütfen tekrar deneyin.");
                        }));
    }

    public void login(String emailOrUsername, String password, AuthCallback callback) {
        String identity = emailOrUsername.trim();
        if (identity.contains("@")) {
            signInWithEmail(identity, password, callback);
            return;
        }

        firestore.collection(USERNAME_LOGINS_COLLECTION)
                .document(normalizeUsername(identity))
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Bu kullanıcı adına ait bir hesap bulunamadı.");
                        return;
                    }
                    String email = document.getString("email");
                    if (email == null || email.trim().isEmpty()) {
                        callback.onError("Hesap bilgileri eksik. Lütfen e-posta ile giriş yapın.");
                        return;
                    }
                    signInWithEmail(email, password, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void resetPassword(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> {
                    if (isPrivacySafePasswordResetSuccess(error)) {
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(readablePasswordResetError(error));
                });
    }

    private boolean isPrivacySafePasswordResetSuccess(Exception error) {
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            return "ERROR_USER_NOT_FOUND".equals(code);
        }
        return false;
    }

    private String readablePasswordResetError(Exception error) {
        if (FriendlyErrorMapper.isNetworkError(error)) {
            return FriendlyErrorMapper.NETWORK_MESSAGE;
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return "Geçerli bir e-posta adresi girin.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Çok fazla deneme yapıldı. Lütfen daha sonra tekrar deneyin.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "İnternet bağlantınızı kontrol edin.";
                default:
                    break;
            }
        }
        return "Şifre sıfırlama isteği gönderilemedi. Lütfen tekrar deneyin.";
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String readableError(Exception error) {
        if (FriendlyErrorMapper.isNetworkError(error)) {
            return FriendlyErrorMapper.NETWORK_MESSAGE;
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.NOT_FOUND
                    || (details != null && details.toLowerCase(Locale.ROOT)
                    .contains("database (default) does not exist"))) {
                return "Firestore veritabanı henüz oluşturulmamış. "
                        + "Lütfen Firebase Console üzerinden Firestore Database oluşturun.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                return "Bu kullanıcı adı zaten kullanılıyor.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Bu işlem için gerekli izin alınamadı.";
            }
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            switch (code) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Bu e-posta adresi zaten kullanılıyor.";
                case "ERROR_INVALID_EMAIL":
                    return "Geçerli bir e-posta adresi girin.";
                case "ERROR_WEAK_PASSWORD":
                    return "Şifre daha güçlü olmalı.";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return "E-posta/kullanıcı adı veya şifre hatalı.";
                case "ERROR_USER_NOT_FOUND":
                    return "Bu bilgilere ait bir hesap bulunamadı.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Çok fazla deneme yapıldı. Lütfen daha sonra tekrar deneyin.";
                default:
                    break;
            }
        }
        return "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
    }
}
