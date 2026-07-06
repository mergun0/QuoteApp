package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.repository.ProfileRepository;

public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository = new ProfileRepository();
    private final MutableLiveData<ProfileStats> profile = new MutableLiveData<>();
    private final MutableLiveData<QuoteState> state = new MutableLiveData<>();
    private boolean loaded;

    public LiveData<ProfileStats> getProfile() {
        return profile;
    }

    public LiveData<QuoteState> getState() {
        return state;
    }

    public void loadProfile() {
        if (loaded) {
            return;
        }
        loaded = true;
        state.setValue(QuoteState.loading());
        repository.getProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProfileStats stats) {
                profile.setValue(stats);
                state.setValue(QuoteState.success(null));
            }

            @Override
            public void onError(String message) {
                loaded = false;
                state.setValue(QuoteState.error(message));
            }
        });
    }
}
