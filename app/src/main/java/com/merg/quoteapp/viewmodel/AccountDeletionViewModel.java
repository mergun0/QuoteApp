package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.repository.AccountDeletionRepository;

public class AccountDeletionViewModel extends ViewModel {

    public static class State {
        public final boolean loading;
        public final boolean success;
        public final boolean pending;
        public final boolean unknown;
        public final String message;

        public State(boolean loading, boolean success, boolean pending, boolean unknown, String message) {
            this.loading = loading;
            this.success = success;
            this.pending = pending;
            this.unknown = unknown;
            this.message = message;
        }
    }

    private final AccountDeletionRepository repository = AccountDeletionRepository.getInstance();
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(false, false, false, false, ""));

    public LiveData<State> getState() {
        return state;
    }

    public boolean usesPasswordProvider() {
        return repository.currentUserUsesPasswordProvider();
    }

    public void checkPending() {
        state.setValue(new State(true, false, false, false, ""));
        repository.checkCurrentUserDeletionState(result -> {
            if (result == AccountDeletionRepository.DeletionState.UNKNOWN) {
                state.postValue(new State(false, false, false, true,
                        "Hesap durumu doğrulanamadı. Lütfen tekrar deneyin veya oturumu kapatın."));
            } else {
                state.postValue(new State(false, false,
                        result == AccountDeletionRepository.DeletionState.PENDING,
                        false, ""));
            }
        });
    }

    public void requestDeletion(String password, String confirmation, String reason) {
        state.setValue(new State(true, false, false, false, ""));
        repository.requestAccountDeletion(password, confirmation, reason,
                new AccountDeletionRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        state.postValue(new State(false, true, true, false,
                                "Hesap silme talebiniz alındı."));
                    }

                    @Override
                    public void onError(String message) {
                        state.postValue(new State(false, false, false, false, message));
                    }
                });
    }
}
