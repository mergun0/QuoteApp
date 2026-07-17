package com.merg.quoteapp.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ReportRepositoryTest {

    @Test
    public void buildSubmitReportPayload_containsOnlyCallableFields() {
        Map<String, Object> payload = ReportRepository.buildSubmitReportPayload(
                " quote123 ",
                " Spam ",
                " optional description "
        );

        assertEquals(3, payload.size());
        assertEquals("quote123", payload.get("quoteId"));
        assertEquals("Spam", payload.get("reason"));
        assertEquals("optional description", payload.get("description"));
        assertFalse(payload.containsKey("reporterUserId"));
        assertFalse(payload.containsKey("reportedUserId"));
        assertFalse(payload.containsKey("reportId"));
        assertFalse(payload.containsKey("status"));
        assertFalse(payload.containsKey("createdAt"));
    }

    @Test
    public void buildSubmitReportPayload_omitsBlankDescription() {
        Map<String, Object> payload = ReportRepository.buildSubmitReportPayload(
                "quote123",
                "Spam",
                "   "
        );

        assertEquals(2, payload.size());
        assertFalse(payload.containsKey("description"));
    }

    @Test
    public void mapCallableErrorCode_mapsDuplicateAndRateLimit() {
        assertEquals(
                "Bu alıntıyı daha önce raporladınız.",
                ReportRepository.mapCallableErrorCode("already-exists")
        );
        assertEquals(
                "Şu anda daha fazla rapor gönderemezsiniz. Lütfen daha sonra tekrar deneyin.",
                ReportRepository.mapCallableErrorCode("resource-exhausted")
        );
    }

    @Test
    public void mapCallableErrorCode_mapsNetworkAndUnknownSafely() {
        assertEquals(
                "Bağlantı kurulamadı. Lütfen tekrar deneyin.",
                ReportRepository.mapCallableErrorCode("deadline-exceeded")
        );
        assertEquals(
                "Rapor gönderilemedi. Lütfen tekrar deneyin.",
                ReportRepository.mapCallableErrorCode("some-new-code")
        );
    }

    @Test
    public void parseSubmitReportResult_readsSuccessFields() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("reportId", "quote123_user456");
        raw.put("status", "PENDING");
        raw.put("remainingDailyReports", 4);

        ReportRepository.ReportResult result = ReportRepository.parseSubmitReportResult(raw);

        assertEquals("quote123_user456", result.getReportId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(Long.valueOf(4L), result.getRemainingDailyReports());
    }

    @Test
    public void parseSubmitReportResult_handlesInvalidResultSafely() {
        ReportRepository.ReportResult result = ReportRepository.parseSubmitReportResult(null);

        assertEquals("", result.getReportId());
        assertEquals("", result.getStatus());
        assertNull(result.getRemainingDailyReports());
    }
}
