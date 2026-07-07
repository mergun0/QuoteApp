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
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.QuoteAdapter;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.utils.ReportBottomSheetHelper;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.ProfileViewModel;
import com.merg.quoteapp.viewmodel.QuoteViewModel;
import com.merg.quoteapp.viewmodel.ReportViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private QuoteAdapter adapter;
    private QuoteViewModel viewModel;
    private LikeViewModel likeViewModel;
    private ReportViewModel reportViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView statusText;
    private TextView greetingText;
    private TextView emptyTitleText;
    private TextView emptyDescriptionText;
    private TextInputEditText searchInput;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final List<Quote> allQuotes = new ArrayList<>();
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
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
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        viewModel.getQuotes().observe(getViewLifecycleOwner(), this::renderQuotes);
        viewModel.getListState().observe(getViewLifecycleOwner(), this::renderListState);
        viewModel.getOperationState().observe(getViewLifecycleOwner(), this::renderOperationState);
        likeViewModel.getLikedStates().observe(getViewLifecycleOwner(), this::renderLikedStates);
        likeViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderLikeLoadingStates);
        likeViewModel.getLikeCounts().observe(getViewLifecycleOwner(), this::renderLikeCounts);
        likeViewModel.getLoadingState().observe(getViewLifecycleOwner(), this::renderLikeState);
        observeReportState();
        viewModel.loadCurrentUserQuotes();
        setupSearch();
        setupFilters(view);
        setupGreeting();

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
        emptyLayout = view.findViewById(R.id.layoutEmptyQuotes);
        statusText = view.findViewById(R.id.textQuotesStatus);
        greetingText = view.findViewById(R.id.textHomeGreeting);
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
        });
        adapter.setLikeActionsEnabled(true);
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
            greetingText.setText(getString(R.string.home_greeting, fallback));
        }

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getProfile().observe(getViewLifecycleOwner(), this::renderGreeting);
        profileViewModel.loadProfile();
    }

    private void renderGreeting(ProfileStats profile) {
        if (profile != null && profile.getUsername() != null
                && !profile.getUsername().trim().isEmpty()) {
            greetingText.setText(getString(R.string.home_greeting, profile.getUsername()));
        }
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
