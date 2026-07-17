package com.merg.quoteapp.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.merg.quoteapp.model.Report;

import org.junit.Test;

import java.util.Date;
import java.util.Map;

public class ReportRepositoryTest {

    @Test
    public void reportDocumentId_usesQuoteIdAndReporterUid() {
        assertEquals("quote123_user456",
                ReportRepository.reportDocumentId(" quote123 ", " user456 "));
    }

    @Test
    public void buildDirectReportData_containsOnlyAllowedClientFields() {
        Date createdAt = new Date();

        Map<String, Object> data = ReportRepository.buildDirectReportData(
                " quote123 ",
                " owner123 ",
                " reporter456 ",
                " SPAM ",
                " optional description ",
                createdAt
        );

        assertEquals(10, data.size());
        assertEquals("quote123_reporter456", data.get("reportId"));
        assertEquals("quote123", data.get("quoteId"));
        assertEquals("owner123", data.get("reportedUserId"));
        assertEquals("reporter456", data.get("reporterUserId"));
        assertEquals("SPAM", data.get("reason"));
        assertEquals("optional description", data.get("description"));
        assertEquals(Report.STATUS_PENDING, data.get("status"));
        assertEquals(createdAt, data.get("createdAt"));
        assertNull(data.get("reviewedAt"));
        assertNull(data.get("reviewedBy"));
        assertNull(data.get("isValidReport"));
        assertFalse(data.containsKey("role"));
        assertFalse(data.containsKey("counter"));
    }

    @Test
    public void buildDirectReportData_normalizesNullDescriptionToEmptyString() {
        Map<String, Object> data = ReportRepository.buildDirectReportData(
                "quote123",
                "owner123",
                "reporter456",
                "OTHER",
                null,
                new Date()
        );

        assertEquals("", data.get("description"));
    }
}
