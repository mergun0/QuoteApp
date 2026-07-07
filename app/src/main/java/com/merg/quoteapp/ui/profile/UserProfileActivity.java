package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.QuoteAdapter;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserProfileData;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.ui.quote.AddQuoteActivity;
import com.merg.quoteapp.ui.quote.QuoteDetailActivity;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.UserProfileViewModel;

import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";

    private UserProfileViewModel viewModel;
    private LikeViewModel likeViewModel;
    private QuoteAdapter adapter;
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
    private String profileUserId;
    private boolean ownProfile;
    private boolean firstResume = true;
    private Map<String, Long> renderedLikeCounts = new HashMap<>();

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
        setupActions();

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        viewModel.getProfile().observe(this, this::renderProfile);
        viewModel.getState().observe(this, this::renderState);
        viewModel.getOperationState().observe(this, this::renderOperationState);
        viewModel.getLoadMoreState().observe(this, this::renderLoadMoreState);
        viewModel.getHasMore().observe(this, this::renderHasMore);
        likeViewModel.getLikeCounts().observe(this, this::renderLikeCounts);
        viewModel.loadProfile(profileUserId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
        } else if (viewModel != null) {
            viewModel.refreshProfile();
        }
    }

    private void bindViews() {
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
                // Favoriler sonraki sürümde etkinleştirilecek.
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
                // Bu ekranda kullanıcı satırı gösterilmez.
            }
        }, false, true, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                super.onScrolled(view, dx, dy);
                LinearLayoutManager manager = (LinearLayoutManager) view.getLayoutManager();
                if (dy > 0 && manager != null
                        && manager.findLastVisibleItemPosition() >= adapter.getItemCount() - 4) {
                    viewModel.loadMoreQuotes();
                }
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
        likeViewModel.loadLikeCounts(profile.getQuotes());
        boolean empty = profile.getQuotes() == null || profile.getQuotes().isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
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
            contentLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            contentLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            errorText.setText(state.getMessage());
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
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
        noMoreText.setVisibility(Boolean.FALSE.equals(hasMore) && hasQuotes
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
}
