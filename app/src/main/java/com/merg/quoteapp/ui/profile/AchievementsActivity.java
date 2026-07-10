package com.merg.quoteapp.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.AchievementAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_SHOW_ONLY_UNLOCKED = "showOnlyUnlocked";

    private AchievementViewModel achievementViewModel;
    private UserStatsViewModel userStatsViewModel;
    private LevelViewModel levelViewModel;
    private AchievementAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorText;
    private TextView emptyText;
    private TextView emptySubtitleText;
    private View emptyLayout;
    private TextView levelText;
    private TextView levelSubtitleText;
    private TextView xpText;
    private TextView xpRemainingText;
    private TextView unlockedCountText;
    private LinearProgressIndicator xpProgressBar;
    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private String userId;
    private boolean showOnlyUnlocked;
    private List<Achievement> achievements = new ArrayList<>();
    private List<UserAchievement> userAchievements = new ArrayList<>();
    private UserStats userStats;
    private Level currentLevel;
    private Level nextLevel;
    private boolean achievementDataLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        showOnlyUnlocked = getIntent().getBooleanExtra(EXTRA_SHOW_ONLY_UNLOCKED, false);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupViewModels();
        loadData();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressAchievements);
        errorText = findViewById(R.id.textAchievementsError);
        emptyLayout = findViewById(R.id.layoutAchievementsEmpty);
        emptyText = findViewById(R.id.textAchievementsEmpty);
        emptySubtitleText = findViewById(R.id.textAchievementsEmptySubtitle);
        levelText = findViewById(R.id.textAchievementsLevel);
        levelSubtitleText = findViewById(R.id.textAchievementsLevelSubtitle);
        xpText = findViewById(R.id.textAchievementsXp);
        xpRemainingText = findViewById(R.id.textAchievementsXpRemaining);
        unlockedCountText = findViewById(R.id.textAchievementsUnlockedCount);
        xpProgressBar = findViewById(R.id.progressAchievementsXp);
        recyclerView = findViewById(R.id.recyclerAchievements);
        chipGroup = findViewById(R.id.chipGroupAchievementFilters);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarAchievements);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AchievementAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                adapter.setFilter(AchievementAdapter.FILTER_ALL);
                updateEmptyState();
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAchievementUnlocked) {
                adapter.setFilter(AchievementAdapter.FILTER_UNLOCKED);
            } else if (checkedId == R.id.chipAchievementLocked) {
                adapter.setFilter(AchievementAdapter.FILTER_LOCKED);
            } else if (checkedId == R.id.chipAchievementSocial) {
                adapter.setFilter(AchievementAdapter.FILTER_SOCIAL);
            } else if (checkedId == R.id.chipAchievementQuote) {
                adapter.setFilter(AchievementAdapter.FILTER_QUOTE);
            } else if (checkedId == R.id.chipAchievementModeration) {
                adapter.setFilter(AchievementAdapter.FILTER_MODERATION);
            } else if (checkedId == R.id.chipAchievementType) {
                adapter.setFilter(AchievementAdapter.FILTER_TYPE_MASTER);
            } else {
                adapter.setFilter(AchievementAdapter.FILTER_ALL);
            }
            updateEmptyState();
        });
        if (showOnlyUnlocked) {
            chipGroup.check(R.id.chipAchievementUnlocked);
            adapter.setFilter(AchievementAdapter.FILTER_UNLOCKED);
        }
    }

    private void setupViewModels() {
        achievementViewModel = new ViewModelProvider(this).get(AchievementViewModel.class);
        userStatsViewModel = new ViewModelProvider(this).get(UserStatsViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);

        achievementViewModel.getActiveAchievements().observe(this, result -> {
            achievements = result == null ? new ArrayList<>() : result;
            renderStats();
            renderAchievements();
        });
        achievementViewModel.getUserAchievements().observe(this, result -> {
            userAchievements = result == null ? new ArrayList<>() : result;
            renderAchievements();
        });
        achievementViewModel.getLoading().observe(this, this::renderLoading);
        achievementViewModel.getError().observe(this, this::renderError);

        userStatsViewModel.getUserStats().observe(this, stats -> {
            userStats = stats == null ? defaultStats() : stats;
            renderStats();
            renderAchievements();
            levelViewModel.loadLevelProgress(Math.max(0, userStats.getTotalXp()));
        });
        userStatsViewModel.getLoading().observe(this, this::renderLoading);
        userStatsViewModel.getError().observe(this, this::renderError);
        userStatsViewModel.getReconciliationComplete().observe(this, complete -> {
            if (Boolean.TRUE.equals(complete) && !achievementDataLoaded) {
                loadAchievementData();
            }
        });

        levelViewModel.getCurrentLevel().observe(this, level -> {
            currentLevel = level;
            renderStats();
        });
        levelViewModel.getNextLevel().observe(this, level -> {
            nextLevel = level;
            renderStats();
        });
        levelViewModel.getError().observe(this, message -> renderFallbackXpProgress(
                Math.max(0, userStats == null ? 0 : userStats.getTotalXp())));
    }

    private void loadData() {
        if (userId == null || userId.trim().isEmpty()) {
            renderError(getString(R.string.achievement_user_missing));
            return;
        }
        userStats = defaultStats();
        renderStats();
        levelViewModel.loadLevelProgress(0);
        userStatsViewModel.reconcileExistingStatsAndAchievements(userId);
    }

    private void loadAchievementData() {
        achievementDataLoaded = true;
        achievementViewModel.loadActiveAchievements();
        achievementViewModel.loadUserAchievements(userId);
        userStatsViewModel.loadUserStats(userId);
    }

    private void renderAchievements() {
        adapter.submitData(achievements, userAchievements, userStats == null ? defaultStats() : userStats);
        updateEmptyState();
    }

    private void renderStats() {
        UserStats stats = userStats == null ? defaultStats() : userStats;
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                userAchievements == null ? 0 : userAchievements.size());
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        levelText.setText(getString(R.string.profile_level_with_icon_format, level));
        levelSubtitleText.setText(levelSubtitle(currentLevel));
        unlockedCountText.setText(getString(
                R.string.achievement_summary_count_format, unlockedCount,
                Math.max(achievements == null ? 0 : achievements.size(), unlockedCount)));
        renderXpProgress(stats, currentLevel, nextLevel);
    }

    private void renderXpProgress(UserStats stats, Level current, Level next) {
        long totalXp = Math.max(0, stats == null ? 0 : stats.getTotalXp());
        if (current == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }
        if (next == null) {
            xpProgressBar.setProgress(100);
            xpText.setText(getString(R.string.xp_total_format, totalXp));
            xpRemainingText.setText(R.string.xp_progress_max);
            return;
        }
        long currentRequiredXp = Math.max(0, current.getRequiredTotalXp());
        long nextRequiredXp = Math.max(currentRequiredXp + 1, next.getRequiredTotalXp());
        long progress = Math.max(0, totalXp - currentRequiredXp);
        long range = Math.max(1, nextRequiredXp - currentRequiredXp);
        xpProgressBar.setProgress((int) Math.min(100, (progress * 100) / range));
        xpText.setText(getString(R.string.xp_progress_format, totalXp, nextRequiredXp));
        xpRemainingText.setText(getString(R.string.xp_remaining_format,
                Math.max(0, nextRequiredXp - totalXp)));
    }

    private void renderFallbackXpProgress(long totalXp) {
        long safeXp = Math.max(0, totalXp);
        long fallbackTarget = 100;
        xpProgressBar.setProgress((int) Math.min(100, (safeXp * 100) / fallbackTarget));
        xpText.setText(getString(R.string.xp_progress_format, safeXp, fallbackTarget));
        xpRemainingText.setText(getString(R.string.xp_remaining_format,
                Math.max(0, fallbackTarget - safeXp)));
        if (levelSubtitleText != null) {
            levelSubtitleText.setText(getString(R.string.next_level_format, 2));
        }
    }

    private String levelSubtitle(Level level) {
        if (level == null) {
            return getString(R.string.next_level_format, 2);
        }
        String title = level.getTitle();
        String badge = level.getBadgeName();
        if (isBlank(title) && isBlank(badge)) {
            return getString(R.string.level_format, Math.max(1, level.getLevel()));
        }
        if (isBlank(title)) {
            return badge;
        }
        if (isBlank(badge)) {
            return title;
        }
        return title + " • " + badge;
    }

    private void renderLoading(Boolean loading) {
        progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
    }

    private void renderError(String message) {
        if (message == null || message.trim().isEmpty()) {
            errorText.setVisibility(View.GONE);
            return;
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void updateEmptyState() {
        boolean empty = adapter.getVisibleCount() == 0;
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty && showOnlyUnlocked
                && chipGroup != null
                && chipGroup.getCheckedChipId() == R.id.chipAchievementUnlocked) {
            emptyText.setText(R.string.user_achievements_empty);
            emptySubtitleText.setText(R.string.achievement_empty_subtitle);
        } else {
            emptyText.setText(R.string.achievement_list_empty);
            emptySubtitleText.setText(R.string.achievement_empty_subtitle);
        }
        emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
