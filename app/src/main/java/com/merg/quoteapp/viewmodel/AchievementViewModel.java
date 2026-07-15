package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.repository.AchievementRepository;

import java.util.ArrayList;
import java.util.List;

public class AchievementViewModel extends ViewModel {

    private final AchievementRepository repository = AchievementRepository.getInstance();
    private final MutableLiveData<List<Achievement>> activeAchievements =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<UserAchievement>> userAchievements =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<Achievement>> getActiveAchievements() {
        return activeAchievements;
    }

    public LiveData<List<UserAchievement>> getUserAchievements() {
        return userAchievements;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadActiveAchievements() {
        loading.setValue(activeAchievements.getValue() == null
                || activeAchievements.getValue().isEmpty());
        error.setValue(null);
        repository.getActiveAchievements(new AchievementRepository.AchievementsCallback() {
            @Override
            public void onSuccess(List<Achievement> achievements) {
                activeAchievements.setValue(achievements);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadAchievementsByCategory(String category) {
        loading.setValue(activeAchievements.getValue() == null
                || activeAchievements.getValue().isEmpty());
        error.setValue(null);
        repository.getAchievementsByCategory(category, new AchievementRepository.AchievementsCallback() {
            @Override
            public void onSuccess(List<Achievement> achievements) {
                activeAchievements.setValue(achievements);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadUserAchievements(String userId) {
        loading.setValue(userAchievements.getValue() == null
                || userAchievements.getValue().isEmpty());
        error.setValue(null);
        repository.getUserAchievements(userId, new AchievementRepository.UserAchievementsCallback() {
            @Override
            public void onSuccess(List<UserAchievement> achievements) {
                userAchievements.setValue(achievements);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadLockedAndUnlockedAchievements(String userId) {
        boolean hasAchievements = activeAchievements.getValue() != null
                && !activeAchievements.getValue().isEmpty();
        boolean hasUserAchievements = userAchievements.getValue() != null
                && !userAchievements.getValue().isEmpty();
        loading.setValue(!hasAchievements && !hasUserAchievements);
        error.setValue(null);
        repository.getLockedAndUnlockedAchievements(userId,
                new AchievementRepository.AchievementStatusCallback() {
                    @Override
                    public void onSuccess(List<Achievement> lockedAchievements,
                                          List<UserAchievement> unlockedAchievements) {
                        activeAchievements.setValue(lockedAchievements);
                        userAchievements.setValue(unlockedAchievements);
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                        loading.setValue(false);
                    }
                });
    }
}
