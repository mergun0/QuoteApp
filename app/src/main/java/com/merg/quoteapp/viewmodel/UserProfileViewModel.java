package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserProfileData;
import com.merg.quoteapp.model.UserProfilePage;
import com.merg.quoteapp.repository.QuoteRepository;
import com.merg.quoteapp.repository.UserProfileRepository;

import java.util.ArrayList;
import java.util.List;

public class UserProfileViewModel extends ViewModel {

    private final UserProfileRepository profileRepository = new UserProfileRepository();
    private final QuoteRepository quoteRepository = QuoteRepository.getInstance();
    private final MutableLiveData<UserProfileData> profile = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> state = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> operationState = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> loadMoreState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    private String loadedUserId;
    private boolean initialLoadStarted;

    public LiveData<UserProfileData> getProfile() {
        return profile;
    }

    public LiveData<QuoteState> getState() {
        return state;
    }

    public LiveData<QuoteState> getOperationState() {
        return operationState;
    }

    public LiveData<QuoteState> getLoadMoreState() {
        return loadMoreState;
    }

    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }

    public void loadProfile(String userId) {
        if (initialLoadStarted && userId != null && userId.equals(loadedUserId)) {
            return;
        }
        loadedUserId = userId;
        initialLoadStarted = true;
        profileRepository.resetPagination(userId);
        state.setValue(QuoteState.loading());
        profileRepository.getNextPage(userId, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfilePage page) {
                profile.setValue(page.getProfile());
                hasMore.setValue(page.hasMore());
                state.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                initialLoadStarted = false;
                state.setValue(QuoteState.error(message));
            }
        });
    }

    public void refreshProfile() {
        String userId = loadedUserId;
        initialLoadStarted = false;
        hasMore.setValue(true);
        loadProfile(userId);
    }

    public void loadMoreQuotes() {
        if (!initialLoadStarted || Boolean.FALSE.equals(hasMore.getValue())) {
            return;
        }
        QuoteState currentState = loadMoreState.getValue();
        if (currentState != null && currentState.getStatus() == QuoteState.Status.LOADING) {
            return;
        }
        loadMoreState.setValue(QuoteState.loading());
        profileRepository.getNextPage(
                loadedUserId, new UserProfileRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfilePage page) {
                        UserProfileData current = profile.getValue();
                        List<Quote> combined = new ArrayList<>();
                        if (current != null && current.getQuotes() != null) {
                            combined.addAll(current.getQuotes());
                        }
                        combined.addAll(page.getProfile().getQuotes());
                        UserProfileData source = page.getProfile();
                        profile.setValue(new UserProfileData(
                                source.getUserId(),
                                source.getUsername(),
                                source.getJoinedAt(),
                                source.getTotalQuotes(),
                                source.getMovieQuotes(),
                                source.getSeriesQuotes(),
                                source.getBookQuotes(),
                                combined));
                        hasMore.setValue(page.hasMore());
                        loadMoreState.setValue(QuoteState.success(null));
                    }

                    @Override
                    public void onError(String message) {
                        loadMoreState.setValue(QuoteState.error(message));
                    }
                });
    }

    public void deleteQuote(String quoteId) {
        operationState.setValue(QuoteState.loading());
        quoteRepository.deleteQuote(quoteId, new QuoteRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                operationState.setValue(QuoteState.success(null));
                String userId = loadedUserId;
                initialLoadStarted = false;
                loadProfile(userId);
            }

            @Override
            public void onError(String message) {
                operationState.setValue(QuoteState.error(message));
            }
        });
    }
}
