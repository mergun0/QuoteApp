package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.AchievementPreviewAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.repository.LikeRepository;
import com.merg.quoteapp.repository.UserStatsRepository;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.ProfileViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfileFragment extends Fragment {

    private TextView usernameText;
    private TextView emailText;
    private TextView avatarText;
    private TextView handleText;
    private TextView statusText;
    private TextView levelText;
    private TextView xpText;
    private TextView xpProgressText;
    private TextView achievementCountText;
    private TextView achievementEmptyText;
    private TextView nextLevelText;
    private TextView totalLikesText;
    private TextView totalQuotesText;
    private TextView achievementsStatText;
    private TextView savedQuotesText;
    private TextView lastAchievementTitleText;
    private TextView lastAchievementDescriptionText;
    private TextView lastAchievementMetaText;
    private TextView nextAchievementTitleText;
    private TextView nextAchievementDescriptionText;
    private TextView nextAchievementProgressText;
    private ProgressBar progressBar;
    private ProgressBar xpProgressBar;
    private ProgressBar nextAchievementProgressBar;
    private RecyclerView achievementRecyclerView;
    private AchievementPreviewAdapter achievementAdapter;
    private UserStatsViewModel userStatsViewModel;
    private AchievementViewModel achievementViewModel;
    private LevelViewModel levelViewModel;
    private FavoriteViewModel favoriteViewModel;
    private UserStats currentStats;
    private List<Achievement> currentAchievements = new ArrayList<>();
    private List<UserAchievement> currentUserAchievements = new ArrayList<>();
    private String currentUserId;
    private boolean profileAchievementDataLoaded;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        avatarText = view.findViewById(R.id.textProfileAvatar);
        usernameText = view.findViewById(R.id.textProfileUsername);
        emailText = view.findViewById(R.id.textProfileEmail);
        handleText = view.findViewById(R.id.textProfileHandle);
        statusText = view.findViewById(R.id.textProfileStatus);
        progressBar = view.findViewById(R.id.progressProfile);
        levelText = view.findViewById(R.id.textProfileLevel);
        xpText = view.findViewById(R.id.textProfileXp);
        xpProgressText = view.findViewById(R.id.textProfileXpProgress);
        nextLevelText = view.findViewById(R.id.textProfileNextLevel);
        achievementCountText = view.findViewById(R.id.textProfileAchievementCount);
        achievementEmptyText = view.findViewById(R.id.textProfileAchievementEmpty);
        totalLikesText = view.findViewById(R.id.textProfileLikesValue);
        totalQuotesText = view.findViewById(R.id.textProfileQuotesValue);
        achievementsStatText = view.findViewById(R.id.textProfileAchievementsStatValue);
        savedQuotesText = view.findViewById(R.id.textProfileSavedValue);
        lastAchievementTitleText = view.findViewById(R.id.textProfileLastAchievementTitle);
        lastAchievementDescriptionText = view.findViewById(R.id.textProfileLastAchievementDescription);
        lastAchievementMetaText = view.findViewById(R.id.textProfileLastAchievementMeta);
        nextAchievementTitleText = view.findViewById(R.id.textProfileNextAchievementTitle);
        nextAchievementDescriptionText = view.findViewById(R.id.textProfileNextAchievementDescription);
        nextAchievementProgressText = view.findViewById(R.id.textProfileNextAchievementProgress);
        xpProgressBar = view.findViewById(R.id.progressProfileXp);
        nextAchievementProgressBar = view.findViewById(R.id.progressProfileNextAchievement);
        achievementRecyclerView = view.findViewById(R.id.recyclerProfileAchievements);
        userStatsViewModel = new ViewModelProvider(this).get(UserStatsViewModel.class);
        achievementViewModel = new ViewModelProvider(this).get(AchievementViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        setupAchievementPreview();
        setupDashboardClicks(view);

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfile().observe(getViewLifecycleOwner(), stats ->
                renderProfile(view, stats));
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.loadProfile();

        observeAchievementState();
        loadAchievementState();
        observeSavedQuotes();
        loadSavedQuotes();

        view.findViewById(R.id.buttonLogout).setOnClickListener(button -> logout());
        view.findViewById(R.id.buttonProfileAllAchievements)
                .setOnClickListener(button -> openAchievements());
    }

    private void setupDashboardClicks(View view) {
        view.findViewById(R.id.cardProfileAchievementsStat)
                .setOnClickListener(card -> openAchievements());
        view.findViewById(R.id.cardProfileSaved)
                .setOnClickListener(card -> openFavorites());
        view.findViewById(R.id.cardProfileLastAchievement)
                .setOnClickListener(card -> openAchievements());
        view.findViewById(R.id.cardProfileNextAchievement)
                .setOnClickListener(card -> openAchievements());
        setupMenuRow(view.findViewById(R.id.menuProfileSettings),
                "⚙️", getString(R.string.profile_menu_settings), menu -> openSettings());
        setupMenuRow(view.findViewById(R.id.menuProfileAbout),
                "ℹ️", getString(R.string.profile_menu_about), menu -> openAbout());
        setupMenuRow(view.findViewById(R.id.buttonLogout),
                "🚪", getString(R.string.logout), menu -> logout());
    }

    private void setupMenuRow(View row, String icon, String title, View.OnClickListener listener) {
        if (row == null) {
            return;
        }
        TextView iconText = row.findViewById(R.id.textProfileMenuIcon);
        TextView titleText = row.findViewById(R.id.textProfileMenuTitle);
        if (iconText != null) {
            iconText.setText(icon);
        }
        if (titleText != null) {
            titleText.setText(title);
        }
        row.setOnClickListener(listener);
    }

    private void renderProfile(View root, ProfileStats stats) {
        if (stats == null) {
            return;
        }
        String username = safeDisplayName(stats.getUsername(), stats.getEmail());
        usernameText.setText(username);
        handleText.setText(getString(R.string.profile_handle_format, safeHandle(username)));
        avatarText.setText(firstLetter(username));
        emailText.setText(stats.getEmail());
        totalQuotesText.setText(String.valueOf(stats.getTotalQuotes()));
        totalLikesText.setText(String.valueOf(stats.getTotalLikes()));
    }

    private void renderState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (state.getStatus() == QuoteState.Status.ERROR) {
            statusText.setText(state.getMessage());
            statusText.setVisibility(View.VISIBLE);
        } else {
            statusText.setVisibility(View.GONE);
        }
    }

    private void setupAchievementPreview() {
        achievementAdapter = new AchievementPreviewAdapter();
        achievementRecyclerView.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        achievementRecyclerView.setAdapter(achievementAdapter);
        renderDefaultAchievementState();
    }

    private void observeAchievementState() {
        if (userStatsViewModel == null || achievementViewModel == null) {
            return;
        }
        userStatsViewModel.getUserStats().observe(getViewLifecycleOwner(), stats -> {
            currentStats = stats == null ? defaultStats() : stats;
            renderUserStats();
            renderAchievementFocusCards();
            if (levelViewModel != null) {
                levelViewModel.loadLevelProgress(currentStats.getTotalXp());
            }
        });
        userStatsViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                renderDefaultAchievementState();
            }
        });
        userStatsViewModel.getReconciliationComplete().observe(getViewLifecycleOwner(), complete -> {
            if (Boolean.TRUE.equals(complete) && !profileAchievementDataLoaded
                    && currentUserId != null) {
                loadProfileAchievementData(currentUserId);
            }
        });
        if (levelViewModel != null) {
            levelViewModel.getCurrentLevel().observe(getViewLifecycleOwner(), level -> renderUserStats());
            levelViewModel.getNextLevel().observe(getViewLifecycleOwner(), level -> renderUserStats());
        }
        achievementViewModel.getActiveAchievements().observe(getViewLifecycleOwner(), achievements -> {
            currentAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            renderAchievementFocusCards();
        });
        achievementViewModel.getUserAchievements().observe(getViewLifecycleOwner(), achievements -> {
            currentUserAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            renderUserStats();
            renderAchievementFocusCards();
        });
        achievementViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && currentAchievements.isEmpty()) {
                renderAchievementPreview();
            }
        });
    }

    private void observeSavedQuotes() {
        if (favoriteViewModel == null || savedQuotesText == null) {
            return;
        }
        favoriteViewModel.getSavedQuotes().observe(getViewLifecycleOwner(), quotes -> {
            int count = quotes == null ? 0 : quotes.size();
            savedQuotesText.setText(String.valueOf(count));
        });
    }

    private void loadAchievementState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            renderDefaultAchievementState();
            return;
        }
        if (userStatsViewModel == null || achievementViewModel == null) {
            renderDefaultAchievementState();
            return;
        }
        currentUserId = user.getUid();
        profileAchievementDataLoaded = false;
        userStatsViewModel.reconcileExistingStatsAndAchievements(currentUserId);
    }

    private void loadProfileAchievementData(String userId) {
        profileAchievementDataLoaded = true;
        userStatsViewModel.loadUserStats(userId);
        achievementViewModel.loadActiveAchievements();
        achievementViewModel.loadUserAchievements(userId);
    }

    private void loadSavedQuotes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || favoriteViewModel == null) {
            if (savedQuotesText != null) {
                savedQuotesText.setText("0");
            }
            return;
        }
        favoriteViewModel.loadSavedQuotes();
    }

    private void renderDefaultAchievementState() {
        currentStats = defaultStats();
        renderUserStats();
        renderAchievementPreview();
        renderAchievementFocusCards();
    }

    private void renderUserStats() {
        if (levelText == null || xpText == null || achievementCountText == null
                || xpProgressBar == null || xpProgressText == null) {
            return;
        }
        UserStats stats = currentStats == null ? defaultStats() : currentStats;
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        long totalXp = Math.max(0, stats.getTotalXp());
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                currentUserAchievements == null ? 0 : currentUserAchievements.size());

        levelText.setText(getString(R.string.profile_level_with_icon_format, level));
        achievementCountText.setText(getString(
                R.string.profile_achievement_count_format, unlockedCount));
        if (achievementsStatText != null) {
            achievementsStatText.setText(String.valueOf(unlockedCount));
        }

        if (levelViewModel == null || levelViewModel.getCurrentLevel() == null
                || levelViewModel.getNextLevel() == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }

        Level currentLevel = levelViewModel.getCurrentLevel().getValue();
        Level nextLevel = levelViewModel.getNextLevel().getValue();
        if (currentLevel == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }
        long currentRequiredXp = Math.max(0, currentLevel.getRequiredTotalXp());
        if (nextLevel == null) {
            xpProgressBar.setProgress(100);
            xpText.setText(getString(R.string.xp_total_format, totalXp));
            xpProgressText.setText(R.string.xp_progress_max);
            if (nextLevelText != null) {
                nextLevelText.setText(R.string.xp_progress_max);
            }
            return;
        }
        long nextRequiredXp = Math.max(currentRequiredXp + 1, nextLevel.getRequiredTotalXp());
        long range = Math.max(1, nextRequiredXp - currentRequiredXp);
        long progress = Math.max(0, totalXp - currentRequiredXp);
        xpProgressBar.setProgress((int) Math.min(100, (progress * 100) / range));
        xpText.setText(getString(R.string.xp_progress_format, totalXp, nextRequiredXp));
        xpProgressText.setText(getString(R.string.xp_remaining_format,
                Math.max(0, nextRequiredXp - totalXp)));
        if (nextLevelText != null) {
            nextLevelText.setText(getString(R.string.next_level_format, nextLevel.getLevel()));
        }
    }

    private void renderFallbackXpProgress(long totalXp) {
        long safeXp = Math.max(0, totalXp);
        xpProgressBar.setProgress(0);
        xpText.setText(getString(R.string.xp_total_format, safeXp));
        xpProgressText.setText(getString(R.string.xp_total_format, safeXp));
        if (nextLevelText != null) {
            nextLevelText.setText(getString(R.string.next_level_format, 2));
        }
    }

    private void renderAchievementPreview() {
        if (achievementAdapter == null || achievementRecyclerView == null
                || achievementEmptyText == null) {
            return;
        }
        achievementAdapter.submitData(currentAchievements, currentUserAchievements);
        boolean empty = currentAchievements == null || currentAchievements.isEmpty();
        achievementRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        achievementEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderAchievementFocusCards() {
        renderLastAchievementCard();
        renderNextAchievementCard();
    }

    private void renderLastAchievementCard() {
        if (lastAchievementTitleText == null || lastAchievementDescriptionText == null
                || lastAchievementMetaText == null) {
            return;
        }
        UserAchievement latest = findLatestUnlockedAchievement();
        if (latest == null) {
            lastAchievementTitleText.setText(R.string.profile_no_last_achievement_title);
            lastAchievementDescriptionText.setText(R.string.profile_no_last_achievement_description);
            lastAchievementMetaText.setVisibility(View.GONE);
            return;
        }
        Achievement achievement = findAchievement(latest.getAchievementId());
        if (achievement == null) {
            lastAchievementTitleText.setText(R.string.achievement_default_title);
            lastAchievementDescriptionText.setText(R.string.achievement_unlocked);
        } else {
            lastAchievementTitleText.setText(safeText(achievement.getTitle(),
                    getString(R.string.achievement_default_title)));
            lastAchievementDescriptionText.setText(safeText(achievement.getDescription(),
                    getString(R.string.achievement_unlocked)));
        }
        String meta = buildAchievementMeta(achievement, latest.getUnlockedAt());
        if (isBlank(meta)) {
            lastAchievementMetaText.setVisibility(View.GONE);
        } else {
            lastAchievementMetaText.setText(meta);
            lastAchievementMetaText.setVisibility(View.VISIBLE);
        }
    }

    private void renderNextAchievementCard() {
        if (nextAchievementTitleText == null || nextAchievementDescriptionText == null
                || nextAchievementProgressText == null || nextAchievementProgressBar == null) {
            return;
        }
        NextAchievement next = findNextAchievement();
        if (next == null) {
            nextAchievementTitleText.setText(R.string.profile_next_achievement_fallback);
            nextAchievementDescriptionText.setText(R.string.profile_next_achievement_fallback_detail);
            nextAchievementProgressText.setText(R.string.profile_progress_unknown);
            nextAchievementProgressBar.setVisibility(View.GONE);
            return;
        }
        nextAchievementTitleText.setText(safeText(next.achievement.getTitle(),
                getString(R.string.achievement_default_title)));
        nextAchievementDescriptionText.setText(safeText(next.achievement.getDescription(),
                getString(R.string.profile_next_achievement_fallback_detail)));
        nextAchievementProgressText.setText(getString(R.string.profile_achievement_progress_format,
                Math.min(next.currentValue, next.targetValue), next.targetValue));
        nextAchievementProgressBar.setVisibility(View.VISIBLE);
        nextAchievementProgressBar.setProgress((int) Math.min(100,
                (Math.max(0, next.currentValue) * 100) / Math.max(1, next.targetValue)));
    }

    private UserAchievement findLatestUnlockedAchievement() {
        if (currentUserAchievements == null || currentUserAchievements.isEmpty()) {
            return null;
        }
        UserAchievement latest = null;
        for (UserAchievement achievement : currentUserAchievements) {
            if (achievement == null) {
                continue;
            }
            if (latest == null || timestampMillis(achievement.getUnlockedAt())
                    > timestampMillis(latest.getUnlockedAt())) {
                latest = achievement;
            }
        }
        return latest;
    }

    private NextAchievement findNextAchievement() {
        if (currentAchievements == null || currentAchievements.isEmpty()) {
            return null;
        }
        Set<String> unlockedIds = new HashSet<>();
        if (currentUserAchievements != null) {
            for (UserAchievement userAchievement : currentUserAchievements) {
                if (userAchievement != null && !isBlank(userAchievement.getAchievementId())) {
                    unlockedIds.add(userAchievement.getAchievementId());
                }
            }
        }
        NextAchievement best = null;
        for (Achievement achievement : currentAchievements) {
            if (achievement == null || isBlank(achievement.getAchievementId())
                    || unlockedIds.contains(achievement.getAchievementId())) {
                continue;
            }
            long target = achievement.getTargetValue();
            Long current = getMetricValue(achievement.getMetric());
            if (current == null || target <= 0) {
                continue;
            }
            NextAchievement candidate = new NextAchievement(achievement, Math.max(0, current), target);
            if (best == null || candidate.progressRatio() > best.progressRatio()
                    || (candidate.progressRatio() == best.progressRatio()
                    && achievement.getSortOrder() < best.achievement.getSortOrder())) {
                best = candidate;
            }
        }
        return best;
    }

    private Achievement findAchievement(String achievementId) {
        if (isBlank(achievementId) || currentAchievements == null) {
            return null;
        }
        for (Achievement achievement : currentAchievements) {
            if (achievement != null && achievementId.equals(achievement.getAchievementId())) {
                return achievement;
            }
        }
        return null;
    }

    private Long getMetricValue(String metric) {
        UserStats stats = currentStats == null ? defaultStats() : currentStats;
        if (isBlank(metric)) {
            return null;
        }
        String normalized = metric.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "totalxp":
                return stats.getTotalXp();
            case "level":
                return (long) Math.max(1, stats.getLevel());
            case "totalquotes":
                return stats.getTotalQuotes();
            case "totallikesreceived":
                return stats.getTotalLikesReceived();
            case "maxsinglequotelikes":
            case "singlequotelikes":
                return stats.getMaxSingleQuoteLikes();
            case "totalmoviequotes":
                return stats.getTotalMovieQuotes();
            case "totalseriesquotes":
                return stats.getTotalSeriesQuotes();
            case "totalbookquotes":
                return stats.getTotalBookQuotes();
            case "validreports":
                return stats.getValidReports();
            case "invalidreports":
                return stats.getInvalidReports();
            case "unlockedachievementcount":
                return stats.getUnlockedAchievementCount();
            default:
                return null;
        }
    }

    private String buildAchievementMeta(Achievement achievement, Timestamp unlockedAt) {
        List<String> parts = new ArrayList<>();
        if (achievement != null && achievement.getXpReward() > 0) {
            parts.add(getString(R.string.achievement_xp_reward_format, achievement.getXpReward()));
        }
        String date = formatDate(unlockedAt);
        if (!isBlank(date)) {
            parts.add(date);
        }
        return joinParts(parts);
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
    }

    private long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    private String joinParts(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (isBlank(part)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }

    private void openAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), AchievementsActivity.class);
        intent.putExtra(AchievementsActivity.EXTRA_USER_ID, user.getUid());
        startActivity(intent);
    }

    private void openFavorites() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFavoritesTab();
        }
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), SettingsActivity.class));
    }

    private void openAbout() {
        startActivity(new Intent(requireContext(), AboutActivity.class));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LikeRepository.clearMemoryCache();
        UserStatsRepository.clearMemoryCache();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String safeDisplayName(String username, String email) {
        if (!isBlank(username)) {
            return username.trim();
        }
        if (!isBlank(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return getString(R.string.username);
    }

    private String safeHandle(String username) {
        String cleaned = username == null ? "" : username.trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1);
        }
        cleaned = cleaned.replaceAll("\\s+", "").toLowerCase();
        return cleaned.isEmpty() ? "kullanici" : cleaned;
    }

    private String firstLetter(String value) {
        if (isBlank(value)) {
            return "K";
        }
        return value.trim().substring(0, 1).toUpperCase();
    }

    private String safeText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class NextAchievement {
        private final Achievement achievement;
        private final long currentValue;
        private final long targetValue;

        private NextAchievement(Achievement achievement, long currentValue, long targetValue) {
            this.achievement = achievement;
            this.currentValue = currentValue;
            this.targetValue = targetValue;
        }

        private long progressRatio() {
            return (Math.max(0, currentValue) * 1000) / Math.max(1, targetValue);
        }
    }
}
