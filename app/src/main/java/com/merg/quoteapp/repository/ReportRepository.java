package com.merg.quoteapp.repository;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.merg.quoteapp.model.Report;

import java.util.Calendar;
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

    private static final String REPORTS_COLLECTION = "reports";
    private static final int DAILY_REPORT_LIMIT = 5;
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
     * Submits a quote report after validating ownership, duplicate report and daily limit.
     *
     * @param quoteId id of the reported quote
     * @param reportedUserId owner id of the reported quote
     * @param reason selected report reason
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
        if (isBlank(quoteId) || isBlank(reportedUserId)) {
            callback.onError("Raporlanacak alıntı bilgisi bulunamadı.");
            return;
        }
        if (isBlank(reason)) {
            callback.onError("Lütfen bir rapor nedeni seçin.");
            return;
        }
        if (user.getUid().equals(reportedUserId)) {
            callback.onError("Kendi alıntınızı raporlayamazsınız.");
            return;
        }

        alreadyReported(quoteId, user.getUid(), new BooleanCallback() {
            @Override
            public void onSuccess(boolean value) {
                if (value) {
                    callback.onAlreadyReported();
                    return;
                }
                todayReportCount(user.getUid(), new CountCallback() {
                    @Override
                    public void onSuccess(long count) {
                        if (count >= DAILY_REPORT_LIMIT) {
                            callback.onDailyLimitReached();
                            return;
                        }
                        writeReport(quoteId, reportedUserId, user.getUid(),
                                reason, description, callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Checks whether the reporter already reported the quote.
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
                .document(reportDocumentId(quoteId, reporterUserId))
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(document.exists()))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    /**
     * Loads today's report count for the reporter.
     *
     * @param reporterUserId reporter user id
     * @param callback report count callback
     */
    public void todayReportCount(String reporterUserId, CountCallback callback) {
        if (isBlank(reporterUserId)) {
            callback.onError("Rapor limiti kontrol edilemedi.");
            return;
        }
        firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("reporterUserId", reporterUserId)
                .whereGreaterThanOrEqualTo("createdAt", todayStart())
                .whereLessThan("createdAt", tomorrowStart())
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void writeReport(String quoteId, String reportedUserId, String reporterUserId,
                             String reason, String description, ReportCallback callback) {
        String reportId = reportDocumentId(quoteId, reporterUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("reportId", reportId);
        data.put("quoteId", quoteId);
        data.put("reportedUserId", reportedUserId);
        data.put("reporterUserId", reporterUserId);
        data.put("reason", reason);
        data.put("description", description == null ? "" : description.trim());
        data.put("status", Report.STATUS_PENDING);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("reviewedAt", null);
        data.put("reviewedBy", null);
        data.put("isValidReport", null);

        firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private String reportDocumentId(String quoteId, String reporterUserId) {
        return quoteId + "_" + reporterUserId;
    }

    private Timestamp todayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    private Timestamp tomorrowStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return new Timestamp(calendar.getTime());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) error;
            String details = firestoreError.getMessage();
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Rapor göndermek için Firestore kurallarını kontrol edin.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    && details != null && details.toLowerCase(Locale.ROOT).contains("index")) {
                return "Rapor limiti için gerekli Firestore indeksi eksik.";
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Rapor gönderilemedi. İnternet bağlantınızı kontrol edin.";
            }
        }
        return "Rapor gönderilemedi. Lütfen tekrar deneyin.";
    }
}
