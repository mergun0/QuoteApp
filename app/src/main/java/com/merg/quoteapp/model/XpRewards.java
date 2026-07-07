package com.merg.quoteapp.model;

public class XpRewards {

    public static final String EVENT_CREATE_QUOTE = "createQuote";
    public static final String EVENT_RECEIVE_LIKE = "receiveLike";
    public static final String EVENT_VALID_REPORT = "validReport";

    private long createQuote;
    private long receiveLike;
    private long validReport;

    public XpRewards() {
        // Required by Firestore.
    }

    public long getCreateQuote() {
        return createQuote;
    }

    public void setCreateQuote(long createQuote) {
        this.createQuote = createQuote;
    }

    public long getReceiveLike() {
        return receiveLike;
    }

    public void setReceiveLike(long receiveLike) {
        this.receiveLike = receiveLike;
    }

    public long getValidReport() {
        return validReport;
    }

    public void setValidReport(long validReport) {
        this.validReport = validReport;
    }

    public long valueFor(String eventType) {
        if (EVENT_CREATE_QUOTE.equals(eventType)) {
            return createQuote;
        }
        if (EVENT_RECEIVE_LIKE.equals(eventType)) {
            return receiveLike;
        }
        if (EVENT_VALID_REPORT.equals(eventType)) {
            return validReport;
        }
        return 0;
    }

    public static XpRewards safeDefaults() {
        XpRewards rewards = new XpRewards();
        rewards.setCreateQuote(10);
        rewards.setReceiveLike(2);
        rewards.setValidReport(20);
        return rewards;
    }
}
