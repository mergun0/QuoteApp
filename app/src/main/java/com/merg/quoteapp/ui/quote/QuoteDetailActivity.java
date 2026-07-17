package com.merg.quoteapp.ui.quote;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.ui.profile.UserProfileActivity;
import com.merg.quoteapp.utils.ReportBottomSheetHelper;
import com.merg.quoteapp.viewmodel.FavoriteViewModel;
import com.merg.quoteapp.viewmodel.LikeViewModel;
import com.merg.quoteapp.viewmodel.QuoteDetailViewModel;
import com.merg.quoteapp.viewmodel.ReportViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class QuoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUOTE_ID = "quoteId";

    private QuoteDetailViewModel viewModel;
    private LikeViewModel likeViewModel;
    private FavoriteViewModel favoriteViewModel;
    private ReportViewModel reportViewModel;
    private ReportBottomSheetHelper.ReportSheetController reportSheetController;
    private Quote currentQuote;
    private ScrollView contentScroll;
    private ProgressBar progressBar;
    private TextView errorText;
    private TextView statusText;
    private LinearLayout quoteContainer;
    private LinearLayout spoilerContainer;
    private LinearLayout commentsPreview;
    private LinearLayout ownerActions;
    private MaterialButton deleteButton;
    private MaterialButton favoriteButton;
    private MaterialButton saveButton;
    private TextView usernameText;
    private long currentLikeCount;
    private long currentSaveCount;
    private boolean currentLiked;
    private boolean currentSaved;
    private boolean currentUserCanManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote_detail);
        bindStaticViews();

        MaterialToolbar toolbar = findViewById(R.id.toolbarQuoteDetail);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionQuoteDetailMore) {
                showActionsSheet();
                return true;
            }
            return false;
        });

        String quoteId = getIntent().getStringExtra(EXTRA_QUOTE_ID);
        viewModel = new ViewModelProvider(this).get(QuoteDetailViewModel.class);
        likeViewModel = new ViewModelProvider(this).get(LikeViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        viewModel.getQuote().observe(this, this::renderQuote);
        viewModel.getState().observe(this, this::renderLoadState);
        viewModel.getDeleteState().observe(this, this::renderDeleteState);
        likeViewModel.getLikedStates().observe(this, this::renderLikedState);
        likeViewModel.getItemLoadingStates().observe(this, this::renderLikeLoadingState);
        likeViewModel.getLikeCount().observe(this, this::renderLikeCount);
        likeViewModel.getLoadingState().observe(this, this::renderLikeState);
        favoriteViewModel.getSavedStates().observe(this, this::renderSavedState);
        favoriteViewModel.getItemLoadingStates().observe(this, this::renderSaveLoadingState);
        favoriteViewModel.getFavoriteCount().observe(this, this::renderSaveCount);
        favoriteViewModel.getOperationState().observe(this, this::renderFavoriteState);
        observeReportState();
        viewModel.loadQuote(quoteId);
    }

    private void bindStaticViews() {
        contentScroll = findViewById(R.id.scrollQuoteDetail);
        progressBar = findViewById(R.id.progressQuoteDetail);
        errorText = findViewById(R.id.textQuoteDetailError);
        statusText = findViewById(R.id.textDetailStatus);
        quoteContainer = findViewById(R.id.layoutDetailQuote);
        spoilerContainer = findViewById(R.id.layoutDetailSpoilerHidden);
        commentsPreview = findViewById(R.id.layoutDetailCommentsPreview);
        ownerActions = findViewById(R.id.layoutDetailOwnerActions);
        deleteButton = findViewById(R.id.buttonDetailDelete);
        favoriteButton = findViewById(R.id.buttonDetailFavorite);
        saveButton = findViewById(R.id.buttonDetailSave);
        usernameText = findViewById(R.id.textDetailUsername);

        findViewById(R.id.buttonShowDetailSpoiler).setOnClickListener(view -> {
            spoilerContainer.setVisibility(View.GONE);
            quoteContainer.setVisibility(View.VISIBLE);
        });
        favoriteButton.setOnClickListener(view -> toggleLike());
        saveButton.setOnClickListener(view -> toggleSave());
        findViewById(R.id.buttonDetailComment).setOnClickListener(view -> scrollToComments());
        findViewById(R.id.buttonDetailShare).setOnClickListener(view -> shareQuote());
        findViewById(R.id.buttonDetailReport).setOnClickListener(view -> showReportSheet());
        findViewById(R.id.buttonDetailEdit).setOnClickListener(view -> editQuote());
        deleteButton.setOnClickListener(view -> confirmDelete());
    }

    private void renderQuote(Quote quote) {
        currentQuote = quote;
        ((TextView) findViewById(R.id.textDetailType))
                .setText(categoryLabel(quote.getType()));
        usernameText.setText(displayUsername(quote.getUsername()));
        boolean hasUserId = quote.getUserId() != null && !quote.getUserId().trim().isEmpty();
        usernameText.setEnabled(hasUserId);
        usernameText.setOnClickListener(hasUserId ? view -> openUserProfile(quote.getUserId()) : null);
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
                .setVisibility(View.GONE);
        quoteContainer.setVisibility(quote.isSpoiler() ? View.GONE : View.VISIBLE);
        spoilerContainer.setVisibility(quote.isSpoiler() ? View.VISIBLE : View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentUserCanManage = currentUserId != null && currentUserId.equals(quote.getUserId());
        ownerActions.setVisibility(View.GONE);
        likeViewModel.loadLikeState(quote.getQuoteId());
        favoriteViewModel.loadQuoteDetailState(quote);
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
            currentLiked = liked;
            renderFavoriteButton(currentLiked, false);
        }
    }

    private void renderLikeLoadingState(Map<String, Boolean> loadingStates) {
        if (currentQuote == null || loadingStates == null) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(loadingStates.get(currentQuote.getQuoteId()));
        Boolean liked = likeViewModel.getLikedStates().getValue() == null
                ? false : likeViewModel.getLikedStates().getValue().get(currentQuote.getQuoteId());
        currentLiked = Boolean.TRUE.equals(liked);
        renderFavoriteButton(currentLiked, loading);
    }

    private void renderLikeState(QuoteState state) {
        if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        }
    }

    private void renderSavedState(Map<String, Boolean> savedStates) {
        if (currentQuote == null || savedStates == null) {
            return;
        }
        currentSaved = Boolean.TRUE.equals(savedStates.get(currentQuote.getQuoteId()));
        renderSaveButton(false);
    }

    private void renderSaveLoadingState(Map<String, Boolean> loadingStates) {
        if (currentQuote == null || loadingStates == null) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(loadingStates.get(currentQuote.getQuoteId()));
        renderSaveButton(loading);
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
                showStatus(getString(R.string.report_already_sent), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getDailyLimitReached().observe(this, reached -> {
            if (Boolean.TRUE.equals(reached)) {
                resetReportSheetLoading();
                showStatus(getString(R.string.report_daily_limit_reached), true);
                reportViewModel.clearResultStates();
            }
        });
        reportViewModel.getError().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                resetReportSheetLoading();
                showStatus(message, true);
                reportViewModel.clearResultStates();
            }
        });
    }

    private void renderLikeCount(Long count) {
        currentLikeCount = count == null ? 0L : count;
        if (currentQuote != null && likeViewModel.getLikedStates().getValue() != null) {
            currentLiked = Boolean.TRUE.equals(
                    likeViewModel.getLikedStates().getValue().get(currentQuote.getQuoteId()));
        }
        renderFavoriteButton(currentLiked, false);
    }

    private void renderSaveCount(Long count) {
        currentSaveCount = count == null ? 0L : Math.max(0L, count);
        renderSaveButton(false);
    }

    private void renderFavoriteButton(boolean liked, boolean loading) {
        int color = ContextCompat.getColor(this, liked
                ? R.color.home_v2_error : R.color.home_v2_text_secondary);
        ColorStateList tint = ColorStateList.valueOf(color);
        favoriteButton.setEnabled(!loading);
        favoriteButton.setAlpha(loading ? 0.55f : 1f);
        favoriteButton.setSelected(liked);
        favoriteButton.setText(String.valueOf(currentLikeCount));
        favoriteButton.setTextColor(color);
        favoriteButton.setIconTint(tint);
        favoriteButton.setStrokeColor(tint);
    }

    private void renderSaveButton(boolean loading) {
        int color = ContextCompat.getColor(this, currentSaved
                ? R.color.home_v2_accent : R.color.home_v2_text_secondary);
        ColorStateList tint = ColorStateList.valueOf(color);
        saveButton.setEnabled(!loading);
        saveButton.setAlpha(loading ? 0.55f : 1f);
        saveButton.setSelected(currentSaved);
        saveButton.setText(String.valueOf(currentSaveCount));
        saveButton.setTextColor(color);
        saveButton.setIconTint(tint);
        saveButton.setStrokeColor(tint);
    }

    private void showActionsSheet() {
        if (currentQuote == null) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(24), dp(24), dp(24));
        container.setBackgroundResource(R.drawable.bg_quote_detail_bottom_sheet);

        TextView title = new TextView(this);
        title.setText(R.string.quote_detail_actions_title);
        title.setTextColor(ContextCompat.getColor(this, R.color.home_v2_text_primary));
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        container.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addSheetAction(container, R.string.copy_quote, false, view -> {
            dialog.dismiss();
            copyQuote();
        });
        if (currentUserCanManage) {
            addSheetAction(container, R.string.quote_detail_action_edit, false, view -> {
                dialog.dismiss();
                editQuote();
            });
            addSheetAction(container, R.string.quote_detail_action_delete, true, view -> {
                dialog.dismiss();
                confirmDelete();
            });
        } else if (canReportCurrentQuote()) {
            addSheetAction(container, R.string.quote_detail_action_report, true, view -> {
                dialog.dismiss();
                showReportSheet();
            });
        }
        dialog.setContentView(container);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void addSheetAction(LinearLayout container, int textRes, boolean danger,
                                View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(textRes);
        row.setTextColor(ContextCompat.getColor(this,
                danger ? R.color.home_v2_error : R.color.home_v2_text_primary));
        row.setTextSize(16);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(48));
        row.setPadding(dp(8), dp(12), dp(8), dp(12));
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        container.addView(row, params);
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

    private void copyQuote() {
        if (currentQuote == null) {
            return;
        }
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        clipboardManager.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.copy_quote), safe(currentQuote.getText())));
        Snackbar.make(contentScroll, R.string.quote_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void toggleLike() {
        if (currentQuote == null || currentQuote.getQuoteId() == null
                || currentQuote.getQuoteId().trim().isEmpty()) {
            return;
        }
        likeViewModel.toggleLike(currentQuote.getQuoteId());
    }

    private void toggleSave() {
        if (currentQuote == null) {
            return;
        }
        favoriteViewModel.toggleSaved(currentQuote);
    }

    private void showReportSheet() {
        if (currentQuote == null) {
            return;
        }
        reportSheetController = ReportBottomSheetHelper.show(this,
                (reason, description) ->
                        reportViewModel.submitReport(currentQuote, reason, description));
    }

    private void dismissReportSheet() {
        if (reportSheetController != null && reportSheetController.isShowing()) {
            reportSheetController.dismiss();
        }
        reportSheetController = null;
    }

    private void resetReportSheetLoading() {
        if (reportSheetController != null && reportSheetController.isShowing()) {
            reportSheetController.setLoading(false);
        }
    }

    private boolean canReportCurrentQuote() {
        if (currentQuote == null || isBlank(currentQuote.getUserId())) {
            return false;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        return !isBlank(currentUserId) && !currentUserId.equals(currentQuote.getUserId());
    }

    private void scrollToComments() {
        if (contentScroll == null || commentsPreview == null) {
            return;
        }
        contentScroll.post(() -> contentScroll.smoothScrollTo(0, commentsPreview.getTop()));
    }

    private String formatCreatedAt(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        SimpleDateFormat formatter =
                new SimpleDateFormat("d MMMM yyyy • HH:mm", new Locale("tr", "TR"));
        return getString(R.string.created_at) + ": " + formatter.format(timestamp.toDate());
    }

    private String categoryLabel(String type) {
        String safeType = safe(type).trim();
        if ("Film".equalsIgnoreCase(safeType)) {
            return "🎬 Film";
        } else if ("Dizi".equalsIgnoreCase(safeType)) {
            return "📺 Dizi";
        } else if ("Kitap".equalsIgnoreCase(safeType)) {
            return "📚 Kitap";
        } else if ("Oyun".equalsIgnoreCase(safeType)) {
            return "🎮 Oyun";
        } else if ("Şarkı".equalsIgnoreCase(safeType) || "Sarki".equalsIgnoreCase(safeType)) {
            return "🎵 Şarkı";
        }
        return safeType.isEmpty() ? "✨ Alıntı" : safeType;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String displayUsername(String username) {
        String safeUsername = safe(username).trim();
        if (safeUsername.isEmpty()) {
            return "@kullanıcı";
        }
        return safeUsername.startsWith("@") ? safeUsername : "@" + safeUsername;
    }

    private void openUserProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
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
}
