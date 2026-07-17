package com.merg.quoteapp.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.merg.quoteapp.model.Report;

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

    private static final String TAG = "ReportRepository";
    private static final String REPORTS_COLLECTION = "reports";
    private static volatile ReportRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private ReportRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
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
     * Submits a quote report with a deterministic create-only Firestore document.
     *
     * @param quoteId id of the reported quote
     * @param reportedUserId owner id of the reported quote
     * @param reason selected stable report reason code
     * @param description optional report description
     * @param callback result callback
     */
    public void submitReport(String quoteId, String reportedUserId, String reason,
                             String description, ReportCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Rapor göndermek için giriş yapmalısınız.");
            return;
        }
        String reporterUserId = user.getUid();
        if (isBlank(quoteId) || isBlank(reportedUserId)) {
            callback.onError("Raporlanacak alıntı bulunamadı.");
            return;
        }
        if (reporterUserId.equals(reportedUserId)) {
            callback.onError("Kendi alıntınızı raporlayamazsınız.");
            return;
        }
        if (isBlank(reason)) {
            callback.onError("Lütfen bir rapor nedeni seçin.");
            return;
        }

        String cleanQuoteId = quoteId.trim();
        String reportId = reportDocumentId(cleanQuoteId, reporterUserId);
        Map<String, Object> data = buildDirectReportData(
                cleanQuoteId,
                reportedUserId.trim(),
                reporterUserId,
                reason,
                description,
                FieldValue.serverTimestamp()
        );
        logReportPayload(reportId, data);

        firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onAlreadyReported();
                        return;
                    }
                    firestore.collection(REPORTS_COLLECTION)
                            .document(reportId)
                            .set(data)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "direct report create success. reportId=" + reportId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(error -> handleCreateFailure(reportId, error, callback));
                })
                .addOnFailureListener(error -> handleDuplicateCheckFailure(reportId, error, callback));
    }

    /**
     * Checks the deterministic report document for lightweight duplicate UX only.
     *
     * @param quoteId quote id to check
     * @param reporterUserId reporter user id
     * @param callback duplicate report callback
     */
    public void alreadyReported(String quoteId, String reporterUserId, BooleanCallback callback) {
        if (isBlank(quoteId) || isBlank(reporterUserId)) {
            callback.onError("Rapor bilgisi kontrol edilemedi.");
            return;
        }
        firestore.collection(REPORTS_COLLECTION)
                .document(reportDocumentId(quoteId.trim(), reporterUserId.trim()))
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(document.exists()))
                .addOnFailureListener(error -> callback.onError("Rapor bilgisi kontrol edilemedi."));
    }

    /**
     * Daily limits require trusted backend support and are deferred in no-billing v1.0.
     *
     * @param reporterUserId reporter user id
     * @param callback report count callback
     */
    public void todayReportCount(String reporterUserId, CountCallback callback) {
        callback.onSuccess(0L);
    }

    /**
     * Builds the report document id used by Android and Firestore Rules.
     *
     * @param quoteId quote id
     * @param reporterUserId reporter uid
     * @return deterministic report id
     */
    @NonNull
    public static String reportDocumentId(String quoteId, String reporterUserId) {
        return (quoteId == null ? "" : quoteId.trim()) + "_"
                + (reporterUserId == null ? "" : reporterUserId.trim());
    }

    /**
     * Builds the direct Firestore report payload. Caller supplies the timestamp sentinel.
     *
     * @param quoteId quote id
     * @param reportedUserId quote owner uid
     * @param reporterUserId current user uid
     * @param reason selected stable reason code
     * @param description optional description
     * @param createdAt server timestamp sentinel for production, test value for unit tests
     * @return report payload
     */
    @NonNull
    public static Map<String, Object> buildDirectReportData(String quoteId,
                                                            String reportedUserId,
                                                            String reporterUserId,
                                                            String reason,
                                                            @Nullable String description,
                                                            Object createdAt) {
        String cleanQuoteId = quoteId == null ? "" : quoteId.trim();
        String cleanReporterUserId = reporterUserId == null ? "" : reporterUserId.trim();
        String reportId = reportDocumentId(cleanQuoteId, cleanReporterUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("reportId", reportId);
        data.put("quoteId", cleanQuoteId);
        data.put("reportedUserId", reportedUserId == null ? "" : reportedUserId.trim());
        data.put("reporterUserId", cleanReporterUserId);
        data.put("reason", reason == null ? "" : reason.trim());
        data.put("description", description == null ? "" : description.trim());
        data.put("status", Report.STATUS_PENDING);
        data.put("createdAt", createdAt);
        data.put("reviewedAt", null);
        data.put("reviewedBy", null);
        data.put("isValidReport", null);
        return data;
    }

    private void handleDuplicateCheckFailure(String reportId, Exception error, ReportCallback callback) {
        Log.e(TAG, "direct report duplicate check failed. reportId=" + reportId
                + ", code=" + firestoreCode(error), error);
        callback.onError("Rapor gönderilemedi. Lütfen tekrar deneyin.");
    }

    private void handleCreateFailure(String reportId, Exception error, ReportCallback callback) {
        Log.e(TAG, "direct report create failed. reportId=" + reportId
                + ", code=" + firestoreCode(error), error);
        callback.onError(mapFirestoreError(error));
    }

    private String mapFirestoreError(Exception error) {
        String code = firestoreCode(error);
        switch (code) {
            case "permission-denied":
                return "Rapor gönderilemedi. Lütfen tekrar deneyin.";
            case "not-found":
                return "Raporlamak istediğiniz alıntı artık mevcut değil.";
            case "unavailable":
            case "deadline-exceeded":
                return "Bağlantı kurulamadı. Lütfen tekrar deneyin.";
            case "aborted":
                return "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
            default:
                return "Rapor gönderilemedi. Lütfen tekrar deneyin.";
        }
    }

    private String firestoreCode(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            return firestoreError.getCode().name().replace("_", "-").toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    private void logReportPayload(String reportId, Map<String, Object> data) {
        Log.d(TAG, "direct report payload. reportId=" + reportId
                + ", fields=" + data.keySet()
                + ", quoteId=" + safeString(data.get("quoteId"))
                + ", reportedUserId=" + safeString(data.get("reportedUserId"))
                + ", reporterUserId=" + safeString(data.get("reporterUserId"))
                + ", reason=" + safeString(data.get("reason"))
                + ", descriptionLength=" + safeString(data.get("description")).length()
                + ", status=" + safeString(data.get("status"))
                + ", createdAtType=" + typeName(data.get("createdAt"))
                + ", reviewedAtType=" + typeName(data.get("reviewedAt"))
                + ", reviewedByType=" + typeName(data.get("reviewedBy"))
                + ", isValidReportType=" + typeName(data.get("isValidReport")));
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
