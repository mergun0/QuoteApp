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
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.AchievementAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_SHOW_ONLY_UNLOCKED = "showOnlyUnlocked";

    private AchievementViewModel achievementViewModel;
    private UserStatsViewModel userStatsViewModel;
    private AchievementAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorText;
    private TextView emptyText;
    private TextView levelText;
    private TextView xpText;
    private TextView unlockedCountText;
    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private String userId;
    private boolean showOnlyUnlocked;
    private List<Achievement> achievements = new ArrayList<>();
    private List<UserAchievement> userAchievements = new ArrayList<>();
    private UserStats userStats;

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
        emptyText = findViewById(R.id.textAchievementsEmpty);
        levelText = findViewById(R.id.textAchievementsLevel);
        xpText = findViewById(R.id.textAchievementsXp);
        unlockedCountText = findViewById(R.id.textAchievementsUnlockedCount);
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

        achievementViewModel.getActiveAchievements().observe(this, result -> {
            achievements = result == null ? new ArrayList<>() : result;
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
        });
        userStatsViewModel.getLoading().observe(this, this::renderLoading);
        userStatsViewModel.getError().observe(this, this::renderError);
    }

    private void loadData() {
        if (userId == null || userId.trim().isEmpty()) {
            renderError(getString(R.string.achievement_user_missing));
            return;
        }
        userStats = defaultStats();
        renderStats();
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
        levelText.setText(getString(R.string.level_format, level));
        xpText.setText(getString(R.string.xp_total_format, Math.max(0, stats.getTotalXp())));
        unlockedCountText.setText(getString(
                R.string.unlocked_achievement_count_format, unlockedCount));
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
        } else {
            emptyText.setText(R.string.achievement_list_empty);
        }
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }
}
