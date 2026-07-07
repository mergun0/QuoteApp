package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class Level {

    private int level;
    private long requiredTotalXp;
    private String title;
    private String badgeName;
    private List<String> unlockedFeatures = new ArrayList<>();
    private Timestamp createdAt;

    public Level() {
        // Required by Firestore.
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getRequiredTotalXp() {
        return requiredTotalXp;
    }

    public void setRequiredTotalXp(long requiredTotalXp) {
        this.requiredTotalXp = requiredTotalXp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public List<String> getUnlockedFeatures() {
        return unlockedFeatures;
    }

    public void setUnlockedFeatures(List<String> unlockedFeatures) {
        this.unlockedFeatures = unlockedFeatures == null ? new ArrayList<>() : unlockedFeatures;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
