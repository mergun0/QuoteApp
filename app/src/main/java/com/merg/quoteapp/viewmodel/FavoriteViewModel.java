package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.FavoriteRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteViewModel extends ViewModel {

    private final FavoriteRepository repository = FavoriteRepository.getInstance();
    private final MutableLiveData<List<Quote>> savedQuotes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<QuoteState> listState = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> operationState = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Boolean>> savedStates =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Boolean>> itemLoadingStates =
            new MutableLiveData<>(new HashMap<>());
    private final Map<String, Integer> stateVersions = new HashMap<>();

    /**
     * Returns current user's saved quotes.
     *
     * @return saved quotes LiveData
     */
    public LiveData<List<Quote>> getSavedQuotes() {
        return savedQuotes;
    }

    /**
     * Returns saved quotes list state.
     *
     * @return list state LiveData
     */
    public LiveData<QuoteState> getListState() {
        return listState;
    }

    /**
     * Returns favorite operation state.
     *
     * @return operation state LiveData
     */
    public LiveData<QuoteState> getOperationState() {
        return operationState;
    }

    /**
     * Returns saved states keyed by quote id.
     *
     * @return saved states LiveData
     */
    public LiveData<Map<String, Boolean>> getSavedStates() {
        return savedStates;
    }

    /**
     * Returns item loading states keyed by quote id.
     *
     * @return item loading state LiveData
     */
    public LiveData<Map<String, Boolean>> getItemLoadingStates() {
        return itemLoadingStates;
    }

    /**
     * Loads saved quotes for current user.
     */
    public void loadSavedQuotes() {
        listState.setValue(QuoteState.loading());
        repository.getSavedQuotesForCurrentUser(new FavoriteRepository.SavedQuotesCallback() {
            @Override
            public void onSuccess(List<Quote> quotes) {
                savedQuotes.setValue(quotes);
                markQuotesAsSaved(quotes);
                listState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                listState.setValue(QuoteState.error(message));
            }
        });
    }

    /**
     * Refreshes saved quotes for current user.
     */
    public void refreshSavedQuotes() {
        loadSavedQuotes();
    }

    /**
     * Loads saved states for the provided quotes.
     *
     * @param quotes quotes whose saved states will be loaded
     */
    public void loadSavedStates(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        for (Quote quote : quotes) {
            if (quote == null || isBlank(quote.getQuoteId())
                    || containsKey(savedStates.getValue(), quote.getQuoteId())) {
                continue;
            }
            loadSavedStateForQuote(quote.getQuoteId());
        }
    }

    /**
     * Toggles current user's saved state for a quote.
     *
     * @param quote quote to save or unsave
     */
    public void toggleSaved(Quote quote) {
        if (quote == null || isBlank(quote.getQuoteId())) {
            operationState.setValue(QuoteState.error("Kaydedilecek alıntı bulunamadı."));
            return;
        }
        String quoteId = quote.getQuoteId();
        incrementVersion(quoteId);
        boolean previousState = getMapValue(savedStates.getValue(), quoteId);
        boolean nextState = !previousState;
        setSavedState(quoteId, nextState);
        setItemLoading(quoteId, true);

        FavoriteRepository.OperationCallback callback = new FavoriteRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                setSavedState(quoteId, nextState);
                setItemLoading(quoteId, false);
                if (!nextState) {
                    removeFromSavedQuotes(quoteId);
                }
                operationState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                setSavedState(quoteId, previousState);
                setItemLoading(quoteId, false);
                operationState.setValue(QuoteState.error(message));
            }
        };

        if (nextState) {
            repository.saveQuote(quoteId, callback);
        } else {
            repository.unsaveQuote(quoteId, callback);
        }
    }

    private void loadSavedStateForQuote(String quoteId) {
        int version = getVersion(quoteId);
        repository.isSavedByCurrentUser(quoteId, new FavoriteRepository.SavedStateCallback() {
            @Override
            public void onSuccess(boolean saved) {
                if (version != getVersion(quoteId)) {
                    return;
                }
                setSavedState(quoteId, saved);
            }

            @Override
            public void onError(String message) {
                operationState.setValue(QuoteState.error(message));
            }
        });
    }

    private void markQuotesAsSaved(List<Quote> quotes) {
        Map<String, Boolean> current = savedStates.getValue();
        Map<String, Boolean> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        if (quotes != null) {
            for (Quote quote : quotes) {
                if (quote != null && !isBlank(quote.getQuoteId())) {
                    updated.put(quote.getQuoteId(), true);
                }
            }
        }
        savedStates.setValue(updated);
    }

    private void removeFromSavedQuotes(String quoteId) {
        List<Quote> current = savedQuotes.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }
        List<Quote> updated = new ArrayList<>();
        for (Quote quote : current) {
            if (quote != null && !quoteId.equals(quote.getQuoteId())) {
                updated.add(quote);
            }
        }
        savedQuotes.setValue(updated);
    }

    private void setSavedState(String quoteId, boolean saved) {
        Map<String, Boolean> current = savedStates.getValue();
        Map<String, Boolean> updated = current == null ? new HashMap<>() : new HashMap<>(current);
        updated.put(quoteId, saved);
        savedStates.setValue(updated);
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
