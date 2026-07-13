package com.merg.quoteapp.viewmodel;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.AuthState;
import com.merg.quoteapp.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository = AuthRepository.getInstance();
    private final MutableLiveData<AuthState> registerState = new MutableLiveData<>();
    private final MutableLiveData<AuthState> loginState = new MutableLiveData<>();
    private final MutableLiveData<AuthState> resetState = new MutableLiveData<>();

    public LiveData<AuthState> getRegisterState() {
        return registerState;
    }

    public LiveData<AuthState> getLoginState() {
        return loginState;
    }

    public LiveData<AuthState> getResetState() {
        return resetState;
    }

    public void register(String username, String email, String password, String confirmPassword) {
        String validationError = validateRegistration(username, email, password, confirmPassword);
        if (validationError != null) {
            registerState.setValue(AuthState.error(validationError));
            return;
        }

        registerState.setValue(AuthState.loading());
        repository.register(username, email, password, callbackFor(registerState));
    }

    public void login(String identity, String password) {
        if (identity == null || identity.trim().isEmpty()) {
            loginState.setValue(AuthState.error("E-posta veya kullanıcı adı girin."));
            return;
        }
        if (password == null || password.isEmpty()) {
            loginState.setValue(AuthState.error("Şifrenizi girin."));
            return;
        }

        loginState.setValue(AuthState.loading());
        repository.login(identity, password, callbackFor(loginState));
    }

    public void resetPassword(String email) {
        String trimmedEmail = email == null ? "" : email.trim();
        if (trimmedEmail.isEmpty()) {
            resetState.setValue(AuthState.error("E-posta adresinizi girin."));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            resetState.setValue(AuthState.error("Geçerli bir e-posta adresi girin."));
            return;
        }

        resetState.setValue(AuthState.loading());
        repository.resetPassword(trimmedEmail, callbackFor(resetState));
    }

    public void clearResetState() {
        resetState.setValue(null);
    }

    private String validateRegistration(String username, String email, String password,
                                        String confirmPassword) {
        if (username == null || username.trim().length() < 3) {
            return "Kullanıcı adı en az 3 karakter olmalıdır.";
        }
        if (!username.trim().matches("[A-Za-z0-9._]+")) {
            return "Kullanıcı adı yalnızca harf, rakam, nokta ve alt çizgi içerebilir.";
        }
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Geçerli bir e-posta adresi girin.";
        }
        if (password == null || password.length() < 6) {
            return "Şifre en az 6 karakter olmalıdır.";
        }
        if (!password.equals(confirmPassword)) {
            return "Şifreler eşleşmiyor.";
        }
        return null;
    }

    private AuthRepository.AuthCallback callbackFor(MutableLiveData<AuthState> state) {
        return new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                state.setValue(AuthState.success(null));
            }

            @Override
            public void onError(String message) {
                state.setValue(AuthState.error(message));
            }
        };
    }
}
