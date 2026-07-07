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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AchievementPreviewAdapter
        extends RecyclerView.Adapter<AchievementPreviewAdapter.AchievementViewHolder> {

    private static final int MAX_PREVIEW_COUNT = 5;

    private final List<Achievement> achievements = new ArrayList<>();
    private final Set<String> unlockedAchievementIds = new HashSet<>();

    public void submitData(List<Achievement> achievementList,
                           List<UserAchievement> userAchievementList) {
        achievements.clear();
        unlockedAchievementIds.clear();

        if (userAchievementList != null) {
            for (UserAchievement userAchievement : userAchievementList) {
                if (userAchievement != null && userAchievement.getAchievementId() != null) {
                    unlockedAchievementIds.add(userAchievement.getAchievementId());
                }
            }
        }

        if (achievementList != null) {
            for (Achievement achievement : achievementList) {
                if (achievement != null && achievements.size() < MAX_PREVIEW_COUNT) {
                    achievements.add(achievement);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement_preview, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);
        boolean unlocked = unlockedAchievementIds.contains(achievement.getAchievementId());
        holder.bind(achievement, unlocked);
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView titleText;
        private final TextView stateText;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imageAchievementIcon);
            titleText = itemView.findViewById(R.id.textAchievementTitle);
            stateText = itemView.findViewById(R.id.textAchievementState);
        }

        void bind(Achievement achievement, boolean unlocked) {
            titleText.setText(isBlank(achievement.getTitle())
                    ? itemView.getContext().getString(R.string.achievement_default_title)
                    : achievement.getTitle());
            stateText.setText(unlocked
                    ? R.string.achievement_unlocked
                    : R.string.achievement_locked);
            int color = ContextCompat.getColor(itemView.getContext(), unlocked
                    ? R.color.quote_primary : R.color.quote_outline);
            iconView.setColorFilter(color);
            titleText.setTextColor(ContextCompat.getColor(itemView.getContext(), unlocked
                    ? R.color.quote_text_primary : R.color.quote_text_secondary));
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
