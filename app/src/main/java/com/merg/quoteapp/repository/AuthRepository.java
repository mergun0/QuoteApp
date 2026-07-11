package com.merg.quoteapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
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

        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("usernameLowercase", normalizedUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onError("Bu kullanıcı adı zaten kullanılıyor.");
                        return;
                    }
                    createAuthUser(username.trim(), normalizedUsername, email.trim(), password, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void createAuthUser(String username, String normalizedUsername, String email,
                                String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        callback.onError("Kullanıcı oluşturulamadı. Lütfen tekrar deneyin.");
                        return;
                    }

                    String uid = result.getUser().getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("username", username);
                    userData.put("usernameLowercase", normalizedUsername);
                    userData.put("email", email);
                    userData.put("createdAt", FieldValue.serverTimestamp());

                    firestore.collection(USERS_COLLECTION)
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(error -> result.getUser().delete()
                                    .addOnCompleteListener(task ->
                                            callback.onError("Profil kaydedilemedi. Lütfen tekrar deneyin.")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void login(String emailOrUsername, String password, AuthCallback callback) {
        String identity = emailOrUsername.trim();
        if (identity.contains("@")) {
            signInWithEmail(identity, password, callback);
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("usernameLowercase", normalizeUsername(identity))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> loginWithUsernameResult(snapshot, password, callback))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void loginWithUsernameResult(QuerySnapshot snapshot, String password,
                                         AuthCallback callback) {
        if (snapshot.isEmpty()) {
            callback.onError("Bu kullanıcı adına ait bir hesap bulunamadı.");
            return;
        }

        String email = snapshot.getDocuments().get(0).getString("email");
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Hesap bilgileri eksik. Lütfen e-posta ile giriş yapın.");
            return;
        }
        signInWithEmail(email, password, callback);
    }

    private void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void resetPassword(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
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
                        + "Lütfen Firebase Console üzerinden veritabanını oluşturun.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Firestore erişim izni reddedildi. Güvenlik kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Firestore hizmetine ulaşılamıyor. İnternet bağlantınızı kontrol edin.";
            }
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return "Geçerli bir e-posta adresi girin.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Bu e-posta adresi zaten kullanılıyor.";
                case "ERROR_WEAK_PASSWORD":
                    return "Şifre yeterince güçlü değil.";
                case "ERROR_USER_NOT_FOUND":
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return "E-posta/kullanıcı adı veya şifre hatalı.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Çok fazla deneme yapıldı. Lütfen daha sonra tekrar deneyin.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "İnternet bağlantınızı kontrol edin.";
                default:
                    break;
            }
        }
        return "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
    }
}
