package com.merg.quoteapp.model;

public class AchievementFeedbackEvent {

    public enum Type {
        XP_GAINED,
        ACHIEVEMENT_UNLOCKED,
        LEVEL_UP
    }

    private final Type type;
    private final String title;
    private final String description;
    private final long xpAmount;
    private final int level;
    private final String levelTitle;
    private final String badgeName;

    private AchievementFeedbackEvent(Type type, String title, String description,
                                     long xpAmount, int level,
                                     String levelTitle, String badgeName) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.xpAmount = xpAmount;
        this.level = level;
        this.levelTitle = levelTitle;
        this.badgeName = badgeName;
    }

    public static AchievementFeedbackEvent xpGained(long xpAmount) {
        return new AchievementFeedbackEvent(Type.XP_GAINED, null, null,
                xpAmount, 0, null, null);
    }

    public static AchievementFeedbackEvent achievementUnlocked(Achievement achievement) {
        return new AchievementFeedbackEvent(
                Type.ACHIEVEMENT_UNLOCKED,
                achievement == null ? "" : achievement.getTitle(),
                achievement == null ? "" : achievement.getDescription(),
                achievement == null ? 0 : achievement.getXpReward(),
                0,
                null,
                null);
    }

    public static AchievementFeedbackEvent levelUp(Level level) {
        return new AchievementFeedbackEvent(
                Type.LEVEL_UP,
                null,
                null,
                0,
                level == null ? 1 : Math.max(1, level.getLevel()),
                level == null ? "" : level.getTitle(),
                level == null ? "" : level.getBadgeName());
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getXpAmount() {
        return xpAmount;
    }

    public int getLevel() {
        return level;
    }

    public String getLevelTitle() {
        return levelTitle;
    }

    public String getBadgeName() {
        return badgeName;
    }
}
