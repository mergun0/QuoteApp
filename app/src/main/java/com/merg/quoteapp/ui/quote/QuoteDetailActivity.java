package com.merg.quoteapp.ui.quote;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.QuoteDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class QuoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUOTE_ID = "quoteId";

    private QuoteDetailViewModel viewModel;
    private LikeViewModel likeViewModel;
    private Quote currentQuote;
    private ScrollView contentScroll;
    private ProgressBar progressBar;
    private TextView errorText;
    private TextView statusText;
    private LinearLayout quoteContainer;
    private LinearLayout spoilerContainer;
    private LinearLayout ownerActions;
    private MaterialButton deleteButton;
    private MaterialButton favoriteButton;
    private long currentLikeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote_detail);
        bindStaticViews();

        MaterialToolbar toolbar = findViewById(R.id.toolbarQuoteDetail);
        toolbar.setNavigationOnClickListener(view -> finish());

        String quoteId = getIntent().getStringExtra(EXTRA_QUOTE_ID);
        viewModel = new ViewModelProvider(this).get(QuoteDetailViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        viewModel.getQuote().observe(this, this::renderQuote);
        viewModel.getState().observe(this, this::renderLoadState);
        viewModel.getDeleteState().observe(this, this::renderDeleteState);
        likeViewModel.getLikedStates().observe(this, this::renderLikedState);
        likeViewModel.getItemLoadingStates().observe(this, this::renderLikeLoadingState);
        likeViewModel.getLikeCount().observe(this, this::renderLikeCount);
        likeViewModel.getLoadingState().observe(this, this::renderLikeState);
        viewModel.loadQuote(quoteId);
    }

    private void bindStaticViews() {
        contentScroll = findViewById(R.id.scrollQuoteDetail);
        progressBar = findViewById(R.id.progressQuoteDetail);
        errorText = findViewById(R.id.textQuoteDetailError);
        statusText = findViewById(R.id.textDetailStatus);
        quoteContainer = findViewById(R.id.layoutDetailQuote);
        spoilerContainer = findViewById(R.id.layoutDetailSpoilerHidden);
        ownerActions = findViewById(R.id.layoutDetailOwnerActions);
        deleteButton = findViewById(R.id.buttonDetailDelete);
        favoriteButton = findViewById(R.id.buttonDetailFavorite);

        findViewById(R.id.buttonShowDetailSpoiler).setOnClickListener(view -> {
            spoilerContainer.setVisibility(View.GONE);
            quoteContainer.setVisibility(View.VISIBLE);
        });
        favoriteButton.setOnClickListener(view -> toggleLike());
        findViewById(R.id.buttonDetailShare).setOnClickListener(view -> shareQuote());
        findViewById(R.id.buttonDetailEdit).setOnClickListener(view -> editQuote());
        deleteButton.setOnClickListener(view -> confirmDelete());
    }

    private void renderQuote(Quote quote) {
        currentQuote = quote;
        ((TextView) findViewById(R.id.textDetailType))
                .setText(safe(quote.getType()).toUpperCase(new Locale("tr", "TR")));
        ((TextView) findViewById(R.id.textDetailUsername))
                .setText("@" + safe(quote.getUsername()));
        ((TextView) findViewById(R.id.textDetailTitle)).setText(safe(quote.getTitle()));
        ((TextView) findViewById(R.id.textDetailAuthor)).setText(safe(quote.getAuthor()));
        ((TextView) findViewById(R.id.textDetailQuote))
                .setText("“" + safe(quote.getText()) + "”");

        setOptionalText(
                findViewById(R.id.textDetailCharacter),
                quote.getCharacterName(),
                getString(R.string.character_label, safe(quote.getCharacterName())));

        boolean hasSeriesInfo = "Dizi".equals(quote.getType())
                && !safe(quote.getSeason()).isEmpty()
                && !safe(quote.getEpisode()).isEmpty();
        TextView seriesText = findViewById(R.id.textDetailSeries);
        seriesText.setVisibility(hasSeriesInfo ? View.VISIBLE : View.GONE);
        if (hasSeriesInfo) {
            seriesText.setText(getString(
                    R.string.series_detail, quote.getSeason(), quote.getEpisode()));
        }

        TextView tagsText = findViewById(R.id.textDetailTags);
        if (quote.getTags() == null || quote.getTags().isEmpty()) {
            tagsText.setVisibility(View.GONE);
        } else {
            tagsText.setText(quote.getTags().stream()
                    .map(tag -> "#" + tag)
                    .collect(Collectors.joining("  ")));
            tagsText.setVisibility(View.VISIBLE);
        }

        ((TextView) findViewById(R.id.textDetailCreatedAt))
                .setText(formatCreatedAt(quote.getCreatedAt()));
        findViewById(R.id.textDetailSpoilerBadge)
                .setVisibility(quote.isSpoiler() ? View.VISIBLE : View.GONE);
        quoteContainer.setVisibility(quote.isSpoiler() ? View.GONE : View.VISIBLE);
        spoilerContainer.setVisibility(quote.isSpoiler() ? View.VISIBLE : View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = currentUserId != null && currentUserId.equals(quote.getUserId());
        ownerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        likeViewModel.loadLikeState(quote.getQuoteId());
    }

    private void renderLoadState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            contentScroll.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            contentScroll.setVisibility(View.GONE);
            errorText.setText(state.getMessage());
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
            contentScroll.setVisibility(View.VISIBLE);
        }
    }

    private void renderDeleteState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        deleteButton.setEnabled(!loading);
        if (loading) {
            showStatus(getString(R.string.operation_in_progress), false);
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else {
            showStatus(getString(R.string.quote_deleted), false);
            statusText.postDelayed(this::finish, 700L);
        }
    }

    private void renderLikedState(Map<String, Boolean> likedStates) {
        if (currentQuote == null || likedStates == null) {
            return;
        }
        Boolean liked = likedStates.get(currentQuote.getQuoteId());
        if (liked != null) {
            renderFavoriteButton(liked, false);
        }
    }

    private void renderLikeLoadingState(Map<String, Boolean> loadingStates) {
        if (currentQuote == null || loadingStates == null) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(loadingStates.get(currentQuote.getQuoteId()));
        Boolean liked = likeViewModel.getLikedStates().getValue() == null
                ? false : likeViewModel.getLikedStates().getValue().get(currentQuote.getQuoteId());
        renderFavoriteButton(Boolean.TRUE.equals(liked), loading);
    }

    private void renderLikeState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderLikeCount(Long count) {
        currentLikeCount = count == null ? 0L : count;
        boolean liked = false;
        if (currentQuote != null && likeViewModel.getLikedStates().getValue() != null) {
            liked = Boolean.TRUE.equals(
                    likeViewModel.getLikedStates().getValue().get(currentQuote.getQuoteId()));
        }
        renderFavoriteButton(liked, false);
    }

    private void renderFavoriteButton(boolean liked, boolean loading) {
        int color = ContextCompat.getColor(this, liked
                ? R.color.quote_status_error : R.color.quote_text_secondary);
        favoriteButton.setEnabled(!loading);
        favoriteButton.setAlpha(loading ? 0.55f : 1f);
        favoriteButton.setSelected(liked);
        favoriteButton.setText(currentLikeCount > 0
                ? getString(R.string.like_button_with_count, currentLikeCount)
                : getString(R.string.favorite));
        favoriteButton.setTextColor(color);
        favoriteButton.setIconTint(ColorStateList.valueOf(color));
    }

    private void editQuote() {
        if (currentQuote == null) {
            return;
        }
        Intent intent = new Intent(this, AddQuoteActivity.class);
        intent.putExtra(AddQuoteActivity.EXTRA_QUOTE_ID, currentQuote.getQuoteId());
        intent.putExtra(AddQuoteActivity.EXTRA_TYPE, currentQuote.getType());
        intent.putExtra(AddQuoteActivity.EXTRA_TEXT, currentQuote.getText());
        intent.putExtra(AddQuoteActivity.EXTRA_TITLE, currentQuote.getTitle());
        intent.putExtra(AddQuoteActivity.EXTRA_AUTHOR, currentQuote.getAuthor());
        intent.putExtra(AddQuoteActivity.EXTRA_CHARACTER, currentQuote.getCharacterName());
        intent.putExtra(AddQuoteActivity.EXTRA_SEASON, currentQuote.getSeason());
        intent.putExtra(AddQuoteActivity.EXTRA_EPISODE, currentQuote.getEpisode());
        intent.putExtra(AddQuoteActivity.EXTRA_TAGS, currentQuote.getTags() == null ? ""
                : String.join(", ", currentQuote.getTags()));
        intent.putExtra(AddQuoteActivity.EXTRA_SPOILER, currentQuote.isSpoiler());
        startActivity(intent);
        finish();
    }

    private void confirmDelete() {
        if (currentQuote == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_quote_title)
                .setMessage(R.string.delete_quote_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.deleteQuote(currentQuote.getQuoteId()))
                .show();
    }

    private void shareQuote() {
        if (currentQuote == null) {
            return;
        }
        String shareText = "“" + safe(currentQuote.getText()) + "”\n\n"
                + safe(currentQuote.getTitle()) + " — " + safe(currentQuote.getAuthor());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void toggleLike() {
        if (currentQuote == null || currentQuote.getQuoteId() == null
                || currentQuote.getQuoteId().trim().isEmpty()) {
            return;
        }
        likeViewModel.toggleLike(currentQuote.getQuoteId());
    }

    private String formatCreatedAt(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        SimpleDateFormat formatter =
                new SimpleDateFormat("d MMMM yyyy • HH:mm", new Locale("tr", "TR"));
        return getString(R.string.created_at) + ": " + formatter.format(timestamp.toDate());
    }

    private void setOptionalText(TextView view, String value, String displayValue) {
        boolean visible = value != null && !value.trim().isEmpty();
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            view.setText(displayValue);
        }
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(getColor(error
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
