package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

public class UserAchievement {

    private String userAchievementId;
    private String userId;
    private String achievementId;
    private String achievementGroup;
    private int tier;
    private Timestamp unlockedAt;
    private long progressAtUnlock;
    private boolean xpRewardGranted;

    public UserAchievement() {
        // Required by Firestore.
    }

    public String getUserAchievementId() {
        return userAchievementId;
    }

    public void setUserAchievementId(String userAchievementId) {
        this.userAchievementId = userAchievementId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public String getAchievementGroup() {
        return achievementGroup;
    }

    public void setAchievementGroup(String achievementGroup) {
        this.achievementGroup = achievementGroup;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public Timestamp getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Timestamp unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public long getProgressAtUnlock() {
        return progressAtUnlock;
    }

    public void setProgressAtUnlock(long progressAtUnlock) {
        this.progressAtUnlock = progressAtUnlock;
    }

    public boolean isXpRewardGranted() {
        return xpRewardGranted;
    }

    public void setXpRewardGranted(boolean xpRewardGranted) {
        this.xpRewardGranted = xpRewardGranted;
    }
}
