package com.merg.quoteapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_UNLOCKED = "UNLOCKED";
    public static final String FILTER_LOCKED = "LOCKED";
    public static final String FILTER_SOCIAL = "SOCIAL";
    public static final String FILTER_QUOTE = "QUOTE";
    public static final String FILTER_MODERATION = "MODERATION";
    public static final String FILTER_TYPE_MASTER = "TYPE_MASTER";

    private final List<Achievement> allAchievements = new ArrayList<>();
    private final List<Achievement> visibleAchievements = new ArrayList<>();
    private final Set<String> unlockedAchievementIds = new HashSet<>();
    private UserStats userStats;
    private String activeFilter = FILTER_ALL;

    public void submitData(List<Achievement> achievements,
                           List<UserAchievement> userAchievements,
                           UserStats stats) {
        allAchievements.clear();
        unlockedAchievementIds.clear();
        userStats = stats;

        if (achievements != null) {
            allAchievements.addAll(achievements);
        }
        if (userAchievements != null) {
            for (UserAchievement userAchievement : userAchievements) {
                if (userAchievement != null && userAchievement.getAchievementId() != null) {
                    unlockedAchievementIds.add(userAchievement.getAchievementId());
                }
            }
        }
        applyFilter();
    }

    public void setFilter(String filter) {
        activeFilter = filter == null ? FILTER_ALL : filter;
        applyFilter();
    }

    public int getVisibleCount() {
        return visibleAchievements.size();
    }

    private void applyFilter() {
        visibleAchievements.clear();
        for (Achievement achievement : allAchievements) {
            if (matchesFilter(achievement)) {
                visibleAchievements.add(achievement);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesFilter(Achievement achievement) {
        if (achievement == null) {
            return false;
        }
        boolean unlocked = isUnlocked(achievement);
        if (FILTER_UNLOCKED.equals(activeFilter)) {
            return unlocked;
        }
        if (FILTER_LOCKED.equals(activeFilter)) {
            return !unlocked;
        }
        if (FILTER_ALL.equals(activeFilter)) {
            return true;
        }
        return activeFilter.equals(achievement.getCategory());
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = visibleAchievements.get(position);
        holder.bind(achievement, isUnlocked(achievement), progressText(achievement));
    }

    @Override
    public int getItemCount() {
        return visibleAchievements.size();
    }

    private boolean isUnlocked(Achievement achievement) {
        return achievement != null
                && achievement.getAchievementId() != null
                && unlockedAchievementIds.contains(achievement.getAchievementId());
    }

    private String progressText(Achievement achievement) {
        if (achievement == null || userStats == null) {
            return "";
        }
        long current = metricValue(achievement.getMetric());
        long target = achievement.getTargetValue();
        if (target <= 0) {
            return "";
        }
        return Math.min(current, target) + " / " + target;
    }

    private long metricValue(String metric) {
        if (metric == null || userStats == null) {
            return 0;
        }
        switch (metric) {
            case "totalXp":
                return userStats.getTotalXp();
            case "totalQuotes":
                return userStats.getTotalQuotes();
            case "totalLikesReceived":
                return userStats.getTotalLikesReceived();
            case "maxSingleQuoteLikes":
            case "singleQuoteLikes":
                return userStats.getMaxSingleQuoteLikes();
            case "totalMovieQuotes":
                return userStats.getTotalMovieQuotes();
            case "totalSeriesQuotes":
                return userStats.getTotalSeriesQuotes();
            case "totalBookQuotes":
                return userStats.getTotalBookQuotes();
            case "validReports":
                return userStats.getValidReports();
            case "invalidReports":
                return userStats.getInvalidReports();
            case "unlockedAchievementCount":
                return userStats.getUnlockedAchievementCount();
            default:
                return 0;
        }
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView categoryText;
        private final TextView rewardText;
        private final TextView stateText;
        private final TextView progressText;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imageAchievementItemIcon);
            titleText = itemView.findViewById(R.id.textAchievementItemTitle);
            descriptionText = itemView.findViewById(R.id.textAchievementItemDescription);
            categoryText = itemView.findViewById(R.id.textAchievementItemCategory);
            rewardText = itemView.findViewById(R.id.textAchievementItemReward);
            stateText = itemView.findViewById(R.id.textAchievementItemState);
            progressText = itemView.findViewById(R.id.textAchievementItemProgress);
        }

        void bind(Achievement achievement, boolean unlocked, String progress) {
            titleText.setText(safe(achievement.getTitle(),
                    itemView.getContext().getString(R.string.achievement_default_title)));
            descriptionText.setText(safe(achievement.getDescription(), ""));
            categoryText.setText(categoryLabel(achievement.getCategory()));
            rewardText.setText(itemView.getContext().getString(
                    R.string.achievement_xp_reward_format, achievement.getXpReward()));
            stateText.setText(unlocked ? R.string.achievement_unlocked : R.string.achievement_locked);
            progressText.setText(itemView.getContext().getString(
                    R.string.achievement_progress_format, progress));
            progressText.setVisibility(progress == null || progress.isEmpty() ? View.GONE : View.VISIBLE);

            int iconColor = ContextCompat.getColor(itemView.getContext(), unlocked
                    ? R.color.quote_primary : R.color.quote_outline);
            iconView.setColorFilter(iconColor);
            stateText.setTextColor(iconColor);
        }

        private String categoryLabel(String category) {
            if (category == null) {
                return "";
            }
            switch (category.toUpperCase(Locale.ROOT)) {
                case "SOCIAL":
                    return itemView.getContext().getString(R.string.achievement_filter_social);
                case "QUOTE":
                    return itemView.getContext().getString(R.string.achievement_filter_quote);
                case "MODERATION":
                    return itemView.getContext().getString(R.string.achievement_filter_moderation);
                case "TYPE_MASTER":
                    return itemView.getContext().getString(R.string.achievement_filter_type);
                default:
                    return category;
            }
        }

        private String safe(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }
    }
}
