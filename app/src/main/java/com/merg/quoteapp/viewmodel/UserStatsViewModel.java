package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.repository.UserStatsRepository;

public class UserStatsViewModel extends ViewModel {

    private final UserStatsRepository repository = UserStatsRepository.getInstance();
    private final MutableLiveData<UserStats> userStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration statsListener;

    public LiveData<UserStats> getUserStats() {
        return userStats;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadUserStats(String userId) {
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
        loading.setValue(true);
        error.setValue(null);
        statsListener = repository.observeUserStats(userId, new UserStatsRepository.UserStatsListenerCallback() {
            @Override
            public void onStatsChanged(UserStats stats) {
                userStats.setValue(stats);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void createDefaultUserStatsIfMissing(String userId) {
        loading.setValue(true);
        error.setValue(null);
        repository.createDefaultUserStatsIfMissing(userId, new UserStatsRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                loadUserStats(userId);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void updateUserStats(String userId, UserStats stats) {
        loading.setValue(true);
        error.setValue(null);
        repository.updateUserStats(userId, stats, new UserStatsRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                loadUserStats(userId);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void addXp(String userId, long amount) {
        loading.setValue(true);
        error.setValue(null);
        repository.addXp(userId, amount, new UserStatsRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                loadUserStats(userId);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        if (statsListener != null) {
            statsListener.remove();
        }
        super.onCleared();
    }
}
