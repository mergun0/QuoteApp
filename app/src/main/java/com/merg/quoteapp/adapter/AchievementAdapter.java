package com.merg.quoteapp.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private final Map<String, UserAchievement> unlockedAchievementMap = new HashMap<>();
    private UserStats userStats;
    private String activeFilter = FILTER_ALL;

    public void submitData(List<Achievement> achievements,
                           List<UserAchievement> userAchievements,
                           UserStats stats) {
        allAchievements.clear();
        unlockedAchievementIds.clear();
        unlockedAchievementMap.clear();
        userStats = stats;

        if (achievements != null) {
            allAchievements.addAll(achievements);
        }
        if (userAchievements != null) {
            for (UserAchievement userAchievement : userAchievements) {
                if (userAchievement != null && userAchievement.getAchievementId() != null) {
                    unlockedAchievementIds.add(userAchievement.getAchievementId());
                    unlockedAchievementMap.put(userAchievement.getAchievementId(), userAchievement);
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
        holder.bind(achievement, isUnlocked(achievement), progressInfo(achievement),
                unlockedAchievementMap.get(achievement.getAchievementId()));
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

    private ProgressInfo progressInfo(Achievement achievement) {
        if (achievement == null || userStats == null) {
            return ProgressInfo.empty();
        }
        long current = metricValue(achievement.getMetric());
        long target = achievement.getTargetValue();
        if (target <= 0) {
            return ProgressInfo.empty();
        }
        return new ProgressInfo(Math.max(0, current), target);
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

        private final MaterialCardView cardView;
        private final ImageView iconView;
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView categoryText;
        private final TextView tierText;
        private final TextView rewardText;
        private final TextView stateText;
        private final TextView progressText;
        private final TextView unlockDateText;
        private final ProgressBar progressBar;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardAchievementItem);
            iconView = itemView.findViewById(R.id.imageAchievementItemIcon);
            titleText = itemView.findViewById(R.id.textAchievementItemTitle);
            descriptionText = itemView.findViewById(R.id.textAchievementItemDescription);
            categoryText = itemView.findViewById(R.id.textAchievementItemCategory);
            tierText = itemView.findViewById(R.id.textAchievementItemTier);
            rewardText = itemView.findViewById(R.id.textAchievementItemReward);
            stateText = itemView.findViewById(R.id.textAchievementItemState);
            progressText = itemView.findViewById(R.id.textAchievementItemProgress);
            unlockDateText = itemView.findViewById(R.id.textAchievementItemUnlockDate);
            progressBar = itemView.findViewById(R.id.progressAchievementItem);
        }

        void bind(Achievement achievement, boolean unlocked, ProgressInfo progress,
                  UserAchievement userAchievement) {
            titleText.setText(safe(achievement.getTitle(),
                    itemView.getContext().getString(R.string.achievement_default_title)));
            descriptionText.setText(safe(achievement.getDescription(), ""));
            categoryText.setText(categoryLabel(achievement.getCategory()));
            rewardText.setText(itemView.getContext().getString(
                    R.string.achievement_xp_reward_format, achievement.getXpReward()));
            tierText.setText(safe(achievement.getLevel(), ""));
            tierText.setVisibility(safe(achievement.getLevel(), "").isEmpty() ? View.GONE : View.VISIBLE);

            int tierColor = tierColor(achievement.getLevel());
            int primaryTextColor = ContextCompat.getColor(itemView.getContext(), R.color.home_v2_text_primary);
            int secondaryTextColor = ContextCompat.getColor(itemView.getContext(), R.color.home_v2_text_secondary);
            int dividerColor = ContextCompat.getColor(itemView.getContext(), R.color.home_v2_divider);
            int cardColor = ContextCompat.getColor(itemView.getContext(), R.color.home_v2_card);
            int lockedIconColor = ContextCompat.getColor(itemView.getContext(), R.color.home_v2_text_secondary);
            int iconColor = unlocked ? tierColor : lockedIconColor;

            cardView.setCardBackgroundColor(cardColor);
            cardView.setStrokeColor(unlocked ? tierColor : dividerColor);
            cardView.setStrokeWidth(unlocked ? 2 : 1);
            titleText.setTextColor(primaryTextColor);
            descriptionText.setTextColor(secondaryTextColor);
            tierText.setTextColor(tierColor);
            iconView.setColorFilter(iconColor);
            iconView.setAlpha(unlocked ? 1f : 0.82f);
            stateText.setText(unlocked ? "✓ " + itemView.getContext().getString(R.string.achievement_unlocked)
                    : "🔒 " + itemView.getContext().getString(R.string.achievement_locked));
            stateText.setTextColor(iconColor);
            rewardText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.home_v2_accent));

            if (progress.isAvailable()) {
                long shownCurrent = Math.min(progress.current, progress.target);
                String base = shownCurrent + " / " + progress.target + " " + metricLabel(achievement.getMetric());
                if (!unlocked && progress.target > progress.current) {
                    progressText.setText(itemView.getContext().getString(
                            R.string.achievement_progress_remaining_format,
                            base, progress.target - progress.current));
                } else {
                    progressText.setText(itemView.getContext().getString(
                            R.string.achievement_progress_format, base));
                }
                progressText.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(unlocked ? 100
                        : (int) Math.min(100, (shownCurrent * 100) / Math.max(1, progress.target)));
                progressBar.setProgressTintList(ColorStateList.valueOf(tierColor));
                progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(dividerColor));
            } else {
                progressText.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            if (unlocked && userAchievement != null && userAchievement.getUnlockedAt() != null) {
                unlockDateText.setText(itemView.getContext().getString(
                        R.string.achievement_unlocked_date_format,
                        formatDate(userAchievement.getUnlockedAt().toDate())));
                unlockDateText.setVisibility(View.VISIBLE);
            } else {
                unlockDateText.setVisibility(View.GONE);
            }
        }

        private int tierColor(String level) {
            if (level == null) {
                return ContextCompat.getColor(itemView.getContext(), R.color.home_v2_primary);
            }
            switch (level.toUpperCase(Locale.ROOT)) {
                case "BRONZE":
                    return ContextCompat.getColor(itemView.getContext(), R.color.achievement_bronze);
                case "SILVER":
                    return ContextCompat.getColor(itemView.getContext(), R.color.achievement_silver);
                case "GOLD":
                    return ContextCompat.getColor(itemView.getContext(), R.color.achievement_gold);
                case "DIAMOND":
                    return ContextCompat.getColor(itemView.getContext(), R.color.achievement_diamond);
                case "LEGENDARY":
                    return ContextCompat.getColor(itemView.getContext(), R.color.achievement_legendary);
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.home_v2_primary);
            }
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

        private String metricLabel(String metric) {
            if (metric == null) {
                return "";
            }
            switch (metric) {
                case "totalLikesReceived":
                case "maxSingleQuoteLikes":
                case "singleQuoteLikes":
                    return "Beğeni";
                case "totalQuotes":
                case "totalMovieQuotes":
                case "totalSeriesQuotes":
                case "totalBookQuotes":
                    return "Alıntı";
                case "validReports":
                    return "Geçerli rapor";
                case "unlockedAchievementCount":
                    return "Başarım";
                case "totalXp":
                    return "XP";
                default:
                    return "";
            }
        }

        private String formatDate(Date date) {
            return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
        }

        private String safe(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }
    }

    private static class ProgressInfo {
        private final long current;
        private final long target;

        private ProgressInfo(long current, long target) {
            this.current = current;
            this.target = target;
        }

        private static ProgressInfo empty() {
            return new ProgressInfo(0, 0);
        }

        private boolean isAvailable() {
            return target > 0;
        }
    }
}
