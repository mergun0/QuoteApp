package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.QuoteRepository;

public class QuoteDetailViewModel extends ViewModel {

    private final QuoteRepository repository = QuoteRepository.getInstance();
    private final MutableLiveData<Quote> quote = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> state = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> deleteState = new MutableLiveData<>();
    private String loadedQuoteId;

    public LiveData<Quote> getQuote() {
        return quote;
    }

    public LiveData<QuoteState> getState() {
        return state;
    }

    public LiveData<QuoteState> getDeleteState() {
        return deleteState;
    }

    public void loadQuote(String quoteId) {
        if (quoteId != null && quoteId.equals(loadedQuoteId) && quote.getValue() != null) {
            return;
        }
        loadedQuoteId = quoteId;
        state.setValue(QuoteState.loading());
        repository.getQuoteById(quoteId, new QuoteRepository.QuoteCallback() {
            @Override
            public void onSuccess(Quote result) {
                quote.setValue(result);
                state.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                state.setValue(QuoteState.error(message));
            }
        });
    }

    public void deleteQuote(String quoteId) {
        deleteState.setValue(QuoteState.loading());
        repository.deleteQuote(quoteId, new QuoteRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                deleteState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                deleteState.setValue(QuoteState.error(message));
            }
        });
    }
}
