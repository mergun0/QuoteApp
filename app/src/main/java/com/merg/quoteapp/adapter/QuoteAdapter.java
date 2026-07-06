package com.merg.quoteapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    }

    private final List<Quote> quotes = new ArrayList<>();
    private final Set<String> revealedSpoilerQuoteIds = new HashSet<>();
    private final QuoteActionListener listener;
    private final boolean showUsername;
    private final String currentUserId;

    public QuoteAdapter(QuoteActionListener listener) {
        this(listener, false, null);
    }

    public QuoteAdapter(QuoteActionListener listener, boolean showUsername,
                        String currentUserId) {
        this.listener = listener;
        this.showUsername = showUsername;
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
        boolean canManage = !showUsername
                || (currentUserId != null && currentUserId.equals(quote.getUserId()));
        holder.bind(quote, listener, spoilerRevealed, showUsername, canManage,
                () -> revealSpoiler(holder, quote));
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
        private final MaterialButton editButton;
        private final MaterialButton deleteButton;
        private final MaterialButton shareButton;
        private final MaterialButton favoriteButton;

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
            editButton = itemView.findViewById(R.id.buttonEditQuote);
            deleteButton = itemView.findViewById(R.id.buttonDeleteQuote);
            shareButton = itemView.findViewById(R.id.buttonShareQuote);
            favoriteButton = itemView.findViewById(R.id.buttonFavoriteQuote);
        }

        void bind(Quote quote, QuoteActionListener listener, boolean spoilerRevealed,
                  boolean showUsername, boolean canManage, Runnable revealSpoiler) {
            typeText.setText(safe(quote.getType()).toUpperCase());
            usernameText.setText("@" + safe(quote.getUsername()));
            userContainer.setVisibility(showUsername ? View.VISIBLE : View.GONE);
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

            editButton.setOnClickListener(view -> listener.onEdit(quote));
            deleteButton.setOnClickListener(view -> listener.onDelete(quote));
            editButton.setVisibility(canManage ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(canManage ? View.VISIBLE : View.GONE);
            shareButton.setOnClickListener(view -> listener.onShare(quote));
            favoriteButton.setEnabled(false);
            favoriteButton.setOnClickListener(view -> listener.onFavorite(quote));
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
