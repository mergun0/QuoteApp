package com.merg.quoteapp.ui.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.QuoteAdapter;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.ui.profile.UserProfileActivity;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.utils.ReportBottomSheetHelper;
import com.merg.quoteapp.viewmodel.DiscoverViewModel;
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.ReportViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscoverFragment extends Fragment {

    private enum QuoteFilter {
        ALL,
        MOVIE,
        SERIES,
        BOOK,
        SPOILER
    }

    private final List<Quote> allQuotes = new ArrayList<>();
    private DiscoverViewModel viewModel;
    private LikeViewModel likeViewModel;
    private FavoriteViewModel favoriteViewModel;
    private ReportViewModel reportViewModel;
    private ReportBottomSheetHelper.ReportSheetController reportSheetController;
    private QuoteAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView emptyTitleText;
    private TextView emptyDescriptionText;
    private TextView statusText;
    private TextInputEditText searchInput;
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
    private Map<String, Boolean> renderedSavedStates = new HashMap<>();
    private Map<String, Boolean> renderedSaveLoadingStates = new HashMap<>();
    private Map<String, Long> renderedSaveCounts = new HashMap<>();
    private String searchQuery = "";
    private QuoteFilter selectedFilter = QuoteFilter.ALL;
    private boolean firstResume = true;

    public DiscoverFragment() {
        super(R.layout.fragment_discover);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilters(view);

        viewModel = new ViewModelProvider(this).get(DiscoverViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        viewModel.getQuotes().observe(getViewLifecycleOwner(), this::renderQuotes);
        viewModel.getListState().observe(getViewLifecycleOwner(), this::renderListState);
        viewModel.getOperationState().observe(
                getViewLifecycleOwner(), this::renderOperationState);
        likeViewModel.getLikedStates().observe(getViewLifecycleOwner(), this::renderLikedStates);
        likeViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderLikeLoadingStates);
        likeViewModel.getLikeCounts().observe(getViewLifecycleOwner(), this::renderLikeCounts);
        likeViewModel.getLoadingState().observe(getViewLifecycleOwner(), this::renderLikeState);
        favoriteViewModel.getSavedStates().observe(getViewLifecycleOwner(), this::renderSavedStates);
        favoriteViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderSaveLoadingStates);
        favoriteViewModel.getFavoriteCounts().observe(
                getViewLifecycleOwner(), this::renderSaveCounts);
        favoriteViewModel.getOperationState().observe(
                getViewLifecycleOwner(), this::renderFavoriteState);
        observeReportState();
        viewModel.loadQuotes();

        swipeRefreshLayout.setColorSchemeResources(
                R.color.quote_primary,
                R.color.quote_secondary);
        swipeRefreshLayout.setOnRefreshListener(viewModel::refreshQuotes);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        refreshInteractionStates();
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerDiscover);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshDiscover);
        progressBar = view.findViewById(R.id.progressDiscover);
        emptyLayout = view.findViewById(R.id.layoutEmptyDiscover);
        emptyTitleText = view.findViewById(R.id.textEmptyDiscoverTitle);
        emptyDescriptionText = view.findViewById(R.id.textEmptyDiscoverDescription);
        statusText = view.findViewById(R.id.textDiscoverStatus);
        searchInput = view.findViewById(R.id.editDiscoverSearch);
    }

    private void setupRecyclerView() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
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
            public void onUserProfile(String userId) {
                openUserProfile(userId);
            }

            @Override
            public void onReport(Quote quote) {
                showReportSheet(quote);
            }

            @Override
            public void onSave(Quote quote) {
                toggleSave(quote);
            }
        }, true, true, currentUserId, R.layout.item_quote_home);
        adapter.setLikeActionsEnabled(true);
        adapter.setSaveActionsEnabled(true);
        adapter.setReportActionsEnabled(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
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
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupDiscoverFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty()
                    ? R.id.chipDiscoverAll : checkedIds.get(0);
            if (checkedId == R.id.chipDiscoverMovie) {
                selectedFilter = QuoteFilter.MOVIE;
            } else if (checkedId == R.id.chipDiscoverSeries) {
                selectedFilter = QuoteFilter.SERIES;
            } else if (checkedId == R.id.chipDiscoverBook) {
                selectedFilter = QuoteFilter.BOOK;
            } else if (checkedId == R.id.chipDiscoverSpoiler) {
                selectedFilter = QuoteFilter.SPOILER;
            } else {
                selectedFilter = QuoteFilter.ALL;
            }
            applyFilters();
        });
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
            favoriteViewModel.loadFavoriteCounts(allQuotes);
        }
        applyFilters();
    }

    private void refreshInteractionStates() {
        if (allQuotes.isEmpty()) {
            return;
        }
        if (likeViewModel != null) {
            likeViewModel.refreshLikedStates(allQuotes);
            likeViewModel.refreshLikeCounts(allQuotes);
        }
        if (favoriteViewModel != null) {
            favoriteViewModel.refreshSavedStates(allQuotes);
        }
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
        reportViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                if (reportSheetController != null) {
                    reportSheetController.setLoading(true);
                }
                showStatus(getString(R.string.operation_in_progress), false);
            }
        });
        reportViewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                dismissReportSheet();
                showStatus(getString(R.string.report_sent), false);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getAlreadyReported().observe(getViewLifecycleOwner(), already -> {
            if (Boolean.TRUE.equals(already)) {
                resetReportSheetLoading();
                showReportSheetMessage(getString(R.string.report_already_sent), true);
                showStatus(getString(R.string.report_already_sent), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getDailyLimitReached().observe(getViewLifecycleOwner(), reached -> {
            if (Boolean.TRUE.equals(reached)) {
                resetReportSheetLoading();
                showReportSheetMessage(getString(R.string.report_daily_limit_reached), true);
                showStatus(getString(R.string.report_daily_limit_reached), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                resetReportSheetLoading();
                showReportSheetMessage(message, true);
                showStatus(message, true);
                reportViewModel.clearResultStates();
            }
        });
    }

    private void applyFilters() {
        List<Quote> filteredQuotes = allQuotes.stream()
                .filter(this::matchesFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
        adapter.submitList(filteredQuotes);

        boolean empty = filteredQuotes.isEmpty();
        boolean filtersActive = !searchQuery.isEmpty() || selectedFilter != QuoteFilter.ALL;
        emptyTitleText.setText(filtersActive
                ? R.string.no_filter_results : R.string.discover_empty);
        emptyDescriptionText.setText(filtersActive
                ? R.string.no_filter_results_detail : R.string.discover_empty_detail);
        emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesFilter(Quote quote) {
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
                safe(quote.getUsername()),
                safe(quote.getAuthor()),
                quote.getTags() == null ? "" : String.join(" ", quote.getTags()));
        return searchable.toLowerCase(new Locale("tr", "TR")).contains(searchQuery);
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
                : String.join(", ", quote.getTags()));
        intent.putExtra(AddQuoteActivity.EXTRA_SPOILER, quote.isSpoiler());
        startActivity(intent);
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

    private void openUserProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId != null && currentUserId.equals(userId)
                && requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openProfileTab();
            return;
        }
        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, userId);
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

    private void shareQuote(Quote quote) {
        String shareText = "“" + safe(quote.getText()) + "”\n\n"
                + safe(quote.getTitle()) + " — " + safe(quote.getAuthor());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void showReportSheet(Quote quote) {
        reportSheetController = ReportBottomSheetHelper.show(requireContext(),
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

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(requireContext(), error
                ? R.color.home_v2_error : R.color.home_v2_secondary));
        statusText.setVisibility(View.VISIBLE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
