package com.merg.quoteapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.AuthState;
import com.merg.quoteapp.repository.AccountDeletionRepository;
import com.merg.quoteapp.ui.profile.AccountDeletionActivity;
import com.merg.quoteapp.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private TextInputEditText identityInput;
    private TextInputEditText passwordInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        identityInput = findViewById(R.id.editLoginIdentity);
        passwordInput = findViewById(R.id.editLoginPassword);
        loginButton = findViewById(R.id.buttonLogin);
        statusText = findViewById(R.id.textLoginStatus);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getLoginState().observe(this, this::renderLoginState);

        loginButton.setOnClickListener(view ->
                viewModel.login(textOf(identityInput), textOf(passwordInput)));

        findViewById(R.id.buttonRegister).setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.buttonForgotPassword).setOnClickListener(view ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void renderLoginState(AuthState state) {
        boolean loading = state.getStatus() == AuthState.Status.LOADING;
        loginButton.setEnabled(!loading);
        identityInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == AuthState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == AuthState.Status.SUCCESS) {
            showStatus(getString(R.string.login_success), false);
            statusText.postDelayed(this::openPostLoginDestination, 500L);
        }
    }

    private void openPostLoginDestination() {
        AccountDeletionRepository.getInstance().checkCurrentUserPending(pending -> {
            Intent intent = new Intent(this, pending ? AccountDeletionActivity.class : MainActivity.class);
            if (pending) {
                intent.putExtra(AccountDeletionActivity.EXTRA_PENDING_ONLY, true);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(getColor(isError
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusText.setText("");
        statusText.setVisibility(View.GONE);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
