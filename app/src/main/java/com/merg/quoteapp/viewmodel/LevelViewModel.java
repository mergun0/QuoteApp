package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Level;
import com.merg.quoteapp.repository.LevelRepository;

import java.util.ArrayList;
import java.util.List;

public class LevelViewModel extends ViewModel {

    private final LevelRepository repository = LevelRepository.getInstance();
    private final MutableLiveData<List<Level>> levels = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Level> currentLevel = new MutableLiveData<>();
    private final MutableLiveData<Level> nextLevel = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<Level>> getLevels() {
        return levels;
    }

    public LiveData<Level> getCurrentLevel() {
        return currentLevel;
    }

    public LiveData<Level> getNextLevel() {
        return nextLevel;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadAllLevels() {
        loading.setValue(true);
        error.setValue(null);
        repository.getAllLevels(new LevelRepository.LevelsCallback() {
            @Override
            public void onSuccess(List<Level> levelList) {
                levels.setValue(levelList);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadCurrentLevel(long totalXp) {
        loading.setValue(true);
        error.setValue(null);
        repository.getCurrentLevel(totalXp, new LevelRepository.LevelCallback() {
            @Override
            public void onSuccess(Level level) {
                currentLevel.setValue(level);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadNextLevel(long totalXp) {
        loading.setValue(true);
        error.setValue(null);
        repository.getNextLevel(totalXp, new LevelRepository.LevelCallback() {
            @Override
            public void onSuccess(Level level) {
                nextLevel.setValue(level);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadLevelProgress(long totalXp) {
        loading.setValue(true);
        error.setValue(null);
        repository.getCurrentLevel(totalXp, new LevelRepository.LevelCallback() {
            @Override
            public void onSuccess(Level level) {
                currentLevel.setValue(level);
                repository.getNextLevel(totalXp, new LevelRepository.LevelCallback() {
                    @Override
                    public void onSuccess(Level level) {
                        nextLevel.setValue(level);
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                        loading.setValue(false);
                    }
                });
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }
}
