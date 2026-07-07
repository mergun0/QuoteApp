package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

public class UserStats {

    private String userId;
    private long totalXp;
    private int level;
    private long totalQuotes;
    private long totalLikesReceived;
    private long maxSingleQuoteLikes;
    private long totalMovieQuotes;
    private long totalSeriesQuotes;
    private long totalBookQuotes;
    private long validReports;
    private long invalidReports;
    private long unlockedAchievementCount;
    private Timestamp lastUpdatedAt;

    public UserStats() {
        // Required by Firestore.
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(long totalXp) {
        this.totalXp = totalXp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getTotalQuotes() {
        return totalQuotes;
    }

    public void setTotalQuotes(long totalQuotes) {
        this.totalQuotes = totalQuotes;
    }

    public long getTotalLikesReceived() {
        return totalLikesReceived;
    }

    public void setTotalLikesReceived(long totalLikesReceived) {
        this.totalLikesReceived = totalLikesReceived;
    }

    public long getMaxSingleQuoteLikes() {
        return maxSingleQuoteLikes;
    }

    public void setMaxSingleQuoteLikes(long maxSingleQuoteLikes) {
        this.maxSingleQuoteLikes = maxSingleQuoteLikes;
    }

    public long getTotalMovieQuotes() {
        return totalMovieQuotes;
    }

    public void setTotalMovieQuotes(long totalMovieQuotes) {
        this.totalMovieQuotes = totalMovieQuotes;
    }

    public long getTotalSeriesQuotes() {
        return totalSeriesQuotes;
    }

    public void setTotalSeriesQuotes(long totalSeriesQuotes) {
        this.totalSeriesQuotes = totalSeriesQuotes;
    }

    public long getTotalBookQuotes() {
        return totalBookQuotes;
    }

    public void setTotalBookQuotes(long totalBookQuotes) {
        this.totalBookQuotes = totalBookQuotes;
    }

    public long getValidReports() {
        return validReports;
    }

    public void setValidReports(long validReports) {
        this.validReports = validReports;
    }

    public long getInvalidReports() {
        return invalidReports;
    }

    public void setInvalidReports(long invalidReports) {
        this.invalidReports = invalidReports;
    }

    public long getUnlockedAchievementCount() {
        return unlockedAchievementCount;
    }

    public void setUnlockedAchievementCount(long unlockedAchievementCount) {
        this.unlockedAchievementCount = unlockedAchievementCount;
    }

    public Timestamp getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Timestamp lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
