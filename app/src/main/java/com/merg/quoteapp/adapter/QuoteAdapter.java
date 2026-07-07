package com.merg.quoteapp.adapter;

import android.content.res.ColorStateList;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Quote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder> {

    public interface QuoteActionListener {
        void onEdit(Quote quote);

        void onDelete(Quote quote);

        void onShare(Quote quote);

        void onFavorite(Quote quote);

        default void onReport(Quote quote) {
        }

        default void onOpen(Quote quote) {
        }

        default void onUserProfile(String userId) {
        }
    }

    private final List<Quote> quotes = new ArrayList<>();
    private final Set<String> revealedSpoilerQuoteIds = new HashSet<>();
    private final Set<String> likedQuoteIds = new HashSet<>();
    private final Set<String> likeLoadingQuoteIds = new HashSet<>();
    private final java.util.Map<String, Long> likeCounts = new java.util.HashMap<>();
    private final QuoteActionListener listener;
    private final boolean showUsername;
    private final boolean restrictManagementToCurrentUser;
    private final String currentUserId;
    private boolean likeActionsEnabled;
    private boolean reportActionsEnabled;

    public QuoteAdapter(QuoteActionListener listener) {
        this(listener, false, false, null);
    }

    public QuoteAdapter(QuoteActionListener listener, boolean showUsername,
                        String currentUserId) {
        this(listener, showUsername, true, currentUserId);
    }

    public QuoteAdapter(QuoteActionListener listener, boolean showUsername,
                        boolean restrictManagementToCurrentUser, String currentUserId) {
        this.listener = listener;
        this.showUsername = showUsername;
        this.restrictManagementToCurrentUser = restrictManagementToCurrentUser;
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Quote> newQuotes) {
        revealedSpoilerQuoteIds.clear();
        quotes.clear();
        if (newQuotes != null) {
            quotes.addAll(newQuotes);
        }
        notifyDataSetChanged();
    }

    /**
     * Enables or disables like button interaction for this adapter.
     *
     * @param enabled true when like button should call the listener
     */
    public void setLikeActionsEnabled(boolean enabled) {
        likeActionsEnabled = enabled;
        notifyDataSetChanged();
    }

    /**
     * Enables or disables report button visibility for this adapter.
     *
     * @param enabled true when report button should be visible
     */
    public void setReportActionsEnabled(boolean enabled) {
        reportActionsEnabled = enabled;
        notifyDataSetChanged();
    }

    /**
     * Updates liked state for one quote item.
     *
     * @param quoteId quote id to update
     * @param liked true if the quote is liked by current user
     */
    public void updateLikeState(String quoteId, boolean liked) {
        if (quoteId == null || quoteId.trim().isEmpty()) {
            return;
        }
        if (liked) {
            likedQuoteIds.add(quoteId);
        } else {
            likedQuoteIds.remove(quoteId);
        }
        notifyQuoteChanged(quoteId);
    }

    /**
     * Updates loading state for one quote like button.
     *
     * @param quoteId quote id to update
     * @param loading true while a like request is running
     */
    public void updateLikeLoadingState(String quoteId, boolean loading) {
        if (quoteId == null || quoteId.trim().isEmpty()) {
            return;
        }
        if (loading) {
            likeLoadingQuoteIds.add(quoteId);
        } else {
            likeLoadingQuoteIds.remove(quoteId);
        }
        notifyQuoteChanged(quoteId);
    }

    /**
     * Updates like count for one quote item.
     *
     * @param quoteId quote id to update
     * @param count total like count
     */
    public void updateLikeCount(String quoteId, long count) {
        if (quoteId == null || quoteId.trim().isEmpty()) {
            return;
        }
        likeCounts.put(quoteId, count);
        notifyQuoteChanged(quoteId);
    }

    @NonNull
    @Override
    public QuoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quote, parent, false);
        return new QuoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuoteViewHolder holder, int position) {
        Quote quote = quotes.get(position);
        boolean spoilerRevealed = quote.getQuoteId() != null
                && revealedSpoilerQuoteIds.contains(quote.getQuoteId());
        boolean canManage = !restrictManagementToCurrentUser
                || (currentUserId != null && currentUserId.equals(quote.getUserId()));
        boolean liked = quote.getQuoteId() != null && likedQuoteIds.contains(quote.getQuoteId());
        boolean likeLoading = quote.getQuoteId() != null
                && likeLoadingQuoteIds.contains(quote.getQuoteId());
        long likeCount = quote.getQuoteId() == null || likeCounts.get(quote.getQuoteId()) == null
                ? 0L : likeCounts.get(quote.getQuoteId());
        holder.bind(quote, listener, spoilerRevealed, showUsername, canManage,
                liked, likeLoading, likeCount, likeActionsEnabled,
                reportActionsEnabled, () -> revealSpoiler(holder, quote));
    }

    private void notifyQuoteChanged(String quoteId) {
        for (int index = 0; index < quotes.size(); index++) {
            Quote quote = quotes.get(index);
            if (quoteId.equals(quote.getQuoteId())) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void revealSpoiler(QuoteViewHolder holder, Quote quote) {
        if (quote.getQuoteId() == null || quote.getQuoteId().isEmpty()) {
            return;
        }
        revealedSpoilerQuoteIds.add(quote.getQuoteId());
        int position = holder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    static class QuoteViewHolder extends RecyclerView.ViewHolder {

        private static final int MENU_COPY = 1;
        private static final int MENU_REPORT = 2;
        private static final int MENU_SAVE = 3;
        private static final int MENU_BLOCK_USER = 4;
        private static final int MENU_EDIT = 5;
        private static final int MENU_DELETE = 6;

        private final TextView typeText;
        private final TextView titleText;
        private final TextView usernameText;
        private final TextView quoteText;
        private final TextView authorText;
        private final TextView characterText;
        private final TextView seriesText;
        private final TextView tagsText;
        private final TextView spoilerText;
        private final LinearLayout userContainer;
        private final LinearLayout quoteTextContainer;
        private final LinearLayout spoilerHiddenContainer;
        private final MaterialButton showSpoilerButton;
        private final MaterialButton shareButton;
        private final MaterialButton favoriteButton;
        private final MaterialButton moreButton;

        QuoteViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.textCardType);
            titleText = itemView.findViewById(R.id.textCardTitle);
            usernameText = itemView.findViewById(R.id.textCardUsername);
            quoteText = itemView.findViewById(R.id.textCardQuote);
            authorText = itemView.findViewById(R.id.textCardAuthor);
            characterText = itemView.findViewById(R.id.textCardCharacter);
            seriesText = itemView.findViewById(R.id.textCardSeries);
            tagsText = itemView.findViewById(R.id.textCardTags);
            spoilerText = itemView.findViewById(R.id.textCardSpoiler);
            userContainer = itemView.findViewById(R.id.layoutCardUser);
            quoteTextContainer = itemView.findViewById(R.id.quoteTextContainer);
            spoilerHiddenContainer = itemView.findViewById(R.id.spoilerHiddenContainer);
            showSpoilerButton = itemView.findViewById(R.id.showSpoilerButton);
            shareButton = itemView.findViewById(R.id.buttonShareQuote);
            favoriteButton = itemView.findViewById(R.id.buttonFavoriteQuote);
            moreButton = itemView.findViewById(R.id.buttonMoreQuote);
        }

        void bind(Quote quote, QuoteActionListener listener, boolean spoilerRevealed,
                  boolean showUsername, boolean canManage, boolean liked,
                  boolean likeLoading, long likeCount, boolean likeActionsEnabled,
                  boolean reportActionsEnabled, Runnable revealSpoiler) {
            typeText.setText(safe(quote.getType()).toUpperCase());
            usernameText.setText("@" + safe(quote.getUsername()));
            userContainer.setVisibility(showUsername ? View.VISIBLE : View.GONE);
            userContainer.setOnClickListener(showUsername
                    ? view -> listener.onUserProfile(quote.getUserId())
                    : null);
            titleText.setText(safe(quote.getTitle()));
            quoteText.setText("“" + safe(quote.getText()) + "”");
            authorText.setText(safe(quote.getAuthor()));
            spoilerText.setVisibility(quote.isSpoiler() ? View.VISIBLE : View.GONE);

            boolean hideQuote = quote.isSpoiler() && !spoilerRevealed;
            quoteTextContainer.setVisibility(hideQuote ? View.GONE : View.VISIBLE);
            spoilerHiddenContainer.setVisibility(hideQuote ? View.VISIBLE : View.GONE);
            showSpoilerButton.setOnClickListener(hideQuote
                    ? view -> revealSpoiler.run()
                    : null);

            setOptionalText(characterText, quote.getCharacterName(),
                    "Karakter: " + safe(quote.getCharacterName()));

            boolean hasSeriesDetails = "Dizi".equals(quote.getType())
                    && !safe(quote.getSeason()).isEmpty()
                    && !safe(quote.getEpisode()).isEmpty();
            seriesText.setVisibility(hasSeriesDetails ? View.VISIBLE : View.GONE);
            if (hasSeriesDetails) {
                seriesText.setText("Sezon " + quote.getSeason() + " • Bölüm " + quote.getEpisode());
            }

            if (quote.getTags() == null || quote.getTags().isEmpty()) {
                tagsText.setVisibility(View.GONE);
            } else {
                tagsText.setText(quote.getTags().stream()
                        .map(tag -> "#" + tag)
                        .collect(Collectors.joining("  ")));
                tagsText.setVisibility(View.VISIBLE);
            }

            shareButton.setOnClickListener(view -> listener.onShare(quote));
            renderFavoriteButton(liked, likeLoading, likeCount, likeActionsEnabled);
            favoriteButton.setOnClickListener(likeActionsEnabled
                    ? view -> animateLikeClick(view, () -> listener.onFavorite(quote))
                    : null);
            moreButton.setOnClickListener(view ->
                    animateOverflowClick(view, () -> showOverflowMenu(
                            quote, listener, canManage, reportActionsEnabled, showUsername)));
            itemView.setOnClickListener(view -> listener.onOpen(quote));
        }

        private void showOverflowMenu(Quote quote, QuoteActionListener listener,
                                      boolean canManage, boolean reportActionsEnabled,
                                      boolean showUsername) {
            PopupMenu popupMenu = new PopupMenu(itemView.getContext(), moreButton);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, MENU_COPY, Menu.NONE, R.string.copy_quote);
            if (reportActionsEnabled) {
                menu.add(Menu.NONE, MENU_REPORT, Menu.NONE, R.string.report_quote_menu);
            }
            menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.save_coming_soon)
                    .setEnabled(false);
            if (showUsername) {
                menu.add(Menu.NONE, MENU_BLOCK_USER, Menu.NONE, R.string.block_user_coming_soon)
                        .setEnabled(false);
            }
            if (canManage) {
                menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, R.string.edit);
                menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.delete);
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == MENU_COPY) {
                    copyQuote(quote);
                    return true;
                } else if (item.getItemId() == MENU_REPORT) {
                    listener.onReport(quote);
                    return true;
                } else if (item.getItemId() == MENU_EDIT) {
                    listener.onEdit(quote);
                    return true;
                } else if (item.getItemId() == MENU_DELETE) {
                    listener.onDelete(quote);
                    return true;
                }
                return true;
            });
            popupMenu.show();
        }

        private void copyQuote(Quote quote) {
            ClipboardManager clipboardManager = (ClipboardManager) itemView.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                return;
            }
            String copyText = "“" + safe(quote.getText()) + "”\n"
                    + safe(quote.getTitle()) + " — " + safe(quote.getAuthor());
            clipboardManager.setPrimaryClip(ClipData.newPlainText(
                    itemView.getContext().getString(R.string.copy_quote), copyText));
        }

        private void animateLikeClick(View view, Runnable action) {
            view.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70L)
                    .withEndAction(() -> view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(110L)
                            .withEndAction(action)
                            .start())
                    .start();
        }

        private void animateOverflowClick(View view, Runnable action) {
            view.animate()
                    .rotationBy(90f)
                    .alpha(0.72f)
                    .setDuration(90L)
                    .withEndAction(() -> {
                        action.run();
                        view.animate()
                                .rotationBy(-90f)
                                .alpha(1f)
                                .setDuration(120L)
                                .start();
                    })
                    .start();
        }

        private void renderFavoriteButton(boolean liked, boolean likeLoading,
                                          long likeCount, boolean likeActionsEnabled) {
            int color = ContextCompat.getColor(itemView.getContext(), liked
                    ? R.color.quote_status_error : R.color.quote_text_secondary);
            ColorStateList tint = ColorStateList.valueOf(color);
            favoriteButton.setEnabled(likeActionsEnabled && !likeLoading);
            favoriteButton.setAlpha(likeLoading ? 0.55f : 1f);
            favoriteButton.setSelected(liked);
            favoriteButton.setText(likeCount > 0
                    ? itemView.getContext().getString(R.string.like_button_with_count, likeCount)
                    : itemView.getContext().getString(R.string.favorite));
            favoriteButton.setTextColor(color);
            favoriteButton.setIconTint(tint);
        }

        private void setOptionalText(TextView view, String value, String displayValue) {
            boolean visible = value != null && !value.trim().isEmpty();
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                view.setText(displayValue);
            }
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
