package com.merg.quoteapp.utils;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Locale;

public final class FriendlyErrorMapper {

    public static final String NETWORK_MESSAGE =
            "İnternet bağlantısı kurulamadı. Bağlantını kontrol edip tekrar dene.";
    public static final String GENERIC_MESSAGE =
            "İşlem tamamlanamadı. Lütfen tekrar deneyin.";

    private FriendlyErrorMapper() {
    }

    public static String from(Exception error, String fallback) {
        if (error == null) {
            return fallback == null ? GENERIC_MESSAGE : fallback;
        }
        if (isNetworkError(error)) {
            return NETWORK_MESSAGE;
        }
        return fallback == null ? GENERIC_MESSAGE : fallback;
    }

    public static boolean isNetworkError(Exception error) {
        if (error == null) {
            return false;
        }
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        if (error instanceof FirebaseNetworkException
                || message.contains("network")
                || message.contains("unavailable")
                || message.contains("unable to resolve host")
                || message.contains("timeout")
                || message.contains("timed out")) {
            return true;
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE
                    || firestoreError.getCode() == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return true;
            }
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
                return true;
            }
        }
        return false;
    }
}
