package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Achievement {

    private String achievementId;
    private String title;
    private String description;
    private String category;
    private String ruleType;
    private String targetScope;
    private String metric;
    private String operator;
    private long targetValue;
    private String achievementGroup;
    private int tier;
    private int xpReward;
    private String iconName;
    private String level;
    private boolean active;
    private int sortOrder;
    private Timestamp createdAt;

    public Achievement() {
        // Required by Firestore.
    }

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(String targetScope) {
        this.targetScope = targetScope;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public long getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(long targetValue) {
        this.targetValue = targetValue;
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

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return active;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
