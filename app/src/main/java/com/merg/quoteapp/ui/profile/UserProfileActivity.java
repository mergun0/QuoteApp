package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.AchievementPreviewAdapter;
import com.merg.quoteapp.adapter.QuoteAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserProfileData;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.repository.LikeRepository;
import com.merg.quoteapp.repository.UserStatsRepository;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.utils.AccountDeletionGuard;
import com.merg.quoteapp.utils.ReportBottomSheetHelper;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.ReportViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;
import com.merg.quoteapp.viewmodel.UserProfileViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";

    private UserProfileViewModel viewModel;
    private LikeViewModel likeViewModel;
    private FavoriteViewModel favoriteViewModel;
    private ReportViewModel reportViewModel;
    private ReportBottomSheetHelper.ReportSheetController reportSheetController;
    private UserStatsViewModel userStatsViewModel;
    private AchievementViewModel achievementViewModel;
    private LevelViewModel levelViewModel;
    private QuoteAdapter adapter;
    private AchievementPreviewAdapter achievementAdapter;
    private NestedScrollView scrollView;
    private LinearLayout contentLayout;
    private LinearLayout ownActions;
    private View followButton;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView errorText;
    private TextView emptyText;
    private TextView statusText;
    private ProgressBar loadMoreProgress;
    private TextView noMoreText;
    private TextView levelText;
    private TextView xpText;
    private TextView xpProgressText;
    private TextView achievementCountText;
    private TextView achievementEmptyText;
    private TextView avatarText;
    private TextView displayNameText;
    private TextView nextLevelText;
    private TextView statLikesValueText;
    private TextView statQuotesValueText;
    private TextView statAchievementsValueText;
    private TextView statSavedValueText;
    private TextView lastAchievementTitleText;
    private TextView lastAchievementDescriptionText;
    private TextView lastAchievementMetaText;
    private TextView nextAchievementTitleText;
    private TextView nextAchievementDescriptionText;
    private TextView nextAchievementProgressText;
    private ProgressBar xpProgressBar;
    private ProgressBar nextAchievementProgressBar;
    private RecyclerView achievementRecyclerView;
    private String profileUserId;
    private boolean ownProfile;
    private boolean firstResume = true;
    private boolean loadMoreRequested;
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
    private Map<String, Boolean> renderedSavedStates = new HashMap<>();
    private Map<String, Boolean> renderedSaveLoadingStates = new HashMap<>();
    private Map<String, Long> renderedSaveCounts = new HashMap<>();
    private List<Achievement> currentAchievements = new ArrayList<>();
    private List<UserAchievement> currentUserAchievements = new ArrayList<>();
    private UserStats currentStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        AccountDeletionGuard.enforce(this);

        profileUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        ownProfile = currentUserId != null && currentUserId.equals(profileUserId);
        if (ownProfile) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_OPEN_PROFILE_TAB, true);
            startActivity(intent);
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupRecyclerView(currentUserId);
        setupScrollPagination();
        setupAchievementPreview();
        setupActions();

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        userStatsViewModel = new ViewModelProvider(this).get(UserStatsViewModel.class);
        achievementViewModel = new ViewModelProvider(this).get(AchievementViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        viewModel.getProfile().observe(this, this::renderProfile);
        viewModel.getState().observe(this, this::renderState);
        viewModel.getOperationState().observe(this, this::renderOperationState);
        viewModel.getLoadMoreState().observe(this, this::renderLoadMoreState);
        viewModel.getHasMore().observe(this, this::renderHasMore);
        likeViewModel.getLikedStates().observe(this, this::renderLikedStates);
        likeViewModel.getItemLoadingStates().observe(this, this::renderLikeLoadingStates);
        likeViewModel.getLikeCounts().observe(this, this::renderLikeCounts);
        likeViewModel.getLoadingState().observe(this, this::renderLikeState);
        favoriteViewModel.getSavedStates().observe(this, this::renderSavedStates);
        favoriteViewModel.getItemLoadingStates().observe(this, this::renderSaveLoadingStates);
        favoriteViewModel.getFavoriteCounts().observe(this, this::renderSaveCounts);
        favoriteViewModel.getOperationState().observe(this, this::renderFavoriteState);
        observeReportState();
        observeAchievementState();
        viewModel.loadProfile(profileUserId);
        loadAchievementState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
        } else if (viewModel != null) {
            loadMoreRequested = false;
            viewModel.refreshProfile();
        }
    }

    private void bindViews() {
        scrollView = findViewById(R.id.scrollUserProfile);
        contentLayout = findViewById(R.id.layoutUserProfileContent);
        ownActions = findViewById(R.id.layoutOwnProfileActions);
        followButton = findViewById(R.id.buttonFollowComingSoon);
        progressBar = findViewById(R.id.progressUserProfile);
        recyclerView = findViewById(R.id.recyclerUserQuotes);
        errorText = findViewById(R.id.textUserProfileError);
        emptyText = findViewById(R.id.textEmptyUserQuotes);
        statusText = findViewById(R.id.textUserProfileStatus);
        loadMoreProgress = findViewById(R.id.progressUserQuotesLoadMore);
        noMoreText = findViewById(R.id.textUserQuotesNoMore);
        levelText = findViewById(R.id.textUserProfileLevel);
        xpText = findViewById(R.id.textUserProfileXp);
        xpProgressText = findViewById(R.id.textUserProfileXpProgress);
        achievementCountText = findViewById(R.id.textUserProfileAchievementCount);
        achievementEmptyText = findViewById(R.id.textUserProfileAchievementEmpty);
        avatarText = findViewById(R.id.textUserProfileAvatar);
        displayNameText = findViewById(R.id.textUserProfileDisplayName);
        nextLevelText = findViewById(R.id.textUserProfileNextLevel);
        statLikesValueText = findViewById(R.id.userStatLikes)
                .findViewById(R.id.textUserProfileStatValue);
        statQuotesValueText = findViewById(R.id.userStatTotal)
                .findViewById(R.id.textUserProfileStatValue);
        statAchievementsValueText = findViewById(R.id.userStatAchievements)
                .findViewById(R.id.textUserProfileStatValue);
        statSavedValueText = findViewById(R.id.userStatSaved)
                .findViewById(R.id.textUserProfileStatValue);
        lastAchievementTitleText = findViewById(R.id.textLastAchievementTitle);
        lastAchievementDescriptionText = findViewById(R.id.textLastAchievementDescription);
        lastAchievementMetaText = findViewById(R.id.textLastAchievementMeta);
        nextAchievementTitleText = findViewById(R.id.textUserProfileNextAchievementTitle);
        nextAchievementDescriptionText = findViewById(R.id.textUserProfileNextAchievementDescription);
        nextAchievementProgressText = findViewById(R.id.textUserProfileNextAchievementProgress);
        xpProgressBar = findViewById(R.id.progressUserProfileXp);
        nextAchievementProgressBar = findViewById(R.id.progressUserProfileNextAchievement);
        achievementRecyclerView = findViewById(R.id.recyclerUserProfileAchievements);
        setupStatCards();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarUserProfile);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void setupRecyclerView(String currentUserId) {
        adapter = new QuoteAdapter(new QuoteAdapter.QuoteActionListener() {
            @Override
            public void onEdit(Quote quote) {
                editQuote(quote);
            }

            @Override
            public void onDelete(Quote quote) {
                confirmDelete(quote);
            }

            @Override
            public void onShare(Quote quote) {
                shareQuote(quote);
            }

            @Override
            public void onFavorite(Quote quote) {
                toggleLike(quote);
            }

            @Override
            public void onSave(Quote quote) {
                toggleSave(quote);
            }

            @Override
            public void onReport(Quote quote) {
                showReportSheet(quote);
            }

            @Override
            public void onOpen(Quote quote) {
                Intent intent = new Intent(
                        UserProfileActivity.this, QuoteDetailActivity.class);
                intent.putExtra(QuoteDetailActivity.EXTRA_QUOTE_ID, quote.getQuoteId());
                startActivity(intent);
            }

            @Override
            public void onUserProfile(String userId) {
                openUserProfile(userId);
            }
        }, false, true, currentUserId, R.layout.item_quote_home);
        adapter.setLikeActionsEnabled(true);
        adapter.setSaveActionsEnabled(true);
        adapter.setReportActionsEnabled(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupScrollPagination() {
        if (scrollView == null) {
            return;
        }
        scrollView.setOnScrollChangeListener((NestedScrollView view, int scrollX, int scrollY,
                                              int oldScrollX, int oldScrollY) -> {
            if (viewModel == null || scrollY <= oldScrollY) {
                return;
            }
            View child = view.getChildAt(0);
            if (child == null) {
                return;
            }
            int remainingScroll = child.getMeasuredHeight() - view.getMeasuredHeight() - scrollY;
            if (remainingScroll <= 240) {
                if (Boolean.TRUE.equals(viewModel.getHasMore().getValue())) {
                    loadMoreRequested = true;
                }
                viewModel.loadMoreQuotes();
            }
        });
    }

    private void setupActions() {
        ownActions.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        followButton.setVisibility(ownProfile ? View.GONE : View.VISIBLE);

        findViewById(R.id.buttonEditUserProfile).setOnClickListener(view ->
                showStatus(getString(R.string.profile_edit_coming_soon), false));
        findViewById(R.id.buttonUserProfileLogout).setOnClickListener(view -> logout());
        followButton.setOnClickListener(view ->
                showStatus(getString(R.string.follow_coming_soon), false));
        findViewById(R.id.buttonUserProfileAllAchievements)
                .setOnClickListener(view -> openAchievements());
        findViewById(R.id.cardUserProfileLastAchievement)
                .setOnClickListener(view -> openAchievements());
        findViewById(R.id.cardUserProfileNextAchievement)
                .setOnClickListener(view -> openAchievements());
    }

    private void renderProfile(UserProfileData profile) {
        String username = safeDisplayName(profile.getUsername());
        displayNameText.setText(username);
        ((TextView) findViewById(R.id.textUserProfileUsername))
                .setText(getString(R.string.profile_handle_format, safeHandle(username)));
        avatarText.setText(firstLetter(username));
        renderJoinedAt(profile.getJoinedAt());
        statLikesValueText.setText(String.valueOf(profile.getTotalLikes()));
        statQuotesValueText.setText(String.valueOf(profile.getTotalQuotes()));
        statSavedValueText.setText("0");

        adapter.submitList(profile.getQuotes());
        likeViewModel.refreshLikedStates(profile.getQuotes());
        likeViewModel.refreshLikeCounts(profile.getQuotes());
        favoriteViewModel.refreshSavedStates(profile.getQuotes());
        favoriteViewModel.loadFavoriteCounts(profile.getQuotes());
        boolean empty = profile.getQuotes() == null || profile.getQuotes().isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            noMoreText.setVisibility(View.GONE);
        } else {
            renderHasMore(viewModel == null ? true : viewModel.getHasMore().getValue());
        }
    }

    private void setupStatCards() {
        setupStatCard(findViewById(R.id.userStatLikes), "♥",
                R.string.profile_stat_total_likes, R.color.home_v2_error);
        setupStatCard(findViewById(R.id.userStatTotal), "✎",
                R.string.profile_stat_total_quotes, R.color.home_v2_primary);
        setupStatCard(findViewById(R.id.userStatAchievements), "★",
                R.string.profile_stat_unlocked_achievements, R.color.home_v2_accent);
        setupStatCard(findViewById(R.id.userStatSaved), "◆",
                R.string.profile_stat_saved_quotes, R.color.home_v2_secondary);
    }

    private void setupStatCard(View statView, String icon, int labelRes, int colorRes) {
        ((TextView) statView.findViewById(R.id.textUserProfileStatIcon)).setText(icon);
        ((TextView) statView.findViewById(R.id.textUserProfileStatIcon))
                .setTextColor(ContextCompat.getColor(this, colorRes));
        ((TextView) statView.findViewById(R.id.textUserProfileStatLabel)).setText(labelRes);
    }

    private void renderJoinedAt(Timestamp joinedAt) {
        TextView joinedText = findViewById(R.id.textUserProfileJoinedAt);
        if (joinedAt == null) {
            joinedText.setVisibility(View.GONE);
            return;
        }
        SimpleDateFormat formatter =
                new SimpleDateFormat("MMMM yyyy", new Locale("tr", "TR"));
        joinedText.setText(getString(
                R.string.joined_at, formatter.format(joinedAt.toDate())));
        joinedText.setVisibility(View.VISIBLE);
    }

    private void renderState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            scrollView.setVisibility(View.GONE);
            contentLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            scrollView.setVisibility(View.GONE);
            contentLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            errorText.setText(state.getMessage());
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.VISIBLE);
        }
    }

    private void renderOperationState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.LOADING) {
            showStatus(getString(R.string.operation_in_progress), false);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else {
            showStatus(getString(R.string.quote_deleted), false);
        }
    }

    private void renderLoadMoreState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        loadMoreProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderHasMore(Boolean hasMore) {
        UserProfileData current = viewModel == null ? null : viewModel.getProfile().getValue();
        boolean hasQuotes = current != null && current.getQuotes() != null
                && !current.getQuotes().isEmpty();
        noMoreText.setVisibility(loadMoreRequested && Boolean.FALSE.equals(hasMore) && hasQuotes
                ? View.VISIBLE : View.GONE);
    }

    private void renderLikeCounts(Map<String, Long> likeCounts) {
        if (likeCounts == null) {
            return;
        }
        for (Map.Entry<String, Long> entry : likeCounts.entrySet()) {
            if (!entry.getValue().equals(renderedLikeCounts.get(entry.getKey()))) {
                adapter.updateLikeCount(entry.getKey(), entry.getValue());
            }
        }
        renderedLikeCounts = new HashMap<>(likeCounts);
    }

    private void renderLikedStates(Map<String, Boolean> likedStates) {
        if (likedStates == null) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : likedStates.entrySet()) {
            if (!entry.getValue().equals(renderedLikedStates.get(entry.getKey()))) {
                adapter.updateLikeState(entry.getKey(), entry.getValue());
            }
        }
        renderedLikedStates = new HashMap<>(likedStates);
    }

    private void renderLikeLoadingStates(Map<String, Boolean> loadingStates) {
        if (loadingStates == null) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : loadingStates.entrySet()) {
            if (!entry.getValue().equals(renderedLikeLoadingStates.get(entry.getKey()))) {
                adapter.updateLikeLoadingState(entry.getKey(), entry.getValue());
            }
        }
        renderedLikeLoadingStates = new HashMap<>(loadingStates);
    }

    private void renderLikeState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderSavedStates(Map<String, Boolean> savedStates) {
        if (savedStates == null) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : savedStates.entrySet()) {
            if (!entry.getValue().equals(renderedSavedStates.get(entry.getKey()))) {
                adapter.updateSaveState(entry.getKey(), entry.getValue());
            }
        }
        renderedSavedStates = new HashMap<>(savedStates);
    }

    private void renderSaveLoadingStates(Map<String, Boolean> loadingStates) {
        if (loadingStates == null) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : loadingStates.entrySet()) {
            if (!entry.getValue().equals(renderedSaveLoadingStates.get(entry.getKey()))) {
                adapter.updateSaveLoadingState(entry.getKey(), entry.getValue());
            }
        }
        renderedSaveLoadingStates = new HashMap<>(loadingStates);
    }

    private void renderSaveCounts(Map<String, Long> saveCounts) {
        if (saveCounts == null) {
            return;
        }
        for (Map.Entry<String, Long> entry : saveCounts.entrySet()) {
            if (!entry.getValue().equals(renderedSaveCounts.get(entry.getKey()))) {
                adapter.updateSaveCount(entry.getKey(), entry.getValue());
            }
        }
        renderedSaveCounts = new HashMap<>(saveCounts);
    }

    private void renderFavoriteState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void observeReportState() {
        reportViewModel.getLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading)) {
                if (reportSheetController != null) {
                    reportSheetController.setLoading(true);
                }
                showStatus(getString(R.string.operation_in_progress), false);
            }
        });
        reportViewModel.getSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                dismissReportSheet();
                showStatus(getString(R.string.report_sent), false);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getAlreadyReported().observe(this, already -> {
            if (Boolean.TRUE.equals(already)) {
                resetReportSheetLoading();
                showReportSheetMessage(getString(R.string.report_already_sent), true);
                showStatus(getString(R.string.report_already_sent), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getDailyLimitReached().observe(this, reached -> {
            if (Boolean.TRUE.equals(reached)) {
                resetReportSheetLoading();
                showReportSheetMessage(getString(R.string.report_daily_limit_reached), true);
                showStatus(getString(R.string.report_daily_limit_reached), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getError().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                resetReportSheetLoading();
                showReportSheetMessage(message, true);
                showStatus(message, true);
                reportViewModel.clearResultStates();
            }
        });
    }

    private void setupAchievementPreview() {
        achievementAdapter = new AchievementPreviewAdapter();
        achievementRecyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        achievementRecyclerView.setAdapter(achievementAdapter);
        renderDefaultAchievementState();
    }

    private void observeAchievementState() {
        if (userStatsViewModel == null || achievementViewModel == null) {
            return;
        }
        userStatsViewModel.getUserStats().observe(this, stats -> {
            UserStats safeStats = stats == null ? defaultStats() : stats;
            currentStats = safeStats;
            renderUserStats(safeStats);
            renderAchievementFocusCards();
            if (levelViewModel != null) {
                levelViewModel.loadLevelProgress(Math.max(0, safeStats.getTotalXp()));
            }
        });
        userStatsViewModel.getError().observe(this, message -> {
            if (message != null) {
                renderUserStats(defaultStats());
            }
        });
        if (levelViewModel != null) {
            levelViewModel.getCurrentLevel().observe(this, level -> {
                UserStats current = userStatsViewModel.getUserStats().getValue();
                renderUserStats(current == null ? defaultStats() : current);
            });
            levelViewModel.getNextLevel().observe(this, level -> {
                UserStats current = userStatsViewModel.getUserStats().getValue();
                renderUserStats(current == null ? defaultStats() : current);
            });
        }
        achievementViewModel.getActiveAchievements().observe(this, achievements -> {
            currentAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            renderAchievementFocusCards();
        });
        achievementViewModel.getUserAchievements().observe(this, achievements -> {
            currentUserAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            UserStats current = userStatsViewModel.getUserStats().getValue();
            renderUserStats(current == null ? defaultStats() : current);
            renderAchievementFocusCards();
        });
        achievementViewModel.getError().observe(this, message -> {
            if (message != null && currentAchievements.isEmpty()) {
                renderAchievementPreview();
            }
        });
    }

    private void loadAchievementState() {
        if (isBlank(profileUserId)) {
            renderDefaultAchievementState();
            return;
        }
        if (userStatsViewModel == null || achievementViewModel == null) {
            renderDefaultAchievementState();
            return;
        }
        userStatsViewModel.loadUserStats(profileUserId);
        achievementViewModel.loadActiveAchievements();
        achievementViewModel.loadUserAchievements(profileUserId);
    }

    private void renderDefaultAchievementState() {
        currentStats = defaultStats();
        renderUserStats(currentStats);
        renderAchievementPreview();
        renderAchievementFocusCards();
    }

    private void renderUserStats(UserStats stats) {
        if (levelText == null || xpText == null || achievementCountText == null) {
            return;
        }
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        long totalXp = Math.max(0, stats.getTotalXp());
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                currentUserAchievements == null ? 0 : currentUserAchievements.size());
        levelText.setText(getString(R.string.profile_level_with_icon_format, level));
        achievementCountText.setText(getString(
                R.string.profile_achievement_count_format, unlockedCount));
        statAchievementsValueText.setText(String.valueOf(unlockedCount));
        renderXpProgress(totalXp);
    }

    private void renderXpProgress(long totalXp) {
        if (xpProgressBar == null || xpProgressText == null) {
            return;
        }
        if (levelViewModel == null || levelViewModel.getCurrentLevel().getValue() == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }
        Level currentLevel = levelViewModel.getCurrentLevel().getValue();
        Level nextLevel = levelViewModel.getNextLevel().getValue();
        if (nextLevel == null) {
            xpProgressBar.setProgress(100);
            xpText.setText(getString(R.string.xp_total_format, totalXp));
            xpProgressText.setText(R.string.xp_progress_max);
            nextLevelText.setVisibility(View.GONE);
            return;
        }
        long currentRequiredXp = Math.max(0, currentLevel.getRequiredTotalXp());
        long nextRequiredXp = Math.max(currentRequiredXp + 1, nextLevel.getRequiredTotalXp());
        long range = nextRequiredXp - currentRequiredXp;
        long progress = Math.max(0, totalXp - currentRequiredXp);
        xpProgressBar.setProgress((int) Math.min(100, (progress * 100) / range));
        xpText.setText(getString(R.string.xp_progress_format, totalXp, nextRequiredXp));
        xpProgressText.setText(getString(R.string.xp_remaining_format,
                Math.max(0, nextRequiredXp - totalXp)));
        nextLevelText.setVisibility(View.GONE);
    }

    private void renderFallbackXpProgress(long totalXp) {
        long fallbackTarget = 100;
        long safeXp = Math.max(0, totalXp);
        xpProgressBar.setProgress((int) Math.min(100, (safeXp * 100) / fallbackTarget));
        xpText.setText(getString(R.string.xp_progress_format,
                Math.min(safeXp, fallbackTarget), fallbackTarget));
        xpProgressText.setText(getString(R.string.xp_remaining_format,
                Math.max(0, fallbackTarget - safeXp)));
        nextLevelText.setVisibility(View.GONE);
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
        lastAchievementTitleText.setText(achievement == null || isBlank(achievement.getTitle())
                ? getString(R.string.achievement_default_title) : achievement.getTitle());
        lastAchievementDescriptionText.setText(achievement == null || isBlank(achievement.getDescription())
                ? getString(R.string.achievement_unlocked) : achievement.getDescription());
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
        nextAchievementTitleText.setText(isBlank(next.achievement.getTitle())
                ? getString(R.string.achievement_default_title) : next.achievement.getTitle());
        nextAchievementDescriptionText.setText(isBlank(next.achievement.getDescription())
                ? getString(R.string.profile_next_achievement_fallback_detail)
                : next.achievement.getDescription());
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

    private long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    private String buildAchievementMeta(Achievement achievement, Timestamp unlockedAt) {
        List<String> parts = new ArrayList<>();
        if (achievement != null && achievement.getXpReward() > 0) {
            parts.add(getString(R.string.achievement_xp_reward_format, achievement.getXpReward()));
        }
        String date = formatAchievementDate(unlockedAt);
        if (!isBlank(date)) {
            parts.add(date);
        }
        return String.join(" • ", parts);
    }

    private String formatAchievementDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date);
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setUserId(profileUserId);
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }

    private void openAchievements() {
        if (isBlank(profileUserId)) {
            showStatus(getString(R.string.achievement_user_missing), true);
            return;
        }
        Intent intent = new Intent(this, AchievementsActivity.class);
        intent.putExtra(AchievementsActivity.EXTRA_USER_ID, profileUserId);
        intent.putExtra(AchievementsActivity.EXTRA_SHOW_ONLY_UNLOCKED, true);
        startActivity(intent);
    }

    private void toggleLike(Quote quote) {
        if (quote == null || isBlank(quote.getQuoteId())) {
            return;
        }
        likeViewModel.toggleLike(quote.getQuoteId());
    }

    private void toggleSave(Quote quote) {
        if (quote == null || isBlank(quote.getQuoteId())) {
            return;
        }
        favoriteViewModel.toggleSaved(quote);
    }

    private void showReportSheet(Quote quote) {
        if (quote == null || isBlank(quote.getQuoteId())) {
            return;
        }
        reportSheetController = ReportBottomSheetHelper.show(this,
                (reason, description) -> reportViewModel.submitReport(quote, reason, description));
    }

    private void dismissReportSheet() {
        if (reportSheetController != null && reportSheetController.isShowing()) {
            reportSheetController.dismiss();
        }
        reportSheetController = null;
    }

    private void showReportSheetMessage(String message, boolean error) {
        if (reportSheetController != null && reportSheetController.isShowing()) {
            reportSheetController.showMessage(message, error);
        }
    }

    private void resetReportSheetLoading() {
        if (reportSheetController != null && reportSheetController.isShowing()) {
            reportSheetController.setLoading(false);
        }
    }

    private void openUserProfile(String userId) {
        if (isBlank(userId)) {
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId != null && currentUserId.equals(userId)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_OPEN_PROFILE_TAB, true);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    private void editQuote(Quote quote) {
        Intent intent = new Intent(this, AddQuoteActivity.class);
        intent.putExtra(AddQuoteActivity.EXTRA_QUOTE_ID, quote.getQuoteId());
        intent.putExtra(AddQuoteActivity.EXTRA_TYPE, quote.getType());
        intent.putExtra(AddQuoteActivity.EXTRA_TEXT, quote.getText());
        intent.putExtra(AddQuoteActivity.EXTRA_TITLE, quote.getTitle());
        intent.putExtra(AddQuoteActivity.EXTRA_AUTHOR, quote.getAuthor());
        intent.putExtra(AddQuoteActivity.EXTRA_CHARACTER, quote.getCharacterName());
        intent.putExtra(AddQuoteActivity.EXTRA_SEASON, quote.getSeason());
        intent.putExtra(AddQuoteActivity.EXTRA_EPISODE, quote.getEpisode());
        intent.putExtra(AddQuoteActivity.EXTRA_TAGS, quote.getTags() == null ? ""
                : String.join(", ", quote.getTags()));
        intent.putExtra(AddQuoteActivity.EXTRA_SPOILER, quote.isSpoiler());
        startActivity(intent);
    }

    private void confirmDelete(Quote quote) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_quote_title)
                .setMessage(R.string.delete_quote_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.deleteQuote(quote.getQuoteId()))
                .show();
    }

    private void shareQuote(Quote quote) {
        String shareText = "“" + safe(quote.getText()) + "”\n\n"
                + safe(quote.getTitle()) + " — " + safe(quote.getAuthor());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LikeRepository.clearMemoryCache();
        UserStatsRepository.clearMemoryCache();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this, error
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeDisplayName(String username) {
        return isBlank(username) ? getString(R.string.username) : username.trim();
    }

    private String safeHandle(String username) {
        String cleaned = username == null ? "" : username.trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1);
        }
        cleaned = cleaned.replaceAll("\\s+", "").toLowerCase(Locale.US);
        return cleaned.isEmpty() ? "kullanici" : cleaned;
    }

    private String firstLetter(String value) {
        if (isBlank(value)) {
            return "K";
        }
        return value.trim().substring(0, 1).toUpperCase(new Locale("tr", "TR"));
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
