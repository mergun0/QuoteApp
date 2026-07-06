package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.QuoteRepository;

import java.util.Collections;
import java.util.List;

public class QuoteViewModel extends ViewModel {

    private final QuoteRepository repository = QuoteRepository.getInstance();
    private final MutableLiveData<List<Quote>> quotes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<QuoteState> listState = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> operationState = new MutableLiveData<>();
    private ListenerRegistration quotesListener;

    public LiveData<List<Quote>> getQuotes() {
        return quotes;
    }

    public LiveData<QuoteState> getListState() {
        return listState;
    }

    public LiveData<QuoteState> getOperationState() {
        return operationState;
    }

    public void loadCurrentUserQuotes() {
        if (quotesListener != null) {
            return;
        }
        listState.setValue(QuoteState.loading());
        quotesListener = repository.getCurrentUserQuotes(new QuoteRepository.QuotesCallback() {
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
        if (quotesListener != null) {
            quotesListener.remove();
            quotesListener = null;
        }
        loadCurrentUserQuotes();
    }

    public void saveQuote(Quote quote, boolean editing) {
        String validationError = validateQuote(quote);
        if (validationError != null) {
            operationState.setValue(QuoteState.error(validationError));
            return;
        }

        operationState.setValue(QuoteState.loading());
        QuoteRepository.OperationCallback callback = operationCallback();
        if (editing) {
            repository.updateQuote(quote, callback);
        } else {
            repository.addQuote(quote, callback);
        }
    }

    public void deleteQuote(String quoteId) {
        operationState.setValue(QuoteState.loading());
        repository.deleteQuote(quoteId, operationCallback());
    }

    private String validateQuote(Quote quote) {
        if (quote.getType() == null || quote.getType().trim().isEmpty()) {
            return "Alıntı türünü seçin.";
        }
        if (quote.getText() == null || quote.getText().trim().isEmpty()) {
            return "Alıntı metnini girin.";
        }
        if (quote.getTitle() == null || quote.getTitle().trim().isEmpty()) {
            return "Eser adını girin.";
        }
        if (quote.getAuthor() == null || quote.getAuthor().trim().isEmpty()) {
            return "Yazar veya yönetmen bilgisini girin.";
        }
        if ("Dizi".equals(quote.getType())
                && (quote.getSeason() == null || quote.getSeason().trim().isEmpty()
                || quote.getEpisode() == null || quote.getEpisode().trim().isEmpty())) {
            return "Dizi alıntıları için sezon ve bölüm bilgilerini girin.";
        }
        return null;
    }

    private QuoteRepository.OperationCallback operationCallback() {
        return new QuoteRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                operationState.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                operationState.setValue(QuoteState.error(message));
            }
        };
    }

    @Override
    protected void onCleared() {
        if (quotesListener != null) {
            quotesListener.remove();
        }
        super.onCleared();
    }
}
