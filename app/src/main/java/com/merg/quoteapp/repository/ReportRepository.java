package com.merg.quoteapp.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportRepository {

    public interface ReportCallback {
        void onSuccess();

        void onAlreadyReported();

        void onDailyLimitReached();

        void onError(String message);
    }

    public interface BooleanCallback {
        void onSuccess(boolean value);

        void onError(String message);
    }

    public interface CountCallback {
        void onSuccess(long count);

        void onError(String message);
    }

    public static final String FUNCTIONS_REGION = "europe-west1";
    private static final String TAG = "ReportRepository";
    private static final String SUBMIT_REPORT_FUNCTION = "submitReport";
    private static volatile ReportRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFunctions functions;

    private ReportRepository() {
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance(FUNCTIONS_REGION);
    }

    /**
     * Returns the shared ReportRepository instance.
     *
     * @return singleton repository instance
     */
    public static ReportRepository getInstance() {
        if (instance == null) {
            synchronized (ReportRepository.class) {
                if (instance == null) {
                    instance = new ReportRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Submits a quote report through the trusted Cloud Function.
     *
     * @param quoteId id of the reported quote
     * @param reason selected report reason
     * @param description optional report description
     * @param callback result callback
     */
    public void submitReport(String quoteId, String reason, String description,
                             ReportCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(mapCallableErrorCode("unauthenticated"));
            return;
        }
        if (isBlank(quoteId)) {
            callback.onError("Raporlanacak alıntı bulunamadı.");
            return;
        }
        if (isBlank(reason)) {
            callback.onError("Lütfen bir rapor nedeni seçin.");
            return;
        }

        Map<String, Object> payload = buildSubmitReportPayload(quoteId, reason, description);
        functions
                .getHttpsCallable(SUBMIT_REPORT_FUNCTION)
                .call(payload)
                .addOnSuccessListener(result -> {
                    parseSubmitReportResult(result.getData());
                    Log.d(TAG, SUBMIT_REPORT_FUNCTION + " success. quoteId=" + safeLogValue(quoteId));
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> handleSubmitFailure(quoteId, error, callback));
    }

    /**
     * Duplicate checks are server-authoritative after the callable migration.
     *
     * @param quoteId quote id to check
     * @param reporterUserId reporter user id
     * @param callback duplicate report callback
     */
    public void alreadyReported(String quoteId, String reporterUserId, BooleanCallback callback) {
        callback.onSuccess(false);
    }

    /**
     * Daily limits are server-authoritative after the callable migration.
     *
     * @param reporterUserId reporter user id
     * @param callback report count callback
     */
    public void todayReportCount(String reporterUserId, CountCallback callback) {
        callback.onSuccess(0L);
    }

    /**
     * Builds the callable payload. Only these fields may be sent by Android.
     *
     * @param quoteId quote id
     * @param reason selected reason
     * @param description optional description
     * @return callable payload
     */
    @NonNull
    public static Map<String, Object> buildSubmitReportPayload(String quoteId, String reason,
                                                               @Nullable String description) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("quoteId", quoteId == null ? "" : quoteId.trim());
        payload.put("reason", reason == null ? "" : reason.trim());
        String cleanDescription = description == null ? "" : description.trim();
        if (!cleanDescription.isEmpty()) {
            payload.put("description", cleanDescription);
        }
        return payload;
    }

    /**
     * Maps callable error codes to user-safe Turkish messages.
     *
     * @param code Firebase Functions error code name
     * @return user-safe message
     */
    @NonNull
    public static String mapCallableErrorCode(@Nullable String code) {
        String normalized = code == null ? "unknown" :
                code.replace("_", "-").toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "unauthenticated":
                return "Rapor göndermek için giriş yapmalısınız.";
            case "invalid-argument":
                return "Rapor bilgileri geçerli değil.";
            case "not-found":
                return "Raporlamak istediğiniz alıntı artık mevcut değil.";
            case "permission-denied":
                return "Bu alıntıyı raporlayamazsınız.";
            case "already-exists":
                return "Bu alıntıyı daha önce raporladınız.";
            case "resource-exhausted":
                return "Şu anda daha fazla rapor gönderemezsiniz. Lütfen daha sonra tekrar deneyin.";
            case "failed-precondition":
                return "Bu rapor şu anda gönderilemez. Lütfen daha sonra tekrar deneyin.";
            case "unavailable":
            case "deadline-exceeded":
                return "Bağlantı kurulamadı. Lütfen tekrar deneyin.";
            default:
                return "Rapor gönderilemedi. Lütfen tekrar deneyin.";
        }
    }

    /**
     * Parses the callable result in a null-safe way.
     *
     * @param rawResult callable raw result
     * @return parsed result
     */
    @NonNull
    public static ReportResult parseSubmitReportResult(@Nullable Object rawResult) {
        if (!(rawResult instanceof Map)) {
            return new ReportResult("", "", null);
        }
        Map<?, ?> result = (Map<?, ?>) rawResult;
        Object reportId = result.get("reportId");
        Object status = result.get("status");
        Object remainingDailyReports = result.get("remainingDailyReports");
        Long remaining = remainingDailyReports instanceof Number
                ? ((Number) remainingDailyReports).longValue()
                : null;
        return new ReportResult(
                reportId instanceof String ? (String) reportId : "",
                status instanceof String ? (String) status : "",
                remaining
        );
    }

    private void handleSubmitFailure(String quoteId, Exception error, ReportCallback callback) {
        String code = "unknown";
        if (error instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException functionsError = (FirebaseFunctionsException) error;
            code = functionsError.getCode().name();
        }
        Log.e(TAG, SUBMIT_REPORT_FUNCTION + " failed. code=" + code
                + ", quoteId=" + safeLogValue(quoteId), error);

        String message = mapCallableErrorCode(code);
        String normalized = code.replace("_", "-").toLowerCase(Locale.ROOT);
        if ("already-exists".equals(normalized)) {
            callback.onAlreadyReported();
        } else if ("resource-exhausted".equals(normalized)) {
            callback.onDailyLimitReached();
        } else {
            callback.onError(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeLogValue(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ReportResult {
        private final String reportId;
        private final String status;
        private final Long remainingDailyReports;

        public ReportResult(String reportId, String status, Long remainingDailyReports) {
            this.reportId = reportId;
            this.status = status;
            this.remainingDailyReports = remainingDailyReports;
        }

        public String getReportId() {
            return reportId;
        }

        public String getStatus() {
            return status;
        }

        public Long getRemainingDailyReports() {
            return remainingDailyReports;
        }
    }
}
