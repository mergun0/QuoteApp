package com.merg.quoteapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.QuoteAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.ui.profile.AchievementsActivity;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.utils.ReportBottomSheetHelper;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.ProfileViewModel;
import com.merg.quoteapp.viewmodel.QuoteViewModel;
import com.merg.quoteapp.viewmodel.ReportViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private QuoteAdapter adapter;
    private QuoteViewModel viewModel;
    private LikeViewModel likeViewModel;
    private FavoriteViewModel favoriteViewModel;
    private ReportViewModel reportViewModel;
    private UserStatsViewModel userStatsViewModel;
    private AchievementViewModel achievementViewModel;
    private LevelViewModel levelViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ProgressBar xpProgressBar;
    private LinearLayout emptyLayout;
    private TextView statusText;
    private TextView greetingText;
    private TextView usernameText;
    private TextView avatarText;
    private TextView levelText;
    private TextView xpProgressText;
    private TextView achievementCountText;
    private TextView emptyTitleText;
    private TextView emptyDescriptionText;
    private TextInputEditText searchInput;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final List<Quote> allQuotes = new ArrayList<>();
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
    private Map<String, Boolean> renderedSavedStates = new HashMap<>();
    private Map<String, Boolean> renderedSaveLoadingStates = new HashMap<>();
    private List<UserAchievement> currentUserAchievements = new ArrayList<>();
    private UserStats currentStats;
    private String searchQuery = "";
    private QuoteFilter selectedFilter = QuoteFilter.ALL;

    private enum QuoteFilter {
        ALL,
        MOVIE,
        SERIES,
        BOOK,
        SPOILER
    }

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(QuoteViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        userStatsViewModel = new ViewModelProvider(this).get(UserStatsViewModel.class);
        achievementViewModel = new ViewModelProvider(this).get(AchievementViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        viewModel.getQuotes().observe(getViewLifecycleOwner(), this::renderQuotes);
        viewModel.getListState().observe(getViewLifecycleOwner(), this::renderListState);
        viewModel.getOperationState().observe(getViewLifecycleOwner(), this::renderOperationState);
        likeViewModel.getLikedStates().observe(getViewLifecycleOwner(), this::renderLikedStates);
        likeViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderLikeLoadingStates);
        likeViewModel.getLikeCounts().observe(getViewLifecycleOwner(), this::renderLikeCounts);
        likeViewModel.getLoadingState().observe(getViewLifecycleOwner(), this::renderLikeState);
        favoriteViewModel.getSavedStates().observe(getViewLifecycleOwner(), this::renderSavedStates);
        favoriteViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderSaveLoadingStates);
        favoriteViewModel.getOperationState().observe(
                getViewLifecycleOwner(), this::renderFavoriteState);
        observeReportState();
        observeDashboardState();
        viewModel.loadCurrentUserQuotes();
        setupSearch();
        setupFilters(view);
        setupGreeting();
        setupDashboardActions(view);
        loadDashboardState();

        swipeRefreshLayout.setColorSchemeResources(
                R.color.quote_primary,
                R.color.quote_secondary);
        swipeRefreshLayout.setOnRefreshListener(viewModel::refreshQuotes);

        view.findViewById(R.id.buttonAddQuote).setOnClickListener(button ->
                startActivity(new Intent(requireContext(), AddQuoteActivity.class)));
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerQuotes);
        progressBar = view.findViewById(R.id.progressQuotes);
        xpProgressBar = view.findViewById(R.id.progressHomeXp);
        emptyLayout = view.findViewById(R.id.layoutEmptyQuotes);
        statusText = view.findViewById(R.id.textQuotesStatus);
        greetingText = view.findViewById(R.id.textHomeGreeting);
        usernameText = view.findViewById(R.id.textHomeUsername);
        avatarText = view.findViewById(R.id.textHomeAvatar);
        levelText = view.findViewById(R.id.textHomeLevel);
        xpProgressText = view.findViewById(R.id.textHomeXpProgress);
        achievementCountText = view.findViewById(R.id.textHomeAchievementCount);
        emptyTitleText = view.findViewById(R.id.textEmptyQuotesTitle);
        emptyDescriptionText = view.findViewById(R.id.textEmptyQuotesDescription);
        searchInput = view.findViewById(R.id.editHomeSearch);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshQuotes);
    }

    private void setupRecyclerView() {
        adapter = new QuoteAdapter(new QuoteAdapter.QuoteActionListener() {
            @Override
            public void onEdit(Quote quote) {
                openEditQuote(quote);
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
                openQuoteDetail(quote);
            }

            @Override
            public void onReport(Quote quote) {
                showReportSheet(quote);
            }

            @Override
            public void onSave(Quote quote) {
                toggleSave(quote);
            }
        }, false, false, null, R.layout.item_quote_home);
        adapter.setLikeActionsEnabled(true);
        adapter.setSaveActionsEnabled(true);
        adapter.setReportActionsEnabled(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void renderQuotes(List<Quote> quotes) {
        allQuotes.clear();
        if (quotes != null) {
            allQuotes.addAll(quotes);
        }
        if (likeViewModel != null) {
            likeViewModel.loadLikedStates(allQuotes);
            likeViewModel.loadLikeCounts(allQuotes);
        }
        if (favoriteViewModel != null) {
            favoriteViewModel.loadSavedStates(allQuotes);
        }
        applyFilters();
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

    private void renderFavoriteState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void observeReportState() {
        reportViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                showStatus(getString(R.string.operation_in_progress), false);
            }
        });
        reportViewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                showStatus(getString(R.string.report_sent), false);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getAlreadyReported().observe(getViewLifecycleOwner(), already -> {
            if (Boolean.TRUE.equals(already)) {
                showStatus(getString(R.string.report_already_sent), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getDailyLimitReached().observe(getViewLifecycleOwner(), reached -> {
            if (Boolean.TRUE.equals(reached)) {
                showStatus(getString(R.string.report_daily_limit_reached), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showStatus(message, true);
                reportViewModel.clearResultStates();
            }
        });
    }

    private void applyFilters() {
        List<Quote> filteredQuotes = allQuotes.stream()
                .filter(this::matchesSelectedFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
        adapter.submitList(filteredQuotes);

        boolean empty = filteredQuotes.isEmpty();
        boolean filtersActive = !searchQuery.isEmpty() || selectedFilter != QuoteFilter.ALL;
        emptyTitleText.setText(filtersActive
                ? R.string.no_filter_results : R.string.no_quotes);
        emptyDescriptionText.setText(filtersActive
                ? R.string.no_filter_results_detail : R.string.no_quotes_detail);
        emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesSelectedFilter(Quote quote) {
        switch (selectedFilter) {
            case MOVIE:
                return "Film".equals(quote.getType());
            case SERIES:
                return "Dizi".equals(quote.getType());
            case BOOK:
                return "Kitap".equals(quote.getType());
            case SPOILER:
                return quote.isSpoiler();
            case ALL:
            default:
                return true;
        }
    }

    private boolean matchesSearch(Quote quote) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        String searchable = String.join(" ",
                safe(quote.getText()),
                safe(quote.getTitle()),
                safe(quote.getAuthor()),
                safe(quote.getCharacterName()),
                quote.getTags() == null ? "" : String.join(" ", quote.getTags()));
        return searchable.toLowerCase(new Locale("tr", "TR")).contains(searchQuery);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                searchQuery = text == null ? "" : text.toString().trim()
                        .toLowerCase(new Locale("tr", "TR"));
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void setupFilters(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupQuoteFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (checkedId == R.id.chipMovie) {
                selectedFilter = QuoteFilter.MOVIE;
            } else if (checkedId == R.id.chipSeries) {
                selectedFilter = QuoteFilter.SERIES;
            } else if (checkedId == R.id.chipBook) {
                selectedFilter = QuoteFilter.BOOK;
            } else if (checkedId == R.id.chipSpoiler) {
                selectedFilter = QuoteFilter.SPOILER;
            } else {
                selectedFilter = QuoteFilter.ALL;
            }
            applyFilters();
        });
    }

    private void setupGreeting() {
        String fallback = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null
                && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            fallback = email.substring(0, email.indexOf('@'));
        }
        if (!fallback.isEmpty()) {
            renderHomeIdentity(fallback);
        }

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getProfile().observe(getViewLifecycleOwner(), this::renderGreeting);
        profileViewModel.loadProfile();
    }

    private void renderGreeting(ProfileStats profile) {
        if (profile != null && profile.getUsername() != null
                && !profile.getUsername().trim().isEmpty()) {
            renderHomeIdentity(profile.getUsername());
        }
    }

    private void renderHomeIdentity(String username) {
        String safeUsername = username == null || username.trim().isEmpty()
                ? getString(R.string.username) : username.trim();
        greetingText.setText(greetingForNow());
        usernameText.setText(safeUsername);
        avatarText.setText(safeUsername.substring(0, 1).toUpperCase(new Locale("tr", "TR")));
    }

    private String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Günaydın 👋";
        }
        if (hour < 18) {
            return "İyi günler 👋";
        }
        return "İyi akşamlar 👋";
    }

    private void setupDashboardActions(View view) {
        avatarText.setOnClickListener(button -> openProfileTab());
        view.findViewById(R.id.buttonHomeAllAchievements)
                .setOnClickListener(button -> openAchievements());
    }

    private void observeDashboardState() {
        userStatsViewModel.getUserStats().observe(getViewLifecycleOwner(), stats -> {
            currentStats = stats == null ? defaultStats() : stats;
            renderDashboardStats();
            levelViewModel.loadLevelProgress(Math.max(0, currentStats.getTotalXp()));
        });
        userStatsViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                currentStats = defaultStats();
                renderDashboardStats();
            }
        });
        achievementViewModel.getUserAchievements().observe(getViewLifecycleOwner(), achievements -> {
            currentUserAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderDashboardStats();
        });
        achievementViewModel.getActiveAchievements().observe(getViewLifecycleOwner(),
                achievements -> renderDashboardStats());
        levelViewModel.getCurrentLevel().observe(getViewLifecycleOwner(), level -> renderDashboardStats());
        levelViewModel.getNextLevel().observe(getViewLifecycleOwner(), level -> renderDashboardStats());
    }

    private void loadDashboardState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            currentStats = defaultStats();
            renderDashboardStats();
            return;
        }
        userStatsViewModel.createDefaultUserStatsIfMissing(user.getUid());
        achievementViewModel.loadActiveAchievements();
        achievementViewModel.loadUserAchievements(user.getUid());
    }

    private void renderDashboardStats() {
        UserStats stats = currentStats == null ? defaultStats() : currentStats;
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        long totalXp = Math.max(0, stats.getTotalXp());
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                currentUserAchievements == null ? 0 : currentUserAchievements.size());

        levelText.setText(getString(R.string.level_format, level));
        achievementCountText.setText(getString(
                R.string.unlocked_achievement_count_format, unlockedCount));
        renderXpProgress(totalXp);
    }

    private void renderXpProgress(long totalXp) {
        Level currentLevel = levelViewModel.getCurrentLevel().getValue();
        Level nextLevel = levelViewModel.getNextLevel().getValue();
        if (currentLevel == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }
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
        xpProgressText.setText(getString(R.string.xp_progress_format, totalXp, nextRequiredXp));
    }

    private void renderFallbackXpProgress(long totalXp) {
        long fallbackTarget = 100;
        long safeXp = Math.max(0, totalXp);
        xpProgressBar.setProgress((int) Math.min(100, (safeXp * 100) / fallbackTarget));
        xpProgressText.setText(getString(R.string.xp_progress_format,
                Math.min(safeXp, fallbackTarget), fallbackTarget));
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }

    private void openProfileTab() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openProfileTab();
        }
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

    private void renderListState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading && !swipeRefreshLayout.isRefreshing()
                ? View.VISIBLE : View.GONE);
        if (loading) {
            statusText.setVisibility(View.GONE);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            swipeRefreshLayout.setRefreshing(false);
            showStatus(state.getMessage(), true);
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setRefreshing(false);
            statusText.setVisibility(View.GONE);
        }
    }

    private void renderOperationState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.LOADING) {
            showStatus(getString(R.string.operation_in_progress), false);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else {
            showStatus(getString(R.string.quote_deleted), false);
            statusText.postDelayed(() -> statusText.setVisibility(View.GONE), 1200L);
        }
    }

    private void openEditQuote(Quote quote) {
        Intent intent = new Intent(requireContext(), AddQuoteActivity.class);
        intent.putExtra(AddQuoteActivity.EXTRA_QUOTE_ID, quote.getQuoteId());
        intent.putExtra(AddQuoteActivity.EXTRA_TYPE, quote.getType());
        intent.putExtra(AddQuoteActivity.EXTRA_TEXT, quote.getText());
        intent.putExtra(AddQuoteActivity.EXTRA_TITLE, quote.getTitle());
        intent.putExtra(AddQuoteActivity.EXTRA_AUTHOR, quote.getAuthor());
        intent.putExtra(AddQuoteActivity.EXTRA_CHARACTER, quote.getCharacterName());
        intent.putExtra(AddQuoteActivity.EXTRA_SEASON, quote.getSeason());
        intent.putExtra(AddQuoteActivity.EXTRA_EPISODE, quote.getEpisode());
        intent.putExtra(AddQuoteActivity.EXTRA_TAGS, quote.getTags() == null ? ""
                : quote.getTags().stream().collect(Collectors.joining(", ")));
        intent.putExtra(AddQuoteActivity.EXTRA_SPOILER, quote.isSpoiler());
        startActivity(intent);
    }

    private void openQuoteDetail(Quote quote) {
        if (quote == null || quote.getQuoteId() == null || quote.getQuoteId().trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(requireContext(), QuoteDetailActivity.class);
        intent.putExtra(QuoteDetailActivity.EXTRA_QUOTE_ID, quote.getQuoteId());
        startActivity(intent);
    }

    private void toggleLike(Quote quote) {
        if (quote == null || quote.getQuoteId() == null || quote.getQuoteId().trim().isEmpty()) {
            return;
        }
        likeViewModel.toggleLike(quote.getQuoteId());
    }

    private void toggleSave(Quote quote) {
        favoriteViewModel.toggleSaved(quote);
    }

    private void confirmDelete(Quote quote) {
        new MaterialAlertDialogBuilder(requireContext())
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

    private void showReportSheet(Quote quote) {
        ReportBottomSheetHelper.show(requireContext(),
                (reason, description) -> reportViewModel.submitReport(quote, reason, description));
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(requireContext(), error
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
