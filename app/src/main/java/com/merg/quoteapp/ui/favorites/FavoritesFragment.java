package com.merg.quoteapp.ui.favorites;

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
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.QuoteViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class FavoritesFragment extends Fragment {

    private QuoteAdapter adapter;
    private FavoriteViewModel favoriteViewModel;
    private LikeViewModel likeViewModel;
    private QuoteViewModel quoteViewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView statusText;
    private TextView countText;
    private TextView emptyTitleText;
    private TextView emptyDescriptionText;
    private TextInputEditText searchInput;
    private List<Quote> allSavedQuotes = new ArrayList<>();
    private String searchQuery = "";
    private String selectedFilter = "ALL";
    private Map<String, Boolean> renderedSavedStates = new HashMap<>();
    private Map<String, Boolean> renderedSaveLoadingStates = new HashMap<>();
    private Map<String, Boolean> renderedLikedStates = new HashMap<>();
    private Map<String, Boolean> renderedLikeLoadingStates = new HashMap<>();
    private Map<String, Long> renderedLikeCounts = new HashMap<>();
    private Map<String, Long> renderedSaveCounts = new HashMap<>();
    private boolean firstResume = true;

    public FavoritesFragment() {
        super(R.layout.fragment_favorites);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupSearchAndFilters(view);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        quoteViewModel = new ViewModelProvider(this).get(QuoteViewModel.class);

        favoriteViewModel.getSavedQuotes().observe(getViewLifecycleOwner(), this::renderQuotes);
        favoriteViewModel.getListState().observe(getViewLifecycleOwner(), this::renderListState);
        favoriteViewModel.getOperationState().observe(getViewLifecycleOwner(), this::renderFavoriteState);
        favoriteViewModel.getSavedStates().observe(getViewLifecycleOwner(), this::renderSavedStates);
        favoriteViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderSaveLoadingStates);
        favoriteViewModel.getFavoriteCounts().observe(
                getViewLifecycleOwner(), this::renderSaveCounts);
        likeViewModel.getLikedStates().observe(getViewLifecycleOwner(), this::renderLikedStates);
        likeViewModel.getItemLoadingStates().observe(
                getViewLifecycleOwner(), this::renderLikeLoadingStates);
        likeViewModel.getLikeCounts().observe(getViewLifecycleOwner(), this::renderLikeCounts);
        likeViewModel.getLoadingState().observe(getViewLifecycleOwner(), this::renderLikeState);
        quoteViewModel.getOperationState().observe(getViewLifecycleOwner(), this::renderQuoteOperationState);

        swipeRefreshLayout.setColorSchemeResources(R.color.home_v2_primary, R.color.home_v2_secondary);
        swipeRefreshLayout.setOnRefreshListener(favoriteViewModel::refreshSavedQuotes);
        favoriteViewModel.loadSavedQuotes();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        if (favoriteViewModel != null) {
            favoriteViewModel.refreshSavedQuotes();
        }
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerFavorites);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshFavorites);
        progressBar = view.findViewById(R.id.progressFavorites);
        emptyLayout = view.findViewById(R.id.layoutEmptyFavorites);
        statusText = view.findViewById(R.id.textFavoritesStatus);
        countText = view.findViewById(R.id.textFavoritesCount);
        emptyTitleText = view.findViewById(R.id.textEmptyFavoritesTitle);
        emptyDescriptionText = view.findViewById(R.id.textEmptyFavoritesDescription);
        searchInput = view.findViewById(R.id.editFavoritesSearch);
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
                likeViewModel.toggleLike(quote.getQuoteId());
            }

            @Override
            public void onSave(Quote quote) {
                favoriteViewModel.toggleSaved(quote);
            }

            @Override
            public void onOpen(Quote quote) {
                Intent intent = new Intent(requireContext(), QuoteDetailActivity.class);
                intent.putExtra(QuoteDetailActivity.EXTRA_QUOTE_ID, quote.getQuoteId());
                startActivity(intent);
            }

            @Override
            public void onUserProfile(String userId) {
                openUserProfile(userId);
            }
        }, true, true, currentUserId, R.layout.item_quote_home);
        adapter.setLikeActionsEnabled(true);
        adapter.setSaveActionsEnabled(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchAndFilters(View view) {
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No-op.
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No-op.
                }
            });
        }
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFavoritesFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chipFavoritesAll : checkedIds.get(0);
            if (checkedId == R.id.chipFavoritesMovie) {
                selectedFilter = "Film";
            } else if (checkedId == R.id.chipFavoritesSeries) {
                selectedFilter = "Dizi";
            } else if (checkedId == R.id.chipFavoritesBook) {
                selectedFilter = "Kitap";
            } else if (checkedId == R.id.chipFavoritesSpoiler) {
                selectedFilter = "SPOILER";
            } else {
                selectedFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void renderQuotes(List<Quote> quotes) {
        allSavedQuotes = quotes == null ? new ArrayList<>() : new ArrayList<>(quotes);
        countText.setText(getString(R.string.favorites_count_format, allSavedQuotes.size()));
        applyFilters();
        if (quotes != null) {
            favoriteViewModel.loadSavedStates(quotes);
            favoriteViewModel.loadFavoriteCounts(quotes);
            likeViewModel.loadLikedStates(quotes);
            likeViewModel.loadLikeCounts(quotes);
        }
    }

    private void applyFilters() {
        List<Quote> filtered = allSavedQuotes.stream()
                .filter(this::matchesFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        renderEmptyText();
    }

    private boolean matchesFilter(Quote quote) {
        if (quote == null) {
            return false;
        }
        if ("ALL".equals(selectedFilter)) {
            return true;
        }
        if ("SPOILER".equals(selectedFilter)) {
            return quote.isSpoiler();
        }
        return selectedFilter.equals(quote.getType());
    }

    private boolean matchesSearch(Quote quote) {
        if (quote == null || searchQuery == null || searchQuery.trim().isEmpty()) {
            return true;
        }
        String query = searchQuery.trim().toLowerCase(Locale.getDefault());
        return contains(quote.getText(), query)
                || contains(quote.getTitle(), query)
                || contains(quote.getAuthor(), query)
                || contains(quote.getCharacterName(), query)
                || contains(quote.getUsername(), query)
                || (quote.getTags() != null && quote.getTags().stream()
                .anyMatch(tag -> contains(tag, query)));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private void renderEmptyText() {
        if (allSavedQuotes.isEmpty()) {
            emptyTitleText.setText(R.string.favorites_empty);
            emptyDescriptionText.setText(R.string.favorites_empty_detail);
        } else {
            emptyTitleText.setText(R.string.no_filter_results);
            emptyDescriptionText.setText(R.string.no_filter_results_detail);
        }
    }

    private void renderListState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading && !swipeRefreshLayout.isRefreshing()
                ? View.VISIBLE : View.GONE);
        if (state.getStatus() == QuoteState.Status.ERROR) {
            swipeRefreshLayout.setRefreshing(false);
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == QuoteState.Status.SUCCESS) {
            swipeRefreshLayout.setRefreshing(false);
            statusText.setVisibility(View.GONE);
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

    private void renderFavoriteState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderLikeState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderQuoteOperationState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
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
                        quoteViewModel.deleteQuote(quote.getQuoteId()))
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
