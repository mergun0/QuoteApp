package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.LikeRepository;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LikeViewModel extends ViewModel {

    private final LikeRepository repository = LikeRepository.getInstance();
    private final MutableLiveData<Long> likeCount = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> currentUserLiked = new MutableLiveData<>(false);
    private final MutableLiveData<QuoteState> loadingState = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Boolean>> likedStates =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Boolean>> itemLoadingStates =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Long>> likeCounts =
            new MutableLiveData<>(new HashMap<>());
    private final Map<String, Integer> stateVersions = new HashMap<>();
    private String loadedQuoteId;

    /**
     * Returns the observable like count for the loaded quote.
     *
     * @return like count LiveData
     */
    public LiveData<Long> getLikeCount() {
        return likeCount;
    }

    /**
     * Returns whether the current user liked the loaded quote.
     *
     * @return current user liked state LiveData
     */
    public LiveData<Boolean> getCurrentUserLiked() {
        return currentUserLiked;
    }

    /**
     * Returns the loading or error state for like operations.
     *
     * @return loading state LiveData
     */
    public LiveData<QuoteState> getLoadingState() {
        return loadingState;
    }

    /**
     * Returns liked states keyed by quote id.
     *
     * @return liked states LiveData
     */
    public LiveData<Map<String, Boolean>> getLikedStates() {
        return likedStates;
    }

    /**
     * Returns item loading states keyed by quote id.
     *
     * @return item loading states LiveData
     */
    public LiveData<Map<String, Boolean>> getItemLoadingStates() {
        return itemLoadingStates;
    }

    /**
     * Returns like counts keyed by quote id.
     *
     * @return like counts LiveData
     */
    public LiveData<Map<String, Long>> getLikeCounts() {
        return likeCounts;
    }

    /**
     * Loads like count and current user liked state for a quote.
     *
     * @param quoteId id of the quote whose like state will be loaded
     */
    public void loadLikeState(String quoteId) {
        loadedQuoteId = quoteId;
        loadingState.setValue(QuoteState.loading());
        repository.getLikeCount(quoteId, new LikeRepository.LikeCountCallback() {
            @Override
            public void onSuccess(long count) {
                likeCount.setValue(count);
                setLikeCount(quoteId, count);
                loadCurrentUserLikedState(quoteId);
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    /**
     * Loads only current user's liked state for a quote without loading like count.
     *
     * @param quoteId quote id whose liked state will be loaded
     */
    public void loadLikedState(String quoteId) {
        loadedQuoteId = quoteId;
        if (isBlank(quoteId)) {
            loadingState.setValue(QuoteState.error("Alıntı bilgisi bulunamadı."));
            return;
        }
        loadLikedStateForQuote(quoteId);
    }

    /**
     * Loads liked state for each quote without changing quote list pagination.
     *
     * @param quotes quotes whose liked states will be loaded
     */
    public void loadLikedStates(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        for (Quote quote : quotes) {
            if (quote == null || isBlank(quote.getQuoteId())
                    || containsKey(likedStates.getValue(), quote.getQuoteId())) {
                continue;
            }
            loadLikedStateForQuote(quote.getQuoteId());
        }
    }

    /**
     * Forces liked state reload for provided quotes even if cached state exists.
     *
     * @param quotes quotes whose liked states will be refreshed
     */
    public void refreshLikedStates(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        for (Quote quote : quotes) {
            if (quote == null || isBlank(quote.getQuoteId())) {
                continue;
            }
            loadLikedStateForQuote(quote.getQuoteId());
        }
    }

    /**
     * Loads like counts for the provided quotes without changing pagination.
     *
     * @param quotes quotes whose like counts will be loaded
     */
    public void loadLikeCounts(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        List<String> quoteIds = new ArrayList<>();
        Map<String, Long> currentCounts = likeCounts.getValue();
        for (Quote quote : quotes) {
            if (quote == null || isBlank(quote.getQuoteId())) {
                continue;
            }
            if (currentCounts == null || !currentCounts.containsKey(quote.getQuoteId())) {
                quoteIds.add(quote.getQuoteId());
            }
        }
        if (quoteIds.isEmpty()) {
            return;
        }
        repository.getLikeCounts(quoteIds, new LikeRepository.LikeCountsCallback() {
            @Override
            public void onSuccess(Map<String, Long> counts) {
                Map<String, Long> current = likeCounts.getValue();
                Map<String, Long> updated = current == null
                        ? new HashMap<>() : new HashMap<>(current);
                updated.putAll(counts);
                likeCounts.setValue(updated);
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    /**
     * Forces like count reload for provided quotes even if cached count exists.
     *
     * @param quotes quotes whose like counts will be refreshed
     */
    public void refreshLikeCounts(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        List<String> quoteIds = new ArrayList<>();
        for (Quote quote : quotes) {
            if (quote != null && !isBlank(quote.getQuoteId())) {
                quoteIds.add(quote.getQuoteId());
            }
        }
        repository.getLikeCounts(quoteIds, new LikeRepository.LikeCountsCallback() {
            @Override
            public void onSuccess(Map<String, Long> counts) {
                Map<String, Long> current = likeCounts.getValue();
                Map<String, Long> updated = current == null
                        ? new HashMap<>() : new HashMap<>(current);
                updated.putAll(counts);
                likeCounts.setValue(updated);
                if (!isBlank(loadedQuoteId) && counts.containsKey(loadedQuoteId)) {
                    likeCount.setValue(counts.get(loadedQuoteId));
                }
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    /**
     * Toggles liked state for a quote with optimistic UI state.
     *
     * @param quoteId quote id whose liked state will be toggled
     */
    public void toggleLike(String quoteId) {
        if (isBlank(quoteId)) {
            loadingState.setValue(QuoteState.error("Beğenilecek alıntı bulunamadı."));
            return;
        }

        incrementVersion(quoteId);
        boolean previousState = getMapValue(likedStates.getValue(), quoteId);
        boolean nextState = !previousState;
        setLikedState(quoteId, nextState);
        applyOptimisticCount(quoteId, previousState, nextState);
        setItemLoading(quoteId, true);

        LikeRepository.OperationCallback callback = new LikeRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                setLikedState(quoteId, nextState);
                setItemLoading(quoteId, false);
                loadingState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                setLikedState(quoteId, previousState);
                applyOptimisticCount(quoteId, nextState, previousState);
                setItemLoading(quoteId, false);
                loadingState.setValue(QuoteState.error(message));
            }
        };

        if (nextState) {
            repository.likeQuote(quoteId, callback);
        } else {
            repository.unlikeQuote(quoteId, callback);
        }
    }

    /**
     * Likes the currently loaded quote.
     */
    public void likeQuote() {
        if (loadedQuoteId == null || loadedQuoteId.trim().isEmpty()) {
            loadingState.setValue(QuoteState.error("Beğenilecek alıntı bulunamadı."));
            return;
        }
        loadingState.setValue(QuoteState.loading());
        repository.likeQuote(loadedQuoteId, new LikeRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                currentUserLiked.setValue(true);
                refreshLikeCount();
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    /**
     * Removes the current user's like from the currently loaded quote.
     */
    public void unlikeQuote() {
        if (loadedQuoteId == null || loadedQuoteId.trim().isEmpty()) {
            loadingState.setValue(QuoteState.error("Beğenisi kaldırılacak alıntı bulunamadı."));
            return;
        }
        loadingState.setValue(QuoteState.loading());
        repository.unlikeQuote(loadedQuoteId, new LikeRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                currentUserLiked.setValue(false);
                refreshLikeCount();
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    private void loadCurrentUserLikedState(String quoteId) {
        int version = getVersion(quoteId);
        repository.isLikedByCurrentUser(quoteId, new LikeRepository.LikedStateCallback() {
            @Override
            public void onSuccess(boolean liked) {
                if (version != getVersion(quoteId)) {
                    return;
                }
                setLikedState(quoteId, liked);
                loadingState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    private void refreshLikeCount() {
        repository.getLikeCount(loadedQuoteId, new LikeRepository.LikeCountCallback() {
            @Override
            public void onSuccess(long count) {
                likeCount.setValue(count);
                setLikeCount(loadedQuoteId, count);
                loadingState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    private void loadLikedStateForQuote(String quoteId) {
        int version = getVersion(quoteId);
        repository.isLikedByCurrentUser(quoteId, new LikeRepository.LikedStateCallback() {
            @Override
            public void onSuccess(boolean liked) {
                if (version != getVersion(quoteId)) {
                    return;
                }
                setLikedState(quoteId, liked);
            }

            @Override
            public void onError(String message) {
                loadingState.setValue(QuoteState.error(message));
            }
        });
    }

    private void applyOptimisticCount(String quoteId, boolean previousState, boolean nextState) {
        if (previousState == nextState) {
            return;
        }
        Map<String, Long> current = likeCounts.getValue();
        Map<String, Long> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        long currentCount = updated.get(quoteId) == null ? 0L : updated.get(quoteId);
        long nextCount = nextState ? currentCount + 1L : Math.max(0L, currentCount - 1L);
        updated.put(quoteId, nextCount);
        likeCounts.setValue(updated);
        if (quoteId.equals(loadedQuoteId)) {
            likeCount.setValue(nextCount);
        }
    }

    private void setLikeCount(String quoteId, long count) {
        if (isBlank(quoteId)) {
            return;
        }
        Map<String, Long> current = likeCounts.getValue();
        Map<String, Long> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        updated.put(quoteId, count);
        likeCounts.setValue(updated);
    }

    private void setLikedState(String quoteId, boolean liked) {
        Map<String, Boolean> current = likedStates.getValue();
        Map<String, Boolean> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        updated.put(quoteId, liked);
        likedStates.setValue(updated);
        if (quoteId.equals(loadedQuoteId)) {
            currentUserLiked.setValue(liked);
        }
    }

    private void setItemLoading(String quoteId, boolean loading) {
        Map<String, Boolean> current = itemLoadingStates.getValue();
        Map<String, Boolean> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        updated.put(quoteId, loading);
        itemLoadingStates.setValue(updated);
    }

    private int getVersion(String quoteId) {
        Integer version = stateVersions.get(quoteId);
        return version == null ? 0 : version;
    }

    private void incrementVersion(String quoteId) {
        stateVersions.put(quoteId, getVersion(quoteId) + 1);
    }

    private boolean getMapValue(Map<String, Boolean> map, String quoteId) {
        return map != null && Boolean.TRUE.equals(map.get(quoteId));
    }

    private boolean containsKey(Map<String, Boolean> map, String quoteId) {
        return map != null && map.containsKey(quoteId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
