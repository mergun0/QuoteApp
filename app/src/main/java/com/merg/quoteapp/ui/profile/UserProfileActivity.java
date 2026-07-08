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
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;
import com.merg.quoteapp.viewmodel.UserProfileViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";

    private UserProfileViewModel viewModel;
    private LikeViewModel likeViewModel;
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
    private ProgressBar xpProgressBar;
    private RecyclerView achievementRecyclerView;
    private String profileUserId;
    private boolean ownProfile;
    private boolean firstResume = true;
    private boolean loadMoreRequested;
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
    private List<Achievement> currentAchievements = new ArrayList<>();
    private List<UserAchievement> currentUserAchievements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        profileUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        ownProfile = currentUserId != null && currentUserId.equals(profileUserId);

        bindViews();
        setupToolbar();
        setupRecyclerView(currentUserId);
        setupScrollPagination();
        setupAchievementPreview();
        setupActions();

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
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
        xpProgressBar = findViewById(R.id.progressUserProfileXp);
        achievementRecyclerView = findViewById(R.id.recyclerUserProfileAchievements);
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
        }, false, true, currentUserId);
        adapter.setLikeActionsEnabled(true);
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
    }

    private void renderProfile(UserProfileData profile) {
        ((TextView) findViewById(R.id.textUserProfileUsername))
                .setText("@" + profile.getUsername());
        renderJoinedAt(profile.getJoinedAt());
        setStat(findViewById(R.id.userStatTotal),
                R.string.profile_total, profile.getTotalQuotes());
        setStat(findViewById(R.id.userStatMovies),
                R.string.profile_movies, profile.getMovieQuotes());
        setStat(findViewById(R.id.userStatSeries),
                R.string.profile_series, profile.getSeriesQuotes());
        setStat(findViewById(R.id.userStatBooks),
                R.string.profile_books, profile.getBookQuotes());
        setStat(findViewById(R.id.userStatLikes),
                R.string.profile_likes, profile.getTotalLikes());

        adapter.submitList(profile.getQuotes());
        likeViewModel.loadLikedStates(profile.getQuotes());
        likeViewModel.loadLikeCounts(profile.getQuotes());
        boolean empty = profile.getQuotes() == null || profile.getQuotes().isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            noMoreText.setVisibility(View.GONE);
        } else {
            renderHasMore(viewModel == null ? true : viewModel.getHasMore().getValue());
        }
    }

    private void setStat(View statView, int labelRes, int value) {
        ((TextView) statView.findViewById(R.id.textStatLabel)).setText(labelRes);
        ((TextView) statView.findViewById(R.id.textStatValue))
                .setText(String.valueOf(value));
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
            renderUserStats(safeStats);
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
        });
        achievementViewModel.getUserAchievements().observe(this, achievements -> {
            currentUserAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            UserStats current = userStatsViewModel.getUserStats().getValue();
            renderUserStats(current == null ? defaultStats() : current);
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
        renderUserStats(defaultStats());
        renderAchievementPreview();
    }

    private void renderUserStats(UserStats stats) {
        if (levelText == null || xpText == null || achievementCountText == null) {
            return;
        }
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        long totalXp = Math.max(0, stats.getTotalXp());
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                currentUserAchievements == null ? 0 : currentUserAchievements.size());
        levelText.setText(getString(R.string.level_format, level));
        xpText.setText(getString(R.string.xp_total_format, totalXp));
        achievementCountText.setText(getString(
                R.string.unlocked_achievement_count_format, unlockedCount));
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
            xpProgressText.setText(R.string.xp_progress_max);
            return;
        }
        long currentRequiredXp = Math.max(0, currentLevel.getRequiredTotalXp());
        long nextRequiredXp = Math.max(currentRequiredXp + 1, nextLevel.getRequiredTotalXp());
        long range = nextRequiredXp - currentRequiredXp;
        long progress = Math.max(0, totalXp - currentRequiredXp);
        xpProgressBar.setProgress((int) Math.min(100, (progress * 100) / range));
        xpProgressText.setText(getString(R.string.xp_progress_format, progress, range));
    }

    private void renderFallbackXpProgress(long totalXp) {
        long fallbackTarget = 100;
        long safeXp = Math.max(0, totalXp);
        xpProgressBar.setProgress((int) Math.min(100, (safeXp * 100) / fallbackTarget));
        xpProgressText.setText(getString(R.string.xp_progress_format,
                Math.min(safeXp, fallbackTarget), fallbackTarget));
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
