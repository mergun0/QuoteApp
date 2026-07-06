package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.DiscoverRepository;
import com.merg.quoteapp.repository.QuoteRepository;

import java.util.Collections;
import java.util.List;

public class DiscoverViewModel extends ViewModel {

    private final DiscoverRepository discoverRepository = new DiscoverRepository();
    private final QuoteRepository quoteRepository = QuoteRepository.getInstance();
    private final MutableLiveData<List<Quote>> quotes =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<QuoteState> listState = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> operationState = new MutableLiveData<>();
    private ListenerRegistration listenerRegistration;

    public LiveData<List<Quote>> getQuotes() {
        return quotes;
    }

    public LiveData<QuoteState> getListState() {
        return listState;
    }

    public LiveData<QuoteState> getOperationState() {
        return operationState;
    }

    public void loadQuotes() {
        if (listenerRegistration != null) {
            return;
        }
        listState.setValue(QuoteState.loading());
        listenerRegistration = discoverRepository.getAllQuotes(
                new DiscoverRepository.DiscoverCallback() {
                    @Override
                    public void onQuotesChanged(List<Quote> result) {
                        quotes.setValue(result);
                        listState.setValue(QuoteState.success(null));
                    }

                    @Override
                    public void onError(String message) {
                        listState.setValue(QuoteState.error(message));
                    }
                });
    }

    public void refreshQuotes() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        loadQuotes();
    }

    public void deleteQuote(String quoteId) {
        operationState.setValue(QuoteState.loading());
        quoteRepository.deleteQuote(quoteId, new QuoteRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                operationState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                operationState.setValue(QuoteState.error(message));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        super.onCleared();
    }
}
